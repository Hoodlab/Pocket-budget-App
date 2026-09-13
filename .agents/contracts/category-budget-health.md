# Implementation Contract: Category Budget Health

## 1. User Outcome

- A user can see spending against each saved category budget for the selected month.
- A user can see the category limit, spent amount, remaining amount, progress, and health status.
- A user can understand whether a category is on track, near its limit, or over budget.
- Category health updates when a transaction is added, edited, recategorised, or deleted.
- Category health updates when a category allocation is updated or removed.
- Category health for one month does not appear in another month.
- Overall monthly budget health remains correct.

## 2. In-Scope Behavior

- **Category Spending Query:** The application retrieves expense spending grouped by category for the selected month at the DAO or repository boundary.
- **Expense Transactions Only:** Only `EXPENSE` transactions contribute to category spending. Income transactions do not affect category-budget health.
- **Category Budget Summary:** Each saved category budget provides its category, limit, spent amount, remaining amount, progress, and health status.
- **Health Status:** A category budget is classified as `ON_TRACK`, `NEAR_LIMIT`, or `OVER_BUDGET` using the project’s existing budget-health policy.
- **Reactive Updates:** Category summaries update when a relevant transaction or category allocation changes.
- **Unallocated Categories:** Expense categories without a category budget do not display a category-budget health row.
- **Uncategorised Spending:** `Uncategorized` expense transactions count toward overall monthly spending but do not create a category-budget health row.
- **Month Isolation:** Category-budget summaries display only data belonging to the selected month and year.
- **Budget Dashboard UI:** The existing budget dashboard displays category-budget health alongside the existing overall monthly budget summary.
- **Responsive Design:** The category-budget health display follows the project design system and preserves the approved mobile and desktop layout anchors.

## 3. Explicit Exclusions

- Creating, editing, or removing category budgets from the dashboard.
- Category-budget charts, historical reporting, or analytics.
- Budget recommendations, forecasting, notifications, or carry-over.
- Income-category budgets or income-category health.
- Custom category management.
- Authentication, user accounts, and cloud synchronization.

## 4. Acceptance Criteria

- [ ] Category spending is calculated only from `EXPENSE` transactions.
- [ ] Category spending includes only transactions from the selected month and year.
- [ ] A transaction contributes spending only to its own saved category.
- [ ] Each saved category budget displays its category limit, spent amount, remaining amount, progress, and health status.
- [ ] Category health status follows the established on-track, near-limit, and over-budget policy.
- [ ] Monetary amounts and health-status decisions use exact `Long` integer cents.
- [ ] `Float` is used only as a non-monetary visual progress ratio.
- [ ] Adding an expense updates spending for the correct category.
- [ ] Editing a transaction’s category moves its spending from the old category to the new category.
- [ ] Deleting an expense updates the correct category summary.
- [ ] Updating or removing a category allocation updates the relevant category summary.
- [ ] Income transactions do not affect category-budget health.
- [ ] `Uncategorized` expenses affect overall monthly spending but do not create a category-budget row.
- [ ] Expense categories without an allocation do not create a category-budget row.
- [ ] Category summaries remain isolated to their selected month and year.
- [ ] Mobile and desktop evidence is captured and reviewed using the design-alignment matrix.

## 5. Verification Plan

### Automated Behavior Tests

- **Test-First Evidence:** Provide genuine RED and GREEN evidence for grouped category spending and category-budget summary behaviour.
- **Data and Repository Tests:** Cover expense-only aggregation, category grouping, selected-month filtering, and empty category-spending results.
- **Domain Tests:** Cover category limit, spent amount, remaining amount, progress, health status, and month isolation.
- **ViewModel Tests:** Cover loaded category summaries, empty category-budget state, and reactive updates after changed data.
- **User Journey Test:** Cover categorised expense → category health update → transaction category change → spending moves to the correct category → transaction deletion → health updates.

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

- Capture the budget dashboard with category budgets in on-track, near-limit, and over-budget states at mobile and desktop viewport sizes.
- Capture the dashboard with an `Uncategorized` expense to verify overall spending changes without an incorrect category-budget row.
- Review each capture against its matching design reference and the design-alignment matrix.
- Record one of these results for each screen: `Aligned`, `Approved deviation`, or `Correction required`.

### Manual Verification Journey

1. Launch the app on Android or Desktop.
2. Open a month with a total budget and at least one category allocation.
3. Add an expense assigned to that category.
4. Verify its category spent amount, remaining amount, progress, and health status update.
5. Edit the transaction and change its category.
6. Verify spending moves from the original category to the new category.
7. Delete the transaction and verify category health updates again.
8. Add an income transaction and verify category spending does not change.
9. Add an `Uncategorized` expense and verify overall spending changes without creating a category-budget row.
10. Move to another month and verify category health remains isolated.
11. Review the dashboard on mobile and desktop.

## 6. Constraints & Rules

- **Monetary Values:** Monetary values and category-budget calculations must use `Long` integer cents. Do not use `Double` or `Float` for money.
- **Health Decisions:** Health-status decisions must use exact `Long` comparisons. `Float` may be used only for visual progress ratios.
- **Data Filtering:** Selected-month, transaction-type, and category aggregation belong in DAO or repository queries, not in-memory ViewModel filtering.
- **Reactive Data:** Category-budget health must update from saved transaction and category-budget data without manual UI refresh logic.
- **Source Set Scoping:** Shared domain models, repository contracts, Room KMP data code, use cases, ViewModels, and shared Compose UI belong in `shared/src/commonMain/`.
- **Presentation Architecture:** Each screen follows the existing ViewModel, `UiState`, stateful screen, and stateless content pattern.
- **Navigation:** Use typed Compose Navigation routes only. Do not introduce string routes.
- **Design Alignment:** Follow the design-system skill and UI-flow-testing workflow.
- **Test-First Workflow:** Follow `.agents/workflows/test-first.md` for new observable domain, data, aggregation, and presentation logic.

## 7. Authority Boundaries

- **File Modifications:** Limited to application code, tests, Gradle configuration, and project-control files necessary for this vertical slice.
- **Git Commits:** Create an atomic commit only when explicitly requested and approved by the user.
- **Git Remotes:** Do not push, configure, or otherwise alter Git remotes.
- **External Services:** Do not add cloud services, authentication, or external network dependencies.
