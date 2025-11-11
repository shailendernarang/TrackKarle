# Feature List

## Mandatory to have
- Privacy-first: Offline only, no cloud, no analytics.
- Stable release builds: ProGuard/R8 rules to preserve SQLCipher, Lottie, MPAndroidChart.
- Flicker-free locale switching: Scoped, per-screen Hindi numerals support without Activity recreation.
- Accurate inputs: Indian number formatting with cursor preservation and proper decimal handling.
- Dark mode compatibility: Chart and UI text colors adapt to theme.
- Device lock support: Optional device-credential overlay gate.

## Done (implemented)
- Locale switching without flicker using a localized Compose context per-route; deferred apply on background.
- Dashboard charts via ChartUtils: Centralized Pie/Bar setup, animations, and theming.
- CalculatorsScreen refactor: Performance/stability (state hoisting, Lazy grid keys, deprecated API fixes).
- Start route helper: Chooses between Dashboard/Invest based on data/notification/timeout.
- Release crash fix: R8/ProGuard rules to preserve SQLCipher native methods and restore Lottie/MPAndroidChart animations.
- Dark mode chart text using Material theme colors.
- InvestmentScreen charts removed (Dashboard only) for simpler UX.
- Firebase Crashlytics integrated (mapping upload enabled for release).

## Pending (Major)
- AMFI NAV annotations for mutual funds (read-only) with simple gain/loss.
- Encrypted local backup/restore flows (offline, user-triggered).
- Richer dashboards: bank/time breakdowns, trend lines, top contributors.
- Tax helpers: 80C/80D/NPS caps tally, LTCG/STCG hints with holding-period tagging.
- Reminders: SIP dates, FD maturity windows (local notifications only).
- Accessibility & Localization: High-contrast mode, broader language coverage.

## Pending (Minor)
- Release hardening: Set release debuggable=false before shipping.
- Exports: More fields and multi-select filters for CSV/PDF.
- Data model polish: Normalize “Others”, unify MF/Equity optional fields, tags.
- Calculators: Persist last-used inputs, quick presets (10K/20K SIP).
- Dashboard charts: Total vs. percentage toggle; optional color themes.
- UI/UX: Copy-on-long-press for amounts, better empty states, placeholders.
- Performance: Use derivedStateOf where helpful; ensure stability of list keys.
- Theming: Optional dynamic color toggle; surfaceVariant contrast tweaks.
- Error handling: Helper text and actionable snackbars.
- Crashlytics hygiene: Add custom keys for critical flows (add/update/delete, export).

## Infra / Quality
- QA checklist: locale/dark mode/cold start/delete-undo/export/notifications.
- CI/CD (optional): GitHub Actions for assemble, lint, unit tests.
- Testing: Unit tests for formatting/parsing; UI tests for add/update/delete.
- Play release prep: Adaptive icon polish, clear privacy policy (offline), store listing.

## Suggested next steps
- Pick 1–2 majors to implement next: AMFI NAV (read-only) and Backup/Restore.
- Quick wins: Turn off release debuggable, enhance exports, add Crashlytics custom keys.
