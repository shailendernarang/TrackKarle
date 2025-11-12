# WealthTracker

Private, offline-first Android app to record manual investments and view a simple dashboard. No cloud. No login. Your data stays on-device.

## Mandatory to have
- **Privacy-first**: Offline only, no cloud, no analytics.
- **Stable release builds**: ProGuard/R8 rules to preserve SQLCipher, Lottie, MPAndroidChart.
- **Flicker-free locale switching**: Scoped, per-screen Hindi numerals support without Activity recreation.
- **Accurate inputs**: Indian number formatting with cursor preservation and proper decimal handling.
- **Dark mode compatibility**: Chart and UI text colors adapt to theme.
- **Device lock support**: Optional device-credential overlay gate.

## MVP Features
- Manual add: record investments with type, amount, and optional metadata (e.g., bank for FD).
- Categories: FD, Mutual Fund, Equity, Gold, PPF/EPF, NPS, Others.
- Dashboard: Totals, counts, and quick visualization (Pie/Bar).
- Filters: View by investment type.
- Exports: Share CSV and PDF reports.
- Privacy: "Private • Offline • No cloud sync" across the app.

## Done (implemented)
- **Locale switching without flicker** using a localized Compose context per-route; deferred apply on background.
- **Dashboard charts via ChartUtils**: Centralized Pie/Bar setup, animations, and theming.
- **CalculatorsScreen refactor**: Performance/stability (state hoisting, Lazy grid keys, deprecated API fixes).
- **Start route helper**: Chooses between Dashboard/Invest based on data/notification/timeout.
- **Release crash fix**: R8/ProGuard rules to preserve SQLCipher native methods and restore Lottie/MPAndroidChart animations.
- **Dark mode chart text**: Uses Material theme colors.
- **InvestmentScreen charts removed** (kept on Dashboard only) for simpler UX.
- **Firebase Crashlytics** integrated (mapping upload enabled for release).

### What's New (27.9.4)
- **Critical Fix**: First-launch crash resolved with comprehensive ProGuard/R8 rules
  - Room entities preserved to prevent database initialization failures
  - Compose functions protected from aggressive obfuscation
  - Repository and data layer classes properly kept
- **UI/UX Improvements**:
  - Investment Type label: Increased spacing and text size for better readability
  - Touch targets fixed: Date pickers and dropdowns now work on entire field, not just icon
  - Three-dot menu: Edit/Delete actions consolidated into dropdown menu (saves screen space)
  - FD Rates section: Responsive sizing for phone screens, reduced header and sort field sizes
  - Numeric keyboards: All amount/rate/tenure fields now show numeric input
  - Input field sizing: Reduced "add investment" area field heights for compact display
- **Enhanced Insights**:
  - Added 25+ new investment metrics across all types
  - FD: Count, Banks, Highest Rate
  - Stocks: Average per stock, Top 3 coverage
  - Health Insurance: Total Premium, Avg Premium, Premium Range, Oldest Policy
  - Mutual Fund: Investment Range, Top 3 Cover
  - Gold: Investment Range, First Purchase
  - PPF/EPF/NPS: Contribution Range, Oldest Account, Added This Year
  - Term Insurance: Premium Range, Oldest Policy
- **Font consistency**: Montserrat font applied throughout app with responsive sizing
- **Test coverage**: 16/18 unit tests passing (FormatUtils 100% coverage)

### Previous Release (27.9.2)
- **Play variant as primary**: Standardized shipping on Play (plain) build.
- **Data migration safety**: Forward/reverse migration with deduplication.
- **UI polish for phones**: FAB navigation, centered CTAs, numeric keyboards.
- **Branding consistency**: App name and exports use "TrackKaro".

### Publishing
- **Gradle Play Publisher** configured. Publish with:
  - `./gradlew :app:publishPlayRelease` (uses Internal track by default).
  - Provide service account JSON via `PLAY_SERVICE_ACCOUNT_JSON` env or `play-service-account.json` at project root.

### ProGuard Configuration
- **Current**: Conservative rules (keeps most libraries intact)
  - Location: `app/proguard-rules.pro`
  - Safe for production, prevents first-launch crash
  - Minimal APK size reduction (~5-10%)
- **Optimized**: Available for future use
  - Location: `app/proguard-rules-optimized.pro`
  - Targets specific classes only
  - Potential 30-40% APK size reduction
  - Requires thorough testing before deployment

## Can be done (backlog)
- AMFI NAV annotations for mutual funds (read-only).
- Encrypted local backup/restore flows.
- Richer dashboards and breakdowns (by bank/time/range).
- Tax helpers (80C/80D, LTCG/STCG hints) and SIP reminders.
- Accessibility polish (high-contrast mode), more localization.

## Pending / Next
- **Backup & Restore**: Export/Import JSON in Settings (offline, user-initiated).
- **Theme polish**: Update status bar handling (avoid deprecated setter) and refine phone typography scale.
- **Analytics (optional)**: If desired later, add Firebase Analytics events for DB backend/migration.
- **Code cleanup**: Optionally remove Play-side encrypted pull stub (it safely no-ops now); extract migration helpers.
- **Test improvements**: 
  - Fix ViewModel test async timing issues (use UnconfinedTestDispatcher)
  - Add instrumented tests for UI components
  - Consider Robolectric for Android-dependent unit tests

## Tech Stack
- UI: Jetpack Compose + Material 3
- DI: Hilt
- Navigation: Navigation Compose
- Data: Room (data.local) with **SQLCipher** encryption
- Charts: MPAndroidChart via AndroidView (with shared **ChartUtils**)
- Language: Kotlin (Coroutines/Flow)
- Messaging/Crash: Firebase Cloud Messaging, **Crashlytics**
- Testing: JUnit 4, Mockito, Kotlinx Coroutines Test, Room Testing

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

## Testing
```bash
# Run all unit tests
./gradlew test

# Run specific test suite
./gradlew test --tests FormatUtilsTest

# Generate test report
./gradlew test
# Report: app/build/reports/tests/testPlayDebugUnitTest/index.html
```

### Test Coverage
- **FormatUtilsTest**: 15/15 tests passing (100%)
  - Currency formatting (INR, INR Short)
  - Integer formatting with Indian numbering
  - Percentage formatting
  - Hindi numerals toggle
- **InvestmentViewModelTest**: 1/3 tests passing
  - Filtering and sorting (has async timing issues)
  - Validation gates (has async timing issues)
- **Overall**: 16/18 tests passing (88.9%)

### Test Dependencies
- JUnit 4.13.2
- Kotlin Test 1.9.20
- Kotlinx Coroutines Test 1.7.3
- Mockito Core 5.7.0
- Mockito Kotlin 5.1.0
- AndroidX Core Testing 2.2.0
- Room Testing 2.7.0

## Usage
- Add Investment: Enter amount → choose type → (FD) select bank or (Others) enter custom name → Add.
- Filter: Use chips at top to filter by type.
- Share: Overflow menu → Share CSV/PDF.
- Navigate: Top-right button switches between Investments and Dashboard.
- Charts are shown on **Dashboard** only.

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
