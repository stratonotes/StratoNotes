package com.example.punchpad2.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;
import androidx.room.Update;

import java.util.List;

@Dao
public interface NoteDao {
    @Insert
    long insert(NoteEntity note);

    @Delete
    void delete(NoteEntity note);

    @Update
    void update(NoteEntity note);

    @Query("SELECT * FROM notes WHERE isHidden = 0 ORDER BY createdAt DESC LIMIT 3")
    List<NoteEntity> get3MostRecentVisibleNotes();

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    List<NoteEntity> getAll();
}