package com.example.punchpad2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TrashAdapter extends RecyclerView.Adapter<TrashAdapter.TrashViewHolder> {

    public interface TrashActionListener {
        void onRestore(NoteEntity note);
        void onDelete(NoteEntity note);
    }

    private final List<NoteEntity> trashedNotes;
    private final TrashActionListener listener;

    public TrashAdapter(List<NoteEntity> trashedNotes, TrashActionListener listener) {
        this.trashedNotes = trashedNotes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TrashViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trash_note, parent, false);
        return new TrashViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TrashViewHolder holder, int position) {
        holder.bind(trashedNotes.get(position));
    }

    @Override
    public int getItemCount() {
        return trashedNotes.size();
    }

    class TrashViewHolder extends RecyclerView.ViewHolder {
        TextView trashContent;
        ImageButton restoreButton;
        ImageButton deleteButton;

        TrashViewHolder(View itemView) {
            super(itemView);
            trashContent = itemView.findViewById(R.id.trashContent);
            restoreButton = itemView.findViewById(R.id.restoreButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        void bind(NoteEntity note) {
            trashContent.setText(note.content);
            restoreButton.setOnClickListener(v -> listener.onRestore(note));
            deleteButton.setOnClickListener(v -> listener.onDelete(note));
        }
    }
}
