# Task Evidence: [Task / Feature Slice Name]

## 1. Outcome
<!-- Summary of user outcome and capability delivered (1-2 sentences) -->

## 2. Scope
### In-Scope Deliverables
<!-- Key components, use cases, domain logic, and UI elements completed -->
- 

### Explicit Exclusions
<!-- Related features, fields, or abstractions intentionally omitted per contract/rules -->
- 

## 3. Test-First Evidence (RED & GREEN)
### RED Phase (Failing Test)
- **Failing Test**: `[Package.TestClassName.testMethodName]`
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "[Package.TestClassName]"
  ```
- **Observed Failure Output**:
  ```text
  <!-- Paste failure output (AssertionError or missing reference before implementation) -->
  ```

### GREEN Phase (Passing Test)
- **Implementation File**: `[path/to/ImplementationFile.kt]`
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "[Package.TestClassName]"
  ```
- **Observed Passing Output**:
  ```text
  <!-- Paste passing test output -->
  BUILD SUCCESSFUL
  ```

## 4. Verification Results
- **Verification Gate Command**:
  ```bash
  ./scripts/verify.sh
  ```
- **Gate Summary**:
  ```text
  Git diff check (whitespace/formatting) : PASS
  Shared JVM Unit Tests (:shared:jvmTest) : PASS
  Android Host Unit Tests (:shared:testAndroidHostTest) : PASS
  Desktop App Compilation (:desktopApp:compileKotlin) : PASS
  Android App Packaging (:androidApp:assembleDebug) : PASS
  iOS Simulator Tests (:shared:iosSimulatorArm64Test) : NOT RUN (Target requires ARM64, host is macOS x86_64)
  ```

## 5. Manual Verification Journey
<!-- Step-by-step user journey verifying end-to-end functionality across platforms -->
1. **Launch App**: Launch on target platform (e.g. `./gradlew :desktopApp:run` or Android device/emulator).
2. **Action 1**: [e.g. Enter Income transaction with description and amount, click Save].
3. **Observation 1**: [e.g. Transaction appears immediately in transaction list with positive green amount].
4. **Action 2**: [e.g. Enter Expense transaction with description and amount, click Save].
5. **Observation 2**: [e.g. Transaction appears in transaction list with signed expense amount].
6. **Restart / Persistence**: [e.g. Close process, relaunch app, confirm transactions remain persisted in local Room database].

## 6. Limitations & Unverified Targets
<!-- Truthful statement of environment limitations, disabled targets, or manual test constraints -->
- **iOS Simulator Tests**: `NOT RUN` (host platform is macOS x86_64, whereas the iOS simulator target requires ARM64).
- 

## 7. Final Checkpoint
- **Commit Hash**: `[e.g. 0f2f4f7]`
- **Commit Message**:
  ```text
  [type(scope): summary]

  - bullet point detail
  ```
- **Files in Checkpoint**:
  - `[path/to/file1]`
  - `[path/to/file2]`
- **Final Git Status**:
  ```text
  On branch main
  nothing to commit, working tree clean
  ```
