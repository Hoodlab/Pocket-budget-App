# Task Evidence: Account and Local-Data Ownership

## 1. Outcome

Implemented email/password sign-up, sign-in, sign-out, authentication state management, explicit Supabase runtime configuration (zero silent localhost fallback), atomic first-sign-in data claiming, and multi-account local data isolation across Room persistence and Compose Multiplatform UI, in accordance with [`.agents/contracts/auth-and-local-data-ownership.md`](file:///Users/apple/Documents/Agentic%20Engineering/budget-poket%20resources/pocket-budget/.agents/contracts/auth-and-local-data-ownership.md).

Additionally, resolved the data disappearance issue post-claiming by establishing hot reactive identity propagation from `AuthRepository.authState` into `RoomTransactionRepository`, `RoomBudgetRepository`, and `RoomCategoryBudgetRepository`.

---

## 2. Scope

### In-Scope Deliverables
- **Supabase Kotlin Auth Client:** Added `io.github.jan-tennert.supabase:auth-kt:3.2.2` and `bom:3.2.2` with platform-specific Ktor engines (Android: `android`, JVM: `cio`, iOS: `darwin`). Compatible with `kotlinx-datetime:0.7.1`.
- **Explicit Runtime Configuration:** Implemented `SupabaseConfig` and `SupabaseEnvironment` (`LOCAL`, `HOSTED_DEV`, `UNCONFIGURED`) preventing silent localhost fallback, paired with `SetupConfigScreen` for runtime environment selection.
- **Room Database Migration v4 -> v5:** Implemented `MIGRATION_4_5` adding `userId TEXT DEFAULT NULL` across `transactions`, `monthly_budgets`, and `category_budgets`.
- **Atomic Data Claiming & ViewModel Decision Ownership:** Implemented `DataOwnershipDao` (`@Transaction suspend fun claimAllUnassigned(userId: String)`), `ClaimLocalDataUseCase`, and `GetUnclaimedDataSummaryUseCase`. Shifted the claim decision to `SignInViewModel`, which checks for unclaimed data upon sign-in success and emits `SignInEvent.ClaimRequired(userId)` or `SignInEvent.SignInSuccess(user)`. Navigation hosts respond purely to ViewModel events.
- **Reactive Identity Propagation Across Repositories:**
  - Repositories (`RoomTransactionRepository`, `RoomBudgetRepository`, `RoomCategoryBudgetRepository`) now accept `authRepository: AuthRepository? = null`.
  - Backing `userFlow` dynamically derives from `authRepository.authState.map { (it as? AuthState.Authenticated)?.user?.id }.distinctUntilChanged()`.
  - When the auth state transitions (e.g. unauthenticated -> authenticated upon sign-in/claim, or authenticated -> unauthenticated on sign-out), Room query flows re-bind dynamically with the active user ID, emitting the claimed records immediately without stale snapshot captures or UI cache inconsistencies.
- **Platform DI & Entry Point Wiring:**
  - `DatabaseBuilder.jvm.kt`, `DatabaseBuilder.android.kt`, `DatabaseBuilder.ios.kt` provide shared `AuthRepository` instances and inject them by default into repository factories.
  - Entry points (`desktopApp/main.kt`, `androidApp/MainActivity.kt`, `iosApp/MainViewController.kt`) pass `authRepository` to `PocketBudgetRoot` and the repository factories.
- **Unified Top-Level Navigation Shell:** Implemented `PocketNavigationShell` wrapping `PocketNavHost`. Keeps Transactions, Budgets, and Account accessible everywhere across Compact (bottom bar: 64dp) and Wider (sidebar: 220dp) layouts. Removed fragmented screen-level sidebars and bottom bars from `TransactionListContent` and `BudgetDashboardContent`. Sub-screens (`AddTransaction`, `EditTransaction`, `SetBudget`, `ClaimData`) operate seamlessly within `PocketNavHost`.
- **Local Multi-Account Isolation:** Push-down query filtering on `TransactionDao`, `BudgetDao`, `CategoryBudgetDao` scoping records to `activeUserId`. Unassigned data (`userId IS NULL`) is visible only when unauthenticated; Account A data is hidden from Account B; sign-out cleanly restores unassigned local data.
- **Truthful Email Confirmation:** Handled `AuthResult.ConfirmationRequired` truthfully without treating unconfirmed users as authenticated, and without modifying Supabase server auth settings.
- **Compose Screen Quads & Previews:**
  - `presentation/auth/sign_in/`: `SignInUiState`, `SignInViewModel`, `SignInScreen`, `SignInContent` (with preview)
  - `presentation/auth/claim/`: `ClaimDataUiState`, `ClaimDataViewModel`, `ClaimDataScreen`, `ClaimDataContent` (with preview)
  - `presentation/auth/account/`: `AccountUiState`, `AccountViewModel`, `AccountScreen`, `AccountContent` (with preview)
  - `presentation/auth/setup/`: `SetupConfigUiState`, `SetupConfigViewModel`, `SetupConfigScreen`, `SetupConfigContent` (with preview)
- **Navigation & Entry Points:** Added type-safe routes `Auth`, `Account`, `ClaimData(targetUserId)`, `SetupConfig` in `Screen.kt`, integrated in `PocketNavHost.kt` inside `PocketNavigationShell`.
- **Automated UI-Flow Tests:** Added 4 comprehensive UI journey tests in `AuthJourneyUiTest.kt` and 3 responsive navigation journey tests in `TopLevelNavigationJourneyUiTest.kt`.

### Explicit Exclusions
- Remote sync engine, sync queue, or network retry outbox.
- Remote mutation of Supabase database rows.
- OAuth / third-party identity providers.
- Password reset, MFA, or account deletion.

---

## 3. Investigation & Verification of Claim Data Propagation

### Issue Analysis
When claiming unassigned local data, `DataOwnershipDao.claimAllUnassigned(userId)` successfully updated SQLite rows (`UPDATE ... SET userId = :userId WHERE userId IS NULL`). However, queries were bound to an immutable initial snapshot of `flowOf(activeUserId())` which evaluated once as `null` while unauthenticated and never emitted again. Thus Room queries remained permanently scoped to `userId IS NULL`, causing claimed records to disappear.

### Three-Point Verification (Reproduced & Confirmed)
1. **Room Rows Verification:**
   - Evaluated direct raw queries on SQLite:
     - All previously unassigned transactions, monthly budgets, and category budgets have `userId == "user-123"`.
     - Zero records remain with `userId IS NULL`.
2. **Active Authentication State Verification:**
   - `authRepo.authState` emits `AuthState.Authenticated(AuthUser(id = "user-123", email = ...))`.
   - `authRepo.currentUser()?.id` equals `"user-123"`.
3. **Query Reactivity Verification:**
   - `transactionRepo.getTransactions()`, `budgetRepo.getBudgetForMonth(month)`, and `categoryBudgetRepo.getCategoryBudgetsForMonth(month)` re-emit immediately upon auth state change.
   - Claimed records appear immediately for `"user-123"`.
   - On sign out, queries immediately re-emit empty collections for the unauthenticated state (`userId IS NULL`).

All three points are systematically validated in [`shared/src/jvmTest/kotlin/org/hoods/pocket/data/local/database/ClaimDataPropagationTest.kt`](file:///Users/apple/Documents/Agentic%20Engineering/budget-poket%20resources/pocket-budget/shared/src/jvmTest/kotlin/org/hoods/pocket/data/local/database/ClaimDataPropagationTest.kt).

---

## 4. Test-First Evidence (RED & GREEN)

### RED Phase
1. **Room Migration Pre-Migration Assertion:**
   - File: `shared/src/jvmTest/kotlin/org/hoods/pocket/data/local/database/RoomDataOwnershipPreMigrationTest.kt`
   - Demonstrated that querying `userId` on a v4 SQLite schema fails with `SQLiteException: no such column: userId`.
2. **Claim Data Propagation Regression Test:**
   - File: `shared/src/jvmTest/kotlin/org/hoods/pocket/data/local/database/ClaimDataPropagationTest.kt`
   - Demonstrated failure before the reactive identity propagation fix: `transactionRepo.getTransactions().first()` emitted 0 items post-claiming.

### GREEN Phase
1. **Room Persistence & Migration:**
   - `RoomMigrationTest.kt` (migrations 2->3, 3->4, 4->5): PASS
   - `RoomDataOwnershipTest.kt` (unassigned scoping, claiming, multi-account isolation): PASS
   - `ClaimDataPropagationTest.kt` (dynamic user flow emission, Room row verification, auth state verification, query reactivity): PASS
2. **Domain & Data Layer:**
   - `ClaimLocalDataUseCaseTest.kt`: PASS
   - `SupabaseAuthRepositoryTest.kt`: PASS
3. **Presentation ViewModels:**
   - `SignInViewModelTest.kt`: PASS
   - `ClaimDataViewModelTest.kt`: PASS
   - `AccountViewModelTest.kt`: PASS
4. **UI Journeys:**
   - `AuthJourneyUiTest.kt`: PASS (4/4 journeys passing, including complete journey across transactions and budget dashboard)
   - `TopLevelNavigationJourneyUiTest.kt`: PASS (3/3 journeys passing)

---

## 5. Automated UI-Flow Test Coverage

### Auth Journeys
Implemented in [`shared/src/jvmTest/kotlin/org/hoods/pocket/presentation/auth/AuthJourneyUiTest.kt`](file:///Users/apple/Documents/Agentic%20Engineering/budget-poket%20resources/pocket-budget/shared/src/jvmTest/kotlin/org/hoods/pocket/presentation/auth/AuthJourneyUiTest.kt):
1. `completeAuthJourney_signIn_claimUnassignedData_viewAccount_andSignOut`: Covers unauthenticated list and budget display -> navigate to account -> sign in -> detect unassigned records -> claim records atomically -> verify transaction and budget dashboard display under account -> view account status -> sign out -> verify isolation from unauthenticated session.
2. `keepOnDeviceJourney_leavesDataUnassigned_andVisibleToUnauthenticated`: Covers signing in -> choosing "Keep on Device" -> confirming unassigned data is hidden from signed-in account -> signing out -> confirming unassigned data remains intact on device.
3. `setupConfigJourney_switchesRuntimeEnvironment`: Covers navigating to Account -> opening Supabase Environment Setup -> switching to Hosted Dev -> returning to Account.
4. `signInErrorHandling_andModeToggling`: Covers invalid credentials error feedback banner and switching between Sign In and Sign Up modes.

### Top-Level Navigation Shell Journeys
Implemented in [`shared/src/jvmTest/kotlin/org/hoods/pocket/presentation/navigation/TopLevelNavigationJourneyUiTest.kt`](file:///Users/apple/Documents/Agentic%20Engineering/budget-poket%20resources/pocket-budget/shared/src/jvmTest/kotlin/org/hoods/pocket/presentation/navigation/TopLevelNavigationJourneyUiTest.kt):
1. `compactLayout_bottomNavigation_persistsAcrossAllTopLevelScreens`: Tests that bottom navigation bar (64dp, destinations: Transactions, Budgets, Account) remains visible and interactive across Transactions, Budgets, and Account screens.
2. `expandedLayout_sidebar_persistsAcrossAllTopLevelScreens`: Tests that desktop sidebar (220dp, header + destinations: Transactions, Budgets, Account) remains visible and interactive across all top-level destinations.
3. `subScreen_addTransaction_operatesWithinNavHostAndReturnsToTransactions`: Verifies that sub-screens (`Screen.AddTransaction`) operate seamlessly inside the NavHost and dismiss cleanly back to the top-level Transactions screen.

---

## 6. Visual-Validation Evidence

Stateless content composables provide realistic multiplatform Compose `@Preview`s:
- `SignInContentPreview` in [`SignInContent.kt`](file:///Users/apple/Documents/Agentic%20Engineering/budget-poket%20resources/pocket-budget/shared/src/commonMain/kotlin/org/hoods/pocket/presentation/auth/sign_in/SignInContent.kt)
- `ClaimDataContentPreview` in [`ClaimDataContent.kt`](file:///Users/apple/Documents/Agentic%20Engineering/budget-poket%20resources/pocket-budget/shared/src/commonMain/kotlin/org/hoods/pocket/presentation/auth/claim/ClaimDataContent.kt)
- `AccountContentPreview` in [`AccountContent.kt`](file:///Users/apple/Documents/Agentic%20Engineering/budget-poket%20resources/pocket-budget/shared/src/commonMain/kotlin/org/hoods/pocket/presentation/auth/account/AccountContent.kt)
- `SetupConfigContentPreview` in [`SetupConfigContent.kt`](file:///Users/apple/Documents/Agentic%20Engineering/budget-poket%20resources/pocket-budget/shared/src/commonMain/kotlin/org/hoods/pocket/presentation/auth/setup/SetupConfigContent.kt)

Captured dual-viewport screenshots in `build/reports/visual-validation/screenshots/`:
- Desktop: `desktop-transaction-list-populated.png`, `desktop-budget-dashboard-populated.png` (verifying persistent sidebar with Transactions, Budgets, Account destinations and clean top headers).
- Mobile: `mobile-transaction-list-populated.png`, `mobile-budget-dashboard-populated.png` (verifying persistent 64dp bottom navigation bar with Transactions, Budgets, Account).

All UI elements use theme tokens (`PocketColors` / `MaterialTheme.colorScheme`) and match the design system tokens.

---

## 7. Verification Results

### Project Verification Gate
- **Execution Command:** `./scripts/verify.sh`
- **Output:**
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

### Supabase Local Database Tests
- **Execution Command:** `supabase test db --local`
- **Output:** 25/25 tests PASS.

---

## 8. Known Debt
- `iosSimulatorArm64Test` remains marked as `NOT RUN` due to macOS x86_64 host architecture limitation.
