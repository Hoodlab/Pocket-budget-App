package org.hoods.pocket.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.hoods.pocket.data.local.dao.CategoryBudgetDao
import org.hoods.pocket.data.local.entity.CategoryBudgetEntity
import kotlinx.coroutines.flow.distinctUntilChanged
import org.hoods.pocket.domain.model.AuthState
import org.hoods.pocket.domain.repository.AuthRepository
import org.hoods.pocket.domain.model.CategoryBudget
import org.hoods.pocket.domain.model.TransactionCategory
import org.hoods.pocket.domain.model.TransactionType
import org.hoods.pocket.domain.model.YearMonth
import org.hoods.pocket.domain.repository.CategoryBudgetRepository

@OptIn(ExperimentalCoroutinesApi::class)
class RoomCategoryBudgetRepository(
    private val dao: CategoryBudgetDao,
    private val authRepository: AuthRepository? = null,
    private val activeUserId: () -> String? = { authRepository?.currentUser()?.id },
    private val activeUserIdFlow: Flow<String?>? = null
) : CategoryBudgetRepository {

    private val userFlow: Flow<String?>
        get() = (activeUserIdFlow
            ?: authRepository?.authState?.map { (it as? AuthState.Authenticated)?.user?.id }
            ?: flowOf(activeUserId())
        ).distinctUntilChanged()

    override fun getCategoryBudgets(yearMonth: YearMonth): Flow<List<CategoryBudget>> {
        return userFlow.flatMapLatest { userId ->
            dao.getCategoryBudgets(yearMonth.year, yearMonth.month, userId).map { entities ->
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
    }

    override suspend fun setCategoryBudget(budget: CategoryBudget) {
        dao.insertOrUpdateCategoryBudget(
            CategoryBudgetEntity(
                year = budget.yearMonth.year,
                month = budget.yearMonth.month,
                category = budget.category.name,
                limitAmount = budget.limitAmount,
                userId = activeUserId()
            )
        )
    }

    override suspend fun setCategoryBudgets(budgets: List<CategoryBudget>) {
        val uid = activeUserId()
        val entities = budgets.map { budget ->
            CategoryBudgetEntity(
                year = budget.yearMonth.year,
                month = budget.yearMonth.month,
                category = budget.category.name,
                limitAmount = budget.limitAmount,
                userId = uid
            )
        }
        dao.insertOrUpdateCategoryBudgets(entities)
    }

    override suspend fun removeCategoryBudget(yearMonth: YearMonth, category: TransactionCategory) {
        dao.deleteCategoryBudget(yearMonth.year, yearMonth.month, category.name, activeUserId())
    }

    override suspend fun deleteCategoryBudgetsForMonth(yearMonth: YearMonth) {
        dao.deleteCategoryBudgetsForMonth(yearMonth.year, yearMonth.month, activeUserId())
    }
}
