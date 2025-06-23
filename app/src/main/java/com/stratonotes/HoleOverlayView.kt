package com.stratonotes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt

class HoleOverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {
    private val overlayPaint = Paint()
    private val holePaint = Paint()

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)

        overlayPaint.color = "#111111".toColorInt()



        // Clear hole
        holePaint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.CLEAR))
        holePaint.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Fill the whole screen with overlay
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        // Draw a bigger transparent circle in the center (30% bigger)
        val cx = width / 2f
        val cy = height / 2f
        val radius = 160f // was 120f → increased by 33%

        canvas.drawCircle(cx, cy, radius, holePaint)
    }
}