package com.example.punchpad2.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class NoteEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String content; // full text content, image/audio tags inline

    public long createdAt;
    public long lastEdited;

    public boolean isHidden;
    public boolean isLarge;

    // future fields can be added here with migration
}