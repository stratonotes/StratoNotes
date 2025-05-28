package com.example.punchpad2

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.stratonotes.FolderWithNotes
import com.stratonotes.NoteEntity

class FolderAdapter(
    private val context: Context,
    private val folders: MutableList<FolderWithNotes>,
    private val listener: (NoteEntity, Boolean) -> Unit,
    private val noteLayoutResId: Int
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    private val expandedFolderIndices = mutableSetOf<Int>()
    private var selectionMode = false
    private val selectedNotes = mutableSetOf<NoteEntity>()

    fun getSelectedNotes(): List<NoteEntity> = selectedNotes.toList()

    fun exitSelectionMode() {
        selectionMode = false
        selectedNotes.clear()
        notifyDataSetChanged()
    }

    fun updateFilteredList(filteredFolders: List<FolderWithNotes>) {
        folders.clear()
        folders.addAll(filteredFolders)
        expandedFolderIndices.clear()
        folders.forEachIndexed { index, folder ->
            if ((folder.notes?.size ?: 0) <= 3) {
                expandedFolderIndices.add(index)
            }
        }
        selectedNotes.clear()
        selectionMode = false
        notifyDataSetChanged()
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

            val noteCount = folderWithNotes.notes?.size ?: 0
            val isExpanded = expandedFolderIndices.contains(position)

            expandButton.rotation = if (isExpanded) 180f else 0f
            notesContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE

            expandButton.setOnClickListener {
                if (noteCount > 3) {
                    if (isExpanded) expandedFolderIndices.remove(position)
                    else expandedFolderIndices.add(position)
                    notifyDataSetChanged()
                } else {
                    Toast.makeText(context, "Nothing to expand", Toast.LENGTH_SHORT).show()
                }
            }

            folderName.setOnLongClickListener {
                val editText = EditText(context)
                editText.setText(folderText)
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
                    } else false
                }
                true
            }

            notesContainer.removeAllViews()
            if (isExpanded) {
                folderWithNotes.notes?.forEach { note ->
                    val noteView = LayoutInflater.from(context)
                        .inflate(noteLayoutResId, notesContainer, false)
                    val noteText = noteView.findViewById<TextView>(R.id.noteText)
                    val starIcon = noteView.findViewById<ImageView>(R.id.starIcon)
                    val checkbox = noteView.findViewById<CheckBox?>(R.id.noteCheckbox)

                    noteText.text = note.content
                    starIcon.setImageResource(if (note.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline)

                    // Long-press on note triggers selection mode
                    noteText.findViewById<View>(R.id.noteText).setOnLongClickListener {

                    if (!selectionMode) {
                            selectionMode = true
                            selectedNotes.clear()
                        }
                        selectedNotes.add(note)
                        notifyDataSetChanged()
                        true
                    }

                    // Handle single tap to toggle checkbox
                    noteView.setOnClickListener {
                        if (selectionMode) {
                            val isChecked = selectedNotes.contains(note).not()
                            if (isChecked) selectedNotes.add(note) else selectedNotes.remove(note)
                            notifyDataSetChanged()
                        } else {
                            listener(note, false)
                        }
                    }

                    // Show checkbox state
                    if (selectionMode) {
                        checkbox?.visibility = View.VISIBLE
                        checkbox?.isChecked = selectedNotes.contains(note)
                    } else {
                        checkbox?.visibility = View.GONE
                        checkbox?.isChecked = false
                    }

                    checkbox?.setOnClickListener {
                        val isChecked = checkbox.isChecked
                        if (isChecked) selectedNotes.add(note) else selectedNotes.remove(note)
                    }

                    starIcon.setOnClickListener {
                        note.isFavorite = !note.isFavorite
                        starIcon.setImageResource(if (note.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
                        listener(note, false)
                    }

                    notesContainer.addView(noteView)
                }
            }
        }
    }
}
