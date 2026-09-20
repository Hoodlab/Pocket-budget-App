package org.hoods.pocket.data.sync

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.hoods.pocket.data.local.database.AppDatabase
import org.hoods.pocket.data.local.database.MIGRATION_2_3
import org.hoods.pocket.data.local.database.MIGRATION_3_4
import org.hoods.pocket.data.local.database.MIGRATION_4_5
import org.hoods.pocket.data.local.database.MIGRATION_5_6
import org.hoods.pocket.data.local.entity.TransactionEntity
import org.hoods.pocket.data.remote.SupabaseConfig
import org.hoods.pocket.data.repository.RoomDataOwnershipRepository
import org.hoods.pocket.data.repository.RoomTransactionRepository
import org.hoods.pocket.data.repository.SupabaseAuthRepository
import org.hoods.pocket.domain.model.AppSyncState
import org.hoods.pocket.domain.model.AuthState
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.repository.AuthResult
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
private data class RemoteTransactionRecord(
    val id: String,
    val user_id: String,
    val description: String,
    val amount: Long,
    val type: String,
    val category: String,
    val updated_at_epoch_millis: Long,
    val deleted_at: String? = null
)

@OptIn(ExperimentalUuidApi::class)
class LocalSupabaseRuntimeSyncIntegrationTest {

    private lateinit var tempFile: File
    private lateinit var database: AppDatabase
    private lateinit var authRepository: SupabaseAuthRepository
    private lateinit var syncEngine: PocketSyncEngine
    private lateinit var syncCoordinator: PocketSyncCoordinator
    private lateinit var transactionRepository: RoomTransactionRepository
    private lateinit var dataOwnershipRepository: RoomDataOwnershipRepository

    private val deviceIdProvider = FixedDeviceIdProvider("test-integration-device")
    private val config = SupabaseConfig.local()

    @BeforeTest
    fun setup() = kotlinx.coroutines.runBlocking {
        tempFile = File.createTempFile("supabase_runtime_sync_test", ".db")
        database = Room.databaseBuilder<AppDatabase>(name = tempFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()

        val client = io.github.jan.supabase.createSupabaseClient(
            supabaseUrl = config.url.orEmpty(),
            supabaseKey = config.publishableKey.orEmpty()
        ) {
            install(io.github.jan.supabase.auth.Auth) {
                autoLoadFromStorage = false
            }
            install(io.github.jan.supabase.postgrest.Postgrest)
        }

        authRepository = SupabaseAuthRepository(config = config, clientOverride = client)

        val remoteDataSource = SupabaseRemoteSyncDataSource(client)
        syncEngine = PocketSyncEngine(
            syncOutboxDao = database.syncOutboxDao(),
            syncMetadataDao = database.syncMetadataDao(),
            transactionDao = database.transactionDao(),
            budgetDao = database.budgetDao(),
            categoryBudgetDao = database.categoryBudgetDao(),
            remoteDataSource = remoteDataSource
        )

        syncCoordinator = PocketSyncCoordinator(
            syncEngine = syncEngine,
            authRepository = authRepository,
            syncOutboxDao = database.syncOutboxDao(),
            config = config
        )

        transactionRepository = RoomTransactionRepository(
            dao = database.transactionDao(),
            authRepository = authRepository,
            syncOutboxDao = database.syncOutboxDao(),
            deviceIdProvider = deviceIdProvider,
            syncCoordinator = syncCoordinator
        )

        dataOwnershipRepository = RoomDataOwnershipRepository(
            dao = database.dataOwnershipDao(),
            syncOutboxDao = database.syncOutboxDao(),
            deviceIdProvider = deviceIdProvider,
            syncCoordinator = syncCoordinator
        )
    }

    @AfterTest
    fun tearDown() {
        database.close()
        tempFile.delete()
    }

    @Test
    fun runtimePath_unclaimedToClaimed_syncsToLocalSupabase_andHandlesUpdateAndDelete() = runTest {
        val outboxDao = database.syncOutboxDao()
        val txDao = database.transactionDao()
        val uniqueTxId = Uuid.random().toString()
        val now = Clock.System.now().toEpochMilliseconds()

        // 1. Initially Unauthenticated: Seed unclaimed transaction in Room
        txDao.insertTransaction(
            TransactionEntity(
                id = uniqueTxId,
                description = "Organic Apples",
                amount = 1250L,
                type = "EXPENSE",
                dateEpochMillis = now,
                category = "GROCERIES",
                userId = null,
                updatedAtEpochMillis = now,
                deletedAtEpochMillis = null
            )
        )

        // Confirm: Unclaimed data does NOT generate outbox entries
        assertEquals(0, outboxDao.getPendingMutationCount("any-user"))
        assertEquals(AppSyncState.Idle, syncCoordinator.syncState.value)

        // 2. Sign up a new unique authenticated user against local Supabase
        val uniqueEmail = "sync_integration_${Uuid.random()}@pocketbudget.test"
        val signUpResult = authRepository.signUp(uniqueEmail, "TestPassword123!")
        assertTrue(signUpResult is AuthResult.Success, "Expected successful sign-up in local Supabase")
        val authUser = signUpResult.user
        val userId = authUser.id

        // Wait for authState to emit Authenticated
        val currentAuthState = authRepository.authState.first { it is AuthState.Authenticated }
        assertEquals(userId, (currentAuthState as AuthState.Authenticated).user.id)

        // Wait for coordinator's initial sync attempt to complete
        syncCoordinator.syncState.first { it is AppSyncState.Success }

        // 3. Confirm remote table has 0 transactions for this new user before claiming
        val client = authRepository.client!!
        val remoteBeforeClaim = client.postgrest["transactions"]
            .select {
                filter {
                    eq("id", uniqueTxId)
                }
            }
            .decodeList<RemoteTransactionRecord>()
        assertEquals(0, remoteBeforeClaim.size, "Remote table must not contain transaction before claiming")

        // 4. Claim local unassigned data for authenticated user
        val claimResult = dataOwnershipRepository.claimLocalDataAtomically(userId)
        assertTrue(claimResult.isSuccess)

        // Wait for coordinator to execute and complete sync
        syncCoordinator.requestSync().join()
        val syncSuccessState = syncCoordinator.syncState.value
        assertTrue(syncSuccessState is AppSyncState.Success && syncSuccessState.pushedCount >= 1)

        // Verify outbox in Room is cleared
        assertEquals(0, outboxDao.getPendingMutationCount(userId))

        // 5. PROVE RUNTIME PATH: Transaction record now appears in local Supabase Postgres!
        val remoteAfterClaim = client.postgrest["transactions"]
            .select {
                filter {
                    eq("id", uniqueTxId)
                }
            }
            .decodeList<RemoteTransactionRecord>()

        assertEquals(1, remoteAfterClaim.size, "Record must appear in local Supabase Postgres")
        val remoteTx = remoteAfterClaim.first()
        assertEquals(uniqueTxId, remoteTx.id)
        assertEquals(userId, remoteTx.user_id)
        assertEquals("Organic Apples", remoteTx.description)
        assertEquals(1250L, remoteTx.amount)
        assertEquals("EXPENSE", remoteTx.type)
        assertEquals("GROCERIES", remoteTx.category)
        assertNull(remoteTx.deleted_at)

        // 6. UPDATE: Update transaction locally -> outbox mutation -> sync -> remote update
        val updatedTx = org.hoods.pocket.domain.model.Transaction(
            id = uniqueTxId,
            description = "Organic Honeycrisp Apples",
            amount = 1450L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(now),
            category = TransactionCategory.GROCERIES
        )
        transactionRepository.updateTransaction(updatedTx)

        // Wait for sync to push update
        syncCoordinator.requestSync().join()
        assertEquals(0, outboxDao.getPendingMutationCount(userId))
        assertTrue(syncCoordinator.syncState.value is AppSyncState.Success)

        // Verify remote record is updated
        val remoteAfterUpdate = client.postgrest["transactions"]
            .select {
                filter {
                    eq("id", uniqueTxId)
                }
            }
            .decodeList<RemoteTransactionRecord>()

        assertEquals(1, remoteAfterUpdate.size)
        assertEquals("Organic Honeycrisp Apples", remoteAfterUpdate.first().description)
        assertEquals(1450L, remoteAfterUpdate.first().amount)

        // 7. DELETE: Delete transaction locally -> outbox tombstone -> sync -> remote soft-delete
        transactionRepository.deleteTransaction(uniqueTxId)

        // Wait for sync to push delete
        syncCoordinator.requestSync().join()
        assertEquals(0, outboxDao.getPendingMutationCount(userId))
        assertTrue(syncCoordinator.syncState.value is AppSyncState.Success)

        // Verify remote record is soft-deleted
        val remoteAfterDelete = client.postgrest["transactions"]
            .select {
                filter {
                    eq("id", uniqueTxId)
                }
            }
            .decodeList<RemoteTransactionRecord>()

        assertEquals(1, remoteAfterDelete.size)
        assertNotNull(remoteAfterDelete.first().deleted_at, "Remote record must be soft-deleted with deleted_at timestamp")

        // 8. SIGN OUT: Verify no sync runs for signed-out state
        authRepository.signOut()
        val signedOutState = syncCoordinator.syncState.first { it is AppSyncState.Idle }
        assertEquals(AppSyncState.Idle, signedOutState)

        // Create unclaimed transaction while signed out
        transactionRepository.insertTransaction(
            org.hoods.pocket.domain.model.Transaction(
                id = Uuid.random().toString(),
                description = "Cash tip",
                amount = 200L,
                type = TransactionType.EXPENSE,
                date = Instant.fromEpochMilliseconds(now),
                category = TransactionCategory.OTHER
            )
        )

        // Confirm 0 outbox entries and sync remains Idle
        assertEquals(0, outboxDao.getPendingMutationCount(userId))
        assertEquals(AppSyncState.Idle, syncCoordinator.syncState.value)
    }
}
