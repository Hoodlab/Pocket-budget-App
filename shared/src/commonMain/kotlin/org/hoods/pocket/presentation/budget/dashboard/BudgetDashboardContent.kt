package org.hoods.pocket.presentation.budget.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hoods.pocket.domain.model.BudgetHealthStatus
import org.hoods.pocket.domain.util.MoneyParser
import org.hoods.pocket.presentation.theme.PocketColors
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.abs

@Composable
fun BudgetDashboardContent(
    uiState: BudgetDashboardUiState,
    onSetBudgetClick: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
    onCurrentMonthClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(PocketColors.Surface)
    ) {
        val isDesktop = maxWidth >= 720.dp

        if (isDesktop) {
            // ==========================================
            // Desktop Layout (Widescreen Sidebar + Main Panel)
            // ==========================================
            Row(modifier = Modifier.fillMaxSize()) {
                // Fixed Sidebar (260dp)
                Column(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .background(PocketColors.Surface)
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    Text(
                        text = "Pocket Budget",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PocketColors.Primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Financial Wellness",
                        fontSize = 13.sp,
                        color = PocketColors.OnSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Primary Action: + Set Monthly Budget
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(PocketColors.Primary)
                            .clickable { onSetBudgetClick() }
                            .testTag("sidebar_set_budget_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Set Budget",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PocketColors.OnPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Navigation Item: Transactions
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigateToTransactions() }
                            .padding(horizontal = 16.dp)
                            .testTag("nav_transactions"),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Transactions",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = PocketColors.OnSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Active Navigation Item: Budgets
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PocketColors.SurfaceContainer)
                            .padding(horizontal = 16.dp)
                            .testTag("nav_budgets"),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Budgets",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PocketColors.Primary
                        )
                    }
                }

                // Vertical hairline divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(PocketColors.OutlineVariant.copy(alpha = 0.3f))
                )

                // Main Content Panel
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(32.dp)
                ) {
                    // Top Header: Title + Month Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Budget Progress",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = PocketColors.OnSurface
                        )

                        // Month Navigation Control
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = uiState.monthTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PocketColors.Primary,
                                modifier = Modifier.testTag("selected_month_text")
                            )

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(PocketColors.SurfaceContainer)
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .clickable { onPreviousMonthClick() }
                                        .testTag("previous_month_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "‹", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PocketColors.OnSurface)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .clickable { onNextMonthClick() }
                                        .testTag("next_month_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "›", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PocketColors.OnSurface)
                                }
                            }

                            if (!uiState.isCurrentMonth) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(PocketColors.Primary)
                                        .clickable { onCurrentMonthClick() }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("current_month_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Today",
                                        color = PocketColors.OnPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Main Budget Card Container
                    Card(
                        modifier = Modifier
                            .widthIn(max = 800.dp)
                            .fillMaxWidth()
                            .shadow(
                                elevation = 3.dp,
                                shape = RoundedCornerShape(24.dp),
                                spotColor = PocketColors.OnSurface.copy(alpha = 0.08f)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = PocketColors.SurfaceContainerLowest)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        ) {
                            if (uiState.hasBudget && uiState.summary != null) {
                                val summary = uiState.summary
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Total Budget Used",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PocketColors.OnSurface
                                    )
                                    HealthBadge(status = summary.status)
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = PocketColors.OutlineVariant.copy(alpha = 0.25f), thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(20.dp))

                                val spentFormatted = MoneyParser.formatCentsToAmount(summary.totalExpense)
                                val limitFormatted = MoneyParser.formatCentsToAmount(summary.budget?.limitAmount ?: 0L)

                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "$$spentFormatted",
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PocketColors.OnSurface
                                    )
                                    Text(
                                        text = " / $$limitFormatted",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = PocketColors.OnSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                val progressColor = when (summary.status) {
                                    BudgetHealthStatus.OVER_BUDGET -> PocketColors.Expense
                                    BudgetHealthStatus.NEAR_LIMIT -> Color(0xFFE65100) // Amber/Orange
                                    else -> PocketColors.Primary
                                }

                                LinearProgressIndicator(
                                    progress = { summary.progress.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(CircleShape)
                                        .testTag("budget_progress_bar"),
                                    color = progressColor,
                                    trackColor = PocketColors.SurfaceContainer
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "0%", fontSize = 12.sp, color = PocketColors.OnSurfaceVariant)
                                    val percentUsed = (summary.progress * 100).toInt()
                                    Text(text = "$percentUsed% Used", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PocketColors.OnSurfaceVariant)
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                val remaining = summary.remainingAmount ?: 0L
                                if (remaining >= 0) {
                                    val remainingFormatted = MoneyParser.formatCentsToAmount(remaining)
                                    Text(
                                        text = "$$remainingFormatted left",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PocketColors.Primary,
                                        modifier = Modifier.testTag("remaining_amount_text")
                                    )
                                } else {
                                    val overFormatted = MoneyParser.formatCentsToAmount(abs(remaining))
                                    Text(
                                        text = "-$$overFormatted over budget",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PocketColors.Expense,
                                        modifier = Modifier.testTag("remaining_amount_text")
                                    )
                                }
                            } else {
                                // Desktop Empty State
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No budget set for ${uiState.monthTitle}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PocketColors.OnSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Set a monthly spending limit to track your expenses",
                                        fontSize = 14.sp,
                                        color = PocketColors.OnSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = onSetBudgetClick,
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("set_monthly_budget_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = PocketColors.Primary),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "+ Set Monthly Budget",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PocketColors.OnPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // Mobile Layout (Handheld Portrait)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 72.dp) // Leave space for bottom nav
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                // Header: Title + Month selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monthly Budgets",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = PocketColors.OnSurface
                    )

                    // Month Selector Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(PocketColors.SurfaceContainer)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "‹",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PocketColors.OnSurface,
                            modifier = Modifier
                                .clickable { onPreviousMonthClick() }
                                .padding(horizontal = 4.dp)
                                .testTag("previous_month_button")
                        )

                        Text(
                            text = uiState.monthTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PocketColors.Primary,
                            modifier = Modifier.testTag("selected_month_text")
                        )

                        Text(
                            text = "›",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PocketColors.OnSurface,
                            modifier = Modifier
                                .clickable { onNextMonthClick() }
                                .padding(horizontal = 4.dp)
                                .testTag("next_month_button")
                        )
                    }
                }

                if (!uiState.isCurrentMonth) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(PocketColors.Primary)
                                .clickable { onCurrentMonthClick() }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("current_month_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Return to Current Month",
                                color = PocketColors.OnPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Card Container
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PocketColors.SurfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        if (uiState.hasBudget && uiState.summary != null) {
                            val summary = uiState.summary
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Budget Used",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PocketColors.OnSurface
                                )
                                HealthBadge(status = summary.status)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            val spentFormatted = MoneyParser.formatCentsToAmount(summary.totalExpense)
                            val limitFormatted = MoneyParser.formatCentsToAmount(summary.budget?.limitAmount ?: 0L)

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$$spentFormatted",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PocketColors.OnSurface
                                )
                                Text(
                                    text = " / $$limitFormatted",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PocketColors.OnSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            val progressColor = when (summary.status) {
                                BudgetHealthStatus.OVER_BUDGET -> PocketColors.Expense
                                BudgetHealthStatus.NEAR_LIMIT -> Color(0xFFE65100)
                                else -> PocketColors.Primary
                            }

                            LinearProgressIndicator(
                                progress = { summary.progress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .testTag("budget_progress_bar"),
                                color = progressColor,
                                trackColor = PocketColors.SurfaceContainer
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "0%", fontSize = 12.sp, color = PocketColors.OnSurfaceVariant)
                                val percentUsed = (summary.progress * 100).toInt()
                                Text(text = "$percentUsed% Used", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PocketColors.OnSurfaceVariant)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            val remaining = summary.remainingAmount ?: 0L
                            if (remaining >= 0) {
                                val remainingFormatted = MoneyParser.formatCentsToAmount(remaining)
                                Text(
                                    text = "$$remainingFormatted left",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PocketColors.Primary,
                                    modifier = Modifier.testTag("remaining_amount_text")
                                )
                            } else {
                                val overFormatted = MoneyParser.formatCentsToAmount(abs(remaining))
                                Text(
                                    text = "-$$overFormatted over budget",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PocketColors.Expense,
                                    modifier = Modifier.testTag("remaining_amount_text")
                                )
                            }
                        } else {
                            // Mobile Empty State
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No budget set for ${uiState.monthTitle}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PocketColors.OnSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Set a monthly spending limit to track expenses",
                                    fontSize = 13.sp,
                                    color = PocketColors.OnSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = onSetBudgetClick,
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("set_monthly_budget_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = PocketColors.Primary),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "+ Set Monthly Budget",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PocketColors.OnPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Mobile Bottom Navigation Bar (Fixed at bottom)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 0.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(PocketColors.SurfaceContainerLowest)
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Transactions Tab
                    Column(
                        modifier = Modifier
                            .clickable { onNavigateToTransactions() }
                            .testTag("nav_transactions"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🧾", fontSize = 18.sp)
                        Text(text = "Transactions", fontSize = 11.sp, color = PocketColors.OnSurfaceVariant)
                    }

                    // Budgets Tab (Active)
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PocketColors.SurfaceContainer)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("nav_budgets"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "💵", fontSize = 18.sp)
                        Text(text = "Budgets", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PocketColors.Primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthBadge(status: BudgetHealthStatus?) {
    if (status == null) return
    val (bgColor, textColor, label) = when (status) {
        BudgetHealthStatus.ON_TRACK -> Triple(PocketColors.SurfaceContainer, PocketColors.Primary, "On Track")
        BudgetHealthStatus.NEAR_LIMIT -> Triple(Color(0xFFFFE0B2), Color(0xFFE65100), "⚠ Near Limit")
        BudgetHealthStatus.OVER_BUDGET -> Triple(Color(0xFFFFCDD2), PocketColors.Expense, "Over Budget")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("budget_health_badge"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Preview
@Composable
fun BudgetDashboardContentPreview() {
    BudgetDashboardContent(
        uiState = BudgetDashboardUiState(
            selectedMonth = org.hoods.pocket.domain.model.YearMonth(2026, 9),
            isCurrentMonth = true,
            summary = org.hoods.pocket.domain.model.MonthlyBudgetSummary(
                yearMonth = org.hoods.pocket.domain.model.YearMonth(2026, 9),
                budget = org.hoods.pocket.domain.model.MonthlyBudget(
                    yearMonth = org.hoods.pocket.domain.model.YearMonth(2026, 9),
                    limitAmount = 300000L
                ),
                totalExpense = 258000L,
                remainingAmount = 42000L,
                progress = 0.86f,
                status = BudgetHealthStatus.NEAR_LIMIT
            )
        ),
        onSetBudgetClick = {},
        onNavigateToTransactions = {},
        onPreviousMonthClick = {},
        onNextMonthClick = {},
        onCurrentMonthClick = {}
    )
}

