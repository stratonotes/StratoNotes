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
        undoStack.setInitialState(editText.getText().toString().trim());
        editText.addTextChangedListener(watcher);
    }

    public void clear() {
        undoStack.clear();
    }

    public void undo() {
        if (editText == null || !canUndo()) return;

        isUserChange = false;
        String current = editText.getText().toString();
        String previous = undoStack.undo(current);

        // Strict check prevents clearing to empty by accident
        if (previous != null && !previous.equals(current) && !previous.isEmpty()) {
            editText.setText(previous);
            editText.setSelection(previous.length());
        }

        isUserChange = true;
    }

    public void redo() {
        if (editText == null || !canRedo()) return;

        isUserChange = false;
        String current = editText.getText().toString();
        String next = undoStack.redo(current);

        if (next != null && !next.equals(current)) {
            editText.setText(next);
            editText.setSelection(next.length());
        }

        isUserChange = true;
    }

    public boolean canUndo() {
        return undoStack != null && !undoStack.isEmptyUndo();
    }

    public boolean canRedo() {
        return undoStack != null && !undoStack.isEmptyRedo();
    }
}
