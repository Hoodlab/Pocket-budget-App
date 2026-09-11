# Implementation Contract: Set and View Monthly Budget

## 1. User Outcome

- A user can set a total monthly spending limit for a selected month.
- A user can view the saved monthly budget after restarting the app.
- A user can see the total amount spent by expense transactions in that selected month.
- A user can see the remaining budget amount and progress toward the monthly limit.
- Income transactions do not count as monthly spending.
- A user can move between months and see the budget state that belongs to each month.

## 2. In-Scope Behavior

- **`BudgetRepository` Contract:** Shared repository interface defining operations to save and reactively observe a monthly budget for a given month and year.
- **Domain Use Cases:**
  - `SetMonthlyBudgetUseCase`: Validates and saves a valid monthly budget.
  - A use case that provides the selected month’s budget summary, including total limit, expense spending, remaining amount, and progress.
- **Monthly Budget Domain Model:** A pure shared model representing a total budget limit for one month and year.
- **Local Room Persistence:** Shared Room Multiplatform entity, DAO, database definition, and repository implementation for monthly budget storage.
- **Monthly Expense Query:** A repository or DAO query that retrieves expense spending for the selected month at the data boundary. It must not load all transactions and filter them only in a ViewModel.
- **Budget Dashboard UI:** A Compose Multiplatform dashboard displaying the selected month, total budget, spent amount, remaining amount, and progress.
- **Set Budget Screen:** A Compose Multiplatform form allowing the user to enter a normal decimal amount, such as `1200.00`, which converts to `Long` cents before storage.
- **Empty Budget State:** When no budget exists for the selected month, the dashboard clearly explains this and provides an action to set one.
- **Month Browsing:** Users can move to the previous or next month and return to the current month from the budget dashboard.
- **Persistence Across Restarts:** Stored monthly budgets remain available in SQLite/Room across app launches.
- **Typed Navigation:** Typed Compose navigation handles transitions between the budget dashboard and set-budget screen.
- **Responsive Design:** The budget dashboard and set-budget screen follow the approved mobile and desktop design references, using the design-alignment matrix before being marked complete.

## 3. Explicit Exclusions

- Editing or deleting an existing monthly budget.
- Category budgets or category allocations.
- Budget carry-over between months.
- Overspending notifications.
- Forecasting, reports, and analytics.
- Recurring budgets.
- Authentication, user accounts, and cloud synchronization.

## 4. Acceptance Criteria

- [ ] A monthly budget is associated with one valid month and year.
- [ ] A user can enter a normal decimal amount, such as `1200.00`, and the application stores it as `Long` integer cents.
- [ ] Zero, negative, blank, and invalid budget amounts are rejected with clear feedback.
- [ ] `BudgetRepository` supports saving and observing a monthly budget for the requested month and year.
- [ ] Room stores and retrieves a monthly budget across application restarts.
- [ ] The budget dashboard shows the selected month’s total limit.
- [ ] The budget dashboard counts only `EXPENSE` transactions in that month as spending.
- [ ] Income transactions do not affect spent amount, remaining amount, or progress.
- [ ] The dashboard accurately shows spent amount, remaining amount, and progress.
- [ ] A month with no saved budget shows an intentional empty state and a clear action to set one.
- [ ] Moving between months shows the correct budget and spending state for each month.
- [ ] The budget dashboard and set-budget screen preserve their intended mobile and desktop layout anchors.
- [ ] Design-reference omissions are recorded as approved deviations rather than being described as visual alignment.

## 5. Verification Plan

### Automated Behavior Tests

- **Test-First Evidence:** Provide genuine RED and GREEN evidence for new monthly-budget domain, repository, and summary behaviour.
- **Budget Rules:** Test valid creation, invalid amounts, month/year boundaries, expense-only spending, remaining amount, progress, and no-budget state.
- **Persistence:** Test Room storage and retrieval for the correct month and year.
- **Presentation:** Test dashboard and set-budget ViewModel states, including invalid input and successful save.
- **User Journey:** Add one JVM UI-flow or journey test in which a user sets a budget and returns to a dashboard showing the saved result.

### Multiplatform Test Suite

- Common / JVM unit tests:

  ```bash
  ./gradlew :shared:jvmTest
  ```

- Android host unit tests:

  ```bash
  ./gradlew :shared:testAndroidHostTest
  ```

Any target not executed must be recorded truthfully as `NOT RUN` with its real reason.

### Build and Compilation Checks

```bash
./gradlew :desktopApp:compileKotlin
./gradlew :androidApp:assembleDebug
./scripts/verify.sh
```

### Visual Validation

- Capture and review the budget dashboard and set-budget screen at mobile and desktop viewport sizes.
- Review each capture against its matching design reference and the design-alignment matrix.
- Record one of these results for each screen: `Aligned`, `Approved deviation`, or `Correction required`.

### Manual Verification Journey

1. Launch the app on Android or Desktop.
2. Navigate to the budget dashboard for the current month.
3. Confirm the no-budget state explains that no monthly limit has been set.
4. Choose the action to set a budget.
5. Enter a valid amount, such as `$1,200.00`, and save it.
6. Return to the dashboard and verify that the saved limit appears.
7. Verify that expense transactions from the selected month contribute to spent amount.
8. Verify that income transactions do not contribute to spent amount.
9. Move to another month and verify its separate budget state.
10. Close and reopen the app, then verify the saved budget remains available for its original month.

## 6. Constraints & Rules

- **Monetary Values:** Monetary values must be stored and handled as `Long` integer cents. Do not use `Double` or `Float` for money.
- **Date and Time:** Use `kotlin.time.Instant` for transaction dates. Represent a budget period clearly with validated month and year values.
- **Data Filtering:** Date- and type-based transaction filtering belongs in DAO or repository queries, not only in presentation state.
- **Source Set Scoping:** Shared domain models, repository contracts, Room KMP data code, use cases, ViewModels, and shared Compose UI belong in `shared/src/commonMain/`.
- **Presentation Architecture:** Each budget screen follows the existing ViewModel, `UiState`, stateful screen, and stateless content pattern.
- **Navigation:** Use typed Compose Navigation routes only. Do not introduce string routes.
- **Design Alignment:** Follow the design-system skill and UI-flow-testing workflow. A matching colour palette alone does not count as desktop or mobile alignment.
- **Test-First Workflow:** Follow `.agents/workflows/test-first.md` for new observable domain and data logic.

## 7. Authority Boundaries

- **File Modifications:** Limited to application code, tests, Gradle configuration, and project-control files necessary for this vertical slice.
- **Git Commits:** Create an atomic commit only when explicitly requested and approved by the user.
- **Git Remotes:** Do not push, configure, or otherwise alter Git remotes.
- **External Services:** Do not add cloud services, authentication, or external network dependencies.
