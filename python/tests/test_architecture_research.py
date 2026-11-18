import unittest

from xhubrea_engine.architecture_research import analyze_app_architecture


class ArchitectureResearchTests(unittest.TestCase):
    def test_maps_components_and_internal_python_imports(self):
        report = analyze_app_architecture([
            {"path": "app/main.py", "language": "python", "imports": ["app.services.auth", "fastapi"]},
            {"path": "app/services/auth.py", "language": "python", "imports": ["os"]},
            {"path": "web/src/App.kt", "language": "kotlin", "imports": []},
        ])
        self.assertEqual(report["summary"]["files_mapped"], 3)
        self.assertEqual(report["summary"]["components_detected"], 2)
        self.assertEqual(report["dependency_edges"], [
            {"from": "app/main.py", "to": "app/services/auth.py", "import": "app.services.auth"}
        ])
        self.assertEqual(report["external_import_roots"], ["fastapi", "os"])

    def test_resolves_python_package_init(self):
        report = analyze_app_architecture([
            {"path": "pkg/__init__.py", "language": "python", "imports": []},
            {"path": "consumer.py", "language": "python", "imports": ["pkg"]},
        ])
        self.assertEqual(report["dependency_edges"][0]["to"], "pkg/__init__.py")

    def test_relative_import_is_reported_not_guessed(self):
        report = analyze_app_architecture([
            {"path": "pkg/a.py", "language": "python", "imports": [".b"]},
        ])
        self.assertEqual(report["summary"]["unresolved_relative_imports"], 1)
        self.assertEqual(report["unresolved_relative_imports"][0]["import"], ".b")

    def test_rejects_unsafe_or_non_normalized_paths(self):
        for path in ("../secret.py", "/tmp/a.py", "a//b.py", "C:/a.py"):
            with self.subTest(path=path), self.assertRaises(ValueError):
                analyze_app_architecture([{"path": path, "imports": []}])

    def test_rejects_duplicate_paths(self):
        with self.assertRaisesRegex(ValueError, "Duplicate"):
            analyze_app_architecture([
                {"path": "a.py", "imports": []},
                {"path": "a.py", "imports": []},
            ])

    def test_enforces_file_limit(self):
        with self.assertRaisesRegex(ValueError, "at most 200"):
            analyze_app_architecture([{"path": f"f{i}.py"} for i in range(201)])

    def test_enforces_import_limit_and_shape(self):
        with self.assertRaises(ValueError):
            analyze_app_architecture([{"path": "a.py", "imports": ["x"] * 101}])
        with self.assertRaises(ValueError):
            analyze_app_architecture([{"path": "a.py", "imports": "os"}])

    def test_rejects_invalid_language_metadata(self):
        with self.assertRaises(ValueError):
            analyze_app_architecture([{"path": "a.py", "language": "", "imports": []}])

    def test_deterministic_ordering_and_no_runtime_claims(self):
        files = [
            {"path": "z/b.py", "language": "python", "imports": []},
            {"path": "a.py", "language": "python", "imports": []},
        ]
        first = analyze_app_architecture(files)
        second = analyze_app_architecture(list(reversed(files)))
        self.assertEqual(first, second)
        self.assertTrue(any("not verified runtime services" in item for item in first["limitations"]))


if __name__ == "__main__":
    unittest.main()
