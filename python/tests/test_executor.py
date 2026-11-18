from __future__ import annotations

import unittest

from xhubrea_engine.executor import (
    DisabledExecutor,
    ExecutionRequest,
    ExecutionResult,
    ExecutorDisabledError,
    GuardedExecutor,
)
from xhubrea_engine.task_store import TaskStore, TaskStoreError


class FakeExecutor:
    def __init__(self, result=None):
        self.result = result or ExecutionResult(True, "Read-only metadata summary")
        self.requests = []

    def execute(self, request):
        self.requests.append(request)
        return self.result


class ExecutorTests(unittest.TestCase):
    def setUp(self):
        self.store = TaskStore(owner_id="project-owner")

    def tearDown(self):
        self.store.close()

    def test_disabled_executor_never_runs(self):
        with self.assertRaises(ExecutorDisabledError):
            DisabledExecutor().execute(ExecutionRequest("task-1", "inspect"))

    def test_missing_executor_never_starts_task_even_if_flag_is_set(self):
        task = self.store.create_task("Inspect", created_by="operator")
        guard = GuardedExecutor(self.store, execution_enabled=True)
        with self.assertRaises(ExecutorDisabledError):
            guard.execute_task(task["task_id"], actor_id="operator")
        self.assertEqual(self.store.get_task(task["task_id"])["status"], "queued")

    def test_guard_is_disabled_by_default(self):
        task = self.store.create_task("Inspect authorized metadata", created_by="operator")
        fake = FakeExecutor()
        guard = GuardedExecutor(self.store, executor=fake)
        with self.assertRaises(ExecutorDisabledError):
            guard.execute_task(task["task_id"], actor_id="operator")
        self.assertEqual(fake.requests, [])
        self.assertEqual(self.store.get_task(task["task_id"])["status"], "queued")

    def test_approved_task_runs_through_injected_executor(self):
        task = self.store.create_task(
            "Review production release plan",
            created_by="operator",
            approval_gates=["production_deployment"],
        )
        self.store.approve_task(task["task_id"], actor_id="project-owner")
        fake = FakeExecutor()
        # The task store also needs explicit enablement; this is test-only injection.
        self.store.execution_enabled = True
        guard = GuardedExecutor(self.store, executor=fake, execution_enabled=True)
        result = guard.execute_task(task["task_id"], actor_id="operator", timeout_seconds=10)
        self.assertTrue(result.succeeded)
        self.assertEqual(self.store.get_task(task["task_id"])["status"], "succeeded")
        self.assertEqual(fake.requests[0].timeout_seconds, 10)

    def test_unapproved_gated_task_is_rejected(self):
        task = self.store.create_task(
            "Release plan", created_by="operator", approval_gates=["release_signing"]
        )
        self.store.execution_enabled = True
        guard = GuardedExecutor(self.store, executor=FakeExecutor(), execution_enabled=True)
        with self.assertRaisesRegex(TaskStoreError, "approval is required"):
            guard.execute_task(task["task_id"], actor_id="operator")

    def test_timeout_is_bounded_before_task_starts(self):
        task = self.store.create_task("Inspect", created_by="operator")
        self.store.execution_enabled = True
        fake = FakeExecutor()
        guard = GuardedExecutor(self.store, executor=fake, execution_enabled=True)
        with self.assertRaisesRegex(ValueError, "timeout_seconds"):
            guard.execute_task(task["task_id"], actor_id="operator", timeout_seconds=61)
        self.assertEqual(fake.requests, [])
        self.assertEqual(self.store.get_task(task["task_id"])["status"], "queued")

    def test_executor_result_summary_is_bounded(self):
        task = self.store.create_task("Inspect", created_by="operator")
        self.store.execution_enabled = True
        fake = FakeExecutor(ExecutionResult(True, "x" * 3000))
        guard = GuardedExecutor(self.store, executor=fake, execution_enabled=True)
        result = guard.execute_task(task["task_id"], actor_id="operator")
        self.assertEqual(len(result.summary), 2000)

    def test_executor_failure_is_recorded_without_exception_details(self):
        class ExplodingExecutor:
            def execute(self, request):
                raise RuntimeError("sensitive token must not be logged")

        task = self.store.create_task("Inspect", created_by="operator")
        self.store.execution_enabled = True
        guard = GuardedExecutor(
            self.store, executor=ExplodingExecutor(), execution_enabled=True
        )
        with self.assertRaisesRegex(RuntimeError, "sensitive token"):
            guard.execute_task(task["task_id"], actor_id="operator")
        stored = self.store.get_task(task["task_id"])
        self.assertEqual(stored["status"], "failed")
        self.assertNotIn("sensitive token", stored["result_summary"])

    def test_non_approved_status_cannot_be_reexecuted(self):
        task = self.store.create_task("Inspect", created_by="operator")
        self.store.execution_enabled = True
        fake = FakeExecutor()
        guard = GuardedExecutor(self.store, executor=fake, execution_enabled=True)
        guard.execute_task(task["task_id"], actor_id="operator")
        with self.assertRaisesRegex(TaskStoreError, "not eligible"):
            guard.execute_task(task["task_id"], actor_id="operator")
        self.assertEqual(len(fake.requests), 1)


if __name__ == "__main__":
    unittest.main()
