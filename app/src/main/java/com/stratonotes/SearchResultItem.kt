package com.stratonotes

sealed class SearchResultItem {
    data class NoteItem(val note: NoteEntity) : SearchResultItem()
    data class FolderItem(val folder: FolderEntity) : SearchResultItem()
    data class Header(val label: String) : SearchResultItem()
}
