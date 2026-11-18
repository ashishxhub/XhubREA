"""Persistent priority queue for XhubREA task metadata.

The scheduler returns eligible task plans only. It never claims, starts, or
executes work and does not call providers or external tools.
"""
from __future__ import annotations

import json
import sqlite3
import uuid
from datetime import datetime, timezone
from typing import Any

from .task_store import TaskStore, TaskStoreError


MAX_PRIORITY = 10
MAX_DEPARTMENT_ID_LENGTH = 80
MAX_DEDUPE_KEY_LENGTH = 160


def _now() -> str:
    return datetime.now(timezone.utc).isoformat()


class TaskQueue:
    """SQLite-backed enqueue/dedupe/priority scheduler over a TaskStore."""

    def __init__(self, task_store: TaskStore) -> None:
        self.task_store = task_store
        self.connection = task_store.connection
        with self.connection:
            self.connection.executescript("""
                CREATE TABLE IF NOT EXISTS task_queue (
                    task_id TEXT PRIMARY KEY REFERENCES tasks(task_id),
                    priority INTEGER NOT NULL DEFAULT 5,
                    department_id TEXT,
                    dedupe_key TEXT UNIQUE,
                    queue_state TEXT NOT NULL DEFAULT 'pending',
                    created_at TEXT NOT NULL
                );
                CREATE INDEX IF NOT EXISTS idx_task_queue_order
                    ON task_queue(queue_state, priority DESC, created_at ASC);
            """)

    def enqueue(
        self,
        goal: str,
        *,
        created_by: str,
        priority: int = 5,
        department_id: str | None = None,
        approval_gates: list[str] | tuple[str, ...] = (),
        dedupe_key: str | None = None,
    ) -> dict[str, Any]:
        if not isinstance(priority, int) or isinstance(priority, bool) or not 0 <= priority <= MAX_PRIORITY:
            raise TaskStoreError(f"priority must be an integer from 0 to {MAX_PRIORITY}")
        if department_id is not None:
            if not isinstance(department_id, str) or not department_id.strip():
                raise TaskStoreError("department_id must be a non-empty string when supplied")
            if len(department_id.strip()) > MAX_DEPARTMENT_ID_LENGTH:
                raise TaskStoreError("department_id is too long")
        if dedupe_key is not None:
            if not isinstance(dedupe_key, str) or not dedupe_key.strip():
                raise TaskStoreError("dedupe_key must be a non-empty string when supplied")
            dedupe_key = dedupe_key.strip()
            if len(dedupe_key) > MAX_DEDUPE_KEY_LENGTH:
                raise TaskStoreError("dedupe_key is too long")
            existing = self.connection.execute(
                "SELECT task_id FROM task_queue WHERE dedupe_key = ?", (dedupe_key,)
            ).fetchone()
            if existing:
                task = self.task_store.get_task(existing["task_id"])
                return {"task": task, "duplicate": True}

        if not isinstance(goal, str) or not goal.strip():
            raise TaskStoreError("goal must be a non-empty string")
        if not isinstance(created_by, str) or not created_by.strip():
            raise TaskStoreError("created_by must be a non-empty string")
        gates = sorted({str(g).strip() for g in approval_gates if str(g).strip()})
        if len(gates) > 20:
            raise TaskStoreError("at most 20 approval gates are allowed")

        task_id = str(uuid.uuid4())
        now = _now()
        status = "awaiting_approval" if gates else "queued"
        with self.connection:
            # A uniqueness race is handled by SQLite; rollback keeps task and queue atomic.
            self.connection.execute(
                """INSERT INTO tasks
                   (task_id, goal, status, approval_gates, created_by, attempts,
                    max_retries, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?)""",
                (task_id, goal.strip()[:2000], status, json.dumps(gates),
                 created_by.strip()[:120], self.task_store.max_retries, now, now),
            )
            self.connection.execute(
                """INSERT INTO task_queue
                   (task_id, priority, department_id, dedupe_key, queue_state, created_at)
                   VALUES (?, ?, ?, ?, 'pending', ?)""",
                (task_id, priority, department_id.strip() if department_id else None,
                 dedupe_key, now),
            )
            self.task_store._audit(
                task_id, created_by.strip()[:120], "queued", None, status,
                "Task added to persistent priority queue; no execution started.",
            )
        return {"task": self.task_store.get_task(task_id), "duplicate": False}

    def next_tasks(self, *, limit: int = 10) -> list[dict[str, Any]]:
        """Return eligible work in priority/FIFO order without claiming it."""
        if not isinstance(limit, int) or isinstance(limit, bool) or not 1 <= limit <= 100:
            raise TaskStoreError("limit must be an integer from 1 to 100")
        rows = self.connection.execute(
            """SELECT q.task_id, q.priority, q.department_id, q.created_at
               FROM task_queue q JOIN tasks t ON t.task_id = q.task_id
               WHERE q.queue_state = 'pending' AND t.status IN ('queued', 'approved')
               ORDER BY q.priority DESC, q.created_at ASC, q.task_id ASC
               LIMIT ?""",
            (limit,),
        ).fetchall()
        return [
            {"task": self.task_store.get_task(row["task_id"]),
             "priority": row["priority"],
             "department_id": row["department_id"],
             "queued_at": row["created_at"]}
            for row in rows
        ]

    def waiting_for_approval(self, *, limit: int = 100) -> list[dict[str, Any]]:
        if not isinstance(limit, int) or isinstance(limit, bool) or not 1 <= limit <= 100:
            raise TaskStoreError("limit must be an integer from 1 to 100")
        rows = self.connection.execute(
            """SELECT q.task_id, q.priority, q.department_id, q.created_at
               FROM task_queue q JOIN tasks t ON t.task_id = q.task_id
               WHERE q.queue_state = 'pending' AND t.status = 'awaiting_approval'
               ORDER BY q.priority DESC, q.created_at ASC LIMIT ?""",
            (limit,),
        ).fetchall()
        return [
            {"task": self.task_store.get_task(row["task_id"]),
             "priority": row["priority"],
             "department_id": row["department_id"],
             "queued_at": row["created_at"]}
            for row in rows
        ]

    def cancel(self, task_id: str, *, actor_id: str, note: str = "") -> dict[str, Any]:
        task = self.task_store.cancel_task(task_id, actor_id=actor_id, note=note)
        with self.connection:
            self.connection.execute(
                "UPDATE task_queue SET queue_state = 'cancelled' WHERE task_id = ?",
                (task_id,),
            )
        return task

    def queue_summary(self) -> dict[str, int]:
        rows = self.connection.execute(
            """SELECT
                 SUM(CASE WHEN q.queue_state = 'pending' AND t.status = 'queued' THEN 1 ELSE 0 END) AS queued,
                 SUM(CASE WHEN q.queue_state = 'pending' AND t.status = 'approved' THEN 1 ELSE 0 END) AS approved,
                 SUM(CASE WHEN q.queue_state = 'pending' AND t.status = 'awaiting_approval' THEN 1 ELSE 0 END) AS awaiting_approval,
                 SUM(CASE WHEN q.queue_state = 'cancelled' OR t.status = 'cancelled' THEN 1 ELSE 0 END) AS cancelled
               FROM task_queue q JOIN tasks t ON t.task_id = q.task_id"""
        ).fetchone()
        return {key: int(rows[key] or 0) for key in
                ("queued", "approved", "awaiting_approval", "cancelled")}
