package org.hoods.pocket.presentation.transaction.edit

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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.presentation.theme.PocketColors
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionContent(
    uiState: EditTransactionUiState,
    onTypeSelected: (TransactionType) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onRequestDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onSaveClick: () -> Unit,
    onDismiss: () -> Unit,
    onOpenDatePicker: () -> Unit = {},
    onDismissDatePicker: () -> Unit = {},
    onDateSelected: (Instant) -> Unit = {},
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
                        .widthIn(max = 680.dp)
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
                        // Card Header: Back arrow + "Edit Transaction" title, plus Delete button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .clickable { onDismiss() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "←",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PocketColors.OnSurface
                                    )
                                }
                                Text(
                                    text = "Edit Transaction",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PocketColors.OnSurface
                                )
                            }

                            // Delete Action Button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PocketColors.SurfaceContainer)
                                    .clickable { onRequestDelete() }
                                    .testTag("delete_transaction_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🗑",
                                    color = PocketColors.Expense,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = PocketColors.OutlineVariant.copy(alpha = 0.25f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(24.dp))

                        // Centered Segmented Type Toggle (Expense / Income)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(PocketColors.SurfaceContainer)
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(
                                            if (uiState.selectedType == TransactionType.EXPENSE) PocketColors.Primary
                                            else Color.Transparent
                                        )
                                        .clickable { onTypeSelected(TransactionType.EXPENSE) }
                                        .padding(horizontal = 24.dp, vertical = 8.dp)
                                        .testTag("type_toggle_expense"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Expense",
                                        color = if (uiState.selectedType == TransactionType.EXPENSE) PocketColors.OnPrimary
                                        else PocketColors.OnSurfaceVariant,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(
                                            if (uiState.selectedType == TransactionType.INCOME) PocketColors.Primary
                                            else Color.Transparent
                                        )
                                        .clickable { onTypeSelected(TransactionType.INCOME) }
                                        .padding(horizontal = 24.dp, vertical = 8.dp)
                                        .testTag("type_toggle_income"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Income",
                                        color = if (uiState.selectedType == TransactionType.INCOME) PocketColors.OnPrimary
                                        else PocketColors.OnSurfaceVariant,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Dedicated Amount Box (Soft Mint Container)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(PocketColors.SurfaceContainer)
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "AMOUNT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PocketColors.OnSurfaceVariant,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    OutlinedTextField(
                                        value = uiState.amountText,
                                        onValueChange = onAmountChanged,
                                        placeholder = {
                                            Text(
                                                "0.00",
                                                color = PocketColors.OnSurfaceVariant.copy(alpha = 0.5f),
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        prefix = {
                                            Text(
                                                "$ ",
                                                color = PocketColors.Primary,
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        modifier = Modifier
                                            .widthIn(min = 160.dp, max = 280.dp)
                                            .testTag("amount_input"),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedBorderColor = PocketColors.Primary,
                                            unfocusedBorderColor = Color.Transparent
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Form Fields in a Two-Column Row: Description + Date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Column 1: Description
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Description",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PocketColors.OnSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = uiState.description,
                                    onValueChange = onDescriptionChanged,
                                    placeholder = {
                                        Text(
                                            "Description",
                                            color = PocketColors.OnSurfaceVariant.copy(alpha = 0.6f),
                                            fontSize = 14.sp
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("description_input"),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = PocketColors.SurfaceContainer,
                                        unfocusedContainerColor = PocketColors.SurfaceContainer,
                                        focusedBorderColor = PocketColors.Primary,
                                        unfocusedBorderColor = Color.Transparent
                                    )
                                )
                            }

                            // Column 2: Date
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Date",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PocketColors.OnSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(PocketColors.SurfaceContainer)
                                        .clickable { onOpenDatePicker() }
                                        .padding(horizontal = 14.dp)
                                        .testTag("date_picker_button"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "📅 ", fontSize = 16.sp)
                                        Text(
                                            text = uiState.dateString,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = PocketColors.OnSurface
                                        )
                                    }
                                    Text(
                                        text = "›",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PocketColors.OnSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (uiState.errorMessage != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = uiState.errorMessage,
                                color = PocketColors.Error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("error_message")
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Card Footer: Side-by-Side "Cancel" and "Save Changes"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .clip(CircleShape)
                                    .background(PocketColors.SurfaceContainer)
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Cancel",
                                    color = PocketColors.Primary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = onSaveClick,
                                enabled = !uiState.isSaving && !uiState.isDeleting,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("save_changes_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = PocketColors.Primary),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = if (uiState.isSaving) "Saving..." else "Save Changes",
                                    color = PocketColors.OnPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
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
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
        // Top Navigation Row: Dismiss button, Segmented Pill, and Delete Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PocketColors.SurfaceContainer)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    color = PocketColors.OnSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Segmented Pill Switch (Expense / Income)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(PocketColors.SurfaceContainer)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            if (uiState.selectedType == TransactionType.EXPENSE) PocketColors.Primary
                            else Color.Transparent
                        )
                        .clickable { onTypeSelected(TransactionType.EXPENSE) }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .testTag("type_toggle_expense"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Expense",
                        color = if (uiState.selectedType == TransactionType.EXPENSE) PocketColors.OnPrimary
                        else PocketColors.OnSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            if (uiState.selectedType == TransactionType.INCOME) PocketColors.Primary
                            else Color.Transparent
                        )
                        .clickable { onTypeSelected(TransactionType.INCOME) }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .testTag("type_toggle_income"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Income",
                        color = if (uiState.selectedType == TransactionType.INCOME) PocketColors.OnPrimary
                        else PocketColors.OnSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Delete Action Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PocketColors.SurfaceContainer)
                    .clickable { onRequestDelete() }
                    .testTag("delete_transaction_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🗑",
                    color = PocketColors.Expense,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Hero Amount Display
        val heroPrefix = if (uiState.selectedType == TransactionType.EXPENSE) "-" else "+"
        val heroColor = if (uiState.selectedType == TransactionType.EXPENSE) PocketColors.Expense else PocketColors.Income
        val displayAmount = uiState.amountText.ifBlank { "0.00" }

        Text(
            text = "$heroPrefix$$displayAmount",
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = heroColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Form Fields Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PocketColors.SurfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Amount Input Field
                Text(
                    text = "AMOUNT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PocketColors.OnSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = uiState.amountText,
                    onValueChange = onAmountChanged,
                    placeholder = {
                        Text("0.00", color = PocketColors.OnSurfaceVariant.copy(alpha = 0.6f))
                    },
                    prefix = {
                        Text("$", color = PocketColors.Primary, fontWeight = FontWeight.Bold)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("amount_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PocketColors.SurfaceContainer,
                        unfocusedContainerColor = PocketColors.SurfaceContainer,
                        focusedBorderColor = PocketColors.Primary,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Description Input Field
                Text(
                    text = "DESCRIPTION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PocketColors.OnSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = onDescriptionChanged,
                    placeholder = {
                        Text("Description", color = PocketColors.OnSurfaceVariant.copy(alpha = 0.6f))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("description_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PocketColors.SurfaceContainer,
                        unfocusedContainerColor = PocketColors.SurfaceContainer,
                        focusedBorderColor = PocketColors.Primary,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Date Display & Selection
                Text(
                    text = "DATE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PocketColors.OnSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PocketColors.SurfaceContainer)
                        .clickable { onOpenDatePicker() }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("date_picker_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📅 ", fontSize = 16.sp)
                        Text(
                            text = uiState.dateString,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = PocketColors.OnSurface
                        )
                    }
                    Text(
                        text = "›",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PocketColors.OnSurfaceVariant
                    )
                }
            }
        }

        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = uiState.errorMessage,
                color = PocketColors.Error,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .testTag("error_message")
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Save Button
        val saveLabel = if (uiState.isSaving) "Saving..." else "Save Changes"

        Button(
            onClick = onSaveClick,
            enabled = !uiState.isSaving && !uiState.isDeleting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("save_changes_button"),
            colors = ButtonDefaults.buttonColors(containerColor = PocketColors.Primary),
            shape = RoundedCornerShape(percent = 50)
        ) {
            Text(
                text = saveLabel,
                color = PocketColors.OnPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
}

    if (uiState.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (uiState.selectedDate ?: Clock.System.now()).toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = onDismissDatePicker,
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(Instant.fromEpochMilliseconds(millis))
                        }
                    },
                    modifier = Modifier.testTag("date_picker_confirm_button")
                ) {
                    Text("OK", color = PocketColors.Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissDatePicker,
                    modifier = Modifier.testTag("date_picker_cancel_button")
                ) {
                    Text("Cancel", color = PocketColors.OnSurfaceVariant)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                modifier = Modifier.testTag("date_picker")
            )
        }
    }

    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = {
                Text(
                    text = "Delete Transaction?",
                    fontWeight = FontWeight.Bold,
                    color = PocketColors.OnSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this transaction? This action cannot be undone.",
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

@Preview
@Composable
fun EditTransactionContentPreview() {
    EditTransactionContent(
        uiState = EditTransactionUiState(
            selectedType = TransactionType.EXPENSE,
            description = "Groceries",
            amountText = "45.50",
            dateString = "Sep 9, 2026",
            isLoading = false
        ),
        onTypeSelected = {},
        onDescriptionChanged = {},
        onAmountChanged = {},
        onRequestDelete = {},
        onDismissDelete = {},
        onConfirmDelete = {},
        onSaveClick = {},
        onDismiss = {},
        onOpenDatePicker = {},
        onDismissDatePicker = {},
        onDateSelected = {}
    )
}
