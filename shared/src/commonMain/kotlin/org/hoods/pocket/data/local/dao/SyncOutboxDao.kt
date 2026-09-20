package org.hoods.pocket.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import org.hoods.pocket.data.local.entity.SyncOutboxEntity

@Dao
interface SyncOutboxDao {
    @Query("SELECT * FROM sync_outbox WHERE userId = :userId AND status = 'PENDING' ORDER BY createdAtEpochMillis ASC")
    suspend fun getPendingMutations(userId: String): List<SyncOutboxEntity>

    @Query("SELECT * FROM sync_outbox WHERE userId = :userId AND entityType = :entityType AND entityId = :entityId AND status = 'PENDING' LIMIT 1")
    suspend fun findPendingMutation(userId: String, entityType: String, entityId: String): SyncOutboxEntity?

    @Query("SELECT * FROM sync_outbox WHERE mutationId = :mutationId")
    suspend fun getMutation(mutationId: String): SyncOutboxEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMutation(mutation: SyncOutboxEntity)

    @Update
    suspend fun updateMutation(mutation: SyncOutboxEntity)

    @Query("DELETE FROM sync_outbox WHERE mutationId = :mutationId")
    suspend fun deleteMutation(mutationId: String)

    @Query("DELETE FROM sync_outbox WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE userId = :userId AND status = 'PENDING'")
    suspend fun getPendingMutationCount(userId: String): Int
}
