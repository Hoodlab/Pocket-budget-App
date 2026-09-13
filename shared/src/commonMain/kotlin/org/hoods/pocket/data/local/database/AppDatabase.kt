package org.hoods.pocket.data.local.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import org.hoods.pocket.data.local.dao.BudgetDao
import org.hoods.pocket.data.local.dao.CategoryBudgetDao
import org.hoods.pocket.data.local.dao.TransactionDao
import org.hoods.pocket.data.local.entity.CategoryBudgetEntity
import org.hoods.pocket.data.local.entity.MonthlyBudgetEntity
import org.hoods.pocket.data.local.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class, MonthlyBudgetEntity::class, CategoryBudgetEntity::class],
    version = 4,
    exportSchema = false
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryBudgetDao(): CategoryBudgetDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>
