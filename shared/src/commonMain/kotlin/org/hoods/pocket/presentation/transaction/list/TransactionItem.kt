package org.hoods.pocket.presentation.transaction.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.util.DateUtils
import org.hoods.pocket.domain.util.MoneyParser
import org.hoods.pocket.presentation.theme.PocketColors
import org.hoods.pocket.presentation.transaction.icon

@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val formattedAmount = MoneyParser.formatSignedAmount(transaction.amount, transaction.type)
    val dateString = DateUtils.formatShortDate(transaction.date)
    val typeLabel = if (isIncome) "Income" else "Expense"

    val baseModifier = modifier.testTag("transaction_item_${transaction.id}")
    val rowModifier = if (onClick != null) {
        baseModifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp)
    } else {
        baseModifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 16.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading 40.dp Type Badge
        val badgeBg = if (isIncome) PocketColors.SurfaceContainer else PocketColors.SurfaceContainerHigh
        val iconColor = if (isIncome) PocketColors.Income else PocketColors.OnSurfaceVariant
        val iconText = if (transaction.category != TransactionCategory.UNCATEGORIZED) {
            transaction.category.icon
        } else {
            if (isIncome) "↑" else "↓"
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(badgeBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iconText,
                color = iconColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Center Content: Description and Category/Date
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = transaction.description,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = PocketColors.OnSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${transaction.category.displayName} • $dateString",
                fontSize = 13.sp,
                color = PocketColors.OnSurfaceVariant,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Trailing Amount
        val amountColor = if (isIncome) PocketColors.Income else PocketColors.OnSurface
        Text(
            text = formattedAmount,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}
