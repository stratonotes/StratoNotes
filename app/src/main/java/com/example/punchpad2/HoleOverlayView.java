package com.example.punchpad2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;

public class HoleOverlayView extends View {

    private final Paint overlayPaint = new Paint();
    private final Paint holePaint = new Paint();

    public HoleOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(LAYER_TYPE_HARDWARE, null);

        // Solid opaque dark overlay
        overlayPaint.setColor(0xFF111111);

        // Clear hole
        holePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        holePaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Fill the whole screen with overlay
        canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);

        // Draw a bigger transparent circle in the center (30% bigger)
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = 160f; // was 120f → increased by 33%

        canvas.drawCircle(cx, cy, radius, holePaint);
    }
}
