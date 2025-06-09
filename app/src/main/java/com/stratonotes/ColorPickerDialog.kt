package com.stratonotes

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
import com.example.punchpad2.R

class ColorPickerDialog(context: Context, private val rootView: View?) : Dialog(context) {


    private val prefs: SharedPreferences =
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    // Real default color = #444588
    private val defaultHex = "#444588"
    private val defaultColor = Color.parseColor(defaultHex)
    private val defaultHSV = FloatArray(3).also {
        Color.colorToHSV(defaultColor, it)
    }

    private var savedColor = 0              // stored in SharedPreferences
    private var sessionColor = 0            // snapshot when dialog opens or user taps "Set App Color"
    private var currentColor = 0            // live editing color

    private var currentHue = defaultHSV[0]
    private var currentBrightness = defaultHSV[2]

    private var resetStage = 0              // 0 = not used, 1 = back to session, 2 = back to default

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

        savedColor = prefs.getInt("app_color", defaultColor)
        sessionColor = savedColor
        currentColor = savedColor

        val hsvStart = FloatArray(3)
        Color.colorToHSV(currentColor, hsvStart)
        currentHue = hsvStart[0]
        currentBrightness = hsvStart[2]

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
                Log.d("ColorTest", "rootView = ${rootView}, color = $currentColor")

                // Live preview
                rootView?.setBackgroundColor(currentColor)
            }
        })

        brightnessSlider.max = 100
        brightnessSlider.progress = (currentBrightness * 100).toInt()
        brightnessSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentBrightness = progress / 100f
                wheel.setBrightness(currentBrightness)
                currentColor = Color.HSVToColor(floatArrayOf(currentHue, 0.5f, currentBrightness))
                resetStage = 0
                updateResetButton()

                // Live preview
                rootView?.setBackgroundColor(currentColor)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnAppColor.text = "SET APP COLOR"
        btnAppColor.setOnClickListener {
            currentColor = Color.HSVToColor(floatArrayOf(currentHue, 0.5f, currentBrightness))
            prefs.edit().putInt("app_color", currentColor).apply()

            savedColor = currentColor
            sessionColor = currentColor
            resetStage = 0
            Toast.makeText(context, "App color set", Toast.LENGTH_SHORT).show()
            updateResetButton()

            (context as? LibraryActivity)?.refreshFolderListColors()


        }

        btnReset.setOnClickListener {
            when (resetStage) {
                0 -> {
                    applyColor(sessionColor)
                    rootView?.setBackgroundColor(sessionColor)
                    resetStage = 1
                    Toast.makeText(context, "Reset to saved color", Toast.LENGTH_SHORT).show()
                }
                1 -> {
                    applyColor(defaultColor)
                    rootView?.setBackgroundColor(defaultColor)
                    currentHue = defaultHSV[0]
                    currentBrightness = defaultHSV[2]
                    resetStage = 2
                    Toast.makeText(context, "Reset to default color", Toast.LENGTH_SHORT).show()
                }
                else -> {} // do nothing
            }
            updateResetButton()
        }

        btnSave.setOnClickListener { dismiss() }
        btnCancel.setOnClickListener { dismiss() }

        updateResetButton()

        setOnShowListener {

            Log.d("ColorTest", "rootView assigned onShow = $rootView")
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

    private fun updateResetButton() {
        btnReset.isEnabled = true
        btnReset.text = when (resetStage) {
            0 -> "Reset"
            1 -> "Reset to Default"
            else -> "Reset"
        }
    }

    override fun dismiss() {
        super.dismiss()
        val restoredColor = prefs.getInt("app_color", defaultColor)
        rootView?.setBackgroundColor(restoredColor)
    }


    fun setHue(value: Float) {
        currentHue = value.coerceIn(0f, 360f)
        wheel.setHue(currentHue)
        wheel.updateSelectorPositionFromAngle(currentHue)
    }
}
