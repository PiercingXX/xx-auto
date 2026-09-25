package com.piercingxx.xxauto.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The single DataStore file for all of xx-auto's settings (AU11 — local-only,
 * backed up through the suite door). Reads and writes delegate to the pure
 * [SettingsMapper] so the prefs⇄[Settings] shape is testable without Android.
 */
class AutoPrefs(private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

    /** The current settings, live from the DataStore. */
    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        SettingsMapper.fromMap(prefs.toRawMap())
    }

    /**
     * Applies [transform] to the current settings and persists the result.
     * The transform runs inside the DataStore edit transaction, so concurrent
     * updates read the freshest value.
     */
    suspend fun update(transform: (Settings) -> Settings) {
        context.dataStore.edit { prefs ->
            val current = SettingsMapper.fromMap(prefs.toRawMap())
            val updated = transform(current)
            SettingsMapper.toMap(updated).forEach { (key, value) ->
                when (value) {
                    is Boolean -> prefs[booleanPreferencesKey(key)] = value
                    is String -> prefs[stringPreferencesKey(key)] = value
                    // A null auto_launch_device is omitted by the mapper; remove
                    // any key type that might hold it so it is not left stale.
                    null -> {
                        prefs.remove(booleanPreferencesKey(key))
                        prefs.remove(stringPreferencesKey(key))
                    }
                }
            }
        }
    }

    private fun Preferences.toRawMap(): Map<String, Any?> =
        asMap().mapKeys { it.key.name }

    companion object {
        /** The one DataStore file name (AU11). */
        const val DATASTORE_NAME = "settings"
    }
}