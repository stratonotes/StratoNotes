package com.stratonotes

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
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

    private var actionMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        trashRecycler = findViewById(R.id.trashRecycler)
        emptyTrashButton = findViewById(R.id.emptyTrashButton)
        backButton = findViewById(R.id.backButton)

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
    }

    override fun onRestore(note: NoteEntity) {
        noteViewModel.restore(note)
        Toast.makeText(this, "Note restored", Toast.LENGTH_SHORT).show()
    }

    override fun onDelete(note: NoteEntity) {
        noteViewModel.permanentlyDelete(note)
        Toast.makeText(this, "Note permanently deleted", Toast.LENGTH_SHORT).show()
    }

    override fun onStartSelection() {
        if (actionMode == null) {
            actionMode = startActionMode(actionModeCallback)
        }
        updateActionTitle()
    }

    override fun onSelectionChanged() {
        if (trashAdapter.selectedNotes.isEmpty()) {
            actionMode?.finish()
        } else {
            updateActionTitle()
        }
    }

    private fun updateActionTitle() {
        actionMode?.title = "${trashAdapter.selectedNotes.size} selected"
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.menu_overlay_action, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            val selected = trashAdapter.selectedNotes.toList()

            when (item?.itemId) {
                R.id.action_restore -> {
                    for (note in selected) {
                        noteViewModel.restore(note)
                    }
                    Toast.makeText(this@TrashActivity, "Restored ${selected.size} note(s)", Toast.LENGTH_SHORT).show()
                    mode?.finish()
                    return true
                }

                R.id.action_delete -> {
                    AlertDialog.Builder(this@TrashActivity)
                        .setTitle("Delete Permanently")
                        .setMessage("This will permanently delete ${selected.size} note(s). Proceed?")
                        .setPositiveButton("Yes") { _, _ ->
                            for (note in selected) {
                                noteViewModel.permanentlyDelete(note)
                            }
                            Toast.makeText(this@TrashActivity, "Deleted", Toast.LENGTH_SHORT).show()
                            mode?.finish()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    return true
                }
            }

            return false
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            trashAdapter.exitSelectionMode()
            actionMode = null
        }
    }
}