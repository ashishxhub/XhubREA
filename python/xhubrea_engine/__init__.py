"""XhubREA read-only APK inspection engine.

This module inspects APK container metadata only. It never installs, launches,
modifies, decrypts, or executes APK contents.
"""
from __future__ import annotations

import hashlib
import json
import os
import re
import stat
import zipfile
from collections import Counter
from pathlib import PurePosixPath

MAX_APK_BYTES = 512 * 1024 * 1024
MAX_ENTRIES = 100_000
MAX_TOTAL_UNCOMPRESSED = 2 * 1024 * 1024 * 1024
DEX_NAME = re.compile(r"classes(?:[2-9][0-9]*)?\.dex\Z|classes\.dex\Z")
MAX_RESOURCE_REPORT_ITEMS = 30


def _sha256_file(path: str) -> str:
    digest = hashlib.sha256()
    with open(path, "rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _resource_category(name: str) -> str | None:
    """Classify a resource path using its archive location, without reading content."""
    normalized = name.replace("\\", "/")
    if normalized.startswith("assets/"):
        return "assets"
    if not normalized.startswith("res/"):
        return None

    parts = PurePosixPath(normalized).parts
    folder = parts[1].split("-", 1)[0].lower() if len(parts) > 1 else ""
    known = {
        "layout": "layout_xml",
        "drawable": "drawable",
        "mipmap": "mipmap",
        "values": "values_xml",
        "xml": "xml",
        "raw": "raw",
        "anim": "animation",
        "animator": "animator",
        "color": "color",
        "font": "font",
        "menu": "menu",
        "navigation": "navigation",
        "transition": "transition",
        "interpolator": "interpolator",
    }
    return known.get(folder, "other_res")


def _resource_inventory(infos: list[zipfile.ZipInfo]) -> dict:
    """Return compact, bounded resource metadata; never extracts or decodes files."""
    category_counts: Counter[str] = Counter()
    extension_counts: Counter[str] = Counter()
    resource_items: list[dict] = []

    for item in infos:
        if item.is_dir():
            continue
        category = _resource_category(item.filename)
        if category is None:
            continue

        category_counts[category] += 1
        suffix = PurePosixPath(item.filename.replace("\\", "/")).suffix.lower()
        extension_counts[suffix if suffix else "[no_extension]"] += 1
        resource_items.append({
            "path": item.filename[:240],
            "category": category,
            "size_bytes": max(0, item.file_size),
            "compressed_size_bytes": max(0, item.compress_size),
        })

    largest = sorted(
        resource_items,
        key=lambda row: row["size_bytes"],
        reverse=True,
    )[:MAX_RESOURCE_REPORT_ITEMS]

    return {
        "total_files": len(resource_items),
        "category_counts": dict(sorted(category_counts.items())),
        "extension_counts": dict(
            sorted(extension_counts.items(), key=lambda pair: (-pair[1], pair[0]))[:20]
        ),
        "largest_files": largest,
        "report_item_limit": MAX_RESOURCE_REPORT_ITEMS,
        "mode": "path_and_size_metadata_only",
        "notes": [
            "Classification is based on archive path and filename extension only.",
            "Resource file contents are not extracted, decoded, or executed.",
            "Android resource IDs and compiled resources.arsc values are not decoded by this module.",
        ],
    }


def analyze_apk(path: str) -> dict:
    """Return a bounded, read-only structural report for an APK file."""
    if not os.path.isfile(path):
        raise FileNotFoundError("APK file nahi mila.")
    size = os.path.getsize(path)
    if size <= 0:
        raise ValueError("Empty APK file analyze nahi ho sakti.")
    if size > MAX_APK_BYTES:
        raise ValueError("APK size allowed limit ke bahar hai.")

    try:
        with zipfile.ZipFile(path, "r") as apk:
            infos = apk.infolist()
            if len(infos) > MAX_ENTRIES:
                raise ValueError("APK mein bahut zyada entries hain.")
            total_uncompressed = sum(max(0, item.file_size) for item in infos)
            if total_uncompressed > MAX_TOTAL_UNCOMPRESSED:
                raise ValueError("APK ka uncompressed data limit se zyada hai.")

            counts = Counter()
            dex_files = []
            native_libs = []
            resource_files = 0
            asset_files = 0
            suspicious_names = []
            duplicate_names = []
            symlink_names = []
            encrypted_names = []
            unsupported_compression = []
            seen_names = set()
            names = []

            for item in infos:
                name = item.filename
                names.append(name)
                normalized = name.replace("\\", "/")
                if normalized in seen_names:
                    duplicate_names.append(name[:240])
                else:
                    seen_names.add(normalized)
                parts = PurePosixPath(normalized).parts
                drive_like = bool(re.match(r"^[A-Za-z]:", normalized))
                has_null = "\\x00" in name
                if normalized.startswith("/") or normalized.startswith("//") or drive_like or ".." in parts or has_null:
                    suspicious_names.append(name[:240])
                unix_mode = item.external_attr >> 16
                if stat.S_ISLNK(unix_mode):
                    symlink_names.append(name[:240])
                if item.flag_bits & 0x1:
                    encrypted_names.append(name[:240])
                if item.compress_type not in (zipfile.ZIP_STORED, zipfile.ZIP_DEFLATED, zipfile.ZIP_BZIP2, zipfile.ZIP_LZMA):
                    unsupported_compression.append(name[:240])
                if DEX_NAME.fullmatch(normalized):
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
            return {
                "engine": "XhubREA Python APK Engine",
                "engine_version": "0.3.0",
                "mode": "read_only",
                "file_name": os.path.basename(path),
                "file_size_bytes": size,
                "sha256": _sha256_file(path),
                "zip_valid": True,
                "entry_count": len(infos),
                "uncompressed_size_bytes": total_uncompressed,
                "manifest": {
                    "present": manifest_present,
                    "format_hint": "binary_or_compiled" if manifest_present else "missing",
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
                "resource_intelligence": _resource_inventory(infos),
                "top_level_entry_counts": dict(sorted(counts.items())),
                "path_safety_warnings": suspicious_names[:100],
                "duplicate_entry_warnings": duplicate_names[:100],
                "symlink_entry_warnings": symlink_names[:100],
                "encrypted_entry_warnings": encrypted_names[:100],
                "unsupported_compression_warnings": unsupported_compression[:100],
                "notes_hi": [
                    "Yeh report sirf APK ki ZIP structure ko padhti hai.",
                    "APK install ya execute nahi hota.",
                    "DEX files ka code decompile ya run nahi kiya jata.",
                    "Manifest format hint hai; binary XML ka full decode alag module karta hai.",
                    "Path warnings suspicious archive names dikhati hain; files extract nahi ki jaati.",
                    "Duplicate entry warnings ek hi normalized path ke multiple records dikhati hain.",
                    "Symlink, encrypted aur unsupported-compression entries sirf metadata se flag hoti hain; unhe read ya extract nahi kiya jata.",
                    "Resource intelligence archive paths aur declared sizes par based hai; content decode nahi karta.",
                ],
            }
    except zipfile.BadZipFile as exc:
        raise ValueError("Yeh valid APK/ZIP container nahi hai.") from exc


def analyze_apk_json(path: str) -> str:
    """Convenience JSON serializer for Kotlin/Python bridge integration."""
    return json.dumps(analyze_apk(path), ensure_ascii=False, indent=2)
