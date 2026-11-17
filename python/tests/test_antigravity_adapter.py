import os
import sys
import types
import unittest
from unittest.mock import AsyncMock, patch

from xhubrea_engine.antigravity_adapter import SYSTEM_INSTRUCTIONS, ask_antigravity


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

    async def test_sdk_missing_has_actionable_error(self):
        with patch.dict(
            os.environ,
            {
                "XHUBREA_ANTIGRAVITY_ENABLED": "1",
                "GEMINI_API_KEY": "test-only-placeholder",
            },
            clear=True,
        ), patch.dict(sys.modules, {"google.antigravity": None}):
            with self.assertRaisesRegex(RuntimeError, "SDK is not installed"):
                await ask_antigravity("Explain this authorized project.")

    async def test_agent_lifecycle_and_response_are_wired(self):
        response = types.SimpleNamespace(text=AsyncMock(return_value="analysis"))
        agent = unittest.mock.MagicMock()
        agent.__aenter__ = AsyncMock(return_value=agent)
        agent.__aexit__ = AsyncMock(return_value=False)
        agent.chat = AsyncMock(return_value=response)

        class FakeAgent:
            def __init__(self, config):
                self.config = config

            async def __aenter__(self):
                return await agent.__aenter__()

            async def __aexit__(self, exc_type, exc, tb):
                return await agent.__aexit__(exc_type, exc, tb)

            async def chat(self, prompt):
                return await agent.chat(prompt)

        class FakeConfig:
            def __init__(self, *, system_instructions):
                self.system_instructions = system_instructions

        fake_sdk = types.ModuleType("google.antigravity")
        fake_sdk.Agent = FakeAgent
        fake_sdk.LocalAgentConfig = FakeConfig

        with patch.dict(
            os.environ,
            {
                "XHUBREA_ANTIGRAVITY_ENABLED": "1",
                "GEMINI_API_KEY": "test-only-placeholder",
            },
            clear=True,
        ), patch.dict(sys.modules, {"google.antigravity": fake_sdk}):
            result = await ask_antigravity("  Inspect authorized source.  ")

        self.assertEqual(result, "analysis")
        agent.chat.assert_awaited_once_with("Inspect authorized source.")
        agent.__aenter__.assert_awaited_once()
        agent.__aexit__.assert_awaited_once()
        self.assertIn("read-only", SYSTEM_INSTRUCTIONS.lower())
        self.assertIn("authorized", SYSTEM_INSTRUCTIONS.lower())


if __name__ == "__main__":
    unittest.main()
