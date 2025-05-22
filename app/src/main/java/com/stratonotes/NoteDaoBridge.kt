package com.stratonotes

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

object NoteDaoBridge {

    @JvmStatic
    fun insertNoteAsync(noteDao: NoteDao, note: NoteEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            noteDao.insertNote(note)
        }
    }

    @JvmStatic
    fun updateNoteAsync(noteDao: NoteDao, note: NoteEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            noteDao.updateNote(note)
        }
    }

    @JvmStatic
    fun deleteNoteAsync(noteDao: NoteDao, note: NoteEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            noteDao.deleteNote(note)
        }
    }
    @JvmStatic
    fun insertFolderAsync(noteDao: NoteDao, folder: FolderEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            noteDao.insertFolder(folder)
        }
    }


    @JvmStatic
    fun getFolderByNameBlocking(dao: NoteDao, name: String): FolderEntity? {
        return runBlocking {
            withContext(Dispatchers.IO) {
                dao.getFolderByName(name)
            }
        }
    }


    @JvmStatic
    fun insertFolderBlocking(dao: NoteDao, folder: FolderEntity): Long {
        return runBlocking {
            dao.insertFolder(folder)
        }
    }




}
