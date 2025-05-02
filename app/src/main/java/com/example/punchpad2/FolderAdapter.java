package com.example.punchpad2;

import android.content.Context;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    private final List<FolderWithNotes> folders;
    private final Context context;
    private final OnNoteInteractionListener listener;

    private boolean deleteMode = false;
    private boolean foldersOnlyView = false;
    private final Set<NoteEntity> expandedNotes = new HashSet<>();
    public int getAdapterPositionForNote(NoteEntity target) {
        for (int i = 0; i < folders.size(); i++) {
            for (NoteEntity note : folders.get(i).notes) {
                if (note.id == target.id) return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    public interface OnNoteInteractionListener {
        void onNoteTapped(NoteEntity note);
    }

    public FolderAdapter(Context context, List<FolderWithNotes> folders, OnNoteInteractionListener listener) {
        this.context = context;
        this.folders = folders;
        this.listener = listener;
    }

    public void setDeleteMode(boolean deleteMode) {
        this.deleteMode = deleteMode;
        notifyDataSetChanged();
    }

    public void setFoldersOnlyView(boolean foldersOnlyView) {
        this.foldersOnlyView = foldersOnlyView;
        notifyDataSetChanged();
    }

    public void updateFilteredList(List<FolderWithNotes> filteredFolders, boolean foldersOnly) {
        this.folders.clear();
        this.folders.addAll(filteredFolders);
        this.foldersOnlyView = foldersOnly;
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
        holder.bind(folders.get(position));
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

        void bind(FolderWithNotes folderWithNotes) {
            folderName.setText(folderWithNotes.folder.name);
            notesContainer.removeAllViews();

            expandButton.setOnClickListener(v -> {
                folderWithNotes.folder.expanded = !folderWithNotes.folder.expanded;
                notifyDataSetChanged();
            });

            expandButton.setRotation(folderWithNotes.folder.expanded ? 180 : 0);
            notesContainer.setVisibility(folderWithNotes.folder.expanded && !foldersOnlyView ? View.VISIBLE : View.GONE);

            if (folderWithNotes.folder.expanded && !foldersOnlyView) {
                List<NoteEntity> notes = folderWithNotes.notes;
                NoteAdapter tempAdapter = new NoteAdapter(context, notes, deleteMode, null);

                for (int i = 0; i < Math.min(3, notes.size()); i++) {
                    View noteView = LayoutInflater.from(context).inflate(R.layout.item_note, notesContainer, false);
                    NoteAdapter.NoteViewHolder noteHolder = tempAdapter.new NoteViewHolder(noteView);
                    NoteEntity note = notes.get(i);
                    noteHolder.bind(note);

                    setupNoteClickListeners(noteView, note);
                    notesContainer.addView(noteView);
                }
            }
        }

        private void setupNoteClickListeners(View noteView, NoteEntity note) {
            GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    listener.onNoteTapped(note);
                    return true;
                }

                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    // double-tap behavior removed—favorites handled elsewhere
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    // reserved for future use
                }
            });

            noteView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
        }
    }

    private void handleNoteExpansion(NoteEntity note) {
        if (!expandedNotes.contains(note)) {
            if (expandedNotes.size() >= 3) {
                NoteEntity toCollapse = new ArrayList<>(expandedNotes).get(0);
                toCollapse.expanded = false;
                expandedNotes.remove(toCollapse);
            }
            note.expanded = true;
            expandedNotes.add(note);
        } else {
            note.expanded = false;
            expandedNotes.remove(note);
        }
    }
}
