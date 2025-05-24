package com.stratonotes

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class DraftManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("Drafts", Context.MODE_PRIVATE)

    fun saveDraft(key: String?, content: String?) {
        prefs.edit() { putString(key, content) }
    }

    fun loadDraft(key: String?): String? {
        return prefs.getString(key, null)
    }

    fun clearDraft(key: String?) {
        prefs.edit() { remove(key) }
    }
}