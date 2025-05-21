package com.stratonotes

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.lifecycle.LiveData

@Dao
interface NoteDao {

    // Insert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    // Update
    @Update
    suspend fun updateNote(note: NoteEntity)

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    // Delete
    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)

    // Query: All Notes
    @Query("SELECT * FROM notes ORDER BY lastEdited DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    // Query: Notes by Folder
    @Query("SELECT * FROM notes WHERE folderId = :folderId ORDER BY lastEdited DESC")
    fun getNotesByFolder(folderId: Long): Flow<List<NoteEntity>>

    // Query: All Folders
    @Query("SELECT * FROM folders ORDER BY createdAt DESC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    // Search
    @Query("SELECT * FROM notes WHERE content LIKE '%' || :query || '%' ORDER BY lastEdited DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Transaction
    @Query("SELECT * FROM folders ORDER BY createdAt DESC")
    fun getFoldersWithNotes(): LiveData<List<FolderWithNotes>>

    @Query("SELECT * FROM notes WHERE isTrashed = 1 ORDER BY lastEdited DESC")
    fun getTrashedNotes(): LiveData<List<NoteEntity>>

    @Query("SELECT * FROM folders WHERE name = :name LIMIT 1")
    fun getFolderByName(name: String): FolderEntity?


    @Query("SELECT * FROM notes WHERE isTrashed = 0 AND isHiddenFromMain = 0 ORDER BY lastEdited DESC LIMIT 3")
    fun get3MostRecentVisibleNotes(): List<NoteEntity>


}
