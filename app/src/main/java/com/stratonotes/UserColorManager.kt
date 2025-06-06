package com.stratonotes

import android.content.Context
import android.graphics.Color
import android.preference.PreferenceManager
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt

object UserColorManager {

    private const val KEY_OVERLAY_COLOR = "overlay_color"
    private const val KEY_APP_COLOR = "app_color"
    private const val KEY_TEXT_COLOR = "text_color"

    private val DEFAULT_OVERLAY_COLOR = "#222222".toColorInt()
    private val DEFAULT_APP_COLOR = "#5D53A3".toColorInt() // updated to preferred purple
    private val DEFAULT_TEXT_COLOR = "#FFFFFF".toColorInt()

    enum class Mode { LIGHT, DARK }
    enum class Variant { BASE, LIGHTER, DARKER }

    fun setOverlayColor(context: Context, color: Int) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putInt(KEY_OVERLAY_COLOR, color)
        }
    }

    fun getOverlayColor(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(KEY_OVERLAY_COLOR, DEFAULT_OVERLAY_COLOR)
    }

    fun getDefaultOverlayColor(): Int = DEFAULT_OVERLAY_COLOR

    fun getAppColor(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(KEY_APP_COLOR, DEFAULT_APP_COLOR)
    }

    fun getTextColor(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(KEY_TEXT_COLOR, DEFAULT_TEXT_COLOR)
    }

    fun getAppColorVariant(context: Context, mode: Mode, variant: Variant): Int {
        val base = getAppColor(context)
        return when (variant) {
            Variant.BASE -> base
            Variant.LIGHTER -> if (mode == Mode.LIGHT) getLightVariant(base) else getDarkVariant(base)
            Variant.DARKER -> if (mode == Mode.LIGHT) getDarkVariant(base) else getLightVariant(base)
        }
    }

    private fun getLightVariant(color: Int, factor: Float = 0.15f): Int {
        val r = (Color.red(color) + (255 - Color.red(color)) * factor).toInt().coerceAtMost(255)
        val g = (Color.green(color) + (255 - Color.green(color)) * factor).toInt().coerceAtMost(255)
        val b = (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt().coerceAtMost(255)
        return Color.rgb(r, g, b)
    }

    private fun getDarkVariant(color: Int, factor: Float = 0.15f): Int {
        val r = (Color.red(color) * (1 - factor)).toInt().coerceAtLeast(0)
        val g = (Color.green(color) * (1 - factor)).toInt().coerceAtLeast(0)
        val b = (Color.blue(color) * (1 - factor)).toInt().coerceAtLeast(0)
        return Color.rgb(r, g, b)
    }

    fun getAppPressedColor(context: Context): Int {
        val base = getAppColor(context)
        return getDarkVariant(base, 0.25f) // Slightly darker than DARKER
    }

    fun getDisabledColor(context: Context): Int {
        val base = getAppColor(context)
        return ColorUtils.setAlphaComponent(base, 100) // translucent
    }
}
