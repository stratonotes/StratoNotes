package com.stratonotes

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.punchpad2.R

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

        fun bind(note: NoteEntity) {
            noteText.text = note.content
            starIcon.setImageResource(
                if (note.isFavorite) R.drawable.ic_star_filled
                else R.drawable.ic_star_outline
            )

            starIcon.setOnClickListener {
                note.isFavorite = !note.isFavorite
                starIcon.setImageResource(
                    if (note.isFavorite) R.drawable.ic_star_filled
                    else R.drawable.ic_star_outline
                )
                listener.onNoteChanged(note)
            }

            itemView.setOnClickListener {
                listener.onNoteSelectedForEditing(note, appendMode = false)
            }

            itemView.setOnLongClickListener {
                listener.onNoteSelectedForEditing(note, appendMode = true)
                true
            }
        }
    }
}
