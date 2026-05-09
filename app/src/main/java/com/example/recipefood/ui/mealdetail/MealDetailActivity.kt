package com.example.recipefood.ui.mealdetail

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.recipefood.R
import com.example.recipefood.data.MealRepository
import com.example.recipefood.data.RecipeDatabase
import com.example.recipefood.data.UserSettingsRepository
import com.example.recipefood.databinding.ActivityMealDetailBinding
import com.example.recipefood.model.Product
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MealDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMealDetailBinding
    private lateinit var repository: MealRepository
    private lateinit var userSettingsRepository: UserSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMealDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = RecipeDatabase.getDatabase(this)
        repository = MealRepository(database.mealDao())
        userSettingsRepository = UserSettingsRepository(database.userSettingsDao())

        val mealId = intent.getLongExtra("meal_id", -1)
        if (mealId != -1L) loadMealDetails(mealId)

        setupToolbar()
    }

    private fun setupToolbar() {
        // toolbar is hidden in v3 layout; use back link instead
        try {
            binding.btnBackLink.setOnClickListener { finish() }
        } catch (_: Exception) {}
    }

    private fun loadMealDetails(mealId: Long) {
        lifecycleScope.launch {
            val meal = repository.getMealById(mealId)
            meal?.let {
                binding.tvFoodName.text = it.name
                val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("uk"))
                binding.tvMealDate.text = dateFormat.format(Date(it.date))
                binding.tvCalories.text = String.format("%.0f", it.totalCalories)
                binding.tvProteins.text = "${String.format("%.1f", it.totalProteins)}г"
                binding.tvFats.text = "${String.format("%.1f", it.totalFats)}г"
                binding.tvCarbs.text = "${String.format("%.1f", it.totalCarbs)}г"

                setupNutritionChart(it.totalProteins, it.totalFats, it.totalCarbs)
                setupGoalContribution(it.totalCalories)

                val gson = Gson()
                val productListType = object : TypeToken<List<Product>>() {}.type
                val products: List<Product> = gson.fromJson(it.products, productListType)
                
                binding.tvProductCount.text = products.size.toString()
                binding.tvTotalWeight.text = "${products.sumOf { p -> p.weight }}г"
                
                displayProducts(products)
            }
        }
    }

    private fun setupGoalContribution(mealCalories: Double) {
        lifecycleScope.launch {
            val settings = userSettingsRepository.getUserSettingsSync()
            val target = settings?.targetCalories?.takeIf { it > 0 } ?: 2000.0
            val percent = ((mealCalories / target) * 100).toInt()
            binding.tvContributionPercent.text = "$percent%"
            binding.goalProgressBar.progress = percent.coerceIn(0, 100)
        }
    }

    private fun setupNutritionChart(proteins: Double, fats: Double, carbs: Double) {
        val p = proteins.toFloat()
        val f = fats.toFloat()
        val c = carbs.toFloat()
        
        if (p + f + c <= 0f) {
            binding.chartCard.visibility = View.GONE
            return
        }
        binding.chartCard.visibility = View.VISIBLE

        val total = p + f + c
        val pPct = if (total > 0) (p / total) * 100 else 0f
        val fPct = if (total > 0) (f / total) * 100 else 0f
        val cPct = if (total > 0) (c / total) * 100 else 0f

        val entries = listOf(
            PieEntry(p, if (pPct >= 10f) "Білки" else ""),
            PieEntry(f, if (fPct >= 10f) "Жири" else ""),
            PieEntry(c, if (cPct >= 10f) "Вугл." else "")
        )
        
        val sliceColors = listOf(
            Color.parseColor("#4CAF50"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#2196F3")
        )
        
        val dataSet = PieDataSet(entries, "").apply {
            colors = sliceColors
            valueTextColor = Color.WHITE
            valueTextSize = 12f
            valueTypeface = Typeface.DEFAULT_BOLD
            sliceSpace = 2f
            yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
            xValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
        }
        
        val data = PieData(dataSet).apply {
            setValueFormatter(object : PercentFormatter(binding.macrosPieChart) {
                override fun getFormattedValue(value: Float): String {
                    return if (value < 10f) "" else super.getFormattedValue(value)
                }
            })
            setValueTextColor(Color.WHITE)
            setValueTextSize(12f)
            setValueTypeface(Typeface.DEFAULT_BOLD)
        }
        
        binding.macrosPieChart.apply {
            this.data = data
            setUsePercentValues(true)
            description.isEnabled = false
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 40f
            transparentCircleRadius = 44f
            setTransparentCircleColor(Color.parseColor("#22FFFFFF"))
            setDrawCenterText(false)
            setExtraOffsets(0f, 0f, 0f, 0f)

            legend.isEnabled = false

            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(11f)
            setEntryLabelTypeface(Typeface.DEFAULT_BOLD)
            setDrawEntryLabels(true)
            animateY(600)
            invalidate()
        }

        tintLegendDot(binding.legendDotProtein, Color.parseColor("#4CAF50"))
        tintLegendDot(binding.legendDotFat, Color.parseColor("#FF9800"))
        tintLegendDot(binding.legendDotCarbs, Color.parseColor("#2196F3"))
        binding.tvLegendProtein.text = "Білки ${pPct.toInt()}%"
        binding.tvLegendFat.text = "Жири ${fPct.toInt()}%"
        binding.tvLegendCarbs.text = "Вугл. ${cPct.toInt()}%"
    }

    private fun tintLegendDot(view: View, color: Int) {
        val shape = GradientDrawable().apply {
            this.shape = GradientDrawable.OVAL
            setColor(color)
        }
        view.background = shape
    }

    private fun displayProducts(products: List<Product>) {
        binding.productsContainer.removeAllViews()
        products.forEach { product ->
            val itemView = layoutInflater.inflate(R.layout.item_product, binding.productsContainer, false)
            itemView.findViewById<android.widget.TextView>(R.id.tvProductName).text = product.name
            itemView.findViewById<android.widget.TextView>(R.id.tvProductWeight).text = "${product.weight} г"
            val nutritionLayout = itemView.findViewById<android.widget.LinearLayout>(R.id.nutritionLayout)
            nutritionLayout.visibility = android.view.View.VISIBLE
            itemView.findViewById<android.widget.ImageButton>(R.id.btnEdit).visibility = android.view.View.GONE
            itemView.findViewById<android.widget.ImageButton>(R.id.btnDelete).visibility = android.view.View.GONE
            product.nutrition?.let { n ->
                itemView.findViewById<android.widget.TextView>(R.id.tvCalories).text = "Калорії: ${String.format("%.1f", n.calories)} ккал"
                itemView.findViewById<android.widget.TextView>(R.id.tvProteins).text = "Білки: ${String.format("%.1f", n.proteins)} г"
                itemView.findViewById<android.widget.TextView>(R.id.tvFats).text = "Жири: ${String.format("%.1f", n.fats)} г"
                itemView.findViewById<android.widget.TextView>(R.id.tvCarbs).text = "Вуглеводи: ${String.format("%.1f", n.carbs)} г"
            }
            binding.productsContainer.addView(itemView)
        }
    }
}
