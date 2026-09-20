package org.hoods.pocket.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.hoods.pocket.data.local.dao.BudgetDao
import org.hoods.pocket.data.local.entity.MonthlyBudgetEntity
import kotlinx.coroutines.flow.distinctUntilChanged
import org.hoods.pocket.domain.model.AuthState
import org.hoods.pocket.domain.repository.AuthRepository
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.repository.BudgetRepository

@OptIn(ExperimentalCoroutinesApi::class)
class RoomBudgetRepository(
    private val dao: BudgetDao,
    private val authRepository: AuthRepository? = null,
    private val activeUserId: () -> String? = { authRepository?.currentUser()?.id },
    private val activeUserIdFlow: Flow<String?>? = null
) : BudgetRepository {

    private val userFlow: Flow<String?>
        get() = (activeUserIdFlow
            ?: authRepository?.authState?.map { (it as? AuthState.Authenticated)?.user?.id }
            ?: flowOf(activeUserId())
        ).distinctUntilChanged()

    override fun getMonthlyBudget(yearMonth: YearMonth): Flow<MonthlyBudget?> {
        return userFlow.flatMapLatest { userId ->
            dao.getMonthlyBudget(yearMonth.year, yearMonth.month, userId).map { entity ->
                entity?.let {
                    MonthlyBudget(
                        yearMonth = YearMonth(it.year, it.month),
                        limitAmount = it.limitAmount
                    )
                }
            }
        }
    }

    override suspend fun setMonthlyBudget(budget: MonthlyBudget) {
        dao.insertOrUpdateMonthlyBudget(
            MonthlyBudgetEntity(
                year = budget.yearMonth.year,
                month = budget.yearMonth.month,
                limitAmount = budget.limitAmount,
                userId = activeUserId()
            )
        )
    }

    override suspend fun deleteMonthlyBudget(yearMonth: YearMonth) {
        dao.deleteMonthlyBudget(yearMonth.year, yearMonth.month, activeUserId())
    }
}
