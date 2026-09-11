# Task Evidence: Set and View Monthly Budget

## 1. Outcome
Users can set a total monthly spending limit for any selected month, view the saved budget after restarting the app, monitor spending calculated exclusively from expense transactions in that month, and observe remaining amounts and progress toward the limit. Income transactions are strictly excluded from spending calculations. Users can seamlessly browse months while maintaining consistent month selection across the Transaction List and Budget Dashboard. Dual-viewport visual-validation evidence (Mobile 390x844 dp and Desktop 1024x768 dp) has been captured, visually reviewed against references, and recorded.

## 2. Scope

### In-Scope Deliverables
- **Domain Layer**:
  - `MonthlyBudget`: Pure shared domain model (`id`, `year`, `month`, `limitCents`).
  - `MonthlyBudgetSummary`: Pure domain summary (`yearMonth`, `limitCents`, `spentCents`, `remainingCents`, `progressRatio`).
  - `BudgetRepository`: Interface for saving and reactively observing a monthly budget by month and year.
  - `TransactionRepository.getExpenseTotalByDateRange`: Direct aggregation method returning sum of expense cents in a date range.
  - `SetMonthlyBudgetUseCase`: Validates positive non-zero budget amounts and saves via repository.
  - `GetMonthlyBudgetSummaryUseCase`: Computes real-time budget summary combining budget limit and expense total within the month boundaries.
- **Persistence Layer (Room Multiplatform)**:
  - `MonthlyBudgetEntity`: Room entity with unique index on `(year, month)`.
  - `BudgetDao`: Room DAO with `upsertBudget` and `getBudgetByMonthYear` returning `Flow<MonthlyBudgetEntity?>`.
  - `TransactionDao.getExpenseTotalByDateRange`: SQLite aggregation query `SELECT COALESCE(SUM(amountCents), 0) FROM transactions WHERE type = 'EXPENSE' AND dateEpochMillis >= :startEpochMillis AND dateEpochMillis < :endEpochMillis`.
  - `AppDatabase`: Version incremented to 2 with `MonthlyBudgetEntity` table registered and database builder caching across target platforms.
  - `RoomBudgetRepository` & `InMemoryBudgetRepository`: Complete implementations of `BudgetRepository`.
- **Presentation & Navigation Layer**:
  - `BudgetDashboardViewModel`, `BudgetDashboardUiState`, `BudgetDashboardEvent`, `BudgetDashboardScreen`, `BudgetDashboardContent`.
  - `SetBudgetViewModel`, `SetBudgetUiState`, `SetBudgetEvent`, `SetBudgetScreen`, `SetBudgetContent`.
  - Typed Compose Navigation routes: `Screen.BudgetDashboard(year, month)` and `Screen.SetBudget(year, month)` in `PocketNavHost`.
  - Navigation wiring between Transaction List and Budget Dashboard (nav header and desktop sidebar).
- **Dual-Viewport Visual Validation**:
  - Mobile (390x844 dp): Empty dashboard, populated dashboard, set budget modal/form.
  - Desktop (1024x768 dp): Empty dashboard with sidebar, populated dashboard with metrics card, set budget dialog container.

### Explicit Exclusions
- Category budgets or category allocations.
- Editing or deleting an existing monthly budget.
- Budget carry-over between months.
- Overspending notifications.
- Forecasting, reports, and analytics.
- Recurring budgets.
- Authentication, user accounts, and cloud synchronization.

## 3. Test-First Evidence (RED & GREEN)

### Phase A: `MonthlyBudget` & `BudgetRepository` / `SetMonthlyBudgetUseCase`
- **Failing Test**: `SetMonthlyBudgetUseCaseTest.validBudget_savesToRepository` & `SetMonthlyBudgetUseCaseTest.invalidAmount_rejectsWithException`
- **RED Output**:
  ```text
  e: .../SetMonthlyBudgetUseCaseTest.kt:33:17 Unresolved reference 'SetMonthlyBudgetUseCase'.
  e: .../SetMonthlyBudgetUseCaseTest.kt:34:17 Unresolved reference 'BudgetRepository'.
  BUILD FAILED
  ```
- **GREEN Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  BUILD SUCCESSFUL in 8s
  ```

### Phase B: `GetMonthlyBudgetSummaryUseCase` (Expense-Only Aggregation)
- **Failing Test**: `GetMonthlyBudgetSummaryUseCaseTest.calculatesSpendingFromExpenseTransactionsOnly`
- **RED Output**:
  ```text
  e: .../GetMonthlyBudgetSummaryUseCaseTest.kt:41:25 Unresolved reference 'GetMonthlyBudgetSummaryUseCase'.
  BUILD FAILED
  ```
- **GREEN Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  BUILD SUCCESSFUL in 7s
  ```

### Phase C: Room Persistence Across Restarts (`RoomBudgetRepositoryTest`)
- **Room-Backed Test**: `RoomBudgetRepositoryTest.upsertAndRetrieve_persistsBudgetAcrossQueries`
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.data.RoomBudgetRepositoryTest"
  ```
- **Observed Passing Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  BUILD SUCCESSFUL in 11s
  ```

### Phase D: Room SQLite Expense Aggregation (`RoomTransactionRepositoryTest`)
- **Room-Backed Test**: `RoomTransactionRepositoryTest.getExpenseTotalByDateRange_sumsOnlyExpenseTransactionsInRange`
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.data.RoomTransactionRepositoryTest"
  ```
- **Observed Passing Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  BUILD SUCCESSFUL in 10s
  ```

## 4. Automated UI-Flow Test Coverage

| Scenario | Test Class & Method | Result | Description |
| :--- | :--- | :--- | :--- |
| **Empty Dashboard State** | `BudgetDashboardUiTest.emptyState_displaysNoBudgetConfiguredAndAction` | PASS | Displays empty state prompt when no budget is set for selected month, with actionable "Set Monthly Budget" button. |
| **Populated Dashboard State** | `BudgetDashboardUiTest.populatedState_displaysMetricsAndProgress` | PASS | Displays limit, spent amount, remaining amount, and progress indicator when budget is configured. |
| **Month Browsing on Dashboard** | `BudgetDashboardUiTest.monthNavigation_switchesMonthAndQueriesBudgetState` | PASS | Navigating previous/next month triggers month change and displays correct budget for that month. |
| **Valid Set Budget** | `SetBudgetUiTest.validAmount_savesBudgetAndTriggersDismiss` | PASS | Entering valid amount (e.g. `1200.00`) converts to `Long` cents, saves budget, and fires dismiss/navigate back. |
| **Invalid Set Budget Validation** | `SetBudgetUiTest.invalidAmount_showsErrorAndPreventsSave` | PASS | Blank, zero, or negative amounts trigger validation error message and prevent saving. |
| **End-to-End Budget Journey** | `BudgetJourneyUiTest.fullBudgetLifecycleJourney_setAndReflectOnDashboard` | PASS | User navigates from Transaction List -> Budget Dashboard (empty) -> Set Budget -> Saves $1,200.00 -> Dashboard reflects $1,200.00 limit, spending, and remaining balance. |

### UI-Flow Execution Commands & Logs
```bash
./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.budget.*"
```
Output:
```text
> Task :shared:compileKotlinJvm
> Task :shared:compileTestKotlinJvm
> Task :shared:jvmTest
BUILD SUCCESSFUL in 15s
```

## 5. Visual-Validation Evidence

### Captured Artifacts (from Build Output to Durable Evidence)
- **Build Output Directory**: `build/reports/visual-validation/screenshots/`
- **Approved Durable Evidence Directory**: `docs/evidence/screenshots/`
  - Mobile Viewport (390x844 dp):
    - `mobile-budget-dashboard-empty.png`
    - `mobile-budget-dashboard-populated.png`
    - `mobile-set-budget.png`
  - Desktop Viewport (1024x768 dp):
    - `desktop-budget-dashboard-empty.png`
    - `desktop-budget-dashboard-populated.png`
    - `desktop-set-budget.png`

### Per-Screen Design-Alignment Matrix

| Screen / Journey | Mobile Reference (`pocket-budget-mobile-design/`) | Desktop Reference (`pocket-budget-desktop-design/`) | Required Layout Anchors | Approved Contract-Driven Omissions | Evidence Paths | Verdict (`Aligned` / `Approved deviation` / `Correction required`) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Budget Dashboard (Empty)** | `docs/design/reference/pocket-budget-mobile-design/budget-transaction-listing.png` | `docs/design/reference/pocket-budget-desktop-design/dashboard-transaction.png` | **Mobile**: Top title, month selector chip (`‹ Month YYYY ›`), empty state card with explanation and "Set Monthly Budget" button.<br>**Desktop**: 260dp fixed sidebar ("Pocket Budget", "+ Add Transaction", "Budgets" active nav), top month header, white container card (`24.dp` corners, shadow) with explanatory prompt and centered action button. | Category budgets breakdown and daily spending forecast (omitted per contract Section 3). | `docs/evidence/screenshots/mobile-budget-dashboard-empty.png`<br>`docs/evidence/screenshots/desktop-budget-dashboard-empty.png` | **Approved deviation** |
| **Budget Dashboard (Populated)** | `docs/design/reference/pocket-budget-mobile-design/budget-transaction-listing.png` (header & cards) | `docs/design/reference/pocket-budget-desktop-design/dashboard-transaction.png` (summary card) | **Mobile**: Top month selector chip, hero spending summary card, remaining balance card, linear progress indicator bar, limit details.<br>**Desktop**: 260dp fixed sidebar, top month navigation, wide white metrics card with limit, spent, remaining pills, and styled progress bar. | Category breakdown list and analytics charts (omitted per contract Section 3). | `docs/evidence/screenshots/mobile-budget-dashboard-populated.png`<br>`docs/evidence/screenshots/desktop-budget-dashboard-populated.png` | **Approved deviation** |
| **Set Budget Screen / Dialog** | `docs/design/reference/pocket-budget-mobile-design/add-expense.png` (amount card composition) | `docs/design/reference/pocket-budget-desktop-design/add-transaction.png` (form composition) | **Mobile**: Dismiss `✕`, title "Set Monthly Budget", month indicator, dedicated mint Amount Box with `AMOUNT` label and large currency input, bottom full-width "Save Budget" button.<br>**Desktop**: Centered card container (max width 680dp) on mint background, header with back arrow `←` and "Set Monthly Budget", horizontal divider, dedicated Amount Box, side-by-side "Cancel" and "Save Budget" pill buttons. | Category selector and date picker (budget applies to total month limit). | `docs/evidence/screenshots/mobile-set-budget.png`<br>`docs/evidence/screenshots/desktop-set-budget.png` | **Approved deviation** |

### Visual Comparison Against Design References

#### Mobile Viewport (390x844 dp)
- **`mobile-budget-dashboard-empty.png`**: Clean header with title "Monthly Budget", month browsing selector (`‹ September 2026 ›`), and central card inviting user to establish a limit.
- **`mobile-budget-dashboard-populated.png`**: Prominent display of total budget limit ($3,000.00), spent amount ($1,350.00), remaining balance ($1,650.00), and horizontal progress bar with percentage indicator.
- **`mobile-set-budget.png`**: Clean modal form with dismiss button, target month label, large currency input box, helper/error text, and full-width save button.

#### Desktop Viewport (1024x768 dp)
- **`desktop-budget-dashboard-empty.png`**: Complete widescreen desktop shell with 260dp navigation sidebar, active "Budgets" item, top month browsing header, and responsive empty state card.
- **`desktop-budget-dashboard-populated.png`**: Desktop layout with sidebar, header, and expansive metrics card presenting financial limit, spent total, remaining balance, and stylized progress bar.
- **`desktop-set-budget.png`**: Centered 680dp form card with back navigation arrow, horizontal divider, dedicated mint amount container, and aligned action buttons.

### Design Token Alignment
- **Primary Color**: `PocketColors.Primary` (`#005136`) applied to buttons, active navigation indicators, and progress bar fill.
- **Surfaces**: `PocketColors.Surface` (`#EEFDF2`) and `PocketColors.SurfaceContainerLowest` (`#FFFFFF`) applied to screen backgrounds and card containers.
- **Typography**: Work Sans scale applied across headlines, titles, body, and numeric amounts.
- **Shapes & Spacing**: 8pt grid, 16dp rounded corners, capsule action buttons.

### Approved Contract-Driven Omissions
- **Category Budgets**: Design references illustrate category-level budget breakdowns (e.g. Groceries, Entertainment). Excluded per contract Section 3; single total monthly spending limit implemented.
- **Daily Spending Forecasting**: Design references show daily burn rate and days-remaining forecasts. Excluded per contract Section 3.
- **Budget Editing / Deletion**: Edit and delete actions omitted per contract Section 3 (set once per month).

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
- `git diff --check`: `PASS` (code 0)
- `./gradlew :shared:jvmTest`: `PASS` (code 0)
- `./gradlew :shared:testAndroidHostTest`: `PASS` (code 0)
- `./gradlew :desktopApp:compileKotlin`: `PASS` (code 0)
- `./gradlew :androidApp:assembleDebug`: `PASS` (code 0)
- `./scripts/verify.sh`: `PASS` (code 0)
- `./gradlew :shared:iosSimulatorArm64Test`: `NOT RUN` (host platform is macOS x86_64, whereas the iOS simulator target requires ARM64).

## 8. Limitations & Unverified Targets
- **iOS Simulator Tests**: `NOT RUN` (host platform is macOS x86_64, whereas the iOS simulator target requires ARM64).

## 9. Final Checkpoint
- **Status**: Ready for user review and approval before creating a commit.
