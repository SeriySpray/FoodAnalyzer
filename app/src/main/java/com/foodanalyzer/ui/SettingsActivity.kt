package com.foodanalyzer.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.foodanalyzer.database.AppDatabase
import com.foodanalyzer.databinding.ActivitySettingsBinding
import com.foodanalyzer.repository.UserSettingsRepository
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repository: UserSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(this)
        repository = UserSettingsRepository(database.userSettingsDao())

        setupToolbar()
        loadSettings()

        binding.btnSaveSettings.setOnClickListener {
            saveSettings()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Налаштування норм"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            repository.getUserSettings().collect { settings ->
                settings?.let {
                    binding.etMinCalories.setText(it.minCalories.toInt().toString())
                    binding.etMaxCalories.setText(it.maxCalories.toInt().toString())
                }
            }
        }
    }

    private fun saveSettings() {
        val minCalories = binding.etMinCalories.text.toString().toDoubleOrNull()
        val maxCalories = binding.etMaxCalories.text.toString().toDoubleOrNull()

        if (minCalories == null || maxCalories == null) {
            Toast.makeText(this, "Заповніть всі поля", Toast.LENGTH_SHORT).show()
            return
        }

        if (minCalories >= maxCalories) {
            Toast.makeText(this, "Мінімум повинен бути менше максимуму", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            repository.saveSettings(minCalories, maxCalories)
            Toast.makeText(this@SettingsActivity, "Налаштування збережено", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}