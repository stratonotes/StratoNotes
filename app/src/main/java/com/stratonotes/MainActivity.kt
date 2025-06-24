package com.stratonotes

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.punchpad2.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Typeface
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.NestedScrollView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.first
import androidx.core.view.isVisible
import kotlinx.coroutines.async
import android.os.Looper

import androidx.core.graphics.toColorInt
import androidx.core.content.edit
import android.net.Uri
import android.view.LayoutInflater


class MainActivity : ComponentActivity() {

    private val internalStratoFolder = "StratoNotes"

    private lateinit var previewContainer: LinearLayout
    private lateinit var overlayContainer: FrameLayout
    private lateinit var adView: View
    private var currentOverlayNote: NoteEntity? = null


    private lateinit var searchInput: EditText
    private lateinit var noteInput: EditText
    private lateinit var filterButton: ImageButton
    private lateinit var submitButton: Button
    private lateinit var clearDraftButton: Button

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

    private var presetFolderName = "QuickNotes" // User-editable display label

    private val draftHandler = Handler(Looper.getMainLooper())

    private var draftRunnable: Runnable? = null

    private val prefsName = "SubmitPrefs"
    private val keyMode = "lastMode"
    private val keyFolderName = "lastFolderName"
    private val draftPrefsName = "DraftPrefs"
    private val keyDraftNote = "draft_note"

    private val themeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val color = intent?.getIntExtra("color", -1) ?: return
            applyThemeColor(color)
        }
    }

    private fun applyThemeColor(color: Int) {
        val root = findViewById<View>(R.id.rootContainer)
        root.setBackgroundColor(color)

        val noteColor = UserColorManager.getNoteColor(this)
        val folderColor = UserColorManager.getFolderColor(this)

        // Update edit card background
        val noteInputCard = findViewById<MaterialCardView>(R.id.note_input_card)
        noteInputCard.setCardBackgroundColor(noteColor)

        // Update actual EditText background
        val noteInput = findViewById<EditText>(R.id.note_input)
        noteInput.setBackgroundColor(noteColor)

        @Suppress("ClickableViewAccessibility")
        noteInput.setOnTouchListener { view, _ ->
            if (searchDropdown.isVisible) {
                searchDropdown.visibility = View.GONE
                view.performClick()
            }
            false
        }

        // Tint textbox wrapper background
        val textboxWrapper = findViewById<View>(R.id.textboxWrapper)
        textboxWrapper.background?.mutate()?.let {
            DrawableCompat.setTint(it, noteColor)
            textboxWrapper.background = it
        }

        // Update overlay card if visible
        if (overlayContainer.isVisible) {
            val overlayView = overlayContainer.getChildAt(0)
            val noteCard = overlayView?.findViewById<MaterialCardView>(R.id.noteCard)
            noteCard?.setCardBackgroundColor(noteColor)
        }

        // Re-tint plus buttons on main and overlay if present
        val plusMain = findViewById<ImageButton>(R.id.iconPlus)
        plusMain?.background?.mutate()?.let {
            DrawableCompat.setTint(it, color)
            plusMain.background = it
        }
        if (overlayContainer.isVisible) {
            val overlayView = overlayContainer.getChildAt(0)
            val plusOverlay = overlayView?.findViewById<ImageButton>(R.id.iconPlus)
            plusOverlay?.background?.mutate()?.let {
                DrawableCompat.setTint(it, color)
                plusOverlay.background = it
            }
        }


        // Update buttons
        refreshSlideoutColors()

        submitButton.setBackgroundColor(folderColor)
        DrawableCompat.setTint(clearDraftButton.background.mutate(), folderColor)


        // Update preview notes
        loadPreviews()
    }


    @SuppressLint("CutPasteId", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        noteInput = findViewById(R.id.note_input)
        val root = findViewById<CoordinatorLayout>(R.id.rootContainer) // or your outermost layout

        val overlay = layoutInflater.inflate(R.layout.wordchar_counter_overlay, root, false)
        root.addView(overlay)

        val counterText = overlay.findViewById<TextView>(R.id.wordCharCounter)
        noteInput.addTextChangedListener(object : TextWatcher {
            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""
                val words = text.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
                val chars = text.length
                counterText.text = "$words\n$chars"
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val parent = findViewById<ViewGroup>(R.id.note_input_card)
        val menuView = layoutInflater.inflate(R.layout.widget_pill_menu, parent, false)

        parent.addView(menuView)
        menuView.elevation = 20f


        this.initPillMenu(menuView)

        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val appColor = prefs.getInt("app_color", "#5D53A3".toColorInt())
        val noteColor = UserColorManager.getNoteColor(this)
        val folderColor = UserColorManager.getFolderColor(this)

        root.setBackgroundColor(appColor)

        val plus = menuView.findViewById<ImageButton>(R.id.iconPlus)
        DrawableCompat.setTint(plus.background.mutate(), appColor)

        val pill = menuView.findViewById<LinearLayout>(R.id.pillContainer)
        pill.background?.mutate()?.let {
            DrawableCompat.setTint(it, appColor)
            pill.background = it
        }

        noteViewModel.ensureStratoNotesFolder()




        previewContainer = findViewById(R.id.previewContainer)
        overlayContainer = findViewById(R.id.overlayContainer)
        adView = findViewById(R.id.dev_ad_banner)
        searchInput = findViewById(R.id.searchInput)

        filterButton = findViewById(R.id.filter_button)
        submitButton = findViewById(R.id.submit_button)
        clearDraftButton = findViewById(R.id.clear_draft_button)
        undoManager = UndoManager(noteInput)

        undoButton = findViewById(R.id.undo_button)
        redoButton = findViewById(R.id.redo_button)
        folderSettingsButton = findViewById(R.id.folder_settings_button_1)

        val textboxWrapper = findViewById<View>(R.id.textboxWrapper)

        textboxWrapper.background?.mutate()?.let {
            DrawableCompat.setTint(it, noteColor)
            textboxWrapper.background = it
        }
        val noteCard = findViewById<MaterialCardView>(R.id.note_input_card)

        noteCard.setCardBackgroundColor(noteColor)

        folderSettingsButton.setOnClickListener {
            showPresetFolderDialog()
        }

        searchDropdown = findViewById(R.id.searchResultsDropdown)

        val dropdownColor = UserColorManager.getNoteColor(this)
        searchDropdown.setBackgroundColor(dropdownColor)

        root.setOnTouchListener { _, _ ->
            if (searchDropdown.isVisible) {
                searchDropdown.visibility = View.GONE
            }
            root.performClick()
            false
        }


        searchAdapter = SearchResultAdapter(this) { item ->
            when (item) {
                is SearchResultItem.NoteItem -> {
                    searchDropdown.visibility = View.GONE
                    showOverlay(item.note)
                }

                is SearchResultItem.FolderItem -> {
                    startActivity(Intent(this, LibraryActivity::class.java).apply {
                        putExtra("query", item.folder.name)
                    })
                }

                else -> {
                    // no-op (required for exhaustiveness)
                }
            }
        }


        searchDropdown.adapter = searchAdapter
        searchDropdown.layoutManager = LinearLayoutManager(this)

// ✅ This now runs safely after adapter init
        searchDropdown.viewTreeObserver.addOnGlobalLayoutListener {
            val itemHeightPx = resources.getDimensionPixelSize(R.dimen.search_result_item_height)
            val visibleItems = minOf(searchAdapter.itemCount, 4)
            val desiredHeight = itemHeightPx * visibleItems

            searchDropdown.layoutParams.height = desiredHeight
            searchDropdown.requestLayout()
        }



        searchDropdown.adapter = searchAdapter



        searchDropdown.layoutManager = LinearLayoutManager(this)

        noteInput.setBackgroundColor(noteColor)
        DrawableCompat.setTint(submitButton.background.mutate(), folderColor)
        clearDraftButton.setBackgroundColor(folderColor)

        val overlayBackdrop = findViewById<View>(R.id.overlayBackdrop)
        overlayBackdrop.setOnClickListener { closeOverlay() }

        submitButton.setOnClickListener {
            val content = noteInput.text.toString().trim()

            if (content.isEmpty()) {
                cycleMode()
                saveSubmitModeToPrefs(
                    currentMode.name,
                    if (currentMode == SaveMode.PRESET) internalStratoFolder else lastUsedFolder
                )
                updateSubmitLabel()


                return@setOnClickListener
            }

            when (currentMode) {
                SaveMode.NEW -> showNewFolderDialog(content)
                SaveMode.RECENT -> showConfirmDialog(content, lastUsedFolder)
                SaveMode.PRESET -> saveNote(content, internalStratoFolder)
            }

            // ✅ Also apply tint again after saving (covers shape fallback case)
            DrawableCompat.setTint(submitButton.background.mutate(), folderColor)
        }



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

        val draft = getSharedPreferences(draftPrefsName, MODE_PRIVATE).getString(keyDraftNote, null)
        if (!draft.isNullOrEmpty()) {
            noteInput.setText(draft)
            clearDraftButton.visibility = View.VISIBLE
            Toast.makeText(this, "Unsaved note restored", Toast.LENGTH_SHORT).show()
        }

        clearDraftButton.setOnClickListener {
            noteInput.setText("")
            getSharedPreferences(draftPrefsName, MODE_PRIVATE).edit { remove(keyDraftNote) }
            clearDraftButton.visibility = View.GONE
        }

        undoButton.setOnClickListener {
            if (undoManager.canUndo()) {
                undoManager.undo()

            }
        }

        redoButton.setOnClickListener { undoManager.redo() }

        noteInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isTyping = true
                draftRunnable?.let { draftHandler.removeCallbacks(it) }
                draftRunnable = Runnable {
                    getSharedPreferences(draftPrefsName, MODE_PRIVATE).edit {
                        putString(keyDraftNote, noteInput.text.toString())
                    }
                }
                draftHandler.postDelayed(draftRunnable!!, 500)
            }
        })

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                val query = editable?.toString()?.trim() ?: ""

                if (query.isEmpty()) {
                    searchAdapter.submitList(emptyList())
                    searchDropdown.visibility = View.GONE
                    return
                }

                val safeQuery = query
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_")

                lifecycleScope.launch {
                    val foldersDeferred = async(Dispatchers.IO) {
                        AppDatabase.getDatabase(this@MainActivity)
                            .noteDao().searchFolders("%$safeQuery%")
                    }

                    val noteResults = withContext(Dispatchers.IO) {
                        AppDatabase.getDatabase(this@MainActivity)
                            .noteDao().searchNotesRaw("%$safeQuery%")
                    }

                    val folders = foldersDeferred.await()
                    val folderItems = folders.map { SearchResultItem.FolderItem(it) }
                    val noteItems = noteResults.map { SearchResultItem.NoteItem(it) }

                    val combined = (folderItems + noteItems).sortedBy {
                        when (it) {
                            is SearchResultItem.FolderItem -> it.folder.name.lowercase()
                            is SearchResultItem.NoteItem -> it.note.content.lowercase()
                            else -> ""
                        }
                    }


                    searchAdapter.submitList(combined)

                    if (combined.isNotEmpty()) {
                        if (searchDropdown.visibility != View.VISIBLE) {
                            searchDropdown.alpha = 0f
                            searchDropdown.visibility = View.VISIBLE
                            searchDropdown.animate().alpha(1f).setDuration(150).start()
                        }
                    } else {
                        searchDropdown.animate().alpha(0f).setDuration(100).withEndAction {
                            searchDropdown.visibility = View.GONE
                        }.start()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })


        filterButton.setOnClickListener {
            startActivity(Intent(this, LibraryActivity::class.java).apply {
                putExtra("query", searchInput.text.toString())
            })
        }

        folderSettingsButton.setOnClickListener {
            if (currentMode == SaveMode.PRESET) {
                showPresetFolderDialog()
            } else {
                val dialog = ColorPickerDialog(this, root)
                dialog.show()
            }
        }

        // TODO: animate search bar in/out later
        //val searchBarContainer = findViewById<View>(R.id.searchBarContainer)


        searchDropdown.post {
            val topOffset = findViewById<View>(R.id.searchBarContainer).bottom

            val screenHeight = resources.displayMetrics.heightPixels
            val desiredHeightInPx = (screenHeight * 0.45).toInt()

            val params = CoordinatorLayout.LayoutParams(
                CoordinatorLayout.LayoutParams.MATCH_PARENT,
                desiredHeightInPx
            )
            params.topMargin = topOffset
            searchDropdown.layoutParams = params

            searchDropdown.setBackgroundColor(dropdownColor)

        }




        loadSubmitModeFromPrefs()
        updateSubmitLabel()
        loadPreviews()


    }


    private fun showPresetFolderDialog() {
        val input = EditText(this)
        input.hint = "New folder name"
        AlertDialog.Builder(this)
            .setTitle("Rename Preset Folder")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    presetFolderName = name
                    updateSubmitLabel()
                } else {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshSlideoutColors() {
        val appColor = UserColorManager.getAppColor(this)

        val pill = findViewById<LinearLayout>(R.id.pillContainer)
        val pillBg = ContextCompat.getDrawable(this, R.drawable.pill_menu_bg)?.mutate()
        if (pillBg != null) {
            DrawableCompat.setTint(pillBg, appColor)
            pill.background = pillBg
        }


    }

    private fun showOverlay(note: NoteEntity) {

        currentOverlayNote = note

        overlayContainer.removeAllViews()
        val inflater = layoutInflater


        val overlayView = inflater.inflate(R.layout.item_note, overlayContainer, false)

        val metrics = resources.displayMetrics
        val width = (metrics.widthPixels * 0.9).toInt()
        overlayView.layoutParams = FrameLayout.LayoutParams(
            width,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )


        val backgroundColor = UserColorManager.getNoteColor(this)
        overlayView.findViewById<MaterialCardView>(R.id.noteCard)
            .setCardBackgroundColor(backgroundColor)


        val noteCard = overlayView.findViewById<MaterialCardView>(R.id.noteCard)


        val userColor = UserColorManager.getNoteColor(this)

        noteCard.setCardBackgroundColor(userColor)

        val scroll = overlayView.findViewById<NestedScrollView>(R.id.noteScroll)
        scroll.isNestedScrollingEnabled = true

        val overlayColor = UserColorManager.getOverlayColor(this)


        val noteText = overlayView.findViewById<EditText>(R.id.noteText)
        val starIcon = overlayView.findViewById<ImageView>(R.id.starIcon)

        val undoButton = overlayView.findViewById<ImageButton>(R.id.undo_button)
        val redoButton = overlayView.findViewById<ImageButton>(R.id.redo_button)

        val overlayUndoManager = UndoManager(noteText)

        undoButton.setOnClickListener {
            overlayUndoManager.undo()
        }

        redoButton.setOnClickListener {
            overlayUndoManager.redo()
        }

        noteText.setText(note.content)
        noteText.requestFocus()

        val wordCounter = overlayView.findViewById<TextView>(R.id.wordCount)
        val charCounter = overlayView.findViewById<TextView>(R.id.charCount)
        bindTextCounter(noteText, wordCounter, charCounter)

        // Set initial star icon state
        starIcon.setImageResource(if (note.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline)

        // Star icon toggle logic
        starIcon.setOnClickListener {
            // Always capture the latest text from noteText
            val updatedContent = noteText.text.toString()
            val updatedFavorite = !currentOverlayNote!!.isFavorite

            // Update currentOverlayNote with the latest state
            currentOverlayNote = currentOverlayNote?.copy(
                content = updatedContent,
                isFavorite = updatedFavorite,
                lastEdited = System.currentTimeMillis()
            )

            // Update UI star icon
            starIcon.setImageResource(
                if (currentOverlayNote!!.isFavorite) R.drawable.ic_star_filled
                else R.drawable.ic_star_outline
            )

            // Save to DB
            lifecycleScope.launch(Dispatchers.IO) {
                noteViewModel.update(currentOverlayNote!!)
            }
        }


        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(noteText, InputMethodManager.SHOW_IMPLICIT)

        // Insert pill menu inside noteContainer
        val container = overlayView.findViewById<LinearLayout>(R.id.noteContainer)
        val pillMenu = layoutInflater.inflate(R.layout.widget_pill_menu, container, false)
        pillMenu.tag = "pillMenu"
        container.addView(pillMenu)
        initPillMenu(pillMenu)
        refreshSlideoutColors() // also tint it


        overlayContainer.addView(overlayView)


        overlayContainer.visibility = View.VISIBLE

        val closeButton = overlayView.findViewById<ImageView>(R.id.closeButton)
        closeButton.setOnClickListener { closeOverlay() }

    }


    private fun closeOverlay() {
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

                withContext(Dispatchers.Main) {
                    loadPreviews()
                }
            }
        }

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(overlayContainer.windowToken, 0)

        overlayContainer.removeAllViews()
        overlayContainer.visibility = View.GONE
        currentOverlayNote = null
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
                getSharedPreferences(draftPrefsName, MODE_PRIVATE).edit {
                    remove(keyDraftNote)
                }
                clearDraftButton.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Saved to $folderName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadPreviews() {
        lifecycleScope.launch(Dispatchers.IO) {
            val notes = AppDatabase.getDatabase(this@MainActivity)
                .noteDao().get3MostRecentVisibleNotes()

            withContext(Dispatchers.Main) {
                previewContainer.removeAllViews()

                // ✅ Move color fetch *outside* loop so it's consistent and fresh
                val noteColor = UserColorManager.getNoteColor(this@MainActivity)
                val folderColor = UserColorManager.getFolderColor(this@MainActivity)

                for (note in notes) {
                    val previewView = layoutInflater.inflate(
                        R.layout.item_note_preview,
                        previewContainer,
                        false
                    )
                    val noteText = previewView.findViewById<EditText>(R.id.noteText)
                    val fadeView = previewView.findViewById<View>(R.id.noteFade)

                    noteText.setText(note.content)
                    noteText.setOnClickListener { showOverlay(note) }

                    val bgRes = if (note.content.length > 150) {
                        R.drawable.note_expanded_background
                    } else {
                        R.drawable.note_preview_background
                    }

                    val drawable = ContextCompat.getDrawable(this@MainActivity, bgRes)?.mutate()
                    if (drawable != null) {
                        DrawableCompat.setTint(drawable, noteColor)
                        previewView.background = drawable
                    } else {
                        previewView.setBackgroundColor(noteColor)
                    }

                    if (note.content.length > 150) {
                        val fadeTop = ColorUtils.setAlphaComponent(folderColor, 0)
                        val gradient = GradientDrawable(
                            GradientDrawable.Orientation.BOTTOM_TOP,
                            intArrayOf(folderColor, fadeTop)
                        )
                        gradient.cornerRadius = 0f
                        fadeView.background = gradient
                        fadeView.visibility = View.VISIBLE
                    } else {
                        fadeView.visibility = View.GONE
                    }


                    previewContainer.addView(previewView)
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
                submitButton.text = getString(R.string.button_add_to_new_folder)
                folderSettingsButton.visibility = View.GONE
            }

            SaveMode.RECENT -> {
                val formatted = getString(R.string.button_add_to_prefix) + " $lastUsedFolder"
                submitButton.text = formatted
                folderSettingsButton.visibility = View.VISIBLE
            }

            SaveMode.PRESET -> {
                val formatted =
                    getString(R.string.button_add_to_prefix) + " $presetFolderName " + getString(R.string.button_preset_suffix)
                submitButton.text = formatted
                folderSettingsButton.visibility = View.GONE
            }
        }




        updateSubmitButtonFont()

        if (currentMode == SaveMode.PRESET) {
            noteInput.requestFocus()
            noteInput.postDelayed({
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(noteInput, InputMethodManager.SHOW_IMPLICIT)
            }, 100)
        }
    }

    private fun updateSubmitButtonFont() {
        val typeface = if (currentMode == SaveMode.PRESET) {
            ResourcesCompat.getFont(this, R.font.orbitron_regular)
        } else {
            null
        }

        submitButton.typeface = typeface
        submitButton.setTypeface(typeface, Typeface.BOLD_ITALIC)
        submitButton.textSize = 18f
    }

    private fun showNewFolderDialog(content: String) {
        val context = this
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 8)
        }

        val input = EditText(context).apply {
            hint = "Folder name"
        }

        val scroll = ScrollView(context)
        val list = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        scroll.addView(list)
        container.addView(input)
        container.addView(scroll)

        lifecycleScope.launch(Dispatchers.IO) {
            val all = AppDatabase.getDatabase(context).noteDao().getAllFolders().first()
            val folders = all
                .filter { it.name != internalStratoFolder }
                .map { it.name }



            withContext(Dispatchers.Main) {
                folders.forEach { name ->
                    val label = TextView(context).apply {
                        text = name
                        textSize = 16f
                        setPadding(8, 8, 8, 16)
                        setTextColor(Color.WHITE)
                        setOnClickListener {
                            input.setText(name)
                        }
                    }
                    list.addView(label)
                }



                AlertDialog.Builder(context)
                    .setTitle("Create or Select Folder")
                    .setView(container)
                    .setPositiveButton("Save") { _, _ ->
                        val folderName = input.text.toString().trim()
                        if (folderName.isNotEmpty()) {
                            saveNote(content, folderName)
                            lastUsedFolder = folderName
                        } else {
                            Toast.makeText(
                                context,
                                "Folder name can't be empty",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun showConfirmDialog(content: String, folderName: String) {
        AlertDialog.Builder(this)
            .setTitle("Save to $folderName?")
            .setPositiveButton("Yes") { _, _ -> saveNote(content, folderName) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun bindTextCounter(editText: EditText, wordView: TextView, charView: TextView) {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""
                val wordCount = text.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
                val charCount = text.length
                wordView.text = wordCount.toString()
                charView.text = charCount.toString()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        editText.addTextChangedListener(watcher)

        // Initialize immediately
        val currentText = editText.text?.toString() ?: ""
        wordView.text =
            currentText.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size.toString()
        charView.text = currentText.length.toString()
    }

    private fun saveSubmitModeToPrefs(mode: String, folderName: String) {
        getSharedPreferences(prefsName, MODE_PRIVATE).edit().apply {
            putString(keyMode, mode)
            putString(keyFolderName, folderName)
            apply()
        }
    }

    private fun loadSubmitModeFromPrefs() {
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        currentMode = when (prefs.getString(keyMode, "RECENT")) {
            "NEW" -> SaveMode.NEW
            "PRESET" -> SaveMode.PRESET
            else -> SaveMode.RECENT
        }
        prefs.getString(keyFolderName, "")?.let {
            if (it.isNotEmpty()) lastUsedFolder = it
        }
    }

    override fun onResume() {
        super.onResume()

        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val appColor = prefs.getInt("app_color", "#5D53A3".toColorInt())
        applyThemeColor(appColor)

        LocalBroadcastManager.getInstance(this).registerReceiver(
            themeReceiver, IntentFilter("com.stratonotes.THEME_COLOR_CHANGED")
        )
    }


    @Suppress("RemoveExplicitTypeArguments")
    private fun initPillMenu(rootView: View) {
        val pill = rootView.findViewById<LinearLayout>(R.id.pillContainer)
        val plus = rootView.findViewById<ImageButton>(R.id.iconPlus)

        val iconAddImage = rootView.findViewById<ImageButton>(R.id.iconAddImage)
        iconAddImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            startActivityForResult(Intent.createChooser(intent, "Select Images"), 101)
        }

        val icons = listOf(
            iconAddImage,
            rootView.findViewById<ImageButton>(R.id.iconAddAudio),
            rootView.findViewById<ImageButton>(R.id.iconMore),
            rootView.findViewById<ImageButton>(R.id.iconDelete)
        )

        var expanded = false

        plus.setOnClickListener {
            expanded = !expanded

            if (expanded) pill.visibility = View.VISIBLE

            val iconWidthPx = (40 * rootView.resources.displayMetrics.density).toInt()
            val paddingPx = (24 * rootView.resources.displayMetrics.density).toInt()
            val targetWidth = if (expanded) (iconWidthPx * (icons.size + 1) + paddingPx) else 0

            val animator = ValueAnimator.ofInt(pill.width, targetWidth).apply {
                duration = 250
                addUpdateListener { animation ->
                    val value = animation.animatedValue as Int
                    pill.layoutParams.width = value
                    pill.requestLayout()
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        if (!expanded) pill.visibility = View.GONE
                    }
                })
            }

            animator.start()

            val rotation = if (expanded) 45f else 0f
            ObjectAnimator.ofFloat(plus, View.ROTATION, rotation).apply {
                duration = 200
                start()
            }

            icons.forEach { icon ->
                icon.visibility = if (expanded) View.VISIBLE else View.GONE
            }
        }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            val uriList = mutableListOf<Uri>()

            // Handle multiple or single image selection
            data.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) {
                    clip.getItemAt(i).uri?.let { uriList.add(it) }
                }
            } ?: data.data?.let { uriList.add(it) }

            // Determine the correct note input EditText
            val editText = if (overlayContainer.isVisible) {
                overlayContainer.getChildAt(0)
                    ?.findViewById<ScrollView>(R.id.noteScroll)
                    ?.findViewById<EditText>(R.id.noteText)
            } else {
                findViewById<EditText>(R.id.note_input)
            }

            if (editText != null) {
                uriList.forEach { uri -> insertImage(uri, editText) }
            }
        }
    }


    fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun insertImage(uri: Uri, target: EditText) {
        val inflater = LayoutInflater.from(this)
        val imageBlock = inflater.inflate(R.layout.image_block, null) as FrameLayout
        val imageView = imageBlock.findViewById<ImageView>(R.id.imageContent)
        val handleLeft = imageBlock.findViewById<View>(R.id.resizeHandleLeft)
        val handleRight = imageBlock.findViewById<View>(R.id.resizeHandleRight)

        val appColor = UserColorManager.getAppColor(this)
        val fillColor = UserColorManager.getNoteColor(this)
        val strokeColor = UserColorManager.getCancelColorRelativeTo(appColor)

        fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

        listOf(handleLeft, handleRight).forEach { handle ->
            val drawable = handle.background?.mutate() as? GradientDrawable
            drawable?.setColor(fillColor)
            drawable?.setStroke(2.dpToPx(), strokeColor)
        }

        imageView.setImageURI(uri)

        imageBlock.setOnClickListener {
            handleLeft.visibility = View.VISIBLE
            handleRight.visibility = View.VISIBLE
        }

        var dX = 0f
        var dY = 0f

        imageBlock.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    v.animate()
                        .x(event.rawX + dX)
                        .y(event.rawY + dY)
                        .setDuration(0)
                        .start()
                }
                MotionEvent.ACTION_UP -> {
                    v.parent.requestDisallowInterceptTouchEvent(false)
                    v.performClick()
                }
            }
            true
        }

        val resizeTouchListener = View.OnTouchListener { handle, event ->
            val params = imageView.layoutParams as ViewGroup.MarginLayoutParams
            val parentWidth = (imageBlock.parent as? ViewGroup)?.width ?: return@OnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = event.rawX
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - dX
                    dX = event.rawX

                    if (handle.id == R.id.resizeHandleLeft) {
                        params.marginStart = (params.marginStart + deltaX).toInt().coerceIn(0, parentWidth - 100)
                    } else if (handle.id == R.id.resizeHandleRight) {
                        params.marginEnd = (params.marginEnd - deltaX).toInt().coerceIn(0, parentWidth - 100)
                    }

                    imageView.layoutParams = params
                    true
                }

                else -> false
            }
        }

        handleLeft.setOnTouchListener(resizeTouchListener)
        handleRight.setOnTouchListener(resizeTouchListener)

        // ✅ Fix: find the true container that holds the EditText
        val container = findViewById<LinearLayout>(R.id.noteContainer)

        val index = container.indexOfChild(target)
        if (index != -1) {
            container.addView(imageBlock, index)
        }

    }


}
