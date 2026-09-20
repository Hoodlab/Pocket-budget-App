package org.hoods.pocket.di

import org.hoods.pocket.data.local.database.AppDatabase
import org.hoods.pocket.data.repository.RoomBudgetRepository
import org.hoods.pocket.data.repository.RoomCategoryBudgetRepository
import org.hoods.pocket.data.repository.RoomDataOwnershipRepository
import org.hoods.pocket.data.repository.RoomTransactionRepository
import org.hoods.pocket.data.repository.SupabaseAuthRepository
import org.hoods.pocket.domain.model.YearMonth
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
import org.hoods.pocket.presentation.auth.account.AccountViewModel
import org.hoods.pocket.presentation.auth.claim.ClaimDataViewModel
import org.hoods.pocket.presentation.auth.setup.SetupConfigViewModel
import org.hoods.pocket.presentation.auth.sign_in.SignInViewModel
import org.hoods.pocket.presentation.budget.dashboard.BudgetDashboardViewModel
import org.hoods.pocket.presentation.budget.set.SetBudgetViewModel
import org.hoods.pocket.presentation.transaction.add.AddTransactionViewModel
import org.hoods.pocket.presentation.transaction.edit.EditTransactionViewModel
import org.hoods.pocket.presentation.transaction.list.TransactionListViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val databaseModule = module {
    single { get<AppDatabase>().transactionDao() }
    single { get<AppDatabase>().budgetDao() }
    single { get<AppDatabase>().categoryBudgetDao() }
    single { get<AppDatabase>().dataOwnershipDao() }
    single { get<AppDatabase>().syncOutboxDao() }
    single { get<AppDatabase>().syncMetadataDao() }
}

val repositoryModule = module {
    single<TransactionRepository> {
        RoomTransactionRepository(
            dao = get(),
            authRepository = get(),
            syncOutboxDao = get(),
            deviceIdProvider = get()
        )
    }
    single<BudgetRepository> {
        RoomBudgetRepository(
            dao = get(),
            authRepository = get(),
            syncOutboxDao = get(),
            deviceIdProvider = get()
        )
    }
    single<CategoryBudgetRepository> {
        RoomCategoryBudgetRepository(
            dao = get(),
            authRepository = get(),
            syncOutboxDao = get(),
            deviceIdProvider = get()
        )
    }
    single<DataOwnershipRepository> {
        RoomDataOwnershipRepository(
            dao = get(),
            syncOutboxDao = get(),
            deviceIdProvider = get()
        )
    }
    single { SupabaseAuthRepository(config = get()) }
    single<AuthRepository> { get<SupabaseAuthRepository>() }
}

val syncModule = module {
    single<org.hoods.pocket.data.sync.RemoteSyncDataSource?> {
        val client = get<SupabaseAuthRepository>().client
        if (client != null) org.hoods.pocket.data.sync.SupabaseRemoteSyncDataSource(client) else null
    }
    single<org.hoods.pocket.data.sync.SyncEngine> {
        org.hoods.pocket.data.sync.PocketSyncEngine(
            syncOutboxDao = get(),
            syncMetadataDao = get(),
            transactionDao = get(),
            budgetDao = get(),
            categoryBudgetDao = get(),
            remoteDataSource = get()
        )
    }
}

val useCaseModule = module {
    // Transactions
    factoryOf(::AddTransactionUseCase)
    factoryOf(::UpdateTransactionUseCase)
    factoryOf(::DeleteTransactionUseCase)
    factoryOf(::GetTransactionByIdUseCase)
    factoryOf(::GetTransactionsByMonthUseCase)
    factoryOf(::GetCashFlowSummaryUseCase)

    // Budgets
    factoryOf(::SetMonthlyBudgetUseCase)
    factoryOf(::GetMonthlyBudgetUseCase)
    factoryOf(::DeleteMonthlyBudgetUseCase)
    factoryOf(::GetCategoryBudgetsUseCase)
    factoryOf(::SaveCategoryAllocationsUseCase)
    factoryOf(::RemoveCategoryBudgetUseCase)
    factoryOf(::SetCategoryBudgetUseCase)
    factoryOf(::GetMonthlyBudgetSummaryUseCase)
    factoryOf(::GetCategoryBudgetSummariesUseCase)

    // Auth & Ownership
    factoryOf(::SignInUseCase)
    factoryOf(::SignUpUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::GetAuthStateUseCase)
    factoryOf(::GetUnclaimedDataSummaryUseCase)
    factoryOf(::ClaimLocalDataUseCase)
}

val viewModelModule = module {
    viewModel {
        TransactionListViewModel(
            getTransactionsByMonthUseCase = get(),
            getCashFlowSummaryUseCase = get(),
            getMonthlyBudgetSummaryUseCase = get(),
            getCategoryBudgetSummariesUseCase = get()
        )
    }
    viewModel {
        AddTransactionViewModel(
            addTransactionUseCase = get()
        )
    }
    viewModel {
        EditTransactionViewModel(
            getTransactionByIdUseCase = get(),
            updateTransactionUseCase = get(),
            deleteTransactionUseCase = get()
        )
    }
    viewModel { (month: YearMonth) ->
        BudgetDashboardViewModel(
            getMonthlyBudgetSummaryUseCase = get(),
            getCategoryBudgetSummariesUseCase = get(),
            initialMonth = month
        )
    }
    viewModel { (month: YearMonth) ->
        SetBudgetViewModel(
            setMonthlyBudgetUseCase = get(),
            getMonthlyBudgetUseCase = get(),
            deleteMonthlyBudgetUseCase = get(),
            getCategoryBudgetsUseCase = get(),
            saveCategoryAllocationsUseCase = get(),
            removeCategoryBudgetUseCase = get(),
            yearMonth = month
        )
    }
    viewModel {
        SignInViewModel(
            signInUseCase = get(),
            signUpUseCase = get(),
            getUnclaimedDataSummaryUseCase = get()
        )
    }
    viewModel { (targetUserId: String) ->
        ClaimDataViewModel(
            targetUserId = targetUserId,
            getUnclaimedDataSummaryUseCase = get(),
            claimLocalDataUseCase = get()
        )
    }
    viewModel {
        AccountViewModel(
            getAuthStateUseCase = get(),
            signOutUseCase = get(),
            config = get()
        )
    }
    viewModel {
        SetupConfigViewModel(
            initialConfig = get()
        )
    }
}

expect val platformModule: Module

val sharedModules = listOf(databaseModule, repositoryModule, syncModule, useCaseModule, viewModelModule)

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(platformModule, *sharedModules.toTypedArray())
    }
}
