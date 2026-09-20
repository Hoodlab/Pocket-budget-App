package org.hoods.pocket.data.sync

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.data.local.database.AppDatabase
import org.hoods.pocket.data.local.entity.SyncOutboxEntity
import org.hoods.pocket.domain.model.YearMonth
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeRemoteSyncDataSource : RemoteSyncDataSource {
    var pushHandler: (suspend (List<SyncMutationDto>) -> List<SyncMutationResultDto>)? = null
    var pullHandler: (suspend (Long) -> PullSyncChangesResponseDto)? = null

    val pushedMutations = mutableListOf<SyncMutationDto>()
    val pullCursorsRequested = mutableListOf<Long>()

    override suspend fun applySyncMutations(mutations: List<SyncMutationDto>): List<SyncMutationResultDto> {
        pushedMutations.addAll(mutations)
        return pushHandler?.invoke(mutations) ?: mutations.map {
            SyncMutationResultDto(mutationId = it.mutationId, status = "APPLIED")
        }
    }

    override suspend fun pullSyncChanges(sinceEpochMillis: Long): PullSyncChangesResponseDto {
        pullCursorsRequested.add(sinceEpochMillis)
        return pullHandler?.invoke(sinceEpochMillis) ?: PullSyncChangesResponseDto(
            transactions = emptyList(),
            monthlyBudgets = emptyList(),
            categoryBudgets = emptyList(),
            serverEpochMillis = sinceEpochMillis + 1000
        )
    }
}

class PocketSyncEngineTest {

    private lateinit var tempFile: File
    private lateinit var database: AppDatabase
    private lateinit var fakeRemote: FakeRemoteSyncDataSource
    private lateinit var syncEngine: PocketSyncEngine

    private val testUserId = "user-test-123"

    @BeforeTest
    fun setup() {
        tempFile = File.createTempFile("sync_engine_test", ".db")
        database = Room.databaseBuilder<AppDatabase>(name = tempFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()

        fakeRemote = FakeRemoteSyncDataSource()
        syncEngine = PocketSyncEngine(
            syncOutboxDao = database.syncOutboxDao(),
            syncMetadataDao = database.syncMetadataDao(),
            transactionDao = database.transactionDao(),
            budgetDao = database.budgetDao(),
            categoryBudgetDao = database.categoryBudgetDao(),
            remoteDataSource = fakeRemote
        )
    }

    @AfterTest
    fun tearDown() {
        database.close()
        tempFile.delete()
    }

    @Test
    fun unavailable_whenRemoteDataSourceIsNull() = runTest {
        val engine = PocketSyncEngine(
            syncOutboxDao = database.syncOutboxDao(),
            syncMetadataDao = database.syncMetadataDao(),
            transactionDao = database.transactionDao(),
            budgetDao = database.budgetDao(),
            categoryBudgetDao = database.categoryBudgetDao(),
            remoteDataSource = null
        )

        val result = engine.synchronize(testUserId)
        assertEquals(SyncResult.Unavailable, result)
    }

    @Test
    fun push_success_removesMutationsFromOutbox_andIncrementsPushedCount() = runTest {
        val outboxDao = database.syncOutboxDao()
        outboxDao.insertOrUpdateMutation(
            SyncOutboxEntity(
                mutationId = "mut-tx-1",
                userId = testUserId,
                deviceId = "device-1",
                entityType = "TRANSACTION",
                entityId = "tx-1",
                operation = "UPSERT",
                payloadJson = """{"id":"tx-1","description":"Groceries","amount":5000,"type":"EXPENSE","category":"GROCERIES","dateEpochMillis":1000,"userId":"$testUserId"}""",
                updatedAtEpochMillis = 1000,
                status = "PENDING",
                retryCount = 0,
                lastError = null,
                createdAtEpochMillis = 1000
            )
        )

        fakeRemote.pushHandler = { mutations ->
            mutations.map { SyncMutationResultDto(mutationId = it.mutationId, status = "APPLIED") }
        }

        val result = syncEngine.synchronize(testUserId)
        assertIs<SyncResult.Success>(result)
        assertEquals(1, result.pushedCount)

        // Mutation must be deleted from outbox
        val pendingAfter = outboxDao.getPendingMutations(testUserId)
        assertTrue(pendingAfter.isEmpty())
    }

    @Test
    fun push_stale_removesMutationFromOutbox() = runTest {
        val outboxDao = database.syncOutboxDao()
        outboxDao.insertOrUpdateMutation(
            SyncOutboxEntity(
                mutationId = "mut-stale",
                userId = testUserId,
                deviceId = "device-1",
                entityType = "TRANSACTION",
                entityId = "tx-1",
                operation = "UPSERT",
                payloadJson = """{"id":"tx-1","description":"Old Groceries","amount":4000,"type":"EXPENSE","category":"GROCERIES","dateEpochMillis":1000,"userId":"$testUserId"}""",
                updatedAtEpochMillis = 500,
                status = "PENDING",
                retryCount = 0,
                lastError = null,
                createdAtEpochMillis = 500
            )
        )

        fakeRemote.pushHandler = { mutations ->
            mutations.map { SyncMutationResultDto(mutationId = it.mutationId, status = "STALE") }
        }

        val result = syncEngine.synchronize(testUserId)
        assertIs<SyncResult.Success>(result)
        assertEquals(1, result.pushedCount)

        // Stale mutation is cleared from outbox so we do not retry
        val pendingAfter = outboxDao.getPendingMutations(testUserId)
        assertTrue(pendingAfter.isEmpty())
    }

    @Test
    fun push_rejected_updatesOutboxStatusToFailed() = runTest {
        val outboxDao = database.syncOutboxDao()
        outboxDao.insertOrUpdateMutation(
            SyncOutboxEntity(
                mutationId = "mut-reject",
                userId = testUserId,
                deviceId = "device-1",
                entityType = "TRANSACTION",
                entityId = "tx-invalid",
                operation = "UPSERT",
                payloadJson = """{"id":"tx-invalid","description":"","amount":0,"type":"INVALID","category":"OTHER","dateEpochMillis":1000,"userId":"$testUserId"}""",
                updatedAtEpochMillis = 1000,
                status = "PENDING",
                retryCount = 0,
                lastError = null,
                createdAtEpochMillis = 1000
            )
        )

        fakeRemote.pushHandler = { mutations ->
            mutations.map {
                SyncMutationResultDto(
                    mutationId = it.mutationId,
                    status = "REJECTED",
                    message = "Invalid payload"
                )
            }
        }

        val result = syncEngine.synchronize(testUserId)
        assertIs<SyncResult.Success>(result)
        assertEquals(0, result.pushedCount)

        val failed = outboxDao.getMutation("mut-reject")
        assertNotNull(failed)
        assertEquals("FAILED", failed.status)
        assertEquals(1, failed.retryCount)
        assertEquals("Invalid payload", failed.lastError)
    }

    @Test
    fun push_networkException_returnsFailure_retainsPendingOutbox() = runTest {
        val outboxDao = database.syncOutboxDao()
        outboxDao.insertOrUpdateMutation(
            SyncOutboxEntity(
                mutationId = "mut-net-error",
                userId = testUserId,
                deviceId = "device-1",
                entityType = "TRANSACTION",
                entityId = "tx-1",
                operation = "UPSERT",
                payloadJson = """{"id":"tx-1","description":"Groceries","amount":5000,"type":"EXPENSE","category":"GROCERIES","dateEpochMillis":1000,"userId":"$testUserId"}""",
                updatedAtEpochMillis = 1000,
                status = "PENDING",
                retryCount = 0,
                lastError = null,
                createdAtEpochMillis = 1000
            )
        )

        fakeRemote.pushHandler = {
            throw RuntimeException("Network connection lost")
        }

        val result = syncEngine.synchronize(testUserId)
        assertIs<SyncResult.Failure>(result)
        assertTrue(result.canRetry)

        // Mutation remains in outbox
        val pendingAfter = outboxDao.getPendingMutations(testUserId)
        assertEquals(1, pendingAfter.size)

        // Sync metadata records error
        val metadata = database.syncMetadataDao().getMetadata(testUserId)
        assertNotNull(metadata)
        assertEquals("ERROR", metadata.lastSyncStatus)
    }

    @Test
    fun pull_insertsActiveEntities_andAdvancesSyncCursor() = runTest {
        fakeRemote.pullHandler = { since ->
            PullSyncChangesResponseDto(
                transactions = listOf(
                    TransactionPullDto(
                        id = "tx-pulled-1",
                        userId = testUserId,
                        description = "Remote Salary",
                        amount = 400000L,
                        type = "INCOME",
                        category = "SALARY",
                        dateEpochMillis = 2000L,
                        updatedAtEpochMillis = 5000L
                    )
                ),
                monthlyBudgets = listOf(
                    MonthlyBudgetPullDto(
                        userId = testUserId,
                        year = 2026,
                        month = 9,
                        limitAmount = 300000L,
                        updatedAtEpochMillis = 5000L
                    )
                ),
                categoryBudgets = listOf(
                    CategoryBudgetPullDto(
                        userId = testUserId,
                        year = 2026,
                        month = 9,
                        category = "SALARY",
                        limitAmount = 50000L,
                        updatedAtEpochMillis = 5000L
                    )
                ),
                serverEpochMillis = 99999L
            )
        }

        val result = syncEngine.synchronize(testUserId)
        assertIs<SyncResult.Success>(result)
        assertEquals(3, result.pulledCount)

        // Verify entities inserted into Room
        val tx = database.transactionDao().getTransactionById("tx-pulled-1", testUserId)
        assertNotNull(tx)
        assertEquals("Remote Salary", tx.description)
        assertEquals(400000L, tx.amount)

        val mb = database.budgetDao().getMonthlyBudget(2026, 9, testUserId).first()
        assertNotNull(mb)
        assertEquals(300000L, mb.limitAmount)

        val cbList = database.categoryBudgetDao().getCategoryBudgets(2026, 9, testUserId).first()
        assertEquals(1, cbList.size)
        assertEquals(50000L, cbList.first().limitAmount)

        // Verify cursor advanced
        val metadata = database.syncMetadataDao().getMetadata(testUserId)
        assertNotNull(metadata)
        assertEquals("99999", metadata.lastSyncCursor)
        assertEquals("IDLE", metadata.lastSyncStatus)
    }

    @Test
    fun pull_conflict_remoteWins_overwritesLocalAndDeletesLocalOutbox() = runTest {
        val outboxDao = database.syncOutboxDao()
        outboxDao.insertOrUpdateMutation(
            SyncOutboxEntity(
                mutationId = "mut-local-1",
                userId = testUserId,
                deviceId = "device-a",
                entityType = "TRANSACTION",
                entityId = "tx-conflict-1",
                operation = "UPSERT",
                payloadJson = """{"id":"tx-conflict-1","description":"Local Old Desc","amount":3000,"type":"EXPENSE","category":"OTHER","dateEpochMillis":1000,"userId":"$testUserId"}""",
                updatedAtEpochMillis = 1000,
                status = "PENDING",
                retryCount = 0,
                lastError = null,
                createdAtEpochMillis = 1000
            )
        )

        // Simulate pending mutation not yet acknowledged by push
        fakeRemote.pushHandler = { emptyList() }

        // Remote has newer record
        fakeRemote.pullHandler = {
            PullSyncChangesResponseDto(
                transactions = listOf(
                    TransactionPullDto(
                        id = "tx-conflict-1",
                        userId = testUserId,
                        description = "Remote Newer Desc",
                        amount = 4500L,
                        type = "EXPENSE",
                        category = "OTHER",
                        dateEpochMillis = 1000L,
                        updatedAtEpochMillis = 2000L, // 2000 > 1000 => remote wins!
                        deviceId = "device-b",
                        mutationId = "mut-remote-1"
                    )
                ),
                serverEpochMillis = 5000L
            )
        }

        val result = syncEngine.synchronize(testUserId)
        assertIs<SyncResult.Success>(result)

        // Remote won: local Room has remote data
        val tx = database.transactionDao().getTransactionById("tx-conflict-1", testUserId)
        assertNotNull(tx)
        assertEquals("Remote Newer Desc", tx.description)
        assertEquals(4500L, tx.amount)

        // Local pending mutation was cleared
        val pending = outboxDao.findPendingMutation(testUserId, "TRANSACTION", "tx-conflict-1")
        assertNull(pending)
    }

    @Test
    fun pull_conflict_localWins_preservesLocalAndKeepsOutboxPending() = runTest {
        val outboxDao = database.syncOutboxDao()
        outboxDao.insertOrUpdateMutation(
            SyncOutboxEntity(
                mutationId = "mut-local-winner",
                userId = testUserId,
                deviceId = "device-a",
                entityType = "TRANSACTION",
                entityId = "tx-conflict-2",
                operation = "UPSERT",
                payloadJson = """{"id":"tx-conflict-2","description":"Local Newest Desc","amount":9000,"type":"EXPENSE","category":"OTHER","dateEpochMillis":1000,"userId":"$testUserId"}""",
                updatedAtEpochMillis = 3000, // 3000 > 2000 => local wins!
                status = "PENDING",
                retryCount = 0,
                lastError = null,
                createdAtEpochMillis = 3000
            )
        )

        // Seed current local entity
        database.transactionDao().insertTransaction(
            org.hoods.pocket.data.local.entity.TransactionEntity(
                id = "tx-conflict-2",
                description = "Local Newest Desc",
                amount = 9000L,
                type = "EXPENSE",
                category = "OTHER",
                dateEpochMillis = 1000L,
                userId = testUserId,
                updatedAtEpochMillis = 3000L
            )
        )

        // Simulate pending mutation not yet acknowledged by push
        fakeRemote.pushHandler = { emptyList() }

        // Remote returns older record
        fakeRemote.pullHandler = {
            PullSyncChangesResponseDto(
                transactions = listOf(
                    TransactionPullDto(
                        id = "tx-conflict-2",
                        userId = testUserId,
                        description = "Remote Older Desc",
                        amount = 4000L,
                        type = "EXPENSE",
                        category = "OTHER",
                        dateEpochMillis = 1000L,
                        updatedAtEpochMillis = 2000L,
                        deviceId = "device-b",
                        mutationId = "mut-remote-older"
                    )
                ),
                serverEpochMillis = 5000L
            )
        }

        val result = syncEngine.synchronize(testUserId)
        assertIs<SyncResult.Success>(result)

        // Local won: Room still has local description
        val tx = database.transactionDao().getTransactionById("tx-conflict-2", testUserId)
        assertNotNull(tx)
        assertEquals("Local Newest Desc", tx.description)

        // Pending mutation STILL in outbox for subsequent push
        val pending = outboxDao.findPendingMutation(testUserId, "TRANSACTION", "tx-conflict-2")
        assertNotNull(pending)
    }

    @Test
    fun pull_softDeleteTombstone_marksLocalEntityDeleted() = runTest {
        // Seed active transaction locally
        database.transactionDao().insertTransaction(
            org.hoods.pocket.data.local.entity.TransactionEntity(
                id = "tx-to-be-deleted-remotely",
                description = "To be deleted",
                amount = 2500L,
                type = "EXPENSE",
                category = "OTHER",
                dateEpochMillis = 1000L,
                userId = testUserId,
                updatedAtEpochMillis = 1000L
            )
        )

        // Remote sends tombstone
        fakeRemote.pullHandler = {
            PullSyncChangesResponseDto(
                transactions = listOf(
                    TransactionPullDto(
                        id = "tx-to-be-deleted-remotely",
                        userId = testUserId,
                        description = "To be deleted",
                        amount = 2500L,
                        type = "EXPENSE",
                        category = "OTHER",
                        dateEpochMillis = 1000L,
                        updatedAtEpochMillis = 2000L,
                        deletedAtEpochMillis = 2000L,
                        deviceId = "device-remote",
                        mutationId = "mut-del-remote"
                    )
                ),
                serverEpochMillis = 5000L
            )
        }

        val result = syncEngine.synchronize(testUserId)
        assertIs<SyncResult.Success>(result)

        // Active query should no longer return it
        val activeTx = database.transactionDao().getTransactionById("tx-to-be-deleted-remotely", testUserId)
        assertNull(activeTx)

        // Tombstone query should find it with deletedAtEpochMillis set
        val rawTx = database.transactionDao().getTransactionByIdIncludingDeleted("tx-to-be-deleted-remotely", testUserId)
        assertNotNull(rawTx)
        assertNotNull(rawTx.deletedAtEpochMillis)
    }
}
