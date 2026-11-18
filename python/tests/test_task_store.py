from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from xhubrea_engine.task_store import TaskStore, TaskStoreError


class TaskStoreTests(unittest.TestCase):
    def setUp(self) -> None:
        self.store = TaskStore(owner_id="project-owner")

    def tearDown(self) -> None:
        self.store.close()

    def test_create_task_is_queued_without_gates(self) -> None:
        task = self.store.create_task("Inspect authorized APK metadata", created_by="operator")
        self.assertEqual(task["status"], "queued")
        self.assertFalse(task["agent_execution_started"])

    def test_sensitive_task_waits_for_owner_approval(self) -> None:
        task = self.store.create_task(
            "Prepare production deployment", created_by="operator",
            approval_gates=["production_deployment"],
        )
        self.assertEqual(task["status"], "awaiting_approval")

    def test_non_owner_cannot_approve(self) -> None:
        task = self.store.create_task("Deploy", created_by="operator",
                                      approval_gates=["production_deployment"])
        with self.assertRaisesRegex(TaskStoreError, "only the configured owner"):
            self.store.approve_task(task["task_id"], actor_id="other-user")

    def test_owner_approval_is_audited(self) -> None:
        task = self.store.create_task("Deploy", created_by="operator",
                                      approval_gates=["production_deployment"])
        approved = self.store.approve_task(task["task_id"], actor_id="project-owner")
        self.assertEqual(approved["status"], "approved")
        events = self.store.list_audit_events(task["task_id"])
        self.assertEqual(events[-1]["event_type"], "owner_approved")

    def test_execution_is_disabled_by_default(self) -> None:
        task = self.store.create_task("Run analysis", created_by="operator")
        with self.assertRaisesRegex(TaskStoreError, "execution is disabled"):
            self.store.start_task(task["task_id"], actor_id="operator")

    def test_gated_task_cannot_start_without_approval_even_when_enabled(self) -> None:
        store = TaskStore(owner_id="project-owner", execution_enabled=True)
        try:
            task = store.create_task("Deploy", created_by="operator",
                                     approval_gates=["production_deployment"])
            with self.assertRaisesRegex(TaskStoreError, "approval is required"):
                store.start_task(task["task_id"], actor_id="operator")
        finally:
            store.close()

    def test_execution_lifecycle_and_retry_bound(self) -> None:
        store = TaskStore(owner_id="project-owner", execution_enabled=True, max_retries=1)
        try:
            task = store.create_task("Run test", created_by="operator")
            started = store.start_task(task["task_id"], actor_id="operator")
            self.assertEqual(started["attempts"], 1)
            failed = store.finish_task(task["task_id"], actor_id="operator",
                                       succeeded=False, summary="Transient test failure")
            self.assertEqual(failed["status"], "failed")
            restarted = store.start_task(task["task_id"], actor_id="operator")
            self.assertEqual(restarted["attempts"], 2)
            store.finish_task(task["task_id"], actor_id="operator", succeeded=False)
            with self.assertRaisesRegex(TaskStoreError, "retry limit"):
                store.start_task(task["task_id"], actor_id="operator")
        finally:
            store.close()

    def test_cannot_finish_non_running_task(self) -> None:
        task = self.store.create_task("Run test", created_by="operator")
        with self.assertRaisesRegex(TaskStoreError, "running task"):
            self.store.finish_task(task["task_id"], actor_id="operator", succeeded=True)

    def test_cancel_is_audited_and_terminal(self) -> None:
        task = self.store.create_task("Review task", created_by="operator")
        cancelled = self.store.cancel_task(task["task_id"], actor_id="project-owner",
                                           note="Owner cancelled")
        self.assertEqual(cancelled["status"], "cancelled")
        with self.assertRaisesRegex(TaskStoreError, "terminal task"):
            self.store.cancel_task(task["task_id"], actor_id="project-owner")

    def test_persistence_across_store_instances(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "tasks.sqlite3"
            first = TaskStore(path, owner_id="project-owner")
            task = first.create_task("Persist this task", created_by="operator")
            first.close()
            second = TaskStore(path, owner_id="project-owner")
            try:
                self.assertEqual(second.get_task(task["task_id"])["goal"],
                                 "Persist this task")
            finally:
                second.close()

    def test_retry_limit_configuration_is_bounded(self) -> None:
        with self.assertRaisesRegex(ValueError, "max_retries"):
            TaskStore(owner_id="project-owner", max_retries=99)


if __name__ == "__main__":
    unittest.main()
