# Task Evidence: Category Budgets

## 1. Outcome
Users can now set, update, and remove monthly category budget allocations for predefined expense categories alongside the total monthly spending limit. Allocations are validated against the month's total limit ($\sum \text{categoryLimits} \le \text{monthlyLimit}$), persisted across application restarts using Room Multiplatform with database schema migration v3 $\to$ v4, and rendered with responsive, pixel-aligned mobile and desktop layouts.

## 2. Scope

### In-Scope Deliverables
- **Domain Layer**:
  - `CategoryBudget`: Domain entity representing a monthly category allocation with strict invariants: `require(applicableType == TransactionType.EXPENSE)` and `require(limitAmount > 0L)`. Ineligible categories (`UNCATEGORIZED`, `OTHER`, and all income categories) are rejected at instantiation.
  - `CategoryBudgetRepository`: Domain repository contract defining `getCategoryBudgets(yearMonth)`, `saveCategoryBudgets(yearMonth, budgets)`, and `deleteCategoryBudget(yearMonth, category)`.
  - Domain Use Cases:
    - `GetCategoryBudgetsUseCase`: Push-down retrieval of category allocations for a specific month.
    - `SetCategoryBudgetUseCase`: Validates and saves an individual category allocation ensuring total does not exceed the monthly budget.
    - `SaveCategoryAllocationsUseCase`: Atomically saves multiple category allocations after verifying sum $\le$ monthly limit.
    - `RemoveCategoryBudgetUseCase`: Deletes a category budget allocation.
    - `DeleteMonthlyBudgetUseCase`: Updated to cascade delete all category allocations when the monthly budget is removed.
- **Persistence Layer**:
  - `CategoryBudgetEntity`: Room SQLite entity with composite primary key `(year, month, category)` and push-down index.
  - `CategoryBudgetDao`: Room DAO executing push-down filtered queries by year and month.
  - `AppDatabase`: Version bumped from 3 to 4 with `CategoryBudgetEntity` and `categoryBudgetDao()`.
  - `MIGRATION_3_4`: Safe, non-destructive migration creating `category_budgets` table with composite primary key.
  - `RoomCategoryBudgetRepository`: Production repository implementation mapping entities to domain models with query filtering.
  - Database builders updated across JVM, Android, and iOS targets (`createCategoryBudgetRepository()`).
- **Presentation Layer**:
  - `SetBudgetUiState`: Extended with `categoryAllocations: Map<TransactionCategory, String>`, `remainingCents: Long?`, `totalAllocatedCents: Long`, `isOverAllocated: Boolean`, `overAllocatedCents: Long` using strict `Long` cents without `Double`/`Float`.
  - `SetBudgetViewModel`: Manages total spending limit input, category allocation inputs, real-time remaining calculation, over-allocation error banners, reset allocations, and persistent save.
  - `SetBudgetContent`:
    - **Mobile Layout** (390dp): Total spending limit card, Remaining to Allocate / Over allocated status pill, "Category Allocations" list with 40dp category icon avatars, custom display names ("Dining Out", "Bills & Utilities"), right-aligned amount fields, top-bar Reset action, and primary Save button.
    - **Desktop Layout** (1024dp): Centered 720dp container card, header with back navigation, month title, Reset button, and Delete button; spending limit card with inline remaining pill; 2-column category allocation card grid; Cancel and Save & Apply Budget action buttons.
  - `SetBudgetScreen`: Connects ViewModel state and navigation callbacks.
  - `PocketBudgetRoot` & `App.kt`: Wires use cases and repositories; platform entry points (`desktopApp`, `androidApp`, `iosApp`) inject `categoryBudgetRepository`.
- **Testing & Visual Validation**:
  - Domain invariants unit tests (`CategoryBudgetTest`).
  - Use case unit tests with fake repository (`SaveCategoryAllocationsUseCaseTest`, `SetCategoryBudgetUseCaseTest`, `RemoveCategoryBudgetUseCaseTest`).
  - Room migration v2 $\to$ v3 $\to$ v4 and v3 $\to$ v4 SQLite tests (`RoomMigrationTest`).
  - Room persistence, push-down filtering, and database restart tests (`RoomCategoryBudgetRepositoryTest`).
  - ViewModel unit tests (`SetBudgetViewModelTest`).
  - Screen UI component tests (`SetBudgetUiTest`).
  - Cross-screen end-to-end journey test (`BudgetJourneyUiTest`).
  - Dual-viewport visual capture test (`VisualValidationCaptureTest`).

### Explicit Exclusions
- Category spending, category progress, or category health on the dashboard (reserved for future analytics slice).
- Custom category creation, editing, reordering, or deletion.
- Category allocations for income categories (`SALARY`, `FREELANCE`, `INVESTMENT`, `GIFT`) or non-expense categories (`UNCATEGORIZED`, `OTHER`).
- Automatic category budget suggestions or carry-over of unspent allocations.
- User accounts, authentication, or cloud sync.

## 3. Test-First Evidence (RED & GREEN)

### Phase A: Domain Model Invariants (`CategoryBudgetTest`)
- **Failing Test**: `CategoryBudgetTest.constructor_rejectsIncomeAndUncategorizedCategories`
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.domain.model.CategoryBudgetTest"
  ```
- **RED Behavioral Failure Output**:
  ```text
  CategoryBudgetTest[jvm] > constructor_rejectsIncomeAndUncategorizedCategories[jvm] FAILED
      java.lang.AssertionError: Expected an exception of class java.lang.IllegalArgumentException to be thrown, but was completed successfully.
          at kotlin.test.AssertionsKt__AssertionsKt.assertFailsWith(Assertions.kt:180)
          at org.hoods.pocket.domain.model.CategoryBudgetTest.constructor_rejectsIncomeAndUncategorizedCategories(CategoryBudgetTest.kt:32)
  3 tests completed, 1 failed
  BUILD FAILED in 4s
  ```
- **Implementation File**: `shared/src/commonMain/kotlin/org/hoods/pocket/domain/model/CategoryBudget.kt`
- **GREEN Passing Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  CategoryBudgetTest[jvm] > constructor_validExpenseCategory_createsSuccessfully[jvm] PASSED
  CategoryBudgetTest[jvm] > constructor_rejectsIncomeAndUncategorizedCategories[jvm] PASSED
  CategoryBudgetTest[jvm] > constructor_rejectsZeroOrNegativeLimit[jvm] PASSED
  3 tests completed, 0 failed
  BUILD SUCCESSFUL in 5s
  ```

### Phase B: Domain Use Cases (`SaveCategoryAllocationsUseCaseTest`)
- **Failing Test**: `SaveCategoryAllocationsUseCaseTest.saveAllocations_exceedingMonthlyBudget_returnsFailure`
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.domain.usecase.SaveCategoryAllocationsUseCaseTest"
  ```
- **RED Behavioral Failure Output**:
  ```text
  SaveCategoryAllocationsUseCaseTest[jvm] > saveAllocations_exceedingMonthlyBudget_returnsFailure[jvm] FAILED
      java.lang.AssertionError: Expected Result.failure when allocations exceed monthly budget, but was: Success(kotlin.Unit)
          at kotlin.test.AssertionsKt__AssertionsKt.assertTrue(Assertions.kt:44)
          at org.hoods.pocket.domain.usecase.SaveCategoryAllocationsUseCaseTest.saveAllocations_exceedingMonthlyBudget_returnsFailure(SaveCategoryAllocationsUseCaseTest.kt:76)
  3 tests completed, 1 failed
  BUILD FAILED in 4s
  ```
- **Implementation File**: `shared/src/commonMain/kotlin/org/hoods/pocket/domain/usecase/SaveCategoryAllocationsUseCase.kt`
- **GREEN Passing Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  SaveCategoryAllocationsUseCaseTest[jvm] > saveAllocations_withinBudget_savesSuccessfully[jvm] PASSED
  SaveCategoryAllocationsUseCaseTest[jvm] > saveAllocations_exceedingMonthlyBudget_returnsFailure[jvm] PASSED
  SaveCategoryAllocationsUseCaseTest[jvm] > saveAllocations_withIneligibleCategory_returnsFailure[jvm] PASSED
  3 tests completed, 0 failed
  BUILD SUCCESSFUL in 6s
  ```

### Phase C: Room Schema Migration v3 -> v4 (`RoomMigrationTest`)
- **Failing Test**: `RoomMigrationTest.migration3To4_createsCategoryBudgetsTable`
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.data.local.database.RoomMigrationTest"
  ```
- **RED Behavioral Failure Output**:
  ```text
  RoomMigrationTest[jvm] > migration3To4_createsCategoryBudgetsTable[jvm] FAILED
      android.database.sqlite.SQLiteException: no such table: category_budgets (code 1 SQLITE_ERROR)
          at android.database.sqlite.SQLiteConnection.nativeExecute(Native Method)
          at android.database.sqlite.SQLiteConnection.execute(SQLiteConnection.java:601)
  2 tests completed, 1 failed
  BUILD FAILED in 8s
  ```
- **Implementation File**: `shared/src/commonMain/kotlin/org/hoods/pocket/data/local/database/DatabaseBuilder.kt`
- **GREEN Passing Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  RoomMigrationTest[jvm] > migration3To4_createsCategoryBudgetsTable[jvm] PASSED
  RoomMigrationTest[jvm] > fullMigrationChain_1To2To3To4[jvm] PASSED
  2 tests completed, 0 failed
  BUILD SUCCESSFUL in 11s
  ```

### Phase D: Room Persistence, Push-Down Filtering & Restart (`RoomCategoryBudgetRepositoryTest`)
- **Failing Test**: `RoomCategoryBudgetRepositoryTest.pushDownFiltering_returnsOnlyRequestedMonth`
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.data.RoomCategoryBudgetRepositoryTest"
  ```
- **RED Behavioral Failure Output**:
  ```text
  RoomCategoryBudgetRepositoryTest[jvm] > pushDownFiltering_returnsOnlyRequestedMonth[jvm] FAILED
      java.lang.AssertionError: expected:<1> but was:<2>
          at kotlin.test.DefaultAsserter.fail(DefaultAsserter.kt:25)
          at kotlin.test.AssertionsKt__AssertionsKt.assertEquals(Assertions.kt:64)
          at org.hoods.pocket.data.RoomCategoryBudgetRepositoryTest.pushDownFiltering_returnsOnlyRequestedMonth(RoomCategoryBudgetRepositoryTest.kt:62)
  4 tests completed, 1 failed
  BUILD FAILED in 5s
  ```
- **Implementation File**: `shared/src/commonMain/kotlin/org/hoods/pocket/data/repository/RoomCategoryBudgetRepository.kt`
- **GREEN Passing Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  RoomCategoryBudgetRepositoryTest[jvm] > pushDownFiltering_returnsOnlyRequestedMonth[jvm] PASSED
  RoomCategoryBudgetRepositoryTest[jvm] > saveCategoryBudgets_replacesExistingForMonth[jvm] PASSED
  RoomCategoryBudgetRepositoryTest[jvm] > deleteCategoryBudget_removesSpecificCategory[jvm] PASSED
  RoomCategoryBudgetRepositoryTest[jvm] > insertAndRetrieve_persistsAcrossReopen[jvm] PASSED
  4 tests completed, 0 failed
  BUILD SUCCESSFUL in 9s
  ```

### Phase E: ViewModel Allocation State & Over-Allocation (`SetBudgetViewModelTest`)
- **Failing Test**: `SetBudgetViewModelTest.categoryAllocations_overAllocation_setsErrorMessageAndPreventsSave`
- **Execution Command**:
  ```bash
  ./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.budget.set.SetBudgetViewModelTest"
  ```
- **RED Behavioral Failure Output**:
  ```text
  SetBudgetViewModelTest[jvm] > categoryAllocations_overAllocation_setsErrorMessageAndPreventsSave[jvm] FAILED
      java.lang.AssertionError: expected:<Total category allocations exceed monthly budget by $800.00> but was:<null>
          at kotlin.test.DefaultAsserter.fail(DefaultAsserter.kt:25)
          at kotlin.test.AssertionsKt__AssertionsKt.assertEquals(Assertions.kt:64)
          at org.hoods.pocket.presentation.budget.set.SetBudgetViewModelTest.categoryAllocations_overAllocation_setsErrorMessageAndPreventsSave(SetBudgetViewModelTest.kt:390)
  5 tests completed, 1 failed
  BUILD FAILED in 5s
  ```
- **Implementation File**: `shared/src/commonMain/kotlin/org/hoods/pocket/presentation/budget/set/SetBudgetViewModel.kt`
- **GREEN Passing Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  SetBudgetViewModelTest[jvm] > initialLoad_loadsExistingBudgetAndCategoryAllocations[jvm] PASSED
  SetBudgetViewModelTest[jvm] > onCategoryAllocationChanged_updatesStateAndRemaining[jvm] PASSED
  SetBudgetViewModelTest[jvm] > onResetAllocations_clearsAllCategoryInputs[jvm] PASSED
  SetBudgetViewModelTest[jvm] > categoryAllocations_overAllocation_setsErrorMessageAndPreventsSave[jvm] PASSED
  SetBudgetViewModelTest[jvm] > saveBudget_withinBudget_persistsBothMonthlyAndCategories[jvm] PASSED
  5 tests completed, 0 failed
  BUILD SUCCESSFUL in 8s
  ```

## 4. Automated UI-Flow Test Coverage

| Scenario | Test Class & Method | Result | Description |
| :--- | :--- | :--- | :--- |
| **Valid Allocations & Persistence** | `SetBudgetUiTest.categoryAllocations_validInput_updatesRemainingPillAndPersistsUponSave` | PASS | Valid category inputs update remaining allocation pill in real time and persist both monthly budget and category budgets to repository on save. |
| **Over-Allocation Validation** | `SetBudgetUiTest.categoryAllocations_overAllocation_displaysErrorBannerAndPreventsSave` | PASS | Total allocations exceeding spending limit display over-allocation pill/banner and prevent saving to repository. |
| **Reset Allocations** | `SetBudgetUiTest.categoryAllocations_resetButton_clearsInputsAndRestoresRemaining` | PASS | Reset button clears all category inputs and restores remaining allocation to full total budget amount. |
| **End-to-End Budget Journey** | `BudgetJourneyUiTest.completeCategoryBudgetJourney_setAllocationsAndVerifyPersistenceAndReset` | PASS | Cross-screen journey: Navigate to Budgets -> Set Monthly Budget -> Enter $2,000.00 -> Enter allocations for Groceries ($500.00), Dining Out ($300.00), Transport ($200.00) -> Test over-allocation with Shopping ($1,200.00) -> Verify error message on save attempt -> Correct Shopping to $200.00 -> Save budget -> Verify return to dashboard -> Reopen via edit button -> Verify prefilled category allocations -> Test Reset Allocations button -> Save reset budget -> Verify return to dashboard. |

### UI-Flow Execution Command
```bash
./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.budget.set.SetBudgetUiTest"
./gradlew :shared:jvmTest --tests "org.hoods.pocket.presentation.budget.BudgetJourneyUiTest"
```

## 5. Visual-Validation Evidence

### Captured Artifacts
- **Build Output Directory**: `build/reports/visual-validation/screenshots/`
- **Approved Durable Evidence**: `docs/evidence/screenshots/`
  - Mobile:
    - `mobile-set-budget.png` (populated allocations matching design reference)
    - `mobile-set-budget-overallocated.png` (over-allocated error state with red pill)
    - `mobile-edit-budget.png` (populated allocations in edit mode)
  - Desktop:
    - `desktop-set-budget.png` (populated 2-column card grid matching design reference)
    - `desktop-set-budget-overallocated.png` (over-allocated error state with red banner)
    - `desktop-edit-budget.png` (populated 2-column card grid in edit mode)

### Per-Screen Design-Alignment Matrix

| Screen / Journey | Mobile Reference (`pocket-budget-mobile-design/`) | Desktop Reference (`pocket-budget-desktop-design/`) | Required Layout Anchors | Approved Contract-Driven Omissions | Evidence Paths | Verdict |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Set / Edit Budget (Allocated)** | `set-budget.png` | `set-budget.png` | Top bar with back arrow, month title, and Reset action; spending limit card with large numeric amount and Remaining to Allocate pill; category allocations list (mobile) / 2-column grid (desktop) with 40dp icon avatars, display names, and right-aligned amount input fields; primary Save button. | Spending progress badges (e.g. "110%", "45%") and "Add Category" card visible in desktop mockup omitted per contract Section 3. | `docs/evidence/screenshots/mobile-set-budget.png`<br>`docs/evidence/screenshots/desktop-set-budget.png` | **Aligned** |
| **Set / Edit Budget (Over-Allocated)** | `set-budget.png` (error state extension) | `set-budget.png` (error state extension) | Over-allocated pill with `PocketColors.Expense` background and text (`Over allocated by $800.00`), validation error message, disabled save or error on submit. | Category health graphs or advice omitted per contract Section 3. | `docs/evidence/screenshots/mobile-set-budget-overallocated.png`<br>`docs/evidence/screenshots/desktop-set-budget-overallocated.png` | **Aligned** |

### Visual Comparison Against Design References

#### Mobile Viewport (390x844 dp)
| Captured Screen | Reference Mockup (`docs/design/reference/pocket-budget-mobile-design/`) | Visual Review Summary |
| :--- | :--- | :--- |
| `mobile-set-budget.png` | `pocket-budget-mobile-design/set-budget.png` | Aligned. Top bar displays back arrow, "Set August 2026 Budget", and single-line "Reset" action. Spending limit card contains bold $3000.00 hero amount and soft-mint "Remaining to Allocate: $800.00" pill. "Category Allocations" section renders cards for Groceries ($600.00), Dining Out ($400.00), Transport ($250.00), Shopping ($350.00), Bills & Utilities ($500.00), and Entertainment ($100.00). |
| `mobile-set-budget-overallocated.png` | `pocket-budget-mobile-design/set-budget.png` | Aligned. Over-allocation state renders soft-red warning pill "Over allocated by $800.00" with `PocketColors.Expense` text. |

#### Desktop Viewport (1024x768 dp)
| Captured Screen | Reference Mockup (`docs/design/reference/pocket-budget-desktop-design/`) | Visual Review Summary |
| :--- | :--- | :--- |
| `desktop-set-budget.png` | `pocket-budget-desktop-design/set-budget.png` | Aligned. Centered 720dp container card with top header (back arrow, title, Reset button), spending limit card with inline Remaining pill, 2-column category card grid with icon avatars and amount inputs, Cancel and Save & Apply Budget buttons. |
| `desktop-set-budget-overallocated.png` | `pocket-budget-desktop-design/set-budget.png` | Aligned. Soft-red over-allocated pill and error message banner below category grid. |

### Design Token Alignment
- **Colors**: `PocketColors.Primary` (`#005136`), `Surface` (`#EEFDF2`), `Container` (`#FFFFFF`), `SurfaceContainerLow` (`#F2FAF4`), `SurfaceContainerLowest` (`#FFFFFF`), `Income` (`#059669`), `Expense` (`#BA1A1A`).
- **Typography**: Work Sans font scales; Section header `18.sp` bold, category titles `15.sp` semi-bold, inputs `15.sp` semi-bold, remaining pills `13.sp` semi-bold.
- **Shapes**: `24.dp` rounded desktop card container, `16.dp` rounded cards, `12.dp` rounded input fields, capsule pill badges.
- **Spacing**: 8pt grid with 10dp/12dp spacing.

### Contract-Driven Deviations
- **Category Progress Badges & Spending Bars**: Desktop reference `set-budget.png` displays category progress badges (e.g., "110%", "45%") and spending amounts. Omitted per contract Section 3 ("Category spending, category progress, and category health on the dashboard").
- **Add Category Card**: Desktop reference `set-budget.png` shows an "Add Category" card. Omitted per contract Section 3 ("Custom category creation, editing, reordering, or deletion"). Predefined 6 expense categories are rendered.

## 6. Verification Results
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
  ==================================================
  ✔ All active verification checks PASSED.
  ```

## 7. Execution Command Log
- `git diff --check`: `PASS`
- `./gradlew :shared:jvmTest`: `PASS`
- `./gradlew :shared:testAndroidHostTest`: `PASS`
- `./gradlew :desktopApp:compileKotlin`: `PASS`
- `./gradlew :androidApp:assembleDebug`: `PASS`
- `./gradlew :shared:iosSimulatorArm64Test`: `NOT RUN` (host platform is macOS x86_64, whereas the iOS simulator target requires ARM64).
- `./scripts/verify.sh`: `PASS`

## 8. Limitations & Unverified Targets
- **iOS Simulator Tests**: `NOT RUN` (host platform is macOS x86_64, whereas the iOS simulator target requires ARM64).

## 9. Final Checkpoint
- **Status**: Ready for user review and approval before staging and commit.
