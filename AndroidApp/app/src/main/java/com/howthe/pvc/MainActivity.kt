/*
 * Copyright (c) 2025 OzzyBozy
 * Custom Non‑Commercial Open Source License — see LICENSE.txt
 */

package com.howthe.pvc

import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.howthe.pvc.databinding.ActivityMainBinding
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt
import androidx.core.content.edit
import android.content.Context
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isGone
import androidx.core.view.isVisible
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isUpdatingProgrammatically = false

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.initializeDayNightMode(this)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            val visibility = savedInstanceState.getInt("settingsMenuVisibility", View.GONE)
            binding.settingsMenu.visibility = visibility
        } else {
            binding.settingsMenu.visibility = View.GONE
        }

        val prefs = getSharedPreferences(ThemeUtils.PREFS_NAME, MODE_PRIVATE)
        val currentTheme = prefs.getString(ThemeUtils.KEY_THEME, ThemeUtils.THEME_SYSTEM) ?: ThemeUtils.THEME_SYSTEM

        val darkModeSwitch = binding.darkModeSwitch
        when (currentTheme) {
            ThemeUtils.THEME_DARK -> {
                darkModeSwitch.isChecked = true
            }
            ThemeUtils.THEME_LIGHT -> {
                darkModeSwitch.isChecked = false
            }
            ThemeUtils.THEME_SYSTEM -> {
                val uiModeNight = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                darkModeSwitch.isChecked = (uiModeNight == Configuration.UI_MODE_NIGHT_YES)
            }
        }

        val advancedSwitch = binding.advancedSwitch
        val advancedSettingsLayout = binding.advancedSettingsLayout
        val isAdvancedMode = prefs.getBoolean(ThemeUtils.KEY_ADVANCED_MODE, false) // Default to false
        advancedSwitch.isChecked = isAdvancedMode
        advancedSettingsLayout.visibility = if (isAdvancedMode) View.VISIBLE else View.GONE

        advancedSwitch.setOnCheckedChangeListener { _, isChecked ->
            advancedSettingsLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            prefs.edit { putBoolean(ThemeUtils.KEY_ADVANCED_MODE, isChecked) }
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
            val newThemePreference = if (isChecked) {
                ThemeUtils.THEME_DARK
            } else {
                ThemeUtils.THEME_LIGHT
            }
            prefs.edit { putString(ThemeUtils.KEY_THEME, newThemePreference) }
            ThemeUtils.applyThemeChangeAndRecreate(this)
        }

        val languageCodes = listOf("en", "it", "de", "fr", "tr", "ar", "es", "hi", "ja", "ko", "ru", "zh", "elv")
        val languages = listOf("English", "Italiano", "Deutsch", "Français", "Türkçe", "العربية", "Español", "हिंदी", "日本語", "한국어", "Русский", "简体中文", "Elvish")
        val langPrefs = getSharedPreferences(ThemeUtils.PREFS_NAME, MODE_PRIVATE)
        val savedLang = langPrefs.getString("app_language", "en")

        val langAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.languageSpinner.adapter = langAdapter

        val savedIndex = languageCodes.indexOf(savedLang)
        if (savedIndex != -1) {
            binding.languageSpinner.setSelection(savedIndex)
        }

        binding.languageSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedLangCode = languageCodes[position]
                if (selectedLangCode != savedLang) {
                    langPrefs.edit { putString("app_language", selectedLangCode) }
                    LocaleUtils.setLocale(this@MainActivity, selectedLangCode)
                    recreate()
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.settingsMenu.isVisible) {
                    binding.settingsMenu.isGone = true
                } else {
                    isEnabled = false
                    this@MainActivity.onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
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
                updateHintFor(input, valueA, valueB, areaA, areaB)
            }
        }
        updatePizzaView(areaA, areaB)

        if (valueA != null && valueB != null) {
            val epsilon = 0.0001
            val diff = valueA - valueB
            binding.resultText.text = if (kotlin.math.abs(diff) < epsilon) {
                getString(R.string.equal_value)
            } else {
                val better = if (diff < 0) "A" else "B"
                val percent = ((kotlin.math.max(valueA, valueB) - kotlin.math.min(valueA, valueB)) / kotlin.math.min(valueA, valueB)) * 100
                getString(R.string.better_pizza1) + better + getString(R.string.better_pizza2) + "%.2f".format(percent) + getString(R.string.better_pizza3)
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
    ) {
        when (input) {
            binding.inputPriceA -> {
                if (binding.inputPriceA.text.isNullOrBlank()) {
                    if (valueB != null && areaA != null) {
                        val estPriceA = valueB * areaA
                        binding.inputPriceA.hint = "%.2f".format(estPriceA)
                    } else {
                        binding.inputPriceA.hint = getString(R.string.enter_price)
                    }
                }
            }
            binding.inputPriceB -> {
                if (binding.inputPriceB.text.isNullOrBlank()) {
                    if (valueA != null && areaB != null) {
                        val estPriceB = valueA * areaB
                        binding.inputPriceB.hint = "%.2f".format(estPriceB)
                    } else {
                        binding.inputPriceB.hint = getString(R.string.enter_price)
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
                        val areaBLocal = computeArea(sizeB, this.isRadius)
                        val valueBLocal = priceB / areaBLocal
                        val estAreaA = if (valueBLocal > 0) priceA / valueBLocal else null
                        val estSizeA = estAreaA?.let {
                            if (this.isRadius) sqrt(it / PI) else 2 * sqrt(it / PI)
                        }
                        binding.inputSizeA.hint = estSizeA?.let { "%.2f".format(it) } ?: getString(R.string.enter_size)
                    } else {
                        binding.inputSizeA.hint = getString(R.string.enter_size)
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
                        val areaALocal = computeArea(sizeA, this.isRadius)
                        val valueALocal = priceA / areaALocal
                        val estAreaB = if (valueALocal > 0) priceB / valueALocal else null
                        val estSizeB = estAreaB?.let {
                            if (this.isRadius) sqrt(it / PI) else 2 * sqrt(it / PI)
                        }
                        binding.inputSizeB.hint = estSizeB?.let { "%.2f".format(it) } ?: getString(R.string.enter_size)
                    } else {
                        binding.inputSizeB.hint = getString(R.string.enter_size)
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
            if (areaA == 0.0 && areaB == 0.0) return
            if (areaA > areaB) {
                val scaleA = 1f
                val scaleB = if (areaA > 0) sqrt(areaB / areaA).toFloat() else 0f
                binding.pizzaA.scaleX = scaleA
                binding.pizzaA.scaleY = scaleA
                binding.pizzaB.scaleX = scaleB
                binding.pizzaB.scaleY = scaleB
            } else {
                val scaleB = 1f
                val scaleA = if (areaB > 0) sqrt(areaA / areaB).toFloat() else 0f
                binding.pizzaA.scaleX = scaleA
                binding.pizzaA.scaleY = scaleA
                binding.pizzaB.scaleX = scaleB
                binding.pizzaB.scaleY = scaleB
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val context = LocaleUtils.applySavedLocale(newBase)
        super.attachBaseContext(context)
    }

    object ThemeUtils {
        const val PREFS_NAME = "pvc_settings"
        const val KEY_THEME = "theme_preference"
        const val KEY_ADVANCED_MODE = "advanced_mode_preference"

        const val THEME_SYSTEM = "default"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        fun initializeDayNightMode(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val selectedTheme = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM

            val nightMode = when (selectedTheme) {
                THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }

        fun applyThemeChangeAndRecreate(activity: AppCompatActivity) {
            val prefs = activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val selectedTheme = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM

            val newMode = when (selectedTheme) {
                THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(newMode)
            activity.recreate()
        }
    }
}

object LocaleUtils {
    private const val PREFS_NAME = "pvc_settings"

    fun setLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    fun applySavedLocale(base: Context): Context {
        val prefs = base.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val langCode = prefs.getString("app_language", "en") ?: "en"
        return setLocale(base, langCode)
    }
}
