package com.stratonotes

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.punchpad2.R

object OverlayManager {

    fun showPreviewOverlay(context: Context, note: NoteEntity, editable: Boolean) {
        // Which screen are we on?
        val activity = context as? MainActivity
            ?: context as? LibraryActivity
            ?: context as? TrashActivity
            ?: return

        val root = activity.findViewById<FrameLayout>(R.id.overlayContainer) ?: return

        val layoutRes = when (activity) {
            is TrashActivity -> R.layout.item_overlay_trash_note
            else             -> R.layout.item_note
        }

        val overlayView = LayoutInflater.from(context).inflate(layoutRes, root, false)

        val noteInput  = overlayView.findViewById<TextView>(R.id.noteText)
        val closeBtn   = overlayView.findViewById<ImageView?>(R.id.closeButton)
        val starIcon   = overlayView.findViewById<ImageView?>(R.id.starIcon)
        val pillMenu   = overlayView.findViewById<View?>(R.id.widgetPillMenu)
        val cardWrap   = overlayView.findViewById<FrameLayout?>(R.id.cardWrapper)

        noteInput.text = note.content

        if (!editable) {
            // disable editing
            noteInput.apply {
                isFocusable          = false
                isFocusableInTouchMode = false
                isCursorVisible      = false
                isEnabled            = false
            }
            pillMenu?.visibility = View.GONE
            starIcon?.visibility = View.GONE

            /* ---------- TRASH-SPECIFIC BUTTONS ---------- */
            val deleteIcon  = overlayView.findViewById<ImageView?>(R.id.permanentlyDeleteIcon)
            val restoreIcon = overlayView.findViewById<ImageView?>(R.id.restoreIcon)

            // get adapter once; reuse for both handlers
            val trashAdapter = (activity as? TrashActivity)
                ?.findViewById<RecyclerView>(R.id.trashRecycler)
                ?.adapter as? TrashAdapter

            deleteIcon?.setOnClickListener {
                val trashListener = activity as? TrashAdapter.TrashActionListener
                if (trashListener != null) {
                    AlertDialog.Builder(activity)
                        .setTitle("Delete Permanently")
                        .setMessage("This note will be permanently deleted and cannot be recovered. Are you sure?")
                        .setPositiveButton("Delete") { _, _ ->
                            trashListener.onDelete(note)
                            trashAdapter?.removeNote(note)   // <-- instant UI refresh
                            root.removeView(overlayView)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }

            restoreIcon?.setOnClickListener {
                val trashListener = activity as? TrashAdapter.TrashActionListener
                if (trashListener != null) {
                    trashListener.onRestore(note)
                    trashAdapter?.removeNote(note)       // <-- instant UI refresh
                    root.removeView(overlayView)
                }
            }
        }

        // overlay dismissal
        closeBtn?.setOnClickListener { root.removeView(overlayView) }
        cardWrap?.setOnClickListener { root.removeView(overlayView) }

        root.addView(overlayView)
    }
}
