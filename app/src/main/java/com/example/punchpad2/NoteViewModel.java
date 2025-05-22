package com.example.punchpad2;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.annotation.NonNull;

import com.stratonotes.NoteEntity;
import com.stratonotes.FolderWithNotes;

import java.util.List;

public class NoteViewModel extends AndroidViewModel {

    private final NoteRepository repository;

    public LiveData<List<FolderWithNotes>> getFoldersWithPreviews() {
        return repository.getFoldersWithNotes();

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
        NoteEntity updated = new NoteEntity(
                note.getId(),
                note.getFolderId(),
                note.getContent(),
                note.getCreatedAt(),
                System.currentTimeMillis(),
                favorite,
                note.isHiddenFromMain(),
                note.isLarge(),
                note.isTrashed()
        );
        repository.update(updated);
    }

    public LiveData<List<NoteEntity>> getTrashedNotes() {
        return repository.getTrashedNotes();
    }

    public void restore(NoteEntity note) {
        NoteEntity updated = new NoteEntity(
                note.getId(),
                note.getFolderId(),
                note.getContent(),
                note.getCreatedAt(),
                System.currentTimeMillis(),
                note.isFavorite(),
                false,
                note.isLarge(),
                note.isTrashed()
        );
        repository.update(updated);
    }

    public void permanentlyDelete(NoteEntity note) {
        repository.delete(note);
    }

    public LiveData<List<NoteEntity>> getAllNotes() {
        return allNotes;
    }

    private final LiveData<List<NoteEntity>> allNotes;

    public NoteViewModel(@NonNull Application application) {
        super(application);
        repository = new NoteRepository(application);
        this.allNotes = repository.getAllNotes(); // Initialize here
    }




}
