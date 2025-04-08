package com.example.punchpad2;

import com.example.punchpad2.NoteEntity;

public class NoteConverter {

    public static Note fromEntity(NoteEntity entity) {
        Note note = new Note();
        note.id = entity.id;
        note.content = entity.content;
        note.timestamp = entity.lastEdited;
        note.hidden = entity.isHidden;
        note.folderId = 0; // Folder ID is unknown in legacy entity
        note.favorited = false; // Default assumption unless migration says otherwise
        return note;
    }

    public static NoteEntity toEntity(Note note) {
        NoteEntity entity = new NoteEntity();
        entity.id = note.id;
        entity.content = note.content;
        entity.createdAt = note.timestamp; // Treat current timestamp as createdAt
        entity.lastEdited = note.timestamp;
        entity.isHidden = note.hidden;
        entity.isLarge = false; // No size calc yet
        return entity;
    }
}
