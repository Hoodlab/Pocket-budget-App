package org.hoods.pocket.presentation.auth.account

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.data.remote.SupabaseConfig
import org.hoods.pocket.data.remote.SupabaseEnvironment
import org.hoods.pocket.data.repository.FakeAuthRepository
import org.hoods.pocket.domain.model.AppSyncState
import org.hoods.pocket.domain.model.AuthState
import org.hoods.pocket.domain.model.AuthUser
import org.hoods.pocket.domain.model.SyncStatus
import org.hoods.pocket.domain.service.ConnectivityObserver
import org.hoods.pocket.domain.service.SyncCoordinator
import org.hoods.pocket.domain.usecase.GetAuthStateUseCase
import org.hoods.pocket.domain.usecase.GetSyncStatusUseCase
import org.hoods.pocket.domain.usecase.RequestSyncUseCase
import org.hoods.pocket.domain.usecase.SignOutUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountViewModelTest {

    private class FakeSyncCoordinator : SyncCoordinator {
        val syncStateFlow = MutableStateFlow<AppSyncState>(AppSyncState.Idle)
        val pendingCountFlow = MutableStateFlow(0)
        var requestSyncCount = 0

        override val syncState = syncStateFlow.asStateFlow()
        override val pendingCount = pendingCountFlow.asStateFlow()
        override fun requestSync(): Job {
            requestSyncCount++
            return Job().apply { complete() }
        }
    }

    private class FakeConnectivityObserver(initialOnline: Boolean = true) : ConnectivityObserver {
        val isOnlineFlow = MutableStateFlow(initialOnline)
        override val isOnline = isOnlineFlow.asStateFlow()
    }

    private fun testLocalConfig() = SupabaseConfig(
        environment = SupabaseEnvironment.LOCAL,
        url = "http://127.0.0.1:54321",
        publishableKey = "sb_publishable_test_key"
    )

    private fun testHostedDevConfig() = SupabaseConfig(
        environment = SupabaseEnvironment.HOSTED_DEV,
        url = "https://hyetpnyruynichpsxyvz.supabase.co",
        publishableKey = "sb_publishable_test_key"
    )

    @Test
    fun unauthenticated_showsUnauthenticatedState() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val fakeRepo = FakeAuthRepository(initialState = AuthState.Unauthenticated)
        val fakeSyncCoordinator = FakeSyncCoordinator()
        val fakeConnectivity = FakeConnectivityObserver()
        val config = testLocalConfig()

        val viewModel = AccountViewModel(
            getAuthStateUseCase = GetAuthStateUseCase(fakeRepo),
            signOutUseCase = SignOutUseCase(fakeRepo),
            getSyncStatusUseCase = GetSyncStatusUseCase(
                authRepository = fakeRepo,
                syncCoordinator = fakeSyncCoordinator,
                connectivityObserver = fakeConnectivity,
                isConfigured = { config.isConfigured }
            ),
            requestSyncUseCase = RequestSyncUseCase(fakeSyncCoordinator),
            config = config,
            coroutineScope = scope
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isAuthenticated)
        assertNull(state.userEmail)
        assertEquals("Local Supabase (127.0.0.1:54321)", state.environmentName)
        assertEquals(SyncStatus.SignedOut, state.syncStatus)
        assertFalse(state.canSyncNow)
        assertFalse(state.canRetry)
    }

    @Test
    fun authenticated_showsUserEmail() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val user = AuthUser("user-1", "test@pocket.org")
        val fakeRepo = FakeAuthRepository(initialState = AuthState.Authenticated(user))
        val fakeSyncCoordinator = FakeSyncCoordinator()
        val fakeConnectivity = FakeConnectivityObserver()
        val config = testHostedDevConfig()

        val viewModel = AccountViewModel(
            getAuthStateUseCase = GetAuthStateUseCase(fakeRepo),
            signOutUseCase = SignOutUseCase(fakeRepo),
            getSyncStatusUseCase = GetSyncStatusUseCase(
                authRepository = fakeRepo,
                syncCoordinator = fakeSyncCoordinator,
                connectivityObserver = fakeConnectivity,
                isConfigured = { config.isConfigured }
            ),
            requestSyncUseCase = RequestSyncUseCase(fakeSyncCoordinator),
            config = config,
            coroutineScope = scope
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAuthenticated)
        assertEquals("test@pocket.org", state.userEmail)
        assertEquals("Hosted Dev (hyetpnyruynichpsxyvz)", state.environmentName)
    }

    @Test
    fun signOut_triggersSignOutSuccessEvent() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val user = AuthUser("user-1", "test@pocket.org")
        val fakeRepo = FakeAuthRepository(initialState = AuthState.Authenticated(user))
        val fakeSyncCoordinator = FakeSyncCoordinator()
        val fakeConnectivity = FakeConnectivityObserver()
        val config = testLocalConfig()

        val viewModel = AccountViewModel(
            getAuthStateUseCase = GetAuthStateUseCase(fakeRepo),
            signOutUseCase = SignOutUseCase(fakeRepo),
            getSyncStatusUseCase = GetSyncStatusUseCase(
                authRepository = fakeRepo,
                syncCoordinator = fakeSyncCoordinator,
                connectivityObserver = fakeConnectivity,
                isConfigured = { config.isConfigured }
            ),
            requestSyncUseCase = RequestSyncUseCase(fakeSyncCoordinator),
            config = config,
            coroutineScope = scope
        )
        testScheduler.advanceUntilIdle()

        var receivedEvent: AccountEvent? = null
        val job = launch(testDispatcher) {
            receivedEvent = viewModel.events.first()
        }

        viewModel.onSignOut()
        testScheduler.advanceUntilIdle()

        assertEquals(AccountEvent.SignOutSuccess, receivedEvent)
        assertEquals(AuthState.Unauthenticated, fakeRepo.authState.value)
        job.cancel()
    }

    @Test
    fun authenticated_online_withSuccess_canSyncNowIsTrue() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val user = AuthUser("user-1", "test@pocketbudget.test")
        val fakeRepo = FakeAuthRepository(initialState = AuthState.Authenticated(user))
        val fakeSyncCoordinator = FakeSyncCoordinator().apply {
            syncStateFlow.value = AppSyncState.Success(lastSyncedAtEpochMillis = 1000L, pushedCount = 1, pulledCount = 0)
            pendingCountFlow.value = 0
        }
        val fakeConnectivity = FakeConnectivityObserver(initialOnline = true)
        val config = testLocalConfig()

        val viewModel = AccountViewModel(
            getAuthStateUseCase = GetAuthStateUseCase(fakeRepo),
            signOutUseCase = SignOutUseCase(fakeRepo),
            getSyncStatusUseCase = GetSyncStatusUseCase(
                authRepository = fakeRepo,
                syncCoordinator = fakeSyncCoordinator,
                connectivityObserver = fakeConnectivity,
                isConfigured = { config.isConfigured }
            ),
            requestSyncUseCase = RequestSyncUseCase(fakeSyncCoordinator),
            config = config,
            coroutineScope = scope
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAuthenticated)
        assertEquals(
            SyncStatus.Success(lastSyncedAtEpochMillis = 1000L, pushedCount = 1, pulledCount = 0),
            state.syncStatus
        )
        assertTrue(state.canSyncNow)
        assertFalse(state.canRetry)

        viewModel.onSyncNow()
        testScheduler.advanceUntilIdle()
        assertEquals(1, fakeSyncCoordinator.requestSyncCount)
    }

    @Test
    fun authenticated_failure_canRetryIsTrue_andInvokesSync() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val user = AuthUser("user-1", "test@pocketbudget.test")
        val fakeRepo = FakeAuthRepository(initialState = AuthState.Authenticated(user))
        val fakeSyncCoordinator = FakeSyncCoordinator().apply {
            syncStateFlow.value = AppSyncState.Error(message = "Network error", pendingCount = 2, canRetry = true)
            pendingCountFlow.value = 2
        }
        val fakeConnectivity = FakeConnectivityObserver(initialOnline = true)
        val config = testLocalConfig()

        val viewModel = AccountViewModel(
            getAuthStateUseCase = GetAuthStateUseCase(fakeRepo),
            signOutUseCase = SignOutUseCase(fakeRepo),
            getSyncStatusUseCase = GetSyncStatusUseCase(
                authRepository = fakeRepo,
                syncCoordinator = fakeSyncCoordinator,
                connectivityObserver = fakeConnectivity,
                isConfigured = { config.isConfigured }
            ),
            requestSyncUseCase = RequestSyncUseCase(fakeSyncCoordinator),
            config = config,
            coroutineScope = scope
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.canRetry)
        assertEquals(
            SyncStatus.Failure(message = "Network error", pendingCount = 2, canRetry = true),
            state.syncStatus
        )

        viewModel.onRetrySync()
        testScheduler.advanceUntilIdle()
        assertEquals(1, fakeSyncCoordinator.requestSyncCount)
    }

    @Test
    fun offline_canSyncNowAndCanRetryAreFalse() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val user = AuthUser("user-1", "test@pocketbudget.test")
        val fakeRepo = FakeAuthRepository(initialState = AuthState.Authenticated(user))
        val fakeSyncCoordinator = FakeSyncCoordinator().apply {
            pendingCountFlow.value = 3
        }
        val fakeConnectivity = FakeConnectivityObserver(initialOnline = false)
        val config = testLocalConfig()

        val viewModel = AccountViewModel(
            getAuthStateUseCase = GetAuthStateUseCase(fakeRepo),
            signOutUseCase = SignOutUseCase(fakeRepo),
            getSyncStatusUseCase = GetSyncStatusUseCase(
                authRepository = fakeRepo,
                syncCoordinator = fakeSyncCoordinator,
                connectivityObserver = fakeConnectivity,
                isConfigured = { config.isConfigured }
            ),
            requestSyncUseCase = RequestSyncUseCase(fakeSyncCoordinator),
            config = config,
            coroutineScope = scope
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SyncStatus.Offline(pendingCount = 3), state.syncStatus)
        assertFalse(state.canSyncNow)
        assertFalse(state.canRetry)
    }
}
