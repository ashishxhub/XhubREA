"""Deterministic structuring of operator-supplied research evidence.

Confidence is an operator label, not an independently verified probability.
This module never infers app behavior from metadata alone.
"""
from __future__ import annotations

from collections import defaultdict
from typing import Any

ALLOWED_CONFIDENCE = {"high", "medium", "low", "unverified"}
MAX_FINDINGS = 12


def structure_findings(findings: list[dict[str, Any]]) -> dict[str, Any]:
    if not isinstance(findings, list) or not findings:
        raise ValueError("At least one finding is required.")
    if len(findings) > MAX_FINDINGS:
        raise ValueError(f"At most {MAX_FINDINGS} findings are allowed.")

    evidence = []
    source_groups: dict[str, list[str]] = defaultdict(list)
    seen_ids: set[str] = set()

    for index, item in enumerate(findings, start=1):
        if not isinstance(item, dict):
            raise ValueError(f"Finding {index} must be an object.")
        fields = ("evidence_id", "source", "observation", "confidence")
        values = {}
        for field in fields:
            value = item.get(field)
            if not isinstance(value, str) or not value.strip():
                raise ValueError(f"finding_{index}.{field} must be non-empty.")
            value = value.strip()
            if len(value) > 500:
                raise ValueError(f"finding_{index}.{field} exceeds 500 characters.")
            values[field] = value

        evidence_id = values["evidence_id"]
        if evidence_id in seen_ids:
            raise ValueError(f"Duplicate evidence_id: {evidence_id}")
        seen_ids.add(evidence_id)

        confidence = values["confidence"].lower()
        if confidence not in ALLOWED_CONFIDENCE:
            raise ValueError("confidence must be high, medium, low, or unverified.")

        evidence.append({
            "evidence_id": evidence_id,
            "source": values["source"],
            "observation": values["observation"],
            "confidence": confidence,
            "classification": "operator_supplied_observation",
            "verification_status": "not_independently_verified",
        })
        source_groups[values["source"]].append(evidence_id)

    correlations = [
        {"shared_source": source, "evidence_ids": ids,
         "relationship": "same_source_only_not_proof_of_causation"}
        for source, ids in sorted(source_groups.items()) if len(ids) > 1
    ]

    return {
        "schema_version": "1.0",
        "evidence_count": len(evidence),
        "evidence": evidence,
        "source_correlations": correlations,
        "hypotheses": [],
        "limitations": [
            "Confidence values are supplied by the operator, not calculated probabilities.",
            "Shared source indicates grouping only; it does not establish causation.",
            "No application behavior or architecture is independently verified here.",
        ],
    }
