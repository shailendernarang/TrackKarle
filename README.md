# WealthTracker

Private, offline-first Android app to record manual investments and view a simple dashboard. No cloud. No login. Your data stays on-device.

## MVP Features
- Manual add: record investments with type, amount, and optional metadata (e.g., bank for FD).
- Categories: FD, Mutual Fund, Equity, Gold, PPF/EPF, NPS, Others.
- Dashboard: Totals, counts, and quick visualization (Pie/Bar).
- Filters: View by investment type.
- Exports: Share CSV and PDF reports.
- Privacy: "Private • Offline • No cloud sync" across the app.

## Tech Stack
- UI: Jetpack Compose + Material 3
- DI: Hilt
- Navigation: Navigation Compose
- Data: Room (data.local)
- Charts: MPAndroidChart via AndroidView
- Language: Kotlin (Coroutines/Flow)

## Project Structure
- `app/src/main/java/com/example/wealthtracker/ui/` Compose screens and ViewModel
- `app/src/main/java/com/example/wealthtracker/data/local/` Room entities, DAO, database, repository impl
- `app/src/main/java/com/example/wealthtracker/data/repository/` Repository interface
- `app/src/main/java/com/example/wealthtracker/util/` Formatting, constants
- `app/src/main/java/com/example/wealthtracker/MainActivity.kt` Navigation host and splash

## Setup
- Requirements: Android Studio Giraffe+ (AGP 8.2+), JDK 17, Android SDK 24+
- Clone: `git clone <repo>`
- Open: In Android Studio, open project root

## Build & Run
```bash
./gradlew assembleDebug   # build APK
# Start an emulator or connect a device
./gradlew installDebug    # install on device/emulator
```
APK path: `app/build/outputs/apk/debug/app-debug.apk`

## Usage
- Add Investment: Enter amount → choose type → (FD) select bank or (Others) enter custom name → Add.
- Filter: Use chips at top to filter by type.
- Share: Overflow menu → Share CSV/PDF.
- Navigate: Top-right button switches between Investments and Dashboard.

## Data & Privacy
- Storage: On-device Room DB only.
- Exports: Explicit user-triggered CSV/PDF share.
- No cloud: No background sync, accounts, or analytics.

## Roadmap (Short-Term)
- AMFI NAV fetch: Read-only MF NAVs from AMFI India to annotate MF entries.
- Local backup/restore: Encrypted file backup to device storage.
- Better charts: More breakdowns (by category, bank, time).

## Known Limitations (MVP)
- No automatic statement parsing.
- No real-time price updates.
- Basic validations only.

---

## India-Focused Small Feature Ideas

- Indian number formatting
  - Keep "₹" and lakh/crore formatting everywhere.

- Quick calculators
  - SIP Calculator: Monthly amount, expected return, tenure → future value.
  - Lumpsum MF Calculator: CAGR-based projection.
  - FD Maturity Calculator: Principal, rate, tenure → maturity & interest.
  - PPF/EPF: Yearly contribution projection and Section 80C tracker (cap ₹1.5L).

- Tax-oriented helpers
  - LTCG/STCG hints: Simple guidance per asset type; mark entries as long/short based on holding period.
  - Deductions tracker: 80C (PPF, ELSS, EPF), 80D (health insurance), NPS (80CCD(1B)) tally against caps.

- Mutual fund niceties
  - AMFI meta: Fund house, category (Large/Small/Hybrid), scheme code.
  - SIP reminders: Optional local notification on SIP date (offline).
  - Folio placeholder: Store folio no. (manual), no sync.

- FD details
  - Bank directory: Pre-populated Indian banks (done for FD selection).
  - Maturity reminders: Local notifications before maturity/auto-renew windows.

- Gold & other assets
  - Gold price reference: Allow manual entry + link to MCX reference (no auto-fetch).
  - RE/Other: Name/location fields and notes for "Others".

- Localization & accessibility
  - Regional language support: Hindi first; localize numerals/labels/amounts.
  - High-contrast mode: Accessibility-friendly theme variant.

- Backup choices (still offline)
  - Encrypted local backup to file with user PIN.
  - Export/Import JSON that stays on device or is user-shared.

- Security polish
  - App lock: Optional 4-digit PIN or biometrics gate on launch.
  - Sensitive masking: Hide/show amounts toggle on Dashboard.

- UX polish
  - Undo delete: Snackbar with "Undo".
  - Quick add presets: Frequently used amounts/types/banks.
