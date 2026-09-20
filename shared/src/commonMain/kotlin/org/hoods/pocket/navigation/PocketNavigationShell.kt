package org.hoods.pocket.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hoods.pocket.presentation.layout.AppLayoutMode
import org.hoods.pocket.presentation.theme.PocketColors

enum class TopLevelDestination {
    TRANSACTIONS,
    BUDGETS,
    ACCOUNT
}

@Composable
fun PocketNavigationShell(
    activeDestination: TopLevelDestination?,
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToAccount: () -> Unit,
    layoutMode: AppLayoutMode,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (layoutMode.isCompact) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(PocketColors.Surface)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                content()
            }
            PocketBottomNavigationBar(
                activeDestination = activeDestination,
                onNavigateToTransactions = onNavigateToTransactions,
                onNavigateToBudgets = onNavigateToBudgets,
                onNavigateToAccount = onNavigateToAccount
            )
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(PocketColors.Surface)
        ) {
            PocketNavigationSidebar(
                activeDestination = activeDestination,
                onNavigateToTransactions = onNavigateToTransactions,
                onNavigateToBudgets = onNavigateToBudgets,
                onNavigateToAccount = onNavigateToAccount
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(PocketColors.OutlineVariant.copy(alpha = 0.3f))
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                content()
            }
        }
    }
}

@Composable
fun PocketNavigationSidebar(
    activeDestination: TopLevelDestination?,
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(PocketColors.Surface)
            .padding(24.dp)
    ) {
        Text(
            text = "Pocket Budget",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = PocketColors.Primary
        )
        Text(
            text = "Financial Wellness",
            fontSize = 13.sp,
            color = PocketColors.OnSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Navigation Item: Transactions
        SidebarNavItem(
            label = "Transactions",
            isSelected = activeDestination == TopLevelDestination.TRANSACTIONS,
            onClick = onNavigateToTransactions,
            testTag = "nav_transactions"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Navigation Item: Budgets
        SidebarNavItem(
            label = "Budgets",
            isSelected = activeDestination == TopLevelDestination.BUDGETS,
            onClick = onNavigateToBudgets,
            testTag = "nav_budgets"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Navigation Item: Account
        SidebarNavItem(
            label = "Account",
            isSelected = activeDestination == TopLevelDestination.ACCOUNT,
            onClick = onNavigateToAccount,
            testTag = "nav_account"
        )
    }
}

@Composable
private fun SidebarNavItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) PocketColors.SurfaceContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
            .testTag(testTag),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) PocketColors.Primary else PocketColors.OnSurfaceVariant
        )
    }
}

@Composable
fun PocketBottomNavigationBar(
    activeDestination: TopLevelDestination?,
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(PocketColors.SurfaceContainerLowest)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            icon = "🧾",
            label = "Transactions",
            isSelected = activeDestination == TopLevelDestination.TRANSACTIONS,
            onClick = onNavigateToTransactions,
            testTag = "nav_transactions"
        )

        BottomNavItem(
            icon = "💵",
            label = "Budgets",
            isSelected = activeDestination == TopLevelDestination.BUDGETS,
            onClick = onNavigateToBudgets,
            testTag = "nav_budgets"
        )

        BottomNavItem(
            icon = "👤",
            label = "Account",
            isSelected = activeDestination == TopLevelDestination.ACCOUNT,
            onClick = onNavigateToAccount,
            testTag = "nav_account"
        )
    }
}

@Composable
private fun BottomNavItem(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) {
                    Modifier
                        .background(PocketColors.SurfaceContainer)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                } else {
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                }
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 18.sp)
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) PocketColors.Primary else PocketColors.OnSurfaceVariant
        )
    }
}
