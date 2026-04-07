package com.foodanalyzer.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
        setupRangePreview()
        loadSettings()

        binding.btnSaveSettings.setOnClickListener {
            saveSettings()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Налаштування"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRangePreview() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { updateRangePreview() }
        }
        binding.etTargetCalories.addTextChangedListener(watcher)
        binding.etDeviationCalories.addTextChangedListener(watcher)
    }

    private fun updateRangePreview() {
        val target = binding.etTargetCalories.text.toString().toDoubleOrNull()
        val deviation = binding.etDeviationCalories.text.toString().toDoubleOrNull()

        if (target != null && target > 0) {
            val dev = deviation ?: 0.0
            val min = (target - dev).toInt()
            val max = (target + dev).toInt()
            binding.tvRangePreview.text = "Норма: $min – $max ккал"
        } else {
            binding.tvRangePreview.text = ""
        }
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            repository.getUserSettings().collect { settings ->
                settings?.let {
                    if (it.targetCalories > 0) {
                        binding.etTargetCalories.setText(it.targetCalories.toInt().toString())
                        binding.etDeviationCalories.setText(it.deviationCalories.toInt().toString())
                    }
                }
            }
        }
    }

    private fun saveSettings() {
        val target = binding.etTargetCalories.text.toString().toDoubleOrNull()
        val deviation = binding.etDeviationCalories.text.toString().toDoubleOrNull()

        if (target == null || target <= 0) {
            Toast.makeText(this, "Введіть ціль калорій", Toast.LENGTH_SHORT).show()
            return
        }

        if (deviation == null || deviation < 0) {
            Toast.makeText(this, "Введіть допустиме відхилення (0 або більше)", Toast.LENGTH_SHORT).show()
            return
        }

        if (deviation >= target) {
            Toast.makeText(this, "Відхилення повинно бути менше цілі", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            repository.saveSettings(target, deviation)
            Toast.makeText(this@SettingsActivity, "Збережено", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
