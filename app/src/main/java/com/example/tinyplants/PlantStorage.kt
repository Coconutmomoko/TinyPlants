package com.example.tinyplants

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


private val Context.dataStore by preferencesDataStore(name = "tiny_plants_store")

class PlantStorage(private val context: Context) {

    // --- Keys ---
    private val FAVORITES = stringSetPreferencesKey("favorites")
    private val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
    private val REMINDER_HOUR = intPreferencesKey("reminder_hour")
    private val REMINDER_MINUTE = intPreferencesKey("reminder_minute")


    val favoritesFlow: Flow<Set<String>> =
        context.dataStore.data.map { prefs ->
            prefs[FAVORITES] ?: emptySet()
        }


    val reminderFlow: Flow<ReminderSettings> =
        context.dataStore.data.map { prefs ->
            ReminderSettings(
                enabled = prefs[REMINDER_ENABLED] ?: false,
                hour = prefs[REMINDER_HOUR] ?: 9,
                minute = prefs[REMINDER_MINUTE] ?: 0
            )
        }


    suspend fun toggleFavorite(id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITES]?.toMutableSet() ?: mutableSetOf()

            if (current.contains(id)) current.remove(id)
            else current.add(id)

            prefs[FAVORITES] = current
        }
    }


    suspend fun setReminder(settings: ReminderSettings) {
        context.dataStore.edit { prefs ->
            prefs[REMINDER_ENABLED] = settings.enabled
            prefs[REMINDER_HOUR] = settings.hour
            prefs[REMINDER_MINUTE] = settings.minute
        }
    }
}
