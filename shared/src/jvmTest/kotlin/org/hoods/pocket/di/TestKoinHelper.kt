package org.hoods.pocket.di

import org.hoods.pocket.data.remote.SupabaseConfig
import org.hoods.pocket.data.repository.FakeAuthRepository
import org.hoods.pocket.data.repository.FakeDataOwnershipRepository
import org.hoods.pocket.domain.repository.AuthRepository
import org.hoods.pocket.domain.repository.BudgetRepository
import org.hoods.pocket.domain.repository.CategoryBudgetRepository
import org.hoods.pocket.domain.repository.DataOwnershipRepository
import org.hoods.pocket.domain.repository.TransactionRepository
import org.hoods.pocket.domain.usecase.FakeBudgetRepository
import org.hoods.pocket.domain.usecase.FakeCategoryBudgetRepository
import org.hoods.pocket.domain.usecase.FakeTransactionRepository
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

fun startTestKoin(
    transactionRepository: TransactionRepository = FakeTransactionRepository(),
    budgetRepository: BudgetRepository = FakeBudgetRepository(),
    categoryBudgetRepository: CategoryBudgetRepository = FakeCategoryBudgetRepository(),
    authRepository: AuthRepository = FakeAuthRepository(),
    dataOwnershipRepository: DataOwnershipRepository = FakeDataOwnershipRepository(),
    supabaseConfig: SupabaseConfig = SupabaseConfig.local()
) {
    stopKoin()
    val testRepoModule = module {
        single<TransactionRepository> { transactionRepository }
        single<BudgetRepository> { budgetRepository }
        single<CategoryBudgetRepository> { categoryBudgetRepository }
        single<AuthRepository> { authRepository }
        single<DataOwnershipRepository> { dataOwnershipRepository }
        single<SupabaseConfig> { supabaseConfig }
    }
    startKoin {
        modules(testRepoModule, useCaseModule, viewModelModule)
    }
}
