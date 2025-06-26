package com.stratonotes

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.stratonotes.data.TrashedFolderWithNotes

class TrashViewModel(private val noteRepository: NoteRepository) : ViewModel() {

    fun getTrashedContent(): LiveData<Pair<List<TrashedFolderWithNotes>, List<NoteEntity>>> = liveData {
        val trashedFolders = noteRepository.getTrashedFoldersWithNotes()
        val trashedNotes = noteRepository.getTrashedUnfolderedNotes()
        emit(Pair(trashedFolders, trashedNotes))
    }

    suspend fun restore(note: NoteEntity) {
        noteRepository.restoreNote(note)
    }

    suspend fun permanentlyDelete(note: NoteEntity) {
        noteRepository.deleteNotePermanently(note)
    }

    suspend fun emptyTrash() {
        noteRepository.emptyTrash()
    }
}
