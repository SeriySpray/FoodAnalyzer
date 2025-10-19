package com.foodanalyzer.repository

import com.foodanalyzer.database.UserSettingsDao
import com.foodanalyzer.models.UserSettings
import kotlinx.coroutines.flow.Flow

class UserSettingsRepository(private val userSettingsDao: UserSettingsDao) {

    fun getUserSettings(): Flow<UserSettings?> = userSettingsDao.getUserSettings()

    suspend fun getUserSettingsSync(): UserSettings? = userSettingsDao.getUserSettingsSync()

    suspend fun saveSettings(minCalories: Double, maxCalories: Double) {
        val settings = UserSettings(
            id = 1,
            minCalories = minCalories,
            maxCalories = maxCalories
        )
        userSettingsDao.insertSettings(settings)
    }

    suspend fun updateStreak(streak: Int, lastDate: Long) {
        val currentSettings = userSettingsDao.getUserSettingsSync()
            ?: UserSettings(currentStreak = streak, lastStreakDate = lastDate)

        val updatedSettings = currentSettings.copy(
            currentStreak = streak,
            lastStreakDate = lastDate
        )
        userSettingsDao.updateSettings(updatedSettings)
    }
}