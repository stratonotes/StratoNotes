package com.stratonotes.data

import androidx.room.Embedded
import androidx.room.Relation
import com.stratonotes.FolderEntity
import com.stratonotes.NoteEntity

data class TrashedFolderWithNotes(
    @Embedded val folder: FolderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "folderId"
    )
    val notes: List<NoteEntity>
)
