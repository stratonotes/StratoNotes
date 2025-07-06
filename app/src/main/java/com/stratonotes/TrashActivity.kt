package com.stratonotes

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.punchpad2.R
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TrashActivity : AppCompatActivity(), TrashAdapter.TrashActionListener {

    private val noteViewModel: NoteViewModel by viewModels()
    private lateinit var trashAdapter: TrashAdapter
    private lateinit var trashRecycler: RecyclerView
    private lateinit var emptyTrashButton: Button
    private lateinit var backButton: ImageButton

    private lateinit var selectionBar: ViewGroup
    private lateinit var selectionCount: TextView
    private lateinit var deleteSelectedBtn: ImageButton
    private lateinit var restoreSelectedBtn: ImageButton
    private lateinit var exitSelectionBtn: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        trashRecycler = findViewById(R.id.trashRecycler)
        emptyTrashButton = findViewById(R.id.emptyTrashButton)
        backButton = findViewById(R.id.backButton)

        selectionBar = findViewById(R.id.selectionBar)
        selectionCount = findViewById(R.id.selectionCount)
        deleteSelectedBtn = findViewById(R.id.deleteSelectedBtn)
        restoreSelectedBtn = findViewById(R.id.restoreSelectedBtn)
        exitSelectionBtn = findViewById(R.id.exitSelectionBtn)

        trashRecycler.layoutManager = LinearLayoutManager(this)
        trashAdapter = TrashAdapter(this)
        trashRecycler.adapter = trashAdapter

        emptyTrashButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Empty Trash")
                .setMessage("This will permanently delete all notes in the Trash. Are you sure?")
                .setPositiveButton("Yes") { _, _ ->
                    lifecycleScope.launch {
                        noteViewModel.emptyTrash()
                        Toast.makeText(this@TrashActivity, "Trash emptied", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        backButton.setOnClickListener { finish() }

        noteViewModel.getTrashedContent().observe(this) { (trashedFolders, trashedNotes) ->
            val now = System.currentTimeMillis()
            val expiredNotes = trashedNotes.filter {
                TimeUnit.MILLISECONDS.toDays(now - it.lastEdited) >= 30
            }
            val validNotes = trashedNotes.filterNot { expiredNotes.contains(it) }

            for (note in expiredNotes) {
                lifecycleScope.launch { noteViewModel.permanentlyDelete(note) }
            }

            trashAdapter.setData(trashedFolders, validNotes)
        }

        restoreSelectedBtn.setOnClickListener {
            val selected = trashAdapter.selectedNotes.toList()
            selected.forEach { noteViewModel.restore(it) }
            trashAdapter.exitSelectionMode()
            hideSelectionBar()
            Toast.makeText(this, "Restored", Toast.LENGTH_SHORT).show()
        }

        deleteSelectedBtn.setOnClickListener {
            val selected = trashAdapter.selectedNotes.toList()
            AlertDialog.Builder(this)
                .setTitle("Delete Permanently")
                .setMessage("Permanently delete ${selected.size} note(s)?")
                .setPositiveButton("Delete") { _, _ ->
                    selected.forEach { noteViewModel.permanentlyDelete(it) }
                    trashAdapter.exitSelectionMode()
                    hideSelectionBar()
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        exitSelectionBtn.setOnClickListener {
            trashAdapter.exitSelectionMode()
            hideSelectionBar()
        }
    }

    override fun onRestore(note: NoteEntity) {
        noteViewModel.restore(note)
        trashAdapter.removeNote(note)
        Toast.makeText(this, "Note restored", Toast.LENGTH_SHORT).show()
    }

    override fun onDelete(note: NoteEntity) {
        noteViewModel.permanentlyDelete(note)
        trashAdapter.removeNote(note)
        Toast.makeText(this, "Note permanently deleted", Toast.LENGTH_SHORT).show()
    }

    override fun onStartSelection() {
        selectionBar.visibility = View.VISIBLE
        updateSelectionCount()
    }

    override fun onSelectionChanged() {
        updateSelectionCount()
        trashAdapter.notifyDataSetChanged() // ✅ Fix: force redraw to update folder checkboxes
    }

    private fun updateSelectionCount() {
        selectionCount.text = "${trashAdapter.selectedNotes.size} selected"
    }

    private fun hideSelectionBar() {
        selectionBar.visibility = View.GONE
    }

    override fun onBackPressed() {
        if (selectionBar.visibility == View.VISIBLE) {
            trashAdapter.exitSelectionMode()
            hideSelectionBar()
            return
        }

        val overlayContainer = findViewById<ViewGroup>(R.id.overlayContainer)
        if (overlayContainer != null && overlayContainer.childCount > 0) {
            overlayContainer.removeAllViews()
        } else {
            super.onBackPressed()
        }
    }
}
