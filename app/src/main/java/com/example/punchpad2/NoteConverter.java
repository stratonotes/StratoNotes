package com.example.punchpad2;

import com.stratonotes.NoteEntity;

public class NoteConverter {

    public static Note fromEntity(NoteEntity entity) {
        Note note = new Note();
        note.id = entity.getId();
        note.content = entity.getContent();
        note.timestamp = entity.getLastEdited();
        note.hidden = entity.isHiddenFromMain();
        note.folderId = entity.getFolderId();
        note.favorited = entity.isFavorite();
        note.large = entity.isLarge();
        note.trashed = entity.isTrashed();
        return note;
    }

    public static NoteEntity toEntity(Note note) {
        return new NoteEntity(
                note.id,
                note.folderId,
                note.content,
                note.timestamp,        // createdAt
                note.timestamp,        // lastEdited
                note.favorited,
                note.hidden,
                note.large,
                note.trashed
        );
    }
}
