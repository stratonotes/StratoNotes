package com.example.punchpad2;

import java.util.ArrayList;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
            folderAdapter.updateFilteredList(folders);
        });

        ImageButton deleteButton = findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> {
            deleteMode = !deleteMode;
            // TODO: toggle delete mode visuals
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

        reloadFiltered(query);
    }

    private void reloadFiltered(String query) {
        noteViewModel.getFoldersWithPreviews().observe(this, folders -> {
            folderAdapter.updateFilteredList(folders);
        });
    }
}
