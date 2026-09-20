package org.hoods.pocket

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import org.hoods.pocket.data.remote.SupabaseConfig
import org.hoods.pocket.data.repository.InMemoryBudgetRepository
import org.hoods.pocket.data.repository.InMemoryCategoryBudgetRepository
import org.hoods.pocket.data.repository.SupabaseAuthRepository
import org.hoods.pocket.domain.model.UnclaimedDataSummary
import org.hoods.pocket.domain.repository.AuthRepository
import org.hoods.pocket.domain.repository.BudgetRepository
import org.hoods.pocket.domain.repository.CategoryBudgetRepository
import org.hoods.pocket.domain.repository.DataOwnershipRepository
import org.hoods.pocket.domain.repository.TransactionRepository
import org.hoods.pocket.domain.usecase.AddTransactionUseCase
import org.hoods.pocket.domain.usecase.ClaimLocalDataUseCase
import org.hoods.pocket.domain.usecase.DeleteMonthlyBudgetUseCase
import org.hoods.pocket.domain.usecase.DeleteTransactionUseCase
import org.hoods.pocket.domain.usecase.GetAuthStateUseCase
import org.hoods.pocket.domain.usecase.GetCashFlowSummaryUseCase
import org.hoods.pocket.domain.usecase.GetCategoryBudgetSummariesUseCase
import org.hoods.pocket.domain.usecase.GetCategoryBudgetsUseCase
import org.hoods.pocket.domain.usecase.GetMonthlyBudgetSummaryUseCase
import org.hoods.pocket.domain.usecase.GetMonthlyBudgetUseCase
import org.hoods.pocket.domain.usecase.GetTransactionByIdUseCase
import org.hoods.pocket.domain.usecase.GetTransactionsByMonthUseCase
import org.hoods.pocket.domain.usecase.GetUnclaimedDataSummaryUseCase
import org.hoods.pocket.domain.usecase.RemoveCategoryBudgetUseCase
import org.hoods.pocket.domain.usecase.SaveCategoryAllocationsUseCase
import org.hoods.pocket.domain.usecase.SetCategoryBudgetUseCase
import org.hoods.pocket.domain.usecase.SetMonthlyBudgetUseCase
import org.hoods.pocket.domain.usecase.SignInUseCase
import org.hoods.pocket.domain.usecase.SignOutUseCase
import org.hoods.pocket.domain.usecase.SignUpUseCase
import org.hoods.pocket.domain.usecase.UpdateTransactionUseCase
import org.hoods.pocket.navigation.PocketNavHost
import org.hoods.pocket.presentation.auth.account.AccountViewModel
import org.hoods.pocket.presentation.auth.claim.ClaimDataViewModel
import org.hoods.pocket.presentation.auth.setup.SetupConfigViewModel
import org.hoods.pocket.presentation.auth.sign_in.SignInViewModel
import org.hoods.pocket.presentation.budget.dashboard.BudgetDashboardViewModel
import org.hoods.pocket.presentation.budget.set.SetBudgetViewModel
import org.hoods.pocket.presentation.layout.AppLayoutMode
import org.hoods.pocket.presentation.layout.LocalAppLayoutMode
import org.hoods.pocket.presentation.theme.PocketColors
import org.hoods.pocket.presentation.theme.PocketTheme
import org.hoods.pocket.presentation.transaction.add.AddTransactionViewModel
import org.hoods.pocket.presentation.transaction.edit.EditTransactionViewModel
import org.hoods.pocket.presentation.transaction.list.TransactionListViewModel

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun App(
    repository: TransactionRepository,
    budgetRepository: BudgetRepository? = null,
    categoryBudgetRepository: CategoryBudgetRepository? = null,
    authRepository: AuthRepository? = null,
    dataOwnershipRepository: DataOwnershipRepository? = null,
    supabaseConfig: SupabaseConfig = SupabaseConfig.local()
) {
    PocketTheme {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(PocketColors.Surface)
                .safeContentPadding()
        ) {
            val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(maxWidth, maxHeight))
            val layoutMode = AppLayoutMode.from(windowSizeClass.widthSizeClass)
            CompositionLocalProvider(LocalAppLayoutMode provides layoutMode) {
                PocketBudgetRoot(
                    repository = repository,
                    budgetRepository = budgetRepository,
                    categoryBudgetRepository = categoryBudgetRepository,
                    authRepository = authRepository,
                    dataOwnershipRepository = dataOwnershipRepository,
                    supabaseConfig = supabaseConfig,
                    layoutMode = layoutMode
                )
            }
        }
    }
}

@Composable
fun PocketBudgetRoot(
    repository: TransactionRepository,
    budgetRepository: BudgetRepository? = null,
    categoryBudgetRepository: CategoryBudgetRepository? = null,
    authRepository: AuthRepository? = null,
    dataOwnershipRepository: DataOwnershipRepository? = null,
    supabaseConfig: SupabaseConfig = SupabaseConfig.local(),
    layoutMode: AppLayoutMode = AppLayoutMode.current(),
    modifier: Modifier = Modifier
) {
    val activeBudgetRepo = budgetRepository ?: remember { InMemoryBudgetRepository() }
    val activeCategoryBudgetRepo = categoryBudgetRepository ?: remember { InMemoryCategoryBudgetRepository() }
    val activeAuthRepo = authRepository ?: remember(supabaseConfig) { SupabaseAuthRepository(supabaseConfig) }
    val activeDataOwnershipRepo = dataOwnershipRepository ?: remember {
        object : DataOwnershipRepository {
            override suspend fun getUnclaimedDataSummary(): UnclaimedDataSummary = UnclaimedDataSummary(0, 0, 0)
            override suspend fun claimLocalDataAtomically(userId: String): Result<Unit> = Result.success(Unit)
        }
    }

    val getTransactionsByMonthUseCase = remember(repository) { GetTransactionsByMonthUseCase(repository) }
    val addTransactionUseCase = remember(repository) { AddTransactionUseCase(repository) }
    val updateTransactionUseCase = remember(repository) { UpdateTransactionUseCase(repository) }
    val deleteTransactionUseCase = remember(repository) { DeleteTransactionUseCase(repository) }
    val getTransactionByIdUseCase = remember(repository) { GetTransactionByIdUseCase(repository) }

    val setMonthlyBudgetUseCase = remember(activeBudgetRepo) { SetMonthlyBudgetUseCase(activeBudgetRepo) }
    val getMonthlyBudgetUseCase = remember(activeBudgetRepo) { GetMonthlyBudgetUseCase(activeBudgetRepo) }
    val getCategoryBudgetsUseCase = remember(activeCategoryBudgetRepo) {
        GetCategoryBudgetsUseCase(activeCategoryBudgetRepo)
    }
    val saveCategoryAllocationsUseCase = remember(activeBudgetRepo, activeCategoryBudgetRepo) {
        SaveCategoryAllocationsUseCase(activeBudgetRepo, activeCategoryBudgetRepo)
    }
    val removeCategoryBudgetUseCase = remember(activeCategoryBudgetRepo) {
        RemoveCategoryBudgetUseCase(activeCategoryBudgetRepo)
    }
    val deleteMonthlyBudgetUseCase = remember(activeBudgetRepo, activeCategoryBudgetRepo) {
        DeleteMonthlyBudgetUseCase(activeBudgetRepo, activeCategoryBudgetRepo)
    }
    val getMonthlyBudgetSummaryUseCase = remember(activeBudgetRepo, repository) {
        GetMonthlyBudgetSummaryUseCase(activeBudgetRepo, repository)
    }
    val getCategoryBudgetSummariesUseCase = remember(activeCategoryBudgetRepo, repository) {
        GetCategoryBudgetSummariesUseCase(activeCategoryBudgetRepo, repository)
    }
    val getCashFlowSummaryUseCase = remember(repository) {
        GetCashFlowSummaryUseCase(repository)
    }

    // Auth Use Cases
    val signInUseCase = remember(activeAuthRepo) { SignInUseCase(activeAuthRepo) }
    val signUpUseCase = remember(activeAuthRepo) { SignUpUseCase(activeAuthRepo) }
    val signOutUseCase = remember(activeAuthRepo) { SignOutUseCase(activeAuthRepo) }
    val getAuthStateUseCase = remember(activeAuthRepo) { GetAuthStateUseCase(activeAuthRepo) }
    val getUnclaimedDataSummaryUseCase = remember(activeDataOwnershipRepo) {
        GetUnclaimedDataSummaryUseCase(activeDataOwnershipRepo)
    }
    val claimLocalDataUseCase = remember(activeDataOwnershipRepo) {
        ClaimLocalDataUseCase(activeDataOwnershipRepo)
    }

    val listViewModel = remember(
        getTransactionsByMonthUseCase,
        getCashFlowSummaryUseCase,
        getMonthlyBudgetSummaryUseCase,
        getCategoryBudgetSummariesUseCase
    ) {
        TransactionListViewModel(
            getTransactionsByMonthUseCase = getTransactionsByMonthUseCase,
            getCashFlowSummaryUseCase = getCashFlowSummaryUseCase,
            getMonthlyBudgetSummaryUseCase = getMonthlyBudgetSummaryUseCase,
            getCategoryBudgetSummariesUseCase = getCategoryBudgetSummariesUseCase
        )
    }

    PocketNavHost(
        listViewModel = listViewModel,
        addViewModelFactory = { AddTransactionViewModel(addTransactionUseCase) },
        editViewModelFactory = {
            EditTransactionViewModel(
                getTransactionByIdUseCase = getTransactionByIdUseCase,
                updateTransactionUseCase = updateTransactionUseCase,
                deleteTransactionUseCase = deleteTransactionUseCase
            )
        },
        budgetDashboardViewModelFactory = { month ->
            BudgetDashboardViewModel(
                getMonthlyBudgetSummaryUseCase = getMonthlyBudgetSummaryUseCase,
                getCategoryBudgetSummariesUseCase = getCategoryBudgetSummariesUseCase,
                initialMonth = month
            )
        },
        setBudgetViewModelFactory = { month ->
            SetBudgetViewModel(
                setMonthlyBudgetUseCase = setMonthlyBudgetUseCase,
                getMonthlyBudgetUseCase = getMonthlyBudgetUseCase,
                deleteMonthlyBudgetUseCase = deleteMonthlyBudgetUseCase,
                getCategoryBudgetsUseCase = getCategoryBudgetsUseCase,
                saveCategoryAllocationsUseCase = saveCategoryAllocationsUseCase,
                removeCategoryBudgetUseCase = removeCategoryBudgetUseCase,
                yearMonth = month
            )
        },
        signInViewModelFactory = {
            SignInViewModel(signInUseCase, signUpUseCase, getUnclaimedDataSummaryUseCase)
        },
        claimDataViewModelFactory = { targetUserId ->
            ClaimDataViewModel(targetUserId, getUnclaimedDataSummaryUseCase, claimLocalDataUseCase)
        },
        accountViewModelFactory = {
            AccountViewModel(getAuthStateUseCase, signOutUseCase, supabaseConfig)
        },
        setupConfigViewModelFactory = {
            SetupConfigViewModel(supabaseConfig)
        },
        layoutMode = layoutMode,
        modifier = modifier
    )
}
