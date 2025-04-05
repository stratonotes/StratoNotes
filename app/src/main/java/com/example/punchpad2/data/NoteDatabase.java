package com.example.punchpad2.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {NoteEntity.class}, version = 1)
public abstract class NoteDatabase extends RoomDatabase {

    public abstract NoteDao noteDao();

    private static volatile NoteDatabase INSTANCE;

    public static NoteDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (NoteDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    NoteDatabase.class,
                                    "punchpad2.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
