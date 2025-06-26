package com.stratonotes

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import com.example.punchpad2.R

object OverlayManager {

    fun showPreviewOverlay(context: Context, note: NoteEntity, editable: Boolean) {
        val activity = context as? MainActivity ?: context as? LibraryActivity ?: context as? TrashActivity ?: return
        val root = activity.findViewById<FrameLayout>(R.id.overlayContainer) ?: return

        val overlayView = LayoutInflater.from(context).inflate(R.layout.item_note, root, false)

        val noteInput = overlayView.findViewById<EditText>(R.id.noteText)
        val closeBtn = overlayView.findViewById<ImageView>(R.id.closeButton)

        val starIcon = overlayView.findViewById<ImageView?>(R.id.starIcon)
        val pillMenu = overlayView.findViewById<View?>(R.id.widgetPillMenu)

        noteInput.setText(note.content)
        noteInput.setSelection(0)

        if (!editable) {
            noteInput.isFocusable = false
            noteInput.isFocusableInTouchMode = false
            noteInput.isCursorVisible = false
            noteInput.isEnabled = false
            pillMenu?.visibility = View.GONE
            starIcon?.visibility = View.GONE
        }

        closeBtn.setOnClickListener {
            root.removeView(overlayView)
        }

        root.addView(overlayView)
    }
}
