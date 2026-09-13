# Task Evidence: Categorise Transactions

## 1. Outcome
Users can now categorise transactions when adding or editing, select from a predefined set of transaction categories, view categories in transaction lists with contextual badge icons, and have legacy transactions safely migrated to `Uncategorized`. Categories are enforced by domain type invariants, persisted in Room SQLite across application restarts, and visually validated across both mobile and desktop viewports.

## 2. Scope

### In-Scope Deliverables
- **Domain Layer**:
  - `TransactionCategory`: Predefined enum containing `UNCATEGORIZED`, expense categories (`GROCERIES`, `DINING`, `TRANSPORT`, `SHOPPING`, `BILLS`, `ENTERTAINMENT`), income categories (`SALARY`, `FREELANCE`, `INVESTMENT`, `GIFT`), and shared categories (`OTHER`). Includes `isValidFor(type)` and `categoriesFor(type)` validation.
  - `Transaction`: Added `val category: TransactionCategory = TransactionCategory.UNCATEGORIZED` with domain invariant `require(category.isValidFor(type))` enforcing category compatibility with transaction type.
- **Persistence Layer**:
  - `TransactionEntity`: Added `val category: String = "UNCATEGORIZED"`.
  - `AppDatabase`: Incremented schema version to 3.
  - `DatabaseBuilder`: Created `MIGRATION_2_3` (`ALTER TABLE transactions ADD COLUMN category TEXT NOT NULL DEFAULT 'UNCATEGORIZED'`) and registered migration.
  - `RoomTransactionRepository`: Bidirectional mapping between `TransactionEntity.category` and `TransactionCategory`.
- **Presentation Layer**:
  - `CategorySelector`: Reusable Compose component rendering 3-column mobile grid and 4-column responsive desktop grid with category icon badges and labels.
  - `AddTransactionUiState` & `AddTransactionViewModel`: Exposes `selectedCategory` defaulting to `UNCATEGORIZED`, auto-resets invalid categories upon type switch, and stores category on save.
  - `EditTransactionUiState` & `EditTransactionViewModel`: Pre-fills stored category, handles category changes, updates in place without duplicating records.
  - `TransactionItem`: Renders category icon badge and `${transaction.category.displayName} • $dateString` in item subtitle.
- **Visual Validation & Tests**:
  - Raw SQLite schema migration test (`RoomMigrationTest`).
  - Repository insert, in-place update, and restart persistence tests (`RoomTransactionRepositoryTest`).
  - ViewModel unit tests (`AddTransactionViewModelTest`, `EditTransactionViewModelTest`).
  - End-to-end UI journey test (`TransactionJourneyUiTest`).
  - Dual-viewport screenshot capture and visual comparison against design references (`VisualValidationCaptureTest`).

### Explicit Exclusions
- Custom category creation, editing, reordering, or deletion.
- User-uploaded category icons.
- Category budgets or category allocations.
- Category spending charts, analytics, recommendations, or reports.
- Authentication, user accounts, and cloud synchronization.
- Recurring transactions.

## 3. Test-First Evidence (RED & GREEN)

### Phase A: Domain Model Invariant (`TransactionTest` & `TransactionCategoryTest`)
- **Failing Test**: `TransactionTest.constructor_enforcesCategoryMatchesType`
- **RED Output**:
  ```text
  TransactionTest[jvm] > constructor_enforcesCategoryMatchesType[jvm] FAILED
      java.lang.IllegalArgumentException: Failed requirement.
  1 test completed, 1 failed
  BUILD FAILED
  ```
- **GREEN Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  BUILD SUCCESSFUL in 8s
  ```

### Phase B: Room Migration & Persistence (`RoomMigrationTest` & `RoomTransactionRepositoryTest`)
- **Failing Test**: `RoomMigrationTest.migration2To3_addsCategoryColumnWithDefaultUncategorized`
- **RED Output**:
  ```text
  RoomMigrationTest[jvm] > migration2To3_addsCategoryColumnWithDefaultUncategorized[jvm] FAILED
      android.database.sqlite.SQLiteException: no such column: category (code 1 SQLITE_ERROR)
  1 test completed, 1 failed
  BUILD FAILED
  ```
- **GREEN Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  BUILD SUCCESSFUL in 14s
  ```

### Phase C: Add & Edit ViewModels (`AddTransactionViewModelTest` & `EditTransactionViewModelTest`)
- **Failing Test**: `AddTransactionViewModelTest.onCategorySelected updates selectedCategory`
- **RED Output**:
  ```text
  > Task :shared:compileTestKotlinJvm FAILED
  e: AddTransactionViewModelTest.kt:147:111 Unresolved reference 'selectedCategory'.
  e: AddTransactionViewModelTest.kt:155:19 Unresolved reference 'onCategorySelected'.
  BUILD FAILED in 6s
  ```
- **GREEN Output**:
  ```text
  > Task :shared:compileKotlinJvm
  > Task :shared:compileTestKotlinJvm
  > Task :shared:jvmTest
  BUILD SUCCESSFUL in 19s
  ```

## 4. Automated UI-Flow Test Coverage

| Scenario | Test Class & Method | Result | Description |
| :--- | :--- | :--- | :--- |
| **Category Validation** | `TransactionCategoryTest.filteringAndValidation` | PASS | Verifies applicable types, category filtering, and name parsing. |
| **Database Migration 2->3** | `RoomMigrationTest.migration2To3_addsCategoryColumnWithDefaultUncategorized` | PASS | Pre-category v2 database migrates safely; existing records migrate to `UNCATEGORIZED` preserving all original fields. |
| **Repository Persistence & Restart** | `RoomTransactionRepositoryTest.insertAndRetrieveCategory_andPersistAcrossRestart` | PASS | Stored categories persist across database close and reopen. Updates change category in-place without duplicate rows. |
| **Add Category Selection** | `AddTransactionViewModelTest.onCategorySelected updates selectedCategory` | PASS | Category chips update UI state, type toggle resets incompatible categories to `UNCATEGORIZED`, and save creates categorized transaction. |
| **Edit Pre-fill & Update** | `EditTransactionViewModelTest.saveChanges saves updated category` | PASS | Form pre-loads transaction's stored category; changing category and saving updates repository entity in-place. |
| **Complete Transaction Journey** | `TransactionJourneyUiTest.completeTransactionLifecycleJourney_addEditDeleteCrossScreen` | PASS | End-to-end multi-screen flow: Add transaction with Groceries category -> verify Groceries subtitle in list -> open Edit with pre-filled Groceries -> change to Dining -> verify updated Dining in list without duplicates -> verify repository persistence -> complete deletion. |

## 5. Visual-Validation Evidence

### Captured Artifacts
- **Build Output Directory**: `build/reports/visual-validation/screenshots/`
- **Approved Durable Evidence**: `docs/evidence/screenshots/`
  - Mobile:
    - `mobile-add-transaction-expense.png`
    - `mobile-add-transaction-income.png`
    - `mobile-edit-transaction.png`
    - `mobile-transaction-list-populated.png`
  - Desktop:
    - `desktop-add-transaction.png`
    - `desktop-edit-transaction.png`
    - `desktop-transaction-list-populated.png`

### Per-Screen Design-Alignment Matrix

| Screen / Journey | Mobile Reference (`pocket-budget-mobile-design/`) | Desktop Reference (`pocket-budget-desktop-design/`) | Required Layout Anchors | Approved Contract-Driven Omissions | Evidence Paths | Verdict |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Add Transaction (Expense)** | `add-expense.png` | `add-transaction.png` | Top segmented type switch, hero amount display, input card container with amount/description/date, and 3-col mobile / 4-col desktop category card grid with icon + label. | Custom category management, receipt scanner omitted per contract Section 3. | `docs/evidence/screenshots/mobile-add-transaction-expense.png`<br>`docs/evidence/screenshots/desktop-add-transaction.png` | **Aligned** |
| **Add Transaction (Income)** | `add-income.png` | `add-transaction.png` | Top segmented switch, income green hero amount, income category options (Salary, Freelance, Investment, Gift, Other), Save Income CTA. | Custom category creation, recurring income scheduling omitted per contract Section 3. | `docs/evidence/screenshots/mobile-add-transaction-income.png`<br>`docs/evidence/screenshots/desktop-add-transaction.png` | **Aligned** |
| **Edit Transaction** | `add-expense.png` (form anchors) | `add-transaction.png` (form anchors) | Top dismiss and delete action button, pre-filled amount, description, date, and pre-selected category card, Save Changes CTA. | Historical audit log, category budget breakdown omitted per contract Section 3. | `docs/evidence/screenshots/mobile-edit-transaction.png`<br>`docs/evidence/screenshots/desktop-edit-transaction.png` | **Aligned** |
| **Transaction List** | `budget-transaction-listing.png` | `dashboard-transaction.png` | Month selector header, list items with 40dp category icon badge, description, category + date subtitle (`${category.displayName} • ${date}`), and signed amount. | Spending category chart, filter bar chips, category breakdown pie chart omitted per contract Section 3. | `docs/evidence/screenshots/mobile-transaction-list-populated.png`<br>`docs/evidence/screenshots/desktop-transaction-list-populated.png` | **Aligned** |

### Visual Comparison Against Design References

#### Mobile Viewport (390x844 dp)
| Captured Screen | Reference Mockup (`docs/design/reference/pocket-budget-mobile-design/`) | Visual Review Summary |
| :--- | :--- | :--- |
| `mobile-add-transaction-expense.png` | `pocket-budget-mobile-design/add-expense.png` | Aligned. Segmented pill switch, hero -$12.50 amount, card container with amount, description, date row, and 3-column category grid with selected Groceries chip matching active green border and bold title. |
| `mobile-add-transaction-income.png` | `pocket-budget-mobile-design/add-income.png` | Aligned. Green +$750.00 hero amount, income category options (Salary, Freelance, Investment, Gift, Other) in 3-column grid with Freelance selected, Save Income button. |
| `mobile-edit-transaction.png` | `pocket-budget-mobile-design/add-expense.png` | Aligned. Back/close and delete trash icon in header, pre-populated values with Dining category selected, Save Changes button. |
| `mobile-transaction-list-populated.png` | `pocket-budget-mobile-design/budget-transaction-listing.png` | Aligned. Month header, items render category icon badges (cart, cash, car), category + date subtitle (`Groceries • Sep 8, 2026`), and signed amount colors. |

#### Desktop Viewport (1024x768 dp)
| Captured Screen | Reference Mockup (`docs/design/reference/pocket-budget-desktop-design/`) | Visual Review Summary |
| :--- | :--- | :--- |
| `desktop-add-transaction.png` | `pocket-budget-desktop-design/add-transaction.png` | Aligned. Centered 680dp card, Expense/Income pill, dedicated soft mint Amount container, two-column Description + Date row, 4-column category grid with Shopping selected, Cancel and Save Transaction buttons. |
| `desktop-edit-transaction.png` | `pocket-budget-desktop-design/add-transaction.png` | Aligned. Centered 680dp card with delete action, pre-filled fields, Dining category selected in 4-column grid, Cancel and Save Changes buttons. |
| `desktop-transaction-list-populated.png` | `pocket-budget-desktop-design/dashboard-transaction.png` | Aligned. Header with month controls and "+ New Transaction" CTA, transaction rows displaying category badge icons, description, and category + date subtitles. |

### Design Token Alignment
- **Colors**: `PocketColors.Primary` (`#005136`), `Surface` (`#EEFDF2`), `SurfaceContainer` (`#E3F2E7`), `SurfaceContainerHigh` (`#DDECE1`), `Income` (`#059669`), `Expense` (`#BA1A1A`).
- **Typography**: Label Medium (`12.sp`, semi-bold) for section headers, `12.sp` medium/semibold for category chips, `15.sp` / `13.sp` for transaction row descriptions and subtitles.
- **Shapes**: `16.dp` rounded cards and category chips, capsule pill toggles.
- **Spacing**: 8pt grid with 10dp/12dp spacing for category cards.

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
- `./gradlew :shared:iosSimulatorArm64Test`: `NOT RUN` (Target requires ARM64, host is macOS x86_64)
- `./scripts/verify.sh`: `PASS`

## 8. Limitations & Unverified Targets
- **iOS Simulator Tests**: `NOT RUN` (host platform is macOS x86_64, whereas the iOS simulator target requires ARM64).
