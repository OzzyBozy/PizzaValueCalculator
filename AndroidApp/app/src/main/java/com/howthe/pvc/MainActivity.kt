package com.howthe.pvc

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.howthe.pvc.databinding.ActivityMainBinding
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var advancedMode = false
    private var isUpdatingProgrammatically = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.switchAdvanced.setOnCheckedChangeListener { _, isChecked ->
            advancedMode = isChecked
            toggleAdvancedMode(isChecked)
            recalculate()
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

        listOf(binding.inputFractionA, binding.inputFractionB).forEach { editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (isUpdatingProgrammatically) return
                    recalculate()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        binding.radioRadius.setOnCheckedChangeListener { _, _ -> recalculate() }

        toggleAdvancedMode(advancedMode)
    }

    private fun toggleAdvancedMode(enabled: Boolean) {
        binding.layoutAdvanced.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun recalculate() {
        if (isUpdatingProgrammatically) return
        isUpdatingProgrammatically = true

        val isRadius = binding.radioRadius.isChecked

        val sizeA = binding.inputSizeA.text.toString().toDoubleOrNull()
        val priceA = binding.inputPriceA.text.toString().toDoubleOrNull()
        val sizeB = binding.inputSizeB.text.toString().toDoubleOrNull()
        val priceB = binding.inputPriceB.text.toString().toDoubleOrNull()
        val fractionA = if (advancedMode) binding.inputFractionA.text.toString().toDoubleOrNull() ?: 1.0 else 1.0
        val fractionB = if (advancedMode) binding.inputFractionB.text.toString().toDoubleOrNull() ?: 1.0 else 1.0

        val areaA = sizeA?.let { computeArea(it, isRadius) }?.times(fractionA)
        val areaB = sizeB?.let { computeArea(it, isRadius) }?.times(fractionB)

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
                "Pizza $better is ${"%.1f".format(percent)}% better value."
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
}
