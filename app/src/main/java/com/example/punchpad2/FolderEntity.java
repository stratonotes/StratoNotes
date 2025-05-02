package com.example.punchpad2;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.annotation.NonNull;
import androidx.room.Ignore;

@Entity(tableName = "folders")
public class FolderEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "name")
    public String name;

    public long createdAt;
    public long lastEdited;

    public boolean isHidden;
    public boolean isTrashed;
    public boolean favorited;

    @Ignore
    public boolean expanded = false;

    public FolderEntity(@NonNull String name) {
        this.name = name;
        this.createdAt = System.currentTimeMillis();
        this.lastEdited = this.createdAt;
        this.isHidden = false;
        this.isTrashed = false;
        this.favorited = false;
    }
}
