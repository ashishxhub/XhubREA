from __future__ import annotations

import unittest

from xhubrea_engine.task_queue import TaskQueue
from xhubrea_engine.task_store import TaskStore, TaskStoreError


class TaskQueueTests(unittest.TestCase):
    def setUp(self):
        self.store = TaskStore(owner_id="project-owner")
        self.queue = TaskQueue(self.store)

    def tearDown(self):
        self.store.close()

    def test_priority_order_and_fifo_tie_break(self):
        low = self.queue.enqueue("low", created_by="operator", priority=1)
        high = self.queue.enqueue("high", created_by="operator", priority=9)
        medium = self.queue.enqueue("medium", created_by="operator", priority=5)
        ordered = self.queue.next_tasks()
        self.assertEqual([x["task"]["task_id"] for x in ordered],
                         [high["task"]["task_id"], medium["task"]["task_id"],
                          low["task"]["task_id"]])

    def test_scheduler_is_read_only_and_does_not_claim_tasks(self):
        result = self.queue.enqueue("Do not auto-run", created_by="operator")
        first = self.queue.next_tasks()
        second = self.queue.next_tasks()
        self.assertEqual(first[0]["task"]["task_id"], result["task"]["task_id"])
        self.assertEqual(second[0]["task"]["task_id"], result["task"]["task_id"])
        self.assertEqual(self.store.get_task(result["task"]["task_id"])["status"], "queued")

    def test_department_metadata_is_preserved(self):
        result = self.queue.enqueue("Analyze APK", created_by="operator",
                                    department_id="android", priority=7)
        item = self.queue.next_tasks()[0]
        self.assertEqual(item["department_id"], "android")
        self.assertEqual(item["priority"], 7)

    def test_duplicate_dedupe_key_returns_existing_task(self):
        first = self.queue.enqueue("Analyze APK", created_by="operator",
                                   dedupe_key="analysis-123")
        second = self.queue.enqueue("Different wording", created_by="operator",
                                    dedupe_key="analysis-123")
        self.assertFalse(first["duplicate"])
        self.assertTrue(second["duplicate"])
        self.assertEqual(first["task"]["task_id"], second["task"]["task_id"])

    def test_approval_gated_task_is_separate_from_runnable_queue(self):
        result = self.queue.enqueue("Production release", created_by="operator",
                                     approval_gates=["production_deployment"])
        self.assertEqual(self.queue.next_tasks(), [])
        waiting = self.queue.waiting_for_approval()
        self.assertEqual(waiting[0]["task"]["task_id"], result["task"]["task_id"])
        self.store.approve_task(result["task"]["task_id"], actor_id="project-owner")
        self.assertEqual(self.queue.next_tasks()[0]["task"]["status"], "approved")

    def test_cancel_removes_task_from_next_queue_and_updates_summary(self):
        result = self.queue.enqueue("Review", created_by="operator")
        self.queue.cancel(result["task"]["task_id"], actor_id="project-owner")
        self.assertEqual(self.queue.next_tasks(), [])
        self.assertEqual(self.queue.queue_summary()["cancelled"], 1)

    def test_queue_summary_counts_states(self):
        self.queue.enqueue("Queued", created_by="operator")
        self.queue.enqueue("Approval", created_by="operator",
                           approval_gates=["release_signing"])
        summary = self.queue.queue_summary()
        self.assertEqual(summary["queued"], 1)
        self.assertEqual(summary["awaiting_approval"], 1)

    def test_priority_and_limit_bounds(self):
        with self.assertRaisesRegex(TaskStoreError, "priority"):
            self.queue.enqueue("Bad priority", created_by="operator", priority=11)
        with self.assertRaisesRegex(TaskStoreError, "limit"):
            self.queue.next_tasks(limit=101)

    def test_bool_is_not_accepted_as_priority(self):
        with self.assertRaisesRegex(TaskStoreError, "priority"):
            self.queue.enqueue("Bad priority", created_by="operator", priority=True)

    def test_dedupe_key_validation(self):
        with self.assertRaisesRegex(TaskStoreError, "dedupe_key"):
            self.queue.enqueue("Bad key", created_by="operator", dedupe_key=" ")


if __name__ == "__main__":
    unittest.main()
