# Architecture

Pocket Budget is organized as a Kotlin Multiplatform project with Compose Multiplatform UI.

| Module | Responsibility |
| --- | --- |
| `shared` | Shared Kotlin logic, Compose UI, resources, and platform abstractions |
| `androidApp` | Android app entry point, manifest, and Android-specific packaging |
| `desktopApp` | JVM desktop entry point and native-distribution configuration |
| `iosApp` | Xcode project and SwiftUI entry point that hosts the shared Compose view |

## Source-set guidance

- Use `shared/src/commonMain/` for code that can run unchanged on every target.
- Use `shared/src/androidMain/`, `shared/src/jvmMain/`, or `shared/src/iosMain/` only for platform-specific integrations.
- Add common tests under `shared/src/commonTest/`; keep platform-specific tests in the corresponding test source set.
- Store shared Compose resources in `shared/src/commonMain/composeResources/`.

## Dependency management

Version numbers and library aliases are centralized in `gradle/libs.versions.toml`. Add or upgrade dependencies there first, then use the generated `libs` alias in module build files. Keep dependencies scoped to the narrowest module and source set that needs them.

## Product direction

The repository does not yet define the budget data model, persistence strategy, network services, or design system. Decide and document each of those before adding feature-specific modules or external services.
