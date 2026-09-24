package org.hoods.pocket.data.sync

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import org.hoods.pocket.data.local.database.AppDatabase
import org.hoods.pocket.data.local.entity.SyncOutboxEntity
import org.hoods.pocket.data.remote.SupabaseConfig
import org.hoods.pocket.data.remote.SupabaseEnvironment
import org.hoods.pocket.domain.model.AppSyncState
import org.hoods.pocket.domain.model.AuthState
import org.hoods.pocket.domain.model.AuthUser
import org.hoods.pocket.domain.repository.AuthRepository
import org.hoods.pocket.domain.repository.AuthResult
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FakeAuthRepository : AuthRepository {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun emitAuthenticated(userId: String, email: String = "test@pocketbudget.test") {
        _authState.value = AuthState.Authenticated(AuthUser(id = userId, email = email))
    }

    fun emitUnauthenticated() {
        _authState.value = AuthState.Unauthenticated
    }

    override suspend fun signUp(email: String, password: String): AuthResult = AuthResult.Error("stub")
    override suspend fun signIn(email: String, password: String): AuthResult = AuthResult.Error("stub")
    override suspend fun signOut(): Result<Unit> {
        emitUnauthenticated()
        return Result.success(Unit)
    }
    override fun currentUser(): AuthUser? = (authState.value as? AuthState.Authenticated)?.user
}

class FakeSyncEngine(
    private val onSyncSuccess: (suspend (String) -> Unit)? = null
) : SyncEngine {
    var syncHandler: (suspend (String) -> SyncResult)? = null
    val syncInvocations = mutableListOf<String>()
    var concurrentSyncCount = 0
    var maxConcurrentSyncs = 0

    override suspend fun synchronize(userId: String): SyncResult {
        concurrentSyncCount++
        if (concurrentSyncCount > maxConcurrentSyncs) {
            maxConcurrentSyncs = concurrentSyncCount
        }
        syncInvocations.add(userId)
        val result = try {
            val res = syncHandler?.invoke(userId) ?: SyncResult.Success(pushedCount = 1, pulledCount = 0)
            if (res is SyncResult.Success) {
                onSyncSuccess?.invoke(userId)
            }
            res
        } finally {
            concurrentSyncCount--
        }
        return result
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PocketSyncCoordinatorTest {

    private lateinit var tempFile: File
    private lateinit var database: AppDatabase
    private lateinit var fakeAuthRepo: FakeAuthRepository
    private lateinit var fakeEngine: FakeSyncEngine
    private lateinit var coordinator: PocketSyncCoordinator

    private val configuredSupabase = SupabaseConfig(
        environment = SupabaseEnvironment.LOCAL,
        url = "http://127.0.0.1:54321",
        publishableKey = "test-key"
    )

    @BeforeTest
    fun setup() {
        tempFile = File.createTempFile("sync_coord_test", ".db")
        database = Room.databaseBuilder<AppDatabase>(name = tempFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()

        fakeAuthRepo = FakeAuthRepository()
        fakeEngine = FakeSyncEngine(
            onSyncSuccess = { userId ->
                database.syncOutboxDao().deleteAllForUser(userId)
            }
        )
    }

    @AfterTest
    fun tearDown() {
        database.close()
        tempFile.delete()
    }

    private fun createCoordinator(
        scope: CoroutineScope,
        config: SupabaseConfig = configuredSupabase,
        connectivityObserver: org.hoods.pocket.domain.service.ConnectivityObserver? = null
    ): PocketSyncCoordinator {
        return PocketSyncCoordinator(
            syncEngine = fakeEngine,
            authRepository = fakeAuthRepo,
            syncOutboxDao = database.syncOutboxDao(),
            config = config,
            connectivityObserver = connectivityObserver,
            coroutineScope = scope
        )
    }

    @Test
    fun signedOutUser_doesNotRunSync() = runTest {
        coordinator = createCoordinator(backgroundScope)
        assertEquals(AppSyncState.Idle, coordinator.syncState.value)

        coordinator.requestSync().join()

        assertEquals(0, fakeEngine.syncInvocations.size)
        assertEquals(AppSyncState.Idle, coordinator.syncState.value)
    }

    @Test
    fun unconfiguredSupabase_doesNotRunSync() = runTest {
        coordinator = createCoordinator(backgroundScope, config = SupabaseConfig.unconfigured)
        fakeAuthRepo.emitAuthenticated("user-1")

        coordinator.requestSync().join()

        assertEquals(0, fakeEngine.syncInvocations.size)
        assertEquals(AppSyncState.Idle, coordinator.syncState.value)
    }

    @Test
    fun emptyOutbox_doesNotRunSyncOnRequest() = runTest {
        coordinator = createCoordinator(backgroundScope)
        fakeAuthRepo.emitAuthenticated("user-1")
        coordinator.syncState.first { it is AppSyncState.Success }
        fakeEngine.syncInvocations.clear()

        // Outbox is empty, calling requestSync must NOT trigger sync
        coordinator.requestSync().join()

        assertEquals(0, fakeEngine.syncInvocations.size)
    }

    @Test
    fun outboxMutation_triggersSync_andTransitionsToSuccess() = runTest {
        coordinator = createCoordinator(backgroundScope)
        fakeAuthRepo.emitAuthenticated("user-1")
        coordinator.syncState.first { it is AppSyncState.Success }
        fakeEngine.syncInvocations.clear()

        // Insert pending outbox mutation
        val outboxDao = database.syncOutboxDao()
        outboxDao.insertOrUpdateMutation(
            SyncOutboxEntity(
                mutationId = "mut-1",
                userId = "user-1",
                deviceId = "device-1",
                entityType = "TRANSACTION",
                entityId = "tx-1",
                operation = "UPSERT",
                payloadJson = "{}",
                updatedAtEpochMillis = 1000L,
                status = "PENDING",
                retryCount = 0,
                lastError = null,
                createdAtEpochMillis = 1000L
            )
        )

        coordinator.requestSync().join()

        assertTrue(fakeEngine.syncInvocations.contains("user-1"))
        val state = coordinator.syncState.value
        assertIs<AppSyncState.Success>(state)
        assertEquals(1, state.pushedCount)
    }

    @Test
    fun signIn_triggersInitialSyncAttempt() = runTest {
        coordinator = createCoordinator(backgroundScope)
        assertEquals(AppSyncState.Idle, coordinator.syncState.value)
        assertEquals(0, fakeEngine.syncInvocations.size)

        fakeAuthRepo.emitAuthenticated("user-signed-in")
        val state = coordinator.syncState.first { it is AppSyncState.Success }

        assertEquals(1, fakeEngine.syncInvocations.size)
        assertEquals("user-signed-in", fakeEngine.syncInvocations.first())
        assertIs<AppSyncState.Success>(state)
    }

    @Test
    fun concurrentRequests_preventOverlappingRuns() = runTest {
        coordinator = createCoordinator(backgroundScope)
        fakeAuthRepo.emitAuthenticated("user-1")
        coordinator.syncState.first { it is AppSyncState.Success }
        fakeEngine.syncInvocations.clear()

        val outboxDao = database.syncOutboxDao()
        outboxDao.insertOrUpdateMutation(
            SyncOutboxEntity(
                mutationId = "mut-concurrent-1",
                userId = "user-1",
                deviceId = "device-1",
                entityType = "TRANSACTION",
                entityId = "tx-1",
                operation = "UPSERT",
                payloadJson = "{}",
                updatedAtEpochMillis = 1000L,
                status = "PENDING",
                retryCount = 0,
                lastError = null,
                createdAtEpochMillis = 1000L
            )
        )

        // Reset concurrent metrics before issuing concurrent requests
        fakeEngine.maxConcurrentSyncs = 0

        // Trigger multiple concurrent sync requests
        val jobs = (1..5).map {
            coordinator.requestSync()
        }
        jobs.joinAll()

        // Mutex ensures maxConcurrentSyncs is strictly 1
        assertEquals(1, fakeEngine.maxConcurrentSyncs)
    }

    @Test
    fun syncFailure_emitsError_andRetainsPendingOutbox() = runTest {
        coordinator = createCoordinator(backgroundScope)
        fakeAuthRepo.emitAuthenticated("user-1")
        coordinator.syncState.first { it is AppSyncState.Success }
        fakeEngine.syncInvocations.clear()

        val outboxDao = database.syncOutboxDao()
        outboxDao.insertOrUpdateMutation(
            SyncOutboxEntity(
                mutationId = "mut-fail",
                userId = "user-1",
                deviceId = "device-1",
                entityType = "TRANSACTION",
                entityId = "tx-1",
                operation = "UPSERT",
                payloadJson = "{}",
                updatedAtEpochMillis = 1000L,
                status = "PENDING",
                retryCount = 0,
                lastError = null,
                createdAtEpochMillis = 1000L
            )
        )

        fakeEngine.syncHandler = {
            SyncResult.Failure(message = "Local network timeout", canRetry = true)
        }

        coordinator.requestSync().join()

        val state = coordinator.syncState.value
        assertIs<AppSyncState.Error>(state)
        assertEquals("Local network timeout", state.message)
        assertTrue(state.canRetry)

        // Mutation remains in outbox
        val pending = outboxDao.getPendingMutations("user-1")
        assertEquals(1, pending.size)
    }

    @Test
    fun signOut_resetsStateToIdle() = runTest {
        coordinator = createCoordinator(backgroundScope)
        fakeAuthRepo.emitAuthenticated("user-1")
        coordinator.syncState.first { it is AppSyncState.Success }

        fakeAuthRepo.emitUnauthenticated()
        val state = coordinator.syncState.first { it is AppSyncState.Idle }

        assertEquals(AppSyncState.Idle, state)
    }

    @Test
    fun pendingCount_emitsOutboxCount_andResetsOnSignOut() = runTest {
        coordinator = createCoordinator(backgroundScope)
        fakeAuthRepo.emitAuthenticated("user-1")
        coordinator.syncState.first { it is AppSyncState.Success }

        val outboxDao = database.syncOutboxDao()
        outboxDao.insertOrUpdateMutation(
            SyncOutboxEntity(
                mutationId = "mut-pending-1",
                userId = "user-1",
                deviceId = "device-1",
                entityType = "TRANSACTION",
                entityId = "tx-1",
                operation = "UPSERT",
                payloadJson = "{}",
                updatedAtEpochMillis = 1000L,
                status = "PENDING",
                retryCount = 0,
                lastError = null,
                createdAtEpochMillis = 1000L
            )
        )

        coordinator.pendingCount.first { it == 1 }
        assertEquals(1, coordinator.pendingCount.value)

        fakeAuthRepo.emitUnauthenticated()
        coordinator.pendingCount.first { it == 0 }
        assertEquals(0, coordinator.pendingCount.value)
    }

    @Test
    fun offlineDevice_requestSync_setsOfflineErrorWithoutInvokingEngine() = runTest {
        val isOnlineFlow = MutableStateFlow(false)
        val fakeConnectivity = object : org.hoods.pocket.domain.service.ConnectivityObserver {
            override val isOnline: StateFlow<Boolean> = isOnlineFlow.asStateFlow()
        }

        coordinator = createCoordinator(backgroundScope, connectivityObserver = fakeConnectivity)
        fakeAuthRepo.emitAuthenticated("user-1")

        val outboxDao = database.syncOutboxDao()
        outboxDao.insertOrUpdateMutation(
            SyncOutboxEntity(
                mutationId = "mut-offline-1",
                userId = "user-1",
                deviceId = "device-1",
                entityType = "TRANSACTION",
                entityId = "tx-1",
                operation = "UPSERT",
                payloadJson = "{}",
                updatedAtEpochMillis = 1000L,
                status = "PENDING",
                retryCount = 0,
                lastError = null,
                createdAtEpochMillis = 1000L
            )
        )

        fakeEngine.syncInvocations.clear()
        coordinator.requestSync().join()

        val state = coordinator.syncState.value
        assertIs<AppSyncState.Error>(state)
        assertEquals("Device is offline", state.message)
        assertEquals(0, fakeEngine.syncInvocations.size)
    }

    @Test
    fun offlineTransitionToOnline_triggersSyncWhenPendingMutationsExist() = runTest {
        val isOnlineFlow = MutableStateFlow(false)
        val fakeConnectivity = object : org.hoods.pocket.domain.service.ConnectivityObserver {
            override val isOnline: StateFlow<Boolean> = isOnlineFlow.asStateFlow()
        }

        coordinator = createCoordinator(backgroundScope, connectivityObserver = fakeConnectivity)
        fakeAuthRepo.emitAuthenticated("user-1")

        val outboxDao = database.syncOutboxDao()
        outboxDao.insertOrUpdateMutation(
            SyncOutboxEntity(
                mutationId = "mut-offline-2",
                userId = "user-1",
                deviceId = "device-1",
                entityType = "TRANSACTION",
                entityId = "tx-2",
                operation = "UPSERT",
                payloadJson = "{}",
                updatedAtEpochMillis = 1000L,
                status = "PENDING",
                retryCount = 0,
                lastError = null,
                createdAtEpochMillis = 1000L
            )
        )

        fakeEngine.syncInvocations.clear()
        // Transition to online
        isOnlineFlow.value = true

        coordinator.syncState.first { it is AppSyncState.Success }
        assertEquals(1, fakeEngine.syncInvocations.size)
    }
}
