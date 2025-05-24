package com.stratonotes

import java.util.Collections
import java.util.Locale

object SearchUtils {
    fun filterAndSort(
        notes: List<Note>,
        query: String?,
        favoritesOnly: Boolean,
        order: SortOrder
    ): List<Note> {
        val result: MutableList<Note> = ArrayList()

        for (note in notes) {
            if ((query == null || note.content!!.lowercase(Locale.getDefault()).contains(
                    query.lowercase(
                        Locale.getDefault()
                    )
                ))
                && (!favoritesOnly || note.isFavorited)
            ) {
                result.add(note)
            }
        }

        var comparator = Comparator.comparingLong { note: Note -> note.timestamp }
        if (order == SortOrder.NEWEST_FIRST) {
            comparator = comparator.reversed()
        }
        Collections.sort(result, comparator)

        return result
    }

    enum class SortOrder {
        NEWEST_FIRST, OLDEST_FIRST
    }
}