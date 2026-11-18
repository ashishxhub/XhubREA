"""Tests for safe, bounded source-code analysis."""
import unittest

from xhubrea_engine.source_analysis import analyze_source_inventory


class SourceAnalysisTests(unittest.TestCase):
    def test_summarizes_python_imports_and_symbols_without_execution(self):
        result = analyze_source_inventory([{
            "path": "src/example.py",
            "content": "import json\nfrom pathlib import Path\n\nclass Demo:\n    def run(self):\n        return 1\n",
        }])
        file = result["files"][0]
        self.assertEqual(file["parse_status"], "ok")
        self.assertIn("json", file["imports"])
        self.assertIn("pathlib", file["imports"])
        self.assertEqual([s["name"] for s in file["symbols"]], ["Demo", "run"])
        self.assertEqual(result["summary"]["python_syntax_errors"], 0)

    def test_reports_syntax_error_without_execution(self):
        result = analyze_source_inventory([{
            "path": "broken.py",
            "content": "def broken(:\n    pass",
        }])
        self.assertEqual(result["files"][0]["parse_status"], "syntax_error")
        self.assertEqual(result["summary"]["python_syntax_errors"], 1)

    def test_non_python_files_are_inventory_only(self):
        result = analyze_source_inventory([{"path": "App.kt", "content": "class App"}])
        self.assertEqual(result["files"][0]["language"], "kt")
        self.assertEqual(result["files"][0]["parse_status"], "inventory_only")

    def test_rejects_path_traversal(self):
        with self.assertRaises(ValueError):
            analyze_source_inventory([{"path": "../secret.py", "content": "pass"}])

    def test_rejects_duplicate_paths(self):
        with self.assertRaises(ValueError):
            analyze_source_inventory([
                {"path": "same.py", "content": "pass"},
                {"path": "same.py", "content": "pass"},
            ])

    def test_rejects_oversized_file(self):
        with self.assertRaises(ValueError):
            analyze_source_inventory([{"path": "large.py", "content": "x" * 64_001}])

    def test_rejects_too_many_files(self):
        with self.assertRaises(ValueError):
            analyze_source_inventory([{"path": f"{i}.txt", "content": ""} for i in range(41)])

    def test_limits_combined_source_size(self):
        with self.assertRaises(ValueError):
            analyze_source_inventory([
                {"path": f"{i}.txt", "content": "x" * 64_000} for i in range(9)
            ])


if __name__ == "__main__":
    unittest.main()
