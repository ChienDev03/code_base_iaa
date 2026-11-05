package com.example.base.core.time
import javax.inject.Inject
/** Injectable wall and monotonic clocks. */
interface TimeProvider {
    fun epochMillis(): Long
    fun monotonicMillis(): Long
}
/** System clock used only at the infrastructure boundary. */
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun epochMillis() = System.currentTimeMillis()
    override fun monotonicMillis() = android.os.SystemClock.elapsedRealtime()
}
