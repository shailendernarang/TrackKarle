# TrackKaro (WealthTracker)

Private, offline-first Android app to record manual investments and view a simple dashboard. No cloud. No login. Your data stays on-device.

## Core Features

### Investment Tracking
- **Manual investment entry** with type, amount, and metadata (bank for FD, custom names)
- **7 Investment Categories**: FD, Mutual Fund, Equity, Gold, PPF/EPF, NPS, Others
- **Filtering & Sorting**: View by investment type, sort by amount/date
- **Edit/Delete**: Three-dot menu for quick actions

### Dashboard & Visualization
- **Portfolio overview**: Total value, counts, and breakdown
- **Custom Compose Pie Charts**: Crash-free SafePieChart (replaced MPAndroidChart)
- **Bar charts**: Investment distribution visualization
- **25+ Investment Insights**: Metrics across all investment types

### Home Screen Widget
- **Portfolio summary widget**: Shows top 3 investment types with progress bars
- **Auto-refresh**: Widget updates when investments are added
- **Hindi localization**: Widget description in Hindi

### Privacy & Security
- **Offline-only**: No cloud sync, no accounts
- **SQLCipher encryption**: Encrypted local database
- **Device lock support**: Optional biometric/PIN gate on launch
- **No PII collection**: Analytics track behavior, not personal data

### Localization
- **Hindi numerals support**: Toggle in settings
- **Flicker-free locale switching**: Per-screen without Activity recreation
- **Indian number formatting**: Lakh/Crore with ₹ symbol

### Exports
- **CSV export**: Share investment data
- **PDF reports**: Formatted portfolio reports

### Calculators
- **SIP Calculator**: Monthly investment projections
- **Lumpsum Calculator**: CAGR-based projections
- **FD Maturity Calculator**: Principal, rate, tenure calculations
- **PPF/EPF Calculator**: Section 80C tracking

### Analytics & Monitoring
- **Firebase Analytics**: User behavior tracking (privacy-compliant)
- **Firebase Crashlytics**: Crash reporting with mapping upload
- **Performance Monitoring**: Custom traces for critical operations

### Ads Integration
- **InMobi SDK**: Banner ads with SDK initialization timeout handling

---

## What's New (27.9.5)
- **Widget Improvements**:
  - Progress bars replace colored views for better visualization
  - Widget auto-refreshes when investments are added
  - Hindi localization for widget description
- **Analytics Enhancements**:
  - Settings screen tracks dark mode, device lock, and language toggles
  - Country selection tracks user preferences
  - Screen view tracking for Settings and CountrySelection
- **InMobi SDK Fixes**:
  - SDK initialization with 5-second timeout
  - Graceful fallback when SDK not initialized
  - Better error logging for ad failures
- **Code Quality**:
  - Removed untracked testing strategy docs
  - Fixed string translatable attribute for AdMob ID

### Previous Release (27.9.4)
- **Critical Fix**: First-launch crash resolved with comprehensive ProGuard/R8 rules
- **UI/UX Improvements**: Touch targets, three-dot menu, numeric keyboards
- **Enhanced Insights**: 25+ new investment metrics across all types
- **Font consistency**: Montserrat font throughout app

### Previous Release (27.9.2)
- **Play variant as primary**: Standardized shipping on Play (plain) build
- **Data migration safety**: Forward/reverse migration with deduplication
- **UI polish for phones**: FAB navigation, centered CTAs, numeric keyboards
- **Branding consistency**: App name and exports use "TrackKaro"

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
- **UI**: Jetpack Compose + Material 3
- **DI**: Hilt
- **Navigation**: Navigation Compose
- **Data**: Room with SQLCipher encryption
- **Charts**: Custom Compose SafePieChart (replaced MPAndroidChart)
- **Language**: Kotlin (Coroutines/Flow)
- **Analytics**: Firebase Analytics, Crashlytics, Performance Monitoring
- **Ads**: InMobi SDK
- **Testing**: JUnit 4, Mockito, Kotlinx Coroutines Test

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

## Backlog Ideas

- **Tax helpers**: LTCG/STCG hints, 80C/80D deductions tracker
- **Mutual fund**: AMFI NAV fetch, SIP reminders, folio storage
- **FD maturity reminders**: Local notifications before maturity
- **Backup/Restore**: Encrypted JSON export/import
- **App lock**: PIN or biometrics gate
- **Amount masking**: Hide/show toggle on Dashboard
- **Undo delete**: Snackbar with undo action
