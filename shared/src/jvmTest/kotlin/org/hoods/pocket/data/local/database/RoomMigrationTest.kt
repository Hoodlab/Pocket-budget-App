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
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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

    @Test
    fun migration_4_to_5_addsUserIdColumnsAndPreservesExistingData() = runTest {
        val tempFile = File.createTempFile("migration_test_v4_v5", ".db")
        tempFile.deleteOnExit()

        try {
            // 1. Create a legacy v4 SQLite database
            val driver = BundledSQLiteDriver()
            val connection = driver.open(tempFile.absolutePath)

            // Version 4 transactions table (with category, NO userId)
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

            // Version 4 monthly_budgets table (NO userId)
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

            // Version 4 category_budgets table (NO userId)
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS category_budgets (
                    year INTEGER NOT NULL,
                    month INTEGER NOT NULL,
                    category TEXT NOT NULL,
                    limitAmount INTEGER NOT NULL,
                    PRIMARY KEY(year, month, category)
                )
                """.trimIndent()
            )

            connection.execSQL("PRAGMA user_version = 4")

            // Insert sample transaction, monthly budget, and category budget in v4
            connection.execSQL(
                """
                INSERT INTO transactions (id, description, amount, type, dateEpochMillis, category)
                VALUES ('tx-v4-1', 'Target Groceries', 8500, 'EXPENSE', 1717171717000, 'GROCERIES')
                """.trimIndent()
            )
            connection.execSQL(
                """
                INSERT INTO monthly_budgets (year, month, limitAmount)
                VALUES (2026, 9, 400000)
                """.trimIndent()
            )
            connection.execSQL(
                """
                INSERT INTO category_budgets (year, month, category, limitAmount)
                VALUES (2026, 9, 'GROCERIES', 80000)
                """.trimIndent()
            )

            connection.close()

            // 2. Open with Room AppDatabase version 5 with MIGRATION_4_5 applied
            val database = Room.databaseBuilder<AppDatabase>(name = tempFile.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()

            // 3. Verify v4 data survived and userId is null (unassigned)
            val txRepo = RoomTransactionRepository(database.transactionDao())
            val transactions = txRepo.getTransactions().first()
            assertEquals(1, transactions.size)
            assertEquals("Target Groceries", transactions[0].description)
            assertEquals(8500L, transactions[0].amount)

            // Verify unclaimed counts via DataOwnershipDao
            val ownershipDao = database.dataOwnershipDao()
            assertEquals(1, ownershipDao.getUnclaimedTransactionCount())
            assertEquals(1, ownershipDao.getUnclaimedMonthlyBudgetCount())
            assertEquals(1, ownershipDao.getUnclaimedCategoryBudgetCount())

            // 4. Test atomic claiming
            ownershipDao.claimAllUnassigned("test-account-uuid")

            assertEquals(0, ownershipDao.getUnclaimedTransactionCount())
            assertEquals(0, ownershipDao.getUnclaimedMonthlyBudgetCount())
            assertEquals(0, ownershipDao.getUnclaimedCategoryBudgetCount())

            database.close()
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun migration_5_to_6_addsSyncColumnsAndOutboxTables() = runTest {
        val tempFile = File.createTempFile("migration_test_v5_v6", ".db")
        tempFile.deleteOnExit()

        try {
            // 1. Create a legacy v5 SQLite database
            val driver = BundledSQLiteDriver()
            val connection = driver.open(tempFile.absolutePath)

            // Version 5 transactions table (with userId, NO sync columns)
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS transactions (
                    id TEXT PRIMARY KEY NOT NULL,
                    description TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    dateEpochMillis INTEGER NOT NULL,
                    category TEXT NOT NULL,
                    userId TEXT DEFAULT NULL
                )
                """.trimIndent()
            )

            // Version 5 monthly_budgets table (with userId, NO sync columns)
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS monthly_budgets (
                    year INTEGER NOT NULL,
                    month INTEGER NOT NULL,
                    limitAmount INTEGER NOT NULL,
                    userId TEXT DEFAULT NULL,
                    PRIMARY KEY(year, month)
                )
                """.trimIndent()
            )

            // Version 5 category_budgets table (with userId, NO sync columns)
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS category_budgets (
                    year INTEGER NOT NULL,
                    month INTEGER NOT NULL,
                    category TEXT NOT NULL,
                    limitAmount INTEGER NOT NULL,
                    userId TEXT DEFAULT NULL,
                    PRIMARY KEY(year, month, category)
                )
                """.trimIndent()
            )

            connection.execSQL("PRAGMA user_version = 5")

            // Insert sample transaction and budgets
            connection.execSQL(
                """
                INSERT INTO transactions (id, description, amount, type, dateEpochMillis, category, userId)
                VALUES ('tx-v5-1', 'Target Groceries', 8500, 'EXPENSE', 1717171717000, 'GROCERIES', 'user-123')
                """.trimIndent()
            )
            connection.execSQL(
                """
                INSERT INTO monthly_budgets (year, month, limitAmount, userId)
                VALUES (2026, 9, 400000, 'user-123')
                """.trimIndent()
            )
            connection.execSQL(
                """
                INSERT INTO category_budgets (year, month, category, limitAmount, userId)
                VALUES (2026, 9, 'GROCERIES', 80000, 'user-123')
                """.trimIndent()
            )

            connection.close()

            // 2. Open with Room AppDatabase version 6 with MIGRATION_5_6 applied
            val database = Room.databaseBuilder<AppDatabase>(name = tempFile.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()

            // 3. Verify v5 transaction survived and has updatedAtEpochMillis
            val txEntity = database.transactionDao().getTransactionById("tx-v5-1", "user-123")
            assertNotNull(txEntity)
            assertEquals("Target Groceries", txEntity.description)
            assertEquals(8500L, txEntity.amount)
            assertEquals(1717171717000L, txEntity.updatedAtEpochMillis)
            assertEquals(null, txEntity.deletedAtEpochMillis)

            // 4. Verify sync_outbox and sync_metadata tables are operational
            val outboxDao = database.syncOutboxDao()
            assertEquals(0, outboxDao.getPendingMutationCount("user-123"))

            val outboxEntity = org.hoods.pocket.data.local.entity.SyncOutboxEntity(
                mutationId = "mut-test-1",
                userId = "user-123",
                deviceId = "device-abc",
                entityType = "TRANSACTION",
                entityId = "tx-v5-1",
                operation = "UPSERT",
                payloadJson = "{}",
                updatedAtEpochMillis = 1717171717000L,
                status = "PENDING",
                createdAtEpochMillis = 1717171717000L
            )
            outboxDao.insertOrUpdateMutation(outboxEntity)
            assertEquals(1, outboxDao.getPendingMutationCount("user-123"))

            val metadataDao = database.syncMetadataDao()
            val initialMeta = metadataDao.getMetadata("user-123")
            assertEquals(null, initialMeta)

            metadataDao.upsertMetadata(
                org.hoods.pocket.data.local.entity.SyncMetadataEntity(
                    userId = "user-123",
                    lastSyncCursor = "2026-09-20T10:00:00Z",
                    lastSyncAtEpochMillis = 1717171800000L,
                    lastSyncStatus = "SUCCESS"
                )
            )
            val updatedMeta = metadataDao.getMetadata("user-123")
            assertNotNull(updatedMeta)
            assertEquals("SUCCESS", updatedMeta.lastSyncStatus)
            assertEquals("2026-09-20T10:00:00Z", updatedMeta.lastSyncCursor)

            database.close()
        } finally {
            tempFile.delete()
        }
    }
}
