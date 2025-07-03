package com.stratonotes

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.punchpad2.R
import java.util.concurrent.TimeUnit

class TrashAdapter(
    private val listener: TrashActionListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val folders = mutableListOf<FolderWithNotes>()
    private val looseNotes = mutableListOf<NoteEntity>()

    private var selectionMode = false
    val selectedNotes = mutableSetOf<NoteEntity>()

    interface TrashActionListener {
        fun onRestore(note: NoteEntity)
        fun onDelete(note: NoteEntity)
        fun onStartSelection()
        fun onSelectionChanged()
    }

    fun setData(newFolders: List<FolderWithNotes>, newLooseNotes: List<NoteEntity>) {
        folders.clear()
        looseNotes.clear()
        selectedNotes.clear()

        folders.addAll(
            newFolders.mapNotNull { fw ->
                val trashedNotes = fw.notes.filter { it.isTrashed }
                if (trashedNotes.isNotEmpty()) {
                    FolderWithNotes(fw.folder, trashedNotes)
                } else null
            }
        )

        looseNotes.addAll(newLooseNotes.sortedByDescending { it.lastEdited })

        selectionMode = false
        notifyDataSetChanged()
    }

    fun exitSelectionMode() {
        selectionMode = false
        selectedNotes.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        var count = looseNotes.size
        folders.forEach { count += 1 + it.notes.size }
        return count
    }

    override fun getItemViewType(position: Int): Int {
        var index = 0
        for (folder in folders) {
            if (index == position) return TYPE_FOLDER
            index++
            for (note in folder.notes) {
                if (index == position) return TYPE_NOTE_IN_FOLDER
                index++
            }
        }
        return TYPE_LOOSE_NOTE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_FOLDER -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_folder, parent, false)
                FolderViewHolder(v)
            }
            TYPE_NOTE_IN_FOLDER, TYPE_LOOSE_NOTE -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_trash_note, parent, false)
                NoteViewHolder(v)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var index = 0
        for (folder in folders) {
            if (index == position) {
                (holder as FolderViewHolder).bind(folder.folder.name)
                return
            }
            index++
            for (note in folder.notes) {
                if (index == position) {
                    (holder as NoteViewHolder).bind(note)
                    return
                }
                index++
            }
        }
        val looseIndex = position - index
        if (looseIndex in looseNotes.indices) {
            (holder as NoteViewHolder).bind(looseNotes[looseIndex])
        }
    }

    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val folderName: TextView = itemView.findViewById(R.id.folderName)
        fun bind(name: String) {
            folderName.text = name
        }
    }

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val noteText: TextView = itemView.findViewById(R.id.noteText)
        private val trashFooter: ViewGroup = itemView.findViewById(R.id.trashFooter)
        private val daysLeft: TextView = itemView.findViewById(R.id.daysLeft)
        private val restoreBtn: ImageButton = itemView.findViewById(R.id.restoreBtn)
        private val deleteBtn: ImageButton = itemView.findViewById(R.id.deleteBtn)
        private val checkbox: CheckBox = itemView.findViewById(R.id.noteCheckbox)
        private val deletedBadge: TextView = itemView.findViewById(R.id.deletedBadge)

        fun bind(note: NoteEntity) {
            noteText.text = note.content

            val daysRemaining = 30 - TimeUnit.MILLISECONDS.toDays(
                System.currentTimeMillis() - note.lastEdited
            )
            daysLeft.text = "$daysRemaining days left"

            trashFooter.visibility = View.VISIBLE
            deletedBadge.visibility = View.VISIBLE

            checkbox.visibility = if (selectionMode) View.VISIBLE else View.GONE
            checkbox.isChecked = selectedNotes.contains(note)

            restoreBtn.setOnClickListener { listener.onRestore(note) }

            deleteBtn.setOnClickListener {
                AlertDialog.Builder(itemView.context)
                    .setTitle("Delete Note Permanently?")
                    .setMessage("This cannot be undone. Are you sure?")
                    .setPositiveButton("Delete") { _, _ ->
                        listener.onDelete(note)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            itemView.setOnClickListener {
                if (selectionMode) {
                    toggleSelection(note)
                } else {
                    val activity = itemView.context as? TrashActivity
                    activity?.let {
                        OverlayManager.showPreviewOverlay(
                            context = it,
                            note = note,
                            editable = false
                        )
                    }
                }
            }

            itemView.setOnLongClickListener {
                if (!selectionMode) {
                    selectionMode = true
                    selectedNotes.add(note)
                    listener.onStartSelection()
                } else {
                    toggleSelection(note)
                }
                true
            }
        }

        private fun toggleSelection(note: NoteEntity) {
            if (selectedNotes.contains(note)) {
                selectedNotes.remove(note)
            } else {
                selectedNotes.add(note)
            }

            if (selectedNotes.isEmpty()) {
                selectionMode = false
            }

            listener.onSelectionChanged()
            notifyDataSetChanged()
        }
    }

    companion object {
        private const val TYPE_FOLDER = 0
        private const val TYPE_NOTE_IN_FOLDER = 1
        private const val TYPE_LOOSE_NOTE = 2
    }

    fun removeNote(note: NoteEntity) {
        looseNotes.remove(note)

        val updatedFolders = folders.map { folder ->
            folder.copy(notes = folder.notes.filterNot { it.id == note.id })
        }
        folders.clear()
        folders.addAll(updatedFolders)

        notifyDataSetChanged()
    }
}
