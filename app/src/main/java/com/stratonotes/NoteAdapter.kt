package com.stratonotes

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.punchpad2.R

class NoteAdapter(


    private val context: Context,
    private val deleteMode: Boolean,
    private val listener: OnNoteChangedListener
) : ListAdapter<NoteEntity, NoteAdapter.NoteViewHolder>(DIFF_CALLBACK) {

    interface OnNoteChangedListener {
        fun onNoteChanged(note: NoteEntity)
    }



    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NoteEntity>() {
            override fun areItemsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val noteText: EditText = itemView.findViewById(R.id.noteText)
        private val starIcon: ImageView = itemView.findViewById(R.id.starIcon)
        private var isEditing = false

        fun bind(note: NoteEntity) {
            noteText.setText(note.content)
            starIcon.setImageResource(if (note.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline)

            noteText.isFocusable = false
            noteText.isCursorVisible = false
            noteText.setBackgroundColor(Color.TRANSPARENT)

            val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    Toast.makeText(
                        context,
                        if (note.isHiddenFromMain) "Note hidden from preview." else "Note visible in preview.",
                        Toast.LENGTH_SHORT
                    ).show()
                    listener.onNoteChanged(note)

                    return true
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (!isEditing) {
                        isEditing = true
                        noteText.isFocusableInTouchMode = true
                        noteText.isCursorVisible = true
                        noteText.requestFocus()
                        noteText.setBackgroundColor(0x22000000)
                    }
                    return true
                }
            })

            noteText.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                false
            }

            noteText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    isEditing = false
                    noteText.isCursorVisible = false
                    noteText.setBackgroundColor(Color.TRANSPARENT)
                }
            }

            noteText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable?) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    listener.onNoteChanged(note)

                }
            })

            if (deleteMode && note.isHiddenFromMain) {
                starIcon.setOnClickListener {
                    Toast.makeText(context, "Can't delete hidden notes.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
