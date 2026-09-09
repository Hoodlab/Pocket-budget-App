# Task Evidence: Date Selection, Month Browsing & Room Filter Controls

## 1. Outcome
Users can browse transactions month-by-month on the transaction list screen with previous month, next month, and return-to-current-month actions. Month filtering is executed via a Room database date-range query directly in the SQLite layer through repository and domain use cases. When adding a new transaction, the date defaults to today with an interactive date picker; editing displays its stored date and allows changing it. All timestamps remain stored as `kotlin.time.Instant`. Dual-viewport visual-validation evidence (Mobile 390x844 dp and Desktop 1024x768 dp) has been captured, visually reviewed, and recorded.

## 2. Scope
### In-Scope Deliverables
- **Strengthened Project Controls**:
  - `.agents/rules/project-rule.md`: Mandates that when a user-controlled filter applies to persisted data, the repository and database query must receive that filter (no unbounded stored collection loading into ViewModels). Mandates dual-viewport inspection and evidence capture whenever mobile and desktop references exist.
  - `.agents/skills/design-system/SKILL.md`: Added dual-viewport reference inspection and evidence rules in required sources, workflow, and pre-implementation checklist.
  - `.agents/workflows/ui-flow-testing.md`: Mandates inspecting both mobile and desktop reference directories and capturing/reviewing evidence for both supported viewport sizes.
  - `.agents/templates/task-evidence.md`: Added dedicated Mobile Viewport and Desktop Viewport visual-validation evidence sections.
- **Room Date-Range Persistence**:
  - `TransactionDao`: Added `@Query("SELECT * FROM transactions WHERE dateEpochMillis >= :startEpochMillis AND dateEpochMillis < :endEpochMillis ORDER BY dateEpochMillis DESC") fun getTransactionsByDateRange(...)`.
  - `TransactionRepository` & `RoomTransactionRepository`: Added `getTransactionsByDateRange(startEpochMillis: Long, endEpochMillis: Long): Flow<List<Transaction>>`.
  - `RoomTransactionRepositoryTest`: Added Room-backed SQLite tests verifying date range containment (`>= start`, `< end`) and descending order.
- **Domain `GetTransactionsByMonthUseCase` & `YearMonth`**:
  - `YearMonth`: Added `startInstant(): Instant` and `endInstant(): Instant` returning exact UTC month boundaries.
  - `GetTransactionsByMonthUseCase`: Queries repository with epoch millis boundaries derived from `YearMonth`.
  - `TransactionListViewModel`: Uses `_selectedMonth.flatMapLatest { month -> getTransactionsByMonthUseCase(month).map { ... } }`, ensuring active database query switches with selected month.
- **Presentation Date Selection**:
  - Add transaction defaults date to today with `DatePickerDialog`.
  - Edit transaction displays stored date and allows modification via `DatePickerDialog`.
- **Dual-Viewport Visual Validation**:
  - Mobile (390x844 dp): Empty list, populated list, add expense, add income, edit transaction, delete confirmation.
  - Desktop (1024x768 dp): Empty list, populated list, add transaction, edit transaction.

### Explicit Exclusions
- External datetime dependencies (no `kotlinx-datetime` added; standard library `kotlin.time.Instant` preserved).
- Budget category selectors and budget allocation features.
- Recurring transaction schedules.

## 3. Test-First Evidence (RED & GREEN)

### Phase A: `YearMonth.startInstant()` / `endInstant()`
- **Failing Test**: `YearMonthTest.startInstant_and_endInstant_returnExactUtcMonthBoundaries`
- **RED Output**:
  ```text
  e: .../YearMonthTest.kt:91:69 Unresolved reference 'startInstant'.
  e: .../YearMonthTest.kt:92:69 Unresolved reference 'endInstant'.
  BUILD FAILED
  ```
- **GREEN Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  BUILD SUCCESSFUL in 20s
  ```

### Phase B: `RoomTransactionRepository.getTransactionsByDateRange()`
- **Room-Backed Test**: `RoomTransactionRepositoryTest.getTransactionsByDateRange_returnsOnlyTransactionsInRangeOrderedDescending`
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.data.RoomTransactionRepositoryTest"
  ```
- **Observed Passing Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  BUILD SUCCESSFUL in 14s
  ```

### Phase C: `GetTransactionsByMonthUseCase`
- **Failing Test**: `GetTransactionsByMonthUseCaseTest.returnsOnlyTransactionsForSpecifiedMonthOrderedDescending`
- **RED Output**:
  ```text
  e: .../GetTransactionsByMonthUseCaseTest.kt:53:23 Unresolved reference 'GetTransactionsByMonthUseCase'.
  BUILD FAILED in 3s
  ```
- **GREEN Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  BUILD SUCCESSFUL in 7s
  ```

## 4. Automated UI-Flow Test Coverage

| Scenario | Test Class & Method | Result | Description |
| :--- | :--- | :--- | :--- |
| **Month Browsing Navigation** | `TransactionListUiTest.monthBrowsing_navigatesMonthsAndFiltersTransactionsCorrectly` | PASS | Navigates previous/next months, queries date range for selected month, displays and uses "Today" return button. |
| **Add Screen Date Selection** | `AddTransactionUiTest.dateSelection_defaultsToTodayAndOpensDatePicker` | PASS | Verifies "Today" default date, opens date picker dialog, confirms and dismisses dialog. |
| **Edit Screen Stored Date** | `EditTransactionUiTest.dateSelection_showsStoredDateAndOpensDatePicker` | PASS | Verifies stored date is formatted and displayed, opens date picker dialog, and dismisses on cancel. |
| **Valid Add Transaction** | `AddTransactionUiTest.validAdd_savesTransactionAndTriggersCompletion` | PASS | Adds and saves valid transaction with timestamp cents. |
| **Invalid Input Validation** | `AddTransactionUiTest.invalidInput_...` | PASS | Blocks saving on blank description and zero amount. |
| **Edit Without Duplication** | `EditTransactionUiTest.editWithoutDuplication_updatesExistingRecordAndRetainsCount` | PASS | Updates existing record in place with new values and retains total count. |
| **Cancel Deletion** | `EditTransactionUiTest.cancelDeletion_leavesTransactionIntactInRepository` | PASS | Confirmation dialog cancel leaves record untouched. |
| **Confirmed Deletion** | `EditTransactionUiTest.confirmedDeletion_removesTransactionAndTriggersDeleteCallback` | PASS | Confirmation dialog delete permanently deletes record. |
| **Cross-Screen Lifecycle Journey** | `TransactionJourneyUiTest.completeTransactionLifecycleJourney_addEditDeleteCrossScreen` | PASS | Full lifecycle journey: Empty list -> Add transaction (defaults today) -> Reactive list update -> Month browsing previous (filtered out via date range) -> Return to current (visible) -> Edit transaction (stored date) -> Update in place -> Delete confirmation -> Empty list. |

### UI-Flow Execution Commands & Logs
```bash
./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.transaction.*"
```
Output:
```text
BUILD SUCCESSFUL in 12s
```

## 5. Visual-Validation Evidence

### Captured Artifacts (from Build Output to Durable Evidence)
- **Build Output Directory**: `build/reports/visual-validation/screenshots/`
- **Approved Durable Evidence Directory**: `docs/evidence/screenshots/`
  - Mobile Viewport (390x844 dp):
    - `mobile-transaction-list-empty.png`
    - `mobile-transaction-list-populated.png`
    - `mobile-add-transaction-expense.png`
    - `mobile-add-transaction-income.png`
    - `mobile-edit-transaction.png`
    - `mobile-delete-confirmation-dialog.png`
  - Desktop Viewport (1024x768 dp):
    - `desktop-transaction-list-empty.png`
    - `desktop-transaction-list-populated.png`
    - `desktop-add-transaction.png`
    - `desktop-edit-transaction.png`

### Visual Comparison Against Design References

#### Mobile Viewport (390x844 dp)
| Captured Screen | Reference Mockup (`docs/design/reference/pocket-budget-mobile-design/`) | Visual Review Summary |
| :--- | :--- | :--- |
| `mobile-transaction-list-populated.png` | `budget-transaction-listing.png` | Month selector row matches layout (`‹ September 2026 ›`), list rows display formatted dates (`Expense • Sep 8, 2026`), 72dp item heights and 40dp badges preserved. |
| `mobile-transaction-list-empty.png` | `budget-transaction-listing.png` | Clean empty state with month context (`No transactions yet\nTap + to add one`) and FAB. |
| `mobile-add-transaction-expense.png` | `add-expense.png` | Date row displays `📅 Today, Sep 9, 2026 ›` matching mockup date indicator and chevron navigation cue. |
| `mobile-add-transaction-income.png` | `add-income.png` | Income toggle selected, green hero display, date indicator with current date. |
| `mobile-edit-transaction.png` | `add-expense.png` | Stored date displayed (`📅 Aug 22, 2026 ›`), pre-filled values, top delete action icon. |
| `mobile-delete-confirmation-dialog.png` | `pocket-budget-design-system.md` Dialog Specs | Material 3 confirmation dialog with bold title, explanatory body, and red destructive action. |

#### Desktop Viewport (1024x768 dp)
| Captured Screen | Reference Mockup (`docs/design/reference/pocket-budget-desktop-design/`) | Visual Review Summary |
| :--- | :--- | :--- |
| `desktop-transaction-list-populated.png` | `dashboard-transaction.png` | Month header and navigation aligned cleanly across widescreen layout, cards styled with container tokens, FAB anchored bottom-right. |
| `desktop-transaction-list-empty.png` | `dashboard-transaction.png` | Widescreen centered empty container with actionable subtitle and FAB. |
| `desktop-add-transaction.png` | `add-transaction.png` | Widescreen modal card container with Expense/Income toggle, large amount hero, inputs, and primary save button. |
| `desktop-edit-transaction.png` | `add-transaction.png` | Widescreen modal card container with prefilled values, stored date, delete action icon, and save changes button. |

### Design Token Alignment
- **Primary Color**: `PocketColors.Primary` (`#005136`) applied to FAB, primary action buttons, active pill toggles, and "Today" return chip.
- **Surfaces**: `PocketColors.Surface` (`#EEFDF2`) and `SurfaceContainerLowest` (`#FFFFFF`) applied to screen backgrounds, cards, and dialogs.
- **Typography & Grid**: Work Sans typography scale, 8pt spacing grid, 16dp rounded card/input corners, capsule full-radius buttons.

### Contract-Driven Deviations
- **Category Selector**: Illustrated in reference mockups; excluded per transaction lifecycle contract.
- **Balance Overview Cards & Budget Bars**: Illustrated in desktop dashboard mockup; deferred to future budget milestone.

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
- `./gradlew :shared:jvmTest`: `PASS` (72 tests total across domain, data, Room, viewmodel, UI-flow, and dual-viewport capture)
- `./gradlew :shared:testAndroidHostTest`: `PASS` (44 tests total)
- `./gradlew :desktopApp:compileKotlin`: `PASS`
- `./gradlew :androidApp:assembleDebug`: `PASS`
- `./gradlew :shared:iosSimulatorArm64Test`: `NOT RUN` (host platform is macOS x86_64, whereas the iOS simulator target requires ARM64).

## 8. Limitations & Unverified Targets
- **iOS Simulator Tests**: `NOT RUN` (host platform is macOS x86_64, whereas the `iosSimulatorArm64Test` target requires ARM64 hardware).

## 9. Final Checkpoint
- **Status**: Verification complete. Waiting for developer approval before staging or committing.
