package com.stratonotes

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.punchpad2.R

class SearchResultAdapter(
    private val context: Context
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
        when (val item = getItem(position)) {
            is SearchResultItem.Header -> holder.bindHeader(item.label)
            is SearchResultItem.FolderItem -> holder.bindFolder(item.folder)
            is SearchResultItem.NoteItem -> holder.bindNote(item.note)
        }
    }


    inner class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val resultText: TextView = itemView.findViewById(R.id.searchResultText)

        fun bindNote(note: NoteEntity) {
            resultText.text = note.content.take(40)
            itemView.setOnClickListener {
                val intent = Intent(context, NoteActivity::class.java)
                intent.putExtra("note_id", note.id)
                context.startActivity(intent)
            }
        }

        fun bindFolder(folder: FolderEntity) {
            resultText.text = "📂 " + folder.name.take(40)
            itemView.setOnClickListener {
                Toast.makeText(context, "Folder: ${folder.name}", Toast.LENGTH_SHORT).show()
            }
        }

        fun bindHeader(label: String) {
            resultText.text = label
            resultText.setTextColor(0xFFAAAAAA.toInt()) // optional: make header gray
            resultText.setPadding(16, 12, 16, 8)
            itemView.setOnClickListener(null) // disable clicks on headers
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

