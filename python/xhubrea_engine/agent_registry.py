"""Load and validate XhubREA's provider-neutral agent role registry.

The registry defines role templates only. This module does not create agents,
dispatch tasks, access credentials, or call external providers.
"""
from __future__ import annotations

from pathlib import Path
from typing import Any

import yaml


class RegistryValidationError(ValueError):
    """Raised when the agent registry does not satisfy its schema/invariants."""

    def __init__(self, errors: list[str]) -> None:
        self.errors = errors
        super().__init__("Invalid XhubREA agent registry: " + "; ".join(errors))


def validate_registry(data: Any) -> dict[str, Any]:
    """Validate a parsed registry and return it unchanged when valid."""
    errors: list[str] = []
    if not isinstance(data, dict):
        raise RegistryValidationError(["root must be a YAML mapping"])

    if data.get("version") != 1:
        errors.append("version must be 1")
    if data.get("organization") != "XhubREA":
        errors.append("organization must be 'XhubREA'")
    if data.get("default_mode") not in {"on_demand"}:
        errors.append("default_mode must be 'on_demand'")
    if data.get("default_permission") not in {"read_only"}:
        errors.append("default_permission must be 'read_only'")

    approvals = data.get("owner_approval_required")
    if not isinstance(approvals, list) or not approvals or not all(
        isinstance(item, str) and item.strip() for item in approvals
    ):
        errors.append("owner_approval_required must be a non-empty list of strings")

    departments = data.get("departments")
    if not isinstance(departments, list) or not departments:
        errors.append("departments must be a non-empty list")
        departments = []

    department_ids: set[str] = set()
    role_ids: set[str] = set()
    role_department: dict[str, str] = {}
    reporting: dict[str, str] = {}
    leads: list[tuple[str, str]] = []
    role_count = 0

    for index, department in enumerate(departments):
        label = f"departments[{index}]"
        if not isinstance(department, dict):
            errors.append(f"{label} must be a mapping")
            continue
        dept_id = department.get("id")
        if not isinstance(dept_id, str) or not dept_id.strip():
            errors.append(f"{label}.id must be a non-empty string")
            dept_id = f"<invalid-{index}>"
        elif dept_id in department_ids:
            errors.append(f"duplicate department id: {dept_id}")
        department_ids.add(dept_id)

        if not isinstance(department.get("name"), str) or not department["name"].strip():
            errors.append(f"{label}.name must be a non-empty string")

        roles = department.get("roles")
        if not isinstance(roles, list) or not roles:
            errors.append(f"{label}.roles must be a non-empty list")
            continue

        department_role_ids: set[str] = set()
        for role_index, role in enumerate(roles):
            role_count += 1
            role_label = f"{label}.roles[{role_index}]"
            if not isinstance(role, dict):
                errors.append(f"{role_label} must be a mapping")
                continue
            role_id = role.get("id")
            if not isinstance(role_id, str) or not role_id.strip():
                errors.append(f"{role_label}.id must be a non-empty string")
                continue
            if role_id in role_ids:
                errors.append(f"duplicate role id: {role_id}")
            role_ids.add(role_id)
            department_role_ids.add(role_id)
            role_department[role_id] = dept_id

            for field in ("title", "purpose"):
                if not isinstance(role.get(field), str) or not role[field].strip():
                    errors.append(f"{role_label}.{field} must be a non-empty string")
            manager = role.get("reports_to")
            if not isinstance(manager, str) or not manager.strip():
                errors.append(f"{role_label}.reports_to must be a non-empty string")
            else:
                reporting[role_id] = manager

        lead = department.get("lead")
        if not isinstance(lead, str) or not lead.strip():
            errors.append(f"{label}.lead must be a non-empty role id")
        else:
            leads.append((dept_id, lead))
            if lead not in department_role_ids:
                errors.append(f"{label}.lead '{lead}' must belong to that department")

    expected_count = data.get("total_roles")
    if not isinstance(expected_count, int) or isinstance(expected_count, bool):
        errors.append("total_roles must be an integer")
    elif expected_count != role_count:
        errors.append(f"total_roles is {expected_count}, but found {role_count} roles")

    for role_id, manager in reporting.items():
        if manager != "owner" and manager not in role_ids:
            errors.append(f"role '{role_id}' reports to unknown role '{manager}'")
        if manager == role_id:
            errors.append(f"role '{role_id}' cannot report to itself")

    # Every reporting chain must terminate at the owner; cycles are rejected.
    for start in reporting:
        visited: set[str] = set()
        current = start
        while current in reporting:
            if current in visited:
                errors.append(f"reporting cycle detected from role '{start}'")
                break
            visited.add(current)
            current = reporting[current]
            if current == "owner":
                break

    if errors:
        raise RegistryValidationError(errors)
    return data


def load_registry(path: str | Path | None = None) -> dict[str, Any]:
    """Load the default repository registry or an explicitly supplied YAML file."""
    registry_path = (
        Path(path) if path is not None
        else Path(__file__).resolve().parents[2] / "config" / "agent_registry.yaml"
    )
    try:
        with registry_path.open("r", encoding="utf-8") as stream:
            parsed = yaml.safe_load(stream)
    except FileNotFoundError:
        raise
    except yaml.YAMLError as exc:
        raise RegistryValidationError([f"YAML parse error: {exc}"]) from exc
    return validate_registry(parsed)
