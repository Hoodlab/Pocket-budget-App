package org.hoods.pocket.presentation.transaction.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.presentation.theme.PocketColors
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionContent(
    uiState: AddTransactionUiState,
    onTypeSelected: (TransactionType) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDismiss: (() -> Unit)?,
    onOpenDatePicker: () -> Unit = {},
    onDismissDatePicker: () -> Unit = {},
    onDateSelected: (Instant) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PocketColors.Surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Navigation Row: Dismiss button and Segmented Type Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (onDismiss != null) {
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
            } else {
                Spacer(modifier = Modifier.size(40.dp))
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

            // Balancing placeholder for layout symmetry
            Spacer(modifier = Modifier.size(40.dp))
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
                        Text("Description (e.g. Whole Foods)", color = PocketColors.OnSurfaceVariant.copy(alpha = 0.6f))
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

                // Date Selection Row
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
        val buttonLabel = if (uiState.selectedType == TransactionType.EXPENSE) {
            if (uiState.isSaving) "Saving..." else "Save Transaction"
        } else {
            if (uiState.isSaving) "Saving..." else "Save Income"
        }

        Button(
            onClick = onSaveClick,
            enabled = !uiState.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("save_transaction_button"),
            colors = ButtonDefaults.buttonColors(containerColor = PocketColors.Primary),
            shape = RoundedCornerShape(percent = 50)
        ) {
            Text(
                text = buttonLabel,
                color = PocketColors.OnPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (uiState.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDate.toEpochMilliseconds()
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
}

@Preview
@Composable
fun AddTransactionContentPreview() {
    AddTransactionContent(
        uiState = AddTransactionUiState(
            selectedType = TransactionType.EXPENSE,
            description = "Groceries",
            amountText = "45.50",
            dateString = "Today, Sep 9, 2026"
        ),
        onTypeSelected = {},
        onDescriptionChanged = {},
        onAmountChanged = {},
        onSaveClick = {},
        onDismiss = {},
        onOpenDatePicker = {},
        onDismissDatePicker = {},
        onDateSelected = {}
    )
}
