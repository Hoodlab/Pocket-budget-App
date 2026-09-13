# Implementation Contract: Category Budgets

## 1. User Outcome

- A user can allocate part of a monthly total budget to an expense category.
- A user can see how much of the total monthly budget remains available for category allocation.
- A user cannot allocate more across categories than the total monthly budget.
- A user can update an existing category allocation without creating a duplicate.
- A user can remove an existing category allocation.
- Category allocations remain correct after restarting the app.
- Category allocations for one month do not appear in another month.

## 2. In-Scope Behavior

- **`CategoryBudget` Domain Model:** A category budget represents a limit for one eligible expense category in one month and year.
- **Expense Categories Only:** Only predefined expense categories can receive a category budget. Income categories and `Uncategorized` are not eligible.
- **`CategoryBudgetRepository` Contract:** Shared repository interface defining operations to save, update, remove, and reactively observe category budgets for a month and year.
- **Domain Use Cases:** Domain use cases validate category eligibility, month and year values, positive allocation amounts, and the total-allocation limit before persisting changes.
- **Total Allocation Rule:** The combined limit of all category budgets for a month cannot exceed that month’s total monthly budget.
- **Allocation Update Rule:** Updating an existing category allocation evaluates the new total correctly by replacing that allocation rather than adding a duplicate amount.
- **Local Room Persistence:** Shared Room Multiplatform entity, DAO, database definition, migration, and repository implementation store category budgets locally.
- **Set Budget Flow:** The existing set-budget screen allows the user to add, update, and remove category allocations for the selected month.
- **Remaining Allocation Feedback:** The user sees the amount of the total monthly limit that has not yet been assigned to categories.
- **Validation Feedback:** The user receives clear feedback when an allocation is invalid or exceeds the remaining amount.
- **Persistence Across Restarts:** Saved, updated, and removed category allocations remain accurate after the application restarts.
- **Month Isolation:** The set-budget screen displays only category allocations for its selected month and year.
- **Responsive Design:** Category-allocation controls follow the project design system and preserve the approved mobile and desktop layout anchors.

## 3. Explicit Exclusions

- Category spending, category progress, and category health on the dashboard.
- Automatic budget-allocation suggestions.
- Budget carry-over between months.
- Custom category creation, editing, reordering, or deletion.
- Category charts, reports, forecasting, and notifications.
- Authentication, user accounts, and cloud synchronization.

## 4. Acceptance Criteria

- [ ] A category budget belongs to one valid expense category, month, and year.
- [ ] A user can add a valid category allocation when a total monthly budget exists.
- [ ] Income categories cannot receive a category budget.
- [ ] `Uncategorized` cannot receive a category budget.
- [ ] Category budget limits are stored and calculated as `Long` integer cents.
- [ ] The combined category allocation never exceeds the total monthly budget.
- [ ] An invalid, blank, zero, negative, or over-limit allocation is rejected with clear feedback.
- [ ] Updating an existing category allocation changes that allocation without creating a duplicate.
- [ ] Removing an allocation removes only that category budget.
- [ ] The user can see the correct remaining unallocated amount for the selected month.
- [ ] Category allocations persist correctly after the app restarts.
- [ ] Category allocations remain isolated to their own month and year.
- [ ] Existing transaction categories, total monthly budgets, and budget-lifecycle behaviour remain intact.
- [ ] Mobile and desktop evidence is captured and reviewed using the design-alignment matrix.

## 5. Verification Plan

### Automated Behavior Tests

- **Test-First Evidence:** Provide genuine RED and GREEN evidence for category-budget validation and persistence behaviour.
- **Domain Tests:** Cover eligible and ineligible categories, valid allocation, zero and negative values, over-allocation, updating an existing allocation, removing an allocation, and month isolation.
- **Repository and Migration Tests:** Verify category budgets save, update, remove, and retrieve correctly after the Room migration.
- **ViewModel Tests:** Cover remaining-allocation calculation, valid input, invalid input, over-allocation feedback, update, and removal states.
- **User Journey Test:** Cover total budget → valid category allocation → attempted over-allocation → corrected allocation → update → remove → restart verification.

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

- Capture the set-budget screen with valid allocations, remaining allocation feedback, and an over-allocation error at mobile and desktop viewport sizes.
- Review each capture against its matching design reference and the design-alignment matrix.
- Record one of these results for each screen: `Aligned`, `Approved deviation`, or `Correction required`.

### Manual Verification Journey

1. Launch the app on Android or Desktop.
2. Open a month with a total monthly budget.
3. Add a valid expense-category allocation.
4. Verify the remaining amount updates correctly.
5. Attempt to allocate more than the remaining amount and verify clear feedback.
6. Correct the allocation and save it.
7. Update the saved allocation and verify no duplicate category budget appears.
8. Remove the allocation and verify the remaining amount updates.
9. Move to another month and verify its allocations are separate.
10. Close and reopen the app, then verify the selected month’s category allocations remain accurate.

## 6. Constraints & Rules

- **Monetary Values:** Monetary values and allocation calculations must use `Long` integer cents. Do not use `Double` or `Float` for money.
- **Domain Enforcement:** Category eligibility and total-allocation limits must be enforced by domain use cases, not only disabled controls or UI validation.
- **Existing Monthly Budget:** Category allocations require a saved total monthly budget for the same month and year.
- **Data Filtering:** Month and category filtering belongs in DAO or repository queries, not only in ViewModel state.
- **Database Migration:** New persisted category-budget data requires an explicit, tested Room migration. Do not use destructive migration.
- **Source Set Scoping:** Shared domain models, repository contracts, Room KMP data code, use cases, ViewModels, and shared Compose UI belong in `shared/src/commonMain/`.
- **Presentation Architecture:** Each screen follows the existing ViewModel, `UiState`, stateful screen, and stateless content pattern.
- **Navigation:** Use typed Compose Navigation routes only. Do not introduce string routes.
- **Design Alignment:** Follow the design-system skill and UI-flow-testing workflow.
- **Test-First Workflow:** Follow `.agents/workflows/test-first.md` for new observable domain, data, migration, and presentation logic.

## 7. Authority Boundaries

- **File Modifications:** Limited to application code, tests, Gradle configuration, and project-control files necessary for this vertical slice.
- **Git Commits:** Create an atomic commit only when explicitly requested and approved by the user.
- **Git Remotes:** Do not push, configure, or otherwise alter Git remotes.
- **External Services:** Do not add cloud services, authentication, or external network dependencies.
