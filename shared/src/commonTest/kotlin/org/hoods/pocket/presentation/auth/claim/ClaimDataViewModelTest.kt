package org.hoods.pocket.presentation.auth.claim

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hoods.pocket.data.repository.FakeDataOwnershipRepository
import org.hoods.pocket.domain.model.UnclaimedDataSummary
import org.hoods.pocket.domain.repository.DataOwnershipRepository
import org.hoods.pocket.domain.usecase.ClaimLocalDataUseCase
import org.hoods.pocket.domain.usecase.GetUnclaimedDataSummaryUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClaimDataViewModelTest {

    @Test
    fun loadSummary_populatesUiState() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val fakeRepo = FakeDataOwnershipRepository(UnclaimedDataSummary(5, 2, 4))
        val viewModel = ClaimDataViewModel(
            targetUserId = "user-1",
            getUnclaimedDataSummaryUseCase = GetUnclaimedDataSummaryUseCase(fakeRepo),
            claimLocalDataUseCase = ClaimLocalDataUseCase(fakeRepo),
            coroutineScope = scope
        )

        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("user-1", state.targetUserId)
        assertEquals(5, state.unclaimedSummary.transactionCount)
        assertEquals(2, state.unclaimedSummary.monthlyBudgetCount)
        assertEquals(4, state.unclaimedSummary.categoryBudgetCount)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun claimForAccount_success_emitsClaimCompleted() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val fakeRepo = FakeDataOwnershipRepository()
        val viewModel = ClaimDataViewModel(
            targetUserId = "user-xyz",
            getUnclaimedDataSummaryUseCase = GetUnclaimedDataSummaryUseCase(fakeRepo),
            claimLocalDataUseCase = ClaimLocalDataUseCase(fakeRepo),
            coroutineScope = scope
        )
        testScheduler.advanceUntilIdle()

        var receivedEvent: ClaimDataEvent? = null
        val job = launch(testDispatcher) {
            receivedEvent = viewModel.events.first()
        }

        viewModel.onClaimForAccount()
        testScheduler.advanceUntilIdle()

        assertEquals(ClaimDataEvent.ClaimCompleted, receivedEvent)
        assertEquals("user-xyz", fakeRepo.claimedUserId)
        assertFalse(viewModel.uiState.value.isClaiming)
        job.cancel()
    }

    @Test
    fun keepOnDevice_emitsKeepOnDeviceCompletedWithoutClaiming() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val fakeRepo = FakeDataOwnershipRepository()
        val viewModel = ClaimDataViewModel(
            targetUserId = "user-xyz",
            getUnclaimedDataSummaryUseCase = GetUnclaimedDataSummaryUseCase(fakeRepo),
            claimLocalDataUseCase = ClaimLocalDataUseCase(fakeRepo),
            coroutineScope = scope
        )
        testScheduler.advanceUntilIdle()

        var receivedEvent: ClaimDataEvent? = null
        val job = launch(testDispatcher) {
            receivedEvent = viewModel.events.first()
        }

        viewModel.onKeepOnDevice()
        testScheduler.advanceUntilIdle()

        assertEquals(ClaimDataEvent.KeepOnDeviceCompleted, receivedEvent)
        assertNull(fakeRepo.claimedUserId)
        job.cancel()
    }

    @Test
    fun claimForAccount_failure_setsErrorMessage() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val fakeRepo = FakeDataOwnershipRepository(shouldFailClaim = true)
        val viewModel = ClaimDataViewModel(
            targetUserId = "user-xyz",
            getUnclaimedDataSummaryUseCase = GetUnclaimedDataSummaryUseCase(fakeRepo),
            claimLocalDataUseCase = ClaimLocalDataUseCase(fakeRepo),
            coroutineScope = scope
        )
        testScheduler.advanceUntilIdle()

        viewModel.onClaimForAccount()
        testScheduler.advanceUntilIdle()

        assertEquals("Claiming failed", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isClaiming)
    }
}
