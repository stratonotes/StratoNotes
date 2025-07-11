package com.stratonotes

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Ignore

data class MediaItem(
    val type: String, // "image" or "audio"
    val uri: String   // URI string pointing to the media file
)

@Entity(tableName = "notes")
data class NoteEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "folderId")
    val folderId: Long,

    @ColumnInfo(name = "content")
    var content: String,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "lastEdited")
    var lastEdited: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "isFavorite")
    var isFavorite: Boolean = false,

    @ColumnInfo(name = "isHiddenFromMain")
    val isHiddenFromMain: Boolean = false,

    @ColumnInfo(name = "isLarge")
    val isLarge: Boolean = false,

    @ColumnInfo(name = "isTrashed")
    var isTrashed: Boolean = false

) {
    @Ignore
    var mediaItems: MutableList<MediaItem> = mutableListOf() // UI-only field

    override fun equals(other: Any?): Boolean {
        return other is NoteEntity && other.id == this.id
    }

    override fun hashCode(): Int = id.hashCode()
}
