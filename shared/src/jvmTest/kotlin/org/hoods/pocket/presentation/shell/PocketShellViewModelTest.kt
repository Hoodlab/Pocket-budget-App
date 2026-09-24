package org.hoods.pocket.presentation.shell

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.domain.model.AppSyncState
import org.hoods.pocket.domain.model.AuthState
import org.hoods.pocket.domain.model.AuthUser
import org.hoods.pocket.domain.model.SyncStatus
import org.hoods.pocket.domain.repository.AuthRepository
import org.hoods.pocket.domain.repository.AuthResult
import org.hoods.pocket.domain.service.ConnectivityObserver
import org.hoods.pocket.domain.service.SyncCoordinator
import org.hoods.pocket.domain.usecase.GetSyncStatusUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

class PocketShellViewModelTest {

    @Test
    fun syncStatus_collectsFromGetSyncStatusUseCase() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher + Job())

        val authFlow = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
        val syncStateFlow = MutableStateFlow<AppSyncState>(AppSyncState.Idle)
        val pendingCountFlow = MutableStateFlow(0)
        val isOnlineFlow = MutableStateFlow(true)

        val fakeAuthRepo = object : AuthRepository {
            override val authState = authFlow.asStateFlow()
            override suspend fun signUp(email: String, password: String) = AuthResult.Error("")
            override suspend fun signIn(email: String, password: String) = AuthResult.Error("")
            override suspend fun signOut() = Result.success(Unit)
            override fun currentUser() = (authFlow.value as? AuthState.Authenticated)?.user
        }
        val fakeSyncCoordinator = object : SyncCoordinator {
            override val syncState = syncStateFlow.asStateFlow()
            override val pendingCount = pendingCountFlow.asStateFlow()
            override fun requestSync() = Job().apply { complete() }
        }
        val fakeConnectivityObserver = object : ConnectivityObserver {
            override val isOnline = isOnlineFlow.asStateFlow()
        }

        val realUseCase = GetSyncStatusUseCase(
            authRepository = fakeAuthRepo,
            syncCoordinator = fakeSyncCoordinator,
            connectivityObserver = fakeConnectivityObserver,
            isConfigured = { true }
        )

        val viewModel = PocketShellViewModel(
            getSyncStatusUseCase = realUseCase,
            coroutineScope = scope
        )
        testScheduler.advanceUntilIdle()

        assertEquals(SyncStatus.SignedOut, viewModel.syncStatus.value)

        authFlow.value = AuthState.Authenticated(AuthUser("u1", "u1@test.com"))
        isOnlineFlow.value = false
        pendingCountFlow.value = 2
        testScheduler.advanceUntilIdle()
        assertEquals(SyncStatus.Offline(pendingCount = 2), viewModel.syncStatus.value)

        isOnlineFlow.value = true
        syncStateFlow.value = AppSyncState.Syncing(pendingCount = 2)
        testScheduler.advanceUntilIdle()
        assertEquals(SyncStatus.Syncing(pendingCount = 2), viewModel.syncStatus.value)

        syncStateFlow.value = AppSyncState.Success(lastSyncedAtEpochMillis = 1000L, pushedCount = 2, pulledCount = 0)
        pendingCountFlow.value = 0
        testScheduler.advanceUntilIdle()
        assertEquals(
            SyncStatus.Success(lastSyncedAtEpochMillis = 1000L, pushedCount = 2, pulledCount = 0),
            viewModel.syncStatus.value
        )

        scope.cancel()
    }
}
