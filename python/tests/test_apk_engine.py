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
        self.assertEqual(report["engine_version"], "0.2.0")

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
            ("assets\\\\config.json", b"second"),
        ])
        report = analyze_apk(path)
        self.assertEqual(report["duplicate_entry_warnings"], ["assets\\\\config.json"])

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


if __name__ == "__main__":
    unittest.main()
