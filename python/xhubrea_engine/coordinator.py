"""Provider-neutral, plan-only task coordinator for XhubREA.

This module creates a reviewable task plan from the role registry. It does not
invoke agents, execute tools, access external services, or perform project changes.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from .agent_registry import load_registry


APPROVAL_KEYWORDS = {
    "production_deployment": ("deploy to production", "production deploy", "release to production"),
    "merge_to_main": ("merge to main", "merge pull request", "merge pr"),
    "secret_access": ("read secrets", "show api key", "access credentials"),
    "external_data_processing": ("send data to external", "upload customer data", "external processing"),
    "paid_service_or_spending": ("buy credits", "purchase", "spend money", "paid service"),
    "destructive_operation": ("delete production", "drop database", "destructive operation"),
    "release_signing": ("sign release", "release signing", "sign apk"),
}

ROUTE_HINTS = {
    "research": ("research", "inspect", "analyze", "compare", "investigate", "apk", "application"),
    "architecture": ("architecture", "design system", "technical design", "module boundary"),
    "backend": ("backend", "api", "python", "database", "server"),
    "android": ("android", "kotlin", "compose", "apk app"),
    "web": ("web", "frontend", "dashboard", "website", "browser"),
    "ai_ml": ("ai", "agent", "model", "prompt", "coordinator", "llm"),
    "design": ("ui", "ux", "interface", "visual design", "figma"),
    "quality_security": ("test", "bug", "security review", "audit", "verify", "quality"),
    "documentation_release": ("documentation", "readme", "release notes", "guide"),
    "management": ("plan", "schedule", "progress", "milestone", "project"),
}

ROLE_HINTS = {
    "application_researcher": ("apk", "application", "app structure", "inspect app"),
    "evidence_analyst": ("evidence", "source", "confidence"),
    "systems_architect": ("architecture", "system design", "module boundary"),
    "api_engineer": ("api", "endpoint", "rest"),
    "python_engineer": ("python", "python module"),
    "kotlin_engineer": ("kotlin", "android logic"),
    "compose_engineer": ("compose", "android ui"),
    "frontend_engineer": ("frontend", "web ui", "dashboard"),
    "agent_engineer": ("agent", "coordinator", "task routing"),
    "model_evaluation_engineer": ("evaluate model", "model evaluation"),
    "ui_designer": ("ui", "visual design", "interface"),
    "qa_engineer": ("test", "verify", "regression"),
    "security_auditor": ("security", "authorization", "data handling"),
    "technical_writer": ("documentation", "readme", "guide"),
    "project_manager": ("plan", "progress", "milestone"),
}


@dataclass(frozen=True)
class TaskPlan:
    """A plan-only assignment proposal requiring owner review before execution."""

    goal: str
    department_id: str
    department_lead: str
    specialist_roles: tuple[str, ...]
    approval_gates: tuple[str, ...]
    execution_mode: str = "plan_only"
    requires_owner_review: bool = True

    def as_dict(self) -> dict[str, Any]:
        return {
            "goal": self.goal,
            "department_id": self.department_id,
            "department_lead": self.department_lead,
            "specialist_roles": list(self.specialist_roles),
            "approval_gates": list(self.approval_gates),
            "execution_mode": self.execution_mode,
            "requires_owner_review": self.requires_owner_review,
            "agent_execution_started": False,
        }


class TaskCoordinator:
    """Select a department and role templates using transparent keyword rules."""

    def __init__(self, registry: dict[str, Any] | None = None) -> None:
        self.registry = registry if registry is not None else load_registry()
        self.departments = {item["id"]: item for item in self.registry["departments"]}
        self.roles = {
            role["id"]: role
            for department in self.registry["departments"]
            for role in department["roles"]
        }

    def plan(self, goal: str, department_id: str | None = None) -> TaskPlan:
        if not isinstance(goal, str) or not goal.strip():
            raise ValueError("goal must be a non-empty string")
        normalized = goal.casefold()

        if department_id is not None:
            if department_id not in self.departments:
                raise ValueError(f"unknown department: {department_id}")
            selected_department = department_id
        else:
            scores = {
                dept_id: sum(
                    1 for hint in hints if hint in normalized
                )
                for dept_id, hints in ROUTE_HINTS.items()
            }
            selected_department = max(
                scores,
                key=lambda item: (scores[item], item == "management"),
            )
            if scores[selected_department] == 0:
                selected_department = "management"

        department = self.departments[selected_department]
        available_roles = {role["id"] for role in department["roles"]}
        specialists = [
            role_id for role_id, hints in ROLE_HINTS.items()
            if role_id in available_roles and any(hint in normalized for hint in hints)
        ]
        # Every proposed plan includes the selected department lead for coordination.
        specialists = [role for role in specialists if role != department["lead"]]
        approval_gates = tuple(
            gate for gate, phrases in APPROVAL_KEYWORDS.items()
            if any(phrase in normalized for phrase in phrases)
        )
        return TaskPlan(
            goal=goal.strip(),
            department_id=selected_department,
            department_lead=department["lead"],
            specialist_roles=tuple(specialists),
            approval_gates=approval_gates,
        )
