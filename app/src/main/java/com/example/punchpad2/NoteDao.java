package com.example.punchpad2;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface NoteDao {

    @Insert
    void insert(NoteEntity note);

    @Update
    void update(NoteEntity note);

    @Delete
    void delete(NoteEntity note);

    @Query("SELECT * FROM notes WHERE isTrashed = 0 AND isHidden = 0 ORDER BY createdAt DESC LIMIT 3")
    List<NoteEntity> get3MostRecentVisibleNotes();

    @Query("SELECT * FROM notes WHERE isTrashed = 0")
    LiveData<List<NoteEntity>> getAllActiveNotes();

    @Query("SELECT * FROM notes WHERE isTrashed = 1")
    LiveData<List<NoteEntity>> getTrashedNotes();

    @Transaction
    @Query("SELECT * FROM Folder")
    LiveData<List<FolderWithNotes>> getFoldersWithNotes();

    @Query("SELECT * FROM notes")
    LiveData<List<NoteEntity>> getAllNotes(); // keep for observers

    @Query("SELECT * FROM notes")
    List<NoteEntity> getAllNotesNow(); // added for background tasks
}
