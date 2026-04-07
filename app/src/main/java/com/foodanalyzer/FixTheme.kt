package com.foodanalyzer

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.foodanalyzer.api.GeminiService

class FixTheme : Application() {
    val geminiService = GeminiService()

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}