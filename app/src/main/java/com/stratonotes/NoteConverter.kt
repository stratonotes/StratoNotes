package com.stratonotes
@Suppress("unused")
object NoteConverter {
    fun fromEntity(entity: NoteEntity): Note {
        return Note().apply {
            id = entity.id
            folderId = entity.folderId
            content = entity.content
            timestamp = entity.lastEdited
            isHidden = entity.isHiddenFromMain
            isFavorited = entity.isFavorite
            isLarge = entity.isLarge
            isTrashed = entity.isTrashed
        }
    }

    fun toEntity(note: Note): NoteEntity {
        return NoteEntity(
            id = note.id,
            folderId = note.folderId,
            content = note.content ?: "",
            createdAt = note.timestamp,   // Adjust as needed
            lastEdited = note.timestamp,
            isFavorite = note.isFavorited,
            isHiddenFromMain = note.isHidden,
            isLarge = note.isLarge,
            isTrashed = note.isTrashed
        )
    }
}
