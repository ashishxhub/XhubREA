"""XhubREA read-only APK inspection engine.

This module inspects APK container metadata only. It never installs, launches,
modifies, decrypts, or executes APK contents.
"""
from __future__ import annotations

import hashlib
import json
import os
import zipfile
from collections import Counter
from pathlib import PurePosixPath

MAX_APK_BYTES = 512 * 1024 * 1024
MAX_ENTRIES = 100_000
MAX_TOTAL_UNCOMPRESSED = 2 * 1024 * 1024 * 1024


def _sha256_file(path: str) -> str:
    digest = hashlib.sha256()
    with open(path, "rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def analyze_apk(path: str) -> dict:
    """Return a bounded, read-only structural report for an APK file."""
    if not os.path.isfile(path):
        raise FileNotFoundError("APK file nahi mila.")
    size = os.path.getsize(path)
    if size <= 0 or size > MAX_APK_BYTES:
        raise ValueError("APK size allowed limit ke bahar hai.")

    try:
        with zipfile.ZipFile(path, "r") as apk:
            infos = apk.infolist()
            if len(infos) > MAX_ENTRIES:
                raise ValueError("APK mein bahut zyada entries hain.")
            total_uncompressed = sum(max(0, item.file_size) for item in infos)
            if total_uncompressed > MAX_TOTAL_UNCOMPRESSED:
                raise ValueError("APK ka uncompressed data limit se zyada hai.")
            names = [item.filename for item in infos]
            counts = Counter()
            dex_files = []
            native_libs = []
            resource_files = 0
            asset_files = 0
            suspicious_names = []

            for name in names:
                normalized = name.replace("\\", "/")
                parts = PurePosixPath(normalized).parts
                if normalized.startswith("/") or ".." in parts:
                    suspicious_names.append(name[:240])
                if normalized.startswith("classes") and normalized.endswith(".dex"):
                    dex_files.append(normalized)
                if normalized.startswith("lib/") and normalized.endswith(".so"):
                    native_libs.append(normalized)
                if normalized.startswith("res/"):
                    resource_files += 1
                if normalized.startswith("assets/"):
                    asset_files += 1
                if normalized:
                    counts[normalized.split("/", 1)[0]] += 1

            manifest_present = "AndroidManifest.xml" in names
            manifest_kind = "binary_or_compiled" if manifest_present else "missing"
            return {
                "engine": "XhubREA Python APK Engine",
                "engine_version": "0.1.0",
                "mode": "read_only",
                "file_name": os.path.basename(path),
                "file_size_bytes": size,
                "sha256": _sha256_file(path),
                "zip_valid": True,
                "entry_count": len(infos),
                "uncompressed_size_bytes": total_uncompressed,
                "manifest": {
                    "present": manifest_present,
                    "format_hint": manifest_kind,
                    "explanation_hi": (
                        "AndroidManifest.xml mila. Iska detailed decode alag parser karega."
                        if manifest_present else
                        "Manifest file nahi mili; APK incomplete ya invalid ho sakta hai."
                    ),
                },
                "dex": {"count": len(dex_files), "files": sorted(dex_files)},
                "native_libraries": {
                    "count": len(native_libs), "files": sorted(native_libs)[:500]
                },
                "resources": {"count": resource_files},
                "assets": {"count": asset_files},
                "top_level_entry_counts": dict(sorted(counts.items())),
                "path_safety_warnings": suspicious_names[:100],
                "notes_hi": [
                    "Yeh report sirf APK ki ZIP structure ko padhti hai.",
                    "APK install ya execute nahi hota.",
                    "DEX files ka code decompile ya run nahi kiya jata.",
                    "Manifest format hint hai; binary XML ka full decode alag module karta hai.",
                ],
            }
    except zipfile.BadZipFile as exc:
        raise ValueError("Yeh valid APK/ZIP container nahi hai.") from exc


def analyze_apk_json(path: str) -> str:
    """Convenience JSON serializer for Kotlin/Python bridge integration."""
    return json.dumps(analyze_apk(path), ensure_ascii=False, indent=2)
