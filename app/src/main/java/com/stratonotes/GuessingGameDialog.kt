package com.stratonotes

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.punchpad2.R
import kotlinx.coroutines.launch


class GuessingGameDialog(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val getAllNotes: suspend () -> List<NoteEntity>
) {
    private val asked = mutableSetOf<String>()

    fun launch() {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val notes = getAllNotes()
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
                allOptions.keys.removeAll(asked)

                if (allOptions.isEmpty()) {
                    asked.clear()
                    Toast.makeText(context, R.string.toast_all_questions_used, Toast.LENGTH_SHORT).show()
                    return@repeatOnLifecycle
                }

                val (target, count) = allOptions.entries.random()
                asked.add(target)

                showDialog(target, count)
            }
        }
    }

    private fun showDialog(target: String, answer: Int) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_guess_game, null)
        val prompt = view.findViewById<TextView>(R.id.questionText)
        val input = view.findViewById<EditText>(R.id.answerInput)
        val result = view.findViewById<TextView>(R.id.resultText)

        prompt.text = context.getString(R.string.guessing_game_prompt, target)

        AlertDialog.Builder(context)
            .setTitle(R.string.guessing_game_title)
            .setView(view)
            .setPositiveButton(R.string.guessing_game_submit, null)
            .setNegativeButton(R.string.guessing_game_close, null)
            .setNeutralButton(R.string.guessing_game_play_again, null)
            .create()
            .apply {
                setOnShowListener {
                    val submit = getButton(AlertDialog.BUTTON_POSITIVE)
                    val retry = getButton(AlertDialog.BUTTON_NEUTRAL)
                    val close = getButton(AlertDialog.BUTTON_NEGATIVE)

                    submit.setOnClickListener {
                        val guess = input.text.toString().toIntOrNull()
                        when (guess) {
                            null -> {
                                result.text = context.getString(R.string.guessing_game_enter_number)
                            }
                            answer -> {
                                result.text = context.getString(R.string.guessing_game_correct)
                            }
                            else -> {
                                result.text = context.getString(R.string.guessing_game_incorrect, answer)
                            }
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
