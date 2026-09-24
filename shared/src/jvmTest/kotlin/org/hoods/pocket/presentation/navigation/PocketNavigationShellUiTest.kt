package org.hoods.pocket.presentation.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import org.hoods.pocket.domain.model.SyncStatus
import org.hoods.pocket.navigation.PocketNavigationShell
import org.hoods.pocket.navigation.TopLevelDestination
import org.hoods.pocket.presentation.layout.AppLayoutMode
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PocketNavigationShellUiTest {

    @Test
    fun compactLayout_steadyState_bannerIsHidden() = runComposeUiTest {
        setContent {
            PocketNavigationShell(
                activeDestination = TopLevelDestination.TRANSACTIONS,
                onNavigateToTransactions = {},
                onNavigateToBudgets = {},
                onNavigateToAccount = {},
                layoutMode = AppLayoutMode.Compact,
                syncStatus = SyncStatus.Success(
                    lastSyncedAtEpochMillis = 1000L,
                    pushedCount = 0,
                    pulledCount = 0
                )
            ) {
                Text("Screen Content")
            }
        }

        onNodeWithTag("sync_indicator_compact").assertDoesNotExist()
    }

    @Test
    fun compactLayout_offlineWithPendingChanges_bannerIsDisplayedAndClickNavigatesToAccount() = runComposeUiTest {
        var navigatedToAccount = false

        setContent {
            PocketNavigationShell(
                activeDestination = TopLevelDestination.TRANSACTIONS,
                onNavigateToTransactions = {},
                onNavigateToBudgets = {},
                onNavigateToAccount = { navigatedToAccount = true },
                layoutMode = AppLayoutMode.Compact,
                syncStatus = SyncStatus.Offline(pendingCount = 3)
            ) {
                Text("Screen Content")
            }
        }

        val banner = onNodeWithTag("sync_indicator_compact")
        banner.assertIsDisplayed()
        banner.assertTextContains("Offline • 3 change(s) waiting")

        banner.performClick()
        assertEquals(true, navigatedToAccount)
    }

    @Test
    fun compactLayout_syncing_bannerShowsSyncingMessage() = runComposeUiTest {
        setContent {
            PocketNavigationShell(
                activeDestination = TopLevelDestination.TRANSACTIONS,
                onNavigateToTransactions = {},
                onNavigateToBudgets = {},
                onNavigateToAccount = {},
                layoutMode = AppLayoutMode.Compact,
                syncStatus = SyncStatus.Syncing(pendingCount = 1)
            ) {
                Text("Screen Content")
            }
        }

        val banner = onNodeWithTag("sync_indicator_compact")
        banner.assertIsDisplayed()
        banner.assertTextContains("Syncing changes...")
    }

    @Test
    fun compactLayout_failure_bannerShowsFailureMessage() = runComposeUiTest {
        setContent {
            PocketNavigationShell(
                activeDestination = TopLevelDestination.TRANSACTIONS,
                onNavigateToTransactions = {},
                onNavigateToBudgets = {},
                onNavigateToAccount = {},
                layoutMode = AppLayoutMode.Compact,
                syncStatus = SyncStatus.Failure(pendingCount = 1, message = "Network error")
            ) {
                Text("Screen Content")
            }
        }

        val banner = onNodeWithTag("sync_indicator_compact")
        banner.assertIsDisplayed()
        banner.assertTextContains("Sync failed • Tap to view")
    }

    @Test
    fun expandedLayout_sidebar_displaysSyncIndicatorAndClickNavigatesToAccount() = runComposeUiTest {
        var navigatedToAccount = false

        setContent {
            PocketNavigationShell(
                activeDestination = TopLevelDestination.TRANSACTIONS,
                onNavigateToTransactions = {},
                onNavigateToBudgets = {},
                onNavigateToAccount = { navigatedToAccount = true },
                layoutMode = AppLayoutMode.Expanded,
                syncStatus = SyncStatus.Success(
                    lastSyncedAtEpochMillis = 2000L,
                    pushedCount = 5,
                    pulledCount = 2
                )
            ) {
                Text("Screen Content")
            }
        }

        val sidebarIndicator = onNodeWithTag("sync_indicator_expanded")
        sidebarIndicator.assertIsDisplayed()
        sidebarIndicator.assertTextContains("Cloud Synced")
        sidebarIndicator.assertTextContains("Up to date")

        sidebarIndicator.performClick()
        assertEquals(true, navigatedToAccount)
    }
}
