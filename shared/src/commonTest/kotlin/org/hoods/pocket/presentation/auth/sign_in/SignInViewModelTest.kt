package org.hoods.pocket.presentation.auth.sign_in

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.data.repository.FakeAuthRepository
import org.hoods.pocket.data.repository.FakeDataOwnershipRepository
import org.hoods.pocket.domain.model.UnclaimedDataSummary
import org.hoods.pocket.domain.usecase.GetUnclaimedDataSummaryUseCase
import org.hoods.pocket.domain.usecase.SignInUseCase
import org.hoods.pocket.domain.usecase.SignUpUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SignInViewModelTest {

    @Test
    fun initialState_isSignInModeWithEmptyFields() = runTest {
        val fakeRepo = FakeAuthRepository()
        val fakeDataRepo = FakeDataOwnershipRepository(UnclaimedDataSummary(0, 0, 0))
        val viewModel = SignInViewModel(
            signInUseCase = SignInUseCase(fakeRepo),
            signUpUseCase = SignUpUseCase(fakeRepo),
            getUnclaimedDataSummaryUseCase = GetUnclaimedDataSummaryUseCase(fakeDataRepo)
        )

        val state = viewModel.uiState.value
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.isSignUpMode)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNull(state.confirmationMessage)
    }

    @Test
    fun toggleMode_switchesBetweenSignInAndSignUp() = runTest {
        val fakeRepo = FakeAuthRepository()
        val fakeDataRepo = FakeDataOwnershipRepository(UnclaimedDataSummary(0, 0, 0))
        val viewModel = SignInViewModel(
            signInUseCase = SignInUseCase(fakeRepo),
            signUpUseCase = SignUpUseCase(fakeRepo),
            getUnclaimedDataSummaryUseCase = GetUnclaimedDataSummaryUseCase(fakeDataRepo)
        )

        viewModel.onToggleMode()
        assertTrue(viewModel.uiState.value.isSignUpMode)

        viewModel.onToggleMode()
        assertFalse(viewModel.uiState.value.isSignUpMode)
    }

    @Test
    fun submit_validatesEmailAndPassword() = runTest {
        val fakeRepo = FakeAuthRepository()
        val fakeDataRepo = FakeDataOwnershipRepository(UnclaimedDataSummary(0, 0, 0))
        val viewModel = SignInViewModel(
            signInUseCase = SignInUseCase(fakeRepo),
            signUpUseCase = SignUpUseCase(fakeRepo),
            getUnclaimedDataSummaryUseCase = GetUnclaimedDataSummaryUseCase(fakeDataRepo)
        )

        // Empty email
        viewModel.onSubmit()
        assertEquals("Please enter a valid email address", viewModel.uiState.value.errorMessage)

        // Invalid email format
        viewModel.onEmailChanged("invalid-email")
        viewModel.onSubmit()
        assertEquals("Please enter a valid email address", viewModel.uiState.value.errorMessage)

        // Short password
        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("123")
        viewModel.onSubmit()
        assertEquals("Password must be at least 6 characters", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun signIn_success_emitsSignInSuccessEvent() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val fakeRepo = FakeAuthRepository()
        val fakeDataRepo = FakeDataOwnershipRepository(UnclaimedDataSummary(0, 0, 0))
        val viewModel = SignInViewModel(
            signInUseCase = SignInUseCase(fakeRepo),
            signUpUseCase = SignUpUseCase(fakeRepo),
            getUnclaimedDataSummaryUseCase = GetUnclaimedDataSummaryUseCase(fakeDataRepo),
            coroutineScope = scope
        )

        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("securePassword123")

        var receivedEvent: SignInEvent? = null
        val job = launch(testDispatcher) {
            receivedEvent = viewModel.events.first()
        }

        viewModel.onSubmit()
        testScheduler.advanceUntilIdle()

        assertNotNull(receivedEvent)
        assertTrue(receivedEvent is SignInEvent.SignInSuccess)
        assertEquals("user@example.com", (receivedEvent as SignInEvent.SignInSuccess).user.email)
        job.cancel()
    }

    @Test
    fun signIn_withUnclaimedData_emitsClaimRequiredEvent() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val fakeRepo = FakeAuthRepository()
        val fakeDataRepo = FakeDataOwnershipRepository(UnclaimedDataSummary(2, 1, 1))
        val viewModel = SignInViewModel(
            signInUseCase = SignInUseCase(fakeRepo),
            signUpUseCase = SignUpUseCase(fakeRepo),
            getUnclaimedDataSummaryUseCase = GetUnclaimedDataSummaryUseCase(fakeDataRepo),
            coroutineScope = scope
        )

        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("securePassword123")

        var receivedEvent: SignInEvent? = null
        val job = launch(testDispatcher) {
            receivedEvent = viewModel.events.first()
        }

        viewModel.onSubmit()
        testScheduler.advanceUntilIdle()

        assertNotNull(receivedEvent)
        assertTrue(receivedEvent is SignInEvent.ClaimRequired)
        job.cancel()
    }

    @Test
    fun signUp_confirmationRequired_emitsSignUpConfirmationSentAndShowsMessage() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val fakeRepo = FakeAuthRepository(shouldRequireConfirmation = true)
        val fakeDataRepo = FakeDataOwnershipRepository(UnclaimedDataSummary(0, 0, 0))
        val viewModel = SignInViewModel(
            signInUseCase = SignInUseCase(fakeRepo),
            signUpUseCase = SignUpUseCase(fakeRepo),
            getUnclaimedDataSummaryUseCase = GetUnclaimedDataSummaryUseCase(fakeDataRepo),
            coroutineScope = scope
        )

        viewModel.onToggleMode() // Switch to sign-up mode
        viewModel.onEmailChanged("newuser@example.com")
        viewModel.onPasswordChanged("password123")

        var receivedEvent: SignInEvent? = null
        val job = launch(testDispatcher) {
            receivedEvent = viewModel.events.first()
        }

        viewModel.onSubmit()
        testScheduler.advanceUntilIdle()

        assertEquals(SignInEvent.SignUpConfirmationSent, receivedEvent)
        assertNotNull(viewModel.uiState.value.confirmationMessage)
        assertTrue(viewModel.uiState.value.confirmationMessage!!.contains("newuser@example.com"))
        assertNull(viewModel.uiState.value.errorMessage)
        job.cancel()
    }

    @Test
    fun signIn_error_updatesErrorMessage() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val fakeRepo = FakeAuthRepository(errorToReturn = "Invalid login credentials")
        val fakeDataRepo = FakeDataOwnershipRepository(UnclaimedDataSummary(0, 0, 0))
        val viewModel = SignInViewModel(
            signInUseCase = SignInUseCase(fakeRepo),
            signUpUseCase = SignUpUseCase(fakeRepo),
            getUnclaimedDataSummaryUseCase = GetUnclaimedDataSummaryUseCase(fakeDataRepo),
            coroutineScope = scope
        )

        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("wrongPassword")

        viewModel.onSubmit()
        testScheduler.advanceUntilIdle()

        assertEquals("Invalid login credentials", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
