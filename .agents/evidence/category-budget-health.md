# Task Evidence: Category Budget Health

## 1. Outcome
Users can now monitor monthly spending against each configured category budget allocation directly on the dashboard. The application calculates category limits, spent amounts, remaining amounts, visual progress ratios, and health statuses (`ON_TRACK`, `NEAR_LIMIT`, `OVER_BUDGET`) using exact `Long` integer cents. Aggregation is pushed down into Room SQLite queries (`GROUP BY category`) filtering exclusively `EXPENSE` transactions for the selected month, ensuring unbounded transaction collections are never loaded into ViewModels. The user experience is supported across mobile (card list) and desktop (3-column responsive card grid) with verified layout alignment.

## 2. Scope

### In-Scope Deliverables
- **Domain Layer**:
  - `CategoryBudgetSummary`: Immutable domain model containing category, limit amount (`Long`), spent amount (`Long`), remaining amount (`Long`), progress (`Float` non-monetary visual ratio), and health status (`BudgetHealthStatus`). Invariants enforce `category.applicableType == TransactionType.EXPENSE` and `limitAmount > 0L`.
  - `TransactionRepository`: Extended with `getCategorySpendingByDateRange(startEpochMillis: Long, endEpochMillis: Long): Flow<Map<TransactionCategory, Long>>`.
  - `GetCategoryBudgetSummariesUseCase`: Combines active category budgets with push-down category spending map for the selected month, computing exact remaining cents and integer-threshold health status ($\ge 80\%$ near limit, $> 100\%$ over budget).
- **Persistence Layer**:
  - `TransactionDao`: Added `CategorySpendingEntity` query projection POJO and `@Query("SELECT category, COALESCE(SUM(amount), 0) AS totalSpent FROM transactions WHERE type = 'EXPENSE' AND timestamp >= :startEpochMillis AND timestamp <= :endEpochMillis GROUP BY category")`.
  - `RoomTransactionRepository`: Implemented `getCategorySpendingByDateRange` delegating directly to SQLite `GROUP BY` query.
  - Zero database migrations required (`CategorySpendingEntity` is a query projection POJO, database schema remains at version 4).
- **Presentation Layer**:
  - `BudgetDashboardUiState`: Extended with `categorySummaries: List<CategoryBudgetSummary>` and `val hasCategoryBudgets: Boolean`.
  - `BudgetDashboardViewModel`: Injects `GetCategoryBudgetSummariesUseCase` and combines category budget summaries with total monthly budget reactive flows.
  - `BudgetDashboardContent`:
    - **Mobile Layout** (390dp): "Category Budgets" list of `CategoryBudgetMobileCard` displaying 42dp icon avatar, category display name, "$spent of $limit", health badge capsule, 6dp progress bar, and "% Used" with remaining or over budget amount.
    - **Desktop Layout** (1024dp): "Category Budgets" section with responsive 3-column card grid of `CategoryBudgetDesktopCard` displaying 32dp icon avatar, single-line title with ellipsis, health badge pill, $spent of $limit hero amount, remaining/over sub-row, and 8dp progress bar.
    - `CategoryHealthBadge`: Distinct badge styling for On Track (soft mint), Near Limit (soft orange), and Over Budget (soft red).
- **Composition & Wiring**:
  - `App.kt`: Instantiated and wired `GetCategoryBudgetSummariesUseCase` into `PocketBudgetRoot` and `BudgetDashboardViewModel`.
- **Testing & Visual Validation**:
  - Domain model invariants unit tests (`CategoryBudgetSummaryTest`).
  - Use case unit tests with fake repository (`GetCategoryBudgetSummariesUseCaseTest`).
  - Room push-down grouping, expense-only filtering, and date range tests (`RoomTransactionRepositoryTest`).
  - ViewModel reactive state flow tests (`BudgetDashboardViewModelTest`).
  - Screen UI component tests (`BudgetDashboardUiTest`).
  - Cross-screen end-to-end user journey test (`BudgetJourneyUiTest`).
  - Dual-viewport visual capture test (`VisualValidationCaptureTest`).

### Explicit Exclusions
- Creating, editing, or removing category allocations from the dashboard (managed strictly via Set/Edit Budget flow).
- Category budget history, analytics charts, or predictive forecasting.
- Budget recommendations, push notifications, or unspent rollover.
- Income category budgets or income category health tracking.
- Custom user-defined category creation.
- Cloud synchronization, user authentication, or multi-user accounts.

## 3. Test-First Evidence (RED & GREEN)

### Phase A: Domain Model Invariants (`CategoryBudgetSummaryTest`)
- **Failing Test**: `CategoryBudgetSummaryTest.constructor_rejectsIncomeCategory`
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.domain.model.CategoryBudgetSummaryTest"
  ```
- **RED Behavioral Failure Output**:
  ```text
  CategoryBudgetSummaryTest[jvm] > constructor_rejectsIncomeCategory[jvm] FAILED
      java.lang.AssertionError: Expected an exception of class java.lang.IllegalArgumentException to be thrown, but was completed successfully.
          at kotlin.test.AssertionsKt__AssertionsKt.assertFailsWith(Assertions.kt:180)
          at org.hoods.pocket.domain.model.CategoryBudgetSummaryTest.constructor_rejectsIncomeCategory(CategoryBudgetSummaryTest.kt:32)
  4 tests completed, 1 failed
  BUILD FAILED in 3s
  ```
- **Implementation File**: `shared/src/commonMain/kotlin/org/hoods/pocket/domain/model/CategoryBudgetSummary.kt`
- **GREEN Passing Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  CategoryBudgetSummaryTest[jvm] > constructor_validExpenseCategory_createsSuccessfully[jvm] PASSED
  CategoryBudgetSummaryTest[jvm] > constructor_rejectsIncomeCategory[jvm] PASSED
  CategoryBudgetSummaryTest[jvm] > constructor_rejectsUncategorized[jvm] PASSED
  CategoryBudgetSummaryTest[jvm] > constructor_rejectsZeroOrNegativeLimit[jvm] PASSED
  4 tests completed, 0 failed
  BUILD SUCCESSFUL in 4s
  ```

### Phase B: Domain Use Case (`GetCategoryBudgetSummariesUseCaseTest`)
- **Failing Test**: `GetCategoryBudgetSummariesUseCaseTest.categorisedExpenses_mappedToCategorySummaries_withCorrectSpentRemainingProgressAndHealth`
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.domain.usecase.GetCategoryBudgetSummariesUseCaseTest"
  ```
- **RED Behavioral Failure Output**:
  ```text
  GetCategoryBudgetSummariesUseCaseTest[jvm] > categorisedExpenses_mappedToCategorySummaries_withCorrectSpentRemainingProgressAndHealth[jvm] FAILED
      java.lang.AssertionError: expected:<45000> but was:<0>
          at kotlin.test.DefaultAsserter.fail(DefaultAsserter.kt:25)
          at kotlin.test.AssertionsKt__AssertionsKt.assertEquals(Assertions.kt:64)
          at org.hoods.pocket.domain.usecase.GetCategoryBudgetSummariesUseCaseTest.categorisedExpenses_mappedToCategorySummaries_withCorrectSpentRemainingProgressAndHealth(GetCategoryBudgetSummariesUseCaseTest.kt:57)
  6 tests completed, 1 failed
  BUILD FAILED in 4s
  ```
- **Implementation File**: `shared/src/commonMain/kotlin/org/hoods/pocket/domain/usecase/GetCategoryBudgetSummariesUseCase.kt`
- **GREEN Passing Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  GetCategoryBudgetSummariesUseCaseTest[jvm] > categorisedExpenses_mappedToCategorySummaries_withCorrectSpentRemainingProgressAndHealth[jvm] PASSED
  GetCategoryBudgetSummariesUseCaseTest[jvm] > incomeTransactions_doNotAffectCategoryHealth[jvm] PASSED
  GetCategoryBudgetSummariesUseCaseTest[jvm] > uncategorizedExpense_doesNotProduceCategorySummary[jvm] PASSED
  GetCategoryBudgetSummariesUseCaseTest[jvm] > unallocatedExpenseCategory_doesNotProduceCategorySummary[jvm] PASSED
  GetCategoryBudgetSummariesUseCaseTest[jvm] > nearLimitAndOverBudgetThresholds_correctlyEvaluated[jvm] PASSED
  GetCategoryBudgetSummariesUseCaseTest[jvm] > monthIsolation_transactionsInOtherMonths_ignored[jvm] PASSED
  6 tests completed, 0 failed
  BUILD SUCCESSFUL in 5s
  ```

### Phase C: Data Layer Push-Down Aggregation (`RoomTransactionRepositoryTest`)
- **Failing Test**: `RoomTransactionRepositoryTest.getCategorySpendingByDateRange_groupsExpensesByCategoryWithinRange_ignoringIncomeAndOutOfRange`
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.data.RoomTransactionRepositoryTest"
  ```
- **RED Behavioral Failure Output**:
  ```text
  RoomTransactionRepositoryTest[jvm] > getCategorySpendingByDateRange_groupsExpensesByCategoryWithinRange_ignoringIncomeAndOutOfRange[jvm] FAILED
      java.lang.AssertionError: expected:<4000> but was:<null>
          at kotlin.test.DefaultAsserter.fail(DefaultAsserter.kt:25)
          at kotlin.test.AssertionsKt__AssertionsKt.assertEquals(Assertions.kt:64)
          at org.hoods.pocket.data.RoomTransactionRepositoryTest.getCategorySpendingByDateRange_groupsExpensesByCategoryWithinRange_ignoringIncomeAndOutOfRange(RoomTransactionRepositoryTest.kt:379)
  2 tests completed, 1 failed
  BUILD FAILED in 5s
  ```
- **Implementation Files**:
  - `shared/src/commonMain/kotlin/org/hoods/pocket/data/local/dao/TransactionDao.kt`
  - `shared/src/commonMain/kotlin/org/hoods/pocket/data/repository/RoomTransactionRepository.kt`
- **GREEN Passing Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  RoomTransactionRepositoryTest[jvm] > getCategorySpendingByDateRange_groupsExpensesByCategoryWithinRange_ignoringIncomeAndOutOfRange[jvm] PASSED
  RoomTransactionRepositoryTest[jvm] > getCategorySpendingByDateRange_emptyTransactions_returnsEmptyMap[jvm] PASSED
  2 tests completed, 0 failed
  BUILD SUCCESSFUL in 8s
  ```

### Phase D: ViewModel Reactive Combining (`BudgetDashboardViewModelTest`)
- **Failing Test**: `BudgetDashboardViewModelTest.uiState emits categorySummaries when category budgets exist`
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.budget.dashboard.BudgetDashboardViewModelTest"
  ```
- **RED Behavioral Failure Output**:
  ```text
  BudgetDashboardViewModelTest[jvm] > uiState emits categorySummaries when category budgets exist[jvm] FAILED
      java.lang.AssertionError: expected:<1> but was:<0>
          at kotlin.test.DefaultAsserter.fail(DefaultAsserter.kt:25)
          at kotlin.test.AssertionsKt__AssertionsKt.assertEquals(Assertions.kt:64)
          at org.hoods.pocket.presentation.budget.dashboard.BudgetDashboardViewModelTest.uiState emits categorySummaries when category budgets exist(BudgetDashboardViewModelTest.kt:172)
  6 tests completed, 1 failed
  BUILD FAILED in 4s
  ```
- **Implementation Files**:
  - `shared/src/commonMain/kotlin/org/hoods/pocket/presentation/budget/dashboard/BudgetDashboardUiState.kt`
  - `shared/src/commonMain/kotlin/org/hoods/pocket/presentation/budget/dashboard/BudgetDashboardViewModel.kt`
- **GREEN Passing Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  BudgetDashboardViewModelTest[jvm] > uiState emits summary with budget and expense spending, ignoring income[jvm] PASSED
  BudgetDashboardViewModelTest[jvm] > empty state when no budget exists for month[jvm] PASSED
  BudgetDashboardViewModelTest[jvm] > navigation between months updates summary reactively[jvm] PASSED
  BudgetDashboardViewModelTest[jvm] > onCurrentMonthClick resets to YearMonth now[jvm] PASSED
  BudgetDashboardViewModelTest[jvm] > uiState emits categorySummaries when category budgets exist[jvm] PASSED
  BudgetDashboardViewModelTest[jvm] > adding expense updates categorySummaries reactively[jvm] PASSED
  6 tests completed, 0 failed
  BUILD SUCCESSFUL in 7s
  ```

## 4. Automated UI-Flow Test Coverage

| Scenario | Test Class & Method | Result | Description |
| :--- | :--- | :--- | :--- |
| **Category Cards & Health Badges UI** | `BudgetDashboardUiTest.populatedCategoryBudgets_displaysCategoryCardsWithSpendingRemainingAndHealthBadge` | PASS | Verifies mobile list rendering, category display names, icons, spent of limit text, health badges (On Track, Near Limit, Over Budget), progress indicators, and remaining amounts. |
| **Near Limit & Over Budget UI Badges** | `BudgetDashboardUiTest.nearLimitAndOverBudgetCategoryCards_displayCorrectHealthBadgesAndRemainingOrOverAmounts` | PASS | Verifies distinct visual badges and amounts for Near Limit (80%) and Over Budget states. |
| **Complete End-to-End User Journey** | `BudgetJourneyUiTest.categoryBudgetHealthJourney_expenseAddChangeDelete_incomeIgnored_uncategorizedIsolated` | PASS | Complete multi-step lifecycle across navigation:<br>1. Set monthly budget ($2000) & category allocations (Groceries $600, Transport $200).<br>2. Add $450 Groceries expense $\to$ Groceries shows $450 of $600, On Track.<br>3. Add $160 Transport expense $\to$ Transport shows $160 of $200, Near Limit (80%).<br>4. Add $200 Groceries expense $\to$ Groceries becomes $650 of $600, Over Budget (over by $50).<br>5. Edit $200 expense to Transport $\to$ Groceries restores to $450 (On Track), Transport becomes $360 (Over Budget).<br>6. Delete $200 expense $\to$ Transport returns to $160 (Near Limit).<br>7. Add $1000 Salary (income) $\to$ Category spending unchanged.<br>8. Add $150 Uncategorized expense $\to$ Total spent increases from $610 to $760, but no Uncategorized category budget card is created and category budgets are unaffected.<br>9. Navigate across month boundary $\to$ Category health isolated to selected month. |

### UI-Flow Execution Commands
```bash
./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.budget.dashboard.BudgetDashboardUiTest"
./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.budget.BudgetJourneyUiTest"
```

## 5. Visual-Validation Evidence

### Captured Artifacts
- **Build Output Directory**: `build/reports/visual-validation/screenshots/`
- **Approved Durable Evidence**: `docs/evidence/screenshots/`
  - Mobile:
    - `mobile-budget-dashboard-populated.png` (categorized cards in on-track, near-limit, and over-budget states)
    - `mobile-budget-dashboard-uncategorized.png` (uncategorized transaction added; total spending increases without creating an uncategorized category card)
  - Desktop:
    - `desktop-budget-dashboard-populated.png` (3-column responsive category card grid with health badges and progress)
    - `desktop-budget-dashboard-uncategorized.png` (3-column card grid unaffected by uncategorized spending)

### Per-Screen Design-Alignment Matrix

| Screen / Journey | Mobile Reference (`pocket-budget-mobile-design/`) | Desktop Reference (`pocket-budget-desktop-design/`) | Required Layout Anchors | Approved Contract-Driven Omissions | Evidence Paths | Verdict |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Budget Dashboard (Category Health - Populated)** | `budget-listing.png` | `budget-progress.png` | Top header with title and month selector; Total Budget Used hero card with progress bar and remaining amount; Category Budgets section header; Category cards with icon avatars, category display names, spent of limit amounts, health status badges (On Track, Near Limit, Over Budget), visual progress bars, and remaining/over sub-rows. | Dashboard analytics charts, weekly trend graphs, and transaction history inline lists visible in mockups omitted per contract Section 3. | `docs/evidence/screenshots/mobile-budget-dashboard-populated.png`<br>`docs/evidence/screenshots/desktop-budget-dashboard-populated.png` | **Aligned** |
| **Budget Dashboard (Uncategorized Expense Isolation)** | `budget-listing.png` (isolation extension) | `budget-progress.png` (isolation extension) | Total budget spent increases by uncategorized amount ($150.00) while category budget cards remain completely identical with no uncategorized category card displayed. | Uncategorized category allocation card (forbidden by contract). | `docs/evidence/screenshots/mobile-budget-dashboard-uncategorized.png`<br>`docs/evidence/screenshots/desktop-budget-dashboard-uncategorized.png` | **Aligned** |

### Visual Comparison Against Design References

#### Mobile Viewport (390x844 dp)
| Captured Screen | Reference Mockup (`docs/design/reference/pocket-budget-mobile-design/`) | Visual Review Summary |
| :--- | :--- | :--- |
| `mobile-budget-dashboard-populated.png` | `pocket-budget-mobile-design/budget-listing.png` | Aligned. Month header shows "Monthly Budgets" with month selector. Total Budget card shows $2580.00 / $3000.00 with 86% Used and Near Limit badge. "Category Budgets" list renders cards for Groceries ($450.00 of $600.00, 75% Used, On Track), Transport ($160.00 of $200.00, 80% Used, Near Limit). Soft elevation and correct padding match mobile mockup. |
| `mobile-budget-dashboard-uncategorized.png` | `pocket-budget-mobile-design/budget-listing.png` | Aligned. Total budget spent increases from $2580.00 to $2730.00 (91% Used), while category budget cards remain identical and no Uncategorized row appears. |

#### Desktop Viewport (1024x768 dp)
| Captured Screen | Reference Mockup (`docs/design/reference/pocket-budget-desktop-design/`) | Visual Review Summary |
| :--- | :--- | :--- |
| `desktop-budget-dashboard-populated.png` | `pocket-budget-desktop-design/budget-progress.png` | Aligned. Sidebar navigation on left with active Budgets tab. Total Budget hero card with Near Limit badge and progress bar. "Category Budgets" title and subtitle "Monitor your monthly spending limits.". Responsive 3-column card grid with cards for Groceries, Transport, Entertainment, and Shopping. Each card features 32dp icon avatar, single-line title with ellipsis, health status badge, hero spent of limit amount, remaining/over sub-row, and 8dp progress bar. |
| `desktop-budget-dashboard-uncategorized.png` | `pocket-budget-desktop-design/budget-progress.png` | Aligned. Total budget spent increases to $2730.00 (91% Used), remaining $270.00 left, while category grid retains exact allocations and health states with zero uncategorized category cards. |

### Design Token Alignment
- **Colors**:
  - `PocketColors.Primary` (`#005136` - Forest green) used for on-track progress and positive remaining text.
  - `PocketColors.Expense` (`#BA1A1A` - Cardinal red) used for over-budget progress, text, and soft red badge background (`#FFCDD2`).
  - Warning/Near Limit (`#E65100` / `#FFF3E0`) used for near-limit progress bar and soft orange badge background.
  - `PocketColors.Surface` (`#EEFDF2`) and `PocketColors.SurfaceContainerLowest` (`#FFFFFF`) used for card containers.
- **Typography**:
  - Desktop category card titles: `14.sp` semi-bold with `TextOverflow.Ellipsis` and `maxLines = 1`.
  - Desktop hero amount: `22.sp` bold.
  - Mobile hero amount: `15.sp` semi-bold.
  - Health badges: `10.sp` (desktop) and `11.sp` (mobile) semi-bold.
- **Shapes & Spacing**:
  - Desktop cards: `20.dp` rounded corners with `16.dp` padding.
  - Mobile cards: `16.dp` rounded corners with `16.dp` padding.
  - Grid spacing: `16.dp` horizontal and vertical arrangement.

### Contract-Driven Deviations
- **Historical Charts and Analytics Graphs**: Desktop reference `budget-progress.png` contains trend line charts and historical category breakdown graphs. Omitted per contract Section 3 ("Category-budget charts, historical reporting, or analytics").
- **Add Category Card on Dashboard**: Reference mockups show quick action buttons on dashboard. Omitted per contract Section 3 ("Creating, editing, or removing category budgets from the dashboard").

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
- `./gradlew :shared:jvmTest --tests "org.hoods.pocket.domain.model.CategoryBudgetSummaryTest"`: `PASS`
- `./gradlew :shared:jvmTest --tests "org.hoods.pocket.domain.usecase.GetCategoryBudgetSummariesUseCaseTest"`: `PASS`
- `./gradlew :shared:jvmTest --tests "org.hoods.pocket.data.RoomTransactionRepositoryTest"`: `PASS`
- `./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.budget.dashboard.BudgetDashboardViewModelTest"`: `PASS`
- `./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.budget.dashboard.BudgetDashboardUiTest"`: `PASS`
- `./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.budget.BudgetJourneyUiTest"`: `PASS`
- `./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.VisualValidationCaptureTest"`: `PASS`
- `./gradlew :shared:jvmTest`: `PASS`
- `./gradlew :shared:testAndroidHostTest`: `PASS`
- `./gradlew :desktopApp:compileKotlin`: `PASS`
- `./gradlew :androidApp:assembleDebug`: `PASS`
- `./gradlew :shared:iosSimulatorArm64Test`: `NOT RUN` (host platform is macOS x86_64, whereas the iOS simulator target requires ARM64).
- `./scripts/verify.sh`: `PASS`

## 8. Limitations & Unverified Targets
- **iOS Simulator Tests**: `NOT RUN` (host platform is macOS x86_64, whereas the iOS simulator target requires ARM64).

## 9. Final Checkpoint
- **Status**: Ready for reviewer subagents and user review before staging and commit.
