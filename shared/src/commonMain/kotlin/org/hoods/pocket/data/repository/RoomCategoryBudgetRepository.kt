package org.hoods.pocket.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.hoods.pocket.data.local.dao.CategoryBudgetDao
import org.hoods.pocket.data.local.entity.CategoryBudgetEntity
import org.hoods.pocket.domain.model.CategoryBudget
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.repository.CategoryBudgetRepository

class RoomCategoryBudgetRepository(
    private val dao: CategoryBudgetDao
) : CategoryBudgetRepository {

    override fun getCategoryBudgets(yearMonth: YearMonth): Flow<List<CategoryBudget>> {
        return dao.getCategoryBudgets(yearMonth.year, yearMonth.month).map { entities ->
            entities.mapNotNull { entity ->
                val cat = TransactionCategory.fromName(entity.category)
                if (cat.applicableType == TransactionType.EXPENSE && entity.limitAmount > 0L) {
                    CategoryBudget(
                        yearMonth = YearMonth(entity.year, entity.month),
                        category = cat,
                        limitAmount = entity.limitAmount
                    )
                } else {
                    null
                }
            }
        }
    }

    override suspend fun setCategoryBudget(budget: CategoryBudget) {
        dao.insertOrUpdateCategoryBudget(
            CategoryBudgetEntity(
                year = budget.yearMonth.year,
                month = budget.yearMonth.month,
                category = budget.category.name,
                limitAmount = budget.limitAmount
            )
        )
    }

    override suspend fun setCategoryBudgets(budgets: List<CategoryBudget>) {
        val entities = budgets.map { budget ->
            CategoryBudgetEntity(
                year = budget.yearMonth.year,
                month = budget.yearMonth.month,
                category = budget.category.name,
                limitAmount = budget.limitAmount
            )
        }
        dao.insertOrUpdateCategoryBudgets(entities)
    }

    override suspend fun removeCategoryBudget(yearMonth: YearMonth, category: TransactionCategory) {
        dao.deleteCategoryBudget(yearMonth.year, yearMonth.month, category.name)
    }

    override suspend fun deleteCategoryBudgetsForMonth(yearMonth: YearMonth) {
        dao.deleteCategoryBudgetsForMonth(yearMonth.year, yearMonth.month)
    }
}
