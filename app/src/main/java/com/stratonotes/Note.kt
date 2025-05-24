package com.stratonotes

class Note {
    var id: Long = 0
    var folderId: Long = 0

    var content: String? = null

    var timestamp: Long = 0 // maps to both createdAt and lastEdited in conversion

    var isFavorited: Boolean = false
    var isHidden: Boolean = false // maps to isHiddenFromMain
    var isLarge: Boolean = false // maps to isLarge
    var isTrashed: Boolean = false // maps to isTrashed
}