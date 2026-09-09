# Task Evidence: Transaction Lifecycle UI-Flow Testing & Visual Validation

## 1. Outcome
Engineered and verified a reusable Compose Multiplatform UI-flow testing and visual-validation system. Automated test coverage validates all primary transaction journeys and edge cases (valid add, blank/zero invalid inputs, in-place edit without duplication, cancel deletion, confirmed deletion, and an end-to-end multi-screen transaction journey) running headlessly on desktop JVM via Skiko. Automated screenshots are isolated in build outputs first (`build/reports/visual-validation/screenshots/`), visually reviewed against reference mockups in `docs/design/reference/`, and approved durable evidence is committed to `docs/evidence/screenshots/`.

## 2. Scope
### In-Scope Deliverables
- **UI Quality Gate Rule**: Added concise quality gate to `.agents/rules/project-rule.md` mandating automated UI testing, semantic test tags, visual validation against reference mockups, durable screenshot workflow, and command logging.
- **Workflow & Skill**: Created `.agents/workflows/ui-flow-testing.md` and `.agents/skills/ui-flow-testing/SKILL.md` defining standard procedures for UI flow testing, semantic tags, headless screenshot capture, and visual validation.
- **Task Evidence Template**: Updated `.agents/templates/task-evidence.md` with structured sections for UI-flow coverage, build-to-durable screenshot workflow, design token alignment, contract deviations, and command logs.
- **Minimal Test Dependencies**: Added `compose-uiTest` (`org.jetbrains.compose.ui:ui-test`) to `libs.versions.toml` and `commonTest`, plus desktop testing runtime to `jvmTest` in `shared/build.gradle.kts`.
- **Semantic Test Tags**: Added stable semantic `Modifier.testTag` identifiers (`add_transaction_fab`, `transaction_item_{id}`, `type_toggle_expense`, `type_toggle_income`, `amount_input`, `description_input`, `save_transaction_button`, `save_changes_button`, `delete_transaction_button`, `confirm_delete_button`, `cancel_delete_button`, `error_message`).
- **Automated UI-Flow Tests**:
  - `AddTransactionUiTest`: Valid add and validation errors on blank description and zero amount.
  - `EditTransactionUiTest`: In-place edit without duplicate creation, delete cancellation leaving records intact, and confirmed deletion removing records.
  - `TransactionJourneyUiTest`: Cross-screen navigation journey covering List -> Add -> List -> Edit -> List -> Edit -> Delete -> List.
- **Headless Screenshot Capture & Visual Review**:
  - `VisualValidationCaptureTest`: Captures 6 screens/states into `build/reports/visual-validation/screenshots/`.
  - Visual review against reference mockups in `docs/design/reference/`.
  - Approved images transferred to `docs/evidence/screenshots/`.

### Explicit Exclusions
- Robolectric or Android emulator UI tests (JVM desktop host tests with Skiko provide fast, deterministic, headless execution without Android emulator/mocking baggage).
- Pixel-diffing / screenshot-diffing assertions in CI (visual inspection review workflow against reference mockups).
- New feature screens or fields (budget allocations, category picker, custom date picker) omitted per contract.

## 3. Test-First Evidence (RED & GREEN)
### RED Phase (Failing Test)
- **Failing Tests**: Missing UI tests and missing test tags for UI interaction.
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.transaction.*"
  ```
- **Observed Failure Output**:
  ```text
  No tests found matching 'org.hoods.pocket.presentation.transaction.*'
  ```

### GREEN Phase (Passing Test)
- **Implementation Files**:
  - `shared/src/commonMain/kotlin/org/hoods/pocket/presentation/transaction/list/TransactionListContent.kt`
  - `shared/src/commonMain/kotlin/org/hoods/pocket/presentation/transaction/list/TransactionItem.kt`
  - `shared/src/commonMain/kotlin/org/hoods/pocket/presentation/transaction/add/AddTransactionContent.kt`
  - `shared/src/commonMain/kotlin/org/hoods/pocket/presentation/transaction/edit/EditTransactionContent.kt`
  - `shared/src/jvmTest/kotlin/org/hoods/pocket/presentation/transaction/add/AddTransactionUiTest.kt`
  - `shared/src/jvmTest/kotlin/org/hoods/pocket/presentation/transaction/edit/EditTransactionUiTest.kt`
  - `shared/src/jvmTest/kotlin/org/hoods/pocket/presentation/transaction/TransactionJourneyUiTest.kt`
  - `shared/src/jvmTest/kotlin/org/hoods/pocket/presentation/VisualValidationCaptureTest.kt`
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest
  ```
- **Observed Passing Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTestClasses
  > Task :shared:jvmTest
  BUILD SUCCESSFUL in 28s
  ```

## 4. Automated UI-Flow Test Coverage

| Scenario | Test Class & Method | Result | Description |
| :--- | :--- | :--- | :--- |
| **Valid Add** | `AddTransactionUiTest.validAdd_savesTransactionAndTriggersCompletion` | PASS | Fills description and amount, clicks save button, verifies transaction is persisted in repository and callback fires. |
| **Invalid Input (Blank Description)** | `AddTransactionUiTest.invalidInput_displaysErrorAndPreventsSave_whenDescriptionIsBlank` | PASS | Leaves description blank, verifies error message appears and no record is added to repository. |
| **Invalid Input (Zero Amount)** | `AddTransactionUiTest.invalidInput_displaysErrorAndPreventsSave_whenAmountIsZero` | PASS | Enters 0 amount, verifies validation error appears and no record is added to repository. |
| **Edit Without Duplication** | `EditTransactionUiTest.editWithoutDuplication_updatesExistingRecordAndRetainsCount` | PASS | Pre-fills existing item, alters description and amount, clicks save, verifies record is updated in place and repository count remains 1. |
| **Cancel Deletion** | `EditTransactionUiTest.cancelDeletion_leavesTransactionIntactInRepository` | PASS | Clicks delete, verifies confirmation dialog appears, clicks cancel, verifies dialog dismisses and transaction remains intact. |
| **Confirmed Deletion** | `EditTransactionUiTest.confirmedDeletion_removesTransactionAndTriggersDeleteCallback` | PASS | Clicks delete, verifies dialog appears, clicks confirm delete, verifies transaction is removed from repository and callback fires. |
| **Cross-Screen Journey** | `TransactionJourneyUiTest.completeTransactionLifecycleJourney_addEditDeleteCrossScreen` | PASS | End-to-end user journey across List -> Add form -> List (reactive update) -> Edit form -> List (in-place update) -> Edit -> Delete confirmation -> List (empty state). |

### UI-Flow Execution Commands & Logs
```bash
./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.transaction.add.AddTransactionUiTest"
./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.transaction.edit.EditTransactionUiTest"
./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.transaction.TransactionJourneyUiTest"
```
Output:
```text
BUILD SUCCESSFUL in 3s
BUILD SUCCESSFUL in 3s
BUILD SUCCESSFUL in 4s
```

## 5. Visual-Validation Evidence

### Captured Artifacts (from Build Output to Durable Evidence)
- **Build Output Directory**: `build/reports/visual-validation/screenshots/`
- **Approved Durable Evidence Directory**: `docs/evidence/screenshots/`
  - `transaction-list-empty.png`
  - `transaction-list-populated.png`
  - `add-transaction-expense.png`
  - `add-transaction-income.png`
  - `edit-transaction.png`
  - `delete-confirmation-dialog.png`

### Visual Comparison Against Design References
| Captured Screen | Reference Mockup (`docs/design/reference/`) | Visual Review Summary |
| :--- | :--- | :--- |
| `transaction-list-empty.png` | `pocket-budget-desktop-design/dashboard-transaction.png` | Empty state layout matches centering, muted description text, and primary call-to-action button styling. |
| `transaction-list-populated.png` | `pocket-budget-desktop-design/dashboard-transaction.png`<br>`pocket-budget-mobile-design/budget-transaction-listing.png` | 72dp row heights, 40dp circular avatar badges (green for income, red for expense), arrow glyphs (`↑`/`↓`), signed amount colors (`+$...` / `-$...`), and divider lines align with design tokens. |
| `add-transaction-expense.png` | `pocket-budget-desktop-design/add-transaction.png`<br>`pocket-budget-mobile-design/add-expense.png` | Capsule Expense/Income toggle with dark green active tab, hero `$0.00` numeric display, rounded text inputs with proper padding, and full-width primary save button. |
| `add-transaction-income.png` | `pocket-budget-mobile-design/add-income.png` | Income tab selection reflects active green indicator, save button updates text to "Save Income", hero display and text fields match design layout. |
| `edit-transaction.png` | `pocket-budget-desktop-design/add-transaction.png` | Top bar features delete trash button in error red, form inputs pre-populated with existing entity values, "Save Changes" primary button. |
| `delete-confirmation-dialog.png` | `pocket-budget-design-system.md` Dialog Specs | Material3 `AlertDialog` with bold title ("Delete Transaction?"), explanatory body, red destructive "Delete" action button, and "Cancel" dismiss button. |

### Design Token Alignment
- **Colors**:
  - `PocketColors.Primary` (`#005136`) used for primary buttons, active toggles, and FAB.
  - `PocketColors.Surface` (`#EEFDF2`) and `Container` (`#FFFFFF`) used for backgrounds and elevated cards.
  - `PocketColors.Income` (`#059669`) used for income badges and signed amounts.
  - `PocketColors.Expense` (`#BA1A1A`) used for expense badges, signed amounts, delete actions, and error text.
- **Typography**: Display/Headline semi-bold, body regular, hero numeric displays matching Work Sans scales.
- **Shapes**: `16.dp` rounded cards/inputs, capsule full-radius buttons and segmented toggles.
- **Spacing**: 8pt grid (8.dp, 16.dp, 24.dp, 32.dp), 72dp item heights with 40dp badges.

### Contract-Driven Deviations
- **Category Selector**: Mockups illustrate category dropdowns/chips (e.g., "Food & Drinks"); intentionally omitted per active transaction lifecycle contract.
- **Interactive Date Picker**: Mockups display interactive date picker; read-only timestamp indicator ("Today") used with `kotlin.time.Instant.now()` per project rules.
- **Budget Balance Cards**: Mockups display overall balance and monthly spending summary cards; deferred to future budget tracking milestone.

## 6. Verification Results
- **Verification Gate Command**:
  ```bash
  ./scripts/verify.sh
  ```
- **Gate Summary**:
  ```text
  ==================================================
            Verification Gate Summary
  ==================================================
    Git diff check (whitespace/formatting) : PASS
    Shared JVM Unit Tests (:shared:jvmTest) : PASS
    Android Host Unit Tests (:shared:testAndroidHostTest) : PASS
    Desktop App Compilation (:desktopApp:compileKotlin) : PASS
    Android App Packaging (:androidApp:assembleDebug) : PASS
    iOS Simulator Tests (:shared:iosSimulatorArm64Test) : NOT RUN (Target requires ARM64, host is macOS x86_64)
  ==================================================
  ✔ All active verification checks PASSED.
  ```

## 7. Execution Command Log
- `git diff --check`: `PASS`
- `./gradlew :shared:jvmTest`: `PASS` (49 tests total, including 7 UI-flow tests and 6 visual-validation capture tests)
- `./gradlew :shared:testAndroidHostTest`: `PASS` (36 tests total, domain + data + viewmodel unit tests)
- `./gradlew :desktopApp:compileKotlin`: `PASS`
- `./gradlew :androidApp:assembleDebug`: `PASS`
- `./gradlew :shared:iosSimulatorArm64Test`: `NOT RUN` (host platform is macOS x86_64, whereas the iOS simulator target requires ARM64).

## 8. Limitations & Unverified Targets
- **iOS Simulator Tests**: `NOT RUN` (host platform is macOS x86_64, whereas the `iosSimulatorArm64Test` target requires ARM64 hardware).
- **Automated Pixel Diffing**: Visual comparison conducted by systematic inspection against reference mockups in `docs/design/reference/` rather than pixel-threshold diffing to avoid fragile cross-OS rendering flakes.

## 9. Final Checkpoint
- **Status**: Ready for review; uncommitted pending explicit user approval.
- **Git Status**:
  ```text
  On branch main
  Changes not staged for commit:
    modified:   .agents/rules/project-rule.md
    modified:   .agents/templates/task-evidence.md
    modified:   gradle/libs.versions.toml
    modified:   shared/build.gradle.kts
    modified:   shared/src/commonMain/kotlin/org/hoods/pocket/presentation/transaction/add/AddTransactionContent.kt
    modified:   shared/src/commonMain/kotlin/org/hoods/pocket/presentation/transaction/edit/EditTransactionContent.kt
    modified:   shared/src/commonMain/kotlin/org/hoods/pocket/presentation/transaction/list/TransactionItem.kt
    modified:   shared/src/commonMain/kotlin/org/hoods/pocket/presentation/transaction/list/TransactionListContent.kt

  Untracked files:
    .agents/evidence/transaction-lifecycle-ui-flow-testing.md
    .agents/skills/ui-flow-testing/
    .agents/workflows/ui-flow-testing.md
    docs/evidence/
    shared/src/jvmTest/kotlin/org/hoods/pocket/presentation/
  ```
