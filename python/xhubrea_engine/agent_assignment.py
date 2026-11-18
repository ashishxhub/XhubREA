"""Plan-only multi-role assignment, dependency, handoff, and review workflow.

This module records proposed role assignments. It never starts agents, dispatches
tools, accesses external services, or changes project files.
"""
from __future__ import annotations

import json
import uuid
from datetime import datetime, timezone
from typing import Any

from .agent_registry import load_registry
from .coordinator import TaskCoordinator
from .task_store import TaskStore, TaskStoreError


def _now() -> str:
    return datetime.now(timezone.utc).isoformat()


class AssignmentError(ValueError):
    """Raised when an assignment plan or review transition is invalid."""


class AssignmentPlanner:
    """Persist reviewable assignments over a TaskStore's SQLite connection."""

    def __init__(self, task_store: TaskStore, registry: dict[str, Any] | None = None):
        self.task_store = task_store
        self.connection = task_store.connection
        self.registry = registry if registry is not None else load_registry()
        self.departments = {d["id"]: d for d in self.registry["departments"]}
        self.roles = {
            role["id"]: (department["id"], role)
            for department in self.registry["departments"]
            for role in department["roles"]
        }
        self.coordinator = TaskCoordinator(self.registry)
        self.quality_reviewer = self.departments["quality_security"]["lead"]
        with self.connection:
            self.connection.execute("""
                CREATE TABLE IF NOT EXISTS assignment_plans (
                    plan_id TEXT PRIMARY KEY,
                    task_id TEXT NOT NULL REFERENCES tasks(task_id),
                    plan_json TEXT NOT NULL,
                    created_at TEXT NOT NULL
                )
            """)
            self.connection.execute("""
                CREATE TABLE IF NOT EXISTS assignment_handoffs (
                    handoff_id TEXT PRIMARY KEY,
                    plan_id TEXT NOT NULL REFERENCES assignment_plans(plan_id),
                    from_step TEXT NOT NULL,
                    to_step TEXT NOT NULL,
                    from_role TEXT NOT NULL,
                    to_role TEXT NOT NULL,
                    summary TEXT NOT NULL,
                    actor_id TEXT NOT NULL,
                    created_at TEXT NOT NULL
                )
            """)

    def create_plan(self, task_id: str, steps: list[dict[str, Any]]) -> dict[str, Any]:
        self.task_store.get_task(task_id)
        if not isinstance(steps, list) or not steps or len(steps) > 30:
            raise AssignmentError("steps must contain between 1 and 30 items")
        normalized: list[dict[str, Any]] = []
        seen: set[str] = set()
        for index, raw in enumerate(steps):
            if not isinstance(raw, dict):
                raise AssignmentError("each step must be a mapping")
            step_id = raw.get("step_id")
            goal = raw.get("goal")
            department_id = raw.get("department_id")
            if not isinstance(step_id, str) or not step_id.strip() or len(step_id) > 80:
                raise AssignmentError("each step needs a step_id of 1–80 characters")
            step_id = step_id.strip()
            if step_id in seen:
                raise AssignmentError(f"duplicate step_id: {step_id}")
            seen.add(step_id)
            if not isinstance(goal, str) or not goal.strip():
                raise AssignmentError(f"step {step_id} needs a non-empty goal")
            proposed = self.coordinator.plan(goal, department_id)
            department_id = proposed.department_id
            department = self.departments[department_id]
            requested = raw.get("specialist_roles")
            if requested is None:
                requested = list(proposed.specialist_roles)
            if not isinstance(requested, list) or len(requested) > 8:
                raise AssignmentError(f"step {step_id} specialist_roles must be a list of at most 8 roles")
            assigned = [department["lead"]]
            for role_id in requested:
                if role_id not in self.roles or self.roles[role_id][0] != department_id:
                    raise AssignmentError(f"role {role_id!r} does not belong to department {department_id}")
                if role_id not in assigned:
                    assigned.append(role_id)
            dependencies = raw.get("depends_on", [])
            if not isinstance(dependencies, list) or any(not isinstance(x, str) for x in dependencies):
                raise AssignmentError(f"step {step_id} depends_on must be a list of step IDs")
            normalized.append({
                "step_id": step_id,
                "goal": goal.strip()[:1000],
                "department_id": department_id,
                "department_lead": department["lead"],
                "assigned_roles": assigned,
                "depends_on": list(dict.fromkeys(dependencies)),
                "status": "planned",
                "result_summary": None,
                "review_feedback": None,
            })

        for step in normalized:
            if step["step_id"] in step["depends_on"]:
                raise AssignmentError("a step cannot depend on itself")
            unknown = set(step["depends_on"]) - seen
            if unknown:
                raise AssignmentError(f"unknown dependencies for {step['step_id']}: {sorted(unknown)}")
        graph = {s["step_id"]: s["depends_on"] for s in normalized}
        visiting: set[str] = set()
        visited: set[str] = set()
        def visit(node: str) -> None:
            if node in visiting:
                raise AssignmentError("dependency cycle detected")
            if node in visited:
                return
            visiting.add(node)
            for dependency in graph[node]:
                visit(dependency)
            visiting.remove(node)
            visited.add(node)
        for step_id in graph:
            visit(step_id)

        plan_id = str(uuid.uuid4())
        plan = {
            "plan_id": plan_id,
            "task_id": task_id,
            "status": "plan_only",
            "requires_owner_review": True,
            "agent_execution_started": False,
            "reviewer_role": self.quality_reviewer,
            "steps": normalized,
            "created_at": _now(),
        }
        with self.connection:
            self.connection.execute(
                "INSERT INTO assignment_plans(plan_id, task_id, plan_json, created_at) VALUES (?, ?, ?, ?)",
                (plan_id, task_id, json.dumps(plan), plan["created_at"]),
            )
        return plan

    def get_plan(self, plan_id: str) -> dict[str, Any]:
        row = self.connection.execute(
            "SELECT plan_json FROM assignment_plans WHERE plan_id = ?", (plan_id,)
        ).fetchone()
        if row is None:
            raise AssignmentError("assignment plan not found")
        return json.loads(row["plan_json"])

    def ready_steps(self, plan_id: str) -> list[dict[str, Any]]:
        plan = self.get_plan(plan_id)
        by_id = {s["step_id"]: s for s in plan["steps"]}
        return [
            dict(step) for step in plan["steps"]
            if step["status"] == "planned"
            and all(by_id[dep]["status"] == "verified" for dep in step["depends_on"])
        ]

    def submit_result(self, plan_id: str, step_id: str, *, actor_role: str, summary: str) -> dict[str, Any]:
        if not isinstance(summary, str) or not summary.strip():
            raise AssignmentError("summary must be a non-empty string")
        plan = self.get_plan(plan_id)
        step = self._step(plan, step_id)
        if actor_role not in step["assigned_roles"]:
            raise AssignmentError("result submitter is not assigned to this step")
        if step not in self.ready_steps(plan_id):
            raise AssignmentError("step is not ready or its dependencies are not verified")
        step["status"] = "submitted"
        step["result_summary"] = summary.strip()[:2000]
        self._save(plan)
        return plan

    def review_result(self, plan_id: str, step_id: str, *, reviewer_role: str,
                      accepted: bool, feedback: str = "") -> dict[str, Any]:
        plan = self.get_plan(plan_id)
        if reviewer_role != plan["reviewer_role"]:
            raise AssignmentError("only the designated quality lead can review this result")
        step = self._step(plan, step_id)
        if step["status"] != "submitted":
            raise AssignmentError("only submitted results can be reviewed")
        step["status"] = "verified" if accepted else "planned"
        step["review_feedback"] = feedback.strip()[:1000]
        if not accepted:
            step["result_summary"] = None
        if all(s["status"] == "verified" for s in plan["steps"]):
            plan["status"] = "ready_for_owner_review"
        self._save(plan)
        return plan

    def record_handoff(self, plan_id: str, from_step: str, to_step: str, *,
                       from_role: str, to_role: str, actor_id: str,
                       summary: str) -> dict[str, Any]:
        plan = self.get_plan(plan_id)
        source = self._step(plan, from_step)
        target = self._step(plan, to_step)
        if from_step not in target["depends_on"]:
            raise AssignmentError("handoff must follow a declared dependency")
        if source["status"] != "verified":
            raise AssignmentError("source step must be verified before handoff")
        if from_role not in source["assigned_roles"] or to_role not in target["assigned_roles"]:
            raise AssignmentError("handoff roles must belong to the source and destination assignments")
        if not all(isinstance(x, str) and x.strip() for x in (actor_id, summary)):
            raise AssignmentError("actor_id and summary are required")
        handoff = {
            "handoff_id": str(uuid.uuid4()), "plan_id": plan_id,
            "from_step": from_step, "to_step": to_step,
            "from_role": from_role, "to_role": to_role,
            "summary": summary.strip()[:1000], "actor_id": actor_id.strip()[:120],
            "created_at": _now(),
        }
        with self.connection:
            self.connection.execute(
                """INSERT INTO assignment_handoffs
                   (handoff_id, plan_id, from_step, to_step, from_role, to_role, summary, actor_id, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                tuple(handoff[k] for k in ("handoff_id", "plan_id", "from_step", "to_step",
                                            "from_role", "to_role", "summary", "actor_id", "created_at")),
            )
        return handoff

    def list_handoffs(self, plan_id: str) -> list[dict[str, Any]]:
        self.get_plan(plan_id)
        rows = self.connection.execute(
            "SELECT * FROM assignment_handoffs WHERE plan_id = ? ORDER BY created_at, handoff_id",
            (plan_id,),
        ).fetchall()
        return [dict(row) for row in rows]

    @staticmethod
    def _step(plan: dict[str, Any], step_id: str) -> dict[str, Any]:
        for step in plan["steps"]:
            if step["step_id"] == step_id:
                return step
        raise AssignmentError("step not found")

    def _save(self, plan: dict[str, Any]) -> None:
        with self.connection:
            self.connection.execute(
                "UPDATE assignment_plans SET plan_json = ? WHERE plan_id = ?",
                (json.dumps(plan), plan["plan_id"]),
            )
