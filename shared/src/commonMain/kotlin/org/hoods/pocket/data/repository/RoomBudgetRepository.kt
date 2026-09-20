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
import kotlin.time.Clock
import org.hoods.pocket.data.local.dao.SyncOutboxDao
import org.hoods.pocket.data.sync.DeviceIdProvider
import org.hoods.pocket.data.sync.OutboxMutationBuilder
import org.hoods.pocket.data.sync.SyncJson

@OptIn(ExperimentalCoroutinesApi::class)
class RoomBudgetRepository(
    private val dao: BudgetDao,
    private val authRepository: AuthRepository? = null,
    private val syncOutboxDao: SyncOutboxDao? = null,
    private val deviceIdProvider: DeviceIdProvider? = null,
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
        val userId = activeUserId()
        val now = Clock.System.now().toEpochMilliseconds()
        val entity = MonthlyBudgetEntity(
            year = budget.yearMonth.year,
            month = budget.yearMonth.month,
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
                entityType = "MONTHLY_BUDGET",
                entityId = "${budget.yearMonth.year}-${budget.yearMonth.month}",
                operation = "UPSERT",
                payloadJson = SyncJson.serializeMonthlyBudget(entity),
                now = now
            )
        } else null

        dao.insertOrUpdateWithOutbox(entity, outbox)
    }

    override suspend fun deleteMonthlyBudget(yearMonth: YearMonth) {
        val userId = activeUserId()
        val now = Clock.System.now().toEpochMilliseconds()
        val entityId = "${yearMonth.year}-${yearMonth.month}"
        if (userId != null && deviceIdProvider != null) {
            val existing = dao.getMonthlyBudgetIncludingDeleted(yearMonth.year, yearMonth.month, userId)
            val tombstone = existing?.copy(deletedAtEpochMillis = now, updatedAtEpochMillis = now)
                ?: MonthlyBudgetEntity(
                    year = yearMonth.year,
                    month = yearMonth.month,
                    limitAmount = 0L,
                    userId = userId,
                    updatedAtEpochMillis = now,
                    deletedAtEpochMillis = now
                )
            val outbox = OutboxMutationBuilder.buildCoalescedMutation(
                syncOutboxDao = syncOutboxDao,
                userId = userId,
                deviceId = deviceIdProvider.getDeviceId(),
                entityType = "MONTHLY_BUDGET",
                entityId = entityId,
                operation = "DELETE",
                payloadJson = SyncJson.serializeMonthlyBudget(tombstone),
                now = now
            )
            dao.deleteWithOutbox(
                year = yearMonth.year,
                month = yearMonth.month,
                activeUserId = userId,
                isSoftDelete = true,
                deletedAt = now,
                updatedAt = now,
                outbox = outbox
            )
        } else {
            dao.deleteWithOutbox(
                year = yearMonth.year,
                month = yearMonth.month,
                activeUserId = userId,
                isSoftDelete = false,
                deletedAt = now,
                updatedAt = now,
                outbox = null
            )
        }
    }
}
