package com.stratonotes

import android.app.Application
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow

class NoteRepository(application: Application) {

    private val noteDao: NoteDao = AppDatabase.getInstance(application).noteDao()

    val allNotes = noteDao.getAllNotes()
    val trashedNotes = noteDao.getTrashedNotes()
    val foldersWithNotes = noteDao.getFoldersWithNotes()

    fun insert(note: NoteEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            noteDao.insertNote(note)
        }
    }

    fun update(note: NoteEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            noteDao.updateNote(note)
        }
    }

    fun delete(note: NoteEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            noteDao.deleteNote(note)
        }
    }

    fun searchNotes(query: String): Flow<List<NoteEntity>> {
        return noteDao.searchNotes(query)
    }
}
