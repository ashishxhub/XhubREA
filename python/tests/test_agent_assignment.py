from __future__ import annotations

import unittest

from xhubrea_engine.agent_assignment import AssignmentError, AssignmentPlanner
from xhubrea_engine.task_store import TaskStore


class AssignmentPlannerTests(unittest.TestCase):
    def setUp(self):
        self.store = TaskStore(owner_id="owner")
        self.task = self.store.create_task("Build safe research workflow", created_by="operator")
        self.planner = AssignmentPlanner(self.store)

    def tearDown(self):
        self.store.close()

    def test_multi_department_plan_includes_department_leads_and_is_plan_only(self):
        plan = self.planner.create_plan(self.task["task_id"], [
            {"step_id": "design", "goal": "design system architecture", "department_id": "architecture",
             "specialist_roles": ["systems_architect"]},
            {"step_id": "build", "goal": "implement python backend API", "department_id": "backend",
             "specialist_roles": ["python_engineer"], "depends_on": ["design"]},
        ])
        self.assertFalse(plan["agent_execution_started"])
        self.assertEqual(plan["status"], "plan_only")
        self.assertIn("systems_architect", plan["steps"][0]["assigned_roles"])
        self.assertIn("backend_lead", plan["steps"][1]["assigned_roles"])
        self.assertEqual([s["step_id"] for s in self.planner.ready_steps(plan["plan_id"])], ["design"])

    def test_unknown_department_and_cross_department_role_are_rejected(self):
        with self.assertRaises(ValueError):
            self.planner.create_plan(self.task["task_id"], [
                {"step_id": "x", "goal": "task", "department_id": "unknown"}
            ])
        with self.assertRaises(AssignmentError):
            self.planner.create_plan(self.task["task_id"], [
                {"step_id": "x", "goal": "backend API", "department_id": "backend",
                 "specialist_roles": ["frontend_engineer"]}
            ])

    def test_duplicate_and_unknown_dependency_rejected(self):
        with self.assertRaisesRegex(AssignmentError, "duplicate"):
            self.planner.create_plan(self.task["task_id"], [
                {"step_id": "x", "goal": "one"}, {"step_id": "x", "goal": "two"}
            ])
        with self.assertRaisesRegex(AssignmentError, "unknown dependencies"):
            self.planner.create_plan(self.task["task_id"], [
                {"step_id": "x", "goal": "one", "depends_on": ["missing"]}
            ])

    def test_dependency_cycle_rejected(self):
        with self.assertRaisesRegex(AssignmentError, "cycle"):
            self.planner.create_plan(self.task["task_id"], [
                {"step_id": "a", "goal": "one", "depends_on": ["b"]},
                {"step_id": "b", "goal": "two", "depends_on": ["a"]},
            ])

    def test_result_requires_assigned_role_and_ready_step(self):
        plan = self.planner.create_plan(self.task["task_id"], [
            {"step_id": "a", "goal": "backend API", "department_id": "backend",
             "specialist_roles": ["python_engineer"]},
            {"step_id": "b", "goal": "write documentation", "department_id": "documentation_release",
             "specialist_roles": ["technical_writer"], "depends_on": ["a"]},
        ])
        with self.assertRaisesRegex(AssignmentError, "not assigned"):
            self.planner.submit_result(plan["plan_id"], "a", actor_role="ui_designer", summary="done")
        with self.assertRaisesRegex(AssignmentError, "not ready"):
            self.planner.submit_result(plan["plan_id"], "b", actor_role="documentation_lead", summary="blocked")

    def test_review_gate_and_dependency_unlock(self):
        plan = self.planner.create_plan(self.task["task_id"], [
            {"step_id": "a", "goal": "backend API", "department_id": "backend",
             "specialist_roles": ["python_engineer"]},
            {"step_id": "b", "goal": "write documentation", "department_id": "documentation_release",
             "specialist_roles": ["technical_writer"], "depends_on": ["a"]},
        ])
        plan_id = plan["plan_id"]
        self.planner.submit_result(plan_id, "a", actor_role="python_engineer", summary="API contract prepared")
        with self.assertRaisesRegex(AssignmentError, "quality lead"):
            self.planner.review_result(plan_id, "a", reviewer_role="backend_lead", accepted=True)
        self.planner.review_result(plan_id, "a", reviewer_role="quality_lead", accepted=True)
        self.assertEqual([s["step_id"] for s in self.planner.ready_steps(plan_id)], ["b"])

    def test_rejected_result_returns_to_planned_for_revision(self):
        plan = self.planner.create_plan(self.task["task_id"], [
            {"step_id": "a", "goal": "backend API", "department_id": "backend",
             "specialist_roles": ["python_engineer"]}
        ])
        self.planner.submit_result(plan["plan_id"], "a", actor_role="python_engineer", summary="draft")
        revised = self.planner.review_result(plan["plan_id"], "a", reviewer_role="quality_lead",
                                             accepted=False, feedback="Needs evidence")
        self.assertEqual(revised["steps"][0]["status"], "planned")
        self.assertIsNone(revised["steps"][0]["result_summary"])

    def test_handoff_requires_verified_dependency_and_assigned_roles(self):
        plan = self.planner.create_plan(self.task["task_id"], [
            {"step_id": "a", "goal": "backend API", "department_id": "backend",
             "specialist_roles": ["python_engineer"]},
            {"step_id": "b", "goal": "write documentation", "department_id": "documentation_release",
             "specialist_roles": ["technical_writer"], "depends_on": ["a"]},
        ])
        plan_id = plan["plan_id"]
        with self.assertRaisesRegex(AssignmentError, "verified"):
            self.planner.record_handoff(plan_id, "a", "b", from_role="python_engineer",
                                        to_role="technical_writer", actor_id="owner", summary="handoff")
        self.planner.submit_result(plan_id, "a", actor_role="python_engineer", summary="API spec")
        self.planner.review_result(plan_id, "a", reviewer_role="quality_lead", accepted=True)
        handoff = self.planner.record_handoff(plan_id, "a", "b", from_role="python_engineer",
                                              to_role="technical_writer", actor_id="owner",
                                              summary="Verified API specification")
        self.assertEqual(handoff["from_step"], "a")
        self.assertEqual(len(self.planner.list_handoffs(plan_id)), 1)

    def test_completed_plan_requires_owner_review_not_auto_release(self):
        plan = self.planner.create_plan(self.task["task_id"], [
            {"step_id": "a", "goal": "backend API", "department_id": "backend",
             "specialist_roles": ["python_engineer"]}
        ])
        self.planner.submit_result(plan["plan_id"], "a", actor_role="python_engineer", summary="done")
        result = self.planner.review_result(plan["plan_id"], "a", reviewer_role="quality_lead", accepted=True)
        self.assertEqual(result["status"], "ready_for_owner_review")
        self.assertTrue(result["requires_owner_review"])
        self.assertFalse(result["agent_execution_started"])


if __name__ == "__main__":
    unittest.main()
