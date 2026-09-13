package org.hoods.pocket.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.data.local.database.AppDatabase
import org.hoods.pocket.data.repository.RoomCategoryBudgetRepository
import org.hoods.pocket.domain.model.CategoryBudget
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.YearMonth
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomCategoryBudgetRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: RoomCategoryBudgetRepository

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = RoomCategoryBudgetRepository(database.categoryBudgetDao())
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun setCategoryBudget_persistsAndEmitsInGetCategoryBudgets() = runTest {
        val targetMonth = YearMonth(2026, 8)
        val budget = CategoryBudget(targetMonth, TransactionCategory.GROCERIES, 60000L)

        repository.setCategoryBudget(budget)

        val retrieved = repository.getCategoryBudgets(targetMonth).first()
        assertEquals(1, retrieved.size)
        assertEquals(budget, retrieved[0])
    }

    @Test
    fun getCategoryBudgets_returnsEmpty_whenNoBudgetsForMonth() = runTest {
        val targetMonth = YearMonth(2026, 8)
        val otherMonth = YearMonth(2026, 9)

        repository.setCategoryBudget(CategoryBudget(targetMonth, TransactionCategory.GROCERIES, 60000L))

        val retrieved = repository.getCategoryBudgets(otherMonth).first()
        assertTrue(retrieved.isEmpty())
    }

    @Test
    fun setCategoryBudget_overwritesExistingBudgetForSameCategoryWithoutDuplicate() = runTest {
        val targetMonth = YearMonth(2026, 8)

        repository.setCategoryBudget(CategoryBudget(targetMonth, TransactionCategory.GROCERIES, 50000L))
        val firstSaved = repository.getCategoryBudgets(targetMonth).first()
        assertEquals(1, firstSaved.size)
        assertEquals(50000L, firstSaved[0].limitAmount)

        repository.setCategoryBudget(CategoryBudget(targetMonth, TransactionCategory.GROCERIES, 75000L))
        val updated = repository.getCategoryBudgets(targetMonth).first()
        assertEquals(1, updated.size)
        assertEquals(75000L, updated[0].limitAmount)
    }

    @Test
    fun removeCategoryBudget_deletesOnlyTargetCategory() = runTest {
        val targetMonth = YearMonth(2026, 8)
        repository.setCategoryBudget(CategoryBudget(targetMonth, TransactionCategory.GROCERIES, 50000L))
        repository.setCategoryBudget(CategoryBudget(targetMonth, TransactionCategory.DINING, 30000L))

        repository.removeCategoryBudget(targetMonth, TransactionCategory.DINING)

        val retrieved = repository.getCategoryBudgets(targetMonth).first()
        assertEquals(1, retrieved.size)
        assertEquals(TransactionCategory.GROCERIES, retrieved[0].category)
    }

    @Test
    fun deleteCategoryBudgetsForMonth_clearsAllCategoriesForTargetMonth() = runTest {
        val aug = YearMonth(2026, 8)
        val sep = YearMonth(2026, 9)

        repository.setCategoryBudget(CategoryBudget(aug, TransactionCategory.GROCERIES, 50000L))
        repository.setCategoryBudget(CategoryBudget(aug, TransactionCategory.DINING, 30000L))
        repository.setCategoryBudget(CategoryBudget(sep, TransactionCategory.TRANSPORT, 20000L))

        repository.deleteCategoryBudgetsForMonth(aug)

        val augRetrieved = repository.getCategoryBudgets(aug).first()
        assertTrue(augRetrieved.isEmpty())

        val sepRetrieved = repository.getCategoryBudgets(sep).first()
        assertEquals(1, sepRetrieved.size)
        assertEquals(TransactionCategory.TRANSPORT, sepRetrieved[0].category)
    }

    @Test
    fun persistenceAcrossReopen_preservesSavedCategoryBudgets() = runTest {
        val tempFile = File.createTempFile("category_budget_test", ".db")
        tempFile.deleteOnExit()

        val targetMonth = YearMonth(2026, 8)
        val budget = CategoryBudget(targetMonth, TransactionCategory.ENTERTAINMENT, 15000L)

        // Session 1: save and close
        val db1 = Room.databaseBuilder<AppDatabase>(tempFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        val repo1 = RoomCategoryBudgetRepository(db1.categoryBudgetDao())
        repo1.setCategoryBudget(budget)
        db1.close()

        // Session 2: reopen (simulating restart) and query
        val db2 = Room.databaseBuilder<AppDatabase>(tempFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        val repo2 = RoomCategoryBudgetRepository(db2.categoryBudgetDao())
        val retrieved = repo2.getCategoryBudgets(targetMonth).first()
        db2.close()

        assertEquals(1, retrieved.size)
        assertEquals(budget, retrieved[0])
        tempFile.delete()
    }
}
