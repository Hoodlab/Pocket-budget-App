# Implementation Contract: Add and Keep Transaction

## 1. User Outcome
- A user can enter a valid income or expense, save it locally, see it in the transaction list, and still see it after restarting the app.
- A user can select a transaction from the list and open it for editing.
- The edit form displays the stored description, amount, type, and date.
- Saving changes updates the existing transaction rather than inserting another record.
- A user can request deletion from the edit screen.
- The app asks for confirmation before deletion.
- Cancelling leaves the transaction unchanged.
- Confirming removes the transaction and returns to the list.
- A user can choose the date for a new or existing transaction, browse transactions by month, and return to the current month. Transactions appear only in the month that contains their selected date.

## 2. In-Scope Behavior
- **`TransactionRepository` Contract:** Shared repository interface defining operations to save, update, delete, and reactively observe transactions.
- **Domain Use Cases:**
  - `AddTransactionUseCase`: Validates transaction invariants and persists valid transactions through the repository.
  - Edit/Update and Delete use cases or repository operations supporting updating an existing transaction and deleting a transaction.
- **Local Room Persistence:** Shared Room Multiplatform database entity, DAO, and database definition for local transaction storage supporting insert, update, and delete operations.
- **Runtime Wiring:** Platform/common dependency factory or runtime wiring to provide the database, repository, and use case instances.
- **Add & Edit Transaction Forms:**
  - Compose Multiplatform add form allowing users to enter a standard monetary amount (e.g., `45.50`), description, and type (`INCOME` / `EXPENSE`), which converts the entered amount into `Long` integer cents (e.g., `4550L`) upon submission.
  - Compose Multiplatform edit form populated with stored description, amount, type, and date, supporting saving updates to the existing record.
- **Transaction List UI:** Compose Multiplatform list view displaying saved transactions with amount, type, description, and date, supporting row selection to open the edit screen.
- **Deletion with Confirmation:** Deletion trigger on the edit screen presenting a confirmation dialog before permanent removal; cancelling leaves the transaction untouched, confirming deletes it and navigates back to the list.
- **Persistence Across Restarts:** Stored transactions and updates are retained locally in SQLite/Room across app launches.
- **Typed Navigation:** Typed Compose navigation handling transitions between transaction list, add transaction, and edit transaction screens.
- **Default Transaction Date:** New transactions default to today using `kotlin.time.Instant`.
- **Date Selection:** Users can choose a date when adding or editing a transaction.
- **Stored-Date Editing:** Editing pre-fills the transaction’s existing saved date.
- **Month Browsing:** Users can move to the previous or next month from the transaction list.
- **Current-Month Return:** Users can return directly to the current month after browsing another period.
- **Monthly Transaction List:** The list displays only transactions whose date belongs to the selected month and year.
- **Date-Based Movement:** Updating a transaction’s date moves it to the appropriate month in the list.

## 3. Explicit Exclusions
- Budgets, budget allocation, and progress tracking.
- Category management or custom categories.
- Date picker component (dates use current `kotlin.time.Instant` / default timestamp; edit form displays stored date).
- Authentication, user accounts, and cloud synchronization.
- Recurring transactions.

## 4. Acceptance Criteria
- [ ] `TransactionRepository` interface contract supports saving a transaction and reactively observing/providing stored transactions.
- [ ] `AddTransactionUseCase` rejects invalid transaction data and successfully delegates valid transactions to the repository.
- [ ] Room database stores transactions and recovers all records across app process restarts.
- [ ] User can enter description, a normal decimal amount (e.g., `45.50`), select Income or Expense, and submit via the UI form.
- [ ] The form converts user-entered decimal amounts to `Long` integer cents before storing; users are never asked to enter raw cents.
- [ ] Adding a transaction updates the transaction list reactively.
- [ ] Restarting the application preserves and renders previously entered transactions.
- [ ] A user can select a transaction from the list and open it for editing.
- [ ] The edit form displays the stored description, amount, type, and date.
- [ ] Saving changes updates the existing transaction rather than inserting another record.
- [ ] A user can request deletion from the edit screen.
- [ ] The app asks for confirmation before deletion.
- [ ] Cancelling leaves the transaction unchanged.
- [ ] Confirming removes the transaction and returns to the list.

## 5. Verification Plan

### Automated Behavior Tests
- **Test-First Evidence:** Provide genuine RED (failing before implementation) and GREEN (passing after implementation) evidence for new behavior in domain use cases and repository contracts (including update and delete operations).
- **Multiplatform Test Suite:**
  - Common / JVM unit tests:
    ```bash
    ./gradlew :shared:jvmTest
    ```
  - Android host unit tests:
    ```bash
    ./gradlew :shared:testAndroidHostTest
    ```
  *(Exact source sets and additional platform-specific test tasks will be confirmed in the discovery step. Any tasks not executed will be recorded truthfully as NOT RUN.)*

### Build & Compilation Checks
- **Platform Packaging & Assembly:**
  ```bash
  ./gradlew :androidApp:assembleDebug
  ```
  *(Provides compilation and packaging evidence; does not substitute for behavioral persistence tests.)*

### Manual Verification Journey
1. Launch the app on Android or Desktop.
2. Enter an income transaction (e.g., "Salary", `$3000.00`) via the form and submit.
3. Enter an expense transaction (e.g., "Groceries", `$45.50`) via the form and submit.
4. Verify both transactions appear immediately in the transaction list with formatted amounts.
5. Select a transaction from the list; verify the edit form opens populated with stored description, amount, type, and date.
6. Modify fields (e.g., change amount or description) and save; verify the existing transaction updates in the list without creating a duplicate record.
7. Open the transaction again, tap delete, and cancel the confirmation dialog; verify the transaction remains intact in the list.
8. Tap delete again and confirm deletion; verify the transaction is permanently removed from the list.
9. Close/terminate the app process, reopen, and verify persisted state accurately reflects the additions, edits, and deletions.

## 6. Constraints & Rules
- **Monetary Values:** Must strictly be stored and handled as `Long` integer cents in domain and data models (no `Double` or `Float` for money representation).
- **Date & Time:** Must use `kotlin.time.Instant`.
- **Source Set Scoping:** Common domain, repository contracts, Room KMP database/DAOs, use cases, and shared Compose UI components belong in `shared/src/commonMain/`.
- **Test-First Workflow:** Follow `.agents/workflows/test-first.md` for new observable domain and data logic.

## 7. Authority Boundaries
- **File Modifications:** Limited strictly to application code, tests, and Gradle configurations necessary for this vertical slice.
- **Git Commits:** Atomic commits created only when explicitly requested and approved by the user.
- **Git Remotes:** No push operations or remote configuration alterations.
- **External Services:** No remote cloud dependencies or external network services.
