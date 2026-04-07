package com.foodanalyzer.repository

import com.foodanalyzer.database.UserSettingsDao
import com.foodanalyzer.models.UserSettings
import kotlinx.coroutines.flow.Flow

class UserSettingsRepository(private val userSettingsDao: UserSettingsDao) {

    fun getUserSettings(): Flow<UserSettings?> = userSettingsDao.getUserSettings()

    suspend fun getUserSettingsSync(): UserSettings? = userSettingsDao.getUserSettingsSync()

    suspend fun saveSettings(targetCalories: Double, deviationCalories: Double) {
        val current = userSettingsDao.getUserSettingsSync()
        val settings = UserSettings(
            id = 1,
            targetCalories = targetCalories,
            deviationCalories = deviationCalories,
            currentStreak = current?.currentStreak ?: 0,
            lastStreakDate = current?.lastStreakDate ?: 0L
        )
        userSettingsDao.insertSettings(settings)
    }

    suspend fun updateStreak(streak: Int, lastDate: Long) {
        val currentSettings = userSettingsDao.getUserSettingsSync() ?: return
        val updatedSettings = currentSettings.copy(
            currentStreak = streak,
            lastStreakDate = lastDate
        )
        userSettingsDao.updateSettings(updatedSettings)
    }
}
