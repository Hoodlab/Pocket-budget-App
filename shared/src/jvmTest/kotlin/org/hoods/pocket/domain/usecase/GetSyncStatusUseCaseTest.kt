package org.hoods.pocket.domain.usecase

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.AppSyncState
import org.hoods.pocket.domain.model.AuthState
import org.hoods.pocket.domain.model.AuthUser
import org.hoods.pocket.domain.model.SyncStatus
import org.hoods.pocket.domain.repository.AuthRepository
import org.hoods.pocket.domain.repository.AuthResult
import org.hoods.pocket.domain.service.ConnectivityObserver
import org.hoods.pocket.domain.service.SyncCoordinator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetSyncStatusUseCaseTest {

    private val authFlow = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    private val fakeAuthRepository = object : AuthRepository {
        override val authState: StateFlow<AuthState> = authFlow.asStateFlow()
        override suspend fun signUp(email: String, password: String): AuthResult = AuthResult.Success(AuthUser("1", email))
        override suspend fun signIn(email: String, password: String): AuthResult = AuthResult.Success(AuthUser("1", email))
        override suspend fun signOut(): Result<Unit> = Result.success(Unit)
        override fun currentUser(): AuthUser? = (authFlow.value as? AuthState.Authenticated)?.user
    }

    private val syncStateFlow = MutableStateFlow<AppSyncState>(AppSyncState.Idle)
    private val pendingCountFlow = MutableStateFlow(0)
    private val fakeSyncCoordinator = object : SyncCoordinator {
        override val syncState: StateFlow<AppSyncState> = syncStateFlow.asStateFlow()
        override val pendingCount: StateFlow<Int> = pendingCountFlow.asStateFlow()
        override fun requestSync(): Job = Job().apply { complete() }
    }

    private val isOnlineFlow = MutableStateFlow(true)
    private val fakeConnectivityObserver = object : ConnectivityObserver {
        override val isOnline: StateFlow<Boolean> = isOnlineFlow.asStateFlow()
    }

    private var isConfiguredVal = true

    private val useCase = GetSyncStatusUseCase(
        authRepository = fakeAuthRepository,
        syncCoordinator = fakeSyncCoordinator,
        connectivityObserver = fakeConnectivityObserver,
        isConfigured = { isConfiguredVal }
    )

    @Test
    fun unauthenticated_emitsSignedOut() = runTest {
        authFlow.value = AuthState.Unauthenticated
        val status = useCase().first()
        assertEquals(SyncStatus.SignedOut, status)
    }

    @Test
    fun unconfigured_emitsUnconfigured() = runTest {
        authFlow.value = AuthState.Authenticated(AuthUser("user-1", "test@test.org"))
        isConfiguredVal = false
        val status = useCase().first()
        assertEquals(SyncStatus.Unconfigured, status)
    }

    @Test
    fun offline_emitsOfflineWithPendingCount() = runTest {
        authFlow.value = AuthState.Authenticated(AuthUser("user-1", "test@test.org"))
        isConfiguredVal = true
        isOnlineFlow.value = false
        pendingCountFlow.value = 3

        val status = useCase().first()
        assertEquals(SyncStatus.Offline(pendingCount = 3), status)
    }

    @Test
    fun syncing_emitsSyncingWithCount() = runTest {
        authFlow.value = AuthState.Authenticated(AuthUser("user-1", "test@test.org"))
        isConfiguredVal = true
        isOnlineFlow.value = true
        syncStateFlow.value = AppSyncState.Syncing(pendingCount = 2)

        val status = useCase().first()
        assertEquals(SyncStatus.Syncing(pendingCount = 2), status)
    }

    @Test
    fun error_emitsFailureWithRetryFlag() = runTest {
        authFlow.value = AuthState.Authenticated(AuthUser("user-1", "test@test.org"))
        isConfiguredVal = true
        isOnlineFlow.value = true
        syncStateFlow.value = AppSyncState.Error(
            message = "Network timeout",
            pendingCount = 4,
            canRetry = true
        )

        val status = useCase().first()
        assertEquals(
            SyncStatus.Failure(
                message = "Network timeout",
                pendingCount = 4,
                canRetry = true
            ),
            status
        )
    }

    @Test
    fun pending_emitsPendingWithCount() = runTest {
        authFlow.value = AuthState.Authenticated(AuthUser("user-1", "test@test.org"))
        isConfiguredVal = true
        isOnlineFlow.value = true
        syncStateFlow.value = AppSyncState.Pending(pendingCount = 5)

        val status = useCase().first()
        assertEquals(SyncStatus.Pending(pendingCount = 5), status)
    }

    @Test
    fun successWithZeroPending_emitsSuccess() = runTest {
        authFlow.value = AuthState.Authenticated(AuthUser("user-1", "test@test.org"))
        isConfiguredVal = true
        isOnlineFlow.value = true
        pendingCountFlow.value = 0
        syncStateFlow.value = AppSyncState.Success(
            lastSyncedAtEpochMillis = 1700000000000L,
            pushedCount = 3,
            pulledCount = 1
        )

        val status = useCase().first()
        assertEquals(
            SyncStatus.Success(
                lastSyncedAtEpochMillis = 1700000000000L,
                pushedCount = 3,
                pulledCount = 1
            ),
            status
        )
    }

    @Test
    fun successWithNonZeroPending_emitsPending() = runTest {
        authFlow.value = AuthState.Authenticated(AuthUser("user-1", "test@test.org"))
        isConfiguredVal = true
        isOnlineFlow.value = true
        pendingCountFlow.value = 1
        syncStateFlow.value = AppSyncState.Success(
            lastSyncedAtEpochMillis = 1700000000000L,
            pushedCount = 3,
            pulledCount = 1
        )

        val status = useCase().first()
        assertEquals(SyncStatus.Pending(pendingCount = 1), status)
    }
}
