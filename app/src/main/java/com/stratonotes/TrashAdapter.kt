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
        selectionMode = false

        folders.addAll(
            newFolders.mapNotNull { fw ->
                val trashedNotes = fw.notes.filter { it.isTrashed }
                if (trashedNotes.isNotEmpty()) {
                    FolderWithNotes(fw.folder, trashedNotes).apply { isExpanded = false }
                } else null
            }
        )

        looseNotes.addAll(newLooseNotes.sortedByDescending { it.lastEdited })
        notifyDataSetChanged()
    }

    fun exitSelectionMode() {
        selectionMode = false
        selectedNotes.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        var count = looseNotes.size
        folders.forEach { folder ->
            count += 1
            if (folder.isExpanded) count += folder.notes.size
        }
        return count
    }

    override fun getItemViewType(position: Int): Int {
        var index = 0
        for (folder in folders) {
            if (index == position) return TYPE_FOLDER
            index++
            if (folder.isExpanded) {
                for (note in folder.notes) {
                    if (index == position) return TYPE_NOTE_IN_FOLDER
                    index++
                }
            }
        }
        return TYPE_LOOSE_NOTE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_FOLDER -> FolderViewHolder(inflater.inflate(R.layout.item_folder, parent, false))
            TYPE_NOTE_IN_FOLDER, TYPE_LOOSE_NOTE ->
                NoteViewHolder(inflater.inflate(R.layout.item_trash_note, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var index = 0
        for ((folderIndex, folder) in folders.withIndex()) {
            if (index == position) {
                (holder as FolderViewHolder).bind(folder, folderIndex)
                return
            }
            index++
            if (folder.isExpanded) {
                for (note in folder.notes) {
                    if (index == position) {
                        (holder as NoteViewHolder).bind(note)
                        return
                    }
                    index++
                }
            }
        }

        val looseIndex = position - folders.sumOf { 1 + if (it.isExpanded) it.notes.size else 0 }
        if (looseIndex in looseNotes.indices) {
            (holder as NoteViewHolder).bind(looseNotes[looseIndex])
        }
    }

    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val folderName: TextView = itemView.findViewById(R.id.folderName)
        private val folderCheckbox: CheckBox = itemView.findViewById(R.id.folderCheckbox)
        private val expandButton: ImageButton = itemView.findViewById(R.id.expandButton)

        fun bind(folder: FolderWithNotes, index: Int) {
            folderName.text = folder.folder.name

            folderCheckbox.visibility = if (selectionMode) View.VISIBLE else View.GONE
            folderCheckbox.isChecked =
                folder.notes.isNotEmpty() && folder.notes.all { selectedNotes.contains(it) }

            folderCheckbox.setOnClickListener {
                toggleFolderSelection(folder)
                folder.notes.forEach { notifyNoteChanged(it) }
                notifyItemChanged(getPositionForFolder(index))
                listener.onSelectionChanged()
            }

            expandButton.setOnClickListener {
                folder.isExpanded = !folder.isExpanded
                notifyDataSetChanged()
            }

            itemView.setOnClickListener {
                if (selectionMode) {
                    toggleFolderSelection(folder)
                    folder.notes.forEach { notifyNoteChanged(it) }
                    notifyItemChanged(getPositionForFolder(index))
                    listener.onSelectionChanged()
                }
            }

            itemView.setOnLongClickListener {
                if (!selectionMode) {
                    selectionMode = true
                    selectedNotes.addAll(folder.notes)
                    folder.notes.forEach { notifyNoteChanged(it) }
                    notifyItemChanged(getPositionForFolder(index))
                    listener.onStartSelection()
                    listener.onSelectionChanged()
                }
                true
            }
        }

        private fun toggleFolderSelection(folder: FolderWithNotes) {
            val allSelected = folder.notes.all { selectedNotes.contains(it) }
            if (allSelected) {
                selectedNotes.removeAll(folder.notes)
            } else {
                selectedNotes.addAll(folder.notes)
            }
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

            val daysRemaining =
                30 - TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - note.lastEdited)
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
                    .setPositiveButton("Delete") { _, _ -> listener.onDelete(note) }
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
                    notifyItemChanged(bindingAdapterPosition)
                    notifyFolderHeaderChangedIfNecessary(note)
                    listener.onStartSelection()
                    listener.onSelectionChanged()
                } else {
                    toggleSelection(note)
                }
                true
            }
        }

        private fun toggleSelection(note: NoteEntity) {
            if (selectedNotes.any { it.id == note.id }) {
                selectedNotes.removeIf { it.id == note.id }
            } else {
                selectedNotes.add(note)
            }

            val pos = bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                notifyItemChanged(pos)
            }

            notifyFolderHeaderChangedIfNecessary(note)
            listener.onSelectionChanged()
        }

        private fun notifyFolderHeaderChangedIfNecessary(note: NoteEntity) {
            for ((folderIndex, folder) in folders.withIndex()) {
                if (folder.notes.any { it.id == note.id }) {
                    val folderPos = getPositionForFolder(folderIndex)
                    if (folderPos >= 0) notifyItemChanged(folderPos)
                    break
                }
            }
        }
    }

    private fun getPositionForFolder(index: Int): Int {
        var position = 0
        for (i in 0 until index) {
            position += 1 + if (folders[i].isExpanded) folders[i].notes.size else 0
        }
        return position
    }

    private fun getPositionForNote(note: NoteEntity): Int {
        var index = 0
        for (folder in folders) {
            index++
            if (folder.isExpanded) {
                for (n in folder.notes) {
                    if (n.id == note.id) return index
                    index++
                }
            }
        }

        val looseIndex = looseNotes.indexOfFirst { it.id == note.id }
        if (looseIndex != -1) {
            return folders.sumOf { 1 + if (it.isExpanded) it.notes.size else 0 } + looseIndex
        }

        return -1
    }

    private fun notifyNoteChanged(note: NoteEntity) {
        val pos = getPositionForNote(note)
        if (pos >= 0) notifyItemChanged(pos)
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

    companion object {
        private const val TYPE_FOLDER = 0
        private const val TYPE_NOTE_IN_FOLDER = 1
        private const val TYPE_LOOSE_NOTE = 2
    }
}
