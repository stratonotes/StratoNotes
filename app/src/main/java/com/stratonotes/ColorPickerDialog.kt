package com.stratonotes

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
import androidx.core.content.edit
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.punchpad2.R
import androidx.core.graphics.toColorInt

class ColorPickerDialog(context: Context, private val rootView: View?) : Dialog(context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    // Real default color = #444588
    private val defaultHex = "#444588"
    private val defaultColor = defaultHex.toColorInt()
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
        btnReset = findViewById(R.id.btnReset)
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

                rootView?.setBackgroundColor(currentColor)
                Log.d("ColorTest", "Preview updated with $currentColor onChange")
            }
        })

        brightnessSlider.max = 100
        brightnessSlider.progress = (currentBrightness * 100).toInt()
        brightnessSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentBrightness = progress / 100f
                wheel.setBrightness(currentBrightness)
                resetStage = 0
                updateResetButton()
                rootView?.setBackgroundColor(currentColor)
                Log.d("ColorTest", "Preview updated with $currentColor onChange")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnAppColor.setText(R.string.set_app_color)
        btnAppColor.setOnClickListener {
            currentColor = Color.HSVToColor(floatArrayOf(currentHue, 0.5f, currentBrightness))
            prefs.edit {
                putInt("app_color", currentColor)
            }

            savedColor = currentColor
            sessionColor = currentColor
            resetStage = 0
            updateResetButton()

            (context as? LibraryActivity)?.refreshFolderListColors()

            val intent = Intent("com.stratonotes.THEME_COLOR_CHANGED")
            intent.putExtra("color", currentColor)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }

        btnReset.setOnClickListener {
            when (resetStage) {
                0 -> {
                    applyColor(sessionColor)
                    rootView?.setBackgroundColor(sessionColor)
                    resetStage = 1
                    Toast.makeText(context, R.string.reset_to_saved, Toast.LENGTH_SHORT).show()
                }
                1 -> {
                    applyColor(defaultColor)
                    rootView?.setBackgroundColor(defaultColor)
                    currentHue = defaultHSV[0]
                    currentBrightness = defaultHSV[2]
                    resetStage = 2
                    Toast.makeText(context, R.string.reset_to_default, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
            updateResetButton()
        }

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
            0 -> context.getString(R.string.reset)
            1 -> context.getString(R.string.reset_to_default)
            else -> context.getString(R.string.reset)
        }
    }

    override fun dismiss() {
        super.dismiss()
        val restoredColor = prefs.getInt("app_color", defaultColor)
        rootView?.setBackgroundColor(restoredColor)
    }

    @Suppress("unused")
    fun setHue(value: Float) {
        currentHue = value.coerceIn(0f, 360f)
        wheel.setHue(currentHue)
        wheel.updateSelectorPositionFromAngle(currentHue)
    }
}
