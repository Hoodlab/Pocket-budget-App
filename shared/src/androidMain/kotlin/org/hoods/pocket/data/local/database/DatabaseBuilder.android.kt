package org.hoods.pocket.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.hoods.pocket.data.repository.RoomBudgetRepository
import org.hoods.pocket.data.repository.RoomCategoryBudgetRepository
import org.hoods.pocket.data.repository.RoomTransactionRepository
import org.hoods.pocket.domain.repository.BudgetRepository
import org.hoods.pocket.domain.repository.CategoryBudgetRepository
import org.hoods.pocket.domain.repository.TransactionRepository

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("pocket_budget.db")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}

private var databaseInstance: AppDatabase? = null

fun getSharedAppDatabase(context: Context): AppDatabase {
    return databaseInstance ?: synchronized(Unit) {
        databaseInstance ?: createRoomDatabase(getDatabaseBuilder(context)).also { databaseInstance = it }
    }
}

fun createTransactionRepository(context: Context): TransactionRepository {
    return RoomTransactionRepository(getSharedAppDatabase(context).transactionDao())
}

fun createBudgetRepository(context: Context): BudgetRepository {
    return RoomBudgetRepository(getSharedAppDatabase(context).budgetDao())
}

fun createCategoryBudgetRepository(context: Context): CategoryBudgetRepository {
    return RoomCategoryBudgetRepository(getSharedAppDatabase(context).categoryBudgetDao())
}
