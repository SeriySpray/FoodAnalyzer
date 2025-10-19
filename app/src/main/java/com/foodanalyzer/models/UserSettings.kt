package com.foodanalyzer.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettings(
        @PrimaryKey
        val id: Int = 1,
        val minCalories: Double = 0.0,
        val maxCalories: Double = 0.0,
        val currentStreak: Int = 0,
        val lastStreakDate: Long = 0
)