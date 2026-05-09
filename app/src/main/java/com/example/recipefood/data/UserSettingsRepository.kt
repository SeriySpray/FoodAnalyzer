package com.example.recipefood.data

import com.example.recipefood.model.UserSettings
import kotlinx.coroutines.flow.Flow

class UserSettingsRepository(private val userSettingsDao: UserSettingsDao) {

    fun getUserSettings(): Flow<UserSettings?> = userSettingsDao.getUserSettings()

    suspend fun getUserSettingsSync(): UserSettings? = userSettingsDao.getUserSettingsSync()

    suspend fun saveSettings(
        targetCalories: Double,
        targetProteins: Double = 0.0,
        targetFats: Double = 0.0,
        targetCarbs: Double = 0.0
    ) {
        val current = userSettingsDao.getUserSettingsSync()
        val settings = UserSettings(
            id = 1,
            targetCalories = targetCalories,
            targetProteins = targetProteins,
            targetFats = targetFats,
            targetCarbs = targetCarbs,
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
