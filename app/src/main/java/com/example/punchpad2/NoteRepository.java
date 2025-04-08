package com.example.punchpad2;

import android.app.Application;
import android.os.AsyncTask;
import androidx.lifecycle.LiveData;
import java.util.List;

public class NoteRepository {

    private final NoteDao noteDao;
    private final LiveData<List<FolderWithNotes>> allFolders;

    public NoteRepository(Application application) {
        NoteDatabase db = NoteDatabase.getInstance(application);
        noteDao = db.noteDao();
        allFolders = noteDao.getFoldersWithNotes();
    }

    public LiveData<List<FolderWithNotes>> getAllFoldersWithNotes() {
        return allFolders;
    }

    public void insert(NoteEntity note) {
        AsyncTask.execute(() -> noteDao.insert(note));
    }

    public void update(NoteEntity note) {
        AsyncTask.execute(() -> noteDao.update(note));
    }

    public void delete(NoteEntity note) {
        AsyncTask.execute(() -> noteDao.delete(note));
    }

    public LiveData<List<NoteEntity>> getTrashedNotes() {
        return noteDao.getTrashedNotes();
    }
}
