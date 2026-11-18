from __future__ import annotations

from copy import deepcopy
from pathlib import Path

import pytest
import yaml

from xhubrea_engine.agent_registry import (
    RegistryValidationError,
    load_registry,
    validate_registry,
)


ROOT = Path(__file__).resolve().parents[2]
REGISTRY_PATH = ROOT / "config" / "agent_registry.yaml"


def valid_registry() -> dict:
    return yaml.safe_load(REGISTRY_PATH.read_text(encoding="utf-8"))


def test_repository_registry_loads_and_has_51_roles() -> None:
    registry = load_registry()
    assert registry["total_roles"] == 51
    assert sum(len(dept["roles"]) for dept in registry["departments"]) == 51
    assert len({role["id"] for dept in registry["departments"] for role in dept["roles"]}) == 51


def test_rejects_duplicate_role_id() -> None:
    data = valid_registry()
    data["departments"][1]["roles"][0]["id"] = data["departments"][0]["roles"][0]["id"]
    with pytest.raises(RegistryValidationError, match="duplicate role id"):
        validate_registry(data)


def test_rejects_wrong_total() -> None:
    data = valid_registry()
    data["total_roles"] = 50
    with pytest.raises(RegistryValidationError, match="total_roles"):
        validate_registry(data)


def test_rejects_unknown_manager() -> None:
    data = valid_registry()
    data["departments"][0]["roles"][0]["reports_to"] = "missing_manager"
    with pytest.raises(RegistryValidationError, match="unknown role"):
        validate_registry(data)


def test_rejects_missing_required_role_field() -> None:
    data = valid_registry()
    del data["departments"][0]["roles"][0]["purpose"]
    with pytest.raises(RegistryValidationError, match="purpose"):
        validate_registry(data)


def test_rejects_duplicate_department_id() -> None:
    data = valid_registry()
    data["departments"][1]["id"] = data["departments"][0]["id"]
    with pytest.raises(RegistryValidationError, match="duplicate department"):
        validate_registry(data)


def test_rejects_lead_from_another_department() -> None:
    data = valid_registry()
    data["departments"][0]["lead"] = data["departments"][1]["lead"]
    with pytest.raises(RegistryValidationError, match="must belong"):
        validate_registry(data)


def test_rejects_reporting_cycle() -> None:
    data = valid_registry()
    data["departments"][0]["roles"][0]["reports_to"] = "project_manager"
    data["departments"][0]["roles"][1]["reports_to"] = "chief_ai_coordinator"
    with pytest.raises(RegistryValidationError, match="cycle"):
        validate_registry(data)


def test_rejects_malformed_yaml(tmp_path: Path) -> None:
    path = tmp_path / "broken.yaml"
    path.write_text("departments: [", encoding="utf-8")
    with pytest.raises(RegistryValidationError, match="YAML parse error"):
        load_registry(path)


def test_missing_file_is_reported(tmp_path: Path) -> None:
    with pytest.raises(FileNotFoundError):
        load_registry(tmp_path / "missing.yaml")
