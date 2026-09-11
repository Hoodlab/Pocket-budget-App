package org.hoods.pocket.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.hoods.pocket.data.local.dao.BudgetDao
import org.hoods.pocket.data.local.entity.MonthlyBudgetEntity
import org.hoods.pocket.domain.model.MonthlyBudget
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.repository.BudgetRepository

class RoomBudgetRepository(
    private val dao: BudgetDao
) : BudgetRepository {

    override fun getMonthlyBudget(yearMonth: YearMonth): Flow<MonthlyBudget?> {
        return dao.getMonthlyBudget(yearMonth.year, yearMonth.month).map { entity ->
            entity?.let {
                MonthlyBudget(
                    yearMonth = YearMonth(it.year, it.month),
                    limitAmount = it.limitAmount
                )
            }
        }
    }

    override suspend fun setMonthlyBudget(budget: MonthlyBudget) {
        dao.insertOrUpdateMonthlyBudget(
            MonthlyBudgetEntity(
                year = budget.yearMonth.year,
                month = budget.yearMonth.month,
                limitAmount = budget.limitAmount
            )
        )
    }
}
