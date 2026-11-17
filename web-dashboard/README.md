# XhubREA Web Dashboard

A standalone React + Vite frontend workspace for the XhubREA project. It is intentionally kept separate from the existing native Android/Kotlin application so the current Android build is not disrupted.

## Run locally

Requirements: Node.js 20.19+ or 22.12+.

```bash
cd web-dashboard
npm install
npm run dev
```

Open the local URL printed by Vite.

## Current scope

- Responsive, dark, ChatGPT-inspired workspace layout.
- Sidebar navigation for the planned intelligence modules.
- Prompt composer and local draft conversation state.
- No backend, authentication, AI model, APK upload, or analysis engine is connected yet. The composer currently stores and displays the user's text locally only.

## Next integration steps

1. Agree on API contracts between the web dashboard and XhubREA services.
2. Add authentication and secure session handling.
3. Connect authorized analysis jobs and evidence-backed reports.
4. Add loading, error, cancellation and accessibility states.
5. Add automated UI tests and run a production build.
