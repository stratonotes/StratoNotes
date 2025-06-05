package com.stratonotes

import android.app.Activity
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.viewpager2.widget.ViewPager2
import com.example.punchpad2.R

class ColorPickerOverlay(
    private val activity: Activity,
    private val rootContainer: ViewGroup,

    private val onApply: () -> Unit
) {
    private var overlayView: View? = null
    private var previewBox: View? = null
    private var redSeek: SeekBar? = null
    private var greenSeek: SeekBar? = null
    private var blueSeek: SeekBar? = null
    private var pager: ViewPager2? = null

    fun show() {
        val inflater = activity.layoutInflater
        overlayView = inflater.inflate(R.layout.color_picker_overlay, rootContainer, false)
        Log.d("ColorPickerOverlay", "Attempting to show overlay")

        overlayView?.let { view ->

                Log.d("ColorPickerOverlay", "Attempting to show overlay")

                // 🔽 ADD THIS LINE right after entering the block:
                view.setBackgroundColor(Color.RED) // Debug only

            // ViewPager
            pager = view.findViewById(R.id.colorPreviewPager)
            val pages = listOf(
                ColorPreviewAdapter.PreviewPageData("Main", UserColorManager.getOverlayColor(activity)),
                ColorPreviewAdapter.PreviewPageData("Library", UserColorManager.getOverlayColor(activity))
            )
            pager?.adapter = ColorPreviewAdapter(pages)
            pager?.orientation = ViewPager2.ORIENTATION_HORIZONTAL

            // Preview color box
            previewBox = view.findViewById(R.id.colorPreview)

            // SeekBars
            redSeek = view.findViewById(R.id.seekRed)
            greenSeek = view.findViewById(R.id.seekGreen)
            blueSeek = view.findViewById(R.id.seekBlue)

            val updateColor: () -> Unit = {
                val color = Color.rgb(
                    redSeek?.progress ?: 0,
                    greenSeek?.progress ?: 0,
                    blueSeek?.progress ?: 0
                )
                previewBox?.setBackgroundColor(color)
                (pager?.adapter as? ColorPreviewAdapter)?.updateColor(color)
            }

            // Set listeners
            redSeek?.setOnSeekBarChangeListener(seekListener(updateColor))
            greenSeek?.setOnSeekBarChangeListener(seekListener(updateColor))
            blueSeek?.setOnSeekBarChangeListener(seekListener(updateColor))

            // Save
            view.findViewById<Button>(R.id.saveColorButton).setOnClickListener {
                val finalColor = Color.rgb(
                    redSeek?.progress ?: 0,
                    greenSeek?.progress ?: 0,
                    blueSeek?.progress ?: 0
                )
                UserColorManager.setOverlayColor(activity, finalColor)
                onApply()
                close()
            }

            // Reset
            view.findViewById<Button>(R.id.resetColorButton).setOnClickListener {
                val default = UserColorManager.getDefaultOverlayColor()
                redSeek?.progress = Color.red(default)
                greenSeek?.progress = Color.green(default)
                blueSeek?.progress = Color.blue(default)
            }

            // Cancel
            view.findViewById<Button>(R.id.cancelButton).setOnClickListener {
                close()
            }

            // Close (X)
            view.findViewById<ImageButton>(R.id.closeColorPicker).setOnClickListener {
                close()
            }

            // Initial values
            val currentColor = UserColorManager.getOverlayColor(activity)
            redSeek?.progress = Color.red(currentColor)
            greenSeek?.progress = Color.green(currentColor)
            blueSeek?.progress = Color.blue(currentColor)
            updateColor()

            rootContainer.addView(view)
            Log.d("ColorPickerOverlay", "overlayView added: ${view.parent != null}")
            view.visibility = View.VISIBLE
            Log.d("ColorPickerOverlay", "view visibility: ${view.visibility}, alpha: ${view.alpha}")

            view.bringToFront()
            view.invalidate()
        }
    }

    fun hide() {
        if (overlayView != null) {
            rootContainer.removeView(overlayView)


            overlayView = null
        }
    }

    fun close() = hide()

    private fun seekListener(onChange: () -> Unit): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                onChange()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
    }
}
