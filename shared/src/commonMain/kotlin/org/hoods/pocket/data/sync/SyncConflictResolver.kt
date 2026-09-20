package org.hoods.pocket.data.sync

object SyncConflictResolver {
    /**
     * Evaluates deterministic Last-Write-Wins ordering based on:
     * (updatedAtEpochMillis, deviceId, mutationId)
     *
     * Returns true if incoming change is strictly greater than existing change.
     */
    fun incomingWins(
        incomingUpdatedAt: Long,
        incomingDeviceId: String,
        incomingMutationId: String,
        existingUpdatedAt: Long,
        existingDeviceId: String,
        existingMutationId: String
    ): Boolean {
        if (incomingUpdatedAt != existingUpdatedAt) {
            return incomingUpdatedAt > existingUpdatedAt
        }
        if (incomingDeviceId != existingDeviceId) {
            return incomingDeviceId > existingDeviceId
        }
        return incomingMutationId > existingMutationId
    }
}
