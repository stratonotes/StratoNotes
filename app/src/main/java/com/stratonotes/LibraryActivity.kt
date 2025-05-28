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
        val btnFilter = findViewById<ImageButton>(R.id.btnFilter)

        val query = intent.getStringExtra("query") ?: ""

        folderAdapter = FolderAdapter(
            this,
            mutableListOf(),
            { note -> noteViewModel.update(note) }
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

        btnFilter.setOnClickListener {
            showFilterGrid(btnFilter)
        }

        findViewById<ImageButton>(R.id.deleteButton).setOnClickListener {
            deleteMode = !deleteMode
            folderAdapter.setDeleteMode(deleteMode)
            folderAdapter.notifyDataSetChanged()
        }

        findViewById<ImageButton>(R.id.favoritesToggle).setOnClickListener {
            favoritesOnly = !favoritesOnly
            reloadFiltered(searchInput.text.toString())
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
