package org.hoods.pocket.presentation.budget.set

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.util.MoneyParser
import org.hoods.pocket.presentation.theme.PocketColors
import org.hoods.pocket.presentation.transaction.icon

private val TransactionCategory.budgetDisplayName: String
    get() = when (this) {
        TransactionCategory.DINING -> "Dining Out"
        TransactionCategory.BILLS -> "Bills & Utilities"
        else -> displayName
    }

@Composable
fun SetBudgetContent(
    uiState: SetBudgetUiState,
    onAmountChanged: (String) -> Unit,
    onCategoryAllocationChanged: (TransactionCategory, String) -> Unit = { _, _ -> },
    onRemoveCategoryAllocation: (TransactionCategory) -> Unit = {},
    onResetAllocations: () -> Unit = {},
    onSaveClick: () -> Unit,
    onRequestDelete: () -> Unit = {},
    onDismissDelete: () -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val expenseCategories = remember {
        TransactionCategory.entries.filter { it.applicableType == TransactionType.EXPENSE }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(PocketColors.Surface)
    ) {
        val isDesktop = maxWidth >= 720.dp

        if (isDesktop) {
            // ==========================================
            // Desktop Layout (Centered Card Container)
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
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
                        // Card Header: Back arrow + Title + Actions (Reset / Delete)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (onDismiss != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable { onDismiss() }
                                            .testTag("back_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "←",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PocketColors.OnSurface
                                        )
                                    }
                                }
                                Text(
                                    text = if (uiState.hasExistingBudget) "Edit ${uiState.monthTitle} Budget" else "Set ${uiState.monthTitle} Budget",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PocketColors.OnSurface
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = onResetAllocations,
                                    modifier = Modifier.testTag("reset_allocations_button")
                                ) {
                                    Text(
                                        text = "Reset",
                                        color = PocketColors.Primary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                if (uiState.hasExistingBudget) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(PocketColors.SurfaceContainer)
                                            .clickable { onRequestDelete() }
                                            .testTag("delete_budget_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "🗑",
                                            color = PocketColors.Expense,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = PocketColors.OutlineVariant.copy(alpha = 0.25f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(24.dp))

                        // Total Monthly Spending Limit Box
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(PocketColors.SurfaceContainer)
                                .padding(24.dp)
                        ) {
                            Text(
                                text = "Total Monthly Spending Limit",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = PocketColors.OnSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = uiState.amountText,
                                onValueChange = onAmountChanged,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("budget_amount_input"),
                                textStyle = TextStyle(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PocketColors.OnSurface
                                ),
                                placeholder = {
                                    Text(
                                        text = "0.00",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PocketColors.OnSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                },
                                prefix = {
                                    Text(
                                        text = "$ ",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PocketColors.Primary
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = PocketColors.SurfaceContainerLowest,
                                    unfocusedContainerColor = PocketColors.SurfaceContainerLowest,
                                    focusedBorderColor = PocketColors.Primary,
                                    unfocusedBorderColor = PocketColors.OutlineVariant
                                )
                            )

                            // Remaining / Over-allocated pill
                            val remainingText = when {
                                uiState.isOverAllocated -> "Over allocated by $${MoneyParser.formatCentsToAmount(uiState.overAllocatedCents)}"
                                uiState.remainingCents != null -> "Remaining to Allocate: $${MoneyParser.formatCentsToAmount(uiState.remainingCents ?: 0L)}"
                                else -> null
                            }

                            if (remainingText != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (uiState.isOverAllocated) PocketColors.ErrorContainer
                                            else PocketColors.SurfaceContainerLow
                                        )
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                        .testTag("remaining_allocation_card"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = remainingText,
                                        color = if (uiState.isOverAllocated) PocketColors.Expense else PocketColors.Primary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.testTag("remaining_allocation_text")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Category Allocations Section Header
                        Text(
                            text = "Category Allocations",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PocketColors.OnSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Desktop 2-column grid of category allocation cards
                        val categoryPairs = expenseCategories.chunked(2)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            categoryPairs.forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    pair.forEach { category ->
                                        CategoryAllocationCard(
                                            category = category,
                                            amountText = uiState.categoryAllocations[category] ?: "",
                                            onAmountChanged = { onCategoryAllocationChanged(category, it) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        if (uiState.errorMessage != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = uiState.errorMessage,
                                color = PocketColors.Expense,
                                fontSize = 13.sp,
                                modifier = Modifier.testTag("error_message_text")
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Actions Row: Cancel & Save Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onDismiss != null) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("cancel_button"),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = PocketColors.OnSurfaceVariant
                                    )
                                ) {
                                    Text(text = "Cancel", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Button(
                                onClick = onSaveClick,
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp)
                                    .testTag("save_budget_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = PocketColors.Primary),
                                shape = CircleShape,
                                enabled = !uiState.isSaving && !uiState.isDeleting
                            ) {
                                Text(
                                    text = if (uiState.isSaving) {
                                        "Saving..."
                                    } else if (uiState.hasExistingBudget) {
                                        "Save Changes"
                                    } else {
                                        "Save & Apply Budget"
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PocketColors.OnPrimary
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // Mobile Layout (Handheld Form)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header with back arrow, Title, and Reset action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (onDismiss != null) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable { onDismiss() }
                                    .testTag("back_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "←",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PocketColors.OnSurface
                                )
                            }
                        }
                        Text(
                            text = if (uiState.hasExistingBudget) "Edit ${uiState.monthTitle} Budget" else "Set ${uiState.monthTitle} Budget",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PocketColors.OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            onClick = onResetAllocations,
                            modifier = Modifier.testTag("reset_allocations_button")
                        ) {
                            Text(
                                text = "Reset",
                                color = PocketColors.Primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        if (uiState.hasExistingBudget) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PocketColors.SurfaceContainer)
                                    .clickable { onRequestDelete() }
                                    .testTag("delete_budget_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🗑",
                                    color = PocketColors.Expense,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Card Container for Spending Limit
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            text = "Total Monthly Spending Limit",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = PocketColors.OnSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = uiState.amountText,
                            onValueChange = onAmountChanged,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("budget_amount_input"),
                            textStyle = TextStyle(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = PocketColors.OnSurface
                            ),
                            placeholder = {
                                Text(
                                    text = "0.00",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PocketColors.OnSurfaceVariant.copy(alpha = 0.4f)
                                )
                            },
                            prefix = {
                                Text(
                                    text = "$ ",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PocketColors.Primary
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = PocketColors.SurfaceContainerLowest,
                                unfocusedContainerColor = PocketColors.SurfaceContainerLowest,
                                focusedBorderColor = PocketColors.Primary,
                                unfocusedBorderColor = PocketColors.OutlineVariant
                            )
                        )

                        // Remaining / Over-allocated Pill
                        val remainingText = when {
                            uiState.isOverAllocated -> "Over allocated by $${MoneyParser.formatCentsToAmount(uiState.overAllocatedCents)}"
                            uiState.remainingCents != null -> "Remaining to Allocate: $${MoneyParser.formatCentsToAmount(uiState.remainingCents ?: 0L)}"
                            else -> null
                        }

                        if (remainingText != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (uiState.isOverAllocated) PocketColors.ErrorContainer
                                        else PocketColors.SurfaceContainerLow
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("remaining_allocation_card"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = remainingText,
                                    color = if (uiState.isOverAllocated) PocketColors.Expense else PocketColors.Primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.testTag("remaining_allocation_text")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section Header: Category Allocations
                Text(
                    text = "Category Allocations",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PocketColors.OnSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                // List of Category Allocation Cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    expenseCategories.forEach { category ->
                        CategoryAllocationCard(
                            category = category,
                            amountText = uiState.categoryAllocations[category] ?: "",
                            onAmountChanged = { onCategoryAllocationChanged(category, it) }
                        )
                    }
                }

                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = uiState.errorMessage,
                        color = PocketColors.Expense,
                        fontSize = 13.sp,
                        modifier = Modifier.testTag("error_message_text")
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Primary Save Button
                Button(
                    onClick = onSaveClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_budget_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PocketColors.Primary),
                    shape = CircleShape,
                    enabled = !uiState.isSaving && !uiState.isDeleting
                ) {
                    Text(
                        text = if (uiState.isSaving) {
                            "Saving..."
                        } else if (uiState.hasExistingBudget) {
                            "Save Changes"
                        } else {
                            "Save & Apply Budget"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PocketColors.OnPrimary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Delete Confirmation Dialog
        if (uiState.showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = onDismissDelete,
                title = {
                    Text(
                        text = "Delete Monthly Budget?",
                        fontWeight = FontWeight.Bold,
                        color = PocketColors.OnSurface
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete the budget for ${uiState.monthTitle}? This will remove the spending limit and category allocations for this month.",
                        color = PocketColors.OnSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = onConfirmDelete,
                        modifier = Modifier.testTag("confirm_delete_button")
                    ) {
                        Text(
                            text = "Delete",
                            color = PocketColors.Expense,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = onDismissDelete,
                        modifier = Modifier.testTag("cancel_delete_button")
                    ) {
                        Text(
                            text = "Cancel",
                            color = PocketColors.OnSurfaceVariant
                        )
                    }
                },
                containerColor = PocketColors.SurfaceContainerLowest,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun CategoryAllocationCard(
    category: TransactionCategory,
    amountText: String,
    onAmountChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PocketColors.SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PocketColors.SurfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.icon,
                        fontSize = 20.sp
                    )
                }

                Text(
                    text = category.budgetDisplayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = PocketColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = onAmountChanged,
                modifier = Modifier
                    .width(135.dp)
                    .testTag("category_allocation_input_${category.name.lowercase()}"),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PocketColors.OnSurface,
                    textAlign = TextAlign.End
                ),
                placeholder = {
                    Text(
                        text = "0.00",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = PocketColors.OnSurfaceVariant.copy(alpha = 0.4f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                prefix = {
                    Text(
                        text = "$ ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = PocketColors.OnSurfaceVariant
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PocketColors.SurfaceContainerLow,
                    unfocusedContainerColor = PocketColors.SurfaceContainerLow,
                    focusedBorderColor = PocketColors.Primary,
                    unfocusedBorderColor = Color.Transparent
                )
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun SetBudgetContentPreview() {
    SetBudgetContent(
        uiState = SetBudgetUiState(
            yearMonth = org.hoods.pocket.domain.model.YearMonth(2026, 8),
            amountText = "3000.00",
            categoryAllocations = mapOf(
                TransactionCategory.GROCERIES to "600.00",
                TransactionCategory.DINING to "400.00",
                TransactionCategory.TRANSPORT to "200.00",
                TransactionCategory.SHOPPING to "500.00",
                TransactionCategory.BILLS to "1000.00",
                TransactionCategory.ENTERTAINMENT to "100.00"
            ),
            isExistingBudget = true
        ),
        onAmountChanged = {},
        onSaveClick = {},
        onRequestDelete = {},
        onDismissDelete = {},
        onConfirmDelete = {},
        onDismiss = {}
    )
}
