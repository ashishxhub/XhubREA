"""Deterministic comparison of two caller-supplied application research snapshots.

This module compares normalized inventory labels only. It does not inspect,
execute, identify, or make security judgments about either application.
"""
from __future__ import annotations

from typing import Any

MAX_ITEMS_PER_CATEGORY = 200
MAX_TEXT_CHARS = 300
CATEGORIES = ("permissions", "components", "source_structure", "dependencies", "architecture")


def _label(value: Any, field: str) -> str:
    if not isinstance(value, str) or not value.strip() or len(value) > MAX_TEXT_CHARS:
        raise ValueError(f"{field} entries must be non-empty strings up to {MAX_TEXT_CHARS} characters.")
    return value.strip()


def _snapshot(value: Any, side: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise ValueError(f"{side} must be an object.")
    app_label = _label(value.get("app_label", side), f"{side}.app_label")
    normalized: dict[str, Any] = {"app_label": app_label}
    for category in CATEGORIES:
        entries = value.get(category, [])
        if not isinstance(entries, list) or len(entries) > MAX_ITEMS_PER_CATEGORY:
            raise ValueError(
                f"{side}.{category} must be a list of at most {MAX_ITEMS_PER_CATEGORY} entries."
            )
        normalized[category] = sorted({_label(item, f"{side}.{category}") for item in entries})
    return normalized


def compare_app_snapshots(app_a: dict[str, Any], app_b: dict[str, Any]) -> dict[str, Any]:
    """Compare two bounded snapshots and report set differences per category."""
    left = _snapshot(app_a, "app_a")
    right = _snapshot(app_b, "app_b")
    comparisons: dict[str, Any] = {}
    for category in CATEGORIES:
        a_values = set(left[category])
        b_values = set(right[category])
        shared = sorted(a_values & b_values)
        only_a = sorted(a_values - b_values)
        only_b = sorted(b_values - a_values)
        comparisons[category] = {
            "app_a_count": len(a_values),
            "app_b_count": len(b_values),
            "shared_count": len(shared),
            "only_in_app_a": only_a,
            "only_in_app_b": only_b,
            "shared": shared,
        }

    return {
        "app_a_label": left["app_label"],
        "app_b_label": right["app_label"],
        "comparisons": comparisons,
        "limitations": [
            "Results compare only the caller-supplied labels; completeness and accuracy are not independently verified.",
            "A difference or overlap does not establish behavior, risk, vulnerability, ownership, or app identity.",
            "Source structure and architecture are compared as supplied labels, not semantically analyzed.",
            "No application code, APK, filesystem, external service, or network resource is accessed.",
        ],
    }
