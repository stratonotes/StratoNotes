package com.stratonotes

import android.app.Application
import androidx.lifecycle.LiveData
import com.stratonotes.data.TrashedFolderWithNotes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class NoteRepository(private val db: AppDatabase) {
    private val noteDao = db.noteDao()

    val allNotes = noteDao.getAllNotes()
    val trashedNotes = noteDao.getTrashedNotes()
    val foldersWithNotes = noteDao.getFoldersWithNotes()

    suspend fun getAllFolders() = db.folderDao().getAllFolders()

    suspend fun insertFolder(folder: FolderEntity) = db.folderDao().insertFolder(folder)

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

    suspend fun permanentlyDeleteAllTrashedNotes() {
        noteDao.permanentlyDeleteAllTrashedNotes()
    }

    suspend fun getAllNotesNow(): List<NoteEntity> {
        return noteDao.getAllNotesNow()
    }

    // ✅ NEW: Get folders with their trashed notes
    suspend fun getTrashedFoldersWithNotes(): List<TrashedFolderWithNotes> {
        return noteDao.getFullyTrashedFolders()
    }


    // ✅ NEW: Get standalone trashed notes (folderId == 0 or orphaned)
    suspend fun getTrashedUnfolderedNotes(): List<NoteEntity> {
        return noteDao.getAllNotesNow().filter { it.isTrashed && it.folderId == 0L }
    }

    // ✅ Optional helpers
    suspend fun restoreNote(note: NoteEntity) {
        noteDao.updateNote(note.copy(isTrashed = false))
    }

    suspend fun deleteNotePermanently(note: NoteEntity) {
        noteDao.deleteNote(note)
    }

    suspend fun emptyTrash() {
        noteDao.permanentlyDeleteAllTrashedNotes()
    }

    fun getTrashedFoldersWithNotesLive(): LiveData<List<FolderWithNotes>> {
        return noteDao.getTrashedFoldersWithNotes()
    }

}
