import os
import tempfile
import unittest
import zipfile

from xhubrea_engine import analyze_apk


class ApkEngineTests(unittest.TestCase):
    def make_apk(self, entries):
        handle = tempfile.NamedTemporaryFile(suffix=".apk", delete=False)
        handle.close()
        with zipfile.ZipFile(handle.name, "w") as apk:
            for name, data in entries.items():
                apk.writestr(name, data)
        self.addCleanup(lambda: os.path.exists(handle.name) and os.unlink(handle.name))
        return handle.name

    def test_reads_basic_apk_structure(self):
        path = self.make_apk({
            "AndroidManifest.xml": b"compiled-manifest",
            "classes.dex": b"dex-placeholder",
            "classes2.dex": b"dex-placeholder-2",
            "lib/arm64-v8a/libsample.so": b"native",
            "res/layout/main.xml": b"layout",
            "assets/data.bin": b"asset",
        })
        report = analyze_apk(path)
        self.assertTrue(report["zip_valid"])
        self.assertEqual(report["dex"]["count"], 2)
        self.assertEqual(report["native_libraries"]["count"], 1)
        self.assertEqual(report["resources"]["count"], 1)
        self.assertEqual(report["assets"]["count"], 1)
        self.assertEqual(len(report["sha256"]), 64)

    def test_rejects_non_zip(self):
        handle = tempfile.NamedTemporaryFile(suffix=".apk", delete=False)
        handle.write(b"not an apk")
        handle.close()
        self.addCleanup(lambda: os.path.exists(handle.name) and os.unlink(handle.name))
        with self.assertRaises(ValueError):
            analyze_apk(handle.name)

    def test_rejects_missing_file(self):
        with self.assertRaises(FileNotFoundError):
            analyze_apk("file-that-does-not-exist.apk")


if __name__ == "__main__":
    unittest.main()
