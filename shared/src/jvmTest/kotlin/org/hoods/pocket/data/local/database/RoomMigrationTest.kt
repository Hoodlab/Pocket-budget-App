package org.hoods.pocket.data.local.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.data.repository.RoomTransactionRepository
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.TransactionType
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RoomMigrationTest {

    @Test
    fun migration_2_to_3_preservesExistingTransactionsAndAssignsUncategorized() = runTest {
        val tempFile = File.createTempFile("migration_test_v2_v3", ".db")
        tempFile.deleteOnExit()

        try {
            // 1. Create a legacy v2 SQLite database using raw BundledSQLiteDriver
            val driver = BundledSQLiteDriver()
            val connection = driver.open(tempFile.absolutePath)

            // Create transactions table schema as it existed in version 2 (NO category column)
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS transactions (
                    id TEXT PRIMARY KEY NOT NULL,
                    description TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    dateEpochMillis INTEGER NOT NULL
                )
                """.trimIndent()
            )

            // Create monthly_budgets table as it existed in version 2
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS monthly_budgets (
                    year INTEGER NOT NULL,
                    month INTEGER NOT NULL,
                    limitAmount INTEGER NOT NULL,
                    PRIMARY KEY(year, month)
                )
                """.trimIndent()
            )

            // Set user_version to 2
            connection.execSQL("PRAGMA user_version = 2")

            // Insert existing transactions created before categories existed
            connection.execSQL(
                """
                INSERT INTO transactions (id, description, amount, type, dateEpochMillis)
                VALUES ('tx-legacy-1', 'Whole Foods Groceries', 5420, 'EXPENSE', 1717171717000)
                """.trimIndent()
            )
            connection.execSQL(
                """
                INSERT INTO transactions (id, description, amount, type, dateEpochMillis)
                VALUES ('tx-legacy-2', 'Acme Corp Salary', 450000, 'INCOME', 1717171800000)
                """.trimIndent()
            )

            connection.close()

            // 2. Open with Room AppDatabase version 3 with MIGRATION_2_3 applied
            val database = Room.databaseBuilder<AppDatabase>(name = tempFile.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .build()

            val repository = RoomTransactionRepository(database.transactionDao())
            val transactions = repository.getTransactions().first()

            // 3. Verify that both transactions survived and were assigned Uncategorized
            assertEquals(2, transactions.size)

            val tx1 = transactions.find { it.id == "tx-legacy-1" }
            assertNotNull(tx1)
            assertEquals("Whole Foods Groceries", tx1.description)
            assertEquals(5420L, tx1.amount)
            assertEquals(TransactionType.EXPENSE, tx1.type)
            assertEquals(1717171717000L, tx1.date.toEpochMilliseconds())
            // CRITICAL: Must be UNCATEGORIZED without guessing from description "Groceries"
            assertEquals(TransactionCategory.UNCATEGORIZED, tx1.category)

            val tx2 = transactions.find { it.id == "tx-legacy-2" }
            assertNotNull(tx2)
            assertEquals("Acme Corp Salary", tx2.description)
            assertEquals(450000L, tx2.amount)
            assertEquals(TransactionType.INCOME, tx2.type)
            assertEquals(1717171800000L, tx2.date.toEpochMilliseconds())
            // CRITICAL: Must be UNCATEGORIZED without guessing from description "Salary"
            assertEquals(TransactionCategory.UNCATEGORIZED, tx2.category)

            database.close()
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun migration_3_to_4_createsCategoryBudgetsTableAndPreservesData() = runTest {
        val tempFile = File.createTempFile("migration_test_v3_v4", ".db")
        tempFile.deleteOnExit()

        try {
            // 1. Create a legacy v3 SQLite database
            val driver = BundledSQLiteDriver()
            val connection = driver.open(tempFile.absolutePath)

            // Version 3 transactions table (with category)
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS transactions (
                    id TEXT PRIMARY KEY NOT NULL,
                    description TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    dateEpochMillis INTEGER NOT NULL,
                    category TEXT NOT NULL
                )
                """.trimIndent()
            )

            // Version 3 monthly_budgets table
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS monthly_budgets (
                    year INTEGER NOT NULL,
                    month INTEGER NOT NULL,
                    limitAmount INTEGER NOT NULL,
                    PRIMARY KEY(year, month)
                )
                """.trimIndent()
            )

            connection.execSQL("PRAGMA user_version = 3")

            // Insert sample transaction and monthly budget in v3
            connection.execSQL(
                """
                INSERT INTO transactions (id, description, amount, type, dateEpochMillis, category)
                VALUES ('tx-v3-1', 'Trader Joes', 3200, 'EXPENSE', 1717171717000, 'GROCERIES')
                """.trimIndent()
            )
            connection.execSQL(
                """
                INSERT INTO monthly_budgets (year, month, limitAmount)
                VALUES (2026, 8, 300000)
                """.trimIndent()
            )

            connection.close()

            // 2. Open with Room AppDatabase version 4 with MIGRATION_2_3 and MIGRATION_3_4
            val database = Room.databaseBuilder<AppDatabase>(name = tempFile.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .build()

            // 3. Verify v3 data survived
            val txRepo = RoomTransactionRepository(database.transactionDao())
            val transactions = txRepo.getTransactions().first()
            assertEquals(1, transactions.size)
            assertEquals("Trader Joes", transactions[0].description)
            assertEquals(TransactionCategory.GROCERIES, transactions[0].category)

            val budgetRepo = org.hoods.pocket.data.repository.RoomBudgetRepository(database.budgetDao())
            val targetMonth = org.hoods.pocket.domain.model.YearMonth(2026, 8)
            val budget = budgetRepo.getMonthlyBudget(targetMonth).first()
            assertNotNull(budget)
            assertEquals(300000L, budget.limitAmount)

            // 4. Verify category_budgets table works
            val categoryRepo = org.hoods.pocket.data.repository.RoomCategoryBudgetRepository(database.categoryBudgetDao())
            val initialCategories = categoryRepo.getCategoryBudgets(targetMonth).first()
            assertEquals(0, initialCategories.size)

            val groceryBudget = org.hoods.pocket.domain.model.CategoryBudget(
                yearMonth = targetMonth,
                category = TransactionCategory.GROCERIES,
                limitAmount = 60000L
            )
            categoryRepo.setCategoryBudget(groceryBudget)

            val afterSave = categoryRepo.getCategoryBudgets(targetMonth).first()
            assertEquals(1, afterSave.size)
            assertEquals(TransactionCategory.GROCERIES, afterSave[0].category)
            assertEquals(60000L, afterSave[0].limitAmount)

            database.close()
        } finally {
            tempFile.delete()
        }
    }
}
