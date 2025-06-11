package com.example.punchpad2

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.stratonotes.FolderWithNotes
import com.stratonotes.NoteEntity
import com.stratonotes.UserColorManager

class FolderAdapter(
    private val context: Context,
    private val folders: MutableList<FolderWithNotes>,
    private val listener: (NoteEntity, Boolean) -> Unit,
    private val noteLayoutResId: Int
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    fun getFolderIndexById(folderId: Long): Int {
        return folders.indexOfFirst { it.folder.id == folderId }
    }

    private val folderStates = mutableMapOf<Long, ExpandMode>()
    private val folderLoadedCounts = mutableMapOf<Long, Int>()
    private var selectionMode = false
    private val selectedNotes = mutableSetOf<NoteEntity>()

    enum class ExpandMode {
        PARTIAL, FULL, COLLAPSED
    }

    fun getSelectedNotes(): List<NoteEntity> = selectedNotes.toList()

    fun exitSelectionMode() {
        selectionMode = false
        selectedNotes.clear()
        notifyDataSetChanged()
    }

    fun updateFilteredList(filteredFolders: List<FolderWithNotes>) {
        folders.clear()
        folders.addAll(filteredFolders)
        filteredFolders.forEach { folder ->
            val id = folder.folder.id
            if (!folderStates.containsKey(id)) {
                folderStates[id] = ExpandMode.PARTIAL
                folderLoadedCounts[id] = 50
            }
        }
        selectedNotes.clear()


        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_folder, parent, false)
        return FolderViewHolder(v)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folders[position]

        val context = holder.itemView.context
        val folderColor = UserColorManager.getFolderColor(context)

        val tabView = holder.itemView.findViewById<View>(R.id.folderHeader)
        val drawable = ContextCompat.getDrawable(tabView.context, R.drawable.folder_header_background)?.mutate()
        (drawable as? GradientDrawable)?.setColor(folderColor)
        tabView.background = drawable

        holder.folderName.setBackgroundColor(folderColor)

        holder.bind(folder)
    }

    override fun getItemCount(): Int = folders.size

    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val folderName: TextView = itemView.findViewById(R.id.folderName)
        val notesContainer: LinearLayout = itemView.findViewById(R.id.notesContainer)

        fun bind(folderWithNotes: FolderWithNotes) {
            val folderId = folderWithNotes.folder.id
            val folderText = folderWithNotes.folder.name ?: "(Unnamed)"
            folderName.text = folderText

            val expandMode = folderStates.getOrDefault(folderId, ExpandMode.PARTIAL)
            val maxCount = folderLoadedCounts.getOrDefault(folderId, 50)

            notesContainer.removeAllViews()
            notesContainer.visibility =
                if (expandMode == ExpandMode.COLLAPSED) View.GONE else View.VISIBLE

            val notesToShow = when (expandMode) {
                ExpandMode.COLLAPSED -> emptyList()
                ExpandMode.PARTIAL -> folderWithNotes.notes.take(3)
                ExpandMode.FULL -> folderWithNotes.notes.take(maxCount)
            }

            val noteColor = UserColorManager.getNoteColor(context)
            val folderColor = UserColorManager.getFolderColor(context)

            notesToShow.forEach { note ->
                val noteView = LayoutInflater.from(context).inflate(noteLayoutResId, notesContainer, false)
                val noteText = noteView.findViewById<TextView>(R.id.noteText)
                val starIcon = noteView.findViewById<ImageView>(R.id.starIcon)
                val checkbox = noteView.findViewById<CheckBox?>(R.id.noteCheckbox)
                val fadeView = noteView.findViewById<View>(R.id.noteFade)

                val bgRes = if (note.content.length > 150) {
                    R.drawable.note_expanded_background
                } else {
                    R.drawable.note_preview_background
                }

                ContextCompat.getDrawable(context, bgRes)?.mutate()?.let { drawable ->
                    DrawableCompat.setTint(drawable, noteColor)
                    noteView.background = drawable
                } ?: noteView.setBackgroundColor(noteColor)

                if (note.content.length > 150) {
                    val fadeBottom = folderColor
                    val fadeTop = ColorUtils.setAlphaComponent(folderColor, 0)
                    val gradient = GradientDrawable(
                        GradientDrawable.Orientation.BOTTOM_TOP,
                        intArrayOf(fadeBottom, fadeTop)
                    )
                    gradient.cornerRadius = 0f
                    fadeView.background = gradient
                    fadeView.visibility = View.VISIBLE
                } else {
                    fadeView.visibility = View.GONE
                }

                starIcon.setImageResource(if (note.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
                starIcon.visibility = View.VISIBLE

                noteText.text = note.content

                noteText.setOnLongClickListener {
                    if (!selectionMode) {
                        selectionMode = true
                        selectedNotes.clear()
                    }
                    selectedNotes.add(note)
                    notifyDataSetChanged()
                    true
                }

                noteText.setOnClickListener {
                    if (selectionMode) {
                        val isChecked = !selectedNotes.contains(note)
                        if (isChecked) selectedNotes.add(note) else selectedNotes.remove(note)
                        notifyDataSetChanged()
                    } else {
                        listener(note, false)
                    }
                }


                if (selectionMode) {
                    checkbox?.visibility = View.VISIBLE
                    checkbox?.isChecked = selectedNotes.contains(note)
                } else {
                    checkbox?.visibility = View.GONE
                }

                checkbox?.setOnClickListener {
                    val isChecked = checkbox.isChecked
                    if (isChecked) selectedNotes.add(note) else selectedNotes.remove(note)
                }

                starIcon.setOnClickListener {
                    note.isFavorite = !note.isFavorite
                    starIcon.setImageResource(if (note.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline)

                }

                notesContainer.addView(noteView)
            }

            val totalNotes = folderWithNotes.notes.size
            if (expandMode == ExpandMode.FULL && totalNotes > notesToShow.size) {
                val loadMore = TextView(context).apply {
                    text = "Load More"
                    setPadding(16, 16, 16, 16)
                    setTextColor(context.getColor(R.color.white))
                    setOnClickListener {
                        folderLoadedCounts[folderId] = folderLoadedCounts.getOrDefault(folderId, 50) + 50
                        notifyItemChanged(adapterPosition)
                    }
                }
                notesContainer.addView(loadMore)
            }

            folderName.setOnClickListener {
                val newMode =
                    if (expandMode == ExpandMode.COLLAPSED) ExpandMode.FULL else ExpandMode.COLLAPSED
                folderStates[folderId] = newMode
                notifyItemChanged(adapterPosition)
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
                        folderWithNotes.folder.name = newName
                        Log.d("FolderAdapter", "Folder renamed to: $newName")
                        (editText.parent as ViewGroup).removeView(editText)
                        (editText.parent as ViewGroup).addView(folderName, 0)
                        folderName.text = newName
                        true
                    } else false
                }
                true
            }
        }
    }
}
