"""Unit tests for the optional authenticated XhubREA API."""
import os
import unittest
from unittest.mock import patch

from fastapi import HTTPException

from xhubrea_engine.api import (
    AgentChatRequest,
    _enforce_rate_limit,
    _request_times,
    require_api_token,
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


if __name__ == "__main__":
    unittest.main()
