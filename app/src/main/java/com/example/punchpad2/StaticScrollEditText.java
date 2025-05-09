package com.example.punchpad2;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

public class StaticScrollEditText extends EditText {

    public StaticScrollEditText(Context context) {
        super(context);
    }

    public StaticScrollEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StaticScrollEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean requestRectangleOnScreen(android.graphics.Rect rect) {
        // Block automatic scroll to cursor
        return false;
    }
}
