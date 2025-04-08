package com.example.punchpad2;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class NoteViewModel extends AndroidViewModel {

    private final NoteRepository repository;
    private final LiveData<List<FolderWithNotes>> allFolders;

    public NoteViewModel(Application application) {
        super(application);
        repository = new NoteRepository(application);
        allFolders = repository.getAllFoldersWithNotes();
    }

    public LiveData<List<FolderWithNotes>> getFoldersWithPreviews() {
        return allFolders;
    }

    public void insert(NoteEntity note) {
        repository.insert(note);
    }

    public void delete(NoteEntity note) {
        repository.delete(note);
    }

    public void update(NoteEntity note) {
        repository.update(note);
    }

    public void toggleFavorite(NoteEntity note, boolean favorite) {
        note.favorited = favorite;
        repository.update(note);
    }

    public LiveData<List<NoteEntity>> getTrashedNotes() {
        return repository.getTrashedNotes();
    }

    public void restore(NoteEntity note) {
        note.isTrashed = false;
        repository.update(note);
    }

    public void permanentlyDelete(NoteEntity note) {
        repository.delete(note);
    }
}
