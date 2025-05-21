package com.example.punchpad2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import android.animation.ObjectAnimator;

import com.stratonotes.FolderEntity;
import com.stratonotes.NoteDaoBridge;
import com.stratonotes.NoteEntity;
import com.stratonotes.AppDatabase;


import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private LinearLayout previewContainer;
    private EditText searchInput;
    private EditText noteInput;
    private ListView liveSearchResults;
    private ImageButton filterButton;
    private Button submitButton;
    private Button clearDraftButton;
    private ImageButton plusButton;
    private LinearLayout mediaMenu;
    private ImageButton undoButton, redoButton;
    private ImageButton folderSettingsButton;

    private UndoManager undoManager = new UndoManager();
    private List<NoteEntity> allNotes = new ArrayList<>();
    private ArrayAdapter<String> liveSearchAdapter;

    private enum SaveMode { NEW, RECENT, PRESET }
    private SaveMode currentMode = SaveMode.NEW;
    private boolean isTyping = false;

    private String lastUsedFolder = "Default";
    private String presetFolder = "StratoNote";

    private static final String PREFS_NAME = "SubmitPrefs";
    private static final String KEY_MODE = "lastMode";
    private static final String KEY_FOLDER = "lastFolderName";
    private static final String DRAFT_PREFS = "DraftPrefs";
    private static final String KEY_DRAFT_NOTE = "draft_note";

    private final Handler draftHandler = new Handler();
    private Runnable draftRunnable;
    private Runnable clearFadeRunnable;
    private boolean isClearFading = false;

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
        folderSettingsButton = findViewById(R.id.folder_settings_button_1);
        clearDraftButton = findViewById(R.id.clear_draft_button);
        searchInput = findViewById(R.id.searchInput);
        noteInput = findViewById(R.id.note_input);
        liveSearchResults = findViewById(R.id.liveSearchResults);
        filterButton = findViewById(R.id.filter_button);
        plusButton = findViewById(R.id.plus_button);
        mediaMenu = findViewById(R.id.media_menu);
        undoButton = findViewById(R.id.undo_button);
        redoButton = findViewById(R.id.redo_button);

        noteInput.setMovementMethod(new ScrollingMovementMethod());
        noteInput.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });

        folderSettingsButton.setVisibility(View.GONE);
        folderSettingsButton.setOnClickListener(v -> {
            EditText input = new EditText(this);
            input.setHint("New folder name");
            input.setText(lastUsedFolder);

            new AlertDialog.Builder(this)
                    .setTitle("Rename Folder")
                    .setView(input)
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        String name = input.getText().toString().trim();
                        if (!name.isEmpty()) {
                            lastUsedFolder = name;
                            saveSubmitModeToPrefs(currentMode.name(), lastUsedFolder);
                            updateSubmitLabel();
                            Toast.makeText(this, "Folder renamed to " + name, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Folder name can't be empty", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        SharedPreferences prefs = getSharedPreferences(DRAFT_PREFS, MODE_PRIVATE);
        String draft = prefs.getString(KEY_DRAFT_NOTE, null);
        if (draft != null && !draft.isEmpty()) {
            noteInput.setText(draft);
            clearDraftButton.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Unsaved note restored", Toast.LENGTH_SHORT).show();
        }

        clearDraftButton.setOnClickListener(v -> {
            noteInput.setText("");
            SharedPreferences.Editor editor = getSharedPreferences(DRAFT_PREFS, MODE_PRIVATE).edit();
            editor.remove(KEY_DRAFT_NOTE);
            editor.apply();
            clearDraftButton.setVisibility(View.GONE);
        });

        undoManager.attach(noteInput);
        undoButton.setOnClickListener(v -> {
            if (undoManager.canUndo()) {
                undoManager.undo();
                if (isClearFading) {
                    draftHandler.removeCallbacks(clearFadeRunnable);
                    clearDraftButton.setAlpha(1f);
                    clearDraftButton.setVisibility(View.VISIBLE);
                    isClearFading = false;
                }
            }
        });
        redoButton.setOnClickListener(v -> undoManager.redo());

        loadSubmitModeFromPrefs();
        updateSubmitLabel();

        submitButton.setOnClickListener(v -> {
            String content = noteInput.getText().toString().trim();

            if (content.isEmpty()) {
                cycleMode();
                saveSubmitModeToPrefs(currentMode.name(), currentMode == SaveMode.PRESET ? presetFolder : lastUsedFolder);
                updateSubmitLabel();
                return;
            }

            switch (currentMode) {
                case NEW: showNewFolderDialog(content); break;
                case RECENT: showConfirmDialog(content, lastUsedFolder); break;
                case PRESET: saveNote(content, presetFolder); break;
            }
        });

        submitButton.setOnLongClickListener(v -> {
            if (!noteInput.getText().toString().trim().isEmpty()) {
                showModeSwitchDialog();
                return true;
            }
            return false;
        });

        noteInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) isTyping = true;
            return false;
        });

        noteInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                isTyping = true;
                if (draftRunnable != null) draftHandler.removeCallbacks(draftRunnable);
                draftRunnable = () -> {
                    SharedPreferences.Editor editor = getSharedPreferences(DRAFT_PREFS, MODE_PRIVATE).edit();
                    editor.putString(KEY_DRAFT_NOTE, noteInput.getText().toString());
                    editor.apply();
                };
                draftHandler.postDelayed(draftRunnable, 500);

                if (s.length() == 0 && clearDraftButton.getVisibility() == View.VISIBLE && !isClearFading) {
                    isClearFading = true;
                    ObjectAnimator fadeOut = ObjectAnimator.ofFloat(clearDraftButton, "alpha", 1f, 0f);
                    fadeOut.setDuration(3000);
                    fadeOut.start();
                    clearFadeRunnable = () -> {
                        clearDraftButton.setVisibility(View.GONE);
                        clearDraftButton.setAlpha(1f);
                        isClearFading = false;
                    };
                    draftHandler.postDelayed(clearFadeRunnable, 3000);
                }

                if (s.length() > 0 && isClearFading) {
                    draftHandler.removeCallbacks(clearFadeRunnable);
                    clearDraftButton.setAlpha(1f);
                    isClearFading = false;
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

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
                if (note.getContent().startsWith(selected)) {
                    Intent intent = new Intent(MainActivity.this, NoteActivity.class);
                    intent.putExtra("content", note.getContent());
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
                    mediaMenu.setVisibility(View.GONE);
                });
            } else {
                mediaMenu.setTranslationX(mediaMenu.getWidth());
                mediaMenu.setAlpha(0f);
                mediaMenu.setVisibility(View.VISIBLE);
                mediaMenu.animate().translationX(0f).alpha(1f).setDuration(200).start();
            }
        });

        loadPreviews();
    }

    private void saveNote(String content, String folderName) {
        AsyncTask.execute(() -> {
            FolderEntity folder = AppDatabase.getInstance(this).noteDao().getFolderByName(folderName);
            if (folder == null) {
                long now = System.currentTimeMillis();
                folder = new FolderEntity(0L, folderName, now, now);
                NoteDaoBridge.insertFolderAsync(AppDatabase.getInstance(this).noteDao(), folder);

            }

            long now = System.currentTimeMillis();

            NoteEntity note = new NoteEntity(
                    0L, folder.getId(), content, now, now,
                    false, false, false, false
            );

            NoteDaoBridge.insertNoteAsync(AppDatabase.getInstance(this).noteDao(), note);


            runOnUiThread(() -> {
                noteInput.setText("");
                isTyping = false;
                updateSubmitLabel();
                loadPreviews();
                SharedPreferences.Editor editor = getSharedPreferences(DRAFT_PREFS, MODE_PRIVATE).edit();
                editor.remove(KEY_DRAFT_NOTE);
                editor.apply();
                clearDraftButton.setVisibility(View.GONE);
                Toast.makeText(this, "Saved to " + folderName, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void loadPreviews() {
        AsyncTask.execute(() -> {

            List<NoteEntity> notes = AppDatabase.getInstance(this).noteDao().get3MostRecentVisibleNotes();
            runOnUiThread(() -> {
                previewContainer.removeAllViews();
                for (NoteEntity note : notes) {
                    String content = note.getContent();
                    TextView preview = new TextView(this);
                    preview.setText(content.length() > 100 ? content.substring(0, 100) + "..." : content);
                    preview.setPadding(0, 16, 0, 16);
                    preview.setTextColor(0xFFFFFFFF);
                    preview.setOnClickListener(v -> {
                        Intent intent = new Intent(MainActivity.this, NoteActivity.class);
                        intent.putExtra("content", content);
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
            String content = note.getContent();
            if (content.toLowerCase().contains(query.toLowerCase())) {
                matches.add(content.length() > 50 ? content.substring(0, 50) + "..." : content);
            }
        }

        if (matches.isEmpty()) {
            liveSearchResults.setVisibility(ListView.GONE);
        } else {
            liveSearchAdapter.clear();
            liveSearchAdapter.addAll(matches);
            liveSearchAdapter.notifyDataSetChanged();
            liveSearchResults.setVisibility(View.VISIBLE);
        }
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
                folderSettingsButton.setVisibility(View.GONE);
                break;
            case RECENT:
                submitButton.setText("Add note to " + lastUsedFolder);
                folderSettingsButton.setVisibility(View.VISIBLE);
                break;
            case PRESET:
                submitButton.setText("Enter text → Add to " + presetFolder);
                folderSettingsButton.setVisibility(View.GONE);
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
                .setPositiveButton("Yes", (dialog, which) -> saveNote(content, folderName))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showModeSwitchDialog() {
        String[] modes = {"New Folder", "StratoNote", "Preset Folder"};
        int checkedItem;
        switch (currentMode) {
            case NEW:
                checkedItem = 0;
                break;
            case RECENT:
                checkedItem = 1;
                break;
            case PRESET:
                checkedItem = 2;
                break;
            default:
                checkedItem = 0;
        }


        new AlertDialog.Builder(this)
                .setTitle("Switch Save Mode")
                .setSingleChoiceItems(modes, checkedItem, null)
                .setPositiveButton("Select", (dialog, which) -> {
                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    switch (selectedPosition) {
                        case 0:
                            currentMode = SaveMode.NEW;
                            break;
                        case 1:
                            currentMode = SaveMode.RECENT;
                            break;
                        case 2:
                            currentMode = SaveMode.PRESET;
                            break;
                    }

                    updateSubmitLabel();
                    Toast.makeText(this, "Save mode switched to " + modes[selectedPosition], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
