package com.example.tinyplants

data class Plant(
    val id: String,
    val name: String,
    val emoji: String = "🪴",
    val note: String = ""
)

data class ReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = 9,
    val minute: Int = 0
)

data class UiState(
    val plants: List<Plant> = emptyList(),
    val favorites: Set<String> = emptySet(), // store plant ids
    val loading: Boolean = false,
    val error: String? = null,
    val reminder: ReminderSettings = ReminderSettings()
)
