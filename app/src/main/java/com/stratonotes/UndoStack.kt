package com.stratonotes

class UndoStack {

    private val stack = mutableListOf<UndoSnapshot>()
    private var index = -1

    fun push(snapshot: UndoSnapshot) {
        // Ignore if same as current snapshot
        if (stack.getOrNull(index) == snapshot) return

        // Remove anything above the current index (if redo path existed)
        while (stack.size > index + 1) {
            stack.removeAt(stack.lastIndex)
        }

        stack.add(snapshot)

        // Keep only last 50 snapshots
        if (stack.size > 50) {
            stack.removeAt(0)
            index = stack.lastIndex
        } else {
            index = stack.lastIndex
        }
    }


    fun canUndo(): Boolean = index >= 1

    fun canRedo(): Boolean = index < stack.lastIndex

    fun undo(): UndoSnapshot? {
        return if (canUndo()) {
            index--
            stack[index]
        } else {
            null
        }
    }

    fun redo(): UndoSnapshot? {
        return if (canRedo()) {
            index++
            stack[index]
        } else {
            null
        }
    }

    fun clear() {
        stack.clear()
        index = -1
    }

    data class UndoSnapshot(val text: String, val cursor: Int)
}
