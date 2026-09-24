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

fun testLocalConfig(
    url: String = "http://127.0.0.1:54321",
    publishableKey: String = "sb_publishable_test_dummy_key"
) = SupabaseConfig(
    environment = org.hoods.pocket.data.remote.SupabaseEnvironment.LOCAL,
    url = url,
    publishableKey = publishableKey
)

fun testHostedDevConfig(
    url: String = "https://test-project.supabase.co",
    publishableKey: String = "sb_publishable_test_dummy_key"
) = SupabaseConfig(
    environment = org.hoods.pocket.data.remote.SupabaseEnvironment.HOSTED_DEV,
    url = url,
    publishableKey = publishableKey
)

fun startTestKoin(
    transactionRepository: TransactionRepository = FakeTransactionRepository(),
    budgetRepository: BudgetRepository = FakeBudgetRepository(),
    categoryBudgetRepository: CategoryBudgetRepository = FakeCategoryBudgetRepository(),
    authRepository: AuthRepository = FakeAuthRepository(),
    dataOwnershipRepository: DataOwnershipRepository = FakeDataOwnershipRepository(),
    supabaseConfig: SupabaseConfig = testLocalConfig(),
    syncCoordinator: org.hoods.pocket.domain.service.SyncCoordinator = FakeTestSyncCoordinator(),
    connectivityObserver: org.hoods.pocket.domain.service.ConnectivityObserver = FakeTestConnectivityObserver()
) {
    stopKoin()
    val testRepoModule = module {
        single<TransactionRepository> { transactionRepository }
        single<BudgetRepository> { budgetRepository }
        single<CategoryBudgetRepository> { categoryBudgetRepository }
        single<AuthRepository> { authRepository }
        single<DataOwnershipRepository> { dataOwnershipRepository }
        single<SupabaseConfig> { supabaseConfig }
        single<org.hoods.pocket.domain.service.SyncCoordinator> { syncCoordinator }
        single<org.hoods.pocket.domain.service.ConnectivityObserver> { connectivityObserver }
    }
    startKoin {
        modules(testRepoModule, useCaseModule, viewModelModule)
    }
}

class FakeTestSyncCoordinator : org.hoods.pocket.domain.service.SyncCoordinator {
    val syncStateFlow = kotlinx.coroutines.flow.MutableStateFlow<org.hoods.pocket.domain.model.AppSyncState>(
        org.hoods.pocket.domain.model.AppSyncState.Idle
    )
    val pendingCountFlow = kotlinx.coroutines.flow.MutableStateFlow(0)
    var requestSyncCount = 0

    override val syncState: kotlinx.coroutines.flow.StateFlow<org.hoods.pocket.domain.model.AppSyncState> = syncStateFlow
    override val pendingCount: kotlinx.coroutines.flow.StateFlow<Int> = pendingCountFlow

    override fun requestSync(): kotlinx.coroutines.Job {
        requestSyncCount++
        return kotlinx.coroutines.Job().apply { complete() }
    }
}

class FakeTestConnectivityObserver(initialOnline: Boolean = true) : org.hoods.pocket.domain.service.ConnectivityObserver {
    val isOnlineFlow = kotlinx.coroutines.flow.MutableStateFlow(initialOnline)
    override val isOnline: kotlinx.coroutines.flow.StateFlow<Boolean> = isOnlineFlow
}
