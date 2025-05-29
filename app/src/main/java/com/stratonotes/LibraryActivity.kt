package com.stratonotes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.punchpad2.FolderAdapter
import com.example.punchpad2.R
import android.view.View
import android.widget.LinearLayout
import android.widget.Button

class LibraryActivity : ComponentActivity() {

    private lateinit var folderAdapter: FolderAdapter

    private var deleteMode = false
    private var favoritesOnly = false
    private var sortNewest = true

    private val noteViewModel: NoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        val folderRecycler = findViewById<RecyclerView>(R.id.folderRecycler)
        folderRecycler.layoutManager = LinearLayoutManager(this)

        val searchInput = findViewById<EditText>(R.id.searchInput)
        //val btnFilter = findViewById<ImageButton>(R.id.btnFilter)
        val backButton = findViewById<ImageButton>(R.id.strato_button)
        backButton.setOnClickListener {
            finish() // Closes LibraryActivity and returns to MainActivity
        }

        val query = intent.getStringExtra("query") ?: ""

        folderAdapter = FolderAdapter(
            context = this,
            folders = mutableListOf(),
            listener = { note, _ ->
                noteViewModel.update(note)
            },
            noteLayoutResId = R.layout.item_note_library // Pass the stripped-down layout for Library screen
        )

        folderRecycler.adapter = folderAdapter

        noteViewModel.getFoldersWithPreviews().observe(this) { folders ->
            Log.d("LibraryActivity", "Room gave us ${folders.size} folders")

            folders.forEach { fw ->
                val folderName = fw.folder?.name ?: "(null)"
                val noteCount = fw.notes?.size ?: -1
                Log.d("LibraryActivity", "Folder: $folderName → $noteCount notes")
            }

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

        //btnFilter.setOnClickListener {
        //    showFilterGrid(btnFilter)
        //}

        val deleteButton = findViewById<ImageButton>(R.id.deleteButton)
        val selectionBar = findViewById<LinearLayout>(R.id.selectionBar)
        val bottomBar = findViewById<LinearLayout>(R.id.bottomBar)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val bombButton = findViewById<ImageButton>(R.id.bombButton)

        deleteButton.setOnClickListener {
            if (folderAdapter.getSelectedNotes().isEmpty()) {
                Toast.makeText(this, "Long-press a note to select.", Toast.LENGTH_SHORT).show()
            } else {
                // Show selection mode UI
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
                selectedNotes.forEach { note ->
                    noteViewModel.delete(note)
                }
                folderAdapter.exitSelectionMode()
                selectionBar.visibility = View.GONE
                bottomBar.visibility = View.VISIBLE
                Toast.makeText(this, "Deleted ${selectedNotes.size} notes.", Toast.LENGTH_SHORT).show()
            }
        }



        findViewById<ImageButton>(R.id.favoritesToggle).setOnClickListener {
            if (folderAdapter.getSelectedNotes().isNotEmpty()) {
                // Selection mode: batch toggle favorites
                val selectedNotes = folderAdapter.getSelectedNotes()
                val makeFavorite = selectedNotes.any { !it.isFavorite } // If any are not favorite, mark all as favorite
                selectedNotes.forEach { note ->
                    note.isFavorite = makeFavorite
                    noteViewModel.update(note)
                }
                Toast.makeText(this, if (makeFavorite) "Favorited ${selectedNotes.size} notes." else "Unfavorited ${selectedNotes.size} notes.", Toast.LENGTH_SHORT).show()
                folderAdapter.exitSelectionMode()
                findViewById<LinearLayout>(R.id.selectionBar).visibility = View.GONE
                findViewById<LinearLayout>(R.id.bottomBar).visibility = View.VISIBLE
            } else {
                // Not in selection mode: toggle filter
                favoritesOnly = !favoritesOnly
                reloadFiltered(findViewById<EditText>(R.id.searchInput).text.toString())
                val favoritesIcon = findViewById<ImageButton>(R.id.favoritesToggle)
                if (favoritesOnly) {
                    favoritesIcon.setImageResource(R.drawable.ic_star_filled)
                } else {
                    favoritesIcon.setImageResource(R.drawable.ic_star_outline)
                }
                Toast.makeText(this, if (favoritesOnly) "Showing favorites only." else "Showing all notes.", Toast.LENGTH_SHORT).show()
            }
        }


        findViewById<ImageButton>(R.id.sortToggle).setOnClickListener {
            sortNewest = !sortNewest
            reloadFiltered(searchInput.text.toString())
        }
    }

    private fun showFilterGrid(anchor: ImageButton) {
        val popupView = LayoutInflater.from(this).inflate(R.layout.popup_filter_grid, null)
        val popupWindow = PopupWindow(popupView, RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT, true)

        popupView.findViewById<ImageButton>(R.id.filterSearch).setOnClickListener {
            Toast.makeText(this, "Search triggered", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        popupView.findViewById<ImageButton>(R.id.filterFavorites).setOnClickListener {
            favoritesOnly = !favoritesOnly
            reloadFiltered(findViewById<EditText>(R.id.searchInput).text.toString())
            popupWindow.dismiss()
        }

        popupView.findViewById<ImageButton>(R.id.filterNewest).setOnClickListener {
            sortNewest = true
            reloadFiltered(findViewById<EditText>(R.id.searchInput).text.toString())
            popupWindow.dismiss()
        }

        popupView.findViewById<ImageButton>(R.id.filterOldest).setOnClickListener {
            sortNewest = false
            reloadFiltered(findViewById<EditText>(R.id.searchInput).text.toString())
            popupWindow.dismiss()
        }

        popupWindow.showAsDropDown(anchor, 0, 8)
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
            }.sortedWith(
                if (sortNewest) compareByDescending { it.createdAt }
                else compareBy { it.createdAt }
            )

            if (filteredNotes.isNotEmpty()) {
                result.add(FolderWithNotes(folder.folder, filteredNotes))
            }
        }

        return result
    }
}
