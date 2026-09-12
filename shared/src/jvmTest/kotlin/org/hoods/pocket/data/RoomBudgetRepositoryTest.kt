package org.hoods.pocket.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.data.local.database.AppDatabase
import org.hoods.pocket.data.repository.RoomBudgetRepository
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.YearMonth
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoomBudgetRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: RoomBudgetRepository

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = RoomBudgetRepository(database.budgetDao())
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun setMonthlyBudget_persistsAndEmitsInGetMonthlyBudget() = runTest {
        val targetMonth = YearMonth(2026, 9)
        val budget = MonthlyBudget(targetMonth, 120000L) // $1,200.00

        repository.setMonthlyBudget(budget)

        val retrieved = repository.getMonthlyBudget(targetMonth).first()
        assertEquals(budget, retrieved)
    }

    @Test
    fun getMonthlyBudget_returnsNull_whenNoBudgetSavedForMonth() = runTest {
        val targetMonth = YearMonth(2026, 9)
        val otherMonth = YearMonth(2026, 10)

        repository.setMonthlyBudget(MonthlyBudget(targetMonth, 120000L))

        val retrieved = repository.getMonthlyBudget(otherMonth).first()
        assertNull(retrieved)
    }

    @Test
    fun setMonthlyBudget_overwritesExistingBudgetForSameMonth() = runTest {
        val targetMonth = YearMonth(2026, 9)

        repository.setMonthlyBudget(MonthlyBudget(targetMonth, 100000L))
        val firstSaved = repository.getMonthlyBudget(targetMonth).first()
        assertEquals(100000L, firstSaved?.limitAmount)

        repository.setMonthlyBudget(MonthlyBudget(targetMonth, 150000L))
        val updated = repository.getMonthlyBudget(targetMonth).first()
        assertEquals(150000L, updated?.limitAmount)
    }

    @Test
    fun persistenceAcrossReopen_preservesSavedBudget() = runTest {
        val tempFile = File.createTempFile("budget_test", ".db")
        tempFile.deleteOnExit()

        val targetMonth = YearMonth(2026, 8)
        val budget = MonthlyBudget(targetMonth, 300000L)

        // 1. First session: open, insert, close
        val db1 = Room.databaseBuilder<AppDatabase>(tempFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        val repo1 = RoomBudgetRepository(db1.budgetDao())
        repo1.setMonthlyBudget(budget)
        db1.close()

        // 2. Second session (simulating app restart): open same file, query
        val db2 = Room.databaseBuilder<AppDatabase>(tempFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        val repo2 = RoomBudgetRepository(db2.budgetDao())
        val retrieved = repo2.getMonthlyBudget(targetMonth).first()
        db2.close()

        assertEquals(budget, retrieved)
        tempFile.delete()
    }

    @Test
    fun deleteMonthlyBudget_removesSavedBudgetAndEmitsNull() = runTest {
        val targetMonth = YearMonth(2026, 9)
        val budget = MonthlyBudget(targetMonth, 120000L)

        repository.setMonthlyBudget(budget)
        val saved = repository.getMonthlyBudget(targetMonth).first()
        assertEquals(budget, saved)

        repository.deleteMonthlyBudget(targetMonth)

        val afterDelete = repository.getMonthlyBudget(targetMonth).first()
        assertNull(afterDelete)
    }

    @Test
    fun setMonthlyBudget_replacesExistingWithoutDuplicate() = runTest {
        val targetMonth = YearMonth(2026, 9)

        repository.setMonthlyBudget(MonthlyBudget(targetMonth, 100000L))
        repository.setMonthlyBudget(MonthlyBudget(targetMonth, 150000L))

        val allBudgets = database.budgetDao().getAllBudgets().first()
        assertEquals(1, allBudgets.size)
        assertEquals(150000L, allBudgets[0].limitAmount)
    }
}
