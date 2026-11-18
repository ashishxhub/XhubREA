"""Tests for bounded, read-only APK inventory analysis."""
import unittest

from xhubrea_engine.apk_research import analyze_apk_inventory


class ApkResearchTests(unittest.TestCase):
    def test_empty_inventory_returns_neutral_summary(self):
        result = analyze_apk_inventory({"package_name": "org.example.demo"})
        self.assertEqual(result["package_name"], "org.example.demo")
        self.assertEqual(result["inventory_counts"]["permissions"], 0)
        self.assertEqual(result["indicators"], [])
        self.assertTrue(result["limitations"])

    def test_flags_review_indicators_without_claiming_vulnerability(self):
        result = analyze_apk_inventory({
            "package_name": "org.example.demo",
            "version_name": "1.2",
            "permissions": ["android.permission.CAMERA", "android.permission.INTERNET"],
            "exported_components": ["activity:org.example.ShareActivity"],
            "native_libraries": ["lib/arm64-v8a/libdemo.so"],
        })
        ids = {item["id"] for item in result["indicators"]}
        self.assertIn("SENSITIVE_PERMISSION_DECLARATIONS", ids)
        self.assertIn("EXPORTED_COMPONENTS_PRESENT", ids)
        self.assertIn("NATIVE_LIBRARIES_PRESENT", ids)
        self.assertTrue(any("not automatically vulnerable" in x for x in result["limitations"]))

    def test_rejects_oversized_lists(self):
        with self.assertRaises(ValueError):
            analyze_apk_inventory({"permissions": ["p"] * 201})

    def test_rejects_invalid_item(self):
        with self.assertRaises(ValueError):
            analyze_apk_inventory({"permissions": ["   "]})

    def test_rejects_invalid_package_name(self):
        with self.assertRaises(ValueError):
            analyze_apk_inventory({"package_name": ""})


if __name__ == "__main__":
    unittest.main()
