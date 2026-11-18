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

The API exposes `GET /health`, authenticated `POST /v1/agent/chat`,
authenticated `POST /v1/research/analyze`, and deterministic
`POST /v1/research/structure`. Send
`Authorization: Bearer <XHUBREA_API_TOKEN>` and JSON
`{"prompt":"..."}`. The chat prompt is limited to 8,000 characters.
Research analysis accepts 1–12 supplied evidence items with bounded fields.
The process applies a 10-request/minute rate limit. This limit is process-local;
production deployment must also use HTTPS, gateway-level rate limiting,
monitoring, and secure secret storage.

## Render deployment (prepared, not deployed)

The repository now includes a root `Dockerfile`, `.dockerignore`, and
`render.yaml` blueprint for the separate Python API. To deploy:

1. Sign in to Render and choose **New → Blueprint**.
2. Connect the `ashishxhub/XhubREA` GitHub repository and select the
   `feature/antigravity-render-deployment` branch for this setup preview.
3. Review the service named `xhubrea-antigravity-api` and let Render build the
   Dockerfile from the repository root.
4. In the service's environment settings, add `GEMINI_API_KEY` as a secret.
   Render generates `XHUBREA_API_TOKEN` from the blueprint; keep it private.
5. Confirm `XHUBREA_ANTIGRAVITY_ENABLED=1` and the dashboard CORS origin
   `https://xhub-rea.vercel.app`. Change that origin if the dashboard domain
   changes.
6. Deploy, then check the generated HTTPS service URL plus `/health`.

Do not paste either secret into chat, GitHub source files, or the browser's
Gemini settings. The browser only receives the XhubREA bearer token when you
enter it into the dashboard's in-memory Settings form. Keep the Gemini key
server-side only.

**Deployment is not complete yet:** an owner must connect the hosting account,
add the Gemini secret, deploy the service, and verify the live provider
response. This repository change only prepares the deployment configuration.

## Web dashboard connection

The React dashboard can connect to the API from **Settings**. Enter the
deployed backend URL and a temporary `XHUBREA_API_TOKEN` value. The token
remains in page memory only; it is not written to localStorage or
sessionStorage. Remote backends must use HTTPS and allow the dashboard origin
through the explicit CORS allowlist.

## Dedicated Antigravity workspace in the web dashboard

The dashboard includes an **Antigravity Agent** workspace in the navigation.
It reuses the authenticated `POST /v1/agent/chat` route through
`web-dashboard/src/api.js`; the browser never calls Google directly and
never receives the Gemini API key. This frontend integration still requires a
deployed backend, server-side secrets, and a live provider test.

## Not yet verified

- Container image build in GitHub Actions or on Render.
- Antigravity SDK installation in the target hosted environment.
- Live Gemini authentication and agent response.
- Production deployment, quotas, monitoring, and gateway-level rate limiting.
- Any Android/Chaquopy-native SDK integration.

The SDK is not installed in the Android APK. Android will connect to the same
secured HTTPS backend through an app-side API client in a later task.
