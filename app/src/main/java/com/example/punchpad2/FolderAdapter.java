package com.example.punchpad2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    private final List<FolderWithNotes> folders;
    private final Context context;
    private final NoteAdapter.OnNoteChangedListener listener;
    private int expandedFolderIndex = -1;

    public FolderAdapter(Context context, List<FolderWithNotes> folders, NoteAdapter.OnNoteChangedListener listener) {
        this.context = context;
        this.folders = folders;
        this.listener = listener;
    }

    public void updateFilteredList(List<FolderWithNotes> filteredFolders) {
        this.folders.clear();
        this.folders.addAll(filteredFolders);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        holder.bind(folders.get(position), position);
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView folderName;
        LinearLayout notesContainer;
        ImageButton expandButton;

        FolderViewHolder(View itemView) {
            super(itemView);
            folderName = itemView.findViewById(R.id.folderName);
            notesContainer = itemView.findViewById(R.id.notesContainer);
            expandButton = itemView.findViewById(R.id.expandButton);
        }

        void bind(FolderWithNotes folderWithNotes, int position) {
            folderName.setText(folderWithNotes.folder.name);
            notesContainer.removeAllViews();

            if (position == expandedFolderIndex) {
                expandButton.setRotation(180);
                List<NoteEntity> notes = folderWithNotes.notes;
                for (int i = 0; i < Math.min(notes.size(), 3); i++) {
                    View noteView = LayoutInflater.from(context).inflate(R.layout.item_note, notesContainer, false);
                    NoteAdapter noteAdapter = new NoteAdapter(context, notes, false, listener);
                    NoteAdapter.NoteViewHolder holder = noteAdapter.new NoteViewHolder(noteView);
                    holder.bind(notes.get(i));
                    notesContainer.addView(noteView);
                }
                notesContainer.setVisibility(View.VISIBLE);
            } else {
                expandButton.setRotation(0);
                notesContainer.setVisibility(View.GONE);
            }

            expandButton.setOnClickListener(v -> {
                if (expandedFolderIndex == position) {
                    expandedFolderIndex = -1;
                } else {
                    expandedFolderIndex = position;
                }
                notifyDataSetChanged();
            });
        }
    }
}
