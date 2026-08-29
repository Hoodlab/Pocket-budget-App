# Contributing

## Before opening a change

1. Create a focused branch from `main`.
2. Keep shared code in `shared/` unless it genuinely belongs to a single platform.
3. Run the relevant Gradle test task before requesting review.
4. Do not include generated `build/` output, local SDK paths, IDE state, secrets, or personal data.

## Suggested checks

```bash
./gradlew :shared:jvmTest
./gradlew :androidApp:assembleDebug
```

Run the iOS application in Xcode when a change affects iOS interop, iOS resources, or the shared UI.

## Change descriptions

Describe the behavior changed, platforms affected, tests run, and any follow-up work. Include screenshots for user-interface changes when practical.

## Commit messages

Use short, imperative summaries such as `Add monthly summary screen` or `Fix Android currency formatting`. Keep unrelated refactors separate from feature changes when possible.
