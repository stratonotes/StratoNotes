package com.stratonotes

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class HoleOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val dimPaint = Paint().apply {
        color = Color.parseColor("#B3000000") // semi-transparent black
    }

    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        isAntiAlias = true
    }

    private val ringPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val holeRadius = 140f
    private val holeOffsetX = 40f
    private val holeOffsetY = 40f

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setLayerType(LAYER_TYPE_HARDWARE, null) // <--- CRUCIAL
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width - holeRadius - holeOffsetX
        val cy = holeRadius + holeOffsetY

        // Fill entire dim background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)

        // Clear circular hole area
        canvas.drawCircle(cx, cy, holeRadius, clearPaint)

        // White outline ring
        canvas.drawCircle(cx, cy, holeRadius, ringPaint)
    }
}
