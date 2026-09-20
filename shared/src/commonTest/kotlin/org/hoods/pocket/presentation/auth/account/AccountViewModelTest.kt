package org.hoods.pocket.presentation.auth.account

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.data.remote.SupabaseConfig
import org.hoods.pocket.data.repository.FakeAuthRepository
import org.hoods.pocket.domain.model.AuthState
import org.hoods.pocket.domain.model.AuthUser
import org.hoods.pocket.domain.usecase.GetAuthStateUseCase
import org.hoods.pocket.domain.usecase.SignOutUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountViewModelTest {

    @Test
    fun unauthenticated_showsUnauthenticatedState() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val fakeRepo = FakeAuthRepository(initialState = AuthState.Unauthenticated)
        val viewModel = AccountViewModel(
            getAuthStateUseCase = GetAuthStateUseCase(fakeRepo),
            signOutUseCase = SignOutUseCase(fakeRepo),
            config = SupabaseConfig.local(),
            coroutineScope = scope
        )
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isAuthenticated)
        assertNull(state.userEmail)
        assertEquals("Local Supabase (127.0.0.1:54321)", state.environmentName)
    }

    @Test
    fun authenticated_showsUserEmail() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val user = AuthUser("user-1", "test@pocket.org")
        val fakeRepo = FakeAuthRepository(initialState = AuthState.Authenticated(user))
        val viewModel = AccountViewModel(
            getAuthStateUseCase = GetAuthStateUseCase(fakeRepo),
            signOutUseCase = SignOutUseCase(fakeRepo),
            config = SupabaseConfig.hostedDev(),
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
        val viewModel = AccountViewModel(
            getAuthStateUseCase = GetAuthStateUseCase(fakeRepo),
            signOutUseCase = SignOutUseCase(fakeRepo),
            config = SupabaseConfig.local(),
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
}
