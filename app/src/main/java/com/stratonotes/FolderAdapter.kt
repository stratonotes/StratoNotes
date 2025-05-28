package com.example.punchpad2

import android.content.Context
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
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

    private val expandedFolderIndices = mutableListOf<Int>()
    private var deleteMode = false

    fun setDeleteMode(deleteMode: Boolean) {
        this.deleteMode = deleteMode
        notifyDataSetChanged()
    }

    fun updateFilteredList(filteredFolders: List<FolderWithNotes>) {
        folders.clear()
        folders.addAll(filteredFolders)
        expandedFolderIndices.clear()
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
            val folderText = folderWithNotes.folder?.name ?: "(Unnamed)"
            folderName.text = folderText
            Log.d("FolderAdapter", "Folder: $folderText has ${folderWithNotes.notes?.size ?: 0} notes")

            notesContainer.removeAllViews()
            val isExpanded = expandedFolderIndices.contains(position)
            expandButton.rotation = if (isExpanded) 180f else 0f
            notesContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE

            expandButton.setOnClickListener {
                if (expandedFolderIndices.contains(position)) {
                    expandedFolderIndices.remove(position)
                } else {
                    if (expandedFolderIndices.size >= 3) {
                        expandedFolderIndices.removeAt(0)
                    }
                    expandedFolderIndices.add(position)
                }
                notifyDataSetChanged()
            }

            if (deleteMode) {
                folderName.setOnClickListener {
                    val editText = EditText(context)
                    editText.setText(folderText)
                    editText.inputType = InputType.TYPE_CLASS_TEXT
                    editText.imeOptions = EditorInfo.IME_ACTION_DONE

                    (folderName.parent as ViewGroup).removeView(folderName)
                    (folderName.parent as ViewGroup).addView(editText, 0)

                    editText.requestFocus()
                    editText.setOnEditorActionListener { v, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            val newName = v.text.toString().trim()
                            folderWithNotes.folder?.name = newName
                            Log.d("FolderAdapter", "Folder renamed to: $newName")
                            (editText.parent as ViewGroup).removeView(editText)
                            (editText.parent as ViewGroup).addView(folderName, 0)
                            folderName.text = newName
                            true
                        } else {
                            false
                        }
                    }
                }
            } else {
                folderName.setOnClickListener(null)
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
