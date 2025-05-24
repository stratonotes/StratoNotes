package com.stratonotes

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import kotlinx.coroutines.*

class UndoManager(private val editText: EditText) {

    private val undoStack = UndoStack()
    private var lastSnapshot: UndoStack.UndoSnapshot? = null
    private var debounceJob: Job? = null

    init {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                debounceJob?.cancel()
                debounceJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(500)
                    pushSnapshot()
                }
            }
        })
    }

    fun pushSnapshot() {
        val text = editText.text.toString()
        val cursor = editText.selectionStart
        val current = UndoStack.UndoSnapshot(text, cursor)

        if (lastSnapshot != current) {
            undoStack.push(current)
            lastSnapshot = current
        }
    }

    fun undo() {
        val snapshot = undoStack.undo()
        snapshot?.let {
            editText.setText(it.text)
            editText.setSelection(it.cursor.coerceIn(0, it.text.length))
        }
    }

    fun redo() {
        val snapshot = undoStack.redo()
        snapshot?.let {
            editText.setText(it.text)
            editText.setSelection(it.cursor.coerceIn(0, it.text.length))
        }
    }

    fun canUndo(): Boolean = undoStack.canUndo()

    fun canRedo(): Boolean = undoStack.canRedo()

    fun clear() {
        undoStack.clear()
        lastSnapshot = null
    }
}
