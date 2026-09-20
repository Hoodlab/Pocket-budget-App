package org.hoods.pocket.data.sync

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncConflictResolverTest {

    @Test
    fun incomingWins_whenIncomingHasNewerTimestamp() {
        val wins = SyncConflictResolver.incomingWins(
            incomingUpdatedAt = 2000L,
            incomingDeviceId = "device-a",
            incomingMutationId = "mut-1",
            existingUpdatedAt = 1000L,
            existingDeviceId = "device-z",
            existingMutationId = "mut-9"
        )
        assertTrue(wins)
    }

    @Test
    fun existingWins_whenIncomingHasOlderTimestamp() {
        val wins = SyncConflictResolver.incomingWins(
            incomingUpdatedAt = 1000L,
            incomingDeviceId = "device-z",
            incomingMutationId = "mut-9",
            existingUpdatedAt = 2000L,
            existingDeviceId = "device-a",
            existingMutationId = "mut-1"
        )
        assertFalse(wins)
    }

    @Test
    fun tieBreakByDeviceId_whenTimestampsAreEqual() {
        val higherWins = SyncConflictResolver.incomingWins(
            incomingUpdatedAt = 1500L,
            incomingDeviceId = "device-b",
            incomingMutationId = "mut-1",
            existingUpdatedAt = 1500L,
            existingDeviceId = "device-a",
            existingMutationId = "mut-9"
        )
        assertTrue(higherWins)

        val lowerLoses = SyncConflictResolver.incomingWins(
            incomingUpdatedAt = 1500L,
            incomingDeviceId = "device-a",
            incomingMutationId = "mut-9",
            existingUpdatedAt = 1500L,
            existingDeviceId = "device-b",
            existingMutationId = "mut-1"
        )
        assertFalse(lowerLoses)
    }

    @Test
    fun tieBreakByMutationId_whenTimestampsAndDeviceIdsAreEqual() {
        val higherWins = SyncConflictResolver.incomingWins(
            incomingUpdatedAt = 1500L,
            incomingDeviceId = "device-a",
            incomingMutationId = "mut-b",
            existingUpdatedAt = 1500L,
            existingDeviceId = "device-a",
            existingMutationId = "mut-a"
        )
        assertTrue(higherWins)

        val lowerLoses = SyncConflictResolver.incomingWins(
            incomingUpdatedAt = 1500L,
            incomingDeviceId = "device-a",
            incomingMutationId = "mut-a",
            existingUpdatedAt = 1500L,
            existingDeviceId = "device-a",
            existingMutationId = "mut-b"
        )
        assertFalse(lowerLoses)
    }

    @Test
    fun identicalTupleDoesNotWin() {
        val wins = SyncConflictResolver.incomingWins(
            incomingUpdatedAt = 1500L,
            incomingDeviceId = "device-a",
            incomingMutationId = "mut-a",
            existingUpdatedAt = 1500L,
            existingDeviceId = "device-a",
            existingMutationId = "mut-a"
        )
        assertFalse(wins)
    }
}
