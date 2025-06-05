package com.stratonotes

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.Toast
import com.example.punchpad2.R


class ColorPickerDialog(context: Context) : Dialog(context) {
    private enum class Mode {
        TEXT_COLOR,
        APP_COLOR
    }

    private var currentMode = Mode.APP_COLOR
    private var textColor = 0
    private var appColor = 0
    private var backgroundImageUri: String? = null

    private val prefs: SharedPreferences =
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_color_picker)
        setCancelable(true)

        // Load current prefs
        textColor = prefs.getInt("text_color", Color.BLACK)
        appColor = prefs.getInt("app_color", Color.BLUE)
        backgroundImageUri = prefs.getString("bg_image", null)

        val wheel: ColorWheelView = findViewById(R.id.colorWheelView)
        val btnTextColor = findViewById<Button>(R.id.btnTextColor)
        val btnAppColor = findViewById<Button>(R.id.btnAppColor)
        val btnBackgroundImage = findViewById<Button>(R.id.btnBackgroundImage)
        val btnReset = findViewById<Button>(R.id.btnReset)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnCancel = findViewById<Button>(R.id.btnCancel)

        // Default selected mode
        currentMode = Mode.APP_COLOR

        wheel.setOnColorSelectedListener { color ->
            if (currentMode == Mode.TEXT_COLOR) {
                textColor = color as Int
            } else {
                appColor = color as Int
            }
        }

        btnTextColor.setOnClickListener { v: View? ->
            currentMode = Mode.TEXT_COLOR
            Toast.makeText(context, "Editing Text Color", Toast.LENGTH_SHORT).show()
        }

        btnAppColor.setOnClickListener { v: View? ->
            currentMode = Mode.APP_COLOR
            Toast.makeText(context, "Editing App Color", Toast.LENGTH_SHORT).show()
        }



        btnReset.setOnClickListener { v: View? ->
            textColor = Color.BLACK
            appColor = Color.BLUE
            Toast.makeText(context, "Colors reset to default", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener { v: View? ->
            val editor = prefs.edit()
            editor.putInt("text_color", textColor)
            editor.putInt("app_color", appColor)
            if (backgroundImageUri != null) {
                editor.putString("bg_image", backgroundImageUri)
            } else {
                editor.remove("bg_image")
            }
            editor.apply()
            dismiss()
        }

        btnCancel.setOnClickListener { v: View? ->
            dismiss() // Discard changes
        }
    }




}
