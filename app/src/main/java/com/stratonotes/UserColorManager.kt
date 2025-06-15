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
    private val DEFAULT_APP_COLOR = "#5D53A3".toColorInt()
    private val DEFAULT_TEXT_COLOR = "#FFFFFF".toColorInt()
    private val DEFAULT_CANCEL_COLOR = "#D06030".toColorInt()

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
        val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        return prefs.getInt(KEY_APP_COLOR, DEFAULT_APP_COLOR)
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

    fun getFolderColor(context: Context): Int {
        val base = getAppColor(context)
        val hsv = FloatArray(3)
        Color.colorToHSV(base, hsv)
        // Slightly dim and desaturate for folder bars
        hsv[1] = (hsv[1] * 0.92f).coerceIn(0f, 1f)
        hsv[2] = (hsv[2] * 0.88f).coerceIn(0f, 1f)
        return Color.HSVToColor(hsv)
    }

    fun getNoteColor(context: Context): Int {
        val base = getAppColor(context)
        val hsv = FloatArray(3)
        Color.colorToHSV(base, hsv)
        // Slightly desaturate and brighten notes
        hsv[1] = (hsv[1] * 0.78f).coerceIn(0f, 1f)
        hsv[2] = (hsv[2] * 1.08f).coerceAtMost(1f)
        return Color.HSVToColor(hsv)
    }
}
