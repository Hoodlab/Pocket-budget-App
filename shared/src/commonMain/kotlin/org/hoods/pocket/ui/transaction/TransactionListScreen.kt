package org.hoods.pocket.ui.transaction

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.ui.theme.PocketColors

@Composable
fun TransactionListScreen(
    transactions: List<Transaction>,
    onAddTransactionClick: () -> Unit,
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
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Transactions",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = PocketColors.OnSurface
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (transactions.isEmpty()) {
                // Empty State Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PocketColors.SurfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(PocketColors.SurfaceContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "💳", fontSize = 24.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No transactions yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PocketColors.OnSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Tap the + button to record your first transaction.",
                            fontSize = 14.sp,
                            color = PocketColors.OnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onAddTransactionClick,
                            shape = RoundedCornerShape(percent = 50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PocketColors.Primary,
                                contentColor = PocketColors.OnPrimary
                            )
                        ) {
                            Text(
                                text = "Add Transaction",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                // Transaction List Container
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PocketColors.SurfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(
                            items = transactions,
                            key = { _, tx -> tx.id }
                        ) { index, transaction ->
                            TransactionItem(transaction = transaction)
                            if (index < transactions.lastIndex) {
                                HorizontalDivider(
                                    color = PocketColors.SurfaceContainerHigh.copy(alpha = 0.5f),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddTransactionClick,
            shape = CircleShape,
            containerColor = PocketColors.Primary,
            contentColor = PocketColors.OnPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(56.dp)
        ) {
            Text(
                text = "+",
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                color = PocketColors.OnPrimary
            )
        }
    }
}
