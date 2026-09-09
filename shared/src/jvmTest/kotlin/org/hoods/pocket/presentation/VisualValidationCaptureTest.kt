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
import org.hoods.pocket.domain.model.YearMonth
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

    private val sampleTransactions = listOf(
        Transaction(
            id = "1",
            description = "Whole Foods Market",
            amount = 4550L,
            type = TransactionType.EXPENSE,
            date = Instant.parse("2026-09-08T14:30:00Z")
        ),
        Transaction(
            id = "2",
            description = "Monthly Salary",
            amount = 320000L,
            type = TransactionType.INCOME,
            date = Instant.parse("2026-09-01T09:00:00Z")
        ),
        Transaction(
            id = "3",
            description = "Uber Ride",
            amount = 1850L,
            type = TransactionType.EXPENSE,
            date = Instant.parse("2026-09-05T18:15:00Z")
        )
    )

    // ==========================================
    // Mobile Viewport (390 x 844 dp)
    // ==========================================

    @Test
    fun captureMobile_transactionList_emptyState() = runComposeUiTest {
        setContent {
            PocketTheme {
                Box(
                    modifier = Modifier
                        .size(width = 390.dp, height = 844.dp)
                        .background(PocketColors.Surface)
                ) {
                    TransactionListContent(
                        uiState = TransactionListUiState(
                            selectedMonth = YearMonth(2026, 9),
                            isCurrentMonth = true,
                            transactions = emptyList()
                        ),
                        onAddTransactionClick = {},
                        onTransactionClick = {},
                        onPreviousMonthClick = {},
                        onNextMonthClick = {},
                        onCurrentMonthClick = {}
                    )
                }
            }
        }
        waitForIdle()
        val image = onRoot().captureToImage()
        saveScreenshot("mobile-transaction-list-empty", image)
        saveScreenshot("transaction-list-empty", image)
    }

    @Test
    fun captureMobile_transactionList_populatedState() = runComposeUiTest {
        val month = YearMonth(2026, 9)
        setContent {
            PocketTheme {
                Box(
                    modifier = Modifier
                        .size(width = 390.dp, height = 844.dp)
                        .background(PocketColors.Surface)
                ) {
                    TransactionListContent(
                        uiState = TransactionListUiState(
                            selectedMonth = month,
                            isCurrentMonth = true,
                            transactions = sampleTransactions
                        ),
                        onAddTransactionClick = {},
                        onTransactionClick = {},
                        onPreviousMonthClick = {},
                        onNextMonthClick = {},
                        onCurrentMonthClick = {}
                    )
                }
            }
        }
        waitForIdle()
        val image = onRoot().captureToImage()
        saveScreenshot("mobile-transaction-list-populated", image)
        saveScreenshot("transaction-list-populated", image)
    }

    @Test
    fun captureMobile_addTransaction_expenseMode() = runComposeUiTest {
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
                            amountText = "12.50",
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
            }
        }
        waitForIdle()
        val image = onRoot().captureToImage()
        saveScreenshot("mobile-add-transaction-expense", image)
        saveScreenshot("add-transaction-expense", image)
    }

    @Test
    fun captureMobile_addTransaction_incomeMode() = runComposeUiTest {
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
                            amountText = "750.00",
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
            }
        }
        waitForIdle()
        val image = onRoot().captureToImage()
        saveScreenshot("mobile-add-transaction-income", image)
        saveScreenshot("add-transaction-income", image)
    }

    @Test
    fun captureMobile_editTransaction_view() = runComposeUiTest {
        val tx = Transaction(
            id = "tx-100",
            description = "Dinner with Friends",
            amount = 6500L,
            type = TransactionType.EXPENSE,
            date = Instant.parse("2026-08-22T19:00:00Z")
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
                            dateString = "Aug 22, 2026",
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
            }
        }
        waitForIdle()
        val image = onRoot().captureToImage()
        saveScreenshot("mobile-edit-transaction", image)
        saveScreenshot("edit-transaction", image)
    }

    @Test
    fun captureMobile_deleteConfirmationDialog() = runComposeUiTest {
        val tx = Transaction(
            id = "tx-100",
            description = "Dinner with Friends",
            amount = 6500L,
            type = TransactionType.EXPENSE,
            date = Instant.parse("2026-08-22T19:00:00Z")
        )

        setContent {
            PocketTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PocketColors.Surface)
                ) {
                    EditTransactionContent(
                        uiState = EditTransactionUiState(
                            initialTransaction = tx,
                            selectedType = TransactionType.EXPENSE,
                            description = "Dinner with Friends",
                            amountText = "65.00",
                            dateString = "Aug 22, 2026",
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
                        onDismiss = {},
                        onOpenDatePicker = {},
                        onDismissDatePicker = {},
                        onDateSelected = {}
                    )
                }
            }
        }
        waitForIdle()
        val image = onNode(isDialog()).captureToImage()
        saveScreenshot("mobile-delete-confirmation-dialog", image)
        saveScreenshot("delete-confirmation-dialog", image)
    }

    // ==========================================
    // Desktop Viewport (1024 x 768 dp)
    // ==========================================

    @Test
    fun captureDesktop_transactionList_emptyState() = runComposeUiTest {
        setContent {
            PocketTheme {
                Box(
                    modifier = Modifier
                        .size(width = 1024.dp, height = 768.dp)
                        .background(PocketColors.Surface)
                ) {
                    TransactionListContent(
                        uiState = TransactionListUiState(
                            selectedMonth = YearMonth(2026, 9),
                            isCurrentMonth = true,
                            transactions = emptyList()
                        ),
                        onAddTransactionClick = {},
                        onTransactionClick = {},
                        onPreviousMonthClick = {},
                        onNextMonthClick = {},
                        onCurrentMonthClick = {}
                    )
                }
            }
        }
        waitForIdle()
        saveScreenshot("desktop-transaction-list-empty", onRoot().captureToImage())
    }

    @Test
    fun captureDesktop_transactionList_populatedState() = runComposeUiTest {
        val month = YearMonth(2026, 9)
        setContent {
            PocketTheme {
                Box(
                    modifier = Modifier
                        .size(width = 1024.dp, height = 768.dp)
                        .background(PocketColors.Surface)
                ) {
                    TransactionListContent(
                        uiState = TransactionListUiState(
                            selectedMonth = month,
                            isCurrentMonth = true,
                            transactions = sampleTransactions
                        ),
                        onAddTransactionClick = {},
                        onTransactionClick = {},
                        onPreviousMonthClick = {},
                        onNextMonthClick = {},
                        onCurrentMonthClick = {}
                    )
                }
            }
        }
        waitForIdle()
        saveScreenshot("desktop-transaction-list-populated", onRoot().captureToImage())
    }

    @Test
    fun captureDesktop_addTransaction() = runComposeUiTest {
        setContent {
            PocketTheme {
                Box(
                    modifier = Modifier
                        .size(width = 1024.dp, height = 768.dp)
                        .background(PocketColors.Surface)
                ) {
                    AddTransactionContent(
                        uiState = AddTransactionUiState(
                            selectedType = TransactionType.EXPENSE,
                            description = "Office Supplies",
                            amountText = "85.20",
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
            }
        }
        waitForIdle()
        saveScreenshot("desktop-add-transaction", onRoot().captureToImage())
    }

    @Test
    fun captureDesktop_editTransaction() = runComposeUiTest {
        val tx = Transaction(
            id = "tx-200",
            description = "Client Lunch",
            amount = 12000L,
            type = TransactionType.EXPENSE,
            date = Instant.parse("2026-09-04T12:30:00Z")
        )

        setContent {
            PocketTheme {
                Box(
                    modifier = Modifier
                        .size(width = 1024.dp, height = 768.dp)
                        .background(PocketColors.Surface)
                ) {
                    EditTransactionContent(
                        uiState = EditTransactionUiState(
                            initialTransaction = tx,
                            selectedType = TransactionType.EXPENSE,
                            description = "Client Lunch",
                            amountText = "120.00",
                            dateString = "Sep 4, 2026",
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
            }
        }
        waitForIdle()
        saveScreenshot("desktop-edit-transaction", onRoot().captureToImage())
    }
}
