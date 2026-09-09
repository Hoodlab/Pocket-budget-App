package org.hoods.pocket.presentation.transaction.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.presentation.theme.PocketColors
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import kotlin.time.Instant

@Composable
fun TransactionListContent(
    uiState: TransactionListUiState,
    onAddTransactionClick: () -> Unit,
    onTransactionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PocketColors.Surface)
    ) {
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

            Spacer(modifier = Modifier.height(20.dp))

            if (uiState.transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions yet\nTap + to add one",
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

@Preview
@Composable
fun TransactionListContentPreview() {
    TransactionListContent(
        uiState = TransactionListUiState(
            transactions = listOf(
                Transaction(
                    id = "1",
                    description = "Groceries",
                    amount = 4550L,
                    type = TransactionType.EXPENSE,
                    date = Instant.fromEpochMilliseconds(1717171717000L)
                ),
                Transaction(
                    id = "2",
                    description = "Salary",
                    amount = 300000L,
                    type = TransactionType.INCOME,
                    date = Instant.fromEpochMilliseconds(1717171717000L)
                )
            )
        ),
        onAddTransactionClick = {},
        onTransactionClick = {}
    )
}
