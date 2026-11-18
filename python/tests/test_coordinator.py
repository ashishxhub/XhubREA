from __future__ import annotations

import unittest

from xhubrea_engine.coordinator import TaskCoordinator


class TaskCoordinatorTests(unittest.TestCase):
    def setUp(self) -> None:
        self.coordinator = TaskCoordinator()

    def test_routes_android_work(self) -> None:
        plan = self.coordinator.plan("Build an Android Kotlin Compose screen")
        self.assertEqual(plan.department_id, "android")
        self.assertEqual(plan.department_lead, "android_lead")
        self.assertIn("kotlin_engineer", plan.specialist_roles)
        self.assertEqual(plan.execution_mode, "plan_only")

    def test_routes_api_work(self) -> None:
        plan = self.coordinator.plan("Implement a Python backend API endpoint")
        self.assertEqual(plan.department_id, "backend")
        self.assertIn("api_engineer", plan.specialist_roles)
        self.assertIn("python_engineer", plan.specialist_roles)

    def test_explicit_department_overrides_keyword_routing(self) -> None:
        plan = self.coordinator.plan("Build a web dashboard", department_id="android")
        self.assertEqual(plan.department_id, "android")

    def test_unknown_department_is_rejected(self) -> None:
        with self.assertRaisesRegex(ValueError, "unknown department"):
            self.coordinator.plan("Build a dashboard", department_id="unknown")

    def test_production_action_requires_owner_approval(self) -> None:
        plan = self.coordinator.plan("Deploy to production")
        self.assertIn("production_deployment", plan.approval_gates)
        self.assertTrue(plan.requires_owner_review)

    def test_merge_action_requires_owner_approval(self) -> None:
        plan = self.coordinator.plan("Merge pull request to main")
        self.assertIn("merge_to_main", plan.approval_gates)

    def test_empty_goal_is_rejected(self) -> None:
        with self.assertRaisesRegex(ValueError, "non-empty"):
            self.coordinator.plan("  ")

    def test_plan_does_not_claim_to_execute_agents(self) -> None:
        result = self.coordinator.plan("Review project progress").as_dict()
        self.assertEqual(result["execution_mode"], "plan_only")
        self.assertFalse(result["agent_execution_started"])


if __name__ == "__main__":
    unittest.main()
