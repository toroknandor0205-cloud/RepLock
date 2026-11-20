package com.replock.app.data

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class TimeQuotaRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "replock_time_quota"
        private const val KEY_REMAINING_SECONDS = "remaining_seconds"
        private const val KEY_USED_SECONDS_TODAY = "used_seconds_today"
        private const val KEY_LAST_RESET_DAY = "last_reset_day"
        private const val KEY_STREAK = "streak_days"
        private const val KEY_LAST_STREAK_DAY = "last_streak_day"
        private const val KEY_TOTAL_PUSHUPS = "total_pushups"

        const val DAILY_LIMIT_MINUTES = 45
        const val DAILY_LIMIT_SECONDS = DAILY_LIMIT_MINUTES * 60
        const val PUSHUPS_PER_MINUTE = 2   // 2 push-ups = 1 minute
    }

    init {
        ensureDailyReset()
    }

    private fun ensureDailyReset() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val lastReset = prefs.getInt(KEY_LAST_RESET_DAY, -1)

        if (lastReset != today) {
            // Streak logic
            val yesterday = if (today == 1) 365 else today - 1
            val lastStreakDay = prefs.getInt(KEY_LAST_STREAK_DAY, -1)
            val currentStreak = prefs.getInt(KEY_STREAK, 0)

            val newStreak = when {
                lastStreakDay == yesterday -> currentStreak + 1
                lastStreakDay == today -> currentStreak
                else -> 1
            }

            prefs.edit()
                .putInt(KEY_REMAINING_SECONDS, DAILY_LIMIT_SECONDS)
                .putInt(KEY_USED_SECONDS_TODAY, 0)
                .putInt(KEY_LAST_RESET_DAY, today)
                .putInt(KEY_STREAK, newStreak)
                .putInt(KEY_LAST_STREAK_DAY, today)
                .apply()
        }
    }

    fun getRemainingSeconds(): Int {
        ensureDailyReset()
        return prefs.getInt(KEY_REMAINING_SECONDS, DAILY_LIMIT_SECONDS).coerceAtLeast(0)
    }

    fun getUsedSecondsToday(): Int {
        ensureDailyReset()
        return prefs.getInt(KEY_USED_SECONDS_TODAY, 0)
    }

    fun getStreak(): Int {
        ensureDailyReset()
        return prefs.getInt(KEY_STREAK, 0)
    }

    fun getTotalPushups(): Int = prefs.getInt(KEY_TOTAL_PUSHUPS, 0)

    fun consumeSeconds(seconds: Int): Int {
        ensureDailyReset()
        val current = getRemainingSeconds()
        val used = getUsedSecondsToday()
        val newRemaining = (current - seconds).coerceAtLeast(0)
        val newUsed = used + seconds

        prefs.edit()
            .putInt(KEY_REMAINING_SECONDS, newRemaining)
            .putInt(KEY_USED_SECONDS_TODAY, newUsed)
            .apply()

        return newRemaining
    }

    /**
     * Called when user completes push-ups.
     * Every 2 push-ups → +60 seconds
     */
    fun addFromPushups(pushupCount: Int): Int {
        ensureDailyReset()
        val minutesToAdd = pushupCount / PUSHUPS_PER_MINUTE
        if (minutesToAdd <= 0) return getRemainingSeconds()

        val secondsToAdd = minutesToAdd * 60
        val current = getRemainingSeconds()
        val newRemaining = (current + secondsToAdd).coerceAtMost(DAILY_LIMIT_SECONDS)

        prefs.edit()
            .putInt(KEY_REMAINING_SECONDS, newRemaining)
            .putInt(KEY_TOTAL_PUSHUPS, getTotalPushups() + pushupCount)
            .apply()

        return newRemaining
    }

    fun isBlocked(): Boolean = getRemainingSeconds() <= 0

    fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%d:%02d".format(m, s)
    }
}
