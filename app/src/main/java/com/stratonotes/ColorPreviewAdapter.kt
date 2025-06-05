package com.stratonotes

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.punchpad2.R

class ColorPreviewAdapter(
    pages: List<PreviewPageData>
) : RecyclerView.Adapter<ColorPreviewAdapter.PreviewViewHolder>() {

    data class PreviewPageData(
        val label: String,
        var color: Int // must be var to allow update
    )

    private val previewPages: MutableList<PreviewPageData> = pages.toMutableList()

    fun updateColor(color: Int) {
        previewPages.forEach { it.color = color }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.pager_color_preview, parent, false)
        return PreviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        val item = previewPages[position]
        holder.previewLabel.text = item.label
        holder.previewBackground.setBackgroundColor(item.color)
    }

    override fun getItemCount(): Int = previewPages.size

    class PreviewViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val previewLabel: TextView = view.findViewById(R.id.previewLabel)
        val previewBackground: ImageView = view.findViewById(R.id.previewBackground)
    }
}
