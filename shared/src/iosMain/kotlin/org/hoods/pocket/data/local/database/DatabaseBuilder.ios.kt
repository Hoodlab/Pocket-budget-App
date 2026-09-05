package org.hoods.pocket.data.local.database

import androidx.room.Room
import androidx.room.RoomDatabase
import org.hoods.pocket.data.repository.RoomTransactionRepository
import org.hoods.pocket.domain.repository.TransactionRepository
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    val path = requireNotNull(documentDirectory?.path)
    return Room.databaseBuilder<AppDatabase>(
        name = "$path/pocket_budget.db"
    )
}

fun createTransactionRepository(): TransactionRepository {
    val database = createRoomDatabase(getDatabaseBuilder())
    return RoomTransactionRepository(database.transactionDao())
}
