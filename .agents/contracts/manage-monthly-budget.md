# Implementation Contract: Manage Monthly Budget

## 1. User Outcome

- A user can open the saved monthly budget for a selected month.
- A user can update the total budget limit for that month.
- Updating a budget changes the existing record rather than creating a duplicate.
- A user can request deletion of a monthly budget.
- The app asks for confirmation before deletion.
- Cancelling leaves the budget unchanged.
- Confirming deletion removes the budget and returns the dashboard to its no-budget state.
- The dashboard accurately shows spending, remaining amount, progress, and health status for the selected month.

## 2. In-Scope Behavior

- **Existing Budget Loading:** The set-budget screen loads the existing monthly budget for its selected month and year.
- **Budget Update:** Saving a changed limit updates the existing monthly budget record for that month and year.
- **Budget Summary Correctness:** The dashboard calculates total expense spending, remaining amount, progress, and health status accurately for the selected month.
- **Progress Calculation:** A budget of `10000L` cents with `2000L` cents of expense spending for the same month produces progress of `0.2`, remaining amount of `8000L`, and an on-track status.
- **Delete Monthly Budget:** A domain use case and repository operation remove the budget for the selected month and year.
- **Deletion with Confirmation:** The set-budget screen presents a confirmation dialog before deletion; cancelling leaves the budget untouched, confirming deletes it and returns to the dashboard.
- **No-Budget State:** After confirmed deletion, the budget dashboard displays its intentional no-budget state and a clear action to set a new budget.
- **Persistence Across Restarts:** Updated and deleted budget state remains accurate after the application restarts.
- **Typed Navigation:** Typed Compose navigation preserves the selected month when moving between the dashboard and set-budget screen.
- **Responsive Design:** The budget dashboard and set-budget screen preserve approved mobile and desktop layout anchors and are reviewed through the design-alignment matrix.

## 3. Explicit Exclusions

- Category budgets and category allocations.
- Budget carry-over between months.
- Budget history.
- Overspending notifications.
- Forecasting, reports, and analytics.
- Recurring budgets.
- Authentication, user accounts, and cloud synchronization.

## 4. Acceptance Criteria

- [ ] The budget summary correctly calculates total expense spending for the selected month.
- [ ] Income transactions do not count toward monthly spending.
- [ ] A budget of `10000L` cents with `2000L` cents of same-month expenses shows `0.2` progress and `8000L` remaining.
- [ ] A user can open an existing monthly budget for the selected month and year.
- [ ] The set-budget screen displays the existing limit as a normal decimal amount.
- [ ] Saving a changed limit updates the existing budget without creating a duplicate record.
- [ ] Invalid, blank, zero, and negative budget input is rejected with clear feedback.
- [ ] A user can request deletion from the set-budget screen.
- [ ] The app asks for confirmation before deletion.
- [ ] Cancelling deletion leaves the budget and dashboard unchanged.
- [ ] Confirming deletion removes the budget and returns to the no-budget dashboard state.
- [ ] Updated and deleted budget state remains correct after restarting the app.
- [ ] Mobile and desktop evidence is captured and reviewed using the design-alignment matrix.

## 5. Verification Plan

### Automated Behavior Tests

- **Test-First Evidence:** Provide genuine RED and GREEN evidence for the known progress-calculation defect before implementing lifecycle behaviour.
- **Budget Summary Tests:** Cover expense-only spending, remaining amount, progress, health status, no-budget state, and month isolation.
- **Lifecycle Tests:** Cover loading an existing budget, updating without duplication, invalid input, cancelling deletion, and confirmed deletion.
- **User Journey Test:** Cover set budget → dashboard → edit budget → cancel deletion → confirm deletion → no-budget dashboard state.

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

- Capture the budget dashboard, set-budget screen, edit state, and delete-confirmation state at mobile and desktop viewport sizes.
- Review each capture against its matching design reference and the design-alignment matrix.
- Record one of these results for each screen: `Aligned`, `Approved deviation`, or `Correction required`.

### Manual Verification Journey

1. Launch the app on Android or Desktop.
2. Open a month with an existing budget.
3. Confirm that spending, remaining amount, progress, and health status are correct.
4. Open the existing budget and change its limit.
5. Save the change and verify the dashboard updates without creating a duplicate budget.
6. Reopen the budget, request deletion, and cancel; verify it remains unchanged.
7. Request deletion again and confirm; verify the dashboard shows the no-budget state.
8. Close and reopen the application; verify the updated or deleted state remains accurate.

## 6. Constraints & Rules

- **Monetary Values:** Monetary values must be stored and handled as `Long` integer cents. Do not use `Double` or `Float` for money.
- **Progress Values:** `Float` may be used only as a non-monetary display ratio. Money amounts and budget-status decisions must be based on exact `Long` values.
- **Date and Time:** Use validated month and year values to identify a monthly budget. Transaction dates use `kotlin.time.Instant`.
- **Data Filtering:** Month and transaction-type filtering belongs in DAO or repository queries, not only in presentation state.
- **Source Set Scoping:** Shared domain models, repository contracts, Room KMP data code, use cases, ViewModels, and shared Compose UI belong in `shared/src/commonMain/`.
- **Presentation Architecture:** Each budget screen follows the existing ViewModel, `UiState`, stateful screen, and stateless content pattern.
- **Navigation:** Use typed Compose Navigation routes only. Do not introduce string routes.
- **Design Alignment:** Follow the design-system skill and UI-flow-testing workflow.
- **Test-First Workflow:** Follow `.agents/workflows/test-first.md` for new observable domain and data logic.

## 7. Authority Boundaries

- **File Modifications:** Limited to application code, tests, Gradle configuration, and project-control files necessary for this vertical slice.
- **Git Commits:** Create an atomic commit only when explicitly requested and approved by the user.
- **Git Remotes:** Do not push, configure, or otherwise alter Git remotes.
- **External Services:** Do not add cloud services, authentication, or external network dependencies.
