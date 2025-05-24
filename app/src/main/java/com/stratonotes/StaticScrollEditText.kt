package com.stratonotes

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

class StaticScrollEditText : AppCompatEditText {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    override fun requestRectangleOnScreen(rect: android.graphics.Rect): Boolean {
        // Prevent EditText from auto-scrolling when focused
        return false
    }
}
