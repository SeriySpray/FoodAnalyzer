package com.foodanalyzer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.foodanalyzer.models.Converters
import com.foodanalyzer.models.SavedMeal
import com.foodanalyzer.models.UserSettings

@Database(
    entities = [SavedMeal::class, UserSettings::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_settings (
                        id INTEGER PRIMARY KEY NOT NULL,
                        minCalories REAL NOT NULL,
                        maxCalories REAL NOT NULL,
                        currentStreak INTEGER NOT NULL,
                        lastStreakDate INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Створюємо нову таблицю з правильною схемою
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_settings_new (
                        id INTEGER PRIMARY KEY NOT NULL,
                        targetCalories REAL NOT NULL DEFAULT 0,
                        deviationCalories REAL NOT NULL DEFAULT 0,
                        currentStreak INTEGER NOT NULL DEFAULT 0,
                        lastStreakDate INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                // Копіюємо дані, конвертуючи min/max → target/deviation
                database.execSQL("""
                    INSERT INTO user_settings_new (id, targetCalories, deviationCalories, currentStreak, lastStreakDate)
                    SELECT id,
                           (minCalories + maxCalories) / 2.0,
                           (maxCalories - minCalories) / 2.0,
                           currentStreak,
                           lastStreakDate
                    FROM user_settings
                """.trimIndent())
                // Видаляємо стару таблицю і перейменовуємо нову
                database.execSQL("DROP TABLE user_settings")
                database.execSQL("ALTER TABLE user_settings_new RENAME TO user_settings")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "food_analyzer_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
