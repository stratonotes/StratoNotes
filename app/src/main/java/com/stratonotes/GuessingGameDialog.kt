package com.stratonotes

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlin.random.Random

class GuessingGameDialog(
    private val context: Context,
    private val scope: CoroutineScope,
    private val getAllNotes: suspend () -> List<NoteEntity>
) {
    private val asked = mutableSetOf<String>()

    fun launch() {
        scope.lifecycleScope.launchWhenStarted {
            val notes = withContext(Dispatchers.IO) { getAllNotes() }
            val allText = notes.joinToString(" ") { it.content }.lowercase()

            val wordFreq = Regex("\\b[a-z]{3,}\\b").findAll(allText)
                .map { it.value }
                .groupingBy { it }
                .eachCount()
                .filter { it.value >= 2 }

            val letterFreq = allText.filter { it in 'a'..'z' }
                .groupingBy { it.toString() }
                .eachCount()
                .filter { it.value >= 2 }

            val digitFreq = allText.filter { it in '0'..'9' }
                .groupingBy { it.toString() }
                .eachCount()
                .filter { it.value >= 2 }

            val allOptions = (wordFreq + letterFreq + digitFreq).toMutableMap()

            // Remove previously asked
            allOptions.keys.removeAll(asked)

            if (allOptions.isEmpty()) {
                asked.clear()
                Toast.makeText(context, "All questions used. Resetting...", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val (target, count) = allOptions.entries.random()
            asked.add(target)

            showDialog(target, count)
        }
    }

    private fun showDialog(target: String, answer: Int) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_guess_game, null)
        val prompt = view.findViewById<TextView>(R.id.questionText)
        val input = view.findViewById<EditText>(R.id.answerInput)
        val result = view.findViewById<TextView>(R.id.resultText)

        prompt.text = "How many times does \"$target\" appear in your notes?"

        AlertDialog.Builder(context)
            .setTitle("Guessing Game")
            .setView(view)
            .setPositiveButton("Submit", null)
            .setNegativeButton("Close", null)
            .setNeutralButton("Play Again", null)
            .create()
            .apply {
                setOnShowListener {
                    val submit = getButton(AlertDialog.BUTTON_POSITIVE)
                    val retry = getButton(AlertDialog.BUTTON_NEUTRAL)
                    val close = getButton(AlertDialog.BUTTON_NEGATIVE)

                    submit.setOnClickListener {
                        val guess = input.text.toString().toIntOrNull()
                        if (guess == null) {
                            result.text = "Enter a number!"
                        } else if (guess == answer) {
                            result.text = "✅ Correct!"
                        } else {
                            result.text = "❌ Nope. The correct answer was $answer."
                        }
                    }

                    retry.setOnClickListener {
                        dismiss()
                        launch()
                    }

                    close.setOnClickListener {
                        dismiss()
                    }
                }
                show()
            }
    }
}
