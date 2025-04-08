package com.example.punchpad2;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity
public class Note {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "folder_id")
    public int folderId;

    @ColumnInfo(name = "content")
    public String content;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @ColumnInfo(name = "favorited")
    public boolean favorited;

    @ColumnInfo(name = "hidden_from_preview")
    public boolean hidden;

    public void setFavorited(boolean favorited) {
        this.favorited = favorited;
    }

    public boolean isFavorited() {
        return favorited;
    }
    @ColumnInfo(name = "isTrashed")
    public boolean isTrashed;

    public void setTrashed(boolean trashed) {
        this.isTrashed = trashed;
    }

    public boolean isTrashed() {
        return isTrashed;
    }

}
