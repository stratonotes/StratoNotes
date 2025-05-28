package com.stratonotes

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.punchpad2.R
import java.util.concurrent.TimeUnit
import android.widget.ImageButton


class TrashActivity : AppCompatActivity() {

    private lateinit var noteViewModel: NoteViewModel
    private lateinit var trashAdapter: TrashAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        val trashRecycler = findViewById<RecyclerView>(R.id.trashRecycler)
        trashRecycler.layoutManager = LinearLayoutManager(this)

        noteViewModel = ViewModelProvider(this).get(NoteViewModel::class.java)

        trashAdapter = TrashAdapter(object : TrashAdapter.TrashActionListener {
            override fun onRestore(note: NoteEntity) {
                noteViewModel.restore(note)
                Toast.makeText(this@TrashActivity, "Note restored", Toast.LENGTH_SHORT).show()
            }

            override fun onDelete(note: NoteEntity) {
                noteViewModel.permanentlyDelete(note)
                Toast.makeText(this@TrashActivity, "Note permanently deleted", Toast.LENGTH_SHORT).show()
            }
        })
        trashRecycler.adapter = trashAdapter

        findViewById<Button>(R.id.emptyTrashButton).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Empty Trash")
                .setMessage("This will permanently delete all notes in the Trash. Are you sure?")
                .setPositiveButton("Yes") { _, _ ->
                    noteViewModel.emptyTrash()
                    Toast.makeText(this, "Trash emptied", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Auto-delete logic: run every time the screen opens
        noteViewModel.trashedNotes.observe(this) { notes ->
            val now = System.currentTimeMillis()
            val safeNotes = notes?.filterNotNull()?.filter { note ->
                val age = now - note.lastEdited
                val days = TimeUnit.MILLISECONDS.toDays(age)
                if (days >= 30) {
                    noteViewModel.permanentlyDelete(note)
                    false
                } else true
            } ?: emptyList()

            trashAdapter.submitList(safeNotes)
        }

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
}
