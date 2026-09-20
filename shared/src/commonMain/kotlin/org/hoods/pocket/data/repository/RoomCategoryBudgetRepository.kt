package org.hoods.pocket.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
import kotlin.time.Clock
import org.hoods.pocket.data.local.dao.SyncOutboxDao
import org.hoods.pocket.data.local.entity.SyncOutboxEntity
import org.hoods.pocket.data.sync.DeviceIdProvider
import org.hoods.pocket.data.sync.OutboxMutationBuilder
import org.hoods.pocket.data.sync.SyncJson

import org.hoods.pocket.domain.service.SyncCoordinator

@OptIn(ExperimentalCoroutinesApi::class)
class RoomCategoryBudgetRepository(
    private val dao: CategoryBudgetDao,
    private val authRepository: AuthRepository? = null,
    private val syncOutboxDao: SyncOutboxDao? = null,
    private val deviceIdProvider: DeviceIdProvider? = null,
    private val syncCoordinator: SyncCoordinator? = null,
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
        val userId = activeUserId()
        val now = Clock.System.now().toEpochMilliseconds()
        val entity = CategoryBudgetEntity(
            year = budget.yearMonth.year,
            month = budget.yearMonth.month,
            category = budget.category.name,
            limitAmount = budget.limitAmount,
            userId = userId,
            updatedAtEpochMillis = now,
            deletedAtEpochMillis = null
        )
        val outbox = if (userId != null && deviceIdProvider != null) {
            OutboxMutationBuilder.buildCoalescedMutation(
                syncOutboxDao = syncOutboxDao,
                userId = userId,
                deviceId = deviceIdProvider.getDeviceId(),
                entityType = "CATEGORY_BUDGET",
                entityId = "${budget.yearMonth.year}-${budget.yearMonth.month}-${budget.category.name}",
                operation = "UPSERT",
                payloadJson = SyncJson.serializeCategoryBudget(entity),
                now = now
            )
        } else null

        dao.insertOrUpdateWithOutbox(entity, outbox)
        if (outbox != null) {
            syncCoordinator?.requestSync()
        }
    }

    override suspend fun setCategoryBudgets(budgets: List<CategoryBudget>) {
        val userId = activeUserId()
        val now = Clock.System.now().toEpochMilliseconds()
        val entities = budgets.map { budget ->
            CategoryBudgetEntity(
                year = budget.yearMonth.year,
                month = budget.yearMonth.month,
                category = budget.category.name,
                limitAmount = budget.limitAmount,
                userId = userId,
                updatedAtEpochMillis = now,
                deletedAtEpochMillis = null
            )
        }
        val outboxes = mutableListOf<SyncOutboxEntity>()
        if (userId != null && deviceIdProvider != null) {
            for (entity in entities) {
                val outbox = OutboxMutationBuilder.buildCoalescedMutation(
                    syncOutboxDao = syncOutboxDao,
                    userId = userId,
                    deviceId = deviceIdProvider.getDeviceId(),
                    entityType = "CATEGORY_BUDGET",
                    entityId = "${entity.year}-${entity.month}-${entity.category}",
                    operation = "UPSERT",
                    payloadJson = SyncJson.serializeCategoryBudget(entity),
                    now = now
                )
                if (outbox != null) {
                    outboxes.add(outbox)
                }
            }
        }

        dao.insertOrUpdateAllWithOutbox(entities, outboxes)
        if (outboxes.isNotEmpty()) {
            syncCoordinator?.requestSync()
        }
    }

    override suspend fun removeCategoryBudget(yearMonth: YearMonth, category: TransactionCategory) {
        val userId = activeUserId()
        val now = Clock.System.now().toEpochMilliseconds()
        val entityId = "${yearMonth.year}-${yearMonth.month}-${category.name}"
        if (userId != null && deviceIdProvider != null) {
            val existing = dao.getCategoryBudgetIncludingDeleted(yearMonth.year, yearMonth.month, category.name, userId)
            val tombstone = existing?.copy(deletedAtEpochMillis = now, updatedAtEpochMillis = now)
                ?: CategoryBudgetEntity(
                    year = yearMonth.year,
                    month = yearMonth.month,
                    category = category.name,
                    limitAmount = 0L,
                    userId = userId,
                    updatedAtEpochMillis = now,
                    deletedAtEpochMillis = now
                )
            val outbox = OutboxMutationBuilder.buildCoalescedMutation(
                syncOutboxDao = syncOutboxDao,
                userId = userId,
                deviceId = deviceIdProvider.getDeviceId(),
                entityType = "CATEGORY_BUDGET",
                entityId = entityId,
                operation = "DELETE",
                payloadJson = SyncJson.serializeCategoryBudget(tombstone),
                now = now
            )
            dao.deleteWithOutbox(
                year = yearMonth.year,
                month = yearMonth.month,
                category = category.name,
                activeUserId = userId,
                isSoftDelete = true,
                deletedAt = now,
                updatedAt = now,
                outbox = outbox
            )
            if (outbox != null) {
                syncCoordinator?.requestSync()
            }
        } else {
            dao.deleteWithOutbox(
                year = yearMonth.year,
                month = yearMonth.month,
                category = category.name,
                activeUserId = userId,
                isSoftDelete = false,
                deletedAt = now,
                updatedAt = now,
                outbox = null
            )
        }
    }

    override suspend fun deleteCategoryBudgetsForMonth(yearMonth: YearMonth) {
        val userId = activeUserId()
        val now = Clock.System.now().toEpochMilliseconds()
        if (userId != null && deviceIdProvider != null) {
            val currentList = dao.getCategoryBudgets(yearMonth.year, yearMonth.month, userId).first()
            val outboxes = mutableListOf<SyncOutboxEntity>()
            for (entity in currentList) {
                val tombstone = entity.copy(deletedAtEpochMillis = now, updatedAtEpochMillis = now)
                val outbox = OutboxMutationBuilder.buildCoalescedMutation(
                    syncOutboxDao = syncOutboxDao,
                    userId = userId,
                    deviceId = deviceIdProvider.getDeviceId(),
                    entityType = "CATEGORY_BUDGET",
                    entityId = "${entity.year}-${entity.month}-${entity.category}",
                    operation = "DELETE",
                    payloadJson = SyncJson.serializeCategoryBudget(tombstone),
                    now = now
                )
                if (outbox != null) {
                    outboxes.add(outbox)
                }
            }
            dao.deleteForMonthWithOutbox(
                year = yearMonth.year,
                month = yearMonth.month,
                activeUserId = userId,
                isSoftDelete = true,
                deletedAt = now,
                updatedAt = now,
                outboxes = outboxes
            )
            if (outboxes.isNotEmpty()) {
                syncCoordinator?.requestSync()
            }
        } else {
            dao.deleteForMonthWithOutbox(
                year = yearMonth.year,
                month = yearMonth.month,
                activeUserId = userId,
                isSoftDelete = false,
                deletedAt = now,
                updatedAt = now,
                outboxes = emptyList()
            )
        }
    }
}
