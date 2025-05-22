package com.example.punchpad2;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.stratonotes.AppDatabase;
import com.stratonotes.NoteDao;
import com.stratonotes.NoteEntity;
import com.stratonotes.FolderWithNotes;
import com.stratonotes.NoteDaoBridge;

import java.util.List;

public class NoteRepository {

    private final NoteDao noteDao;

    public NoteRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        noteDao = db.noteDao();
    }

    public LiveData<List<FolderWithNotes>> getFoldersWithNotes() {
        return noteDao.getFoldersWithNotes();
    }


    public void insert(NoteEntity note) {
        NoteDaoBridge.insertNoteAsync(noteDao, note);
    }

    public void update(NoteEntity note) {
        NoteDaoBridge.updateNoteAsync(noteDao, note);
    }

    public void delete(NoteEntity note) {
        NoteDaoBridge.deleteNoteAsync(noteDao, note);
    }

    public LiveData<List<NoteEntity>> getTrashedNotes() {
        return noteDao.getTrashedNotes();
    }

    public LiveData<List<NoteEntity>> getAllNotes() {
        return noteDao.getAllNotes();
    }

}
