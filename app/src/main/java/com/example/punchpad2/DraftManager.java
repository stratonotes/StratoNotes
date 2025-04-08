package com.example.punchpad2;

import android.content.Context;
import android.content.SharedPreferences;

public class DraftManager {

    private final SharedPreferences prefs;

    public DraftManager(Context context) {
        prefs = context.getSharedPreferences("Drafts", Context.MODE_PRIVATE);
    }

    public void saveDraft(String key, String content) {
        prefs.edit().putString(key, content).apply();
    }

    public String loadDraft(String key) {
        return prefs.getString(key, null);
    }

    public void clearDraft(String key) {
        prefs.edit().remove(key).apply();
    }
}
