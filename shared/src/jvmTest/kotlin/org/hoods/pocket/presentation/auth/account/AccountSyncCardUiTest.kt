package org.hoods.pocket.presentation.auth.account

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import org.hoods.pocket.domain.model.SyncStatus
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AccountSyncCardUiTest {

    @Test
    fun signedOut_showsLocalOnlyMode_noSyncButtons() = runComposeUiTest {
        setContent {
            AccountContent(
                uiState = AccountUiState(
                    isAuthenticated = false,
                    syncStatus = SyncStatus.SignedOut
                ),
                onSignOut = {},
                onNavigateToAuth = {},
                onNavigateToSetup = {},
                onBackClick = {},
                onSyncNow = {},
                onRetrySync = {}
            )
        }

        onNodeWithTag("account_sync_card").assertIsDisplayed()
        val label = onNodeWithTag("account_sync_state_label")
        label.assertIsDisplayed()
        label.assertTextContains("Local-Only Mode")

        onNodeWithTag("sync_now_button").assertDoesNotExist()
        onNodeWithTag("retry_sync_button").assertDoesNotExist()
    }

    @Test
    fun authenticated_offlineWithPendingChanges_showsOfflineBadgeAndQueueText() = runComposeUiTest {
        setContent {
            AccountContent(
                uiState = AccountUiState(
                    isAuthenticated = true,
                    userEmail = "user@example.com",
                    syncStatus = SyncStatus.Offline(pendingCount = 4)
                ),
                onSignOut = {},
                onNavigateToAuth = {},
                onNavigateToSetup = {},
                onBackClick = {},
                onSyncNow = {},
                onRetrySync = {}
            )
        }

        val networkStatus = onNodeWithTag("account_network_status")
        networkStatus.assertIsDisplayed()
        networkStatus.assertTextContains("Offline")

        val stateLabel = onNodeWithTag("account_sync_state_label")
        stateLabel.assertIsDisplayed()
        stateLabel.assertTextContains("Device Offline")

        val desc = onNodeWithTag("account_sync_state_description")
        desc.assertIsDisplayed()
        desc.assertTextContains("4 change(s) queued to sync when connected.")

        onNodeWithTag("sync_now_button").assertDoesNotExist()
        onNodeWithTag("retry_sync_button").assertDoesNotExist()
    }

    @Test
    fun authenticated_pendingChangesOnline_showsSyncNowButtonAndInvokesCallback() = runComposeUiTest {
        var syncNowClicked = false

        setContent {
            AccountContent(
                uiState = AccountUiState(
                    isAuthenticated = true,
                    userEmail = "user@example.com",
                    syncStatus = SyncStatus.Pending(pendingCount = 2)
                ),
                onSignOut = {},
                onNavigateToAuth = {},
                onNavigateToSetup = {},
                onBackClick = {},
                onSyncNow = { syncNowClicked = true },
                onRetrySync = {}
            )
        }

        val networkStatus = onNodeWithTag("account_network_status")
        networkStatus.assertIsDisplayed()
        networkStatus.assertTextContains("Online")

        val stateLabel = onNodeWithTag("account_sync_state_label")
        stateLabel.assertIsDisplayed()
        stateLabel.assertTextContains("2 Change(s) Waiting")

        val syncNowButton = onNodeWithTag("sync_now_button")
        syncNowButton.assertIsDisplayed()
        syncNowButton.performClick()

        assertEquals(true, syncNowClicked)
    }

    @Test
    fun authenticated_failure_showsRetryButtonAndInvokesCallback() = runComposeUiTest {
        var retryClicked = false

        setContent {
            AccountContent(
                uiState = AccountUiState(
                    isAuthenticated = true,
                    userEmail = "user@example.com",
                    syncStatus = SyncStatus.Failure(
                        pendingCount = 1,
                        message = "Network connection timed out"
                    )
                ),
                onSignOut = {},
                onNavigateToAuth = {},
                onNavigateToSetup = {},
                onBackClick = {},
                onSyncNow = {},
                onRetrySync = { retryClicked = true }
            )
        }

        val stateLabel = onNodeWithTag("account_sync_state_label")
        stateLabel.assertIsDisplayed()
        stateLabel.assertTextContains("Sync Failed")

        val desc = onNodeWithTag("account_sync_state_description")
        desc.assertIsDisplayed()
        desc.assertTextContains("Network connection timed out", substring = true)

        val retryButton = onNodeWithTag("retry_sync_button")
        retryButton.assertIsDisplayed()
        retryButton.performClick()

        assertEquals(true, retryClicked)
    }

    @Test
    fun authenticated_success_showsCloudSyncedAndSyncNow() = runComposeUiTest {
        setContent {
            AccountContent(
                uiState = AccountUiState(
                    isAuthenticated = true,
                    userEmail = "user@example.com",
                    syncStatus = SyncStatus.Success(
                        lastSyncedAtEpochMillis = 1700000000000L,
                        pushedCount = 3,
                        pulledCount = 1
                    )
                ),
                onSignOut = {},
                onNavigateToAuth = {},
                onNavigateToSetup = {},
                onBackClick = {},
                onSyncNow = {},
                onRetrySync = {}
            )
        }

        val stateLabel = onNodeWithTag("account_sync_state_label")
        stateLabel.assertIsDisplayed()
        stateLabel.assertTextContains("Cloud Synced")

        val desc = onNodeWithTag("account_sync_state_description")
        desc.assertIsDisplayed()
        desc.assertTextContains("Pushed: 3 | Pulled: 1", substring = true)

        onNodeWithTag("sync_now_button").assertIsDisplayed()
        onNodeWithTag("retry_sync_button").assertDoesNotExist()
    }
}
