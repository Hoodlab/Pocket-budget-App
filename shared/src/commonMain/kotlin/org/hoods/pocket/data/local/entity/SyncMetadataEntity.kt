package org.hoods.pocket.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val userId: String,
    val lastSyncCursor: String? = null,
    val lastSyncAtEpochMillis: Long? = null,
    val lastSyncStatus: String = "IDLE",
    val lastSyncError: String? = null
)
