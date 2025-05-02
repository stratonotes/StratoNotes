package com.example.punchpad2;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Library screen:
 * • Single-tap note → expand / collapse preview (up to 3 at once)
 * • Second tap on same note → full-screen editor overlay (shares widget with MainActivity)
 * • Six-button bottom bar mirrors XML order: ⭐, ⇅, view-toggle,  ←, ⋮, 💣
 * • Top search bar filters in real-time; sort & favorites toggles honored
 */
public class LibraryActivity extends AppCompatActivity
        implements FolderAdapter.OnNoteInteractionListener {

    private NoteViewModel      noteViewModel;
    private FolderAdapter      folderAdapter;

    /* UI state toggles */
    private boolean deleteMode      = false;
    private boolean favoritesOnly   = false;
    private boolean sortNewest      = true;
    private boolean foldersOnlyView = false;

    /* expanded-note tracking (max 3) */
    private final Set<NoteEntity> expandedNotes = new HashSet<>();

    /* overlay editor */
    private FrameLayout editorOverlay;
    private EditText    editorField;
    private NoteEntity  noteBeingEdited = null;

    /* search */
    private EditText searchInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        /* ---------- bind views ---------- */
        RecyclerView folderRecycler = findViewById(R.id.folderRecycler);
        folderRecycler.setLayoutManager(new LinearLayoutManager(this));

        searchInput    = findViewById(R.id.searchInput);
        editorOverlay  = findViewById(R.id.editorOverlay);
        editorField    = editorOverlay.findViewById(R.id.noteInput);

        /* overlay buttons (re-use MainActivity’s handlers when hooked up) */
        ImageButton fabUndo = editorOverlay.findViewById(R.id.fabUndo);
        ImageButton fabRedo = editorOverlay.findViewById(R.id.fabRedo);
        ImageButton fabPlus = editorOverlay.findViewById(R.id.fabPlus);
        fabUndo.setOnClickListener(v -> {/* TODO: UndoManager hook */});
        fabRedo.setOnClickListener(v -> {/* TODO: UndoManager hook */});
        fabPlus.setOnClickListener(v -> Toast.makeText(this,"+ media stub",Toast.LENGTH_SHORT).show());
        editorOverlay.setOnClickListener(v -> closeEditor());

        /* ---------- ViewModel / adapter ---------- */
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        folderAdapter = new FolderAdapter(this, new ArrayList<>(), this);
        folderRecycler.setAdapter(folderAdapter);

        // initial query (e.g., from SearchActivity)
        String initialQuery = getIntent().getStringExtra("query");
        subscribeFolders(initialQuery);

        /* ---------- live search filter ---------- */
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            @Override public void onTextChanged(CharSequence s,int a,int b,int c){ reloadFiltered(s.toString()); }
            @Override public void afterTextChanged(Editable s){}
        });

        /* ---------- bottom-bar buttons ---------- */
        ImageButton deleteButton     = findViewById(R.id.deleteButton);
        ImageButton toggleFavorites  = findViewById(R.id.toggleFavorites);
        ImageButton sortToggle       = findViewById(R.id.sortToggle);
        ImageButton viewToggle       = findViewById(R.id.viewToggleButton);
        ImageButton backButton       = findViewById(R.id.backButton);
        ImageButton menuButton       = findViewById(R.id.menuButton);

        deleteButton.setOnClickListener(v -> {
            deleteMode = !deleteMode;
            folderAdapter.setDeleteMode(deleteMode);
        });

        toggleFavorites.setOnClickListener(v -> {
            favoritesOnly = !favoritesOnly;
            reloadFiltered(searchInput.getText().toString());
        });

        sortToggle.setOnClickListener(v -> {
            sortNewest = !sortNewest;
            reloadFiltered(searchInput.getText().toString());
        });

        viewToggle.setOnClickListener(v -> {
            foldersOnlyView = !foldersOnlyView;
            reloadFiltered(searchInput.getText().toString());
        });

        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        menuButton.setOnClickListener(v -> {
            // future library options
            new AlertDialog.Builder(this)
                    .setTitle("Library options")
                    .setMessage("Coming soon.")
                    .setPositiveButton(android.R.string.ok,null)
                    .show();
        });
    }

    /* ---------- adapter callback ---------- */
    @Override
    public void onNoteTapped(NoteEntity note) {
        if (!expandedNotes.contains(note)) {              // first tap → expand
            handleNoteExpansion(note);
            int pos = folderAdapter.getAdapterPositionForNote(note);
            if (pos != RecyclerView.NO_POSITION) folderAdapter.notifyItemChanged(pos);

        } else {                                          // second tap → edit
            openEditor(note);
        }
    }

    /* ---------- overlay editor ---------- */
    private void openEditor(NoteEntity note) {
        noteBeingEdited = note;
        editorField.setText(note.content);
        editorOverlay.setVisibility(View.VISIBLE);
    }

    private void closeEditor() {
        if (noteBeingEdited != null) {
            String newBody = editorField.getText().toString();
            if (!newBody.equals(noteBeingEdited.content)) {
                noteBeingEdited.content = newBody;
                noteViewModel.update(noteBeingEdited);
            }
            noteBeingEdited = null;
        }
        editorOverlay.setVisibility(View.GONE);
    }

    /* ---------- LiveData subscription / filtering ---------- */
    private void subscribeFolders(String baseQuery) {
        noteViewModel.getFoldersWithPreviews()
                .observe(this,
                        folders -> folderAdapter.updateFilteredList(
                                filterFolders(folders, baseQuery), foldersOnlyView));
    }

    private void reloadFiltered(String q) { subscribeFolders(q); }

    /* ---------- filter + sort ---------- */
    private List<FolderWithNotes> filterFolders(List<FolderWithNotes> original, String query) {
        List<FolderWithNotes> result = new ArrayList<>();

        for (FolderWithNotes folder : original) {
            List<NoteEntity> filtered = new ArrayList<>();

            for (NoteEntity note : folder.notes) {
                boolean match = true;

                if (query != null && !query.trim().isEmpty()
                        && !note.content.toLowerCase().contains(query.toLowerCase())) {
                    match = false;
                }
                if (favoritesOnly && !note.favorited) match = false;

                if (match) filtered.add(note);
            }

            if (!filtered.isEmpty()) {
                filtered.sort((a, b) -> sortNewest
                        ? Long.compare(b.createdAt, a.createdAt)
                        : Long.compare(a.createdAt, b.createdAt));

                FolderWithNotes copy = new FolderWithNotes();
                copy.folder = folder.folder;
                copy.notes  = filtered;
                result.add(copy);
            }
        }
        return result;
    }

    /* ---------- expand / collapse helper ---------- */
    private void handleNoteExpansion(NoteEntity note) {
        if (!expandedNotes.contains(note)) {
            if (expandedNotes.size() >= 3) {            // keep max 3 expanded
                NoteEntity oldest = expandedNotes.iterator().next();
                oldest.expanded = false;
                expandedNotes.remove(oldest);
            }
            note.expanded = true;
            expandedNotes.add(note);
        } else {
            note.expanded = false;
            expandedNotes.remove(note);
        }
    }
}
