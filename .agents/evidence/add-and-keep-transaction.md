# Task Evidence: Add and Keep Transaction

## 1. Outcome
A user can enter a valid income or expense with standard decimal text through the Compose Multiplatform UI, save it locally to SQLite/Room across Android, Desktop, and iOS, view it reactively in the transaction list with signed currency formatting, and retain all transaction records across application restarts.

## 2. Scope
### In-Scope Deliverables
- **`TransactionRepository` Contract**: Shared repository interface in domain layer (`shared/src/commonMain/kotlin/org/hoods/pocket/domain/repository/TransactionRepository.kt`) providing reactive `getTransactions(): Flow<List<Transaction>>` and `suspend fun insertTransaction(transaction: Transaction)`.
- **`AddTransactionUseCase`**: Domain use case (`shared/src/commonMain/kotlin/org/hoods/pocket/domain/usecase/AddTransactionUseCase.kt`) validating transaction invariants and persisting via repository.
- **Local Room Persistence**: Shared Room Multiplatform database entity (`TransactionEntity`), DAO (`TransactionDao`), database (`AppDatabase`), and Room repository implementation (`RoomTransactionRepository`) configured with schema version 1.
- **Runtime Wiring**: Platform factory functions (`getDatabaseBuilder`, `createTransactionRepository`) wiring Room persistence into Android (`MainActivity`), Desktop (`main.kt`), and iOS entry points, with desktop database isolated in `~/.pocket-budget-course/pocket_budget.db`.
- **`MoneyParser`**: Domain monetary conversion utility (`MoneyParser.kt`) converting user-entered decimal strings directly into `Long` integer cents and formatting signed amounts without floating-point arithmetic.
- **Add Transaction Form**: Compose Multiplatform form (`AddTransactionForm.kt`) implementing Expense/Income segmented toggle, hero amount display, description and amount inputs, and read-only "Today" date indicator using `PocketTheme` tokens.
- **Transaction List UI & Flow**: Reactive list screen (`TransactionListScreen.kt`, `TransactionItem.kt`) rendering `72.dp` list rows, `40.dp` type badges, empty state illustration/prompt, and primary FAB, wired into `App.kt` with state-driven navigation.

### Explicit Exclusions
- Edit and delete transaction capabilities.
- Budgets, budget allocation, and progress tracking (balance summary cards deferred to future feature).
- Category management or custom categories.
- Date picker component (timestamps use current `kotlin.time.Instant`).
- Typed navigation frameworks and complex navigation graphs.
- Authentication, user accounts, and cloud synchronization.
- Recurring transactions.

## 3. Test-First Evidence (RED & GREEN)

### Cycle 1: Monetary Parsing (`MoneyParser.parseAmountToCents`)
- **Failing Test**: `org.hoods.pocket.domain.util.MoneyParserTest`
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.domain.util.MoneyParserTest"
  ```
- **Observed Failure Output**:
  ```text
  e: .../MoneyParserTest.kt:11:32 Unresolved reference 'MoneyParser'.
  FAILURE: Build failed with an exception.
  Execution failed for task ':shared:compileTestKotlinJvm'.
  ```
- **Implementation File**: `shared/src/commonMain/kotlin/org/hoods/pocket/domain/util/MoneyParser.kt`
- **Observed Passing Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTestClasses
  > Task :shared:jvmTest
  BUILD SUCCESSFUL in 14s
  ```

### Cycle 2: Signed Currency Formatting (`MoneyParser.formatSignedAmount`)
- **Failing Test**: `org.hoods.pocket.domain.util.MoneyParserTest.formatSignedAmount_formatsIncomeAndExpenseCorrectly`
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest
  ```
- **Observed Failure Output**:
  ```text
  e: .../MoneyParserTest.kt:46:46 Unresolved reference 'formatSignedAmount'.
  e: .../MoneyParserTest.kt:47:45 Unresolved reference 'formatSignedAmount'.
  FAILURE: Build failed with an exception.
  Execution failed for task ':shared:compileTestKotlinJvm'.
  ```
- **Implementation File**: `shared/src/commonMain/kotlin/org/hoods/pocket/domain/util/MoneyParser.kt`
- **Observed Passing Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTestClasses
  > Task :shared:jvmTest
  BUILD SUCCESSFUL in 15s
  ```

## 4. Verification Results
- **Verification Gate Command**:
  ```bash
  ./scripts/verify.sh
  ```
- **Gate Summary**:
  ```text
  Git diff check (whitespace/formatting) : PASS
  Shared JVM Unit Tests (:shared:jvmTest) : PASS
  Android Host Unit Tests (:shared:testAndroidHostTest) : PASS
  Desktop App Compilation (:desktopApp:compileKotlin) : PASS
  Android App Packaging (:androidApp:assembleDebug) : PASS
  iOS Simulator Tests (:shared:iosSimulatorArm64Test) : NOT RUN (Target requires ARM64, host is macOS x86_64)
  ```

## 5. Manual Verification Journey
1. **Launch App**: Executed `./gradlew :desktopApp:run`. The Compose Desktop window launched titled "pocket-budget" displaying `TransactionListScreen`.
2. **Empty State Observation**: Verified initial state renders the clean empty state card ("No transactions yet. Tap the + button to record your first transaction.") with "Add Transaction" button and floating action button.
3. **Open Add Form**: Clicked the `+` FAB. The screen transitioned smoothly to `AddTransactionForm` showing Expense/Income toggle, `$0.00` hero display, input fields, and read-only "Today" date indicator.
4. **Dismiss Navigation**: Clicked `✕` dismiss button. Returned immediately to `TransactionListScreen` without creating a transaction.
5. **Add Income Transaction**: Clicked `+`, selected "Income" tab, entered description "Client Payment" and amount "650.00", and clicked "Save Income".
6. **Observation & Reactive Update**: Form automatically closed and returned to `TransactionListScreen`. The list reactively updated to show "Client Payment", date caption, and green signed amount `+$650.00` with `↑` avatar badge.
7. **Add Expense Transaction**: Clicked `+`, kept "Expense" tab, entered description "Groceries" and amount "45.50", and clicked "Save Transaction".
8. **Observation**: Returned to list showing both items with dividers; "Groceries" displayed with `-$45.50` and `↓` avatar badge.
9. **Restart & Persistence**: Terminated the desktop application process and relaunched with `./gradlew :desktopApp:run`. Confirmed both transactions were recovered from `~/.pocket-budget-course/pocket_budget.db` and rendered on startup.

## 6. Limitations & Unverified Targets
- **iOS Simulator Tests**: `NOT RUN` (host platform is macOS x86_64, whereas the `iosSimulatorArm64Test` target requires ARM64 hardware).

## 7. Final Checkpoint
- **Commit History for Slice**:
  - `5999624`: `feat(domain): add TransactionRepository contract and AddTransactionUseCase`
  - `afbf6b4`: `feat(data): add Room Multiplatform local transaction persistence`
  - `d126e6d`: `feat(runtime): wire Room transaction repository to Android, Desktop, and iOS entry points`
  - `de18de6`: `feat(ui): implement Add Transaction form and isolate desktop database location`
  - `e8e3de1`: `feat(ui): implement Transaction List screen and interactive flow`
- **Final Slice Completion Commit**: `e8e3de1`
- **Final Git Status**:
  ```text
  On branch main
  nothing to commit, working tree clean
  ```
