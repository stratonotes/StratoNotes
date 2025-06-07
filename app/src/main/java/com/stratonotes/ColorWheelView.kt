package com.stratonotes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class ColorWheelView : View {
    private var paint: Paint? = null
    private var selectorPaint: Paint? = null
    private var centerX = 0f
    private var centerY = 0f
    private var outerRadius = 0f
    private var innerRadius = 0f
    private var selectorX = 0f
    private var selectorY = 0f
    private var brightness: Float = 0.64f
    private var currentBrightness: Float = 0.64f
    private var currentHue: Float = 0f
    private var isDragging = false

    var selectedColor: Int = Color.RED
        private set

    private var listener: OnColorSelectedListener? = null

    interface OnColorSelectedListener {
        fun onColorSelected(color: Int)
    }

    fun setOnColorSelectedListener(l: OnColorSelectedListener) {
        this.listener = l
    }

    fun setBrightness(value: Float) {
        brightness = value.coerceIn(0f, 1f)
        currentBrightness = value.coerceIn(0f, 1f)
        updateSelectedColor()
        invalidate()
    }

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }

        selectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = Color.WHITE
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        centerX = w / 2f
        centerY = h / 2f
        outerRadius = (min(w.toDouble(), h.toDouble()) * 0.45f).toFloat()
        innerRadius = outerRadius * 0.85f
        paint!!.strokeWidth = outerRadius - innerRadius

        // Fix: reflect actual hue instead of resetting to 0 (red)
        updateSelectorPositionFromAngle(currentHue)
        updateSelectedColor(currentHue)
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val ringRadius = (outerRadius + innerRadius) / 2f
        val brightness = currentBrightness
        val saturation = when {
            brightness <= 0.25f -> 0.49f * (brightness / 0.25f)  // fade from black
            brightness >= 0.75f -> 0.49f * ((1f - brightness) / 0.25f)  // fade to white
            else -> 0.49f  // full saturation
        }

        for (i in 0..359) {
            val hsv = floatArrayOf(i.toFloat(), saturation, brightness)
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



    fun updateSelectorPositionFromAngle(angleDegrees: Float) {
        val radius = (outerRadius + innerRadius) / 2f
        val angleRad = Math.toRadians(angleDegrees.toDouble())
        selectorX = centerX + (radius * cos(angleRad)).toFloat()
        selectorY = centerY + (radius * sin(angleRad)).toFloat()
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        val dx = event.x - centerX
        val dy = event.y - centerY
        val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        val padding = 40f

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (dist in (innerRadius - padding)..(outerRadius + padding)) {
                    isDragging = true
                    updateFromTouch(dx, dy)
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    updateFromTouch(dx, dy)
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }

        return false
    }



    private fun updateSelectedColor(angle: Float = 0f) {
        val sat = if (currentBrightness >= 0.99f) 0f else 0.49f
        val hsv = floatArrayOf(currentHue, sat, currentBrightness)

        selectedColor = Color.HSVToColor(hsv)
        listener?.onColorSelected(selectedColor)
    }
    private fun updateFromTouch(dx: Float, dy: Float) {
        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        angle = (angle + 360) % 360
        currentHue = angle
        updateSelectorPositionFromAngle(angle)
        updateSelectedColor(angle)
        invalidate()
    }
    fun setHue(hue: Float) {
        currentHue = hue.coerceIn(0f, 360f)
        updateSelectorPositionFromAngle(currentHue)
        updateSelectedColor(currentHue)
        invalidate()
    }
    fun setInitialColor(color: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        currentHue = hsv[0]
        currentBrightness = hsv[2]
        updateSelectorPositionFromAngle(currentHue)
        selectedColor = color
        listener?.onColorSelected(color)
        invalidate()
    }



}
