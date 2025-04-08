package com.example.punchpad2;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "notes")
public class NoteEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String content; // full text content, image/audio tags inline

    public long createdAt;
    public long lastEdited;

    public boolean isHidden;
    public boolean isLarge;

    @ColumnInfo(name = "folder_id")
    public int folderId;

    public boolean isTrashed;
    public boolean favorited;

    // future fields can be added here with migration
}
