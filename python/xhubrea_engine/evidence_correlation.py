"""Deterministic evidence correlation from bounded, caller-supplied observations.

Correlations indicate shared identifiers only; they are not proof of causation,
validity, exploitability, or independent corroboration.
"""
from __future__ import annotations

import re
from typing import Any

MAX_EVIDENCE_ITEMS = 100
MAX_TEXT_CHARS = 500
MAX_KEY_CHARS = 160
CONFIDENCE_LEVELS = {"low", "medium", "high"}


def _text(value: Any, field: str, limit: int = MAX_TEXT_CHARS) -> str:
    if not isinstance(value, str) or not value.strip() or len(value) > limit:
        raise ValueError(f"{field} must be a non-empty string up to {limit} characters.")
    return value.strip()


def correlate_evidence(evidence: list[dict[str, Any]]) -> dict[str, Any]:
    """Group observations that share an explicit evidence_key."""
    if not isinstance(evidence, list) or not evidence or len(evidence) > MAX_EVIDENCE_ITEMS:
        raise ValueError(f"evidence must be a non-empty list of at most {MAX_EVIDENCE_ITEMS} items.")

    normalized: list[dict[str, str]] = []
    seen_ids: set[str] = set()
    groups: dict[str, list[dict[str, str]]] = {}

    for index, item in enumerate(evidence, start=1):
        if not isinstance(item, dict):
            raise ValueError(f"Evidence item {index} must be an object.")
        evidence_id = _text(item.get("id"), f"id for evidence item {index}", 80)
        if evidence_id in seen_ids:
            raise ValueError(f"Duplicate evidence id: {evidence_id}")
        seen_ids.add(evidence_id)

        source = _text(item.get("source"), f"source for {evidence_id}", 80)
        category = _text(item.get("category"), f"category for {evidence_id}", 80)
        subject = _text(item.get("subject"), f"subject for {evidence_id}", 160)
        observation = _text(item.get("observation"), f"observation for {evidence_id}")
        key = _text(item.get("evidence_key"), f"evidence_key for {evidence_id}", MAX_KEY_CHARS)
        confidence = item.get("confidence", "low")
        if confidence not in CONFIDENCE_LEVELS:
            raise ValueError(f"confidence for {evidence_id} must be low, medium, or high.")

        record = {
            "id": evidence_id,
            "source": source,
            "category": category,
            "subject": subject,
            "observation": observation,
            "evidence_key": key,
            "confidence": confidence,
        }
        normalized.append(record)
        groups.setdefault(re.sub(r"\\s+", " ", key).casefold(), []).append(record)

    correlations = []
    for key, items in sorted(groups.items()):
        distinct_sources = sorted({item["source"] for item in items})
        if len(items) < 2 or len(distinct_sources) < 2:
            continue
        correlations.append({
            "correlation_key": key,
            "evidence_ids": sorted(item["id"] for item in items),
            "sources": distinct_sources,
            "categories": sorted({item["category"] for item in items}),
            "subjects": sorted({item["subject"] for item in items}),
            "observations": sorted(
                [{"id": item["id"], "source": item["source"], "observation": item["observation"],
                  "confidence": item["confidence"]} for item in items],
                key=lambda row: (row["source"], row["id"]),
            ),
            "interpretation": "Shared evidence key across distinct sources; manual review is required.",
            "confidence": "not_assessed",
        })

    return {
        "summary": {
            "evidence_items": len(normalized),
            "distinct_sources": len({item["source"] for item in normalized}),
            "correlation_groups": len(correlations),
            "correlated_evidence_items": len({item_id for group in correlations for item_id in group["evidence_ids"]}),
        },
        "correlations": correlations,
        "unmatched_evidence_ids": sorted(
            item["id"] for item in normalized
            if not any(item["id"] in group["evidence_ids"] for group in correlations)
        ),
        "limitations": [
            "A shared evidence key is a grouping hint, not proof that observations are true or causally related.",
            "Confidence values are caller-supplied and are not combined or upgraded by this engine.",
            "Correlations are only as reliable as the supplied keys and source labels.",
            "No source code, APK, external service, or network resource is accessed.",
        ],
    }
