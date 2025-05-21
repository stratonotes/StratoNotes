package com.stratonotes

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "folderId")
    val folderId: Long,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "lastEdited")
    val lastEdited: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "isFavorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "isHiddenFromMain")
    val isHiddenFromMain: Boolean = false,

    @ColumnInfo(name = "isLarge")
    val isLarge: Boolean = false,

    @ColumnInfo(name = "isTrashed")
    val isTrashed: Boolean = false

)
