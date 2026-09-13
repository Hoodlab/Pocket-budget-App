# Task Evidence: Financial Dashboard

## 1. Outcome
Users can now view a unified monthly cash-flow overview—including monthly income, monthly spending, and net balance—alongside a read-only view of overall budget health and category-budget health directly on the **Transactions tab** for its selected month, matching the reference designs (`docs/design/reference/pocket-budget-desktop-design/dashboard-transaction.png` and `docs/design/reference/pocket-budget-mobile-design/budget-transaction-listing.png`).

The **Budget tab** remains the dedicated screen where users configure and manage their monthly budget limits and category allocations (set, update, remove), matching `docs/design/reference/pocket-budget-desktop-design/dashboard-budget.png` and `docs/design/reference/pocket-budget-mobile-design/budget-listing.png`.

Both tabs display related budget information by reusing the exact same domain summary use cases (`GetMonthlyBudgetSummaryUseCase`, `GetCategoryBudgetSummariesUseCase`, `GetCashFlowSummaryUseCase`) with zero duplicate calculations, queries, or financial rules. Each tab manages its own selected month independently. Monies are strictly modeled and calculated as `Long` integer cents, with `Float` permitted solely for non-monetary visual progress ratios. Aggregation is pushed down into Room SQLite queries (`WHERE type = 'INCOME'` and `WHERE type = 'EXPENSE'`) across month timestamp boundaries, guaranteeing unbounded transaction records are never loaded into memory. Responsive layouts are verified across mobile handheld portrait (390dp) and desktop widescreen (1024dp) viewports against design references.

## 2. Scope

### In-Scope Deliverables
- **Domain Layer**:
  - `CashFlowSummary`: Immutable domain model containing `yearMonth: YearMonth`, `income: Long`, `expense: Long`, and `val netBalance: Long = income - expense`. Enforces invariants `income >= 0L` and `expense >= 0L`.
  - `TransactionRepository`: Extended with `getIncomeTotalByDateRange(startEpochMillis: Long, endEpochMillis: Long): Flow<Long>`.
  - `GetCashFlowSummaryUseCase`: Combines `getIncomeTotalByDateRange` and `getExpenseTotalByDateRange` for the given `YearMonth` into a reactive `Flow<CashFlowSummary>`.
- **Persistence Layer**:
  - `TransactionDao`: Push-down query `@Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'INCOME' AND dateEpochMillis >= :startEpochMillis AND dateEpochMillis < :endEpochMillis") fun getIncomeTotalByDateRange(...)`.
  - `RoomTransactionRepository`: Implemented `getIncomeTotalByDateRange` delegating directly to SQLite aggregation query.
  - Zero database schema migrations required (database remains at version 4).
- **Presentation Layer (Transactions Tab - Overview)**:
  - `TransactionListUiState`: Extended with `cashFlow: CashFlowSummary? = null`, `budgetSummary: MonthlyBudgetSummary? = null`, `categorySummaries: List<CategoryBudgetSummary> = emptyList()`.
  - `TransactionListViewModel`: Injects `getCashFlowSummaryUseCase`, `getMonthlyBudgetSummaryUseCase`, and `getCategoryBudgetSummariesUseCase`, combining `transactionsFlow`, `cashFlowFlow`, `budgetFlow`, and `categoryFlow` reactively for its independently selected month.
  - `TransactionListContent`:
    - **Desktop Layout** (1024dp): Dual-column split matching `dashboard-transaction.png`. Left/center column houses `CashflowOverviewCardDesktop` (Net Balance, Monthly Income, Monthly Spent) and the Recent Transactions white container card (+ New Transaction button). Right column houses `BudgetHealthCardDesktop` displaying read-only overall budget health badge, `$spent / $limit`, progress bar, and category budget item rows.
    - **Mobile Layout** (390dp): Scrollable layout matching `budget-transaction-listing.png`. Features `CashFlowOverviewMobile` hero card (Net Balance, hairline divider, Income & Spent 2-column split), followed by `BudgetHealthReadOnlyCardMobile` (read-only budget health status and compact category budgets), and recent transactions list.
- **Presentation Layer (Budget Tab - Management)**:
  - `BudgetDashboardContent`: Dedicated to managing monthly budgets and category allocations (Set Monthly Budget button, Total Budget Used card with edit/delete actions, category allocation cards with edit/delete actions). Removed cash flow overview to adhere to the reference design.
- **Composition & Wiring**:
  - `App.kt`: Injected `getCashFlowSummaryUseCase`, `getMonthlyBudgetSummaryUseCase`, and `getCategoryBudgetSummariesUseCase` into `transactionListViewModelFactory`.
- **Testing & Quality Assurance**:
  - Unit tests for domain model invariants (`CashFlowSummaryTest`).
  - Unit tests for cash flow use case (`GetCashFlowSummaryUseCaseTest`).
  - Persistence unit tests for Room query push-down aggregation (`RoomTransactionRepositoryTest`).
  - Presentation ViewModel state flow and reactivity tests (`TransactionListViewModelTest`, `BudgetDashboardViewModelTest`).
  - Screen component tests for cash flow and read-only budget health (`TransactionListUiTest`, `BudgetDashboardUiTest`).
  - Cross-screen end-to-end user journey test verifying both tabs and month isolation (`BudgetJourneyUiTest`, `TransactionJourneyUiTest`).
  - Dual-viewport visual capture test (`VisualValidationCaptureTest`).

### Explicit Exclusions
- Analytics charts, trends, forecasting, or historical reports.
- Push notifications, alerts, or budget rollover.
- Multiple bank/cash accounts, transfers, or balance reconciliation.
- User authentication, cloud sync, or multi-device profiles.

---

## 3. Test-First Evidence (RED & GREEN)

### Phase A: Domain Model Invariants (`CashFlowSummaryTest`)
- **Failing Test**: `CashFlowSummaryTest.constructor_rejectsNegativeIncome`
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.domain.model.CashFlowSummaryTest"
  ```
- **RED Behavioral Failure Output**:
  ```text
  CashFlowSummaryTest[jvm] > constructor_rejectsNegativeIncome[jvm] FAILED
      java.lang.AssertionError: Expected an exception of class java.lang.IllegalArgumentException to be thrown, but was completed successfully.
          at kotlin.test.AssertionsKt__AssertionsKt.assertFailsWith(Assertions.kt:180)
          at org.hoods.pocket.domain.model.CashFlowSummaryTest.constructor_rejectsNegativeIncome(CashFlowSummaryTest.kt:33)
  5 tests completed, 1 failed
  BUILD FAILED in 3s
  ```
- **Implementation File**: `shared/src/commonMain/kotlin/org/hoods/pocket/domain/model/CashFlowSummary.kt`
- **GREEN Passing Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  CashFlowSummaryTest[jvm] > constructor_validValues_computesNetBalanceCorrectly[jvm] PASSED
  CashFlowSummaryTest[jvm] > constructor_negativeNetBalance_whenExpensesExceedIncome[jvm] PASSED
  CashFlowSummaryTest[jvm] > constructor_rejectsNegativeIncome[jvm] PASSED
  CashFlowSummaryTest[jvm] > constructor_rejectsNegativeExpense[jvm] PASSED
  CashFlowSummaryTest[jvm] > zeroCashFlow_computesZeroNetBalance[jvm] PASSED
  5 tests completed, 0 failed
  BUILD SUCCESSFUL in 4s
  ```

### Phase B: Domain Use Case (`GetCashFlowSummaryUseCaseTest`)
- **Failing Test**: `GetCashFlowSummaryUseCaseTest.cashFlowSummary_combinesIncomeAndExpense_andComputesNetBalance`
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.domain.usecase.GetCashFlowSummaryUseCaseTest"
  ```
- **RED Behavioral Failure Output**:
  ```text
  GetCashFlowSummaryUseCaseTest[jvm] > cashFlowSummary_combinesIncomeAndExpense_andComputesNetBalance[jvm] FAILED
      java.lang.AssertionError: expected:<500000> but was:<0>
          at kotlin.test.DefaultAsserter.fail(DefaultAsserter.kt:25)
          at kotlin.test.AssertionsKt__AssertionsKt.assertEquals(Assertions.kt:64)
          at org.hoods.pocket.domain.usecase.GetCashFlowSummaryUseCaseTest.cashFlowSummary_combinesIncomeAndExpense_andComputesNetBalance(GetCashFlowSummaryUseCaseTest.kt:36)
  4 tests completed, 1 failed
  BUILD FAILED in 3s
  ```
- **Implementation File**: `shared/src/commonMain/kotlin/org/hoods/pocket/domain/usecase/GetCashFlowSummaryUseCase.kt`
- **GREEN Passing Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  GetCashFlowSummaryUseCaseTest[jvm] > cashFlowSummary_combinesIncomeAndExpense_andComputesNetBalance[jvm] PASSED
  GetCashFlowSummaryUseCaseTest[jvm] > cashFlowSummary_negativeNetBalance_whenExpensesExceedIncome[jvm] PASSED
  GetCashFlowSummaryUseCaseTest[jvm] > cashFlowSummary_emptyMonth_returnsZeros[jvm] PASSED
  GetCashFlowSummaryUseCaseTest[jvm] > cashFlowSummary_monthFiltering_ignoresOtherMonths[jvm] PASSED
  4 tests completed, 0 failed
  BUILD SUCCESSFUL in 4s
  ```

### Phase C: Persistence Layer Push-Down Aggregation (`RoomTransactionRepositoryTest`)
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.data.RoomTransactionRepositoryTest"
  ```
- **GREEN Passing Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  RoomTransactionRepositoryTest[jvm] > insertAndGetById[jvm] PASSED
  RoomTransactionRepositoryTest[jvm] > getExpenseTotalByDateRange_filtersCorrectly[jvm] PASSED
  RoomTransactionRepositoryTest[jvm] > getIncomeTotalByDateRange_filtersIncomeOnlyAndSumsAmounts[jvm] PASSED
  RoomTransactionRepositoryTest[jvm] > getCategorySpendingByDateRange_groupsByExpenseCategoryOnly[jvm] PASSED
  RoomTransactionRepositoryTest[jvm] > getCategorySpendingByDateRange_emptyRange_returnsEmptyMap[jvm] PASSED
  ...
  8 tests completed, 0 failed
  BUILD SUCCESSFUL in 5s
  ```

### Phase D: Presentation ViewModel Reactive State (`TransactionListViewModelTest`)
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.transaction.list.TransactionListViewModelTest"
  ```
- **GREEN Passing Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  TransactionListViewModelTest[jvm] > uiState emits transactions filtered to selectedMonth[jvm] PASSED
  TransactionListViewModelTest[jvm] > onNextMonth and onPreviousMonth navigate months and filter transactions[jvm] PASSED
  TransactionListViewModelTest[jvm] > onCurrentMonth resets selectedMonth to YearMonth now[jvm] PASSED
  TransactionListViewModelTest[jvm] > uiState combines cashFlow, budgetSummary, and categorySummaries reactively[jvm] PASSED
  TransactionListViewModelTest[jvm] > uiState updates cashFlow and budgetSummary when repository emits changes[jvm] PASSED
  TransactionListViewModelTest[jvm] > uiState handles month without budget gracefully[jvm] PASSED
  6 tests completed, 0 failed
  BUILD SUCCESSFUL in 4s
  ```

### Phase E: Screen Component Tests (`TransactionListUiTest`)
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.transaction.list.TransactionListUiTest"
  ```
- **GREEN Passing Output**:
  ```text
  > Task :shared:jvmTest
  TransactionListUiTest[jvm] > monthBrowsing_navigatesMonthsAndFiltersTransactionsCorrectly[jvm] PASSED
  TransactionListUiTest[jvm] > cashFlowAndBudgetHealth_displayedWhenDataPresent[jvm] PASSED
  TransactionListUiTest[jvm] > cashFlowAndBudgetHealth_whenNoBudgetSet_displaysEmptyBudgetState[jvm] PASSED
  3 tests completed, 0 failed
  BUILD SUCCESSFUL in 4s
  ```

### Phase F: End-to-End User Journey Tests (`BudgetJourneyUiTest`)
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.budget.BudgetJourneyUiTest"
  ```
- **GREEN Passing Output**:
  ```text
  > Task :shared:jvmTest
  BudgetJourneyUiTest[jvm] > completeBudgetJourney_crossScreenSetAndObserveWithTransactions[jvm] PASSED
  BudgetJourneyUiTest[jvm] > completeBudgetLifecycleJourney_setEditCancelDeleteConfirm[jvm] PASSED
  BudgetJourneyUiTest[jvm] > categoryBudgetJourney_setAllocations_editAllocations_resetToZero[jvm] PASSED
  BudgetJourneyUiTest[jvm] > categoryBudgetHealthJourney_expenseAddChangeDelete_incomeIgnored_uncategorizedIsolated[jvm] PASSED
  BudgetJourneyUiTest[jvm] > financialDashboardJourney_incomeExpenseNetBalance_budgetHealth_categoryHealth_reactivity[jvm] PASSED
  5 tests completed, 0 failed
  BUILD SUCCESSFUL in 25s
  ```

---

## 4. UI Quality Gate

### Dual-Viewport Visual Validation Capture Summary
Automated headless Compose tests in `VisualValidationCaptureTest.kt` generated persistent visual screenshots for both handheld mobile and desktop widescreen layouts:

| Viewport | Screen State | Capture File | Status |
| :--- | :--- | :--- | :--- |
| Mobile (390 x 844 dp) | Populated Transactions Overview (Cash Flow + Budget Health + List) | `docs/evidence/screenshots/mobile-transaction-list-populated.png` | APPROVED |
| Mobile (390 x 844 dp) | Empty Transactions Overview | `docs/evidence/screenshots/mobile-transaction-list-empty.png` | APPROVED |
| Mobile (390 x 844 dp) | Budget Management Populated Dashboard | `docs/evidence/screenshots/mobile-budget-dashboard-populated.png` | APPROVED |
| Mobile (390 x 844 dp) | Budget Management Empty State | `docs/evidence/screenshots/mobile-budget-dashboard-empty.png` | APPROVED |
| Desktop (1024 x 768 dp) | Populated Transactions Overview (Dual-Column: Cash Flow + List + Budget Health) | `docs/evidence/screenshots/desktop-transaction-list-populated.png` | APPROVED |
| Desktop (1024 x 768 dp) | Empty Transactions Overview | `docs/evidence/screenshots/desktop-transaction-list-empty.png` | APPROVED |
| Desktop (1024 x 768 dp) | Budget Management Populated Dashboard | `docs/evidence/screenshots/desktop-budget-dashboard-populated.png` | APPROVED |
| Desktop (1024 x 768 dp) | Budget Management Empty State | `docs/evidence/screenshots/desktop-budget-dashboard-empty.png` | APPROVED |

### Per-Screen Design-Alignment Matrix

| UI Component | Reference Mockup | Implementation Details | Verdict |
| :--- | :--- | :--- | :--- |
| **Mobile Cash Flow Hero Card** | `docs/design/reference/pocket-budget-mobile-design/budget-transaction-listing.png` | White container (`PocketColors.SurfaceContainerLowest`), 24dp rounded corners, 2dp elevation. Top: "Net Balance" caption, bold 28sp formatted amount. Hairline horizontal divider. Bottom: 2-column split with green indicator dot + "Income" (`+$X.XX`) and red indicator dot + "Spent" (`-$X.XX`). Positioned at top of Transactions tab. | MATCH |
| **Mobile Read-Only Budget Health Card** | `docs/design/reference/pocket-budget-mobile-design/budget-transaction-listing.png` | Positioned beneath cash-flow hero card. Displays Budget Health title, status badge ("Near Limit", "Healthy", "Over Budget"), Used amount `$spent / $limit`, progress bar, and compact Category Budgets row items. | MATCH |
| **Desktop Cashflow Overview Card** | `docs/design/reference/pocket-budget-desktop-design/dashboard-transaction.png` | Left column top card: 3 stat cards in a row (`NET BALANCE`, `MONTHLY INCOME`, `MONTHLY SPENT`) with 10dp rounded corners, 0.04em letter spacing, and bold 18sp amounts. Income in `PocketColors.Income` (#059669), Spent in `PocketColors.Expense` (#BA1A1A). | MATCH |
| **Desktop Recent Transactions Card** | `docs/design/reference/pocket-budget-desktop-design/dashboard-transaction.png` | Left column bottom card: White container with section title "Recent Transactions" and pill button "+ New Transaction", followed by list of transaction rows with category icons and formatted amounts. | MATCH |
| **Desktop Read-Only Budget Health Card** | `docs/design/reference/pocket-budget-desktop-design/dashboard-transaction.png` | Dedicated 280dp right-hand column: Budget Health header with status badge capsule, Used `$spent / $limit`, progress bar, and Category Budgets section with icon, category name, `$spent / $limit`, progress bar, and status/over-budget text. | MATCH |
| **Budget Tab Management Flow** | `docs/design/reference/pocket-budget-desktop-design/dashboard-budget.png` & mobile design | Dedicated screen for managing monthly budget and category allocations. Shows Total Budget Used card with edit/delete actions, category allocation cards with edit/delete actions, and "+ Set Monthly Budget" action button. | MATCH |

---

## 5. Verification Gate Summary

Command executed: `./scripts/verify.sh`

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
