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
public abstract class NoteDao {

    // --- Note CRUD ---

    @Insert
    public abstract void insert(NoteEntity note);

    @Update
    public abstract void update(NoteEntity note);

    @Delete
    public abstract void delete(NoteEntity note);

    // --- Note Queries ---

    @Query("SELECT * FROM notes WHERE isTrashed = 0 AND isHidden = 0 ORDER BY createdAt DESC LIMIT 3")
    public abstract List<NoteEntity> get3MostRecentVisibleNotes();

    @Query("SELECT * FROM notes WHERE isTrashed = 0")
    public abstract LiveData<List<NoteEntity>> getAllActiveNotes();

    @Query("SELECT * FROM notes WHERE isTrashed = 1")
    public abstract LiveData<List<NoteEntity>> getTrashedNotes();

    @Query("SELECT * FROM notes")
    public abstract LiveData<List<NoteEntity>> getAllNotes();

    @Query("SELECT * FROM notes")
    public abstract List<NoteEntity> getAllNotesNow();

    // --- Folder Queries and Relationship ---

    @Insert
    public abstract long insertFolder(FolderEntity folder);

    @Update
    public abstract void updateFolder(FolderEntity folder);

    @Delete
    public abstract void deleteFolder(FolderEntity folder);

    @Query("SELECT * FROM folders WHERE name = :name LIMIT 1")
    public abstract FolderEntity getFolderByName(String name);

    @Query("SELECT * FROM folders WHERE id = :folderId")
    public abstract FolderEntity getFolderById(long folderId);

    @Query("SELECT * FROM folders ORDER BY name ASC")
    public abstract List<FolderEntity> getAllFolders();

    @Transaction
    @Query("SELECT * FROM folders")
    public abstract LiveData<List<FolderWithNotes>> getFoldersWithNotes();

    // --- Smart insert-or-get logic ---

    @Transaction
    public FolderEntity getOrCreateFolderByName(String name) {
        FolderEntity existing = getFolderByName(name);
        if (existing != null) return existing;

        FolderEntity folder = new FolderEntity(name);
        long id = insertFolder(folder);
        folder.id = id;
        return folder;
    }
}
