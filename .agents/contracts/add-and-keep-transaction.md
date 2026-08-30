# Implementation Contract: Add and Keep Transaction

## 1. User Outcome
A user can enter a valid income or expense, save it locally, see it in the transaction list, and still see it after restarting the app.

## 2. In-Scope Behavior
- **`TransactionRepository` Contract:** Shared repository interface defining operations to save transactions and provide stored transactions reactively.
- **`AddTransactionUseCase`:** Domain use case that validates transaction invariants and persists valid transactions through the repository.
- **Local Room Persistence:** Shared Room Multiplatform database entity, DAO, and database definition for local transaction storage.
- **Runtime Wiring:** Platform/common dependency factory or runtime wiring to provide the database, repository, and use case instances.
- **Add-Transaction Form:** Compose Multiplatform input form allowing users to enter a standard monetary amount (e.g., `45.50`), description, and type (`INCOME` / `EXPENSE`), which converts the entered amount into `Long` integer cents (e.g., `4550L`) upon submission.
- **Transaction List UI:** Compose Multiplatform list view displaying saved transactions with amount, type, description, and date.
- **Persistence Across Restarts:** Stored transactions are retained locally in SQLite/Room across app launches.

## 3. Explicit Exclusions
- Edit and delete transaction capabilities.
- Budgets, budget allocation, and progress tracking.
- Category management or custom categories.
- Date picker component (dates use current `kotlin.time.Instant` / default timestamp).
- Typed navigation frameworks and complex navigation graphs.
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

## 5. Verification Plan

### Automated Behavior Tests
- **Test-First Evidence:** Provide genuine RED (failing before implementation) and GREEN (passing after implementation) evidence for new behavior in domain use cases and repository contracts.
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

### Manual Verification Journey (Add, List, and Restart)
1. Launch the app on Android or Desktop.
2. Enter an income transaction (e.g., "Salary", `$3000.00`) via the form and submit.
3. Enter an expense transaction (e.g., "Groceries", `$45.50`) via the form and submit.
4. Verify both transactions appear immediately in the transaction list with formatted amounts.
5. Close/terminate the app process.
6. Reopen the application and verify that both transactions remain present in the list with correct amounts, descriptions, types, and timestamps.

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
