package com.example.punchpad2

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stratonotes.FolderWithNotes
import com.stratonotes.NoteAdapter
import com.stratonotes.NoteEntity

class FolderAdapter(
    private val context: Context,
    private val folders: MutableList<FolderWithNotes>,
    private val listener: (NoteEntity) -> Unit

) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {
    private var expandedFolderIndex = -1
    private var deleteMode = false

    fun setDeleteMode(deleteMode: Boolean) {
        this.deleteMode = deleteMode
        notifyDataSetChanged()
    }

    fun updateFilteredList(filteredFolders: List<FolderWithNotes>) {
        folders.clear()
        folders.addAll(filteredFolders)
        expandedFolderIndex = 0
        Log.d("FolderAdapter", "Loaded ${filteredFolders.size} folders")
        notifyDataSetChanged()
    }

    fun submitList(notes: List<FolderWithNotes>) {
        updateFilteredList(notes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_folder, parent, false)
        return FolderViewHolder(v)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(folders[position], position)
    }

    override fun getItemCount(): Int = folders.size

    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val folderName: TextView = itemView.findViewById(R.id.folderName)
        private val notesContainer: LinearLayout = itemView.findViewById(R.id.notesContainer)
        private val expandButton: ImageButton = itemView.findViewById(R.id.expandButton)

        fun bind(folderWithNotes: FolderWithNotes, position: Int) {
            folderName.text = folderWithNotes.folder?.name ?: "(Unnamed)"
            Log.d("FolderAdapter", "Folder: ${folderWithNotes.folder?.name} has ${folderWithNotes.notes?.size ?: 0} notes")

            notesContainer.removeAllViews()
            val isExpanded = position == expandedFolderIndex

            expandButton.rotation = if (isExpanded) 180f else 0f
            notesContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE

            expandButton.setOnClickListener {
                expandedFolderIndex = if (expandedFolderIndex == position) -1 else position
                notifyDataSetChanged()
            }

            if (isExpanded) {
                val tempAdapter = NoteAdapter(
                    context = context,
                    deleteMode = deleteMode,
                    listener = object : NoteAdapter.OnNoteChangedListener {
                        override fun onNoteChanged(note: NoteEntity) {
                            listener(note)
                        }
                    }

                )

                for (i in 0 until minOf(3, folderWithNotes.notes?.size ?: 0)) {
                    val noteView = LayoutInflater.from(context)
                        .inflate(R.layout.item_note, notesContainer, false)
                    val noteHolder = tempAdapter.NoteViewHolder(noteView)
                    noteHolder.bind(folderWithNotes.notes!![i])
                    notesContainer.addView(noteView)
                }
            }
        }
    }
}
