import os
import unittest
from unittest.mock import patch

from xhubrea_engine.antigravity_adapter import ask_antigravity


class AntigravityAdapterTests(unittest.IsolatedAsyncioTestCase):
    async def test_disabled_by_default(self):
        with patch.dict(os.environ, {}, clear=True):
            with self.assertRaisesRegex(RuntimeError, "integration is disabled"):
                await ask_antigravity("Explain this authorized project.")

    async def test_requires_server_side_api_key(self):
        with patch.dict(os.environ, {"XHUBREA_ANTIGRAVITY_ENABLED": "1"}, clear=True):
            with self.assertRaisesRegex(RuntimeError, "GEMINI_API_KEY"):
                await ask_antigravity("Explain this authorized project.")

    async def test_rejects_empty_prompt(self):
        with patch.dict(
            os.environ,
            {
                "XHUBREA_ANTIGRAVITY_ENABLED": "1",
                "GEMINI_API_KEY": "test-only-placeholder",
            },
            clear=True,
        ):
            with self.assertRaisesRegex(ValueError, "non-empty string"):
                await ask_antigravity("   ")

    async def test_rejects_non_string_prompt(self):
        with patch.dict(
            os.environ,
            {
                "XHUBREA_ANTIGRAVITY_ENABLED": "1",
                "GEMINI_API_KEY": "test-only-placeholder",
            },
            clear=True,
        ):
            with self.assertRaisesRegex(ValueError, "non-empty string"):
                await ask_antigravity(None)


if __name__ == "__main__":
    unittest.main()
