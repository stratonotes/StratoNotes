package com.stratonotes

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NoteRepository(application)
    private val _allNotes: LiveData<List<NoteEntity>> = repository.allNotes

    val allNotes: LiveData<List<NoteEntity>> get() = _allNotes

    fun getFoldersWithPreviews(): LiveData<List<FolderWithNotes>> {
        return repository.foldersWithNotes

    }

    fun insert(note: NoteEntity) {
        viewModelScope.launch {
            repository.insert(note)
        }
    }

    fun delete(note: NoteEntity) {
        viewModelScope.launch {
            repository.delete(note)
        }
    }

    fun update(note: NoteEntity) {
        viewModelScope.launch {
            repository.update(note)
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

    fun getTrashedNotes(): LiveData<List<NoteEntity>> {
        return repository.trashedNotes
    }



    fun searchNotes(query: String): LiveData<List<NoteEntity>> {
        return repository.searchNotes(query).asLiveData()
    }
}
