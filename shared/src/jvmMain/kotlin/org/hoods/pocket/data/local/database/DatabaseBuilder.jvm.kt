package org.hoods.pocket.data.local.database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File
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

private var authRepositoryInstance: AuthRepository? = null

fun getSharedAuthRepository(config: SupabaseConfig = SupabaseConfig.unconfigured): AuthRepository {
    return authRepositoryInstance ?: synchronized(Unit) {
        authRepositoryInstance ?: SupabaseAuthRepository(config).also { authRepositoryInstance = it }
    }
}

fun createAuthRepository(config: SupabaseConfig = SupabaseConfig.unconfigured): AuthRepository {
    return getSharedAuthRepository(config)
}

fun createTransactionRepository(authRepository: AuthRepository = getSharedAuthRepository()): TransactionRepository {
    return RoomTransactionRepository(getSharedAppDatabase().transactionDao(), authRepository = authRepository)
}

fun createBudgetRepository(authRepository: AuthRepository = getSharedAuthRepository()): BudgetRepository {
    return RoomBudgetRepository(getSharedAppDatabase().budgetDao(), authRepository = authRepository)
}

fun createCategoryBudgetRepository(authRepository: AuthRepository = getSharedAuthRepository()): CategoryBudgetRepository {
    return RoomCategoryBudgetRepository(getSharedAppDatabase().categoryBudgetDao(), authRepository = authRepository)
}

fun createDataOwnershipRepository(): DataOwnershipRepository {
    return RoomDataOwnershipRepository(getSharedAppDatabase().dataOwnershipDao())
}
