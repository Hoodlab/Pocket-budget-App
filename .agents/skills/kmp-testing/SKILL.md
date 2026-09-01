---
name: kmp-testing
description: >-
  Guide for executing and structuring Kotlin Multiplatform tests in Pocket Budget,
  with focus on Room persistence and shared source sets. Use to support the test-first
  workflow across JVM and Android host targets.
---

# Kotlin Multiplatform Testing in Pocket Budget

This skill provides testing guidelines and run commands for Kotlin Multiplatform (KMP) code in Pocket Budget. It complements the `.agents/workflows/test-first.md` workflow with specific instructions for the upcoming Room persistence layer.

---

## 1. Source Sets & Test Capabilities

Pocket Budget partitions shared code across several targets in `:shared`:

Target             | Source Set Location              | Execution Task                       | Purpose / Context
:----------------- | :------------------------------- | :----------------------------------- | :----------------
**Common**         | `shared/src/commonTest/`         | Executed via JVM / Host tasks        | Pure Kotlin domain models, use cases, business logic.
**JVM (Desktop)**  | `shared/src/jvmTest/`            | `./gradlew :shared:jvmTest`          | Fast in-memory SQLite/Room tests, repository implementations.
**Android Host**   | `shared/src/androidHostTest/`    | `./gradlew :shared:testAndroidHostTest` | Android host unit tests with Android resources enabled.
**iOS Simulator**  | `shared/src/iosTest/`            | `./gradlew :shared:iosSimulatorArm64Test` | Disabled on x86_64 Mac hosts (requires ARM64 hardware).

---

## 2. Room Multiplatform Test Strategy

When testing Room persistence (entities, DAOs, and repository implementations):

### In-Memory Database Creation
- Use `Room.inMemoryDatabaseBuilder` with a platform-appropriate or bundled SQLite driver (e.g., `BundledSQLiteDriver`).
- Keep database builders scoped per test or test class to guarantee clean isolation.
- Ensure the database instance is properly closed in teardown or `use {}` blocks.

### Testing DAOs & Repositories
1. **Flow & Coroutine Safety:** Run tests inside `kotlinx.coroutines.test.runTest`.
2. **Reactive Queries:** Use `.first()` or collect from DAO `Flow` queries to assert emitted lists.
3. **Data Types & Boundaries:**
   - Verify monetary amounts are stored and retrieved accurately as `Long` integer cents.
   - Verify `kotlin.time.Instant` values serialize and deserialize accurately without precision loss.

---

## 3. Test-First Execution Sequence

Support the RED → GREEN cycle using focused test execution:

### Step 1: RED (Failing Test)
Write the failing test case for the new DAO method or repository mapping before writing the implementation:
```bash
# Run focused test on JVM
./gradlew :shared:jvmTest --tests "org.hoods.pocket.data.local.<TestClassName>"
```
*Confirm the test fails with an `AssertionError` (missing data/behavior), not a compilation error.*

### Step 2: GREEN (Passing Test)
Implement the minimal DAO query, entity mapping, or repository method:
```bash
# Verify the focused test passes
./gradlew :shared:jvmTest --tests "org.hoods.pocket.data.local.<TestClassName>"
```

### Step 3: Wider Verification
After the focused test passes, verify across all available active targets:
```bash
./gradlew :shared:jvmTest
./gradlew :shared:testAndroidHostTest
```

---

## 4. Diagnostics & Troubleshooting

- **Architecture mismatch warnings:** `Native task 'iosSimulatorArm64Test' is disabled` is expected when running on macOS x86_64; do not treat this warning as a failure.
- **Incremental task failures:** If Gradle caches stale compilation outputs after schema changes, run with `--rerun-tasks`:
  ```bash
  ./gradlew :shared:jvmTest --rerun-tasks
  ```
