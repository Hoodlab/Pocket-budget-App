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
        get() = environment != SupabaseEnvironment.UNCONFIGURED &&
                !url.isNullOrBlank() &&
                !publishableKey.isNullOrBlank()

    companion object {
        val unconfigured = SupabaseConfig(
            environment = SupabaseEnvironment.UNCONFIGURED,
            url = null,
            publishableKey = null
        )

        fun resolve(
            rawEnvironment: String?,
            rawUrl: String?,
            rawPublishableKey: String?
        ): SupabaseConfig {
            val envStr = rawEnvironment?.trim().orEmpty()
            val urlStr = rawUrl?.trim().orEmpty()
            val keyStr = rawPublishableKey?.trim().orEmpty()

            val env = when (envStr.uppercase()) {
                "LOCAL" -> SupabaseEnvironment.LOCAL
                "HOSTED_DEV" -> SupabaseEnvironment.HOSTED_DEV
                else -> SupabaseEnvironment.UNCONFIGURED
            }

            if (env == SupabaseEnvironment.UNCONFIGURED || urlStr.isBlank() || keyStr.isBlank()) {
                return unconfigured
            }

            val isValidUrl = when (env) {
                SupabaseEnvironment.LOCAL -> urlStr.startsWith("http://") || urlStr.startsWith("https://")
                SupabaseEnvironment.HOSTED_DEV -> urlStr.startsWith("https://")
                SupabaseEnvironment.UNCONFIGURED -> false
            }

            if (!isValidUrl) {
                return unconfigured
            }

            return SupabaseConfig(
                environment = env,
                url = urlStr,
                publishableKey = keyStr
            )
        }
    }
}
