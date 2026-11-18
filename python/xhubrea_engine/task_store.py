"""Persistent, approval-gated task lifecycle for XhubREA.

This is a task ledger, not an agent runner. Execution is disabled by default;
no provider, shell, repository, or external tool is invoked by this module.
"""
from __future__ import annotations

import json
import sqlite3
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


TASK_STATUSES = {
    "awaiting_approval", "queued", "approved", "running",
    "succeeded", "failed", "cancelled",
}
TERMINAL_STATUSES = {"succeeded", "cancelled"}
MAX_RETRIES_LIMIT = 5


def _utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


class TaskStoreError(ValueError):
    """Raised when a task operation violates the lifecycle contract."""


class TaskStore:
    """SQLite task/audit store with explicit owner approval and disabled execution."""

    def __init__(
        self,
        database_path: str | Path = ":memory:",
        *,
        owner_id: str,
        execution_enabled: bool = False,
        max_retries: int = 2,
    ) -> None:
        if not isinstance(owner_id, str) or not owner_id.strip():
            raise ValueError("owner_id must be a non-empty string")
        if not isinstance(max_retries, int) or not 0 <= max_retries <= MAX_RETRIES_LIMIT:
            raise ValueError(f"max_retries must be between 0 and {MAX_RETRIES_LIMIT}")
        self.owner_id = owner_id.strip()
        self.execution_enabled = bool(execution_enabled)
        self.max_retries = max_retries
        self.connection = sqlite3.connect(str(database_path))
        self.connection.row_factory = sqlite3.Row
        self.connection.execute("PRAGMA foreign_keys = ON")
        self._initialize()

    def _initialize(self) -> None:
        with self.connection:
            self.connection.executescript("""
                CREATE TABLE IF NOT EXISTS tasks (
                    task_id TEXT PRIMARY KEY,
                    goal TEXT NOT NULL,
                    status TEXT NOT NULL,
                    approval_gates TEXT NOT NULL,
                    created_by TEXT NOT NULL,
                    approved_by TEXT,
                    attempts INTEGER NOT NULL DEFAULT 0,
                    max_retries INTEGER NOT NULL,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    result_summary TEXT
                );
                CREATE TABLE IF NOT EXISTS task_audit (
                    event_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_id TEXT NOT NULL REFERENCES tasks(task_id),
                    actor_id TEXT NOT NULL,
                    event_type TEXT NOT NULL,
                    from_status TEXT,
                    to_status TEXT,
                    note TEXT NOT NULL,
                    created_at TEXT NOT NULL
                );
                CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
                CREATE INDEX IF NOT EXISTS idx_task_audit_task ON task_audit(task_id, event_id);
            """)

    def close(self) -> None:
        self.connection.close()

    def _audit(
        self, task_id: str, actor_id: str, event_type: str,
        from_status: str | None, to_status: str | None, note: str,
    ) -> None:
        self.connection.execute(
            """INSERT INTO task_audit
               (task_id, actor_id, event_type, from_status, to_status, note, created_at)
               VALUES (?, ?, ?, ?, ?, ?, ?)""",
            (task_id, actor_id, event_type, from_status, to_status, note[:500], _utc_now()),
        )

    def create_task(
        self, goal: str, *, created_by: str, approval_gates: list[str] | tuple[str, ...] = (),
    ) -> dict[str, Any]:
        if not isinstance(goal, str) or not goal.strip():
            raise TaskStoreError("goal must be a non-empty string")
        if not isinstance(created_by, str) or not created_by.strip():
            raise TaskStoreError("created_by must be a non-empty string")
        gates = sorted({str(gate).strip() for gate in approval_gates if str(gate).strip()})
        if len(gates) > 20:
            raise TaskStoreError("at most 20 approval gates are allowed")
        status = "awaiting_approval" if gates else "queued"
        task_id = str(uuid.uuid4())
        now = _utc_now()
        with self.connection:
            self.connection.execute(
                """INSERT INTO tasks
                   (task_id, goal, status, approval_gates, created_by, attempts,
                    max_retries, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?)""",
                (task_id, goal.strip()[:2000], status, json.dumps(gates),
                 created_by.strip()[:120], self.max_retries, now, now),
            )
            self._audit(task_id, created_by.strip()[:120], "created", None, status,
                        "Task metadata recorded; no agent execution started.")
        return self.get_task(task_id)

    def get_task(self, task_id: str) -> dict[str, Any]:
        row = self.connection.execute(
            "SELECT * FROM tasks WHERE task_id = ?", (task_id,)
        ).fetchone()
        if row is None:
            raise TaskStoreError("task not found")
        result = dict(row)
        result["approval_gates"] = json.loads(result["approval_gates"])
        result["agent_execution_started"] = result["status"] == "running"
        return result

    def list_audit_events(self, task_id: str) -> list[dict[str, Any]]:
        self.get_task(task_id)
        rows = self.connection.execute(
            "SELECT * FROM task_audit WHERE task_id = ? ORDER BY event_id",
            (task_id,),
        ).fetchall()
        return [dict(row) for row in rows]

    def approve_task(self, task_id: str, *, actor_id: str, note: str = "") -> dict[str, Any]:
        if actor_id != self.owner_id:
            raise TaskStoreError("only the configured owner can approve a task")
        task = self.get_task(task_id)
        if task["status"] != "awaiting_approval":
            raise TaskStoreError("task is not awaiting approval")
        now = _utc_now()
        with self.connection:
            self.connection.execute(
                """UPDATE tasks SET status = 'approved', approved_by = ?, updated_at = ?
                   WHERE task_id = ?""",
                (self.owner_id, now, task_id),
            )
            self._audit(task_id, self.owner_id, "owner_approved",
                        "awaiting_approval", "approved", note or "Owner approval recorded.")
        return self.get_task(task_id)

    def start_task(self, task_id: str, *, actor_id: str) -> dict[str, Any]:
        if not self.execution_enabled:
            raise TaskStoreError("task execution is disabled; enable only after owner review")
        task = self.get_task(task_id)
        if task["approval_gates"] and task["status"] != "approved":
            raise TaskStoreError("owner approval is required before execution")
        if task["status"] not in {"queued", "approved", "failed"}:
            raise TaskStoreError("task cannot start from its current status")
        if task["attempts"] > task["max_retries"]:
            raise TaskStoreError("task retry limit has been reached")
        now = _utc_now()
        with self.connection:
            self.connection.execute(
                """UPDATE tasks SET status = 'running', attempts = attempts + 1,
                   updated_at = ? WHERE task_id = ?""", (now, task_id)
            )
            self._audit(task_id, actor_id, "execution_started", task["status"],
                        "running", "Execution explicitly enabled; executor integration remains separate.")
        return self.get_task(task_id)

    def finish_task(
        self, task_id: str, *, actor_id: str, succeeded: bool, summary: str = "",
    ) -> dict[str, Any]:
        task = self.get_task(task_id)
        if task["status"] != "running":
            raise TaskStoreError("only a running task can be completed")
        target = "succeeded" if succeeded else "failed"
        now = _utc_now()
        with self.connection:
            self.connection.execute(
                """UPDATE tasks SET status = ?, result_summary = ?, updated_at = ?
                   WHERE task_id = ?""",
                (target, summary[:2000], now, task_id),
            )
            self._audit(task_id, actor_id, "execution_finished", "running", target,
                        "Result metadata recorded; validate outputs before relying on them.")
        return self.get_task(task_id)

    def cancel_task(self, task_id: str, *, actor_id: str, note: str = "") -> dict[str, Any]:
        task = self.get_task(task_id)
        if task["status"] in TERMINAL_STATUSES:
            raise TaskStoreError("terminal task cannot be cancelled")
        now = _utc_now()
        with self.connection:
            self.connection.execute(
                "UPDATE tasks SET status = 'cancelled', updated_at = ? WHERE task_id = ?",
                (now, task_id),
            )
            self._audit(task_id, actor_id, "cancelled", task["status"], "cancelled", note)
        return self.get_task(task_id)
