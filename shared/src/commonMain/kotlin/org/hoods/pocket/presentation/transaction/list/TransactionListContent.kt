package org.hoods.pocket.presentation.transaction.list

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.hoods.pocket.domain.model.BudgetHealthStatus
import org.hoods.pocket.domain.model.CashFlowSummary
import org.hoods.pocket.domain.model.CategoryBudgetSummary
import org.hoods.pocket.domain.model.MonthlyBudgetSummary
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.util.MoneyParser
import org.hoods.pocket.presentation.layout.AppLayoutMode
import org.hoods.pocket.presentation.layout.LocalAppLayoutMode
import org.hoods.pocket.presentation.theme.PocketColors
import org.hoods.pocket.presentation.transaction.icon
import kotlin.math.abs
import kotlin.time.Instant

@Composable
fun TransactionListContent(
    uiState: TransactionListUiState,
    onAddTransactionClick: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onPreviousMonthClick: () -> Unit = {},
    onNextMonthClick: () -> Unit = {},
    onCurrentMonthClick: () -> Unit = {},
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
                    .padding(24.dp)
            ) {
                // Top Month Header & Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = uiState.selectedMonth.format(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = PocketColors.Primary,
                            modifier = Modifier.testTag("selected_month_text")
                        )

                        // Navigation arrows
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(PocketColors.SurfaceContainer)
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onPreviousMonthClick() }
                                    .testTag("previous_month_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "‹",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PocketColors.OnSurface
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onNextMonthClick() }
                                    .testTag("next_month_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "›",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PocketColors.OnSurface
                                )
                            }
                        }

                        if (!uiState.isCurrentMonth) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(PocketColors.Primary)
                                    .clickable { onCurrentMonthClick() }
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .testTag("current_month_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Today",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PocketColors.OnPrimary
                                )
                            }
                        }
                    }

                    // + Add Transaction Action Button on Desktop Header
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(PocketColors.Primary)
                            .clickable { onAddTransactionClick() }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("add_transaction_fab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Add Transaction",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PocketColors.OnPrimary
                        )
                    }
                }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Desktop Dual-Column Split: Overview & Transactions (Left) + Budget Health (Right)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Left / Center Column: Cashflow Overview + Recent Transactions
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Cashflow Overview Card
                            CashflowOverviewCardDesktop(
                                cashFlow = uiState.cashFlow ?: CashFlowSummary(uiState.selectedMonth, 0L, 0L, 0L)
                            )

                            // Structured White Container Card: "Recent Transactions"
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .shadow(
                                        elevation = 2.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        spotColor = PocketColors.OnSurface.copy(alpha = 0.05f)
                                    )
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(PocketColors.SurfaceContainerLowest)
                                    .padding(24.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Card Header: Section Title + Inline New Transaction Action
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Recent Transactions",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PocketColors.OnSurface
                                        )

                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(PocketColors.Primary)
                                                .clickable { onAddTransactionClick() }
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                                .testTag("new_transaction_button"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "+ New Transaction",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = PocketColors.OnPrimary,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = PocketColors.OutlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)

                                    if (uiState.transactions.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val emptyMessage = if (uiState.isCurrentMonth) {
                                                "No transactions yet\nTap + to add one"
                                            } else {
                                                "No transactions in ${uiState.selectedMonth.format()}\nTap + to add one"
                                            }
                                            Text(
                                                text = emptyMessage,
                                                fontSize = 15.sp,
                                                color = PocketColors.OnSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                        ) {
                                            itemsIndexed(
                                                items = uiState.transactions,
                                                key = { _, item -> item.id }
                                            ) { index, transaction ->
                                                TransactionItem(
                                                    transaction = transaction,
                                                    onClick = { onTransactionClick(transaction.id) }
                                                )
                                                if (index < uiState.transactions.lastIndex) {
                                                    HorizontalDivider(
                                                        color = PocketColors.OutlineVariant.copy(alpha = 0.2f),
                                                        thickness = 0.5.dp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Right Column: Budget Health & Category Budgets (read-only)
                        BudgetHealthCardDesktop(
                            budgetSummary = uiState.budgetSummary,
                            categorySummaries = uiState.categorySummaries,
                            selectedMonth = uiState.selectedMonth,
                            modifier = Modifier
                                .width(280.dp)
                                .fillMaxHeight()
                        )
                    }
                }
        } else {
            // ==========================================
            // Mobile Layout (Handheld Portrait)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = PocketColors.OnSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Month Navigation Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PocketColors.SurfaceContainer)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Month Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPreviousMonthClick() }
                            .testTag("previous_month_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "‹",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PocketColors.OnSurface
                        )
                    }

                    // Month Display & "Today" Action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = uiState.selectedMonth.format(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PocketColors.OnSurface,
                            modifier = Modifier.testTag("selected_month_text")
                        )

                        if (!uiState.isCurrentMonth) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(PocketColors.Primary)
                                    .clickable { onCurrentMonthClick() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .testTag("current_month_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Today",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PocketColors.OnPrimary
                                )
                            }
                        }
                    }

                    // Next Month Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onNextMonthClick() }
                            .testTag("next_month_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "›",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PocketColors.OnSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Overview & Transactions List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Item 1: Hero Cashflow Card (CashFlowOverviewMobile)
                    item {
                        val cashFlow = uiState.cashFlow ?: CashFlowSummary(uiState.selectedMonth, 0L, 0L, 0L)
                        CashFlowOverviewMobile(cashFlow = cashFlow)
                    }

                    // Item 2: Read-Only Budget Health Card
                    item {
                        BudgetHealthReadOnlyCardMobile(
                            budgetSummary = uiState.budgetSummary,
                            categorySummaries = uiState.categorySummaries,
                            selectedMonth = uiState.selectedMonth
                        )
                    }

                    // Item 3: Recent Transactions Header
                    item {
                        Text(
                            text = "Recent Transactions",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PocketColors.OnSurface,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Items 4+: Transactions or Empty State
                    if (uiState.transactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val emptyMessage = if (uiState.isCurrentMonth) {
                                    "No transactions yet\nTap + to add one"
                                } else {
                                    "No transactions in ${uiState.selectedMonth.format()}\nTap + to add one"
                                }
                                Text(
                                    text = emptyMessage,
                                    fontSize = 15.sp,
                                    color = PocketColors.OnSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = uiState.transactions,
                            key = { _, item -> item.id }
                        ) { index, transaction ->
                            TransactionItem(
                                transaction = transaction,
                                onClick = { onTransactionClick(transaction.id) }
                            )
                            if (index < uiState.transactions.lastIndex) {
                                HorizontalDivider(
                                    color = PocketColors.OutlineVariant.copy(alpha = 0.3f),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            }

            // Mobile FAB
            FloatingActionButton(
                onClick = onAddTransactionClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 24.dp)
                    .testTag("add_transaction_fab"),
                containerColor = PocketColors.Primary,
                contentColor = PocketColors.OnPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "+",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CashflowOverviewCardDesktop(
    cashFlow: CashFlowSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cash_flow_overview"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PocketColors.SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Cashflow Overview",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = PocketColors.OnSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // NET BALANCE
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PocketColors.SurfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "NET BALANCE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PocketColors.OnSurfaceVariant,
                            letterSpacing = 0.04.em,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val netFormatted = if (cashFlow.netBalance >= 0) {
                            "$" + MoneyParser.formatCentsToAmount(cashFlow.netBalance)
                        } else {
                            "-$" + MoneyParser.formatCentsToAmount(abs(cashFlow.netBalance))
                        }
                        Text(
                            text = netFormatted,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cashFlow.netBalance < 0) PocketColors.Expense else PocketColors.OnSurface,
                            modifier = Modifier.testTag("net_balance_amount"),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // MONTHLY INCOME
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PocketColors.SurfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "MONTHLY INCOME",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PocketColors.OnSurfaceVariant,
                            letterSpacing = 0.04.em,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "+$" + MoneyParser.formatCentsToAmount(cashFlow.income),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PocketColors.Income,
                            modifier = Modifier.testTag("income_amount"),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // MONTHLY SPENT
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PocketColors.SurfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "MONTHLY SPENT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PocketColors.OnSurfaceVariant,
                            letterSpacing = 0.04.em,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "-$" + MoneyParser.formatCentsToAmount(cashFlow.expense),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PocketColors.Expense,
                            modifier = Modifier.testTag("spent_amount"),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetHealthCardDesktop(
    budgetSummary: MonthlyBudgetSummary?,
    categorySummaries: List<CategoryBudgetSummary>,
    selectedMonth: YearMonth,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("budget_health_overview"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PocketColors.SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budget Health",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PocketColors.OnSurface
                )
                val status = budgetSummary?.status
                if (status != null && budgetSummary.budget != null) {
                    val (badgeText, badgeBg, badgeFg) = when (status) {
                        BudgetHealthStatus.ON_TRACK -> Triple("Healthy", PocketColors.Income.copy(alpha = 0.15f), PocketColors.Income)
                        BudgetHealthStatus.NEAR_LIMIT -> Triple("Near Limit", Color(0xFFFFE0B2), Color(0xFFE65100))
                        BudgetHealthStatus.OVER_BUDGET -> Triple("Over Budget", PocketColors.Expense.copy(alpha = 0.15f), PocketColors.Expense)
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(badgeBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeFg
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (budgetSummary?.budget != null) {
                val budget = budgetSummary.budget
                val totalSpent = budgetSummary.totalExpense
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Used",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = PocketColors.OnSurfaceVariant
                    )
                    Text(
                        text = "$" + MoneyParser.formatCentsToAmount(totalSpent) + " / $" + MoneyParser.formatCentsToAmount(budget.limitAmount),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PocketColors.OnSurface,
                        modifier = Modifier.testTag("total_budget_spent_amount")
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                val progress = budgetSummary.progress.coerceIn(0f, 1f)
                val progressColor = when (budgetSummary.status) {
                    BudgetHealthStatus.ON_TRACK -> PocketColors.Primary
                    BudgetHealthStatus.NEAR_LIMIT -> Color(0xFFE65100)
                    BudgetHealthStatus.OVER_BUDGET -> PocketColors.Expense
                    null -> PocketColors.Primary
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = progressColor,
                    trackColor = PocketColors.SurfaceContainer
                )
            } else {
                Text(
                    text = "No budget set for ${selectedMonth.format()}",
                    fontSize = 13.sp,
                    color = PocketColors.OnSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = PocketColors.OutlineVariant.copy(alpha = 0.2f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Category Budgets",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = PocketColors.OnSurface,
                modifier = Modifier.testTag("category_budgets_overview")
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (categorySummaries.isEmpty()) {
                Text(
                    text = "No category budgets set",
                    fontSize = 13.sp,
                    color = PocketColors.OnSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    categorySummaries.forEach { catSummary ->
                        CategoryReadOnlyRow(summary = catSummary)
                    }
                }
            }
        }
    }
}

@Composable
private fun CashFlowOverviewMobile(
    cashFlow: CashFlowSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cash_flow_overview"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PocketColors.SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Net Balance",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = PocketColors.OnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            val netFormatted = if (cashFlow.netBalance >= 0) {
                "$" + MoneyParser.formatCentsToAmount(cashFlow.netBalance)
            } else {
                "-$" + MoneyParser.formatCentsToAmount(abs(cashFlow.netBalance))
            }
            Text(
                text = netFormatted,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (cashFlow.netBalance < 0) PocketColors.Expense else PocketColors.OnSurface,
                modifier = Modifier.testTag("net_balance_amount")
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(
                color = PocketColors.OutlineVariant.copy(alpha = 0.25f),
                thickness = 0.5.dp
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Income
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PocketColors.Income)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Income",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = PocketColors.OnSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val incomeFormatted = "+$" + MoneyParser.formatCentsToAmount(cashFlow.income)
                    Text(
                        text = incomeFormatted,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PocketColors.Income,
                        modifier = Modifier.testTag("income_amount")
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(PocketColors.OutlineVariant.copy(alpha = 0.25f))
                )

                // Expenses / Spent
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PocketColors.Expense)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Spent",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = PocketColors.OnSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val spentFormatted = "-$" + MoneyParser.formatCentsToAmount(cashFlow.expense)
                    Text(
                        text = spentFormatted,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PocketColors.Expense,
                        modifier = Modifier.testTag("spent_amount")
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetHealthReadOnlyCardMobile(
    budgetSummary: MonthlyBudgetSummary?,
    categorySummaries: List<CategoryBudgetSummary>,
    selectedMonth: YearMonth,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("budget_health_overview"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PocketColors.SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budget Health",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PocketColors.OnSurface
                )
                val status = budgetSummary?.status
                if (status != null && budgetSummary.budget != null) {
                    val (badgeText, badgeBg, badgeFg) = when (status) {
                        BudgetHealthStatus.ON_TRACK -> Triple("Healthy", PocketColors.Income.copy(alpha = 0.15f), PocketColors.Income)
                        BudgetHealthStatus.NEAR_LIMIT -> Triple("Near Limit", Color(0xFFFFE0B2), Color(0xFFE65100))
                        BudgetHealthStatus.OVER_BUDGET -> Triple("Over Budget", PocketColors.Expense.copy(alpha = 0.15f), PocketColors.Expense)
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeFg
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (budgetSummary?.budget != null) {
                val budget = budgetSummary.budget
                val totalSpent = budgetSummary.totalExpense
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Used",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = PocketColors.OnSurfaceVariant
                    )
                    Text(
                        text = "$" + MoneyParser.formatCentsToAmount(totalSpent) + " / $" + MoneyParser.formatCentsToAmount(budget.limitAmount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PocketColors.OnSurface,
                        modifier = Modifier.testTag("total_budget_spent_amount")
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                val progress = budgetSummary.progress.coerceIn(0f, 1f)
                val progressColor = when (budgetSummary.status) {
                    BudgetHealthStatus.ON_TRACK -> PocketColors.Primary
                    BudgetHealthStatus.NEAR_LIMIT -> Color(0xFFE65100)
                    BudgetHealthStatus.OVER_BUDGET -> PocketColors.Expense
                    null -> PocketColors.Primary
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = progressColor,
                    trackColor = PocketColors.SurfaceContainer
                )
            } else {
                Text(
                    text = "No budget set for ${selectedMonth.format()}",
                    fontSize = 12.sp,
                    color = PocketColors.OnSurfaceVariant
                )
            }

            if (categorySummaries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = PocketColors.OutlineVariant.copy(alpha = 0.2f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Category Budgets",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PocketColors.OnSurface,
                    modifier = Modifier.testTag("category_budgets_overview")
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    categorySummaries.forEach { catSummary ->
                        CategoryReadOnlyRow(summary = catSummary)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryReadOnlyRow(
    summary: CategoryBudgetSummary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_budget_card_${summary.category.name}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = summary.category.icon, fontSize = 15.sp)
                Text(
                    text = summary.category.displayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = PocketColors.OnSurface
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$" + MoneyParser.formatCentsToAmount(summary.spentAmount) + " / $" + MoneyParser.formatCentsToAmount(summary.limitAmount),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PocketColors.OnSurface
                )
                if (summary.status == BudgetHealthStatus.OVER_BUDGET) {
                    Text(
                        text = "Over budget!",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = PocketColors.Expense
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        val catProgress = summary.progress.coerceIn(0f, 1f)
        val catColor = when (summary.status) {
            BudgetHealthStatus.ON_TRACK -> PocketColors.Primary
            BudgetHealthStatus.NEAR_LIMIT -> Color(0xFFE65100)
            BudgetHealthStatus.OVER_BUDGET -> PocketColors.Expense
        }
        LinearProgressIndicator(
            progress = { catProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = catColor,
            trackColor = PocketColors.SurfaceContainer
        )
    }
}

@Preview
@Composable
fun TransactionListContentPreview() {
    val month = YearMonth(2026, 9)
    TransactionListContent(
        uiState = TransactionListUiState(
            selectedMonth = month,
            isCurrentMonth = true,
            cashFlow = CashFlowSummary(
                yearMonth = month,
                income = 450000L,
                expense = 258000L
            ),
            budgetSummary = MonthlyBudgetSummary(
                yearMonth = month,
                budget = org.hoods.pocket.domain.model.MonthlyBudget(month, 300000L),
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
                )
            ),
            transactions = listOf(
                Transaction(
                    id = "1",
                    description = "Groceries",
                    amount = 4550L,
                    type = TransactionType.EXPENSE,
                    date = Instant.parse("2026-09-09T10:00:00Z"),
                    category = org.hoods.pocket.domain.model.TransactionCategory.GROCERIES
                ),
                Transaction(
                    id = "2",
                    description = "Salary",
                    amount = 300000L,
                    type = TransactionType.INCOME,
                    date = Instant.parse("2026-09-01T09:00:00Z"),
                    category = org.hoods.pocket.domain.model.TransactionCategory.SALARY
                )
            )
        ),
        onAddTransactionClick = {},
        onTransactionClick = {},
        onPreviousMonthClick = {},
        onNextMonthClick = {},
        onCurrentMonthClick = {}
    )
}
