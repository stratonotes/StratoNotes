package com.stratonotes

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders")
    suspend fun getAllFolders(): List<FolderEntity>

    @Insert
    suspend fun insertFolder(folder: FolderEntity)
}
