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
import com.stratonotes.FolderWithNotes;


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
            android.util.Log.d("LibraryActivity", "Room gave us " + folders.size() + " folders");

            for (FolderWithNotes fw : folders) {
                String folderName = fw.getFolder() != null ? fw.getFolder().getName() : "(null)";
                int noteCount = fw.getNotes() != null ? fw.getNotes().size() : -1;
                android.util.Log.d("LibraryActivity", "Folder: " + folderName + " → " + noteCount + " notes");
            }

            folderAdapter.updateFilteredList(filterFolders(folders, query));
        });


        ImageButton deleteButton = findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> {
            deleteMode = !deleteMode;
            folderAdapter.setDeleteMode(deleteMode);
            folderAdapter.notifyDataSetChanged();
        });

        ImageButton favoritesToggle = findViewById(R.id.favoritesToggle);
        favoritesToggle.setOnClickListener(v -> {
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

            for (NoteEntity note : folder.getNotes()) {
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

                FolderWithNotes copy = new FolderWithNotes(folder.getFolder(), filteredNotes);
                copy = new FolderWithNotes(folder.getFolder(), filteredNotes);

                result.add(copy);
            }
        }

        return result;
    }
}
