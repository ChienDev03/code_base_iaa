package com.your.sourbase.core.ads
import android.app.Activity
import kotlinx.coroutines.flow.StateFlow
import com.your.sourbase.core.config.IntegrationState
/** Optional SDK boundary; no provider is registered in the default app. */
interface AdsManager {
    val state: StateFlow<IntegrationState>
    suspend fun requestConsent(activity: Activity)
    suspend fun showInterstitial(activity: Activity, placement: String): AdOutcome
    suspend fun showRewarded(activity: Activity, placement: String): AdOutcome
    fun cancelPendingShows()
}
/** Only Earned may grant a local reward; the feature deduplicates it. Dismiss alone grants nothing. */
enum class AdOutcome { Earned, Dismissed, Skipped, Failed }
