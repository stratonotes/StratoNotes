package com.example.punchpad2;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class UndoManager {

    private final UndoStack undoStack = new UndoStack();
    private boolean isUserChange = true;
    private EditText editText;
    private final TextWatcher watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (isUserChange) {
                undoStack.recordState(s.toString());
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {}
    };

    public void attach(EditText editText) {
        this.editText = editText;
        editText.addTextChangedListener(watcher);
    }

    public void clear() {
        undoStack.clear();
    }

    public void undo() {
        if (editText == null) return;
        isUserChange = false;
        String current = editText.getText().toString();
        String previous = undoStack.undo(current);
        editText.setText(previous);
        editText.setSelection(previous.length());
        isUserChange = true;
    }

    public void redo() {
        if (editText == null) return;
        isUserChange = false;
        String current = editText.getText().toString();
        String next = undoStack.redo(current);
        editText.setText(next);
        editText.setSelection(next.length());
        isUserChange = true;
    }
}
