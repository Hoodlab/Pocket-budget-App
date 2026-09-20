package org.hoods.pocket.data.remote

enum class SupabaseEnvironment {
    LOCAL,
    HOSTED_DEV,
    UNCONFIGURED
}

data class SupabaseConfig(
    val environment: SupabaseEnvironment,
    val url: String?,
    val publishableKey: String?
) {
    val isConfigured: Boolean
        get() = environment != SupabaseEnvironment.UNCONFIGURED && !url.isNullOrBlank() && !publishableKey.isNullOrBlank()

    companion object {
        fun local(
            url: String = "http://127.0.0.1:54321",
            publishableKey: String = "sb_publishable_ACJWlzQHlZjBrEguHvfOxg_3BJgxAaH"
        ) = SupabaseConfig(
            environment = SupabaseEnvironment.LOCAL,
            url = url,
            publishableKey = publishableKey
        )

        fun hostedDev(
            url: String = "https://hyetpnyruynichpsxyvz.supabase.co",
            publishableKey: String = "sb_publishable_BtXs57VHNDku3yKeiFCmuw_hcmQUVdO"
        ) = SupabaseConfig(
            environment = SupabaseEnvironment.HOSTED_DEV,
            url = url,
            publishableKey = publishableKey
        )

        val unconfigured = SupabaseConfig(
            environment = SupabaseEnvironment.UNCONFIGURED,
            url = null,
            publishableKey = null
        )
    }
}
