# Pocket Budget

Pocket Budget is a Kotlin Multiplatform Compose application for exploring personal-budget functionality across Android, desktop, and iOS. The project is currently based on the standard Compose Multiplatform starter; its shared UI and logic live in the `shared` module.

## Platforms

- **Android:** `androidApp/`
- **Desktop (JVM):** `desktopApp/`
- **iOS:** `iosApp/`
- **Shared Kotlin and Compose code:** `shared/`

See [the architecture guide](docs/architecture.md) for module boundaries, [the configuration guide](docs/configuration.md) for Supabase environment setup, and [the contribution guide](docs/contributing.md) for day-to-day workflow.

## Prerequisites

- JDK 11 or newer (the project targets JVM 11)
- Android Studio or IntelliJ IDEA for Android and desktop development
- Xcode on macOS for iOS development

The Gradle wrapper selects the project’s Gradle version. Android development also needs a local Android SDK; configure it in `local.properties`, which is deliberately not committed.

## Run the application

From the repository root:

```bash
# Android debug APK
./gradlew :androidApp:assembleDebug

# Desktop application
./gradlew :desktopApp:run

# Shared JVM tests
./gradlew :shared:jvmTest
```

For iOS, open `iosApp/` in Xcode and run the `iosApp` scheme on a simulator or device.

## Test

```bash
./gradlew :shared:jvmTest
./gradlew :shared:testAndroidHostTest
```

Run the iOS simulator tests from Xcode or with `./gradlew :shared:iosSimulatorArm64Test` on an Apple Silicon Mac with the required simulator available.

## Repository conventions

- Place cross-platform product code in `shared/src/commonMain/`.
- Keep platform integrations within their respective source sets (for example, `shared/src/androidMain/`).
- Keep generated output, local SDK configuration, IDE files, and secrets out of commits; `.gitignore` already covers the common cases.
- Update the relevant tests whenever you change shared behavior.

## Branches and Student Checkpoints

This repository is designed for both learning and reference:

- **`main`:** Contains the complete, production-grade reference implementation with all features, tests, migrations, and documentation.
- **`dev`:** Starter branch set at the initial commit (`checkpoint-00-starter`). Check out this branch if you are following the course to build the app from scratch.

### Checkpoints Guide
We provide milestone git tags (`checkpoint-00-starter` through `checkpoint-10-runtime-config`) allowing you to inspect code or compare your progress at every stage of the curriculum.

For complete details on navigating and comparing checkpoints, see **[CHECKPOINTS.md](CHECKPOINTS.md)**.

## Features

- **Transaction Management:** Add, view, categorize, and browse transactions with Month/Year filtering.
- **Monthly & Category Budgets:** Create spending targets, monitor category limits, and track real-time spending progress.
- **Financial Dashboard:** Monthly cash flow summary with income, expense, and budget health metrics.
- **Authentication & Local Data Claiming:** Guest-first local budgeting with seamless sign-in and atomic data ownership claiming.
- **Offline-First Synchronization:** Local Room persistence with deterministic background sync, outbox retry, and Supabase PostgreSQL backend.
- **Responsive Navigation:** Adaptive `PocketNavigationShell` providing bottom navigation on compact mobile screens and sidebars on desktop.
- **Clean Architecture:** Strict separation between pure Domain models, Room/Supabase Data sources, and Compose Presentation quads.
- **Financial Precision:** Strict `Long` integer cents representation across all monetary logic.
