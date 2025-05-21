package com.example.punchpad2;

import androidx.room.Embedded;
import androidx.room.Relation;
import com.stratonotes.NoteEntity;
import java.util.List;

public class FolderWithNotes {

    @Embedded
    public FolderEntity folder;

    @Relation(
            parentColumn = "id",
            entityColumn = "folderId"  // <-- must match @ColumnInfo in NoteEntity
    )
    public List<NoteEntity> notes;
}
