package com.example.punchpad2;

public class Note {

    public long id;
    public long folderId;

    public String content;

    public long timestamp; // maps to both createdAt and lastEdited in conversion

    public boolean favorited;
    public boolean hidden;    // maps to isHiddenFromMain
    public boolean large;     // maps to isLarge
    public boolean trashed;   // maps to isTrashed

    public void setFavorited(boolean favorited) {
        this.favorited = favorited;
    }

    public boolean isFavorited() {
        return favorited;
    }

    public void setTrashed(boolean trashed) {
        this.trashed = trashed;
    }

    public boolean isTrashed() {
        return trashed;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setLarge(boolean large) {
        this.large = large;
    }

    public boolean isLarge() {
        return large;
    }
}
