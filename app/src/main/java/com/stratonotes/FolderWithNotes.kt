package com.stratonotes

import androidx.room.Embedded
import androidx.room.Relation
import androidx.room.Ignore

data class FolderWithNotes(
    @Embedded val folder: FolderEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "folderId"
    )
    val notes: List<NoteEntity>
) {
    @Ignore
    var isExpanded: Boolean = true
}
