"""Tests for the authenticated app comparison endpoint."""
import unittest
from unittest.mock import patch

from fastapi import FastAPI
from fastapi.testclient import TestClient
from pydantic import ValidationError

from xhubrea_engine.app_comparison_api import (
    AppComparisonRequest,
    AppSnapshotRequest,
    create_app_comparison_router,
)


class AppComparisonApiTests(unittest.TestCase):
    def setUp(self):
        app = FastAPI()
        app.include_router(create_app_comparison_router(lambda: None, lambda: None))
        self.client = TestClient(app)
        self.payload = {
            "app_a": {"app_label": "Alpha", "permissions": ["CAMERA", "INTERNET"]},
            "app_b": {"app_label": "Beta", "permissions": ["INTERNET", "LOCATION"]},
        }

    def test_request_defaults_inventory_categories(self):
        request = AppSnapshotRequest()
        self.assertEqual(request.permissions, [])
        self.assertEqual(request.architecture, [])

    def test_rejects_oversized_category(self):
        with self.assertRaises(ValidationError):
            AppSnapshotRequest(permissions=[f"P{i}" for i in range(201)])

    def test_rejects_oversized_label(self):
        with self.assertRaises(ValidationError):
            AppSnapshotRequest(components=["x" * 301])

    def test_rejects_blank_app_label(self):
        with self.assertRaises(ValidationError):
            AppSnapshotRequest(app_label="  ")

    def test_endpoint_returns_comparison(self):
        with patch(
            "xhubrea_engine.app_comparison_api.compare_app_snapshots",
            return_value={"comparisons": {"permissions": {"shared": ["INTERNET"]}}},
        ) as compare:
            response = self.client.post("/v1/apps/compare", json=self.payload)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["comparisons"]["permissions"]["shared"], ["INTERNET"])
        compare.assert_called_once()

    def test_endpoint_maps_invalid_input_to_bad_request(self):
        with patch(
            "xhubrea_engine.app_comparison_api.compare_app_snapshots",
            side_effect=ValueError("invalid snapshot"),
        ):
            response = self.client.post("/v1/apps/compare", json=self.payload)
        self.assertEqual(response.status_code, 400)

    def test_endpoint_requires_auth_dependency(self):
        app = FastAPI()
        def deny():
            from fastapi import HTTPException
            raise HTTPException(status_code=401, detail="unauthorized")
        app.include_router(create_app_comparison_router(deny, lambda: None))
        response = TestClient(app).post("/v1/apps/compare", json=self.payload)
        self.assertEqual(response.status_code, 401)

    def test_request_requires_both_snapshots(self):
        with self.assertRaises(ValidationError):
            AppComparisonRequest(app_a={"app_label": "Alpha"})


if __name__ == "__main__":
    unittest.main()
