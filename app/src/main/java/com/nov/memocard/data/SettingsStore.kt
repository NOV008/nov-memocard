package com.nov.memocard.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("memocard_settings", Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(prefs.getInt("theme_mode", 0))
        private set
    var vibration by mutableStateOf(prefs.getBoolean("vibration", true))
        private set
    var fontScale by mutableStateOf(prefs.getFloat("font_scale", 1f))
        private set
    var requestRetention by mutableStateOf(prefs.getFloat("request_retention", 0.9f))
        private set
    var newPerDay by mutableStateOf(prefs.getInt("new_per_day", 20))
        private set
    var reviewsPerDay by mutableStateOf(prefs.getInt("reviews_per_day", 300))
        private set
    var reviewDeckId by mutableStateOf(prefs.getLong("review_deck", 0L))
        private set

    fun updateThemeMode(v: Int) {
        themeMode = v; prefs.edit().putInt("theme_mode", v).apply()
    }

    fun updateVibration(v: Boolean) {
        vibration = v; prefs.edit().putBoolean("vibration", v).apply()
    }

    fun updateFontScale(v: Float) {
        fontScale = v; prefs.edit().putFloat("font_scale", v).apply()
    }

    fun updateRequestRetention(v: Float) {
        requestRetention = v; prefs.edit().putFloat("request_retention", v).apply()
    }

    fun updateNewPerDay(v: Int) {
        newPerDay = v; prefs.edit().putInt("new_per_day", v).apply()
    }

    fun updateReviewsPerDay(v: Int) {
        reviewsPerDay = v; prefs.edit().putInt("reviews_per_day", v).apply()
    }

    fun setReviewDeck(id: Long) {
        reviewDeckId = id; prefs.edit().putLong("review_deck", id).apply()
    }
}