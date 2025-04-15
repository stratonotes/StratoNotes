package com.example.punchpad2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

import com.example.punchpad2.NoteDatabase;
import com.example.punchpad2.NoteEntity;
import com.example.punchpad2.FolderEntity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private LinearLayout previewContainer;
    private EditText searchInput;
    private EditText noteInput;
    private ListView liveSearchResults;
    private ImageButton filterButton;
    private Button submitButton;
    private ImageButton plusButton;
    private LinearLayout mediaMenu;
    private ImageButton undoButton, redoButton;

    private UndoManager undoManager = new UndoManager();

    private List<NoteEntity> allNotes = new ArrayList<>();
    private ArrayAdapter<String> liveSearchAdapter;

    private enum SaveMode { NEW, RECENT, PRESET }
    private SaveMode currentMode = SaveMode.NEW;
    private boolean isTyping = false;

    private String lastUsedFolder = "Default";
    private String presetFolder = "QuickNotes";

    private static final String PREFS_NAME = "SubmitPrefs";
    private static final String KEY_MODE = "lastMode";
    private static final String KEY_FOLDER = "lastFolderName";

    private static final String MODE_NEW = "NEW";
    private static final String MODE_RECENT = "RECENT";
    private static final String MODE_PRESET = "PRESET";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewContainer = findViewById(R.id.previewContainer);
        Button goToLibrary = findViewById(R.id.goToLibrary);
        submitButton = findViewById(R.id.submit_button);
        searchInput = findViewById(R.id.searchInput);
        noteInput = findViewById(R.id.note_input);
        liveSearchResults = findViewById(R.id.liveSearchResults);
        filterButton = findViewById(R.id.filterButton);
        plusButton = findViewById(R.id.plus_button);
        mediaMenu = findViewById(R.id.media_menu);
        undoButton = findViewById(R.id.undo_button);
        redoButton = findViewById(R.id.redo_button);

        undoManager.attach(noteInput);
        undoButton.setOnClickListener(v -> undoManager.undo());
        redoButton.setOnClickListener(v -> undoManager.redo());

        loadSubmitModeFromPrefs();
        updateSubmitLabel();

        submitButton.setOnClickListener(v -> {
            if (!isTyping) {
                cycleMode();
                saveSubmitModeToPrefs(currentMode.name(), currentMode == SaveMode.PRESET ? presetFolder : lastUsedFolder);
                updateSubmitLabel();
                return;
            }

            String content = noteInput.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "Can't save empty note", Toast.LENGTH_SHORT).show();
                return;
            }

            switch (currentMode) {
                case NEW:
                    showNewFolderDialog(content);
                    break;
                case RECENT:
                    showConfirmDialog(content, lastUsedFolder);
                    break;
                case PRESET:
                    saveNote(content, presetFolder);
                    break;
            }
        });

        noteInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) isTyping = true;
            return false;
        });

        noteInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                isTyping = true;
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        SharedPreferences prefs = getSharedPreferences("DraftPrefs", MODE_PRIVATE);
        String draft = prefs.getString("draft_note", null);
        if (draft != null && !draft.isEmpty()) {
            noteInput.setText(draft);
            Toast.makeText(this, "Unsaved note restored", Toast.LENGTH_SHORT).show();
        }

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

        plusButton.setOnClickListener(v -> {
            if (mediaMenu.getVisibility() == LinearLayout.VISIBLE) {
                mediaMenu.animate().translationX(mediaMenu.getWidth()).alpha(0f).setDuration(200).withEndAction(() -> {
                    mediaMenu.setVisibility(LinearLayout.GONE);
                });
            } else {
                mediaMenu.setTranslationX(mediaMenu.getWidth());
                mediaMenu.setAlpha(0f);
                mediaMenu.setVisibility(LinearLayout.VISIBLE);
                mediaMenu.animate().translationX(0f).alpha(1f).setDuration(200).start();
            }
        });

        loadPreviews();
    }

    private void saveSubmitModeToPrefs(String mode, String folderName) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_MODE, mode);
        editor.putString(KEY_FOLDER, folderName);
        editor.apply();
    }

    private void loadSubmitModeFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedMode = prefs.getString(KEY_MODE, MODE_RECENT);
        String savedFolder = prefs.getString(KEY_FOLDER, "");

        switch (savedMode) {
            case MODE_NEW: currentMode = SaveMode.NEW; break;
            case MODE_PRESET: currentMode = SaveMode.PRESET; break;
            default: currentMode = SaveMode.RECENT; break;
        }

        if (!savedFolder.isEmpty()) lastUsedFolder = savedFolder;
    }

    private void cycleMode() {
        switch (currentMode) {
            case NEW: currentMode = SaveMode.RECENT; break;
            case RECENT: currentMode = SaveMode.PRESET; break;
            case PRESET: currentMode = SaveMode.NEW; break;
        }
    }

    private void updateSubmitLabel() {
        switch (currentMode) {
            case NEW:
                submitButton.setText("Enter text → Add to New Folder");
                break;
            case RECENT:
                submitButton.setText("Add note to " + lastUsedFolder);
                break;
            case PRESET:
                submitButton.setText("Enter text → Add to " + presetFolder);
                break;
        }
    }

    private void showNewFolderDialog(String content) {
        EditText input = new EditText(this);
        input.setHint("Folder name");

        new AlertDialog.Builder(this)
                .setTitle("Create Folder")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String folderName = input.getText().toString().trim();
                    if (!folderName.isEmpty()) {
                        saveNote(content, folderName);
                        lastUsedFolder = folderName;
                    } else {
                        Toast.makeText(this, "Folder name can't be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showConfirmDialog(String content, String folderName) {
        new AlertDialog.Builder(this)
                .setTitle("Save to " + folderName + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    saveNote(content, folderName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveNote(String content, String folderName) {
        AsyncTask.execute(() -> {
            FolderEntity folder = NoteDatabase.getInstance(this).noteDao().getOrCreateFolderByName(folderName);

            NoteEntity note = new NoteEntity();
            note.content = content;
            note.createdAt = System.currentTimeMillis();
            note.lastEdited = note.createdAt;
            note.isHidden = false;
            note.isLarge = false;
            note.folderId = folder.id;

            NoteDatabase.getInstance(this).noteDao().insert(note);

            runOnUiThread(() -> {
                noteInput.setText("");
                isTyping = false;
                updateSubmitLabel();
                loadPreviews();
                Toast.makeText(this, "Saved to " + folderName, Toast.LENGTH_SHORT).show();
            });
        });
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
