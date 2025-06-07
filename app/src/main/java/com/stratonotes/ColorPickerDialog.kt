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

    private var appColor = 0
    private var backgroundImageUri: String? = null
    private var currentHue = 250f
    private var currentBrightness = 0.64f
    private var hasModified = false
    private var resetOnce = false
    private var previousSavedColor = 0

    private val prefs: SharedPreferences =
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    private lateinit var wheel: ColorWheelView
    private lateinit var btnReset: Button

    private val defaultBlue = Color.parseColor("#5D53A3")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        setContentView(R.layout.dialog_color_picker)
        setCancelable(true)
        setCanceledOnTouchOutside(false)

        previousSavedColor = prefs.getInt("app_color", defaultBlue)
        appColor = previousSavedColor
        backgroundImageUri = prefs.getString("bg_image", null)

        wheel = findViewById(R.id.colorWheelView)
        val brightnessSlider = findViewById<SeekBar>(R.id.brightnessSlider)
        val btnAppColor = findViewById<Button>(R.id.btnAppColor)
        val btnBackgroundImage = findViewById<Button>(R.id.btnBackgroundImage)
        btnReset = findViewById(R.id.btnReset)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnCancel = findViewById<Button>(R.id.btnCancel)

        val hsv = FloatArray(3)
        Color.colorToHSV(appColor, hsv)
        currentHue = hsv[0]
        currentBrightness = hsv[2]

        wheel.setInitialColor(appColor)

        wheel.setOnColorSelectedListener(object : ColorWheelView.OnColorSelectedListener {
            override fun onColorSelected(color: Int) {
                val temp = FloatArray(3)
                Color.colorToHSV(color, temp)
                currentHue = temp[0]
                hasModified = true
                resetOnce = false
                updateResetButtonState()
                updateColor()
            }
        })

        brightnessSlider.max = 100
        brightnessSlider.progress = (currentBrightness * 100).toInt()
        brightnessSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentBrightness = progress / 100f
                wheel.setBrightness(currentBrightness)
                hasModified = true
                resetOnce = false
                updateResetButtonState()
                updateColor()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnAppColor.text = "SET APP COLOR"
        btnAppColor.setOnClickListener {
            val hsvColor = floatArrayOf(currentHue, 0.49f, currentBrightness)
            appColor = Color.HSVToColor(hsvColor)
            prefs.edit().putInt("app_color", appColor).apply()
            previousSavedColor = appColor
            Toast.makeText(context, "App color set", Toast.LENGTH_SHORT).show()
            resetOnce = false
            updateResetButtonState()
        }

        btnReset.setOnClickListener {
            val resetColor = if (!resetOnce) {
                resetOnce = true
                previousSavedColor
            } else {
                resetOnce = false
                defaultBlue
            }

            val hsvReset = FloatArray(3)
            Color.colorToHSV(resetColor, hsvReset)
            currentHue = hsvReset[0]
            currentBrightness = hsvReset[2]

            brightnessSlider.progress = (currentBrightness * 100).toInt()
            wheel.setInitialColor(resetColor)

            updateColor()
            hasModified = false
            updateResetButtonState()

            val msg = if (resetOnce) "Reset to saved color" else "Reset to default color"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener {
            prefs.edit().apply {
                putInt("app_color", appColor)
                if (backgroundImageUri != null) {
                    putString("bg_image", backgroundImageUri)
                } else {
                    remove("bg_image")
                }
                apply()
            }
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        updateColor()
    }

    private fun updateColor() {
        val hsv = floatArrayOf(currentHue, 0.49f, currentBrightness)
        appColor = Color.HSVToColor(hsv)
    }

    private fun updateResetButtonState() {
        val storedColor = prefs.getInt("app_color", defaultBlue)
        val current = Color.HSVToColor(floatArrayOf(currentHue, 0.49f, currentBrightness))
        btnReset.isEnabled = hasModified || current != storedColor || resetOnce
        btnReset.text = if (resetOnce) "Reset to Default" else "Reset"
    }

    fun setHue(value: Float) {
        currentHue = value.coerceIn(0f, 360f)
        wheel.setHue(currentHue)
        wheel.updateSelectorPositionFromAngle(currentHue)
    }
}
