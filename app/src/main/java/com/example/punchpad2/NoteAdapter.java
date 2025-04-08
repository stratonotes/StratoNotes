package com.example.punchpad2;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private final List<NoteEntity> notes;
    private final Context context;
    private final boolean deleteMode;
    private final OnNoteChangedListener listener;

    public interface OnNoteChangedListener {
        void onNoteUpdated(NoteEntity note);
    }

    public NoteAdapter(Context context, List<NoteEntity> notes, boolean deleteMode, OnNoteChangedListener listener) {
        this.context = context;
        this.notes = notes;
        this.deleteMode = deleteMode;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.bind(notes.get(position));
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {
        EditText noteText;
        ImageView starIcon;
        boolean isEditing = false;

        NoteViewHolder(View itemView) {
            super(itemView);
            noteText = itemView.findViewById(R.id.noteText);
            starIcon = itemView.findViewById(R.id.starIcon);
        }

        void bind(NoteEntity note) {
            noteText.setText(note.content);
            starIcon.setImageResource(note.isHidden ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);

            noteText.setFocusable(false);
            noteText.setCursorVisible(false);
            noteText.setBackgroundColor(Color.TRANSPARENT);

            GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    note.isHidden = !note.isHidden;
                    starIcon.setImageResource(note.isHidden ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
                    Toast.makeText(context,
                            note.isHidden ? "Note hidden from preview." : "Note visible in preview.",
                            Toast.LENGTH_SHORT).show();
                    listener.onNoteUpdated(note);
                    return true;
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    if (!isEditing) {
                        isEditing = true;
                        noteText.setFocusableInTouchMode(true);
                        noteText.setCursorVisible(true);
                        noteText.requestFocus();
                        noteText.setBackgroundColor(0x22000000);
                    }
                    return true;
                }
            });

            noteText.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

            noteText.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    isEditing = false;
                    noteText.setCursorVisible(false);
                    noteText.setBackgroundColor(Color.TRANSPARENT);
                }
            });

            noteText.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(Editable s) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    note.content = s.toString();
                    listener.onNoteUpdated(note);
                }
            });

            if (deleteMode && note.isHidden) {
                starIcon.setOnClickListener(v ->
                        Toast.makeText(context, "Can't delete hidden notes.", Toast.LENGTH_SHORT).show());
            }
        }
    }
}
