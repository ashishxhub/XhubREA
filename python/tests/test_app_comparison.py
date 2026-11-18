import unittest

from xhubrea_engine.app_comparison import CATEGORIES, compare_app_snapshots


class AppComparisonTests(unittest.TestCase):
    def setUp(self):
        self.app_a = {
            "app_label": "Alpha",
            "permissions": ["CAMERA", "INTERNET", "CAMERA"],
            "components": ["MainActivity", "SettingsActivity"],
            "source_structure": ["src/main", "src/test"],
            "dependencies": ["fastapi", "httpx"],
            "architecture": ["MVVM", "Repository"],
        }
        self.app_b = {
            "app_label": "Beta",
            "permissions": ["INTERNET", "LOCATION"],
            "components": ["MainActivity"],
            "source_structure": ["src/main", "app"],
            "dependencies": ["httpx", "pydantic"],
            "architecture": ["MVVM", "Clean Architecture"],
        }

    def test_reports_shared_and_distinct_entries(self):
        result = compare_app_snapshots(self.app_a, self.app_b)
        permissions = result["comparisons"]["permissions"]
        self.assertEqual(permissions["shared"], ["INTERNET"])
        self.assertEqual(permissions["only_in_app_a"], ["CAMERA"])
        self.assertEqual(permissions["only_in_app_b"], ["LOCATION"])
        self.assertEqual(permissions["shared_count"], 1)

    def test_compares_every_supported_category(self):
        result = compare_app_snapshots(self.app_a, self.app_b)
        self.assertEqual(set(result["comparisons"]), set(CATEGORIES))
        for category in CATEGORIES:
            self.assertIn("only_in_app_a", result["comparisons"][category])
            self.assertIn("only_in_app_b", result["comparisons"][category])

    def test_deterministic_and_deduplicated(self):
        first = compare_app_snapshots(self.app_a, self.app_b)
        reversed_a = dict(self.app_a)
        reversed_a["permissions"] = list(reversed(self.app_a["permissions"]))
        second = compare_app_snapshots(reversed_a, self.app_b)
        self.assertEqual(first, second)
        self.assertEqual(first["comparisons"]["permissions"]["app_a_count"], 2)

    def test_empty_categories_default_to_empty(self):
        result = compare_app_snapshots({"app_label": "A"}, {"app_label": "B"})
        self.assertEqual(result["comparisons"]["permissions"]["shared"], [])

    def test_rejects_non_object_snapshot(self):
        with self.assertRaisesRegex(ValueError, "must be an object"):
            compare_app_snapshots([], self.app_b)

    def test_rejects_invalid_category_and_entries(self):
        with self.assertRaisesRegex(ValueError, "must be a list"):
            compare_app_snapshots(dict(self.app_a, dependencies="httpx"), self.app_b)
        with self.assertRaisesRegex(ValueError, "non-empty strings"):
            compare_app_snapshots(dict(self.app_a, permissions=[""]), self.app_b)

    def test_enforces_category_limit(self):
        oversized = dict(self.app_a, permissions=[f"PERM_{i}" for i in range(201)])
        with self.assertRaisesRegex(ValueError, "at most 200"):
            compare_app_snapshots(oversized, self.app_b)

    def test_does_not_make_security_or_identity_claims(self):
        result = compare_app_snapshots(self.app_a, self.app_b)
        limitations = " ".join(result["limitations"]).lower()
        self.assertIn("does not establish behavior, risk, vulnerability", limitations)
        self.assertIn("app identity", limitations)


if __name__ == "__main__":
    unittest.main()
