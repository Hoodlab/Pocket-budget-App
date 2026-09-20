package org.hoods.pocket.presentation.budget.dashboard

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hoods.pocket.domain.model.BudgetHealthStatus
import org.hoods.pocket.domain.model.CategoryBudgetSummary
import org.hoods.pocket.domain.util.MoneyParser
import org.hoods.pocket.presentation.theme.PocketColors
import org.hoods.pocket.presentation.transaction.icon
import androidx.compose.ui.unit.em
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.abs
import kotlin.math.roundToInt

import org.hoods.pocket.presentation.layout.AppLayoutMode
import org.hoods.pocket.presentation.layout.LocalAppLayoutMode

@Composable
fun BudgetDashboardContent(
    uiState: BudgetDashboardUiState,
    onSetBudgetClick: () -> Unit,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
    onCurrentMonthClick: () -> Unit,
    layoutMode: AppLayoutMode = AppLayoutMode.current(),
    modifier: Modifier = Modifier
) {
    val isExpanded = layoutMode.isExpanded

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PocketColors.Surface)
    ) {
        if (isExpanded) {
            // ==========================================
            // Desktop Layout (Main Content Panel)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp)
            ) {
                // Top Header: Title + Month Navigation + Action
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

                    // Month Navigation Control + Action Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
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

                        // Primary Action: + Set Monthly Budget
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(PocketColors.Primary)
                                .clickable { onSetBudgetClick() }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .testTag("sidebar_set_budget_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ Set Budget",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PocketColors.OnPrimary
                            )
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        HealthBadge(status = summary.status)
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(PocketColors.SurfaceContainer)
                                                .clickable { onSetBudgetClick() }
                                                .testTag("edit_budget_button"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "✎",
                                                color = PocketColors.OnSurfaceVariant,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
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
                                        color = PocketColors.OnSurface,
                                        modifier = Modifier.testTag("total_budget_spent_amount")
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
                                    val percentUsed = (summary.progress * 100).roundToInt()
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

                    if (uiState.categorySummaries.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(36.dp))

                        Text(
                            text = "Category Budgets",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PocketColors.OnSurface,
                            modifier = Modifier.testTag("category_budgets_title")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Monitor your monthly spending limits.",
                            fontSize = 14.sp,
                            color = PocketColors.OnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        val columns = 3
                        val categoryRows = uiState.categorySummaries.chunked(columns)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 1000.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            categoryRows.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    rowItems.forEach { catSummary ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            CategoryBudgetDesktopCard(catSummary)
                                        }
                                    }
                                    repeat(columns - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HealthBadge(status = summary.status)
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(PocketColors.SurfaceContainer)
                                            .clickable { onSetBudgetClick() }
                                            .testTag("edit_budget_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "✎",
                                            color = PocketColors.OnSurfaceVariant,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            val spentFormatted = MoneyParser.formatCentsToAmount(summary.totalExpense)
                            val limitFormatted = MoneyParser.formatCentsToAmount(summary.budget?.limitAmount ?: 0L)

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$$spentFormatted",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PocketColors.OnSurface,
                                    modifier = Modifier.testTag("total_budget_spent_amount")
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
                                val percentUsed = (summary.progress * 100).roundToInt()
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

                if (uiState.categorySummaries.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "Category Budgets",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PocketColors.OnSurface,
                        modifier = Modifier.testTag("category_budgets_title")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        uiState.categorySummaries.forEach { catSummary ->
                            CategoryBudgetMobileCard(catSummary)
                        }
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

@Composable
private fun CategoryHealthBadge(
    status: BudgetHealthStatus,
    tag: String? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp,
    horizontalPadding: androidx.compose.ui.unit.Dp = 10.dp,
    verticalPadding: androidx.compose.ui.unit.Dp = 3.dp
) {
    val (bgColor, textColor, label) = when (status) {
        BudgetHealthStatus.ON_TRACK -> Triple(PocketColors.SurfaceContainer, PocketColors.Primary, "On Track")
        BudgetHealthStatus.NEAR_LIMIT -> Triple(Color(0xFFFFE0B2), Color(0xFFE65100), "Near Limit")
        BudgetHealthStatus.OVER_BUDGET -> Triple(Color(0xFFFFCDD2), PocketColors.Expense, "Over Budget")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(bgColor)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
private fun CategoryBudgetMobileCard(
    catSummary: CategoryBudgetSummary,
    modifier: Modifier = Modifier
) {
    val isOver = catSummary.status == BudgetHealthStatus.OVER_BUDGET
    val isNear = catSummary.status == BudgetHealthStatus.NEAR_LIMIT

    val avatarBg = when {
        isOver -> Color(0xFFFFEBEE)
        isNear -> Color(0xFFFFF3E0)
        else -> PocketColors.SurfaceContainerLow
    }

    val progressColor = when {
        isOver -> PocketColors.Expense
        isNear -> Color(0xFFE65100)
        else -> PocketColors.Primary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_budget_card_${catSummary.category.name}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PocketColors.SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = if (isOver) BorderStroke(1.dp, PocketColors.Expense.copy(alpha = 0.35f)) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Avatar + Name & Subtitle + Badge (if near or over)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = catSummary.category.icon, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = catSummary.category.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PocketColors.OnSurface,
                        modifier = Modifier.testTag("category_name_${catSummary.category.name}")
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val spent = MoneyParser.formatCentsToAmount(catSummary.spentAmount)
                    val limit = MoneyParser.formatCentsToAmount(catSummary.limitAmount)
                    Text(
                        text = "$$spent of $$limit",
                        fontSize = 13.sp,
                        color = PocketColors.OnSurfaceVariant,
                        modifier = Modifier.testTag("category_amounts_${catSummary.category.name}")
                    )
                }

                CategoryHealthBadge(
                    status = catSummary.status,
                    tag = "category_health_badge_${catSummary.category.name}"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { catSummary.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .testTag("category_progress_${catSummary.category.name}"),
                color = progressColor,
                trackColor = PocketColors.SurfaceContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Sub-Row: % Used + Left / Over
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val percentUsed = (catSummary.progress * 100).roundToInt()
                Text(
                    text = "$percentUsed% Used",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isOver) PocketColors.Expense else PocketColors.OnSurfaceVariant
                )

                if (catSummary.remainingAmount >= 0) {
                    val rem = MoneyParser.formatCentsToAmount(catSummary.remainingAmount)
                    Text(
                        text = "$$rem left",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PocketColors.Primary,
                        modifier = Modifier.testTag("category_remaining_${catSummary.category.name}")
                    )
                } else {
                    val over = MoneyParser.formatCentsToAmount(abs(catSummary.remainingAmount))
                    Text(
                        text = "-$$over over budget",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PocketColors.Expense,
                        modifier = Modifier.testTag("category_remaining_${catSummary.category.name}")
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBudgetDesktopCard(
    catSummary: CategoryBudgetSummary,
    modifier: Modifier = Modifier
) {
    val isOver = catSummary.status == BudgetHealthStatus.OVER_BUDGET
    val isNear = catSummary.status == BudgetHealthStatus.NEAR_LIMIT

    val avatarBg = when {
        isOver -> Color(0xFFFFEBEE)
        isNear -> Color(0xFFFFF3E0)
        else -> PocketColors.SurfaceContainerLow
    }

    val progressColor = when {
        isOver -> PocketColors.Expense
        isNear -> Color(0xFFE65100)
        else -> PocketColors.Primary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_budget_card_${catSummary.category.name}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PocketColors.SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isOver) BorderStroke(1.dp, PocketColors.Expense.copy(alpha = 0.35f)) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Avatar + Category Name + Health Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = catSummary.category.icon, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = catSummary.category.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PocketColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("category_name_${catSummary.category.name}")
                )

                Spacer(modifier = Modifier.width(6.dp))

                CategoryHealthBadge(
                    status = catSummary.status,
                    tag = "category_health_badge_${catSummary.category.name}",
                    fontSize = 10.sp,
                    horizontalPadding = 8.dp,
                    verticalPadding = 2.dp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Large spent amount of limit
            val spent = MoneyParser.formatCentsToAmount(catSummary.spentAmount)
            val limit = MoneyParser.formatCentsToAmount(catSummary.limitAmount)

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.testTag("category_amounts_${catSummary.category.name}")
            ) {
                Text(
                    text = "$$spent",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PocketColors.OnSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "of $$limit",
                    fontSize = 13.sp,
                    color = PocketColors.OnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub row: Remaining / Over by + % Used
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (catSummary.remainingAmount >= 0) {
                    val rem = MoneyParser.formatCentsToAmount(catSummary.remainingAmount)
                    Text(
                        text = "Remaining: $$rem",
                        fontSize = 12.sp,
                        color = PocketColors.OnSurfaceVariant,
                        modifier = Modifier.testTag("category_remaining_${catSummary.category.name}")
                    )
                } else {
                    val over = MoneyParser.formatCentsToAmount(abs(catSummary.remainingAmount))
                    Text(
                        text = "Over by $$over",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PocketColors.Expense,
                        modifier = Modifier.testTag("category_remaining_${catSummary.category.name}")
                    )
                }

                val percentUsed = (catSummary.progress * 100).roundToInt()
                Text(
                    text = "$percentUsed% Used",
                    fontSize = 12.sp,
                    color = PocketColors.OnSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { catSummary.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .testTag("category_progress_${catSummary.category.name}"),
                color = progressColor,
                trackColor = PocketColors.SurfaceContainer
            )
        }
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
            ),
            categorySummaries = listOf(
                CategoryBudgetSummary(
                    category = org.hoods.pocket.domain.model.TransactionCategory.GROCERIES,
                    limitAmount = 60000L,
                    spentAmount = 45000L,
                    remainingAmount = 15000L,
                    progress = 0.75f,
                    status = BudgetHealthStatus.ON_TRACK
                ),
                CategoryBudgetSummary(
                    category = org.hoods.pocket.domain.model.TransactionCategory.TRANSPORT,
                    limitAmount = 20000L,
                    spentAmount = 12000L,
                    remainingAmount = 8000L,
                    progress = 0.60f,
                    status = BudgetHealthStatus.ON_TRACK
                ),
                CategoryBudgetSummary(
                    category = org.hoods.pocket.domain.model.TransactionCategory.ENTERTAINMENT,
                    limitAmount = 30000L,
                    spentAmount = 35000L,
                    remainingAmount = -5000L,
                    progress = 1.16f,
                    status = BudgetHealthStatus.OVER_BUDGET
                )
            )
        ),
        onSetBudgetClick = {},
        onPreviousMonthClick = {},
        onNextMonthClick = {},
        onCurrentMonthClick = {}
    )
}

