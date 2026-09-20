package org.hoods.pocket.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_outbox",
    indices = [Index(value = ["userId", "status", "createdAtEpochMillis"])]
)
data class SyncOutboxEntity(
    @PrimaryKey
    val mutationId: String,
    val userId: String,
    val deviceId: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payloadJson: String,
    val updatedAtEpochMillis: Long,
    val status: String,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val createdAtEpochMillis: Long
)
