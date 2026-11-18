"""Unit tests for the optional authenticated XhubREA API."""
import asyncio
import os
import unittest
from unittest.mock import patch

from fastapi import HTTPException

from xhubrea_engine.api import (
    AgentChatRequest,
    ResearchStructureRequest,
    app,
    _enforce_rate_limit,
    _request_times,
    require_api_token,
    research_structure,
)


class ApiAuthTests(unittest.TestCase):
    def setUp(self):
        _request_times.clear()

    def test_missing_server_token_disables_api(self):
        with patch.dict(os.environ, {}, clear=True):
            with self.assertRaises(HTTPException) as caught:
                require_api_token("Bearer anything")
        self.assertEqual(caught.exception.status_code, 503)

    def test_rejects_invalid_bearer_token(self):
        with patch.dict(os.environ, {"XHUBREA_API_TOKEN": "server-secret"}):
            with self.assertRaises(HTTPException) as caught:
                require_api_token("Bearer wrong")
        self.assertEqual(caught.exception.status_code, 401)

    def test_accepts_configured_bearer_token(self):
        with patch.dict(os.environ, {"XHUBREA_API_TOKEN": "server-secret"}):
            self.assertIsNone(require_api_token("Bearer server-secret"))

    def test_rejects_malformed_authorization_headers(self):
        malformed_headers = [
            None,
            "",
            "Basic server-secret",
            "Bearer",
            "Bearer ",
            "Bearer  server-secret",
            "Bearer wrong",
        ]
        with patch.dict(os.environ, {"XHUBREA_API_TOKEN": "server-secret"}):
            for header in malformed_headers:
                with self.subTest(header=header):
                    with self.assertRaises(HTTPException) as caught:
                        require_api_token(header)
                    self.assertEqual(caught.exception.status_code, 401)

    def test_every_versioned_api_route_requires_server_token(self):
        protected_routes = [
            route for route in app.routes
            if getattr(route, "path", "").startswith("/v1/")
        ]
        self.assertTrue(protected_routes, "Expected at least one protected API route")
        for route in protected_routes:
            with self.subTest(path=route.path):
                self.assertTrue(
                    any(dependency.call is require_api_token for dependency in route.dependencies),
                    f"{route.path} must depend on require_api_token",
                )

    def test_health_route_remains_public(self):
        health_routes = [
            route for route in app.routes
            if getattr(route, "path", "") == "/health"
        ]
        self.assertEqual(len(health_routes), 1)
        self.assertFalse(
            any(dependency.call is require_api_token for dependency in health_routes[0].dependencies)
        )

    def test_prompt_has_strict_length_bounds(self):
        with self.assertRaises(ValueError):
            AgentChatRequest(prompt="")
        with self.assertRaises(ValueError):
            AgentChatRequest(prompt="   ")
        with self.assertRaises(ValueError):
            AgentChatRequest(prompt="x" * 8001)

    def test_rate_limit_blocks_eleventh_request(self):
        for i in range(10):
            _enforce_rate_limit(now=100 + i)
        with self.assertRaises(HTTPException) as caught:
            _enforce_rate_limit(now=110)
        self.assertEqual(caught.exception.status_code, 429)

    def test_rate_limit_window_expires_after_sixty_seconds(self):
        for i in range(10):
            _enforce_rate_limit(now=100 + i)
        # The oldest request is exactly 60 seconds old and is expired.
        _enforce_rate_limit(now=160)
        self.assertEqual(len(_request_times), 10)

    def test_structure_endpoint_obeys_rate_limit(self):
        for i in range(10):
            _enforce_rate_limit(now=200 + i)
        payload = ResearchStructureRequest(findings=[{
            "evidence_id": "E-1",
            "source": "manifest",
            "observation": "Network permission declared",
            "confidence": "medium",
        }])
        with patch("xhubrea_engine.api.time.monotonic", return_value=210):
            with self.assertRaises(HTTPException) as caught:
                asyncio.run(research_structure(payload))
        self.assertEqual(caught.exception.status_code, 429)


if __name__ == "__main__":
    unittest.main()
