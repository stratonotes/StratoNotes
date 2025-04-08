package com.example.punchpad2;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class TrashActivity extends AppCompatActivity {

    private NoteViewModel noteViewModel;
    private TrashAdapter trashAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        RecyclerView trashRecycler = findViewById(R.id.trashRecycler);
        trashRecycler.setLayoutManager(new LinearLayoutManager(this));

        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        noteViewModel.getTrashedNotes().observe(this, notes -> {
            trashAdapter = new TrashAdapter(notes, new TrashAdapter.TrashActionListener() {
                @Override
                public void onRestore(NoteEntity note) {
                    noteViewModel.restore(note);
                }

                @Override
                public void onDelete(NoteEntity note) {
                    noteViewModel.permanentlyDelete(note);
                }
            });

            trashRecycler.setAdapter(trashAdapter);
        });

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }
}
