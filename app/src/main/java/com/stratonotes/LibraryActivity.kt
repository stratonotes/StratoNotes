package com.stratonotes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.punchpad2.FolderAdapter
import com.example.punchpad2.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryActivity : ComponentActivity() {


    private lateinit var folderAdapter: FolderAdapter

    private var deleteMode = false
    private var favoritesOnly = false
    private var sortNewest = true

    private val noteViewModel: NoteViewModel by viewModels()
    private lateinit var overlayContainer: FrameLayout
    private var currentOverlayNote: NoteEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        overlayContainer = findViewById(R.id.overlayContainer)
        val overlayBackdrop = findViewById<View>(R.id.overlayBackdrop)
        overlayBackdrop.setOnClickListener { closeOverlay() }

        val folderRecycler = findViewById<RecyclerView>(R.id.folderRecycler)
        folderRecycler.layoutManager = LinearLayoutManager(this)

        val folderId = intent.getLongExtra("folder_id", -1L)
        if (folderId != -1L) {
            folderRecycler.post {
                val index = folderAdapter.getFolderIndexById(folderId)
                if (index != -1) {
                    folderRecycler.scrollToPosition(index)
                }
            }
        }


        val searchInput = findViewById<EditText>(R.id.searchInput)
        val backButton = findViewById<ImageButton>(R.id.strato_button)
        backButton.setOnClickListener {
            finish()
        }

        val query = intent.getStringExtra("query") ?: ""

        folderAdapter = FolderAdapter(
            context = this,
            folders = mutableListOf(),
            listener = { note, _ ->
                showOverlay(note) // ✅ Open overlay like in MainActivity
            },
            noteLayoutResId = R.layout.item_note_library
        )

        folderRecycler.adapter = folderAdapter

        noteViewModel.getFoldersWithPreviews().observe(this) { folders ->
            folderAdapter.updateFilteredList(filterFolders(folders, query))
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length > 100) {
                    Toast.makeText(this@LibraryActivity, "Search is limited to 100 characters.", Toast.LENGTH_SHORT).show()
                    searchInput.setText(s.take(100))
                    searchInput.setSelection(searchInput.text.length)
                } else {
                    reloadFiltered(s.toString())
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Selection + delete logic unchanged
        val deleteButton = findViewById<ImageButton>(R.id.deleteButton)
        val selectionBar = findViewById<LinearLayout>(R.id.selectionBar)
        val bottomBar = findViewById<LinearLayout>(R.id.bottomBar)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val bombButton = findViewById<ImageButton>(R.id.bombButton)

        deleteButton.setOnClickListener {
            if (folderAdapter.getSelectedNotes().isEmpty()) {
                Toast.makeText(this, "Long-press a note to select.", Toast.LENGTH_SHORT).show()
            } else {
                bottomBar.visibility = View.GONE
                selectionBar.visibility = View.VISIBLE
            }
        }

        cancelButton.setOnClickListener {
            folderAdapter.exitSelectionMode()
            selectionBar.visibility = View.GONE
            bottomBar.visibility = View.VISIBLE
        }

        bombButton.setOnClickListener {
            val selectedNotes = folderAdapter.getSelectedNotes()
            if (selectedNotes.isEmpty()) {
                Toast.makeText(this, "No notes selected.", Toast.LENGTH_SHORT).show()
            } else {
                selectedNotes.forEach { note -> noteViewModel.delete(note) }
                folderAdapter.exitSelectionMode()
                selectionBar.visibility = View.GONE
                bottomBar.visibility = View.VISIBLE
                Toast.makeText(this, "Deleted ${selectedNotes.size} notes.", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<ImageButton>(R.id.favoritesToggle).setOnClickListener {
            if (folderAdapter.getSelectedNotes().isNotEmpty()) {
                val selectedNotes = folderAdapter.getSelectedNotes()
                val makeFavorite = selectedNotes.any { !it.isFavorite }
                selectedNotes.forEach { note ->
                    note.isFavorite = makeFavorite
                    noteViewModel.update(note)
                }
                Toast.makeText(this, if (makeFavorite) "Favorited ${selectedNotes.size} notes." else "Unfavorited ${selectedNotes.size} notes.", Toast.LENGTH_SHORT).show()
                folderAdapter.exitSelectionMode()
                selectionBar.visibility = View.GONE
                bottomBar.visibility = View.VISIBLE
            } else {
                favoritesOnly = !favoritesOnly
                reloadFiltered(searchInput.text.toString())
                val favoritesIcon = findViewById<ImageButton>(R.id.favoritesToggle)
                favoritesIcon.setImageResource(if (favoritesOnly) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
                Toast.makeText(this, if (favoritesOnly) "Showing favorites only." else "Showing all notes.", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<ImageButton>(R.id.sortToggle).setOnClickListener {
            sortNewest = !sortNewest
            reloadFiltered(searchInput.text.toString())
        }
    }

    private fun showOverlay(note: NoteEntity) {
        currentOverlayNote = note
        overlayContainer.removeAllViews()

        val inflater = layoutInflater
        val overlayView = inflater.inflate(R.layout.item_note, overlayContainer, false)
        val noteText = overlayView.findViewById<EditText>(R.id.noteText)
        noteText.setText(note.content)
        noteText.requestFocus()

        noteText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val updated = currentOverlayNote?.copy(
                    content = s.toString(),
                    lastEdited = System.currentTimeMillis()
                )
                if (updated != null) {
                    currentOverlayNote = updated
                    lifecycleScope.launch(Dispatchers.IO) {
                        noteViewModel.update(updated)
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(noteText, InputMethodManager.SHOW_IMPLICIT)


        val xButton = ImageButton(this).apply {
            setImageResource(R.drawable.ic_close)
            background = null
            setOnClickListener { closeOverlay() }
            layoutParams = FrameLayout.LayoutParams(100, 100, Gravity.TOP or Gravity.END).apply {
                marginEnd = 16
                topMargin = 16
            }
        }

        overlayContainer.addView(overlayView)
        overlayContainer.addView(xButton)
        overlayContainer.setBackgroundColor(resources.getColor(R.color.black, theme))
        overlayContainer.visibility = View.VISIBLE
    }

    private fun closeOverlay() {
        currentOverlayNote?.let { note ->
            val overlayView = overlayContainer.getChildAt(0)
            val noteText = overlayView?.findViewById<EditText>(R.id.noteText)
            val updatedContent = noteText?.text?.toString() ?: ""

            val updatedNote = note.copy(
                content = updatedContent,
                lastEdited = System.currentTimeMillis()
            )

            // Save immediately
            noteViewModel.update(updatedNote)

            // Update the current reference to the note
            currentOverlayNote = updatedNote
        }

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(overlayContainer.windowToken, 0)

        overlayContainer.removeAllViews()
        overlayContainer.visibility = View.GONE
        currentOverlayNote = null
    }


    override fun onBackPressed() {
        if (overlayContainer.visibility == View.VISIBLE) {
            closeOverlay()
        } else {
            super.onBackPressed()
        }
    }
    override fun onPause() {
        super.onPause()
        if (overlayContainer.visibility == View.VISIBLE) {
            currentOverlayNote?.let { note ->
                val overlayView = overlayContainer.getChildAt(0)
                val noteText = overlayView?.findViewById<EditText>(R.id.noteText)
                val updatedContent = noteText?.text?.toString() ?: ""

                lifecycleScope.launch(Dispatchers.IO) {
                    val updatedNote = note.copy(
                        content = updatedContent,
                        lastEdited = System.currentTimeMillis()
                    )
                    noteViewModel.update(updatedNote)
                }
            }
        }
    }

    private fun reloadFiltered(query: String) {
        noteViewModel.getFoldersWithPreviews().observe(this) { folders ->
            folderAdapter.updateFilteredList(filterFolders(folders, query))
        }
    }

    private fun filterFolders(original: List<FolderWithNotes>, query: String): List<FolderWithNotes> {
        val result = mutableListOf<FolderWithNotes>()
        for (folder in original) {
            val filteredNotes = folder.notes.filter { note ->
                var match = true
                if (query.isNotBlank() && !note.content.contains(query, ignoreCase = true)) {
                    match = false
                }
                if (favoritesOnly && !note.isFavorite) {
                    match = false
                }
                match
            }.sortedWith(if (sortNewest) compareByDescending { it.createdAt } else compareBy { it.createdAt })
            if (filteredNotes.isNotEmpty()) {
                result.add(FolderWithNotes(folder.folder, filteredNotes))
            }
        }
        return result
    }
}
