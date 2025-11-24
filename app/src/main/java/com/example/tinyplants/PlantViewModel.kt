package com.example.tinyplants

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class PlantViewModel(
    private val repo: PlantRepository,
    private val storage: PlantStorage,
    private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(UiState(loading = true))
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        // combine local storage flows into ui state
        viewModelScope.launch {
            combine(storage.favoritesFlow, storage.reminderFlow) { favs, reminder ->
                _state.value.copy(favorites = favs, reminder = reminder)
            }.collect { merged ->
                _state.value = merged
            }
        }

        refreshPlants()
    }

    fun refreshPlants() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { repo.fetchPlants() }
                .onSuccess { list ->
                    _state.value = _state.value.copy(plants = list, loading = false)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        loading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch { storage.toggleFavorite(id) }
    }

    fun updateReminder(enabled: Boolean, hour: Int, minute: Int) {
        val newSettings = ReminderSettings(enabled, hour, minute)
        viewModelScope.launch {
            storage.setReminder(newSettings)
            scheduleReminder(newSettings)
        }
    }

    private fun scheduleReminder(settings: ReminderSettings) {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(appContext, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)

        if (!settings.enabled) return

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, settings.hour)
            set(Calendar.MINUTE, settings.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
