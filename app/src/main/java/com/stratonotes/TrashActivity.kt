package com.stratonotes

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.punchpad2.R

class TrashActivity : AppCompatActivity() {

    private lateinit var noteViewModel: NoteViewModel
    private lateinit var trashAdapter: TrashAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        val trashRecycler = findViewById<RecyclerView>(R.id.trashRecycler)
        trashRecycler.layoutManager = LinearLayoutManager(this)

        noteViewModel = ViewModelProvider(this).get(NoteViewModel::class.java)

        noteViewModel.getTrashedNotes().observe(this) { notes ->
            val safeNotes = notes?.filterNotNull() ?: emptyList()

            trashAdapter = TrashAdapter(
                safeNotes,
                object : TrashAdapter.TrashActionListener {
                    override fun onRestore(note: NoteEntity) {
                        noteViewModel.restore(note)
                    }
                    override fun onDelete(note: NoteEntity) {
                        noteViewModel.permanentlyDelete(note)
                    }
                }
            )
            trashRecycler.adapter = trashAdapter
        }

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener { finish() }
    }
}
