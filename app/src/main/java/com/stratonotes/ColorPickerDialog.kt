package com.stratonotes

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.Window
import android.widget.*
import com.example.punchpad2.R

class ColorPickerDialog(context: Context) : Dialog(context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    private val defaultBlue = Color.parseColor("#5D53A3")

    private var savedColor = 0             // stored in SharedPreferences
    private var previousSessionColor = 0   // remembered only during this session
    private var sessionColor = 0           // updated on dialog open or "Set App Color"
    private var currentColor = 0           // live preview as user drags or changes

    private var currentHue = 250f
    private var currentBrightness = 0.64f

    private var resetStage = 0             // 0 = ready to reset to previous, 1 = default, 2 = stop

    private lateinit var wheel: ColorWheelView
    private lateinit var brightnessSlider: SeekBar
    private lateinit var btnReset: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        setContentView(R.layout.dialog_color_picker)
        setCancelable(true)
        setCanceledOnTouchOutside(false)

        // Pull SharedPreferences value
        savedColor = prefs.getInt("app_color", defaultBlue)
        previousSessionColor = savedColor
        sessionColor = savedColor
        currentColor = savedColor

        // Initialize UI references
        wheel = findViewById(R.id.colorWheelView)
        brightnessSlider = findViewById(R.id.brightnessSlider)
        val btnAppColor = findViewById<Button>(R.id.btnAppColor)
        val btnBackgroundImage = findViewById<Button>(R.id.btnBackgroundImage)
        btnReset = findViewById(R.id.btnReset)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnCancel = findViewById<Button>(R.id.btnCancel)

        applyColor(currentColor)

        wheel.setOnColorSelectedListener(object : ColorWheelView.OnColorSelectedListener {
            override fun onColorSelected(color: Int) {
                val hsv = FloatArray(3)
                Color.colorToHSV(color, hsv)
                currentHue = hsv[0]
                currentBrightness = hsv[2]
                currentColor = color
                resetStage = 0
                updateResetButton()
            }
        })

        brightnessSlider.max = 100
        brightnessSlider.progress = (currentBrightness * 100).toInt()
        brightnessSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentBrightness = progress / 100f
                wheel.setBrightness(currentBrightness)
                currentColor = Color.HSVToColor(floatArrayOf(currentHue, 0.49f, currentBrightness))
                resetStage = 0
                updateResetButton()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnAppColor.text = "SET APP COLOR"
        btnAppColor.setOnClickListener {
            // Capture the current saved state before changing it
            previousSessionColor = savedColor

            // Update the color to the user's new selection
            currentColor = Color.HSVToColor(floatArrayOf(currentHue, 0.49f, currentBrightness))
            prefs.edit().putInt("app_color", currentColor).apply()

            // Update tracking states
            savedColor = currentColor
            sessionColor = currentColor
            resetStage = 0

            Toast.makeText(context, "App color set", Toast.LENGTH_SHORT).show()
            updateResetButton()
        }


        btnReset.setOnClickListener {
            val targetColor = when (resetStage) {
                0 -> {
                    resetStage = 1
                    previousSessionColor
                }
                1 -> {
                    resetStage = 2
                    defaultBlue
                }
                else -> return@setOnClickListener // Do nothing on third+ tap
            }

            applyColor(targetColor)
            updateColor()
            updateResetButton()

            val msg = when (resetStage) {
                1 -> "Reset to saved color"
                2 -> "Reset to default color"
                else -> ""
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener { dismiss() }
        btnCancel.setOnClickListener { dismiss() }

        updateResetButton()
    }

    private fun updateResetButton() {
        btnReset.isEnabled = true
        btnReset.text = when (resetStage) {
            0 -> "Reset"
            1 -> "Reset to Default"
            else -> "Reset"
        }
    }

    private fun applyColor(color: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        currentHue = hsv[0]
        currentBrightness = hsv[2]
        currentColor = color
        brightnessSlider.progress = (currentBrightness * 100).toInt()
        wheel.setHue(currentHue)
        wheel.setBrightness(currentBrightness)
        wheel.updateSelectorPositionFromAngle(currentHue)
        wheel.setInitialColor(color)
    }

    private fun updateColor() {
        val hsv = floatArrayOf(currentHue, 0.49f, currentBrightness)
        currentColor = Color.HSVToColor(hsv)
    }

    fun setHue(value: Float) {
        currentHue = value.coerceIn(0f, 360f)
        wheel.setHue(currentHue)
        wheel.updateSelectorPositionFromAngle(currentHue)
    }
}
