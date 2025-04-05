package com.example.punchpad2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.AsyncTask;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.punchpad2.data.NoteDatabase;
import com.example.punchpad2.data.NoteEntity;

import java.util.List;

public class MainActivity extends Activity {

    private LinearLayout previewContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewContainer = findViewById(R.id.previewContainer);
        Button goToLibrary = findViewById(R.id.goToLibrary);
        Button createNote = findViewById(R.id.generateTestNote);

        goToLibrary.setOnClickListener(v -> {
            // Library screen not implemented yet
        });

        createNote.setOnClickListener(v -> insertTestNote());

        loadPreviews();
    }

    private void loadPreviews() {
        AsyncTask.execute(() -> {
            List<NoteEntity> notes = NoteDatabase.getInstance(this)
                    .noteDao()
                    .get3MostRecentVisibleNotes();

            runOnUiThread(() -> {
                previewContainer.removeAllViews();
                for (NoteEntity note : notes) {
                    TextView preview = new TextView(this);
                    preview.setText(note.content.length() > 100 ? note.content.substring(0, 100) + "..." : note.content);
                    preview.setPadding(0, 16, 0, 16);
                    preview.setTextColor(0xFFFFFFFF);
                    preview.setOnClickListener(v -> {
                        Intent intent = new Intent(MainActivity.this, NoteActivity.class);
                        intent.putExtra("content", note.content);
                        startActivity(intent);
                    });
                    previewContainer.addView(preview);
                }
            });
        });
    }

    private void insertTestNote() {
        AsyncTask.execute(() -> {
            NoteEntity note = new NoteEntity();
            note.content = "This is a test note generated at " + System.currentTimeMillis();
            note.createdAt = System.currentTimeMillis();
            note.lastEdited = note.createdAt;
            note.isHidden = false;
            note.isLarge = false;
            NoteDatabase.getInstance(this).noteDao().insert(note);
            runOnUiThread(this::loadPreviews);
        });
    }
}