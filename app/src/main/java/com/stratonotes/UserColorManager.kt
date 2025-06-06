package com.stratonotes

import android.content.Context
import android.graphics.Color
import android.preference.PreferenceManager
import androidx.core.content.edit
import androidx.core.graphics.toColorInt

object UserColorManager {

    private const val KEY_OVERLAY_COLOR = "overlay_color"
    private const val KEY_APP_COLOR = "app_color"
    private const val KEY_TEXT_COLOR = "text_color"

    private val DEFAULT_OVERLAY_COLOR = "#222222".toColorInt()
    private val DEFAULT_APP_COLOR = "#3333AA".toColorInt()
    private val DEFAULT_TEXT_COLOR = "#FFFFFF".toColorInt()

    fun setOverlayColor(context: Context, color: Int) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putInt(KEY_OVERLAY_COLOR, color)
        }
    }

    fun getOverlayColor(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(KEY_OVERLAY_COLOR, DEFAULT_OVERLAY_COLOR)
    }

    fun getDefaultOverlayColor(): Int {
        return DEFAULT_OVERLAY_COLOR
    }

    fun getAppColor(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(KEY_APP_COLOR, DEFAULT_APP_COLOR)
    }

    fun getTextColor(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(KEY_TEXT_COLOR, DEFAULT_TEXT_COLOR)
    }
}
