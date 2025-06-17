package com.stratonotes

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.punchpad2.R

class SearchResultAdapter(
    private val context: Context,
    private val onItemClick: (SearchResultItem) -> Unit
) : ListAdapter<SearchResultItem, SearchResultAdapter.ResultViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SearchResultItem>() {
            override fun areItemsTheSame(oldItem: SearchResultItem, newItem: SearchResultItem): Boolean {
                return when {
                    oldItem is SearchResultItem.NoteItem && newItem is SearchResultItem.NoteItem ->
                        oldItem.note.id == newItem.note.id
                    oldItem is SearchResultItem.FolderItem && newItem is SearchResultItem.FolderItem ->
                        oldItem.folder.id == newItem.folder.id
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: SearchResultItem, newItem: SearchResultItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val layout = when (viewType) {
            0 -> R.layout.item_search_header
            else -> R.layout.item_search_result
        }

        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val resultText: TextView = itemView.findViewById(R.id.searchResultText)

        fun bind(item: SearchResultItem) {
            when (item) {
                is SearchResultItem.NoteItem -> {
                    resultText.text = item.note.content.take(40)
                }
                is SearchResultItem.FolderItem -> {
                    resultText.text = "📂 " + item.folder.name.take(40)
                }
                is SearchResultItem.Header -> {
                    resultText.text = item.label
                    resultText.setTextAppearance(android.R.style.TextAppearance_Material_Medium)
                    resultText.setPadding(16, 12, 16, 8)
                }
            }

            // Unified click listener for all item types (used in MainActivity)
            itemView.setOnClickListener {

                onItemClick(item)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SearchResultItem.Header -> 0
            is SearchResultItem.FolderItem -> 1
            is SearchResultItem.NoteItem -> 2
        }
    }
}
