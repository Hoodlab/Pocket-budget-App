package org.hoods.pocket.data.local.database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File
import org.hoods.pocket.data.repository.RoomTransactionRepository
import org.hoods.pocket.domain.repository.TransactionRepository

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(System.getProperty("user.home"), ".pocket-budget/pocket_budget.db")
    dbFile.parentFile?.mkdirs()
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath
    )
}

fun createTransactionRepository(): TransactionRepository {
    val database = createRoomDatabase(getDatabaseBuilder())
    return RoomTransactionRepository(database.transactionDao())
}
