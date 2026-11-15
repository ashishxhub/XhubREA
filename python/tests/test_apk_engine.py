import json
import os
import tempfile
import unittest
import zipfile
from unittest.mock import patch

from xhubrea_engine import analyze_apk, analyze_apk_json


class ApkEngineTests(unittest.TestCase):
    def make_apk(self, entries):
        handle = tempfile.NamedTemporaryFile(suffix=".apk", delete=False)
        handle.close()
        with zipfile.ZipFile(handle.name, "w") as apk:
            for name, data in entries:
                apk.writestr(name, data)
        self.addCleanup(lambda: os.path.exists(handle.name) and os.unlink(handle.name))
        return handle.name

    def test_reads_basic_apk_structure(self):
        path = self.make_apk([
            ("AndroidManifest.xml", b"compiled-manifest"),
            ("classes.dex", b"dex-placeholder"),
            ("classes2.dex", b"dex-placeholder-2"),
            ("lib/arm64-v8a/libsample.so", b"native"),
            ("res/layout/main.xml", b"layout"),
            ("assets/data.bin", b"asset"),
        ])
        report = analyze_apk(path)
        self.assertTrue(report["zip_valid"])
        self.assertEqual(report["dex"]["count"], 2)
        self.assertEqual(report["native_libraries"]["count"], 1)
        self.assertEqual(report["resources"]["count"], 1)
        self.assertEqual(report["assets"]["count"], 1)
        self.assertEqual(len(report["sha256"]), 64)
        self.assertEqual(report["engine_version"], "0.3.0")

    def test_resource_intelligence_categorizes_paths_and_extensions(self):
        path = self.make_apk([
            ("res/layout/main.xml", b"layout-data"),
            ("res/layout-land/secondary.xml", b"landscape"),
            ("res/drawable/icon.webp", b"image"),
            ("res/mipmap-hdpi/app.png", b"icon"),
            ("res/values/strings.xml", b"values"),
            ("res/raw/sample.dat", b"raw"),
            ("assets/models/data.bin", b"asset-data"),
            ("classes.dex", b"not-a-resource"),
        ])
        report = analyze_apk(path)["resource_intelligence"]
        self.assertEqual(report["total_files"], 7)
        self.assertEqual(report["category_counts"]["layout_xml"], 2)
        self.assertEqual(report["category_counts"]["drawable"], 1)
        self.assertEqual(report["category_counts"]["mipmap"], 1)
        self.assertEqual(report["category_counts"]["values_xml"], 1)
        self.assertEqual(report["category_counts"]["raw"], 1)
        self.assertEqual(report["category_counts"]["assets"], 1)
        self.assertEqual(report["extension_counts"][".xml"], 3)
        self.assertEqual(report["mode"], "path_and_size_metadata_only")

    def test_resource_largest_files_are_sorted_and_capped(self):
        entries = [(f"assets/file-{index}.bin", b"x" * (index + 1)) for index in range(45)]
        path = self.make_apk(entries)
        report = analyze_apk(path)["resource_intelligence"]
        sizes = [item["size_bytes"] for item in report["largest_files"]]
        self.assertEqual(len(sizes), 30)
        self.assertEqual(sizes, sorted(sizes, reverse=True))
        self.assertEqual(sizes[0], 45)

    def test_resource_report_does_not_include_file_contents(self):
        secret_like_content = b"PRIVATE_SAMPLE_CONTENT_SHOULD_NOT_APPEAR"
        path = self.make_apk([("assets/config.bin", secret_like_content)])
        report_json = json.dumps(analyze_apk(path))
        self.assertNotIn(secret_like_content.decode(), report_json)
        self.assertIn("assets/config.bin", report_json)

    def test_rejects_non_zip(self):
        handle = tempfile.NamedTemporaryFile(suffix=".apk", delete=False)
        handle.write(b"not an apk")
        handle.close()
        self.addCleanup(lambda: os.path.exists(handle.name) and os.unlink(handle.name))
        with self.assertRaisesRegex(ValueError, "valid APK/ZIP"):
            analyze_apk(handle.name)

    def test_rejects_missing_file(self):
        with self.assertRaises(FileNotFoundError):
            analyze_apk("file-that-does-not-exist.apk")

    def test_rejects_empty_file(self):
        handle = tempfile.NamedTemporaryFile(suffix=".apk", delete=False)
        handle.close()
        self.addCleanup(lambda: os.path.exists(handle.name) and os.unlink(handle.name))
        with self.assertRaisesRegex(ValueError, "Empty APK"):
            analyze_apk(handle.name)

    def test_rejects_file_over_size_limit(self):
        path = self.make_apk([("AndroidManifest.xml", b"manifest")])
        with patch("xhubrea_engine.MAX_APK_BYTES", 1):
            with self.assertRaisesRegex(ValueError, "size allowed limit"):
                analyze_apk(path)

    def test_rejects_too_many_entries(self):
        path = self.make_apk([("one", b"1"), ("two", b"2")])
        with patch("xhubrea_engine.MAX_ENTRIES", 1):
            with self.assertRaisesRegex(ValueError, "bahut zyada entries"):
                analyze_apk(path)

    def test_rejects_declared_uncompressed_size_over_limit(self):
        path = self.make_apk([("payload", b"123456")])
        with patch("xhubrea_engine.MAX_TOTAL_UNCOMPRESSED", 5):
            with self.assertRaisesRegex(ValueError, "uncompressed data limit"):
                analyze_apk(path)

    def test_reports_suspicious_paths_without_extracting(self):
        path = self.make_apk([
            ("../outside.txt", b"data"),
            ("AndroidManifest.xml", b"manifest"),
        ])
        report = analyze_apk(path)
        self.assertIn("../outside.txt", report["path_safety_warnings"])

    def test_reports_duplicate_normalized_archive_entries(self):
        path = self.make_apk([
            ("assets/config.json", b"first"),
            ("assets\\config.json", b"second"),
        ])
        report = analyze_apk(path)
        self.assertEqual(report["duplicate_entry_warnings"], ["assets\\config.json"])

    def test_detects_backslash_traversal_paths(self):
        path = self.make_apk([
            ("..\\\\outside.txt", b"data"),
            ("AndroidManifest.xml", b"manifest"),
        ])
        report = analyze_apk(path)
        self.assertIn("..\\\\outside.txt", report["path_safety_warnings"])

    def test_only_counts_valid_dex_filename_patterns(self):
        path = self.make_apk([
            ("classes.dex", b"one"),
            ("classes2.dex", b"two"),
            ("classes-not-real.dex", b"no"),
            ("classesX.dex", b"no"),
        ])
        report = analyze_apk(path)
        self.assertEqual(report["dex"]["files"], ["classes.dex", "classes2.dex"])

    def test_json_bridge_returns_valid_json(self):
        path = self.make_apk([("AndroidManifest.xml", b"manifest")])
        report = json.loads(analyze_apk_json(path))
        self.assertTrue(report["manifest"]["present"])
        self.assertEqual(report["mode"], "read_only")
        self.assertIn("resource_intelligence", report)


if __name__ == "__main__":
    unittest.main()
