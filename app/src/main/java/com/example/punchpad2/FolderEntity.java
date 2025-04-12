package com.example.punchpad2;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "folders")
public class FolderEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name;

    public FolderEntity(@NonNull String name) {
        this.name = name;
    }
}
