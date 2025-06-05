package com.stratonotes

import android.content.Context
import android.graphics.Color
import android.preference.PreferenceManager
import androidx.core.graphics.toColorInt
import androidx.core.content.edit

object UserColorManager {

    private const val KEY_OVERLAY_COLOR = "overlay_color"
    private val DEFAULT_OVERLAY_COLOR = "#222222".toColorInt()


    fun setOverlayColor(context: Context, color: Int) {
        PreferenceManager.getDefaultSharedPreferences(context).edit() {
            putInt(KEY_OVERLAY_COLOR, color)
        }
    }

    fun getOverlayColor(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(KEY_OVERLAY_COLOR, DEFAULT_OVERLAY_COLOR)
    }
    fun getDefaultOverlayColor(): Int {
        return Color.parseColor("#2E2E2E") // or whatever your default should be
    }
    // (Optional future use)
    // fun resetOverlayColor(context: Context) {
    //     setOverlayColor(context, DEFAULT_OVERLAY_COLOR)
    // }
}
