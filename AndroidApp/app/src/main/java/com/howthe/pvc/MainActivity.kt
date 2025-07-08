/*
 * Copyright (c) 2025 OzzyBozy
 * Custom Non‑Commercial Open Source License — see LICENSE.txt
 */

package com.howthe.pvc

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.howthe.pvc.databinding.ActivityMainBinding
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isUpdatingProgrammatically = false

    private val PREFS_NAME = "pvc_settings"
    private val KEY_THEME = "theme_preference"

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            val visibility = savedInstanceState.getInt("settingsMenuVisibility", View.GONE)
            binding.settingsMenu.visibility = visibility
        } else {
            binding.settingsMenu.visibility = View.GONE // default
        }
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val currentTheme = prefs.getString(KEY_THEME, "default")

        val darkModeSwitch = binding.darkModeSwitch
        val themeTextView = binding.themeText

        when (currentTheme) {
            "dark" -> {
                darkModeSwitch.isChecked = true
                themeTextView.background = null
                Log.d("MainActivity", "Dark theme applied")

            }
            "light" -> {
                darkModeSwitch.isChecked = false
                themeTextView.background = null
                Log.d("MainActivity", "Light theme applied")
            }
            else -> {
                themeTextView.setBackgroundResource(R.drawable.gray_border)
                Log.d("MainActivity", "Default theme applied")
            }
        }

        val priceInputs = listOf(binding.inputPriceA, binding.inputPriceB)
        val sizeInputs = listOf(binding.inputSizeA, binding.inputSizeB)

        (priceInputs + sizeInputs).forEach { editText ->
            editText.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    editText.hint = ""
                } else {
                    updateHintFor(editText)
                }
            }
            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (isUpdatingProgrammatically) return
                    recalculate()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
        val settingsLayout = binding.settingsMenu
        val settingsButton = binding.settingsButton
        val settingsExitButton = binding.settingsExitButton
        settingsButton.setOnClickListener {
            settingsLayout.visibility = View.VISIBLE
        }
        settingsExitButton.setOnClickListener {
            settingsLayout.visibility = View.GONE
        }
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ThemeUtils.setThemePreference(prefs, ThemeUtils.THEME_DARK)
                updateUIForTheme(ThemeUtils.THEME_DARK, darkModeSwitch, themeTextView)
            } else {
                ThemeUtils.setThemePreference(prefs, ThemeUtils.THEME_LIGHT)
                updateUIForTheme(ThemeUtils.THEME_LIGHT, darkModeSwitch, themeTextView)
            }
            ThemeUtils.applyTheme(this)
        }
        themeTextView.setOnClickListener {
            if (currentTheme != "default") {
                ThemeUtils.setThemePreference(prefs, ThemeUtils.THEME_SYSTEM)
                updateUIForTheme(ThemeUtils.THEME_SYSTEM, darkModeSwitch, themeTextView)
                ThemeUtils.applyTheme(this)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("settingsMenuVisibility", binding.settingsMenu.visibility)
    }

    val isRadius = true
    private fun recalculate() {
        if (isUpdatingProgrammatically) return
        isUpdatingProgrammatically = true

        val sizeA = binding.inputSizeA.text.toString().toDoubleOrNull()
        val priceA = binding.inputPriceA.text.toString().toDoubleOrNull()
        val sizeB = binding.inputSizeB.text.toString().toDoubleOrNull()
        val priceB = binding.inputPriceB.text.toString().toDoubleOrNull()

        val areaA = sizeA?.let { computeArea(it, isRadius) }
        val areaB = sizeB?.let { computeArea(it, isRadius) }

        val valueA = if (areaA != null && priceA != null) priceA / areaA else null
        val valueB = if (areaB != null && priceB != null) priceB / areaB else null

        listOf(
            binding.inputPriceA,
            binding.inputPriceB,
            binding.inputSizeA,
            binding.inputSizeB
        ).forEach { input ->
            if (!input.isFocused) {
                updateHintFor(input, valueA, valueB, areaA, areaB, isRadius)
            }
        }
        updatePizzaView(areaA, areaB)

        if (valueA != null && valueB != null) {
            val epsilon = 0.0001
            val diff = valueA - valueB
            binding.resultText.text = if (kotlin.math.abs(diff) < epsilon) {
                "Both pizzas have equal value."
            } else {
                val better = if (diff < 0) "A" else "B"
                val percent = ((kotlin.math.max(valueA, valueB) - kotlin.math.min(valueA, valueB)) / kotlin.math.min(valueA, valueB)) * 100
                "Pizza $better is ${"%.2f".format(percent)}% better value."
            }
        } else {
            binding.resultText.text = ""
        }

        isUpdatingProgrammatically = false
    }

    private fun updateHintFor(
        input: View,
        valueA: Double? = null,
        valueB: Double? = null,
        areaA: Double? = null,
        areaB: Double? = null,
        isRadius: Boolean = false
    ) {
        when (input) {
            binding.inputPriceA -> {
                if (binding.inputPriceA.text.isNullOrBlank()) {
                    if (valueB != null && areaA != null) {
                        val estPriceA = valueB * areaA
                        binding.inputPriceA.hint = "%.2f".format(estPriceA)
                    } else {
                        binding.inputPriceA.hint = "Enter price A"
                    }
                }
            }
            binding.inputPriceB -> {
                if (binding.inputPriceB.text.isNullOrBlank()) {
                    if (valueA != null && areaB != null) {
                        val estPriceB = valueA * areaB
                        binding.inputPriceB.hint = "%.2f".format(estPriceB)
                    } else {
                        binding.inputPriceB.hint = "Enter price B"
                    }
                }
            }
            binding.inputSizeA -> {
                if (binding.inputSizeA.text.isNullOrBlank()) {
                    if (binding.inputPriceA.text.toString().toDoubleOrNull() != null &&
                        binding.inputPriceB.text.toString().toDoubleOrNull() != null &&
                        binding.inputSizeB.text.toString().toDoubleOrNull() != null
                    ) {
                        val priceA = binding.inputPriceA.text.toString().toDouble()
                        val priceB = binding.inputPriceB.text.toString().toDouble()
                        val sizeB = binding.inputSizeB.text.toString().toDouble()
                        val areaBLocal = computeArea(sizeB, isRadius)
                        val valueBLocal = priceB / areaBLocal
                        val estAreaA = if (valueBLocal > 0) priceA / valueBLocal else null
                        val estSizeA = estAreaA?.let {
                            if (isRadius) sqrt(it / PI) else 2 * sqrt(it / PI)
                        }
                        binding.inputSizeA.hint = estSizeA?.let { "%.2f".format(it) } ?: "Enter size A"
                    } else {
                        binding.inputSizeA.hint = "Enter size A"
                    }
                }
            }
            binding.inputSizeB -> {
                if (binding.inputSizeB.text.isNullOrBlank()) {
                    if (binding.inputPriceA.text.toString().toDoubleOrNull() != null &&
                        binding.inputPriceB.text.toString().toDoubleOrNull() != null &&
                        binding.inputSizeA.text.toString().toDoubleOrNull() != null
                    ) {
                        val priceA = binding.inputPriceA.text.toString().toDouble()
                        val priceB = binding.inputPriceB.text.toString().toDouble()
                        val sizeA = binding.inputSizeA.text.toString().toDouble()
                        val areaALocal = computeArea(sizeA, isRadius)
                        val valueALocal = priceA / areaALocal
                        val estAreaB = if (valueALocal > 0) priceB / valueALocal else null
                        val estSizeB = estAreaB?.let {
                            if (isRadius) sqrt(it / PI) else 2 * sqrt(it / PI)
                        }
                        binding.inputSizeB.hint = estSizeB?.let { "%.2f".format(it) } ?: "Enter size B"
                    } else {
                        binding.inputSizeB.hint = "Enter size B"
                    }
                }
            }
        }
    }

    private fun computeArea(value: Double, isRadius: Boolean): Double {
        val radius = if (isRadius) value else value / 2.0
        return PI * radius.pow(2.0)
    }

    private fun updatePizzaView(areaA: Double?, areaB: Double?) {
        if (areaA != null && areaB != null) {
            if (areaA > areaB) {
                val scaleA = 1f
                val scaleB = sqrt(areaB / areaA).toFloat()
                binding.pizzaA.scaleX = scaleA
                binding.pizzaA.scaleY = scaleA
                binding.pizzaB.scaleX = scaleB
                binding.pizzaB.scaleY = scaleB
            } else {
                val scaleB = 1f
                val scaleA = sqrt(areaA / areaB).toFloat()
                binding.pizzaA.scaleX = scaleA
                binding.pizzaA.scaleY = scaleA
                binding.pizzaB.scaleX = scaleB
                binding.pizzaB.scaleY = scaleB
            }
        }
    }
    private fun updateUIForTheme(theme: String, switch: Switch, textView: TextView) {
        when (theme) {
            ThemeUtils.THEME_SYSTEM -> {
                switch.isChecked = false
                setTextViewBorder(textView, true)
            }
            else -> {
                switch.isChecked = theme == ThemeUtils.THEME_DARK
                setTextViewBorder(textView, false)
            }
        }
    }

    private fun setTextViewBorder(textView: TextView, showBorder: Boolean) {
        if (showBorder) {
            textView.setBackgroundResource(R.drawable.gray_border)
        } else {
            textView.background = null
        }
    }

    object ThemeUtils {
        private const val KEY_THEME = "theme_preference"

        const val THEME_SYSTEM = "default"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        fun getThemePreference(prefs: SharedPreferences): String {
            return prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        }

        fun setThemePreference(prefs: SharedPreferences, theme: String) {
            prefs.edit() { putString(KEY_THEME, theme) }
        }

        fun applyTheme(activity: AppCompatActivity) {
            val prefs = activity.getSharedPreferences("pvc_settings", MODE_PRIVATE)
            val selectedTheme = getThemePreference(prefs)

            val newMode = when (selectedTheme) {
                THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }

            val lastMode = prefs.getInt("last_mode_applied", -1)

            if (lastMode != newMode && newMode != -1) {
                prefs.edit() { putInt("last_mode_applied", newMode) }
                AppCompatDelegate.setDefaultNightMode(newMode)
                activity.recreate()
            }else if (lastMode != newMode && newMode == -1){
                prefs.edit() { putInt("last_mode_applied", newMode) }
                AppCompatDelegate.setDefaultNightMode(newMode)
                }
        }


    }
}
