package com.stratonotes

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val noteRepository: NoteRepository = NoteRepository(AppDatabase.getDatabase(application))
    val allNotes: LiveData<List<NoteEntity>>
    val trashedNotes: LiveData<List<NoteEntity>>
    val foldersWithNotes: LiveData<List<FolderWithNotes>>

    init {
        allNotes = noteRepository.allNotes
        trashedNotes = noteRepository.trashedNotes
        foldersWithNotes = noteRepository.foldersWithNotes
    }

    fun insert(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.insert(note)
        }
    }

    fun ensureStratoNotesFolder() {
        viewModelScope.launch {
            val existingFolders = noteRepository.getAllFolders()
            if (existingFolders.none { it.name == "StratoNotes" }) {
                noteRepository.insertFolder(FolderEntity(name = "StratoNotes"))
                Log.d("NoteViewModel", "StratoNotes folder created.")
            } else {
                Log.d("NoteViewModel", "StratoNotes folder already exists.")
            }
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

    suspend fun getAllNotesNow(): List<NoteEntity> {
        return noteRepository.getAllNotesNow()
    }

    fun getTrashedContent(): LiveData<Pair<List<FolderWithNotes>, List<NoteEntity>>> {
        return MediatorLiveData<Pair<List<FolderWithNotes>, List<NoteEntity>>>().apply {
            var folders: List<FolderWithNotes>? = null
            var notes: List<NoteEntity>? = null

            val fSource = noteRepository.getTrashedFoldersWithNotesLive()
            val nSource = noteRepository.trashedNotes

            addSource(fSource) {
                folders = it
                if (notes != null) value = Pair(folders ?: emptyList(), notes ?: emptyList())
            }

            addSource(nSource) {
                notes = it.filter { note -> note.folderId == 0L }
                if (folders != null) value = Pair(folders ?: emptyList(), notes ?: emptyList())
            }
        }
    }
}
