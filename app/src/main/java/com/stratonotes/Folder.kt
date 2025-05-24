package com.stratonotes

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Folder {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    @ColumnInfo(name = "name")
    var name: String? = null

    @ColumnInfo(name = "created_at")
    var createdAt: Long = 0
}