# XhubREA Web Dashboard

A standalone React + Vite frontend workspace for XhubREA, kept separate from the native Android/Kotlin application so web iteration does not disrupt Android builds.

## Run locally

Requirements: Node.js 20.19+ or 22.12+.

```bash
cd web-dashboard
npm install
npm run dev
```

## V2 frontend

The V2 visual direction is a dark, evidence-first research workspace with a responsive sidebar, research composer, intelligence module entry points, settings and help states.

See [../docs/FRONTEND_DESIGN.md](../docs/FRONTEND_DESIGN.md) and the [editable Figma design](https://www.figma.com/design/WNcsrmXau5nAC4SQWAKUDm).

## Current implementation boundaries

- Responsive React + Vite workspace and sidebar navigation.
- Local preview conversation state and research composer.
- Optional HTTPS backend health/chat integration through user-supplied settings.
- Backend token remains in page memory only; it is not persisted in browser storage.
- Intelligence module pages are currently frontend placeholders until their corresponding API workflows are connected.
- APK selection, analysis jobs and report generation must be wired to verified backend contracts before being described as live functionality.

## Checks

```bash
npm test -- --run
npm run build
```
