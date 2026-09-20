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
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.presentation.theme.PocketColors
import org.hoods.pocket.presentation.theme.PocketTheme
import org.hoods.pocket.presentation.transaction.add.AddTransactionContent
import org.hoods.pocket.presentation.transaction.add.AddTransactionUiState
import org.hoods.pocket.presentation.transaction.edit.EditTransactionContent
import org.hoods.pocket.presentation.transaction.edit.EditTransactionUiState
import org.hoods.pocket.navigation.PocketNavigationShell
import org.hoods.pocket.navigation.TopLevelDestination
import org.hoods.pocket.presentation.transaction.list.TransactionListContent
import org.hoods.pocket.presentation.transaction.list.TransactionListUiState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import org.hoods.pocket.presentation.layout.AppLayoutMode
import org.hoods.pocket.presentation.layout.LocalAppLayoutMode
import org.hoods.pocket.domain.model.BudgetHealthStatus
import org.hoods.pocket.domain.model.CashFlowSummary
import org.hoods.pocket.domain.model.CategoryBudgetSummary
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.MonthlyBudgetSummary
import org.hoods.pocket.presentation.budget.dashboard.BudgetDashboardContent
import org.hoods.pocket.presentation.budget.dashboard.BudgetDashboardUiState
import org.hoods.pocket.presentation.budget.set.SetBudgetContent
import org.hoods.pocket.presentation.budget.set.SetBudgetUiState
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

    private fun androidx.compose.ui.test.ComposeUiTest.setMobileContent(
        modifier: Modifier = Modifier.size(width = 390.dp, height = 844.dp),
        content: @Composable () -> Unit
    ) {
        setContent {
            PocketTheme {
                CompositionLocalProvider(LocalAppLayoutMode provides AppLayoutMode.Compact) {
                    Box(
                        modifier = modifier.background(PocketColors.Surface)
                    ) {
                        content()
                    }
                }
            }
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.setDesktopContent(
        modifier: Modifier = Modifier.size(width = 1024.dp, height = 768.dp),
        content: @Composable () -> Unit
    ) {
        setContent {
            PocketTheme {
                CompositionLocalProvider(LocalAppLayoutMode provides AppLayoutMode.Expanded) {
                    Box(
                        modifier = modifier.background(PocketColors.Surface)
                    ) {
                        content()
                    }
                }
            }
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.setMobileShellContent(
        activeDestination: TopLevelDestination,
        content: @Composable () -> Unit
    ) {
        setContent {
            PocketTheme {
                CompositionLocalProvider(LocalAppLayoutMode provides AppLayoutMode.Compact) {
                    Box(
                        modifier = Modifier.size(width = 390.dp, height = 844.dp).background(PocketColors.Surface)
                    ) {
                        PocketNavigationShell(
                            activeDestination = activeDestination,
                            onNavigateToTransactions = {},
                            onNavigateToBudgets = {},
                            onNavigateToAccount = {},
                            layoutMode = AppLayoutMode.Compact,
                            content = content
                        )
                    }
                }
            }
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.setDesktopShellContent(
        activeDestination: TopLevelDestination,
        content: @Composable () -> Unit
    ) {
        setContent {
            PocketTheme {
                CompositionLocalProvider(LocalAppLayoutMode provides AppLayoutMode.Expanded) {
                    Box(
                        modifier = Modifier.size(width = 1024.dp, height = 768.dp).background(PocketColors.Surface)
                    ) {
                        PocketNavigationShell(
                            activeDestination = activeDestination,
                            onNavigateToTransactions = {},
                            onNavigateToBudgets = {},
                            onNavigateToAccount = {},
                            layoutMode = AppLayoutMode.Expanded,
                            content = content
                        )
                    }
                }
            }
        }
    }

    private val sampleTransactions = listOf(
        Transaction(
            id = "1",
            description = "Whole Foods Market",
            amount = 4550L,
            type = TransactionType.EXPENSE,
            category = TransactionCategory.GROCERIES,
            date = Instant.parse("2026-09-08T14:30:00Z")
        ),
        Transaction(
            id = "2",
            description = "Monthly Salary",
            amount = 320000L,
            type = TransactionType.INCOME,
            category = TransactionCategory.SALARY,
            date = Instant.parse("2026-09-01T09:00:00Z")
        ),
        Transaction(
            id = "3",
            description = "Uber Ride",
            amount = 1850L,
            type = TransactionType.EXPENSE,
            category = TransactionCategory.TRANSPORT,
            date = Instant.parse("2026-09-05T18:15:00Z")
        )
    )

    private val sampleCategoryAllocations = mapOf(
        TransactionCategory.GROCERIES to "600.00",
        TransactionCategory.DINING to "400.00",
        TransactionCategory.TRANSPORT to "250.00",
        TransactionCategory.SHOPPING to "350.00",
        TransactionCategory.BILLS to "500.00",
        TransactionCategory.ENTERTAINMENT to "100.00"
    )

    private val sampleOverAllocations = mapOf(
        TransactionCategory.GROCERIES to "1200.00",
        TransactionCategory.DINING to "800.00",
        TransactionCategory.TRANSPORT to "500.00",
        TransactionCategory.SHOPPING to "600.00",
        TransactionCategory.BILLS to "500.00",
        TransactionCategory.ENTERTAINMENT to "200.00"
    )

    private val sampleCategoryBudgetSummaries = listOf(
        CategoryBudgetSummary(
            category = TransactionCategory.GROCERIES,
            limitAmount = 60000L,
            spentAmount = 45000L,
            remainingAmount = 15000L,
            progress = 0.75f,
            status = BudgetHealthStatus.ON_TRACK
        ),
        CategoryBudgetSummary(
            category = TransactionCategory.TRANSPORT,
            limitAmount = 20000L,
            spentAmount = 16000L,
            remainingAmount = 4000L,
            progress = 0.80f,
            status = BudgetHealthStatus.NEAR_LIMIT
        ),
        CategoryBudgetSummary(
            category = TransactionCategory.ENTERTAINMENT,
            limitAmount = 30000L,
            spentAmount = 35000L,
            remainingAmount = -5000L,
            progress = 1.16f,
            status = BudgetHealthStatus.OVER_BUDGET
        ),
        CategoryBudgetSummary(
            category = TransactionCategory.SHOPPING,
            limitAmount = 40000L,
            spentAmount = 8000L,
            remainingAmount = 32000L,
            progress = 0.20f,
            status = BudgetHealthStatus.ON_TRACK
        )
    )

    // ==========================================
    // Mobile Viewport (390 x 844 dp)
    // ==========================================

    @Test
    fun captureMobile_transactionList_emptyState() = runComposeUiTest {
        val month = YearMonth(2026, 9)
        setMobileShellContent(TopLevelDestination.TRANSACTIONS) {
            TransactionListContent(
                uiState = TransactionListUiState(
                    selectedMonth = month,
                    isCurrentMonth = true,
                    transactions = emptyList(),
                    cashFlow = CashFlowSummary(month, income = 0L, expense = 0L),
                    budgetSummary = null,
                    categorySummaries = emptyList()
                ),
                onAddTransactionClick = {},
                onTransactionClick = {},
                onPreviousMonthClick = {},
                onNextMonthClick = {},
                onCurrentMonthClick = {}
            )
        }
        waitForIdle()
        val image = onRoot().captureToImage()
        saveScreenshot("mobile-transaction-list-empty", image)
        saveScreenshot("transaction-list-empty", image)
    }

    @Test
    fun captureMobile_transactionList_populatedState() = runComposeUiTest {
        val month = YearMonth(2026, 9)
        setMobileShellContent(TopLevelDestination.TRANSACTIONS) {
            TransactionListContent(
                uiState = TransactionListUiState(
                    selectedMonth = month,
                    isCurrentMonth = true,
                    transactions = sampleTransactions,
                    cashFlow = CashFlowSummary(month, income = 500000L, expense = 258000L),
                    budgetSummary = MonthlyBudgetSummary(
                        yearMonth = month,
                        budget = MonthlyBudget(month, 300000L),
                        totalExpense = 258000L,
                        remainingAmount = 42000L,
                        progress = 0.86f,
                        status = BudgetHealthStatus.NEAR_LIMIT
                    ),
                    categorySummaries = sampleCategoryBudgetSummaries
                ),
                onAddTransactionClick = {},
                onTransactionClick = {},
                onPreviousMonthClick = {},
                onNextMonthClick = {},
                onCurrentMonthClick = {}
            )
        }
        waitForIdle()
        val image = onRoot().captureToImage()
        saveScreenshot("mobile-transaction-list-populated", image)
        saveScreenshot("transaction-list-populated", image)
    }

    @Test
    fun captureMobile_addTransaction_expenseMode() = runComposeUiTest {
        setMobileContent {
            AddTransactionContent(
                uiState = AddTransactionUiState(
                    selectedType = TransactionType.EXPENSE,
                    selectedCategory = TransactionCategory.GROCERIES,
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
        waitForIdle()
        val image = onRoot().captureToImage()
        saveScreenshot("mobile-add-transaction-expense", image)
        saveScreenshot("add-transaction-expense", image)
    }

    @Test
    fun captureMobile_addTransaction_incomeMode() = runComposeUiTest {
        setMobileContent {
            AddTransactionContent(
                uiState = AddTransactionUiState(
                    selectedType = TransactionType.INCOME,
                    selectedCategory = TransactionCategory.FREELANCE,
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
            category = TransactionCategory.DINING,
            date = Instant.parse("2026-08-22T19:00:00Z")
        )

        setMobileContent {
            EditTransactionContent(
                uiState = EditTransactionUiState(
                    initialTransaction = tx,
                    selectedType = TransactionType.EXPENSE,
                    selectedCategory = TransactionCategory.DINING,
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

        setMobileContent(modifier = Modifier.fillMaxSize()) {
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
        val month = YearMonth(2026, 9)
        setDesktopShellContent(TopLevelDestination.TRANSACTIONS) {
            TransactionListContent(
                uiState = TransactionListUiState(
                    selectedMonth = month,
                    isCurrentMonth = true,
                    transactions = emptyList(),
                    cashFlow = CashFlowSummary(month, income = 0L, expense = 0L),
                    budgetSummary = null,
                    categorySummaries = emptyList()
                ),
                onAddTransactionClick = {},
                onTransactionClick = {},
                onPreviousMonthClick = {},
                onNextMonthClick = {},
                onCurrentMonthClick = {}
            )
        }
        waitForIdle()
        saveScreenshot("desktop-transaction-list-empty", onRoot().captureToImage())
    }

    @Test
    fun captureDesktop_transactionList_populatedState() = runComposeUiTest {
        val month = YearMonth(2026, 9)
        setDesktopShellContent(TopLevelDestination.TRANSACTIONS) {
            TransactionListContent(
                uiState = TransactionListUiState(
                    selectedMonth = month,
                    isCurrentMonth = true,
                    transactions = sampleTransactions,
                    cashFlow = CashFlowSummary(month, income = 500000L, expense = 258000L),
                    budgetSummary = MonthlyBudgetSummary(
                        yearMonth = month,
                        budget = MonthlyBudget(month, 300000L),
                        totalExpense = 258000L,
                        remainingAmount = 42000L,
                        progress = 0.86f,
                        status = BudgetHealthStatus.NEAR_LIMIT
                    ),
                    categorySummaries = sampleCategoryBudgetSummaries
                ),
                onAddTransactionClick = {},
                onTransactionClick = {},
                onPreviousMonthClick = {},
                onNextMonthClick = {},
                onCurrentMonthClick = {}
            )
        }
        waitForIdle()
        saveScreenshot("desktop-transaction-list-populated", onRoot().captureToImage())
    }

    @Test
    fun captureDesktop_addTransaction() = runComposeUiTest {
        setDesktopContent {
            AddTransactionContent(
                uiState = AddTransactionUiState(
                    selectedType = TransactionType.EXPENSE,
                    selectedCategory = TransactionCategory.SHOPPING,
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
            category = TransactionCategory.DINING,
            date = Instant.parse("2026-09-04T12:30:00Z")
        )

        setDesktopContent {
            EditTransactionContent(
                uiState = EditTransactionUiState(
                    initialTransaction = tx,
                    selectedType = TransactionType.EXPENSE,
                    selectedCategory = TransactionCategory.DINING,
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
        waitForIdle()
        saveScreenshot("desktop-edit-transaction", onRoot().captureToImage())
    }

    // ==========================================
    // Budget Visual Validation (Mobile)
    // ==========================================

    @Test
    fun captureMobile_budgetDashboard_empty() = runComposeUiTest {
        setMobileShellContent(TopLevelDestination.BUDGETS) {
            BudgetDashboardContent(
                uiState = BudgetDashboardUiState(
                    selectedMonth = YearMonth(2026, 9),
                    isCurrentMonth = true,
                    summary = null
                ),
                onSetBudgetClick = {},
                onPreviousMonthClick = {},
                onNextMonthClick = {},
                onCurrentMonthClick = {}
            )
        }
        waitForIdle()
        saveScreenshot("mobile-budget-dashboard-empty", onRoot().captureToImage())
    }

    @Test
    fun captureMobile_budgetDashboard_populated() = runComposeUiTest {
        val targetMonth = YearMonth(2026, 8)
        setMobileShellContent(TopLevelDestination.BUDGETS) {
            BudgetDashboardContent(
                uiState = BudgetDashboardUiState(
                    selectedMonth = targetMonth,
                    isCurrentMonth = false,
                    summary = MonthlyBudgetSummary(
                        yearMonth = targetMonth,
                        budget = MonthlyBudget(targetMonth, 300000L),
                        totalExpense = 258000L,
                        remainingAmount = 42000L,
                        progress = 0.86f,
                        status = BudgetHealthStatus.NEAR_LIMIT
                    ),
                    categorySummaries = sampleCategoryBudgetSummaries
                ),
                onSetBudgetClick = {},
                onPreviousMonthClick = {},
                onNextMonthClick = {},
                onCurrentMonthClick = {}
            )
        }
        waitForIdle()
        saveScreenshot("mobile-budget-dashboard-populated", onRoot().captureToImage())
    }

    @Test
    fun captureMobile_budgetDashboard_uncategorized() = runComposeUiTest {
        val targetMonth = YearMonth(2026, 8)
        setMobileShellContent(TopLevelDestination.BUDGETS) {
            BudgetDashboardContent(
                uiState = BudgetDashboardUiState(
                    selectedMonth = targetMonth,
                    isCurrentMonth = false,
                    summary = MonthlyBudgetSummary(
                        yearMonth = targetMonth,
                        budget = MonthlyBudget(targetMonth, 300000L),
                        totalExpense = 273000L, // 258000 + 15000 uncategorized
                        remainingAmount = 27000L,
                        progress = 0.91f,
                        status = BudgetHealthStatus.NEAR_LIMIT
                    ),
                    categorySummaries = sampleCategoryBudgetSummaries
                ),
                onSetBudgetClick = {},
                onPreviousMonthClick = {},
                onNextMonthClick = {},
                onCurrentMonthClick = {}
            )
        }
        waitForIdle()
        saveScreenshot("mobile-budget-dashboard-uncategorized", onRoot().captureToImage())
    }

    @Test
    fun captureMobile_setBudget() = runComposeUiTest {
        val targetMonth = YearMonth(2026, 8)
        setMobileContent {
            SetBudgetContent(
                uiState = SetBudgetUiState(
                    yearMonth = targetMonth,
                    amountText = "3000.00",
                    categoryAllocations = sampleCategoryAllocations
                ),
                onAmountChanged = {},
                onSaveClick = {},
                onDismiss = {}
            )
        }
        waitForIdle()
        saveScreenshot("mobile-set-budget", onRoot().captureToImage())
    }

    @Test
    fun captureMobile_setBudget_overallocated() = runComposeUiTest {
        val targetMonth = YearMonth(2026, 8)
        setMobileContent {
            SetBudgetContent(
                uiState = SetBudgetUiState(
                    yearMonth = targetMonth,
                    amountText = "3000.00",
                    categoryAllocations = sampleOverAllocations,
                    errorMessage = "Total category allocations exceed monthly budget by $800.00"
                ),
                onAmountChanged = {},
                onSaveClick = {},
                onDismiss = {}
            )
        }
        waitForIdle()
        saveScreenshot("mobile-set-budget-overallocated", onRoot().captureToImage())
    }

    // ==========================================
    // Budget Visual Validation (Desktop)
    // ==========================================

    @Test
    fun captureDesktop_budgetDashboard_empty() = runComposeUiTest {
        setDesktopShellContent(TopLevelDestination.BUDGETS) {
            BudgetDashboardContent(
                uiState = BudgetDashboardUiState(
                    selectedMonth = YearMonth(2026, 9),
                    isCurrentMonth = true,
                    summary = null
                ),
                onSetBudgetClick = {},
                onPreviousMonthClick = {},
                onNextMonthClick = {},
                onCurrentMonthClick = {}
            )
        }
        waitForIdle()
        saveScreenshot("desktop-budget-dashboard-empty", onRoot().captureToImage())
    }

    @Test
    fun captureDesktop_budgetDashboard_populated() = runComposeUiTest {
        val targetMonth = YearMonth(2026, 8)
        setDesktopShellContent(TopLevelDestination.BUDGETS) {
            BudgetDashboardContent(
                uiState = BudgetDashboardUiState(
                    selectedMonth = targetMonth,
                    isCurrentMonth = false,
                    summary = MonthlyBudgetSummary(
                        yearMonth = targetMonth,
                        budget = MonthlyBudget(targetMonth, 300000L),
                        totalExpense = 258000L,
                        remainingAmount = 42000L,
                        progress = 0.86f,
                        status = BudgetHealthStatus.NEAR_LIMIT
                    ),
                    categorySummaries = sampleCategoryBudgetSummaries
                ),
                onSetBudgetClick = {},
                onPreviousMonthClick = {},
                onNextMonthClick = {},
                onCurrentMonthClick = {}
            )
        }
        waitForIdle()
        saveScreenshot("desktop-budget-dashboard-populated", onRoot().captureToImage())
    }

    @Test
    fun captureDesktop_budgetDashboard_uncategorized() = runComposeUiTest {
        val targetMonth = YearMonth(2026, 8)
        setDesktopShellContent(TopLevelDestination.BUDGETS) {
            BudgetDashboardContent(
                uiState = BudgetDashboardUiState(
                    selectedMonth = targetMonth,
                    isCurrentMonth = false,
                    summary = MonthlyBudgetSummary(
                        yearMonth = targetMonth,
                        budget = MonthlyBudget(targetMonth, 300000L),
                        totalExpense = 273000L, // 258000 + 15000 uncategorized
                        remainingAmount = 27000L,
                        progress = 0.91f,
                        status = BudgetHealthStatus.NEAR_LIMIT
                    ),
                    categorySummaries = sampleCategoryBudgetSummaries
                ),
                onSetBudgetClick = {},
                onPreviousMonthClick = {},
                onNextMonthClick = {},
                onCurrentMonthClick = {}
            )
        }
        waitForIdle()
        saveScreenshot("desktop-budget-dashboard-uncategorized", onRoot().captureToImage())
    }

    @Test
    fun captureDesktop_setBudget() = runComposeUiTest {
        val targetMonth = YearMonth(2026, 8)
        setDesktopContent {
            SetBudgetContent(
                uiState = SetBudgetUiState(
                    yearMonth = targetMonth,
                    amountText = "3000.00",
                    categoryAllocations = sampleCategoryAllocations
                ),
                onAmountChanged = {},
                onSaveClick = {},
                onDismiss = {}
            )
        }
        waitForIdle()
        saveScreenshot("desktop-set-budget", onRoot().captureToImage())
    }

    @Test
    fun captureDesktop_setBudget_overallocated() = runComposeUiTest {
        val targetMonth = YearMonth(2026, 8)
        setDesktopContent {
            SetBudgetContent(
                uiState = SetBudgetUiState(
                    yearMonth = targetMonth,
                    amountText = "3000.00",
                    categoryAllocations = sampleOverAllocations,
                    errorMessage = "Total category allocations exceed monthly budget by $800.00"
                ),
                onAmountChanged = {},
                onSaveClick = {},
                onDismiss = {}
            )
        }
        waitForIdle()
        saveScreenshot("desktop-set-budget-overallocated", onRoot().captureToImage())
    }

    @Test
    fun captureMobile_editBudget() = runComposeUiTest {
        val targetMonth = YearMonth(2026, 8)
        setMobileContent {
            SetBudgetContent(
                uiState = SetBudgetUiState(
                    yearMonth = targetMonth,
                    amountText = "3000.00",
                    categoryAllocations = sampleCategoryAllocations,
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
        waitForIdle()
        saveScreenshot("mobile-edit-budget", onRoot().captureToImage())
    }

    @Test
    fun captureMobile_deleteBudgetDialog() = runComposeUiTest {
        val targetMonth = YearMonth(2026, 8)
        setMobileContent {
            SetBudgetContent(
                uiState = SetBudgetUiState(
                    yearMonth = targetMonth,
                    amountText = "3000.00",
                    isExistingBudget = true,
                    showDeleteConfirmation = true
                ),
                onAmountChanged = {},
                onSaveClick = {},
                onRequestDelete = {},
                onDismissDelete = {},
                onConfirmDelete = {},
                onDismiss = {}
            )
        }
        waitForIdle()
        val image = onNode(isDialog()).captureToImage()
        saveScreenshot("mobile-delete-budget-dialog", image)
    }

    @Test
    fun captureDesktop_editBudget() = runComposeUiTest {
        val targetMonth = YearMonth(2026, 8)
        setDesktopContent {
            SetBudgetContent(
                uiState = SetBudgetUiState(
                    yearMonth = targetMonth,
                    amountText = "3000.00",
                    categoryAllocations = sampleCategoryAllocations,
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
        waitForIdle()
        saveScreenshot("desktop-edit-budget", onRoot().captureToImage())
    }

    @Test
    fun captureDesktop_deleteBudgetDialog() = runComposeUiTest {
        val targetMonth = YearMonth(2026, 8)
        setDesktopContent {
            SetBudgetContent(
                uiState = SetBudgetUiState(
                    yearMonth = targetMonth,
                    amountText = "3000.00",
                    isExistingBudget = true,
                    showDeleteConfirmation = true
                ),
                onAmountChanged = {},
                onSaveClick = {},
                onRequestDelete = {},
                onDismissDelete = {},
                onConfirmDelete = {},
                onDismiss = {}
            )
        }
        waitForIdle()
        val image = onNode(isDialog()).captureToImage()
        saveScreenshot("desktop-delete-budget-dialog", image)
    }
}
