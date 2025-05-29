
package com.stratonotes

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.View.OnTouchListener
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.punchpad2.R

class NoteActivity : Activity() {
    private var noteText: EditText? = null
    private var starIcon: ImageView? = null
    private var isEditing = false
    private var isFavorited = false
    private var noteContainer: LinearLayout? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)

        noteText = findViewById(R.id.noteText)
        starIcon = findViewById(R.id.starIcon)
        val plusButton = findViewById<ImageButton>(R.id.plusButton)
        val mediaOverlay = findViewById<LinearLayout>(R.id.mediaOverlay)
        val recordAudio = findViewById<ImageButton>(R.id.recordAudio)
        val addImage = findViewById<ImageButton>(R.id.addImage)
        noteContainer = findViewById(R.id.noteContainer)
        val voiceOverlay = findViewById<View>(R.id.voiceOverlay)
        val startStopRecording = findViewById<ImageButton>(R.id.startStopRecording)
        val recordingTimer = findViewById<TextView>(R.id.recordingTimer)
        val saveRecording = findViewById<ImageButton>(R.id.saveRecording)
        val recordingIndicator = findViewById<ImageView>(R.id.recordingIndicator)
        val noteText = findViewById<EditText>(R.id.noteText)
        val undoManager = UndoManager(noteText)

        val undoButton = findViewById<ImageButton>(R.id.undoButton)
        val redoButton = findViewById<ImageButton>(R.id.redoButton)

        undoButton.setOnClickListener { v: View? -> undoManager.undo() }
        redoButton.setOnClickListener { v: View? -> undoManager.redo() }

        val isRecording = booleanArrayOf(false)
        val secondsElapsed = intArrayOf(0)
        val timerHandler = Handler()
        val timerRunnable: Runnable = object : Runnable {
            override fun run() {
                secondsElapsed[0]++
                val minutes = secondsElapsed[0] / 60
                val seconds = secondsElapsed[0] % 60
                recordingTimer.text = String.format("%02d:%02d", minutes, seconds)
                timerHandler.postDelayed(this, 1000)
            }
        }

        val incoming = intent.getStringExtra("content")
        val draftManager = DraftManager(this)
        val draftKey = incoming?.hashCode()?.toString() ?: "placeholder"
        val draft = draftManager.loadDraft(draftKey)

        if (draft != null) {
            noteText.setText(draft)
        } else if (incoming != null) {
            noteText.setText(incoming)
        } else {
            noteText.setText("This is a placeholder note.")
        }

        noteText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                draftManager.saveDraft(draftKey, s.toString())
            }
        })

        noteText.setOnClickListener(View.OnClickListener { v: View? ->
            if (!isEditing) {
                isEditing = true
                noteText.setFocusableInTouchMode(true)
                noteText.requestFocus()
                noteText.setCursorVisible(true)
                noteText.setBackgroundColor(0x22000000)
            }
        })

        noteText.setOnFocusChangeListener(OnFocusChangeListener { v: View?, hasFocus: Boolean ->
            if (!hasFocus) {
                isEditing = false
                noteText.setCursorVisible(false)
                noteText.setBackgroundColor(0x00000000)
            }
        })

        val gestureDetector = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                isFavorited = !isFavorited
                val starIcon: ImageView = findViewById(R.id.starIcon)

                Toast.makeText(
                    this@NoteActivity,
                    if (isFavorited) "Note favorited." else "Note unfavorited.", Toast.LENGTH_SHORT
                ).show()
                return true
            }
        })

        noteText.setOnTouchListener(OnTouchListener { v: View?, event: MotionEvent? ->
            gestureDetector.onTouchEvent(
                event!!
            )
        })

        recordAudio.setOnClickListener { v: View? ->
            mediaOverlay.visibility = View.GONE
            voiceOverlay.visibility = View.VISIBLE
        }

        addImage.setOnClickListener { v: View? ->
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.setType("image/*")
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        startStopRecording.setOnClickListener { v: View? ->
            if (!isRecording[0]) {
                isRecording[0] = true
                secondsElapsed[0] = 0
                recordingTimer.text = "00:00"
                timerHandler.postDelayed(timerRunnable, 1000)
                saveRecording.isEnabled = true
                recordingIndicator.alpha = 1f
                Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
            } else {
                isRecording[0] = false
                timerHandler.removeCallbacks(timerRunnable)
                recordingIndicator.alpha = 0.3f
                Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
            }
        }

        saveRecording.setOnClickListener { v: View? ->
            if (!isRecording[0]) {
                voiceOverlay.visibility = View.GONE
                Toast.makeText(this, "Recording saved (stub)", Toast.LENGTH_SHORT)
                    .show()

                val widget = LinearLayout(this)
                widget.orientation = LinearLayout.HORIZONTAL
                widget.setPadding(16, 16, 16, 16)
                widget.setBackgroundColor(-0xcccccd)

                val trimStart = ImageView(this)
                trimStart.setImageResource(R.drawable.ic_trim_left)
                trimStart.layoutParams = LinearLayout.LayoutParams(24, 48)

                val waveform = View(this)
                val waveParams = LinearLayout.LayoutParams(0, 48, 1f)
                waveParams.setMargins(16, 0, 16, 0)
                waveform.layoutParams = waveParams
                waveform.setBackgroundColor(-0x777778)

                val trimEnd = ImageView(this)
                trimEnd.setImageResource(R.drawable.ic_trim_right)
                trimEnd.layoutParams = LinearLayout.LayoutParams(24, 48)

                val timestamp = TextView(this)
                timestamp.text = String.format(
                    "%02d:%02d / %02d:%02d",
                    0,
                    0,
                    secondsElapsed[0] / 60,
                    secondsElapsed[0] % 60
                )
                timestamp.setTextColor(-0x1)
                timestamp.setPadding(8, 0, 8, 0)

                val recordMore = ImageButton(this)
                recordMore.setImageResource(R.drawable.ic_record_add)
                recordMore.setBackgroundColor(0x00000000)
                recordMore.layoutParams = LinearLayout.LayoutParams(48, 48)

                val delete = ImageButton(this)
                delete.setImageResource(R.drawable.ic_delete)
                delete.setBackgroundColor(0x00000000)
                delete.layoutParams = LinearLayout.LayoutParams(48, 48)
                delete.setOnClickListener { d: View? ->
                    (noteText.getParent() as LinearLayout).removeView(
                        widget
                    )
                }

                widget.addView(trimStart)
                widget.addView(waveform)
                widget.addView(trimEnd)
                widget.addView(timestamp)
                widget.addView(recordMore)
                widget.addView(delete)

                // Inside onCreate:
                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

                val widgetGesture = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        Toast.makeText(this@NoteActivity, "Trim mode (stub)", Toast.LENGTH_SHORT).show()
                        return true
                    }
                })

                var dY = 0f
                var dragging = false

                widget.setOnTouchListener { view, event ->
                    if (widgetGesture.onTouchEvent(event)) {
                        return@setOnTouchListener true
                    }

                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            dY = view.y - event.rawY
                            dragging = false
                        }

                        MotionEvent.ACTION_MOVE -> if (event.eventTime - event.downTime > 200) {
                            if (!dragging) {
                                dragging = true
                                vibrator.vibrate(10)
                            }
                            view.y = event.rawY + dY
                        }

                        MotionEvent.ACTION_UP -> {
                            dragging = false
                            view.performClick() // Accessibility compliance
                        }
                    }
                    true
                }





                (noteText.getParent() as LinearLayout).addView(widget)
                noteText.append("\n")
            } else {
                Toast.makeText(
                    this,
                    "Stop recording before saving.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // ✅ Sliding plus menu behavior
        plusButton.setOnClickListener { v: View? ->
            if (mediaOverlay.visibility == View.VISIBLE) {
                mediaOverlay.animate()
                    .translationX(mediaOverlay.width.toFloat())
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction {
                        mediaOverlay.visibility = View.GONE
                        voiceOverlay.visibility = View.GONE
                    }
            } else {
                mediaOverlay.translationX = mediaOverlay.width.toFloat()
                mediaOverlay.alpha = 0f
                mediaOverlay.visibility = View.VISIBLE
                mediaOverlay.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(200)
                    .start()
                voiceOverlay.visibility = View.GONE
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            try {
                val imageUri = data.data
                val inputStream = contentResolver.openInputStream(
                    imageUri!!
                )
                val bitmap = BitmapFactory.decodeStream(inputStream)

                val imageView = ImageView(this)
                imageView.setImageBitmap(bitmap)
                imageView.adjustViewBounds = true
                imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 16, 0, 16)
                imageView.layoutParams = params

                imageView.setOnClickListener { v: View? ->
                    val intent = Intent(
                        this@NoteActivity,
                        FullscreenImageActivity::class.java
                    )
                    intent.putExtra("image_uri", imageUri.toString())
                    startActivity(intent)
                }

                noteContainer!!.addView(imageView, noteContainer!!.indexOfChild(noteText))
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Unsupported image format. Could not import.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 101
    }
}