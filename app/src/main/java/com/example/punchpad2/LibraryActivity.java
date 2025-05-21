package com.example.punchpad2;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import com.stratonotes.NoteEntity;
import com.example.punchpad2.FolderWithNotes;


public class LibraryActivity extends AppCompatActivity {

    private NoteViewModel noteViewModel;
    private FolderAdapter folderAdapter;

    private boolean deleteMode = false;
    private boolean favoritesOnly = false;
    private boolean sortNewest = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        RecyclerView folderRecycler = findViewById(R.id.folderRecycler);
        folderRecycler.setLayoutManager(new LinearLayoutManager(this));

        String query = getIntent().getStringExtra("query");

        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        folderAdapter = new FolderAdapter(this, new ArrayList<>(), note -> {
            noteViewModel.update(note);
        });

        folderRecycler.setAdapter(folderAdapter);

        noteViewModel.getFoldersWithPreviews().observe(this, folders -> {
            folderAdapter.updateFilteredList(filterFolders(folders, query));
        });

        ImageButton deleteButton = findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> {
            deleteMode = !deleteMode;
            folderAdapter.setDeleteMode(deleteMode);
            folderAdapter.notifyDataSetChanged();
        });

        ImageButton toggleFavorites = findViewById(R.id.toggleFavorites);
        toggleFavorites.setOnClickListener(v -> {
            favoritesOnly = !favoritesOnly;
            reloadFiltered(query);
        });

        ImageButton sortToggle = findViewById(R.id.sortToggle);
        sortToggle.setOnClickListener(v -> {
            sortNewest = !sortNewest;
            reloadFiltered(query);
        });
    }

    private void reloadFiltered(String query) {
        noteViewModel.getFoldersWithPreviews().observe(this, folders -> {
            folderAdapter.updateFilteredList(filterFolders(folders, query));
        });
    }

    private List<FolderWithNotes> filterFolders(List<FolderWithNotes> original, String query) {
        List<FolderWithNotes> result = new ArrayList<>();

        for (FolderWithNotes folder : original) {
            List<NoteEntity> filteredNotes = new ArrayList<>();

            for (NoteEntity note : folder.notes) {
                boolean match = true;

                if (query != null && !query.trim().isEmpty() && !note.getContent().toLowerCase().contains(query.toLowerCase())) {
                    match = false;
                }

                if (favoritesOnly && !note.isFavorite()) {
                    match = false;
                }

                if (match) {
                    filteredNotes.add(note);
                }
            }

            if (!filteredNotes.isEmpty()) {
                if (!sortNewest) {
                    filteredNotes.sort((a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
                } else {
                    filteredNotes.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                }

                FolderWithNotes copy = new FolderWithNotes();
                copy.folder = folder.folder;
                copy.notes = filteredNotes;
                result.add(copy);
            }
        }

        return result;
    }
}
