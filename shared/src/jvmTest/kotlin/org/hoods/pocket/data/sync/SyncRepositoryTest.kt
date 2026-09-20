package org.hoods.pocket.data.sync

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.data.local.database.AppDatabase
import org.hoods.pocket.data.local.entity.CategoryBudgetEntity
import org.hoods.pocket.data.local.entity.MonthlyBudgetEntity
import org.hoods.pocket.data.repository.RoomBudgetRepository
import org.hoods.pocket.data.repository.RoomCategoryBudgetRepository
import org.hoods.pocket.data.repository.RoomDataOwnershipRepository
import org.hoods.pocket.data.repository.RoomTransactionRepository
import org.hoods.pocket.domain.model.CategoryBudget
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.model.YearMonth
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class SyncRepositoryTest {

    private lateinit var tempFile: File
    private lateinit var database: AppDatabase
    private val deviceIdProvider = FixedDeviceIdProvider("test-device-uuid")
    private var currentUserId: String? = null

    private lateinit var transactionRepository: RoomTransactionRepository
    private lateinit var budgetRepository: RoomBudgetRepository
    private lateinit var categoryBudgetRepository: RoomCategoryBudgetRepository
    private lateinit var dataOwnershipRepository: RoomDataOwnershipRepository

    @BeforeTest
    fun setup() {
        tempFile = File.createTempFile("sync_repo_test", ".db")
        database = Room.databaseBuilder<AppDatabase>(name = tempFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()

        transactionRepository = RoomTransactionRepository(
            dao = database.transactionDao(),
            syncOutboxDao = database.syncOutboxDao(),
            deviceIdProvider = deviceIdProvider,
            activeUserId = { currentUserId }
        )

        budgetRepository = RoomBudgetRepository(
            dao = database.budgetDao(),
            syncOutboxDao = database.syncOutboxDao(),
            deviceIdProvider = deviceIdProvider,
            activeUserId = { currentUserId }
        )

        categoryBudgetRepository = RoomCategoryBudgetRepository(
            dao = database.categoryBudgetDao(),
            syncOutboxDao = database.syncOutboxDao(),
            deviceIdProvider = deviceIdProvider,
            activeUserId = { currentUserId }
        )

        dataOwnershipRepository = RoomDataOwnershipRepository(
            dao = database.dataOwnershipDao(),
            syncOutboxDao = database.syncOutboxDao(),
            deviceIdProvider = deviceIdProvider
        )
    }

    @AfterTest
    fun tearDown() {
        database.close()
        tempFile.delete()
    }

    @Test
    fun unclaimedTransaction_createAndDelete_doesNotTouchOutboxAndHardDeletes() = runTest {
        currentUserId = null
        val tx = Transaction(
            id = "tx-unclaimed-1",
            description = "Cash Coffee",
            amount = 450L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(1000L),
            category = TransactionCategory.DINING
        )

        // 1. Create unclaimed transaction
        transactionRepository.insertTransaction(tx)

        val transactions = transactionRepository.getTransactions().first()
        assertEquals(1, transactions.size)
        assertEquals("tx-unclaimed-1", transactions.first().id)

        // Outbox must have 0 entries
        val outboxDao = database.syncOutboxDao()
        assertEquals(0, outboxDao.getPendingMutationCount("any-user"))

        // 2. Delete unclaimed transaction -> should be hard-deleted
        transactionRepository.deleteTransaction("tx-unclaimed-1")
        val afterDelete = transactionRepository.getTransactions().first()
        assertTrue(afterDelete.isEmpty())

        // Verify row is physically gone from DB
        val rawTx = database.transactionDao().getTransactionByIdIncludingDeleted("tx-unclaimed-1", null)
        assertNull(rawTx)
    }

    @Test
    fun claimedTransaction_create_writesEntityAndPendingOutboxMutationAtomically() = runTest {
        currentUserId = "user-123"
        val tx = Transaction(
            id = "tx-claimed-1",
            description = "Groceries Market",
            amount = 5500L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(2000L),
            category = TransactionCategory.GROCERIES
        )

        transactionRepository.insertTransaction(tx)

        // Verify entity in Room
        val list = transactionRepository.getTransactions().first()
        assertEquals(1, list.size)
        assertEquals("tx-claimed-1", list.first().id)

        // Verify outbox mutation
        val outboxDao = database.syncOutboxDao()
        val pending = outboxDao.getPendingMutations("user-123")
        assertEquals(1, pending.size)
        val mutation = pending.first()
        assertEquals("user-123", mutation.userId)
        assertEquals("test-device-uuid", mutation.deviceId)
        assertEquals("TRANSACTION", mutation.entityType)
        assertEquals("tx-claimed-1", mutation.entityId)
        assertEquals("UPSERT", mutation.operation)
        assertEquals("PENDING", mutation.status)

        // Verify payload JSON
        val payload = SyncJson.deserializeTransaction(mutation.payloadJson)
        assertEquals("tx-claimed-1", payload.id)
        assertEquals(5500L, payload.amount)
        assertEquals("user-123", payload.userId)
    }

    @Test
    fun claimedTransaction_update_coalescesExistingPendingMutation() = runTest {
        currentUserId = "user-123"
        val tx1 = Transaction(
            id = "tx-claimed-coalesce",
            description = "Initial Groceries",
            amount = 5000L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(3000L),
            category = TransactionCategory.GROCERIES
        )
        transactionRepository.insertTransaction(tx1)

        val outboxDao = database.syncOutboxDao()
        val initialPending = outboxDao.getPendingMutations("user-123")
        assertEquals(1, initialPending.size)

        // Update transaction locally while offline
        val tx2 = tx1.copy(description = "Updated Groceries Supermarket", amount = 6500L)
        transactionRepository.updateTransaction(tx2)

        // Coalescing: still exactly 1 pending mutation in outbox
        val afterUpdatePending = outboxDao.getPendingMutations("user-123")
        assertEquals(1, afterUpdatePending.size)
        val coalescedMutation = afterUpdatePending.first()
        assertEquals("UPSERT", coalescedMutation.operation)

        val payload = SyncJson.deserializeTransaction(coalescedMutation.payloadJson)
        assertEquals("Updated Groceries Supermarket", payload.description)
        assertEquals(6500L, payload.amount)
    }

    @Test
    fun claimedTransaction_delete_createsSoftDeleteTombstoneAndQueuesDeleteMutation() = runTest {
        currentUserId = "user-123"
        val tx = Transaction(
            id = "tx-to-delete",
            description = "Online Shopping",
            amount = 3200L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(4000L),
            category = TransactionCategory.SHOPPING
        )
        transactionRepository.insertTransaction(tx)

        // Delete claimed transaction
        transactionRepository.deleteTransaction("tx-to-delete")

        // Active user query must NOT return soft-deleted transaction
        val activeTx = transactionRepository.getTransactions().first()
        assertTrue(activeTx.isEmpty())

        // Physical row in Room still exists as a tombstone
        val rawTx = database.transactionDao().getTransactionByIdIncludingDeleted("tx-to-delete", "user-123")
        assertNotNull(rawTx)
        assertNotNull(rawTx.deletedAtEpochMillis)

        // Outbox contains DELETE mutation
        val outboxDao = database.syncOutboxDao()
        val pending = outboxDao.getPendingMutations("user-123")
        assertEquals(1, pending.size)
        val deleteMutation = pending.first()
        assertEquals("DELETE", deleteMutation.operation)
        assertEquals("tx-to-delete", deleteMutation.entityId)
    }

    @Test
    fun claimedMonthlyBudget_createUpdateDelete_managesOutboxAndTombstone() = runTest {
        currentUserId = "user-123"
        val budget = MonthlyBudget(
            yearMonth = YearMonth(2026, 9),
            limitAmount = 250000L
        )

        // 1. Create
        budgetRepository.setMonthlyBudget(budget)
        val loaded = budgetRepository.getMonthlyBudget(YearMonth(2026, 9)).first()
        assertNotNull(loaded)
        assertEquals(250000L, loaded.limitAmount)

        val outboxDao = database.syncOutboxDao()
        var pending = outboxDao.getPendingMutations("user-123")
        assertEquals(1, pending.size)
        assertEquals("MONTHLY_BUDGET", pending.first().entityType)
        assertEquals("2026-9", pending.first().entityId)
        assertEquals("UPSERT", pending.first().operation)

        // 2. Delete
        budgetRepository.deleteMonthlyBudget(YearMonth(2026, 9))
        val activeAfterDelete = budgetRepository.getMonthlyBudget(YearMonth(2026, 9)).first()
        assertNull(activeAfterDelete)

        // Raw tombstone in Room
        val rawBudget = database.budgetDao().getMonthlyBudgetIncludingDeleted(2026, 9, "user-123")
        assertNotNull(rawBudget)
        assertNotNull(rawBudget.deletedAtEpochMillis)

        // Outbox mutation coalesced to DELETE
        pending = outboxDao.getPendingMutations("user-123")
        assertEquals(1, pending.size)
        assertEquals("DELETE", pending.first().operation)
    }

    @Test
    fun claimedCategoryBudget_createUpdateDelete_managesOutboxAndTombstone() = runTest {
        currentUserId = "user-123"
        val catBudget = CategoryBudget(
            yearMonth = YearMonth(2026, 9),
            category = TransactionCategory.GROCERIES,
            limitAmount = 80000L
        )

        // 1. Set Category Budget
        categoryBudgetRepository.setCategoryBudget(catBudget)
        val loaded = categoryBudgetRepository.getCategoryBudgets(YearMonth(2026, 9)).first()
        assertEquals(1, loaded.size)
        assertEquals(80000L, loaded.first().limitAmount)

        val outboxDao = database.syncOutboxDao()
        var pending = outboxDao.getPendingMutations("user-123")
        assertEquals(1, pending.size)
        assertEquals("CATEGORY_BUDGET", pending.first().entityType)
        assertEquals("2026-9-GROCERIES", pending.first().entityId)
        assertEquals("UPSERT", pending.first().operation)

        // 2. Remove Category Budget
        categoryBudgetRepository.removeCategoryBudget(YearMonth(2026, 9), TransactionCategory.GROCERIES)
        val activeAfterDelete = categoryBudgetRepository.getCategoryBudgets(YearMonth(2026, 9)).first()
        assertTrue(activeAfterDelete.isEmpty())

        val rawCat = database.categoryBudgetDao().getCategoryBudgetIncludingDeleted(2026, 9, "GROCERIES", "user-123")
        assertNotNull(rawCat)
        assertNotNull(rawCat.deletedAtEpochMillis)

        pending = outboxDao.getPendingMutations("user-123")
        assertEquals(1, pending.size)
        assertEquals("DELETE", pending.first().operation)
    }

    @Test
    fun claimingLocalData_claimsEntitiesAndEnqueuesUpsertMutationsAtomically() = runTest {
        // Seed unclaimed entities
        currentUserId = null
        val txDao = database.transactionDao()
        val budgetDao = database.budgetDao()
        val catDao = database.categoryBudgetDao()

        txDao.insertTransaction(
            org.hoods.pocket.data.local.entity.TransactionEntity(
                id = "tx-seed-1",
                description = "Offline dinner",
                amount = 3500L,
                type = "EXPENSE",
                dateEpochMillis = 5000L,
                category = "DINING",
                userId = null
            )
        )
        budgetDao.insertOrUpdateMonthlyBudget(
            MonthlyBudgetEntity(
                year = 2026,
                month = 9,
                limitAmount = 150000L,
                userId = null
            )
        )
        catDao.insertOrUpdateCategoryBudget(
            CategoryBudgetEntity(
                year = 2026,
                month = 9,
                category = "DINING",
                limitAmount = 30000L,
                userId = null
            )
        )

        // Outbox is empty before claim
        val outboxDao = database.syncOutboxDao()
        assertEquals(0, outboxDao.getPendingMutationCount("user-signed-in"))

        // Perform atomic claiming
        val result = dataOwnershipRepository.claimLocalDataAtomically("user-signed-in")
        assertTrue(result.isSuccess)

        // All 3 entities should now belong to "user-signed-in"
        currentUserId = "user-signed-in"
        val claimedTx = transactionRepository.getTransactions().first()
        assertEquals(1, claimedTx.size)
        val claimedBudget = budgetRepository.getMonthlyBudget(YearMonth(2026, 9)).first()
        assertNotNull(claimedBudget)
        val claimedCats = categoryBudgetRepository.getCategoryBudgets(YearMonth(2026, 9)).first()
        assertEquals(1, claimedCats.size)

        // Outbox must contain exactly 3 UPSERT mutations for "user-signed-in"
        val pendingMutations = outboxDao.getPendingMutations("user-signed-in")
        assertEquals(3, pendingMutations.size)
        assertTrue(pendingMutations.all { it.operation == "UPSERT" && it.status == "PENDING" && it.userId == "user-signed-in" })
    }
}
