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
    private val DEFAULT_APP_COLOR = "#5D53A3".toColorInt() // preferred purple
    private val DEFAULT_TEXT_COLOR = "#FFFFFF".toColorInt()
    private val DEFAULT_CANCEL_COLOR = "#D06030".toColorInt() // default orange for cancel

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

    fun getAppColorHSV(context: Context): FloatArray {
        val hsv = FloatArray(3)
        Color.colorToHSV(getAppColor(context), hsv)
        return hsv
    }

    fun getTextColor(context: Context): Int {
        val appColor = getAppColor(context)
        return getAutoTextColor(appColor)
    }

    fun getAppColorVariant(context: Context, mode: Mode, variant: Variant): Int {
        val base = getAppColor(context)
        return when (variant) {
            Variant.BASE -> base
            Variant.LIGHTER -> if (mode == Mode.LIGHT) getLightVariant(base) else getDarkVariant(base)
            Variant.DARKER -> if (mode == Mode.LIGHT) getDarkVariant(base) else getLightVariant(base)
        }
    }

    fun getAppPressedColor(context: Context): Int {
        val base = getAppColor(context)
        return getDarkVariant(base, 0.25f)
    }

    fun getDisabledColor(context: Context): Int {
        val base = getAppColor(context)
        return ColorUtils.setAlphaComponent(base, 100)
    }

    fun getAutoTextColor(backgroundColor: Int): Int {
        val luminance = ColorUtils.calculateLuminance(backgroundColor)
        return if (luminance < 0.5) Color.WHITE else Color.BLACK
    }

    fun getCancelColorRelativeTo(appColor: Int): Int {
        val hsvApp = FloatArray(3)
        val hsvDefaultApp = FloatArray(3)
        val hsvDefaultCancel = FloatArray(3)

        Color.colorToHSV(appColor, hsvApp)
        Color.colorToHSV(DEFAULT_APP_COLOR, hsvDefaultApp)
        Color.colorToHSV(DEFAULT_CANCEL_COLOR, hsvDefaultCancel)

        val offset = (hsvDefaultCancel[0] - hsvDefaultApp[0] + 360f) % 360f
        val cancelHue = (hsvApp[0] + offset) % 360f

        return Color.HSVToColor(floatArrayOf(cancelHue, hsvDefaultCancel[1], hsvDefaultCancel[2]))
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
}
