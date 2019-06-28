package de.soundboardcrafter.model;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * A soundboard that might be selected.
 */
public class SelectableSoundboard {
    private final Soundboard soundboard;
    private boolean selected;

    public SelectableSoundboard(Soundboard soundboard, boolean selected) {
        this.soundboard = soundboard;
        this.selected = selected;
    }

    public Soundboard getSoundboard() {
        return soundboard;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SelectableSoundboard that = (SelectableSoundboard) o;
        return selected == that.selected &&
                Objects.equals(soundboard, that.soundboard);
    }

    @Override
    public int hashCode() {
        return Objects.hash(soundboard, selected);
    }

    @Override
    @NonNull
    public String toString() {
        return "SelectableSoundboard{" +
                "soundboard=" + soundboard +
                ", selected=" + selected +
                '}';
    }
}
