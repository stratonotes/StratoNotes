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
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.punchpad2.R
import android.net.Uri

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
        private val mediaContainer: LinearLayout = itemView.findViewById(R.id.mediaContainer)
        private var isEditing = false

        private val undoStack = mutableListOf<String>()
        private var undoIndex = -1
        private var largeNoteWarningShown = false

        fun bind(note: NoteEntity) {
            noteText.setText(note.content)
            starIcon.setImageResource(if (note.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline)

            noteText.isFocusable = false
            noteText.isCursorVisible = false
            noteText.setBackgroundColor(Color.TRANSPARENT)

            mediaContainer.removeAllViews()
            note.mediaItems?.forEach { media ->
                when (media.type) {
                    "image" -> {
                        val imageView = ImageView(context)
                        imageView.setImageURI(Uri.parse(media.uri))
                        imageView.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        mediaContainer.addView(imageView)

                        imageView.setOnClickListener {
                            Toast.makeText(context, "Open fullscreen image", Toast.LENGTH_SHORT).show()
                        }

                        imageView.setOnLongClickListener {
                            Toast.makeText(context, "Resize mode (long press)", Toast.LENGTH_SHORT).show()
                            true
                        }
                    }

                    "audio" -> {
                        val audioView = LayoutInflater.from(context).inflate(R.layout.audio_widget, mediaContainer, false)
                        val playPauseButton = audioView.findViewById<ImageView>(R.id.playPauseButton)
                        val deleteButton = audioView.findViewById<ImageView>(R.id.deleteAudioButton)
                        val timestampText = audioView.findViewById<TextView>(R.id.audioTimestamp)

                        playPauseButton.setOnClickListener {
                            Toast.makeText(context, "Play/Pause audio (stub)", Toast.LENGTH_SHORT).show()
                            // TODO: Hook into MediaPlayer or ExoPlayer instance
                        }

                        deleteButton.setOnClickListener {
                            Toast.makeText(context, "Audio removed", Toast.LENGTH_SHORT).show()
                            mediaContainer.removeView(audioView)
                            note.mediaItems.remove(media)
                            listener.onNoteChanged(note)
                        }

                        audioView.setOnLongClickListener {
                            Toast.makeText(context, "Trim mode (long press)", Toast.LENGTH_SHORT).show()
                            // TODO: Trigger trim UI
                            true
                        }

                        mediaContainer.addView(audioView)
                    }
                }
            }

            val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    note.isFavorite = !note.isFavorite
                    starIcon.setImageResource(if (note.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
                    val msg = if (note.isFavorite) "Note favorited" else "Note unfavorited"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
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
                    if (s != null) {
                        if (s.length > 5000 && !largeNoteWarningShown) {
                            Toast.makeText(context, "Large note detected. Performance may be affected.", Toast.LENGTH_LONG).show()
                            largeNoteWarningShown = true
                        }

                        if (undoIndex < undoStack.size - 1) {
                            undoStack.subList(undoIndex + 1, undoStack.size).clear()
                        }
                        undoStack.add(s.toString())
                        undoIndex = undoStack.size - 1

                        note.content = s.toString()
                        listener.onNoteChanged(note)
                    }
                }
            })

            if (deleteMode && note.isHiddenFromMain) {
                starIcon.setOnClickListener {
                    Toast.makeText(context, "Can't delete hidden notes.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        fun undo() {
            if (undoIndex > 0) {
                undoIndex--
                noteText.setText(undoStack[undoIndex])
            }
        }

        fun redo() {
            if (undoIndex < undoStack.size - 1) {
                undoIndex++
                noteText.setText(undoStack[undoIndex])
            }
        }
    }
}
