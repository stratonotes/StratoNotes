package com.stratonotes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.punchpad2.R



class TrashAdapter(
    private val trashedNotes: List<NoteEntity>,
    private val listener: TrashActionListener
) :
    RecyclerView.Adapter<TrashAdapter.TrashViewHolder>() {
    interface TrashActionListener {
        fun onRestore(note: NoteEntity)
        fun onDelete(note: NoteEntity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrashViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_trash_note, parent, false)
        return TrashViewHolder(v)
    }

    override fun onBindViewHolder(holder: TrashViewHolder, position: Int) {
        holder.bind(trashedNotes[position])
    }

    override fun getItemCount(): Int {
        return trashedNotes.size
    }

    inner class TrashViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var trashContent: TextView = itemView.findViewById(R.id.trashContent)
        var restoreButton: ImageButton = itemView.findViewById(R.id.restoreButton)
        var deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(note: NoteEntity) {
            trashContent.text = note.content
            restoreButton.setOnClickListener { v: View? -> listener.onRestore(note) }
            deleteButton.setOnClickListener { v: View? -> listener.onDelete(note) }
        }
    }
}