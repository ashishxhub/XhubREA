"""Tests for the bounded APK research API router."""
import asyncio
import unittest
from unittest.mock import patch

from fastapi import FastAPI
from fastapi.testclient import TestClient
from pydantic import ValidationError

from xhubrea_engine.apk_research_api import ApkResearchRequest, create_apk_research_router


class ApkResearchApiTests(unittest.TestCase):
    def setUp(self):
        app = FastAPI()
        app.include_router(create_apk_research_router(lambda: None, lambda: None))
        self.client = TestClient(app)

    def test_request_defaults_to_empty_metadata_inventory(self):
        request = ApkResearchRequest()
        self.assertEqual(request.package_name, "unknown")
        self.assertEqual(request.permissions, [])

    def test_rejects_oversized_permission_inventory(self):
        with self.assertRaises(ValidationError):
            ApkResearchRequest(permissions=["android.permission.INTERNET"] * 201)

    def test_rejects_oversized_inventory_item(self):
        with self.assertRaises(ValidationError):
            ApkResearchRequest(permissions=["x" * 301])

    def test_rejects_blank_package_name(self):
        with self.assertRaises(ValidationError):
            ApkResearchRequest(package_name="   ")

    def test_endpoint_returns_read_only_inventory_analysis(self):
        with patch(
            "xhubrea_engine.apk_research_api.analyze_apk_inventory",
            return_value={"package_name": "org.example.demo", "indicators": []},
        ) as analyzer:
            response = self.client.post(
                "/v1/apk/research",
                json={"package_name": "org.example.demo", "permissions": ["android.permission.CAMERA"]},
            )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["package_name"], "org.example.demo")
        analyzer.assert_called_once()

    def test_endpoint_maps_invalid_inventory_to_bad_request(self):
        with patch(
            "xhubrea_engine.apk_research_api.analyze_apk_inventory",
            side_effect=ValueError("invalid inventory"),
        ):
            response = self.client.post("/v1/apk/research", json={})
        self.assertEqual(response.status_code, 400)

    def test_endpoint_requires_auth_dependency(self):
        app = FastAPI()
        def deny():
            from fastapi import HTTPException
            raise HTTPException(status_code=401, detail="unauthorized")
        app.include_router(create_apk_research_router(deny, lambda: None))
        response = TestClient(app).post("/v1/apk/research", json={})
        self.assertEqual(response.status_code, 401)


if __name__ == "__main__":
    unittest.main()
