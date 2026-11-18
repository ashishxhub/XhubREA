from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

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


class AgentRegistryTests(unittest.TestCase):
    def test_repository_registry_loads_and_has_51_roles(self) -> None:
        registry = load_registry()
        self.assertEqual(registry["total_roles"], 51)
        self.assertEqual(sum(len(dept["roles"]) for dept in registry["departments"]), 51)
        role_ids = [role["id"] for dept in registry["departments"] for role in dept["roles"]]
        self.assertEqual(len(set(role_ids)), 51)

    def test_rejects_duplicate_role_id(self) -> None:
        data = valid_registry()
        data["departments"][1]["roles"][0]["id"] = data["departments"][0]["roles"][0]["id"]
        with self.assertRaisesRegex(RegistryValidationError, "duplicate role id"):
            validate_registry(data)

    def test_rejects_wrong_total(self) -> None:
        data = valid_registry()
        data["total_roles"] = 50
        with self.assertRaisesRegex(RegistryValidationError, "total_roles"):
            validate_registry(data)

    def test_rejects_unknown_manager(self) -> None:
        data = valid_registry()
        data["departments"][0]["roles"][0]["reports_to"] = "missing_manager"
        with self.assertRaisesRegex(RegistryValidationError, "unknown role"):
            validate_registry(data)

    def test_rejects_missing_required_role_field(self) -> None:
        data = valid_registry()
        del data["departments"][0]["roles"][0]["purpose"]
        with self.assertRaisesRegex(RegistryValidationError, "purpose"):
            validate_registry(data)

    def test_rejects_duplicate_department_id(self) -> None:
        data = valid_registry()
        data["departments"][1]["id"] = data["departments"][0]["id"]
        with self.assertRaisesRegex(RegistryValidationError, "duplicate department"):
            validate_registry(data)

    def test_rejects_lead_from_another_department(self) -> None:
        data = valid_registry()
        data["departments"][0]["lead"] = data["departments"][1]["lead"]
        with self.assertRaisesRegex(RegistryValidationError, "must belong"):
            validate_registry(data)

    def test_rejects_reporting_cycle(self) -> None:
        data = valid_registry()
        data["departments"][0]["roles"][0]["reports_to"] = "project_manager"
        data["departments"][0]["roles"][1]["reports_to"] = "chief_ai_coordinator"
        with self.assertRaisesRegex(RegistryValidationError, "cycle"):
            validate_registry(data)

    def test_rejects_malformed_yaml(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "broken.yaml"
            path.write_text("departments: [", encoding="utf-8")
            with self.assertRaisesRegex(RegistryValidationError, "YAML parse error"):
                load_registry(path)

    def test_missing_file_is_reported(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(FileNotFoundError):
                load_registry(Path(directory) / "missing.yaml")


if __name__ == "__main__":
    unittest.main()
