package com.stratonotes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.punchpad2.R
import java.util.concurrent.TimeUnit

class TrashAdapter(
    private val listener: TrashActionListener
) : RecyclerView.Adapter<TrashAdapter.TrashViewHolder>() {

    private var notes: List<NoteEntity> = emptyList()

    interface TrashActionListener {
        fun onRestore(note: NoteEntity)
        fun onDelete(note: NoteEntity)
    }

    fun submitList(list: List<NoteEntity>) {
        notes = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrashViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_trash_note, parent, false)
        return TrashViewHolder(v)
    }

    override fun onBindViewHolder(holder: TrashViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount(): Int = notes.size

    inner class TrashViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val noteText: TextView = itemView.findViewById(R.id.noteText)
        private val daysLeft: TextView = itemView.findViewById(R.id.daysLeft)
        private val restoreButton: ImageButton = itemView.findViewById(R.id.restoreButton)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(note: NoteEntity) {
            noteText.text = note.content

            val daysRemaining = 30 - TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - note.lastEdited)

            daysLeft.text = "$daysRemaining days left until permanent deletion"

            restoreButton.setOnClickListener { listener.onRestore(note) }
            deleteButton.setOnClickListener { listener.onDelete(note) }
        }
    }
}
