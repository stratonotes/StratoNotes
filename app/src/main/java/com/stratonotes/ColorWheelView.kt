package com.stratonotes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class ColorWheelView : View {
    private var paint: Paint? = null
    private var selectorPaint: Paint? = null
    private var centerX = 0f
    private var centerY = 0f
    private var outerRadius = 0f
    private var innerRadius = 0f
    private var selectorX = 0f
    private var selectorY = 0f
    var selectedColor: Int = Color.RED
        private set
    private var listener: OnColorSelectedListener? = null

    interface OnColorSelectedListener {
        fun onColorSelected(color: Int)
    }

    fun setOnColorSelectedListener(l: (Any) -> Unit) {
        this.listener = listener
    }

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint!!.style = Paint.Style.STROKE
        selectorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        selectorPaint!!.style = Paint.Style.STROKE
        selectorPaint!!.strokeWidth = 6f
        selectorPaint!!.color = Color.WHITE
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        centerX = w / 2f
        centerY = h / 2f
        outerRadius = (min(w.toDouble(), h.toDouble()) * 0.45f).toFloat()
        innerRadius = outerRadius * 0.85f // thinner ring
        paint!!.strokeWidth = outerRadius - innerRadius
        updateSelectorPositionFromAngle(0f) // default: 0°
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val ringRadius = (outerRadius + innerRadius) / 2f

        for (i in 0..359) {
            val hsv = floatArrayOf(i.toFloat(), 1f, 1f)
            paint!!.color = Color.HSVToColor(hsv)
            canvas.drawArc(
                centerX - ringRadius, centerY - ringRadius,
                centerX + ringRadius, centerY + ringRadius,
                i.toFloat(), 1f, false, paint!!
            )
        }

        val selectorSize = (outerRadius - innerRadius) / 1.8f
        canvas.drawCircle(selectorX, selectorY, selectorSize, selectorPaint!!)
    }

    private fun updateSelectorPositionFromAngle(angleDegrees: Float) {
        val radius = (outerRadius + innerRadius) / 2f
        val angleRad = Math.toRadians(angleDegrees.toDouble())
        selectorX = centerX + (radius * cos(angleRad)).toFloat()
        selectorY = centerY + (radius * sin(angleRad)).toFloat()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val dx = event.x - centerX
        val dy = event.y - centerY
        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        if (dist < innerRadius || dist > outerRadius) {
            return false // outside the ring
        }

        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        angle = (angle + 360) % 360

        selectedColor = Color.HSVToColor(floatArrayOf(angle, 1f, 1f))
        updateSelectorPositionFromAngle(angle)
        invalidate()

        if (listener != null) listener!!.onColorSelected(selectedColor)
        return true
    }
}