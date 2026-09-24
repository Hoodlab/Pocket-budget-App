package org.hoods.pocket.presentation.auth.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hoods.pocket.presentation.theme.PocketColors
import org.hoods.pocket.presentation.theme.PocketTheme
import androidx.compose.ui.tooling.preview.Preview

import org.hoods.pocket.domain.model.SyncStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountContent(
    uiState: AccountUiState,
    onSignOut: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onBackClick: () -> Unit,
    onSyncNow: () -> Unit = {},
    onRetrySync: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account & Settings", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onBackClick() }
                            .testTag("account_back_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "←",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(PocketColors.SurfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.isAuthenticated && !uiState.userEmail.isNullOrBlank()) {
                            uiState.userEmail.take(1).uppercase()
                        } else {
                            "👤"
                        },
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = if (uiState.isAuthenticated) uiState.userEmail.orEmpty() else "Local Offline Account",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("account_user_email")
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Status",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (uiState.isAuthenticated) {
                                    Text(
                                        text = "✓",
                                        fontWeight = FontWeight.Bold,
                                        color = PocketColors.Income,
                                        fontSize = 14.sp
                                    )
                                }
                                Text(
                                    text = if (uiState.isAuthenticated) "Signed In" else "Unauthenticated (Offline)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (uiState.isAuthenticated) PocketColors.Income else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Environment",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.environmentName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("account_environment_label")
                            )
                        }
                    }
                }

                // Cloud Synchronization Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_sync_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cloud Synchronization",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val isOffline = uiState.syncStatus is SyncStatus.Offline
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isOffline) PocketColors.SurfaceContainerHigh
                                        else PocketColors.PrimaryContainer
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isOffline) "Offline" else "Online",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isOffline) PocketColors.OnSurfaceVariant else PocketColors.OnPrimaryContainer,
                                    modifier = Modifier.testTag("account_network_status")
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val (title, description) = when (val s = uiState.syncStatus) {
                                is SyncStatus.SignedOut ->
                                    "Local-Only Mode" to "Changes remain stored on this device. Sign in to synchronize across devices."
                                is SyncStatus.Unconfigured ->
                                    "Sync Unavailable" to "Supabase backend is unconfigured. Connect a project in Environment Setup."
                                is SyncStatus.Offline ->
                                    "Device Offline" to if (s.pendingCount > 0) "${s.pendingCount} change(s) queued to sync when connected." else "You can continue working offline."
                                is SyncStatus.Syncing ->
                                    "Syncing with Cloud..." to "Synchronizing ${s.pendingCount} pending change(s)..."
                                is SyncStatus.Pending ->
                                    "${s.pendingCount} Change(s) Waiting" to "Local mutations queued for upload."
                                is SyncStatus.Success ->
                                    "Cloud Synced" to "Last synced: ${formatLastSyncedTime(s.lastSyncedAtEpochMillis)} • Pushed: ${s.pushedCount} | Pulled: ${s.pulledCount}"
                                is SyncStatus.Failure ->
                                    "Sync Failed" to "${s.message} • ${s.pendingCount} change(s) saved safely locally."
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (uiState.syncStatus is SyncStatus.Syncing) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when (uiState.syncStatus) {
                                        is SyncStatus.Failure -> MaterialTheme.colorScheme.error
                                        is SyncStatus.Success -> PocketColors.Income
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.testTag("account_sync_state_label")
                                )
                            }
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("account_sync_state_description")
                            )
                        }

                        if (uiState.canRetry) {
                            Button(
                                onClick = onRetrySync,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("retry_sync_button")
                            ) {
                                Text("Retry Sync")
                            }
                        } else if (uiState.canSyncNow) {
                            OutlinedButton(
                                onClick = onSyncNow,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("sync_now_button")
                            ) {
                                Text("Sync Now")
                            }
                        }
                    }
                }

                if (uiState.errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.isAuthenticated) {
                    Button(
                        onClick = onSignOut,
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PocketColors.Expense,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sign_out_button")
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text("Sign Out")
                        }
                    }
                } else {
                    Button(
                        onClick = onNavigateToAuth,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sign_in_or_register_button")
                    ) {
                        Text("Sign In or Create Account")
                    }
                }

                OutlinedButton(
                    onClick = onNavigateToSetup,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("configure_environment_button")
                ) {
                    Text(
                        text = "⚙  Supabase Environment Setup",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun AccountContentPreview() {
    PocketTheme {
        AccountContent(
            uiState = AccountUiState(
                userEmail = "developer@pocketbudget.org",
                isAuthenticated = true,
                environmentName = "Hosted Dev (hyetpnyruynichpsxyvz)"
            ),
            onSignOut = {},
            onNavigateToAuth = {},
            onNavigateToSetup = {},
            onBackClick = {}
        )
    }
}

private fun formatLastSyncedTime(lastSyncedAtEpochMillis: Long): String {
    if (lastSyncedAtEpochMillis == 0L) return "Never"
    val diff = kotlin.time.Clock.System.now().toEpochMilliseconds() - lastSyncedAtEpochMillis
    return when {
        diff < 0L -> "Just now"
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000L} min ago"
        diff < 86_400_000L -> "${diff / 3_600_000L} hr ago"
        else -> "${diff / 86_400_000L} day(s) ago"
    }
}
