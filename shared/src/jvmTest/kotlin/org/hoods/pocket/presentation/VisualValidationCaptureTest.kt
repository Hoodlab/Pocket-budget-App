package org.hoods.pocket.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.hoods.pocket.domain.model.Transaction
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.presentation.theme.PocketColors
import org.hoods.pocket.presentation.theme.PocketTheme
import org.hoods.pocket.presentation.transaction.add.AddTransactionContent
import org.hoods.pocket.presentation.transaction.add.AddTransactionUiState
import org.hoods.pocket.presentation.transaction.edit.EditTransactionContent
import org.hoods.pocket.presentation.transaction.edit.EditTransactionUiState
import org.hoods.pocket.presentation.transaction.list.TransactionListContent
import org.hoods.pocket.presentation.transaction.list.TransactionListUiState
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class VisualValidationCaptureTest {

    private fun getOutputDirectory(): File {
        val currentDir = File(System.getProperty("user.dir"))
        val rootDir = if (currentDir.name == "shared") currentDir.parentFile else currentDir
        return File(rootDir, "build/reports/visual-validation/screenshots").apply { mkdirs() }
    }

    private fun saveScreenshot(name: String, imageBitmap: androidx.compose.ui.graphics.ImageBitmap) {
        val dir = getOutputDirectory()
        val file = File(dir, "$name.png")
        val awtImage = imageBitmap.toAwtImage()
        ImageIO.write(awtImage, "PNG", file)
        assertTrue(file.exists() && file.length() > 0, "Screenshot file $name.png should exist and have bytes")
    }

    @Test
    fun captureTransactionList_emptyState() = runComposeUiTest {
        setContent {
            PocketTheme {
                Box(
                    modifier = Modifier
                        .size(width = 390.dp, height = 844.dp)
                        .background(PocketColors.Surface)
                ) {
                    TransactionListContent(
                        uiState = TransactionListUiState(transactions = emptyList()),
                        onAddTransactionClick = {},
                        onTransactionClick = {}
                    )
                }
            }
        }
        waitForIdle()
        saveScreenshot("transaction-list-empty", onRoot().captureToImage())
    }

    @Test
    fun captureTransactionList_populatedState() = runComposeUiTest {
        val sampleTransactions = listOf(
            Transaction(
                id = "1",
                description = "Whole Foods Market",
                amount = 4550L,
                type = TransactionType.EXPENSE,
                date = Instant.fromEpochMilliseconds(1717171717000L)
            ),
            Transaction(
                id = "2",
                description = "Monthly Salary",
                amount = 320000L,
                type = TransactionType.INCOME,
                date = Instant.fromEpochMilliseconds(1717171717000L)
            ),
            Transaction(
                id = "3",
                description = "Uber Ride",
                amount = 1850L,
                type = TransactionType.EXPENSE,
                date = Instant.fromEpochMilliseconds(1717171717000L)
            )
        )

        setContent {
            PocketTheme {
                Box(
                    modifier = Modifier
                        .size(width = 390.dp, height = 844.dp)
                        .background(PocketColors.Surface)
                ) {
                    TransactionListContent(
                        uiState = TransactionListUiState(transactions = sampleTransactions),
                        onAddTransactionClick = {},
                        onTransactionClick = {}
                    )
                }
            }
        }
        waitForIdle()
        saveScreenshot("transaction-list-populated", onRoot().captureToImage())
    }

    @Test
    fun captureAddTransaction_expenseMode() = runComposeUiTest {
        setContent {
            PocketTheme {
                Box(
                    modifier = Modifier
                        .size(width = 390.dp, height = 844.dp)
                        .background(PocketColors.Surface)
                ) {
                    AddTransactionContent(
                        uiState = AddTransactionUiState(
                            selectedType = TransactionType.EXPENSE,
                            description = "Coffee & Bakery",
                            amountText = "12.50"
                        ),
                        onTypeSelected = {},
                        onDescriptionChanged = {},
                        onAmountChanged = {},
                        onSaveClick = {},
                        onDismiss = {}
                    )
                }
            }
        }
        waitForIdle()
        saveScreenshot("add-transaction-expense", onRoot().captureToImage())
    }

    @Test
    fun captureAddTransaction_incomeMode() = runComposeUiTest {
        setContent {
            PocketTheme {
                Box(
                    modifier = Modifier
                        .size(width = 390.dp, height = 844.dp)
                        .background(PocketColors.Surface)
                ) {
                    AddTransactionContent(
                        uiState = AddTransactionUiState(
                            selectedType = TransactionType.INCOME,
                            description = "Freelance Consulting",
                            amountText = "750.00"
                        ),
                        onTypeSelected = {},
                        onDescriptionChanged = {},
                        onAmountChanged = {},
                        onSaveClick = {},
                        onDismiss = {}
                    )
                }
            }
        }
        waitForIdle()
        saveScreenshot("add-transaction-income", onRoot().captureToImage())
    }

    @Test
    fun captureEditTransaction_view() = runComposeUiTest {
        val tx = Transaction(
            id = "tx-100",
            description = "Dinner with Friends",
            amount = 6500L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )

        setContent {
            PocketTheme {
                Box(
                    modifier = Modifier
                        .size(width = 390.dp, height = 844.dp)
                        .background(PocketColors.Surface)
                ) {
                    EditTransactionContent(
                        uiState = EditTransactionUiState(
                            initialTransaction = tx,
                            selectedType = TransactionType.EXPENSE,
                            description = "Dinner with Friends",
                            amountText = "65.00",
                            dateString = "2024-05-31",
                            isLoading = false
                        ),
                        onTypeSelected = {},
                        onDescriptionChanged = {},
                        onAmountChanged = {},
                        onRequestDelete = {},
                        onDismissDelete = {},
                        onConfirmDelete = {},
                        onSaveClick = {},
                        onDismiss = {}
                    )
                }
            }
        }
        waitForIdle()
        saveScreenshot("edit-transaction", onRoot().captureToImage())
    }

    @Test
    fun captureDeleteConfirmation_dialog() = runComposeUiTest {
        val tx = Transaction(
            id = "tx-100",
            description = "Dinner with Friends",
            amount = 6500L,
            type = TransactionType.EXPENSE,
            date = Instant.fromEpochMilliseconds(1717171717000L)
        )

        setContent {
            PocketTheme {
                Box(
                    modifier = Modifier
                        .size(width = 390.dp, height = 844.dp)
                        .background(PocketColors.Surface)
                ) {
                    EditTransactionContent(
                        uiState = EditTransactionUiState(
                            initialTransaction = tx,
                            selectedType = TransactionType.EXPENSE,
                            description = "Dinner with Friends",
                            amountText = "65.00",
                            dateString = "2024-05-31",
                            showDeleteConfirmation = true,
                            isLoading = false
                        ),
                        onTypeSelected = {},
                        onDescriptionChanged = {},
                        onAmountChanged = {},
                        onRequestDelete = {},
                        onDismissDelete = {},
                        onConfirmDelete = {},
                        onSaveClick = {},
                        onDismiss = {}
                    )
                }
            }
        }
        waitForIdle()
        saveScreenshot("delete-confirmation-dialog", onNode(isDialog()).captureToImage())
    }
}
