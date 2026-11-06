package com.your.sourbase.core.config

/** Optional remote app configuration; financial records are never inputs. */
interface RemoteSettings {
    /** Returns activated configuration or bundled defaults without waiting for the network. */
    fun string(key: String): String
    /** Best-effort explicit refresh. Failure leaves cached/default configuration usable. */
    suspend fun refresh(): Boolean
}

/** Conservative defaults: no configured ad placements and no forced update. */
object RemoteDefaults {
    val values = mapOf(
        "ads_config" to """{"banner":[],"rewarded":[],"interstitial":[],"native":[]}""",
        "app_update_config" to """{"store_url":"","store_latest_version":"0.0.0","force_update_versions":[]}""",
    )
}
