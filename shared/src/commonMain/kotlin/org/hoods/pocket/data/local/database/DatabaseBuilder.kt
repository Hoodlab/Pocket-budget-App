package org.hoods.pocket.data.local.database

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE transactions ADD COLUMN category TEXT NOT NULL DEFAULT 'UNCATEGORIZED'")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS category_budgets (
                year INTEGER NOT NULL,
                month INTEGER NOT NULL,
                category TEXT NOT NULL,
                limitAmount INTEGER NOT NULL,
                PRIMARY KEY (year, month, category)
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE transactions ADD COLUMN userId TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE monthly_budgets ADD COLUMN userId TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE category_budgets ADD COLUMN userId TEXT DEFAULT NULL")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        // 1. Add sync tracking columns to transactions
        connection.execSQL("ALTER TABLE transactions ADD COLUMN updatedAtEpochMillis INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE transactions ADD COLUMN deletedAtEpochMillis INTEGER DEFAULT NULL")
        connection.execSQL("UPDATE transactions SET updatedAtEpochMillis = dateEpochMillis WHERE updatedAtEpochMillis = 0")

        // 2. Add sync tracking columns to monthly_budgets
        connection.execSQL("ALTER TABLE monthly_budgets ADD COLUMN updatedAtEpochMillis INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE monthly_budgets ADD COLUMN deletedAtEpochMillis INTEGER DEFAULT NULL")

        // 3. Add sync tracking columns to category_budgets
        connection.execSQL("ALTER TABLE category_budgets ADD COLUMN updatedAtEpochMillis INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE category_budgets ADD COLUMN deletedAtEpochMillis INTEGER DEFAULT NULL")

        // 4. Create sync_outbox table
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sync_outbox (
                mutationId TEXT PRIMARY KEY NOT NULL,
                userId TEXT NOT NULL,
                deviceId TEXT NOT NULL,
                entityType TEXT NOT NULL,
                entityId TEXT NOT NULL,
                operation TEXT NOT NULL,
                payloadJson TEXT NOT NULL,
                updatedAtEpochMillis INTEGER NOT NULL,
                status TEXT NOT NULL,
                retryCount INTEGER NOT NULL DEFAULT 0,
                lastError TEXT,
                createdAtEpochMillis INTEGER NOT NULL
            )
            """.trimIndent()
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_sync_outbox_userId_status_createdAtEpochMillis ON sync_outbox (userId, status, createdAtEpochMillis)")

        // 5. Create sync_metadata table
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sync_metadata (
                userId TEXT PRIMARY KEY NOT NULL,
                lastSyncCursor TEXT,
                lastSyncAtEpochMillis INTEGER,
                lastSyncStatus TEXT NOT NULL,
                lastSyncError TEXT
            )
            """.trimIndent()
        )
    }
}

fun createRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
        .fallbackToDestructiveMigration(true)
        .build()
}
