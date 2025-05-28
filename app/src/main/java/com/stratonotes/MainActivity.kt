package com.stratonotes

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.punchpad2.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var previewContainer: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var noteInput: EditText
    private lateinit var filterButton: ImageButton
    private lateinit var submitButton: Button
    private lateinit var clearDraftButton: Button
    private lateinit var plusButton: ImageButton
    private lateinit var mediaMenu: LinearLayout
    private lateinit var undoButton: ImageButton
    private lateinit var redoButton: ImageButton
    private lateinit var folderSettingsButton: ImageButton

    private lateinit var searchDropdown: RecyclerView
    private lateinit var searchAdapter: SearchResultAdapter
    private lateinit var undoManager: UndoManager

    private val noteViewModel: NoteViewModel by viewModels()

    private enum class SaveMode { NEW, RECENT, PRESET }
    private var currentMode = SaveMode.NEW
    private var isTyping = false

    private var lastUsedFolder = "Default"
    private val presetFolder = "StratoNote"

    private val draftHandler = Handler()
    private var draftRunnable: Runnable? = null
    private var clearFadeRunnable: Runnable? = null
    private var isClearFading = false

    private val PREFS_NAME = "SubmitPrefs"
    private val KEY_MODE = "lastMode"
    private val KEY_FOLDER = "lastFolderName"
    private val DRAFT_PREFS = "DraftPrefs"
    private val KEY_DRAFT_NOTE = "draft_note"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewContainer = findViewById(R.id.previewContainer)
        searchInput = findViewById(R.id.searchInput)
        noteInput = findViewById(R.id.note_input)
        filterButton = findViewById(R.id.filter_button)
        submitButton = findViewById(R.id.submit_button)
        clearDraftButton = findViewById(R.id.clear_draft_button)
        plusButton = findViewById(R.id.plus_button)
        mediaMenu = findViewById(R.id.media_menu)
        undoButton = findViewById(R.id.undo_button)
        redoButton = findViewById(R.id.redo_button)
        folderSettingsButton = findViewById(R.id.folder_settings_button_1)
        searchDropdown = findViewById(R.id.searchResultsDropdown)

        searchAdapter = SearchResultAdapter(this)
        searchDropdown.adapter = searchAdapter
        searchDropdown.layoutManager = LinearLayoutManager(this)

        undoManager = UndoManager(noteInput)

        @Suppress("ClickableViewAccessibility")
        noteInput.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> v.parent.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP -> {
                    v.parent.requestDisallowInterceptTouchEvent(false)
                    v.performClick()
                }
            }
            false
        }

        val draft = getSharedPreferences(DRAFT_PREFS, MODE_PRIVATE).getString(KEY_DRAFT_NOTE, null)
        if (!draft.isNullOrEmpty()) {
            noteInput.setText(draft)
            clearDraftButton.visibility = View.VISIBLE
            Toast.makeText(this, "Unsaved note restored", Toast.LENGTH_SHORT).show()
        }

        clearDraftButton.setOnClickListener {
            noteInput.setText("")
            getSharedPreferences(DRAFT_PREFS, MODE_PRIVATE).edit().remove(KEY_DRAFT_NOTE).apply()
            clearDraftButton.visibility = View.GONE
        }

        undoButton.setOnClickListener {
            if (undoManager.canUndo()) {
                undoManager.undo()
                if (isClearFading) {
                    draftHandler.removeCallbacks(clearFadeRunnable!!)
                    clearDraftButton.alpha = 1f
                    clearDraftButton.visibility = View.VISIBLE
                    isClearFading = false
                }
            }
        }

        redoButton.setOnClickListener {
            undoManager.redo()
        }

        loadSubmitModeFromPrefs()
        updateSubmitLabel()

        submitButton.setOnClickListener {
            val content = noteInput.text.toString().trim()
            if (content.isEmpty()) {
                cycleMode()
                saveSubmitModeToPrefs(currentMode.name, if (currentMode == SaveMode.PRESET) presetFolder else lastUsedFolder)
                updateSubmitLabel()
                return@setOnClickListener
            }

            when (currentMode) {
                SaveMode.NEW -> showNewFolderDialog(content)
                SaveMode.RECENT -> showConfirmDialog(content, lastUsedFolder)
                SaveMode.PRESET -> saveNote(content, presetFolder)
            }
        }

        noteInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isTyping = true
                draftRunnable?.let { draftHandler.removeCallbacks(it) }
                draftRunnable = Runnable {
                    getSharedPreferences(DRAFT_PREFS, MODE_PRIVATE).edit()
                        .putString(KEY_DRAFT_NOTE, noteInput.text.toString()).apply()
                }
                draftHandler.postDelayed(draftRunnable!!, 500)
            }
        })
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                val query = editable?.toString() ?: ""

                if (query.length > 100) {
                    Toast.makeText(this@MainActivity, "Search is limited to 100 characters.", Toast.LENGTH_SHORT).show()
                    return
                }

                if (query.isBlank()) {
                    searchDropdown.animate().alpha(0f).setDuration(100).withEndAction {
                        searchDropdown.visibility = View.GONE
                    }.start()
                    searchAdapter.submitList(emptyList())
                    return
                }

                noteViewModel.searchNotes(query).observe(this@MainActivity) { notes ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        val folders = AppDatabase.getDatabase(this@MainActivity)

                            .noteDao()
                            .searchFolders("%$query%")

                        val items = (folders.map { SearchResultItem.FolderItem(it) } +
                                notes.map { SearchResultItem.NoteItem(it) })
                            .sortedBy {
                                when (it) {
                                    is SearchResultItem.FolderItem -> it.folder.name.lowercase()
                                    is SearchResultItem.NoteItem -> it.note.content.lowercase()
                                    is SearchResultItem.Header -> TODO()
                                }
                            }



                        withContext(Dispatchers.Main) {
                            searchAdapter.submitList(items)
                            if (items.isNotEmpty()) {
                                searchDropdown.alpha = 0f
                                searchDropdown.visibility = View.VISIBLE
                                searchDropdown.animate().alpha(1f).setDuration(150).start()
                            } else {
                                searchDropdown.animate().alpha(0f).setDuration(100).withEndAction {
                                    searchDropdown.visibility = View.GONE
                                }.start()
                            }
                        }


                    }
                }
            }
        })


        filterButton.setOnClickListener {
            startActivity(Intent(this, LibraryActivity::class.java).apply {
                putExtra("query", searchInput.text.toString())
            })
        }

        plusButton.setOnClickListener {
            if (mediaMenu.visibility == View.VISIBLE) {
                mediaMenu.animate().translationX(mediaMenu.width.toFloat()).alpha(0f).setDuration(200).withEndAction {
                    mediaMenu.visibility = View.GONE
                }
            } else {
                mediaMenu.translationX = mediaMenu.width.toFloat()
                mediaMenu.alpha = 0f
                mediaMenu.visibility = View.VISIBLE
                mediaMenu.animate().translationX(0f).alpha(1f).setDuration(200).start()
            }
        }

        loadPreviews()
    }

    private fun saveNote(content: String, folderName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val db = AppDatabase.getDatabase(this@MainActivity)

            val noteDao = db.noteDao()

            var folder = noteDao.getFolderByName(folderName)
            if (folder == null) {
                val folderId = noteDao.insertFolder(FolderEntity(0L, folderName, now, now))
                folder = FolderEntity(folderId, folderName, now, now)
            }

            val note = NoteEntity(
                id = 0L,
                folderId = folder.id,
                content = content,
                createdAt = now,
                lastEdited = now,
                isFavorite = false,
                isHiddenFromMain = false,
                isLarge = false,
                isTrashed = false
            )

            noteDao.insertNote(note)

            withContext(Dispatchers.Main) {
                noteInput.setText("")
                isTyping = false
                updateSubmitLabel()
                loadPreviews()
                getSharedPreferences(DRAFT_PREFS, MODE_PRIVATE).edit().remove(KEY_DRAFT_NOTE).apply()
                clearDraftButton.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Saved to $folderName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadPreviews() {
        lifecycleScope.launch(Dispatchers.IO) {
            val notes = AppDatabase.getDatabase(this@MainActivity).noteDao().get3MostRecentVisibleNotes()
            withContext(Dispatchers.Main) {
                previewContainer.removeAllViews()
                for (note in notes) {
                    val preview = TextView(this@MainActivity).apply {
                        text = if (note.content.length > 100) note.content.substring(0, 100) + "..." else note.content
                        setPadding(0, 16, 0, 16)
                        setTextColor(0xFFFFFFFF.toInt())
                        setOnClickListener {
                            startActivity(Intent(this@MainActivity, NoteActivity::class.java).apply {
                                putExtra("content", note.content)
                            })
                        }
                    }
                    previewContainer.addView(preview)
                }
            }
        }
    }

    private fun cycleMode() {
        currentMode = when (currentMode) {
            SaveMode.NEW -> SaveMode.RECENT
            SaveMode.RECENT -> SaveMode.PRESET
            SaveMode.PRESET -> SaveMode.NEW
        }
    }

    private fun updateSubmitLabel() {
        when (currentMode) {
            SaveMode.NEW -> {
                submitButton.text = "Enter text → Add to New Folder"
                folderSettingsButton.visibility = View.GONE
            }
            SaveMode.RECENT -> {
                submitButton.text = "Add note to $lastUsedFolder"
                folderSettingsButton.visibility = View.VISIBLE
            }
            SaveMode.PRESET -> {
                submitButton.text = "Enter text → Add to $presetFolder"
                folderSettingsButton.visibility = View.GONE
            }
        }
    }

    private fun showNewFolderDialog(content: String) {
        val input = EditText(this)
        input.hint = "Folder name"
        AlertDialog.Builder(this)
            .setTitle("Create Folder")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val folderName = input.text.toString().trim()
                if (folderName.isNotEmpty()) {
                    saveNote(content, folderName)
                    lastUsedFolder = folderName
                } else {
                    Toast.makeText(this, "Folder name can't be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showConfirmDialog(content: String, folderName: String) {
        AlertDialog.Builder(this)
            .setTitle("Save to $folderName?")
            .setPositiveButton("Yes") { _, _ -> saveNote(content, folderName) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveSubmitModeToPrefs(mode: String, folderName: String) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
            putString(KEY_MODE, mode)
            putString(KEY_FOLDER, folderName)
            apply()
        }
    }

    private fun loadSubmitModeFromPrefs() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        when (prefs.getString(KEY_MODE, "RECENT")) {
            "NEW" -> currentMode = SaveMode.NEW
            "PRESET" -> currentMode = SaveMode.PRESET
            else -> currentMode = SaveMode.RECENT
        }
        prefs.getString(KEY_FOLDER, "")?.let {
            if (it.isNotEmpty()) lastUsedFolder = it
        }
    }
}
