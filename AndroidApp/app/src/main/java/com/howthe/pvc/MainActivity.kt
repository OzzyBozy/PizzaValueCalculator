/*
 * Copyright (c) 2025 OzzyBozy
 * Custom Non‑Commercial Open Source License — see LICENSE.txt
 */

package com.howthe.pvc

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.howthe.pvc.databinding.ActivityMainBinding
import java.util.Locale
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isUpdatingProgrammatically = false
    private var sliceAValue: Double = 1.0
    private var sliceBValue: Double = 1.0
    private var areAchievementsEnabled = false

    private val seekBarLabels = listOf(
        "1/18",
        "1/9",
        "1/6",
        "2/9",
        "5/18",
        "2/6",
        "7/18",
        "4/9",
        "3/6",
        "5/9",
        "11/18",
        "4/6",
        "13/18",
        "7/9",
        "5/6",
        "8/9",
        "17/18",
        "18/18"
    )

    companion object {
        private var hasCountedLaunch = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.initializeDayNightMode(this)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pizzaA.foreground =
            ContextCompat.getDrawable(this, R.drawable.pizza_overlay_layer_drawable)
        binding.pizzaB.foreground =
            ContextCompat.getDrawable(this, R.drawable.pizza_overlay_layer_drawable)

        if (savedInstanceState != null) {
            val visibility = savedInstanceState.getInt("settingsMenuVisibility", View.GONE)
            binding.settingsMenu.visibility = visibility
        } else {
            binding.settingsMenu.visibility = View.GONE
        }

        val prefs = getSharedPreferences(ThemeUtils.PREFS_NAME, MODE_PRIVATE)
        val currentTheme = prefs.getString(ThemeUtils.KEY_THEME, ThemeUtils.THEME_SYSTEM)
            ?: ThemeUtils.THEME_SYSTEM

        val darkModeSwitch = binding.darkModeSwitch
        when (currentTheme) {
            ThemeUtils.THEME_DARK -> {
                darkModeSwitch.isChecked = true
            }

            ThemeUtils.THEME_LIGHT -> {
                darkModeSwitch.isChecked = false
            }

            ThemeUtils.THEME_SYSTEM -> {
                val uiModeNight =
                    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                darkModeSwitch.isChecked = (uiModeNight == Configuration.UI_MODE_NIGHT_YES)
            }
        }

        val advancedSwitch = binding.advancedSwitch
        val advancedSettingsLayout = binding.advancedSettingsLayout
        val isAdvancedMode = prefs.getBoolean(ThemeUtils.KEY_ADVANCED_MODE, false)
        advancedSwitch.isChecked = isAdvancedMode
        advancedSettingsLayout.visibility = if (isAdvancedMode) View.VISIBLE else View.GONE

        advancedSwitch.setOnCheckedChangeListener { _, isChecked ->
            advancedSettingsLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            prefs.edit { putBoolean(ThemeUtils.KEY_ADVANCED_MODE, isChecked) }
            if (!isChecked) {
                binding.sliceSeekBarA.progress = 17
                updateSliceValueAndText(18, binding.sliceSeekBarAValue, true)
                binding.sliceSeekBarB.progress = 17
                updateSliceValueAndText(18, binding.sliceSeekBarBValue, false)
                recalculate()
                if (binding.achievementSwitch.isChecked) {
                    binding.achievementSwitch.isChecked = false
                }
            }
        }

        val achievementSwitch = binding.achievementSwitch
        areAchievementsEnabled = prefs.getBoolean(ThemeUtils.KEY_ACHIEVEMENTS_ENABLED, false)
        achievementSwitch.isChecked = areAchievementsEnabled
        if (!hasCountedLaunch) {
            hasCountedLaunch = true
            if (areAchievementsEnabled) {
                val achievements = Achievements.getInstance()
                listOf("customer1", "customer2", "customer3", "customer4", "customer5").forEach {
                    achievements.increment(this, it)
                }
            }
        }

        achievementSwitch.setOnCheckedChangeListener { _, isChecked ->
            areAchievementsEnabled = isChecked
            prefs.edit { putBoolean(ThemeUtils.KEY_ACHIEVEMENTS_ENABLED, isChecked) }
            if (isChecked) {
                val achievements = Achievements.getInstance()
                achievements.unlock(this, "enable_achievements")
            }
        }

        val sliceSeekBarA = binding.sliceSeekBarA
        val sliceTextViewA = binding.sliceSeekBarAValue
        sliceSeekBarA.max = 17
        val initialSliceAProgress = savedInstanceState?.getInt("sliceAProgress", 17) ?: 17
        sliceSeekBarA.progress = initialSliceAProgress
        updateSliceValueAndText(initialSliceAProgress + 1, sliceTextViewA, true)

        sliceSeekBarA.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                currentProgress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    val actualProgress = currentProgress + 1
                    updateSliceValueAndText(actualProgress, sliceTextViewA, true)
                    if (binding.inputSizeA.text.isNotBlank() && binding.inputSizeB.text.isNotBlank()) {
                        recalculate()
                    }
                    if (areAchievementsEnabled) {
                        val achievements = Achievements.getInstance()
                        listOf("slicer1", "slicer2", "slicer3", "slicer4").forEach {
                            achievements.increment(this@MainActivity, it)
                        }
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val sliceSeekBarB = binding.sliceSeekBarB
        val sliceTextViewB = binding.sliceSeekBarBValue
        sliceSeekBarB.max = 17
        val initialSliceBProgress = savedInstanceState?.getInt("sliceBProgress", 17) ?: 17
        sliceSeekBarB.progress = initialSliceBProgress
        updateSliceValueAndText(initialSliceBProgress + 1, sliceTextViewB, false)

        sliceSeekBarB.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                currentProgress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    val actualProgress = currentProgress + 1
                    updateSliceValueAndText(actualProgress, sliceTextViewB, false)
                    recalculate()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

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

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
        val labels = listOf(
            binding.labelSizeA,
            binding.labelPriceA,
            binding.labelSizeB,
            binding.labelPriceB
        )

        binding.root.post {
            var maxWidth = 0
            labels.forEach { tv ->
                tv.measure(
                    View.MeasureSpec.UNSPECIFIED,
                    View.MeasureSpec.UNSPECIFIED
                )
                val width = tv.measuredWidth
                if (width > maxWidth) maxWidth = width
            }
            labels.forEach { tv ->
                tv.layoutParams?.width = maxWidth
                tv.requestLayout()
            }
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
            if (areAchievementsEnabled) {
                Achievements.getInstance().unlock(this, "darkmode")
            }
            ThemeUtils.applyThemeChangeAndRecreate(this)
        }

        val languageCodes =
            listOf(
                "ar",
                "de",
                "elv",
                "en",
                "es",
                "fr",
                "hi",
                "it",
                "ja",
                "ko",
                "la",
                "ru",
                "tr",
                "zh"
            )
        val languages = listOf(
            "العربية",
            "Deutsch",
            "Elvish",
            "English",
            "Español",
            "Français",
            " हिंदी",
            "Italiano",
            "日本語",
            "한국어",
            "Latina",
            "Русский",
            "Türkçe",
            "简体中文"
        )
        val langPrefs = getSharedPreferences(ThemeUtils.PREFS_NAME, MODE_PRIVATE)
        val savedLang = langPrefs.getString("app_language", "en")

        val langAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.languageSpinner.adapter = langAdapter

        val savedIndex = languageCodes.indexOf(savedLang)
        if (savedIndex != -1) {
            binding.languageSpinner.setSelection(savedIndex)
        }

        binding.languageSpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedLangCode = languageCodes[position]
                    if (selectedLangCode != savedLang) {
                        langPrefs.edit { putString("app_language", selectedLangCode) }
                        LocaleUtils.setLocale(this@MainActivity, selectedLangCode)
                        if (areAchievementsEnabled) {
                            Achievements.getInstance().unlock(this@MainActivity, "multilingual")
                        }
                        recreate()
                    }
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
            }
        val achievementsList = Achievements.getInstance().getAll(this)

        val recyclerView = findViewById<RecyclerView>(R.id.achievementsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = AchievementsAdapter(this, achievementsList)

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
        outState.putInt("sliceAProgress", binding.sliceSeekBarA.progress)
        outState.putInt("sliceBProgress", binding.sliceSeekBarB.progress)
    }

    private fun updateSliceValueAndText(progress: Int, textView: TextView, isPizzaA: Boolean) {
        val normalizedValue = progress / 18.0
        if (isPizzaA) {
            sliceAValue = normalizedValue
        } else {
            sliceBValue = normalizedValue
        }
        val labelIndex = progress - 1
        if (labelIndex in seekBarLabels.indices) {
            textView.text = seekBarLabels[labelIndex]
        }


        val pizzaView = if (isPizzaA) binding.pizzaA else binding.pizzaB
        val overlayDrawable = pizzaView.foreground as? LayerDrawable
        overlayDrawable?.let {
            val totalSlices = it.numberOfLayers
            for (i in 0 until totalSlices) {
                val slice = it.getDrawable(i)
                slice.alpha = if (i < totalSlices - progress) 255 else 0
            }
        }
    }


    private fun recalculate() {
        if (isUpdatingProgrammatically) return
        isUpdatingProgrammatically = true

        val sizeA = binding.inputSizeA.text.toString().toDoubleOrNull()
        val priceA = binding.inputPriceA.text.toString().toDoubleOrNull()
        val sizeB = binding.inputSizeB.text.toString().toDoubleOrNull()
        val priceB = binding.inputPriceB.text.toString().toDoubleOrNull()

        val rawAreaA = sizeA?.let { computeArea(it) }
        val rawAreaB = sizeB?.let { computeArea(it) }

        val areaA = rawAreaA?.let { it * sliceAValue }
        val areaB = rawAreaB?.let { it * sliceBValue }

        val valueA = if (areaA != null && priceA != null && areaA > 0.0) priceA / areaA else null
        val valueB = if (areaB != null && priceB != null && areaB > 0.0) priceB / areaB else null

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
        updatePizzaView(rawAreaA, rawAreaB)

        if (valueA != null && valueB != null) {
            val epsilon = 0.0001
            val diff = valueA - valueB
            binding.resultText.text = if (kotlin.math.abs(diff) < epsilon) {
                if (areAchievementsEnabled) {
                    Achievements.getInstance().unlock(this, "balanced")
                }
                getString(R.string.equal_value)
            } else {
                val better = if (diff < 0) "A" else "B"
                val percent = ((kotlin.math.max(valueA, valueB) - kotlin.math.min(
                    valueA,
                    valueB
                )) / kotlin.math.min(valueA, valueB)) * 100
                getString(R.string.better_pizza1) + better + getString(R.string.better_pizza2) + "%.2f".format(
                    percent
                ) + getString(R.string.better_pizza3)
            }
            if (areAchievementsEnabled) {
                if (valueA.isInfinite() || valueB.isInfinite()) {
                    Achievements.getInstance().unlock(this, "infinity")
                }
                if ((valueA == 0.0) || (valueB == 0.0)) {
                    Achievements.getInstance().unlock(this, "zero")
                }
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
                    if (valueB != null && areaA != null && areaA > 0.0) {
                        val estPriceA = valueB * areaA
                        binding.inputPriceA.hint = "%.2f".format(estPriceA)
                    } else {
                        binding.inputPriceA.hint = getString(R.string.enter_price)
                    }
                }
            }

            binding.inputPriceB -> {
                if (binding.inputPriceB.text.isNullOrBlank()) {
                    if (valueA != null && areaB != null && areaB > 0.0) {
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
                        val priceAVal = binding.inputPriceA.text.toString().toDouble()
                        val priceBVal = binding.inputPriceB.text.toString().toDouble()
                        val sizeBVal = binding.inputSizeB.text.toString().toDouble()

                        val rawAreaBLocal = computeArea(sizeBVal)
                        val areaBLocal = rawAreaBLocal * sliceBValue

                        if (areaBLocal > 0) {
                            val valueBLocal = priceBVal / areaBLocal
                            if (valueBLocal > 0) {
                                val targetAreaA = priceAVal / valueBLocal
                                val estRawAreaA = targetAreaA / sliceAValue
                                val estSizeA = sqrt(estRawAreaA / PI)
                                binding.inputSizeA.hint = "%.2f".format(estSizeA)
                            } else {
                                binding.inputSizeA.hint = getString(R.string.enter_size)
                            }
                        } else {
                            binding.inputSizeA.hint = getString(R.string.enter_size)
                        }
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
                        val priceAVal = binding.inputPriceA.text.toString().toDouble()
                        val priceBVal = binding.inputPriceB.text.toString().toDouble()
                        val sizeAVal = binding.inputSizeA.text.toString().toDouble()

                        val rawAreaALocal = computeArea(sizeAVal)
                        val areaALocal = rawAreaALocal * sliceAValue

                        if (areaALocal > 0) {
                            val valueALocal = priceAVal / areaALocal
                            if (valueALocal > 0) {
                                val targetAreaB = priceBVal / valueALocal
                                val estRawAreaB = targetAreaB / sliceBValue
                                val estSizeB = sqrt(estRawAreaB / PI)
                                binding.inputSizeB.hint = "%.2f".format(estSizeB)
                            } else {
                                binding.inputSizeB.hint = getString(R.string.enter_size)
                            }
                        } else {
                            binding.inputSizeB.hint = getString(R.string.enter_size)
                        }
                    } else {
                        binding.inputSizeB.hint = getString(R.string.enter_size)
                    }
                }
            }
        }
    }

    private fun computeArea(value: Double): Double {
        return PI * value.pow(2.0)
    }

    private fun updatePizzaView(rawAreaA: Double?, rawAreaB: Double?) {
        if (rawAreaA != null && rawAreaB != null) {
            if (rawAreaA == 0.0 && rawAreaB == 0.0) {
                binding.pizzaA.scaleX = 1f
                binding.pizzaA.scaleY = 1f
                binding.pizzaB.scaleX = 1f
                binding.pizzaB.scaleY = 1f
                return
            }
            if (rawAreaA > rawAreaB) {
                val scaleA = 1f
                val scaleB = if (rawAreaA > 0) sqrt(rawAreaB / rawAreaA).toFloat() else 1f
                binding.pizzaA.scaleX = scaleA
                binding.pizzaA.scaleY = scaleA
                binding.pizzaB.scaleX = scaleB
                binding.pizzaB.scaleY = scaleB
            } else {
                val scaleB = 1f
                val scaleA = if (rawAreaB > 0) sqrt(rawAreaA / rawAreaB).toFloat() else 1f
                binding.pizzaA.scaleX = scaleA
                binding.pizzaA.scaleY = scaleA
                binding.pizzaB.scaleX = scaleB
                binding.pizzaB.scaleY = scaleB
            }
        } else if (rawAreaA != null) {
            binding.pizzaA.scaleX = 1f
            binding.pizzaA.scaleY = 1f
            binding.pizzaB.scaleX = 1f
            binding.pizzaB.scaleY = 1f
        } else if (rawAreaB != null) {
            binding.pizzaA.scaleX = 1f
            binding.pizzaA.scaleY = 1f
            binding.pizzaB.scaleX = 1f
            binding.pizzaB.scaleY = 1f
        } else {
            binding.pizzaA.scaleX = 1f
            binding.pizzaA.scaleY = 1f
            binding.pizzaB.scaleX = 1f
            binding.pizzaB.scaleY = 1f
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
        const val KEY_ACHIEVEMENTS_ENABLED = "achievements_enabled_preference"

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
        val locale = Locale.forLanguageTag(language)
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
