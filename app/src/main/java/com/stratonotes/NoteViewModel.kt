package com.stratonotes

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val noteRepository: NoteRepository
    val allNotes: LiveData<List<NoteEntity>>
    val trashedNotes: LiveData<List<NoteEntity>>
    val foldersWithNotes: LiveData<List<FolderWithNotes>>

    init {
        val noteDao = AppDatabase.getDatabase(application).noteDao()
        noteRepository = NoteRepository(noteDao)

        allNotes = noteRepository.allNotes
        trashedNotes = noteRepository.trashedNotes
        foldersWithNotes = noteRepository.foldersWithNotes
    }

    fun insert(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.insert(note)
        }
    }

    fun delete(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.delete(note)
        }
    }

    fun update(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.update(note)
        }
    }

    fun toggleFavorite(note: NoteEntity, favorite: Boolean) {
        val updated = note.copy(
            isFavorite = favorite,
            lastEdited = System.currentTimeMillis()
        )
        update(updated)
    }

    fun restore(note: NoteEntity) {
        val restored = note.copy(
            isHiddenFromMain = false,
            lastEdited = System.currentTimeMillis()
        )
        update(restored)
    }

    fun permanentlyDelete(note: NoteEntity) {
        delete(note)
    }

    fun getFoldersWithPreviews(): LiveData<List<FolderWithNotes>> = foldersWithNotes

    fun searchNotes(query: String): LiveData<List<NoteEntity>> {
        return noteRepository.searchNotes(query).asLiveData()
    }

    fun emptyTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            noteRepository.permanentlyDeleteAllTrashedNotes()
        }
    }
}
