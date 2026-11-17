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

## Optional HTTP API

Install the backend dependencies (Python 3.10+):

```bash
python -m pip install -r python/requirements-backend.txt
export XHUBREA_API_TOKEN="a-long-random-server-token"
export GEMINI_API_KEY="your_key"
export XHUBREA_ANTIGRAVITY_ENABLED=1
uvicorn xhubrea_engine.api:app --app-dir python --host 127.0.0.1 --port 8000
```

The API exposes `GET /health`, authenticated `POST /v1/agent/chat`, and authenticated `POST /v1/research/analyze`, and deterministic `POST /v1/research/structure`.
Send `Authorization: Bearer <XHUBREA_API_TOKEN>` and JSON `{\"prompt\":\"...\"}`.
The chat prompt is limited to 8,000 characters. Research analysis accepts 1–12 supplied
evidence items with bounded fields and builds an evidence-grounded report prompt. The
process applies a 10-request/minute rate limit. The limit is process-local; production deployment must also use HTTPS,
a trusted gateway-level rate limit, monitoring, and secure secret storage. Do not
expose the development server directly to the public internet.

## Not yet verified

- Antigravity SDK installation in the target backend environment.
- Live Gemini authentication and agent response.
- Backend API endpoint, authentication, quotas, monitoring, and deployment.
- Any Android/Chaquopy-native integration.

This commit adds the optional integration scaffold only. It does not claim the
SDK is installed in the Android APK or that a live agent has been tested.


## Web dashboard connection

The React dashboard can connect to the optional API from **Settings**. Enter the deployed backend URL and a temporary `XHUBREA_API_TOKEN` value. The token remains in page memory only; it is not written to localStorage or sessionStorage.

For browser access, configure an explicit comma-separated origin allowlist on the backend before starting Uvicorn:

```bash
export XHUBREA_CORS_ORIGINS="https://your-dashboard.example.com"
```

Use HTTPS for remote deployments. Do not expose the development server directly to the public internet. Do not place the Gemini API key in the browser; only the backend should hold `GEMINI_API_KEY`. The dashboard connection has been implemented, but production deployment and live Gemini responses still require owner-managed hosting and credentials.


## Dedicated Antigravity workspace in the web dashboard

The dashboard includes an **Antigravity Agent** workspace in the navigation. It reuses the authenticated `POST /v1/agent/chat` route through `web-dashboard/src/api.js`; the browser never calls Google directly and never receives the Gemini API key. Configure the backend URL and temporary bearer token in Settings, then open Antigravity Agent and send a research prompt. Remote backends must use HTTPS and allow the dashboard origin through the explicit CORS allowlist. This is a frontend integration; it still requires a deployed backend, server-side secrets, and a live provider test before it can be considered production-ready.
