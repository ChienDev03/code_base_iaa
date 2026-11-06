package com.your.sourbase.core.config
import com.your.sourbase.BuildConfig
/** Runtime environment is independent of build type and sandbox policy. */
data class AppConfig(val environment: String, val sandbox: Boolean, val firebaseEnabled: Boolean) {
    companion object {
        /** Validated build configuration. */
        val current = AppConfig(BuildConfig.ENVIRONMENT, BuildConfig.SANDBOX, BuildConfig.FIREBASE_ENABLED)
    }
}
/** Distinguishes installation, configuration, initialization, consent and runtime readiness. */
data class IntegrationState(
    val installed: Boolean = false,
    val configured: Boolean = false,
    val initialized: Boolean = false,
    val consentGranted: Boolean = false,
    val runtimeReady: Boolean = false,
) {
    val available: Boolean get() = installed && configured && initialized && consentGranted && runtimeReady
}
