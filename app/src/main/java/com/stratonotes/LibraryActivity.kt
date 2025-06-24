package com.stratonotes

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import android.content.res.Resources
import android.content.Intent
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.Uri
import android.view.ViewGroup
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.card.MaterialCardView
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.activity.OnBackPressedCallback

class LibraryActivity : ComponentActivity() {


    private lateinit var folderAdapter: FolderAdapter



    private var favoritesOnly = false
    private var sortNewest = true

    private val noteViewModel: NoteViewModel by viewModels()
    private lateinit var overlayContainer: FrameLayout
    private var currentOverlayNote: NoteEntity? = null

    private val themeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val color = intent?.getIntExtra("color", -1) ?: return
            applyThemeColor(color)
        }
    }


    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val appColor = prefs.getInt("app_color", "#5D53A3".toColorInt())

        val root = findViewById<View>(R.id.rootContainer)
        root.setBackgroundColor(appColor)

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
        val bombButton = findViewById<Button>(R.id.bombButton)

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
        val cancelColor = UserColorManager.getCancelColorRelativeTo(UserColorManager.getAppColor(this))
        bombButton.setBackgroundColor(cancelColor)

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




        val menuButton = findViewById<ImageButton>(R.id.menuButton)

        val inflater = LayoutInflater.from(this)
        val rootViewGroup = findViewById<ViewGroup>(R.id.rootContainer)
        val menuOverlay = inflater.inflate(R.layout.menu_overlay, rootViewGroup, false) as FrameLayout


        addContentView(menuOverlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        menuOverlay.visibility = View.GONE

        menuButton.setOnClickListener {
            menuOverlay.visibility = View.VISIBLE
        }

        menuOverlay.setOnClickListener {
            menuOverlay.visibility = View.GONE
        }

        menuOverlay.findViewById<ImageButton>(R.id.menuCloseButton).setOnClickListener {
            menuOverlay.visibility = View.GONE
        }

        val iconColorPicker = menuOverlay.findViewById<ImageButton>(R.id.iconColorPicker)
        val iconTrash = menuOverlay.findViewById<ImageButton>(R.id.iconTrash)
        val iconAbout = menuOverlay.findViewById<ImageButton>(R.id.iconAbout)
        val iconTBA = menuOverlay.findViewById<ImageButton>(R.id.iconTBA)






        iconColorPicker.setOnClickListener {
            val dialog = ColorPickerDialog(this, root)


            dialog.setCanceledOnTouchOutside(false)
            dialog.show()

            menuOverlay.visibility = View.GONE
        }







        iconTrash.setOnClickListener {
            startActivity(Intent(this, TrashActivity::class.java))
            menuOverlay.visibility = View.GONE
        }


        iconAbout.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_about, null)
            val signature = dialogView.findViewById<TextView>(R.id.aboutSignature)

            val userColor = UserColorManager.getAppColor(this)
            signature.setTextColor(userColor)

            signature.setOnClickListener {
                GuessingGameDialog(this, this, noteViewModel::getAllNotesNow).launch()
            }


            AlertDialog.Builder(this)
                .setTitle("About StratoNotes")
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show()

            menuOverlay.visibility = View.GONE
        }


        iconTBA.setOnClickListener {
            Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show()
            menuOverlay.visibility = View.GONE
        }

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (overlayContainer.isVisible) {
                        closeOverlay()
                    } else if (folderAdapter.getSelectedNotes().isNotEmpty()) {
                        folderAdapter.exitSelectionMode()
                        findViewById<LinearLayout>(R.id.selectionBar).visibility = View.GONE
                        findViewById<LinearLayout>(R.id.bottomBar).visibility = View.VISIBLE
                    } else {
                        finish()
                    }
                }
            }
        )



    }



    private fun showOverlay(note: NoteEntity) {
        currentOverlayNote = note
        overlayContainer.removeAllViews()

        val inflater = layoutInflater
        val overlayView = inflater.inflate(R.layout.item_note, overlayContainer, false)
        val noteCard = overlayView.findViewById<MaterialCardView>(R.id.noteCard)
        val backgroundColor = UserColorManager.getNoteColor(this)
        noteCard.setCardBackgroundColor(backgroundColor)

        val metrics = Resources.getSystem().displayMetrics
        val width = (metrics.widthPixels * 0.9).toInt()
        overlayView.layoutParams = FrameLayout.LayoutParams(width, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)

        val noteText = overlayView.findViewById<EditText>(R.id.noteText)
        val starIcon = overlayView.findViewById<ImageView>(R.id.starIcon)

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

        starIcon.setImageResource(if (note.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
        starIcon.setOnClickListener {
            val updatedContent = noteText.text.toString()
            val updatedFavorite = !currentOverlayNote!!.isFavorite

            currentOverlayNote = currentOverlayNote?.copy(
                content = updatedContent,
                isFavorite = updatedFavorite,
                lastEdited = System.currentTimeMillis()
            )

            starIcon.setImageResource(
                if (currentOverlayNote!!.isFavorite) R.drawable.ic_star_filled
                else R.drawable.ic_star_outline
            )

            lifecycleScope.launch(Dispatchers.IO) {
                noteViewModel.update(currentOverlayNote!!)
            }
        }

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(noteText, InputMethodManager.SHOW_IMPLICIT)

        // Inject pill menu
        val container = overlayView.findViewById<LinearLayout>(R.id.noteContainer)
        val pillMenu = layoutInflater.inflate(R.layout.widget_pill_menu, container, false)
        pillMenu.tag = "pillMenu"
        container.addView(pillMenu)
        initPillMenu(pillMenu)
        refreshFolderListColors()

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

            val updatedNote = note.copy(
                content = updatedContent,
                lastEdited = System.currentTimeMillis()
            )

            noteViewModel.update(updatedNote)
            currentOverlayNote = updatedNote
        }

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(overlayContainer.windowToken, 0)

        overlayContainer.removeAllViews()
        overlayContainer.visibility = View.GONE
        currentOverlayNote = null
    }





    private fun applyThemeColor(color: Int) {
        val root = findViewById<View>(R.id.rootContainer)
        root.setBackgroundColor(color)

        if (overlayContainer.isVisible) {
            val overlayView = overlayContainer.getChildAt(0)
            val noteCard = overlayView?.findViewById<MaterialCardView>(R.id.noteCard)
            noteCard?.setCardBackgroundColor(UserColorManager.getNoteColor(this))
        }

        refreshFolderListColors()
    }



    override fun onPause() {
        super.onPause()

        if (overlayContainer.isVisible) {
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

        // Always unregister broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(themeReceiver)
    }


    override fun onResume() {
        super.onResume()

        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val appColor = prefs.getInt("app_color", "#5D53A3".toColorInt())
        val root = findViewById<View>(R.id.rootContainer)
        root.setBackgroundColor(appColor)

        LocalBroadcastManager.getInstance(this).registerReceiver(
            themeReceiver, IntentFilter("com.stratonotes.THEME_COLOR_CHANGED")
        )
    }


    private fun reloadFiltered(query: String) {
        noteViewModel.getFoldersWithPreviews().observe(this) { folders ->
            folderAdapter.updateFilteredList(filterFolders(folders, query))
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshFolderListColors() {
        folderAdapter.notifyDataSetChanged()
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
    @Suppress("RemoveExplicitTypeArguments")
    private fun initPillMenu(rootView: View) {
        val pill = rootView.findViewById<LinearLayout>(R.id.pillContainer)
        val plus = rootView.findViewById<ImageButton>(R.id.iconPlus)

        val iconAddImage = rootView.findViewById<ImageButton>(R.id.iconAddImage)
        iconAddImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            startActivityForResult(intent, 101)
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

            val animator = ValueAnimator.ofInt(pill.width, targetWidth)
            animator.duration = 250
            animator.addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                pill.layoutParams.width = value
                pill.requestLayout()
            }

            animator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (!expanded) pill.visibility = View.GONE
                }
            })

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
            val uri = data.data ?: return
            val targetCard = if (overlayContainer.isVisible)
                overlayContainer.getChildAt(0)?.findViewById<LinearLayout>(R.id.noteContainer)
            else
                null

            val scrollView = targetCard?.findViewById<ScrollView>(R.id.noteScroll)
            val editText = scrollView?.findViewById<EditText>(R.id.noteText)
            if (editText != null) {
                insertImage(uri, editText)
            }
        }
    }

    private fun insertImage(uri: Uri, target: EditText) {
        val context = this
        val imageView = ImageView(context).apply {
            setImageURI(uri)
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 16)
            }
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER

            setOnClickListener {
                Toast.makeText(context, "Image tapped (drag/resize placeholder)", Toast.LENGTH_SHORT).show()
            }
        }

        val parent = target.parent as ViewGroup
        val index = parent.indexOfChild(target)
        parent.addView(imageView, index)
    }


}
