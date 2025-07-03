package com.stratonotes

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.punchpad2.R

object OverlayManager {

    fun showPreviewOverlay(context: Context, note: NoteEntity, editable: Boolean) {
        val activity = context as? MainActivity
            ?: context as? LibraryActivity
            ?: context as? TrashActivity
            ?: return

        val root = activity.findViewById<FrameLayout>(R.id.overlayContainer) ?: return

        // Select layout based on screen
        val layoutRes = when (activity) {
            is TrashActivity -> R.layout.item_overlay_trash_note
            else -> R.layout.item_note
        }

        val overlayView = LayoutInflater.from(context).inflate(layoutRes, root, false)

        val noteInput = overlayView.findViewById<TextView>(R.id.noteText)
        val closeBtn = overlayView.findViewById<ImageView?>(R.id.closeButton)
        val starIcon = overlayView.findViewById<ImageView?>(R.id.starIcon)
        val pillMenu = overlayView.findViewById<View?>(R.id.widgetPillMenu)
        val cardWrapper = overlayView.findViewById<FrameLayout?>(R.id.cardWrapper)

        noteInput.text = note.content

        if (!editable) {
            noteInput.isFocusable = false
            noteInput.isFocusableInTouchMode = false
            noteInput.isCursorVisible = false
            noteInput.isEnabled = false
            pillMenu?.visibility = View.GONE
            starIcon?.visibility = View.GONE

            // Trash-specific actions
            val deleteIcon = overlayView.findViewById<ImageView?>(R.id.permanentlyDeleteIcon)
            val restoreIcon = overlayView.findViewById<ImageView?>(R.id.restoreIcon)

            deleteIcon?.setOnClickListener {
                if (activity is TrashActivity) {
                    AlertDialog.Builder(activity)
                        .setTitle("Delete Permanently")
                        .setMessage("This note will be permanently deleted and cannot be recovered. Are you sure?")
                        .setPositiveButton("Delete") { _, _ ->
                            activity.permanentlyDeleteNote(note)
                            root.removeView(overlayView)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }

            restoreIcon?.setOnClickListener {
                if (activity is TrashActivity) {
                    activity.restoreNote(note)
                    root.removeView(overlayView)
                }
            }
        }

        closeBtn?.setOnClickListener {
            root.removeView(overlayView)
        }

        // Handle background tap to dismiss
        cardWrapper?.setOnClickListener {
            root.removeView(overlayView)
        }

        root.addView(overlayView)
    }
}
