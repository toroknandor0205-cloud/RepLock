package com.replock.app.data

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class VideoQuotaRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "replock_quota"
        private const val KEY_REMAINING = "remaining_videos"
        private const val KEY_LAST_RESET_DAY = "last_reset_day"
        const val DAILY_LIMIT = 75
    }

    init {
        ensureDailyReset()
    }

    private fun ensureDailyReset() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val lastReset = prefs.getInt(KEY_LAST_RESET_DAY, -1)
        if (lastReset != today) {
            prefs.edit()
                .putInt(KEY_REMAINING, DAILY_LIMIT)
                .putInt(KEY_LAST_RESET_DAY, today)
                .apply()
        }
    }

    fun getRemaining(): Int {
        ensureDailyReset()
        return prefs.getInt(KEY_REMAINING, DAILY_LIMIT)
    }

    fun consumeVideo(): Int {
        ensureDailyReset()
        val current = getRemaining()
        val newValue = (current - 1).coerceAtLeast(0)
        prefs.edit().putInt(KEY_REMAINING, newValue).apply()
        return newValue
    }

    fun addFromPushup(): Int {
        ensureDailyReset()
        val current = getRemaining()
        val newValue = (current + 1).coerceAtMost(DAILY_LIMIT)
        prefs.edit().putInt(KEY_REMAINING, newValue).apply()
        return newValue
    }

    fun isBlocked(): Boolean = getRemaining() <= 0
}
