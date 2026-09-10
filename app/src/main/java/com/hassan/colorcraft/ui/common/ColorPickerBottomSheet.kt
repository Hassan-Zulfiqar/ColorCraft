package com.hassan.colorcraft.ui.common

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hassan.colorcraft.databinding.BottomSheetColorPickerBinding
import com.hassan.colorcraft.ui.coloring.ColorSwatchAdapter
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ColorPickerBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetColorPickerBinding
    private val colorPickerViewModel: ColorPickerViewModel by viewModel()
    private lateinit var recentColorsAdapter: ColorSwatchAdapter

    private var currentHue: Float = 0f
    private var currentSaturation: Float = 0f
    private var currentValue: Float = 1f
    private var isUpdatingProgrammatically = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetColorPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val initialColor = arguments?.getInt(ARG_INITIAL_COLOR) ?: Color.RED

        val initialHsv = FloatArray(3)
        Color.colorToHSV(initialColor, initialHsv)
        currentHue = initialHsv[0]
        currentSaturation = initialHsv[1]
        currentValue = initialHsv[2]

        binding.hsvColorView.setSelection(currentHue, currentSaturation)
        applySliderValue(currentValue)
        setHexText(initialColor)
        updatePreviewAndHex()

        binding.closeButton.setOnClickListener { dismiss() }

        binding.hsvColorView.onColorSelected = { hue, saturation ->
            currentHue = hue
            currentSaturation = saturation
            updatePreviewAndHex()
        }

        binding.brightnessSlider.addOnChangeListener { slider, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            val range = slider.valueTo - slider.valueFrom
            currentValue = if (range == 0f) 0f else (value - slider.valueFrom) / range
            updatePreviewAndHex()
        }

        binding.hexInput.doAfterTextChanged { editable ->
            if (isUpdatingProgrammatically) return@doAfterTextChanged

            val rawText = editable?.toString().orEmpty().trim()
            if (rawText.isEmpty()) return@doAfterTextChanged

            val normalized = if (rawText.startsWith("#")) rawText else "#$rawText"
            if (normalized.length != 7) return@doAfterTextChanged

            val parsedColor = try {
                Color.parseColor(normalized)
            } catch (e: IllegalArgumentException) {
                return@doAfterTextChanged
            }

            val parsedHsv = FloatArray(3)
            Color.colorToHSV(parsedColor, parsedHsv)
            currentHue = parsedHsv[0]
            currentSaturation = parsedHsv[1]
            currentValue = parsedHsv[2]

            binding.hsvColorView.setSelection(currentHue, currentSaturation)
            applySliderValue(currentValue)
            binding.hexInputLayout.boxStrokeColor = parsedColor
        }

        recentColorsAdapter = ColorSwatchAdapter(emptyList()) { color ->
            val hsv = FloatArray(3)
            Color.colorToHSV(color, hsv)
            currentHue = hsv[0]
            currentSaturation = hsv[1]
            currentValue = hsv[2]

            binding.hsvColorView.setSelection(currentHue, currentSaturation)
            applySliderValue(currentValue)
            updatePreviewAndHex()
        }
        binding.recentColorsRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recentColorsRecyclerView.adapter = recentColorsAdapter

        colorPickerViewModel.recentColors.observe(viewLifecycleOwner) { colors ->
            recentColorsAdapter.updateColors(colors)
        }

        binding.applyColorButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                colorPickerViewModel.addRecentColor(currentColor())
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(RESULT_COLOR_KEY to currentColor())
                )
                dismiss()
            }
        }
    }

    private fun currentColor(): Int =
        Color.HSVToColor(floatArrayOf(currentHue, currentSaturation, currentValue))

    private fun updatePreviewAndHex() {
        val color = currentColor()
        binding.hexInputLayout.boxStrokeColor = color
        binding.currentColorPreview.backgroundTintList = ColorStateList.valueOf(color)

        val newHex = String.format("%06X", 0xFFFFFF and color)
        val existingHex = binding.hexInput.text?.toString().orEmpty()
        if (!existingHex.equals(newHex, ignoreCase = true)) {
            setHexText(color)
        }
    }

    private fun setHexText(color: Int) {
        isUpdatingProgrammatically = true
        val hex = String.format("%06X", 0xFFFFFF and color)
        binding.hexInput.setText(hex)
        binding.hexInput.setSelection(hex.length)
        isUpdatingProgrammatically = false
    }

    private fun applySliderValue(normalizedValue: Float) {
        val valueFrom = binding.brightnessSlider.valueFrom
        val valueTo = binding.brightnessSlider.valueTo
        binding.brightnessSlider.value = valueFrom + normalizedValue * (valueTo - valueFrom)
    }

    companion object {
        const val REQUEST_KEY = "color_picker_request"
        const val RESULT_COLOR_KEY = "result_color"
        private const val ARG_INITIAL_COLOR = "arg_initial_color"

        fun newInstance(initialColor: Int): ColorPickerBottomSheet {
            return ColorPickerBottomSheet().apply {
                arguments = bundleOf(ARG_INITIAL_COLOR to initialColor)
            }
        }
    }
}
