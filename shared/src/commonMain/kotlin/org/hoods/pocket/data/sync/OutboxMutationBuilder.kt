package org.hoods.pocket.data.sync

import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.hoods.pocket.data.local.dao.SyncOutboxDao
import org.hoods.pocket.data.local.entity.SyncOutboxEntity

@OptIn(ExperimentalUuidApi::class)
object OutboxMutationBuilder {

    suspend fun buildCoalescedMutation(
        syncOutboxDao: SyncOutboxDao?,
        userId: String,
        deviceId: String,
        entityType: String,
        entityId: String,
        operation: String,
        payloadJson: String,
        now: Long = Clock.System.now().toEpochMilliseconds()
    ): SyncOutboxEntity? {
        if (syncOutboxDao == null) return null

        val existing = syncOutboxDao.findPendingMutation(userId, entityType, entityId)
        return if (existing != null) {
            existing.copy(
                deviceId = deviceId,
                operation = operation,
                payloadJson = payloadJson,
                updatedAtEpochMillis = now,
                status = "PENDING",
                retryCount = 0,
                lastError = null
            )
        } else {
            SyncOutboxEntity(
                mutationId = Uuid.random().toString(),
                userId = userId,
                deviceId = deviceId,
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                payloadJson = payloadJson,
                updatedAtEpochMillis = now,
                status = "PENDING",
                retryCount = 0,
                lastError = null,
                createdAtEpochMillis = now
            )
        }
    }
}
