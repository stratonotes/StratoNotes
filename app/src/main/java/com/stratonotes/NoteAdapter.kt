package com.stratonotes

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.punchpad2.R
import android.view.GestureDetector

class NoteAdapter(
    private val context: Context,
    private val listener: OnNoteChangedListener
) : ListAdapter<NoteEntity, NoteAdapter.NoteViewHolder>(DIFF_CALLBACK) {

    interface OnNoteChangedListener {
        fun onNoteChanged(note: NoteEntity)
        fun onNoteSelectedForEditing(note: NoteEntity, appendMode: Boolean)
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
        private val noteText: TextView = itemView.findViewById(R.id.noteText)
        private val starIcon: ImageView = itemView.findViewById(R.id.starIcon)
        private val mediaContainer: LinearLayout = itemView.findViewById(R.id.mediaContainer)

        fun bind(note: NoteEntity) {
            noteText.text = note.content
            starIcon.setImageResource(if (note.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline)

            // Star icon toggles favorite
            starIcon.setOnClickListener {
                note.isFavorite = !note.isFavorite
                starIcon.setImageResource(if (note.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
                listener.onNoteChanged(note)
            }

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
                    }

                    "audio" -> {
                        val audioView = LayoutInflater.from(context).inflate(R.layout.audio_widget, mediaContainer, false)
                        val playPauseButton = audioView.findViewById<ImageView>(R.id.playPauseButton)
                        val deleteButton = audioView.findViewById<ImageView>(R.id.deleteAudioButton)

                        playPauseButton.setOnClickListener {
                            Toast.makeText(context, "Play/Pause audio (stub)", Toast.LENGTH_SHORT).show()
                        }

                        deleteButton.setOnClickListener {
                            Toast.makeText(context, "Audio removed", Toast.LENGTH_SHORT).show()
                            mediaContainer.removeView(audioView)
                            note.mediaItems.remove(media)
                            listener.onNoteChanged(note)
                        }

                        mediaContainer.addView(audioView)
                    }
                }
            }

            // Single and Double Tap Detection for Note Editing
            val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: android.view.MotionEvent): Boolean {
                    listener.onNoteSelectedForEditing(note, appendMode = false)
                    return true
                }

                override fun onDoubleTap(e: android.view.MotionEvent): Boolean {
                    listener.onNoteSelectedForEditing(note, appendMode = true)
                    return true
                }
            })

            itemView.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                false
            }
        }
    }
}
