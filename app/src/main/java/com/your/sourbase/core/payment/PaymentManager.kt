package com.your.sourbase.core.payment
import android.app.Activity
import kotlinx.coroutines.flow.StateFlow
/** Unsynced is unknown, never confirmed free. No SDK is linked by this facade. */
sealed interface Entitlement {
    data object Unknown : Entitlement
    data class Synced(val premium: Boolean) : Entitlement
}
/** Host owns authentication; SDK adapter must reject stale session results and never retain Activity. */
interface PaymentManager {
    val entitlement: StateFlow<Entitlement>
    suspend fun sync(sessionId: String)
    fun resetSession()
    suspend fun purchase(activity: Activity, productId: String): PaymentOutcome
    suspend fun restore(): PaymentOutcome
}
/** Verification retry is deliberately distinct from starting another charge. */
sealed interface PaymentOutcome {
    data object Verified : PaymentOutcome
    data object Pending : PaymentOutcome
    data object Cancelled : PaymentOutcome
    data object StoreFailure : PaymentOutcome
    data object VerificationRequired : PaymentOutcome
    data object Unavailable : PaymentOutcome
}
