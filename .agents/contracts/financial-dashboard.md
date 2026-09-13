# Implementation Contract: Financial Dashboard

## 1. User Outcome

- A user can see total income for the selected month.
- A user can see total spending for the selected month.
- A user can see net balance for the selected month.
- A user can see the selected month’s overall budget limit, spent amount, remaining amount, progress, and health status.
- A user can see category-budget health beneath the overall dashboard summaries.
- Dashboard values update when a transaction, category, total budget, or category allocation changes.
- Dashboard values remain isolated to the selected month.

## 2. In-Scope Behavior

- **Cash Flow Overview:** The dashboard displays:
  - Monthly income from `INCOME` transactions.
  - Monthly spending from `EXPENSE` transactions.
  - Net balance calculated as monthly income minus monthly spending.
- **Selected-Month Filtering:** Cash-flow values, total-budget health, and category-budget health use the dashboard’s selected month and year.
- **Overall Budget Health:** When a total monthly budget exists, the dashboard displays its limit, total spending, remaining amount, progress, and health status.
- **No-Budget State:** When no total monthly budget exists for the selected month, the dashboard presents a clear no-budget state and an action to set one.
- **Category Budget Health:** Existing category-budget health summaries appear as supporting dashboard content beneath the cash-flow and overall-budget summaries.
- **Reactive Updates:** The dashboard reacts to saved changes in transactions, transaction categories, total monthly budgets, and category budgets without manual refresh logic.
- **Domain Summary Reuse:** Existing total-budget and category-budget summary logic is reused where it already provides the required domain facts. The dashboard does not create duplicate financial calculations.
- **Responsive Design:** The dashboard follows the project design system and preserves approved mobile and desktop layout anchors.

## 3. Explicit Exclusions

- Historical cash-flow charts.
- Forecasting, recommendations, and notifications.
- Reports and data export.
- Bank accounts, account balances, bank connections, or imports.
- Editing total or category budgets directly from dashboard summary cards.
- Authentication, user accounts, and cloud synchronization.

## 4. Acceptance Criteria

- [ ] Monthly income includes only `INCOME` transactions from the selected month.
- [ ] Monthly spending includes only `EXPENSE` transactions from the selected month.
- [ ] Net balance equals monthly income minus monthly spending.
- [ ] Income, spending, and net balance use exact `Long` integer cents.
- [ ] Transaction data from another month does not affect the selected month’s dashboard.
- [ ] Overall budget health displays the selected month’s total limit, spent amount, remaining amount, progress, and status when a budget exists.
- [ ] The no-budget state is clear and provides a route to set a budget.
- [ ] Existing category-budget health is visible as supporting dashboard content.
- [ ] Adding, editing, recategorising, or deleting a transaction updates the relevant dashboard values.
- [ ] Updating or removing a category allocation updates the relevant dashboard values.
- [ ] The dashboard does not store derived income, spending, net-balance, or health values as independent persisted records.
- [ ] The mobile dashboard preserves mobile hierarchy and interaction placement.
- [ ] The desktop dashboard preserves desktop shell, summary-card, and budget-health layout anchors.
- [ ] Mobile and desktop evidence is captured and reviewed using the design-alignment matrix.

## 5. Verification Plan

### Automated Behavior Tests

- **Test-First Evidence:** Provide genuine RED and GREEN evidence for the missing cash-flow summary behaviour.
- **Data and Repository Tests:** Cover selected-month income totals, expense totals, net-balance inputs, and month isolation.
- **Domain Tests:** Cover net balance, total-budget health, no-budget state, and reuse of category-budget health summaries.
- **ViewModel Tests:** Cover loaded dashboard state, no-budget state, selected-month changes, and reactive updates after saved data changes.
- **User Journey Test:** Cover add income → add expense → verify cash flow → set or update budget → verify overall health → recategorise expense → verify category health → delete expense → verify all summaries update.

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

- Capture the dashboard with representative income, spending, net balance, total budget health, and category-budget health at mobile and desktop viewport sizes.
- Capture the intentional no-budget state at both viewport sizes.
- Review each capture against its matching design reference and the design-alignment matrix.
- Record one of these results for each screen: `Aligned`, `Approved deviation`, or `Correction required`.

### Manual Verification Journey

1. Launch the app on Android or Desktop.
2. Select a month and add an income transaction.
3. Add one or more expense transactions in that month.
4. Verify income, spending, and net balance are correct.
5. Set or update a total monthly budget.
6. Verify overall budget-health values are correct.
7. Add or update a category allocation and verify category-budget health appears correctly.
8. Edit an expense and change its category; verify overall cash flow remains correct and category health moves correctly.
9. Delete an expense and verify cash flow, overall budget health, and category health update.
10. Browse another month and verify all dashboard values are isolated.
11. Review the dashboard on mobile and desktop.

## 6. Constraints & Rules

- **Monetary Values:** Income, spending, net balance, budget limits, and remaining amounts must use `Long` integer cents. Do not use `Double` or `Float` for money.
- **Health Decisions:** Budget-health decisions must use exact `Long` comparisons. `Float` may be used only as a non-monetary visual progress ratio.
- **Data Filtering:** Selected-month and transaction-type filtering belongs in DAO or repository queries, not in-memory ViewModel filtering.
- **Derived Values:** Do not persist cash-flow totals, net balance, progress, or health summaries as separate database records.
- **Domain Summary Reuse:** Reuse existing total-budget and category-budget domain summaries where they already provide the required facts. Do not duplicate financial rules in the dashboard ViewModel or UI.
- **Source Set Scoping:** Shared domain models, repository contracts, Room KMP data code, use cases, ViewModels, and shared Compose UI belong in `shared/src/commonMain/`.
- **Presentation Architecture:** The dashboard follows the existing ViewModel, `UiState`, stateful screen, and stateless content pattern.
- **Navigation:** Use typed Compose Navigation routes only. Do not introduce string routes.
- **Design Alignment:** Follow the design-system skill and UI-flow-testing workflow.
- **Test-First Workflow:** Follow `.agents/workflows/test-first.md` for new observable domain, data, aggregation, and presentation logic.

## 7. Authority Boundaries

- **File Modifications:** Limited to application code, tests, Gradle configuration, and project-control files necessary for this vertical slice.
- **Git Commits:** Create an atomic commit only when explicitly requested and approved by the user.
- **Git Remotes:** Do not push, configure, or otherwise alter Git remotes.
- **External Services:** Do not add cloud services, authentication, or external network dependencies.
