"""Bounded, read-only APK inventory research helpers.

Consumes metadata already extracted by the Android client; never executes APKs,
decompiles instructions, or treats archive contents as trusted instructions.
"""
from __future__ import annotations

from typing import Any

MAX_ITEMS = 200
MAX_TEXT = 300


def _strings(values: Any, field: str) -> list[str]:
    if not isinstance(values, list) or len(values) > MAX_ITEMS:
        raise ValueError(f"{field} must be a list of at most {MAX_ITEMS} items.")
    cleaned: list[str] = []
    for value in values:
        if not isinstance(value, str) or not value.strip() or len(value) > MAX_TEXT:
            raise ValueError(f"{field} items must be non-empty strings up to {MAX_TEXT} characters.")
        cleaned.append(value.strip())
    return sorted(set(cleaned))


def analyze_apk_inventory(inventory: dict[str, Any]) -> dict[str, Any]:
    """Summarize supplied package metadata and flag review-worthy indicators."""
    if not isinstance(inventory, dict):
        raise ValueError("inventory must be an object.")
    package_name = inventory.get("package_name", "unknown")
    version_name = inventory.get("version_name", "unknown")
    for field, value in (("package_name", package_name), ("version_name", version_name)):
        if not isinstance(value, str) or not value.strip() or len(value) > MAX_TEXT:
            raise ValueError(f"{field} must be a non-empty string up to {MAX_TEXT} characters.")

    permissions = _strings(inventory.get("permissions", []), "permissions")
    components = _strings(inventory.get("exported_components", []), "exported_components")
    native_libs = _strings(inventory.get("native_libraries", []), "native_libraries")

    indicators: list[dict[str, str]] = []
    # These are review prompts, not vulnerability verdicts.
    if components:
        indicators.append({
            "id": "EXPORTED_COMPONENTS_PRESENT",
            "severity": "review",
            "detail": f"{len(components)} exported component entries were supplied; verify intended access controls.",
        })
    sensitive_markers = ("READ_SMS", "RECEIVE_SMS", "READ_CONTACTS", "RECORD_AUDIO",
                         "ACCESS_FINE_LOCATION", "CAMERA", "READ_CALL_LOG")
    matched = [p for p in permissions if any(marker in p for marker in sensitive_markers)]
    if matched:
        indicators.append({
            "id": "SENSITIVE_PERMISSION_DECLARATIONS",
            "severity": "review",
            "detail": f"{len(matched)} sensitive permission declaration(s) may warrant purpose review.",
        })
    if native_libs:
        indicators.append({
            "id": "NATIVE_LIBRARIES_PRESENT",
            "severity": "info",
            "detail": f"{len(native_libs)} native library path(s) were listed; static inventory alone does not establish behavior.",
        })

    return {
        "package_name": package_name.strip(),
        "version_name": version_name.strip(),
        "inventory_counts": {
            "permissions": len(permissions),
            "exported_components": len(components),
            "native_libraries": len(native_libs),
        },
        "indicators": indicators,
        "evidence": {
            "permissions": permissions,
            "exported_components": components,
            "native_libraries": native_libs,
        },
        "limitations": [
            "Analysis uses only metadata supplied by the client.",
            "A declared permission does not prove that the app uses it.",
            "An exported component is not automatically vulnerable.",
            "No APK code was executed or independently inspected by this endpoint.",
        ],
    }
