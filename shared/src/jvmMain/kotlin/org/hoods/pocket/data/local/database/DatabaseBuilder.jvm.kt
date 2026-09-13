package org.hoods.pocket.data.local.database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File
import org.hoods.pocket.data.repository.RoomBudgetRepository
import org.hoods.pocket.data.repository.RoomCategoryBudgetRepository
import org.hoods.pocket.data.repository.RoomTransactionRepository
import org.hoods.pocket.domain.repository.BudgetRepository
import org.hoods.pocket.domain.repository.CategoryBudgetRepository
import org.hoods.pocket.domain.repository.TransactionRepository

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(System.getProperty("user.home"), ".pocket-budget-course/pocket_budget.db")
    dbFile.parentFile?.mkdirs()
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath
    )
}

private var databaseInstance: AppDatabase? = null

fun getSharedAppDatabase(): AppDatabase {
    return databaseInstance ?: synchronized(Unit) {
        databaseInstance ?: createRoomDatabase(getDatabaseBuilder()).also { databaseInstance = it }
    }
}

fun createTransactionRepository(): TransactionRepository {
    return RoomTransactionRepository(getSharedAppDatabase().transactionDao())
}

fun createBudgetRepository(): BudgetRepository {
    return RoomBudgetRepository(getSharedAppDatabase().budgetDao())
}

fun createCategoryBudgetRepository(): CategoryBudgetRepository {
    return RoomCategoryBudgetRepository(getSharedAppDatabase().categoryBudgetDao())
}
