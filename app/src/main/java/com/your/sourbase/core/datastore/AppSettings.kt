package com.your.sourbase.core.datastore
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
private val Context.settings by preferencesDataStore("settings")
/** Small app-local preferences (theme, currency, onboarding); financial records belong in Room. */
@Singleton
class AppSettings @Inject constructor(@ApplicationContext private val context: Context) {
    private val dark = booleanPreferencesKey("dark_theme")
    private val currencyCode = stringPreferencesKey("currency")
    private val onboarded = booleanPreferencesKey("onboarding_completed")
    /** Whether dark theme is enabled. */
    val darkTheme = context.settings.data.map { it[dark] ?: false }
    /** Preferred three-letter ISO currency code. */
    val currency = context.settings.data.map { it[currencyCode] ?: "USD" }
    /** Whether the initial onboarding flow has been completed. */
    val onboardingCompleted = context.settings.data.map { it[onboarded] ?: false }
    /** Updates the dark theme preference. */
    suspend fun setDarkTheme(enabled: Boolean) { context.settings.edit { it[dark] = enabled } }
    /** Updates the preferred three-letter uppercase currency code. */
    suspend fun setCurrency(code: String) {
        require(code.length == 3 && code.all { it in 'A'..'Z' })
        context.settings.edit { it[currencyCode] = code }
    }
    /** Marks onboarding completion state. */
    suspend fun setOnboardingCompleted(completed: Boolean) { context.settings.edit { it[onboarded] = completed } }
}
