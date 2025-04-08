package com.example.punchpad2;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class FolderWithNotes {

    @Embedded
    public Folder folder;

    @Relation(
            parentColumn = "id",
            entityColumn = "folder_id"
    )
    public List<NoteEntity> notes;
}
