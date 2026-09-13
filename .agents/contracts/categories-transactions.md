# Implementation Contract: Categorise Transactions

## 1. User Outcome

- A user can choose a category when adding a transaction.
- A user can see the selected category in the transaction list.
- A user can open an existing transaction and change its category.
- Saving a changed category updates the existing transaction rather than creating a duplicate.
- Existing saved transactions remain available after the database change.
- Existing saved transactions are shown as `Uncategorized` rather than being assigned a category the user did not choose.
- Selected categories remain correct after restarting the app.

## 2. In-Scope Behavior

- **Predefined Transaction Categories:** The application provides a small fixed set of transaction categories for the MVP, including `Uncategorized`.
- **Transaction Domain Model:** Each transaction has one category represented by the shared domain model.
- **Database Migration:** A Room migration updates existing transaction records safely and assigns `Uncategorized` to records created before categories existed.
- **Transaction Repository:** The repository saves, updates, retrieves, and maps transaction categories between domain and local storage.
- **Add Transaction Form:** The add form allows the user to choose a category before saving a transaction.
- **Edit Transaction Form:** The edit form displays the saved category and allows the user to choose a different category.
- **Transaction List UI:** Transaction rows display the stored category alongside existing transaction information.
- **Persistence Across Restarts:** Saved categories remain correct in SQLite/Room after the application restarts.
- **Responsive Design:** Category controls and category labels follow the project design system and preserve the approved mobile and desktop layout anchors.

## 3. Explicit Exclusions

- Custom category creation, editing, reordering, or deletion.
- User-uploaded category icons.
- Category budgets or category allocations.
- Category spending charts, analytics, recommendations, or reports.
- Authentication, user accounts, and cloud synchronization.
- Recurring transactions.

## 4. Acceptance Criteria

- [ ] Each transaction has one valid predefined category.
- [ ] The predefined category set includes `Uncategorized`.
- [ ] Existing transactions migrate safely to `Uncategorized`.
- [ ] The migration preserves every existing transaction’s ID, description, amount, type, and date.
- [ ] A user can choose a category when adding a transaction.
- [ ] A user can see the selected category in the transaction list.
- [ ] The edit form pre-fills the transaction’s stored category.
- [ ] Saving a changed category updates the existing transaction without creating a duplicate.
- [ ] Category data remains correct after the application restarts.
- [ ] Existing add, edit, delete, date-selection, month-browsing, and monthly-budget behaviour remains intact.
- [ ] Mobile and desktop evidence is captured and reviewed using the design-alignment matrix.

## 5. Verification Plan

### Automated Behavior Tests

- **Test-First Evidence:** Provide genuine RED and GREEN evidence for category persistence and the Room migration.
- **Migration Tests:** Verify a database containing pre-category transactions migrates successfully and exposes those transactions as `Uncategorized`.
- **Repository Tests:** Verify categories are stored, retrieved, and mapped correctly.
- **Presentation Tests:** Verify add and edit ViewModels expose and save category selection correctly.
- **User Journey Test:** Cover add categorised transaction → view category in list → edit category → verify list update → restart and verify persistence.

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

- Capture the add-transaction form, edit-transaction form, and transaction list at mobile and desktop viewport sizes.
- Review each capture against its matching design reference and the design-alignment matrix.
- Record one of these results for each screen: `Aligned`, `Approved deviation`, or `Correction required`.

### Manual Verification Journey

1. Launch the app on Android or Desktop.
2. Add a transaction and choose a category.
3. Verify the selected category appears in the transaction list.
4. Open that transaction for editing.
5. Change its category and save.
6. Verify the existing transaction updates without creating a duplicate.
7. Close and reopen the application.
8. Verify the transaction and its updated category remain correct.

## 6. Constraints & Rules

- **Category Truthfulness:** Existing transactions must migrate to `Uncategorized`. Do not infer or guess
