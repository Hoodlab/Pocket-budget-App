package org.hoods.pocket.presentation.transaction.list

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.presentation.theme.PocketColors
import kotlin.time.Instant

@Composable
fun TransactionListContent(
    uiState: TransactionListUiState,
    onAddTransactionClick: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onPreviousMonthClick: () -> Unit = {},
    onNextMonthClick: () -> Unit = {},
    onCurrentMonthClick: () -> Unit = {},
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
            // Desktop Layout (Sidebar + Container Card)
            // ==========================================
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Navigation Rail
                Column(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .background(PocketColors.Surface)
                        .padding(horizontal = 24.dp, vertical = 32.dp)
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

                    // Primary Action: + Add Transaction
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(PocketColors.Primary)
                            .clickable { onAddTransactionClick() }
                            .testTag("add_transaction_fab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Add Transaction",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PocketColors.OnPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Active Navigation Item: Transactions
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PocketColors.SurfaceContainer)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Transactions",
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
                        .padding(32.dp)
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
                    }

                    Spacer(modifier = Modifier.height(24.dp))

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
                                        color = PocketColors.OnPrimary
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
            }
        } else {
            // ==========================================
            // Mobile Layout (Handheld Portrait)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
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
                            fontSize = 16.sp,
                            color = PocketColors.OnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    .padding(24.dp)
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

@Preview
@Composable
fun TransactionListContentPreview() {
    TransactionListContent(
        uiState = TransactionListUiState(
            selectedMonth = YearMonth(2026, 9),
            isCurrentMonth = true,
            transactions = listOf(
                Transaction(
                    id = "1",
                    description = "Groceries",
                    amount = 4550L,
                    type = TransactionType.EXPENSE,
                    date = Instant.parse("2026-09-09T10:00:00Z")
                ),
                Transaction(
                    id = "2",
                    description = "Salary",
                    amount = 300000L,
                    type = TransactionType.INCOME,
                    date = Instant.parse("2026-09-01T09:00:00Z")
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
