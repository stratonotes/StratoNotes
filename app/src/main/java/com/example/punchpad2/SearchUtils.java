package com.example.punchpad2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SearchUtils {

    public enum SortOrder { NEWEST_FIRST, OLDEST_FIRST }

    public static List<Note> filterAndSort(List<Note> notes, String query, boolean favoritesOnly, SortOrder order) {
        List<Note> result = new ArrayList<>();

        for (Note note : notes) {
            if ((query == null || note.content.toLowerCase().contains(query.toLowerCase()))
                    && (!favoritesOnly || note.favorited)) {
                result.add(note);
            }
        }

        Comparator<Note> comparator = Comparator.comparingLong(note -> note.timestamp);
        if (order == SortOrder.NEWEST_FIRST) {
            comparator = comparator.reversed();
        }
        Collections.sort(result, comparator);

        return result;
    }
}
