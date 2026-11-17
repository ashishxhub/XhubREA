# Google Antigravity SDK — XhubREA Integration

## Integration decision

The Google Antigravity SDK is added as an **optional backend agent adapter**, not
as a dependency of the Android APK-analysis runtime.

Why it is separated:
- XhubREA's Android app currently embeds Python through Chaquopy 3.13.
- The Antigravity SDK ships a compiled local-harness runtime binary in its
  platform-specific PyPI wheels. The official installation guidance requires
  installing the PyPI package; cloning the SDK repository is insufficient.
- Android/Chaquopy wheel compatibility has not been established. Therefore,
  installing it directly into the Android app would be an unsupported and
  unverified integration.

## Install in a separate Python backend

Use Python 3.10+ in the backend environment:

```bash
python -m pip install -r python/requirements-antigravity.txt
```

Configure the Gemini key as a server-side environment variable. Never commit
API keys to Git or put them in the Android app:

```bash
export GEMINI_API_KEY="your_key"
export XHUBREA_ANTIGRAVITY_ENABLED=1
```

Windows PowerShell:

```powershell
$env:GEMINI_API_KEY="your_key"
$env:XHUBREA_ANTIGRAVITY_ENABLED="1"
```

## Adapter

Call `await ask_antigravity("...")` from an async backend route or service:

```python
from xhubrea_engine.antigravity_adapter import ask_antigravity

answer = await ask_antigravity(
    "Summarize the architecture of this authorized source project."
)
```

The adapter is opt-in, checks for a backend API key, and imports the SDK
lazily. It uses the SDK's default read-only capability mode. Do not enable
write-capable tools for untrusted prompts or inspected application contents.

## Not yet verified

- Antigravity SDK installation in the target backend environment.
- Live Gemini authentication and agent response.
- Backend API endpoint, authentication, quotas, monitoring, and deployment.
- Any Android/Chaquopy-native integration.

This commit adds the optional integration scaffold only. It does not claim the
SDK is installed in the Android APK or that a live agent has been tested.
