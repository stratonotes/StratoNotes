package com.example.punchpad2;

import java.util.Stack;

public class UndoStack {

    private final Stack<String> undoStack = new Stack<>();
    private final Stack<String> redoStack = new Stack<>();
    private String lastState = "";
    private boolean isInitialStateSet = false;

    public void recordState(String content) {
        if (!isInitialStateSet) {
            lastState = content;
            isInitialStateSet = true;
            return;
        }

        if (!content.equals(lastState)) {
            undoStack.push(lastState);
            lastState = content;
            redoStack.clear();
        }
    }

    public String undo(String currentContent) {
        if (!undoStack.isEmpty()) {
            redoStack.push(currentContent);
            lastState = undoStack.pop();
            return lastState;
        }
        return currentContent;
    }

    public String redo(String currentContent) {
        if (!redoStack.isEmpty()) {
            undoStack.push(currentContent);
            lastState = redoStack.pop();
            return lastState;
        }
        return currentContent;
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
        lastState = "";
        isInitialStateSet = false;
    }

    public boolean isEmptyUndo() {
        return undoStack.isEmpty();
    }

    public boolean isEmptyRedo() {
        return redoStack.isEmpty();
    }

    public void setInitialState(String content) {
        lastState = content;
        isInitialStateSet = true;
    }
}
