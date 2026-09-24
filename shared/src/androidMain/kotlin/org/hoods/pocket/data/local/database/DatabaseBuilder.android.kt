package org.hoods.pocket.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.hoods.pocket.data.remote.SupabaseConfig
import org.hoods.pocket.data.repository.RoomBudgetRepository
import org.hoods.pocket.data.repository.RoomCategoryBudgetRepository
import org.hoods.pocket.data.repository.RoomDataOwnershipRepository
import org.hoods.pocket.data.repository.RoomTransactionRepository
import org.hoods.pocket.data.repository.SupabaseAuthRepository
import org.hoods.pocket.domain.repository.AuthRepository
import org.hoods.pocket.domain.repository.BudgetRepository
import org.hoods.pocket.domain.repository.CategoryBudgetRepository
import org.hoods.pocket.domain.repository.DataOwnershipRepository
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

private var authRepositoryInstance: AuthRepository? = null

fun getSharedAuthRepository(config: SupabaseConfig = SupabaseConfig.unconfigured): AuthRepository {
    return authRepositoryInstance ?: synchronized(Unit) {
        authRepositoryInstance ?: SupabaseAuthRepository(config).also { authRepositoryInstance = it }
    }
}

fun createAuthRepository(config: SupabaseConfig = SupabaseConfig.unconfigured): AuthRepository {
    return getSharedAuthRepository(config)
}

fun createTransactionRepository(context: Context, authRepository: AuthRepository = getSharedAuthRepository()): TransactionRepository {
    return RoomTransactionRepository(getSharedAppDatabase(context).transactionDao(), authRepository = authRepository)
}

fun createBudgetRepository(context: Context, authRepository: AuthRepository = getSharedAuthRepository()): BudgetRepository {
    return RoomBudgetRepository(getSharedAppDatabase(context).budgetDao(), authRepository = authRepository)
}

fun createCategoryBudgetRepository(context: Context, authRepository: AuthRepository = getSharedAuthRepository()): CategoryBudgetRepository {
    return RoomCategoryBudgetRepository(getSharedAppDatabase(context).categoryBudgetDao(), authRepository = authRepository)
}

fun createDataOwnershipRepository(context: Context): DataOwnershipRepository {
    return RoomDataOwnershipRepository(getSharedAppDatabase(context).dataOwnershipDao())
}
