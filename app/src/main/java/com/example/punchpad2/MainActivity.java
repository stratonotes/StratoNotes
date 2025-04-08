package com.example.punchpad2;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;
import com.example.punchpad2.NoteDatabase;
import com.example.punchpad2.NoteEntity;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private LinearLayout previewContainer;
    private EditText searchInput;
    private ListView liveSearchResults;
    private ImageButton filterButton;

    private List<NoteEntity> allNotes = new ArrayList<>();
    private ArrayAdapter<String> liveSearchAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewContainer = findViewById(R.id.previewContainer);
        Button goToLibrary = findViewById(R.id.goToLibrary);
        Button createNote = findViewById(R.id.generateTestNote);

        searchInput = findViewById(R.id.searchInput);
        liveSearchResults = findViewById(R.id.liveSearchResults);
        filterButton = findViewById(R.id.filterButton);

        liveSearchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        liveSearchResults.setAdapter(liveSearchAdapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                filterLiveResults(s.toString());
            }
        });

        liveSearchResults.setOnItemClickListener((parent, view, position, id) -> {
            String selected = liveSearchAdapter.getItem(position);
            for (NoteEntity note : allNotes) {
                if (note.content.startsWith(selected)) {
                    Intent intent = new Intent(MainActivity.this, NoteActivity.class);
                    intent.putExtra("content", note.content);
                    startActivity(intent);
                    break;
                }
            }
        });

        filterButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LibraryActivity.class);
            intent.putExtra("query", searchInput.getText().toString());
            startActivity(intent);
        });

        goToLibrary.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LibraryActivity.class);
            startActivity(intent);
        });

        createNote.setOnClickListener(v -> insertTestNote());

        loadPreviews();
    }

    private void loadPreviews() {
        AsyncTask.execute(() -> {
            allNotes = NoteDatabase.getInstance(this).noteDao().getAllNotesNow();

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

    private void filterLiveResults(String query) {
        if (query.isEmpty()) {
            liveSearchResults.setVisibility(ListView.GONE);
            return;
        }

        List<String> matches = new ArrayList<>();
        for (NoteEntity note : allNotes) {
            if (note.content.toLowerCase().contains(query.toLowerCase())) {
                matches.add(note.content.length() > 50 ? note.content.substring(0, 50) + "..." : note.content);
            }
        }

        if (matches.isEmpty()) {
            liveSearchResults.setVisibility(ListView.GONE);
        } else {
            liveSearchAdapter.clear();
            liveSearchAdapter.addAll(matches);
            liveSearchAdapter.notifyDataSetChanged();
            liveSearchResults.setVisibility(ListView.VISIBLE);
        }
    }
}
