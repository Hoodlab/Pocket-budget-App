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

## Project status

The current UI is the starter screen. Product requirements, data model, branding, and release policy have not yet been defined in this repository.
