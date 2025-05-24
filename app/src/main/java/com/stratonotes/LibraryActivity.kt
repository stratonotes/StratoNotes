package com.stratonotes

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
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

        findViewById<ImageButton>(R.id.deleteButton).setOnClickListener {
            deleteMode = !deleteMode
            folderAdapter.setDeleteMode(deleteMode)
            folderAdapter.notifyDataSetChanged()
        }

        findViewById<ImageButton>(R.id.favoritesToggle).setOnClickListener {
            favoritesOnly = !favoritesOnly
            reloadFiltered(query)
        }

        findViewById<ImageButton>(R.id.sortToggle).setOnClickListener {
            sortNewest = !sortNewest
            reloadFiltered(query)
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
