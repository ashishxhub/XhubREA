# XhubREA V2 — Frontend Design Handoff

Figma: https://www.figma.com/design/WNcsrmXau5nAC4SQWAKUDm

## Frames
- **XhubREA / Web Dashboard** — 1440 × 980 desktop workspace.
- **XhubREA / Android Home** — 390 × 844 mobile home screen.

Both frames are built from editable Figma text and shape layers (not flattened screenshots).

## Product direction
A calm, technical research workspace for understanding software the user owns or is authorized to inspect. The visual direction is dark, precise, evidence-first and approachable.

## Design tokens
- Background: #0C0F15
- Sidebar: #10151E
- Surface: #151A23
- Elevated surface: #1A2230
- Border: #293241
- Primary text: #EDF1F8
- Secondary text: #929EAF
- Primary accent: #6487FA
- Positive / safe status: #4AC7A2
- Typeface: Inter (desktop and Android design handoff)

## Desktop structure
1. Left navigation: Overview, Application intelligence, Security center, Behaviour intelligence, Research assistant, Reports, Projects, Settings.
2. Top bar: breadcrumb, local-first status, help and profile.
3. Hero: “Understand any app. With evidence.”
4. Research composer: APK, open-source URL and compare entry points.
5. Workspace metrics: active projects, reports, comparisons.
6. Intelligence module cards.

## Android structure
1. Compact top bar and profile.
2. Hero title and concise explanation.
3. APK selection card and quick actions.
4. Intelligence module list.
5. Bottom navigation: Home, Research, Reports, Settings.

## Interaction and accessibility notes
- Navigation items need visible selected, hover, focus and pressed states.
- Keep keyboard focus visible; respect reduced-motion preferences.
- Do not expose secrets or persist temporary API tokens in local storage.
- Label confidence and evidence source in analysis results.
- Keep file inspection read-only by default and require explicit authorization for external processing.

## Scope boundary
This handoff is a frontend design, not a claim that every module or backend integration is implemented. Android frame is a design reference; native Compose implementation must be integrated and tested separately.

## Implementation status (2026-11-17)
- Web: V2 color tokens are applied to the existing React dashboard stylesheet. Existing navigation, local research draft, help dialog and optional backend settings remain in place.
- Android: Material 3 theme colors are aligned to the V2 palette in the existing Compose application. The full Android screen layout remains to be redesigned incrementally so existing APK inspection capabilities are preserved.
- Verification: latest branch commits do not yet have an associated CI run. Do not treat web build, Android lint, unit tests, or device behavior as verified until CI and device checks complete.
