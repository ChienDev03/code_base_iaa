package com.example.base.core.datastore
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
private val Context.settings by preferencesDataStore("settings")
/** App-local preferences; no account entitlement is persisted here. */
@Singleton
class AppSettings @Inject constructor(@ApplicationContext private val context: Context) {
    private val dark = booleanPreferencesKey("dark_theme")
    val darkTheme = context.settings.data.map { it[dark] ?: false }
    suspend fun setDarkTheme(enabled: Boolean) { context.settings.edit { it[dark] = enabled } }
}
