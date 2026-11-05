package com.your.sourbase.core.logging
import android.util.Log
import com.your.sourbase.BuildConfig
/** Debug diagnostics accept fixed action labels, never payloads or personal data. */
object AppLog {
    fun d(action: String) { if (BuildConfig.DEBUG) Log.d("Base", action) }
}
