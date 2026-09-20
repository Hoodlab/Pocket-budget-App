package org.hoods.pocket.presentation.transaction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.presentation.layout.AppLayoutMode
import org.hoods.pocket.presentation.layout.LocalAppLayoutMode
import org.hoods.pocket.presentation.theme.PocketColors

val TransactionCategory.icon: String
    get() = when (this) {
        TransactionCategory.UNCATEGORIZED -> "🏷"
        TransactionCategory.GROCERIES -> "🛒"
        TransactionCategory.DINING -> "🍽"
        TransactionCategory.TRANSPORT -> "🚗"
        TransactionCategory.SHOPPING -> "🛍"
        TransactionCategory.BILLS -> "🧾"
        TransactionCategory.ENTERTAINMENT -> "🎬"
        TransactionCategory.SALARY -> "💵"
        TransactionCategory.FREELANCE -> "💼"
        TransactionCategory.INVESTMENT -> "📈"
        TransactionCategory.GIFT -> "🎁"
        TransactionCategory.OTHER -> "•••"
    }

@Composable
fun CategorySelector(
    selectedType: TransactionType,
    selectedCategory: TransactionCategory,
    onCategorySelected: (TransactionCategory) -> Unit,
    modifier: Modifier = Modifier,
    layoutMode: AppLayoutMode = AppLayoutMode.current()
) {
    val categories = TransactionCategory.categoriesFor(selectedType)
    val columns = if (layoutMode.isExpanded) 4 else 3
    val rows = categories.chunked(columns)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "CATEGORY",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = PocketColors.OnSurfaceVariant,
            letterSpacing = 0.5.sp
        )

        rows.forEach { rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowCategories.forEach { category ->
                    val isSelected = category == selectedCategory
                    val shape = RoundedCornerShape(16.dp)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .clip(shape)
                            .background(
                                if (isSelected) PocketColors.SurfaceContainerHigh
                                else PocketColors.SurfaceContainer
                            )
                            .border(
                                border = if (isSelected) {
                                    BorderStroke(1.5.dp, PocketColors.Primary)
                                } else {
                                    BorderStroke(1.dp, Color.Transparent)
                                },
                                shape = shape
                            )
                            .clickable { onCategorySelected(category) }
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .testTag("category_chip_${category.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = category.icon,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = category.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) PocketColors.Primary else PocketColors.OnSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                val remaining = columns - rowCategories.size
                repeat(remaining) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
