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

## 5. Visual-Validation Evidence & Per-Screen Design-Alignment Matrix

### Per-Screen Design-Alignment Matrix

| Screen / Journey | Mobile Reference | Desktop Reference | Required Layout Anchors | Approved Contract-Driven Omissions | Evidence Paths | Verdict |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Transaction List (Populated & Empty)** | `docs/design/reference/pocket-budget-mobile-design/budget-transaction-listing.png` | `docs/design/reference/pocket-budget-desktop-design/dashboard-transaction.png` | **Mobile**: Top title, month selector chip (`‹ Month YYYY ›`), transaction item rows (badge, description, date, amount), bottom-right FAB.<br>**Desktop**: 260dp fixed sidebar ("Pocket Budget", "+ Add Transaction", "Transactions"), top month navigation header (`Month YYYY ‹ ›`), white rounded card container (`Recent Transactions`, inline `+ New Transaction` pill, divider, transaction rows / centered empty text). | Budgets sidebar item, Cashflow overview cards, Budget health card, Category budgets cards (all out of transaction slice scope). | `docs/evidence/screenshots/mobile-transaction-list-populated.png`<br>`docs/evidence/screenshots/mobile-transaction-list-empty.png`<br>`docs/evidence/screenshots/desktop-transaction-list-populated.png`<br>`docs/evidence/screenshots/desktop-transaction-list-empty.png` | **Approved deviation** |
| **Add Transaction (Expense & Income)** | `docs/design/reference/pocket-budget-mobile-design/add-expense.png`<br>`docs/design/reference/pocket-budget-mobile-design/add-income.png` | `docs/design/reference/pocket-budget-desktop-design/add-transaction.png` | **Mobile**: Dismiss `✕`, segmented pill toggle, hero amount, input card (amount, description, date button), bottom full-width button.<br>**Desktop**: Centered card container (max 680dp), header (`← New Transaction`), horizontal divider, centered segmented toggle, dedicated Amount Box (mint container with `AMOUNT` label and large currency input), two-column row (`Description` + `Date`), side-by-side `Cancel` and `Save Transaction` pill buttons. | Category selector grid (categories not in active transaction scope). | `docs/evidence/screenshots/mobile-add-transaction-expense.png`<br>`docs/evidence/screenshots/mobile-add-transaction-income.png`<br>`docs/evidence/screenshots/desktop-add-transaction.png` | **Approved deviation** |
| **Edit Transaction** | `docs/design/reference/pocket-budget-mobile-design/add-expense.png` (form layout) | `docs/design/reference/pocket-budget-desktop-design/add-transaction.png` (form composition) | **Mobile**: Dismiss `✕`, segmented pill, trash icon, hero amount, prefilled inputs, save button.<br>**Desktop**: Centered card container (max 680dp), header (`← Edit Transaction` on left, trash icon `🗑` button on right), horizontal divider, centered segmented toggle, dedicated Amount Box (`AMOUNT` label, large prefilled amount), two-column row (`Description` + `Date`), side-by-side `Cancel` and `Save Changes` buttons. | Category selector grid. | `docs/evidence/screenshots/mobile-edit-transaction.png`<br>`docs/evidence/screenshots/desktop-edit-transaction.png` | **Approved deviation** |
| **Delete Confirmation Dialog** | `docs/design/reference/pocket-budget-design-system.md` Dialog Specs | `docs/design/reference/pocket-budget-design-system.md` Dialog Specs | Dialog title ("Delete Transaction?"), explanatory body, red destructive "Delete" action, "Cancel" action, rounded 20dp container. | None. | `docs/evidence/screenshots/mobile-delete-confirmation-dialog.png`<br>`docs/evidence/screenshots/delete-confirmation-dialog.png` | **Aligned** |

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
- **`mobile-transaction-list-populated.png`**: Matches `budget-transaction-listing.png`. Month selector row (`‹ September 2026 ›`), list rows with formatted dates (`Expense • Sep 8, 2026`), 72dp item heights and 40dp badges.
- **`mobile-transaction-list-empty.png`**: Matches `budget-transaction-listing.png` empty state with month context and FAB.
- **`mobile-add-transaction-expense.png` & `mobile-add-transaction-income.png`**: Matches `add-expense.png` and `add-income.png`. Segmented toggle, hero amount, input card with date indicator (`📅 Today, Sep 9, 2026 ›`), capsule save button.
- **`mobile-edit-transaction.png`**: Matches mobile form specs with pre-filled values, stored date (`📅 Aug 22, 2026 ›`), and top-right delete action icon.
- **`mobile-delete-confirmation-dialog.png`**: Material 3 confirmation dialog with bold title, explanatory body, and red destructive action.

#### Desktop Viewport (1024x768 dp)
- **`desktop-transaction-list-populated.png`**: Implements desktop layout anchors from `dashboard-transaction.png`. 260dp fixed sidebar with "Pocket Budget", "Financial Wellness", "+ Add Transaction" button, and active "Transactions" nav item. Top month navigation header with `September 2026 ‹ ›`. "Recent Transactions" white container card (`24.dp` corners, shadow) with inline `+ New Transaction` button pill, subtle divider, and transaction items with arrows, titles, dates, and amounts.
- **`desktop-transaction-list-empty.png`**: Implements desktop layout anchors from `dashboard-transaction.png`. Sidebar and month header intact; "Recent Transactions" card displays clean centered empty state message with actionable prompt.
- **`desktop-add-transaction.png`**: Implements desktop form composition from `add-transaction.png`. Centered white card container (max width 680dp) on soft mint background; card header with back arrow `←` and "New Transaction" title; divider; centered segmented Expense/Income pill; dedicated Amount Box with `AMOUNT` label and large currency input; two-column row for Description and Date; side-by-side "Cancel" and "Save Transaction" buttons.
- **`desktop-edit-transaction.png`**: Implements desktop form composition from `add-transaction.png`. Centered white card container (max width 680dp) with back arrow `←`, "Edit Transaction" title, and delete trash icon button `🗑` in header; divider; centered segmented pill; dedicated Amount Box; two-column row for Description and Date; side-by-side "Cancel" and "Save Changes" buttons.

### Design Token Alignment
- **Primary Color**: `PocketColors.Primary` (`#005136`) applied to sidebar buttons, primary action buttons, active pill toggles, and "Today" return chip.
- **Surfaces**: `PocketColors.Surface` (`#EEFDF2`) and `SurfaceContainerLowest` (`#FFFFFF`) applied to screen backgrounds, cards, and dialogs.
- **Typography & Grid**: Work Sans typography scale, 8pt spacing grid, 16dp rounded card/input corners, capsule full-radius buttons.

### Approved Contract-Driven Omissions
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
