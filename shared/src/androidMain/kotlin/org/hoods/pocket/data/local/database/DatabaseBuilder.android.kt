package org.hoods.pocket.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.hoods.pocket.data.repository.RoomTransactionRepository
import org.hoods.pocket.domain.repository.TransactionRepository

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("pocket_budget.db")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}

fun createTransactionRepository(context: Context): TransactionRepository {
    val database = createRoomDatabase(getDatabaseBuilder(context))
    return RoomTransactionRepository(database.transactionDao())
}
