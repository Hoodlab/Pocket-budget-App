package org.hoods.pocket.data.local.database

import androidx.room.Room
import androidx.sqlite.SQLiteException
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.data.local.entity.MonthlyBudgetEntity
import org.hoods.pocket.data.local.entity.TransactionEntity
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RoomDataOwnershipPreMigrationTest {

    @Test
    fun version4Database_lacksUserIdColumns_provingNeedForMigration45() = runTest {
        val tempFile = File.createTempFile("pre_migration_v4", ".db")
        tempFile.deleteOnExit()

        try {
            // 1. Create a genuine legacy v4 SQLite database using raw BundledSQLiteDriver
            val driver = BundledSQLiteDriver()
            val connection = driver.open(tempFile.absolutePath)

            connection.execSQL(
                """
                CREATE TABLE transactions (
                    id TEXT PRIMARY KEY NOT NULL,
                    description TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    dateEpochMillis INTEGER NOT NULL,
                    category TEXT NOT NULL
                )
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE TABLE monthly_budgets (
                    year INTEGER NOT NULL,
                    month INTEGER NOT NULL,
                    limitAmount INTEGER NOT NULL,
                    PRIMARY KEY(year, month)
                )
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE TABLE category_budgets (
                    year INTEGER NOT NULL,
                    month INTEGER NOT NULL,
                    category TEXT NOT NULL,
                    limitAmount INTEGER NOT NULL,
                    PRIMARY KEY(year, month, category)
                )
                """.trimIndent()
            )

            connection.execSQL("PRAGMA user_version = 4")

            // 2. Assert that querying userId on transactions fails on version 4
            assertFailsWith<SQLiteException> {
                connection.execSQL("SELECT userId FROM transactions")
            }

            // Assert that querying userId on monthly_budgets fails on version 4
            assertFailsWith<SQLiteException> {
                connection.execSQL("SELECT userId FROM monthly_budgets")
            }

            // Assert that querying userId on category_budgets fails on version 4
            assertFailsWith<SQLiteException> {
                connection.execSQL("SELECT userId FROM category_budgets")
            }

            connection.close()
        } finally {
            tempFile.delete()
        }
    }
}
