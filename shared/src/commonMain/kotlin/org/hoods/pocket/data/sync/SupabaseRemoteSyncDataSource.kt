package org.hoods.pocket.data.sync

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable

class SupabaseRemoteSyncDataSource(
    private val client: SupabaseClient
) : RemoteSyncDataSource {

    @Serializable
    private data class ApplySyncMutationsParams(
        val p_mutations: List<SyncMutationDto>
    )

    @Serializable
    private data class PullSyncChangesParams(
        val p_since_epoch_millis: Long
    )

    override suspend fun applySyncMutations(mutations: List<SyncMutationDto>): List<SyncMutationResultDto> {
        val result = client.postgrest.rpc(
            function = "apply_sync_mutations",
            parameters = ApplySyncMutationsParams(p_mutations = mutations)
        )
        return result.decodeAs<List<SyncMutationResultDto>>()
    }

    override suspend fun pullSyncChanges(sinceEpochMillis: Long): PullSyncChangesResponseDto {
        val result = client.postgrest.rpc(
            function = "pull_sync_changes",
            parameters = PullSyncChangesParams(p_since_epoch_millis = sinceEpochMillis)
        )
        return result.decodeAs<PullSyncChangesResponseDto>()
    }
}
