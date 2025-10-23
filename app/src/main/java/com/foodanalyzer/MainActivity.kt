package com.foodanalyzer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.foodanalyzer.api.GeminiService
import com.foodanalyzer.database.AppDatabase
import com.foodanalyzer.databinding.ActivityMainBinding
import com.foodanalyzer.models.UserSettings
import com.foodanalyzer.repository.MealRepository
import androidx.lifecycle.lifecycleScope
import com.foodanalyzer.repository.UserSettingsRepository
import com.foodanalyzer.ui.CameraActivity
import com.foodanalyzer.ui.EditProductsActivity
import com.foodanalyzer.ui.HistoryActivity
import com.foodanalyzer.ui.SettingsActivity
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.Date

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMainBinding
    private val geminiService = GeminiService()
    private lateinit var toggle: ActionBarDrawerToggle

    private lateinit var userSettingsRepository: UserSettingsRepository

    private lateinit var mealRepository: MealRepository

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            analyzeImageFromGallery(it)
        }
    }

    override fun onResume() {
        super.onResume()
        loadTodayProgress()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigationDrawer()

        val database = AppDatabase.getDatabase(this)
        userSettingsRepository = UserSettingsRepository(database.userSettingsDao())
        mealRepository = MealRepository(database.mealDao())

        loadTodayProgress()

        binding.btnTakePhoto.setOnClickListener {
            if (checkCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        binding.btnSelectFromGallery.setOnClickListener {
            openGallery()
        }

        binding.btnCreateMeal.setOnClickListener {
            createMealManually()
        }
    }
    private fun createMealManually() {
        // Створюємо порожню страву з порожньою назвою та списком продуктів
        val food = com.foodanalyzer.models.Food(
            name = "",
            products = mutableListOf(),
            nutrition = null
        )

        // Перетворюємо об'єкт в JSON
        val foodJson = Gson().toJson(food)

        // Створюємо Intent для переходу до EditProductsActivity
        val intent = Intent(this, EditProductsActivity::class.java)
        intent.putExtra("food_json", foodJson)
        startActivity(intent)
    }
    private fun loadTodayProgress() {
        lifecycleScope.launch {
            userSettingsRepository.getUserSettings().collect { settings ->
                if (settings != null && settings.minCalories > 0 && settings.maxCalories > 0) {
                    binding.caloriesProgressContainer.visibility = android.view.View.VISIBLE

                    // Отримуємо страви за сьогодні
                    val today = Date()

                    // Запускаємо окремий launch для другого flow
                    launch {
                        mealRepository.getMealsByDate(today).collect { meals ->
                            val totalCalories = meals.sumOf { it.totalCalories }

                            // Форматуємо текст з діапазоном норми
                            binding.tvTodayCalories.text = "Сьогодні: ${String.format("%.0f", totalCalories)} ккал / ${String.format("%.0f", settings.maxCalories)} ккал"

                            binding.caloriesProgressView.setCaloriesData(
                                settings.minCalories,
                                settings.maxCalories,
                                totalCalories
                            )

                            binding.tvStreak.text = "${settings.currentStreak} днів підряд"

                            // Перевіряємо і оновлюємо streak
                            updateStreak(settings, totalCalories, today)
                        }
                    }
                } else {
                    binding.caloriesProgressContainer.visibility = android.view.View.GONE
                }
            }
        }
    }

    private suspend fun updateStreak(settings: UserSettings, todayCalories: Double, today: Date) {
        // Якщо норми не встановлені, не оновлюємо streak
        if (settings.minCalories == 0.0 && settings.maxCalories == 0.0) {
            return
        }

        val calendar = Calendar.getInstance()
        calendar.time = today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        // Перевіряємо чи сьогодні в нормі
        val isInRange = todayCalories >= settings.minCalories && todayCalories <= settings.maxCalories

        // Якщо останнє оновлення було 0 (ніколи), це перший день
        if (settings.lastStreakDate == 0L) {
            if (isInRange && todayCalories > 0) {
                userSettingsRepository.updateStreak(1, todayStart)
            }
            return
        }

        val lastStreakCalendar = Calendar.getInstance()
        lastStreakCalendar.timeInMillis = settings.lastStreakDate
        lastStreakCalendar.set(Calendar.HOUR_OF_DAY, 0)
        lastStreakCalendar.set(Calendar.MINUTE, 0)
        lastStreakCalendar.set(Calendar.SECOND, 0)
        lastStreakCalendar.set(Calendar.MILLISECOND, 0)
        val lastStreakStart = lastStreakCalendar.timeInMillis

        val daysDifference = ((todayStart - lastStreakStart) / (1000 * 60 * 60 * 24)).toInt()

        when {
            daysDifference == 0 -> {
                // Той самий день - оновлюємо тільки якщо змінився статус норми
                if (isInRange && todayCalories > 0) {
                    // Якщо зараз в нормі, але streak = 0, встановлюємо 1
                    if (settings.currentStreak == 0) {
                        userSettingsRepository.updateStreak(1, todayStart)
                    }
                } else if (!isInRange || todayCalories == 0.0) {
                    // Якщо вийшли з норми або немає калорій
                    if (settings.currentStreak > 0) {
                        userSettingsRepository.updateStreak(0, todayStart)
                    }
                }
            }
            daysDifference == 1 -> {
                // Наступний день
                if (isInRange && todayCalories > 0) {
                    // Продовжуємо streak
                    userSettingsRepository.updateStreak(settings.currentStreak + 1, todayStart)
                } else {
                    // Скидаємо streak
                    userSettingsRepository.updateStreak(0, todayStart)
                }
            }
            daysDifference > 1 -> {
                // Пропустили дні - скидаємо streak
                if (isInRange && todayCalories > 0) {
                    userSettingsRepository.updateStreak(1, todayStart)
                } else {
                    userSettingsRepository.updateStreak(0, todayStart)
                }
            }
        }
    }

    private fun setupNavigationDrawer() {

        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        toggle.drawerArrowDrawable.color = android.graphics.Color.WHITE
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_history -> {
                startActivity(Intent(this, HistoryActivity::class.java))
            }
            R.id.nav_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Потрібен дозвіл на камеру", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openCamera() {
        startActivity(Intent(this, CameraActivity::class.java))
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun analyzeImageFromGallery(uri: Uri) {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnTakePhoto.isEnabled = false
        binding.btnSelectFromGallery.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = uriToBitmap(uri)
                val base64Image = bitmapToBase64(bitmap)
                val food = geminiService.analyzeFood(base64Image)

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnTakePhoto.isEnabled = true
                    binding.btnSelectFromGallery.isEnabled = true

                    val intent = Intent(this@MainActivity, EditProductsActivity::class.java)
                    intent.putExtra("food_json", Gson().toJson(food))
                    startActivity(intent)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnTakePhoto.isEnabled = true
                    binding.btnSelectFromGallery.isEnabled = true
                    Toast.makeText(
                        this@MainActivity,
                        "Помилка аналізу: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        val inputStream = contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(inputStream)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}