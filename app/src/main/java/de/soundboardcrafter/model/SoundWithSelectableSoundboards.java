package de.soundboardcrafter.model;

import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Sound with all soundboards, of which some may be selected.
 */
public class SoundWithSelectableSoundboards implements Iterable<SelectableModel<Soundboard>> {
    private final Sound sound;
    private final ImmutableList<SelectableModel<Soundboard>> soundboards;

    public SoundWithSelectableSoundboards(Sound sound,
                                          List<SelectableModel<Soundboard>> soundboards) {
        this.sound = sound;
        this.soundboards = ImmutableList.copyOf(soundboards);
    }

    public Sound getSound() {
        return sound;
    }

    @NonNull
    @Override
    public Iterator<SelectableModel<Soundboard>> iterator() {
        return soundboards.iterator();
    }

    /**
     * Returns the soundboards in order, unmodifiable.
     */
    public ImmutableList<SelectableModel<Soundboard>> getSoundboards() {
        return soundboards;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SoundWithSelectableSoundboards that = (SoundWithSelectableSoundboards) o;
        return Objects.equals(sound, that.sound) &&
                Objects.equals(soundboards, that.soundboards);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sound, soundboards);
    }

    @Override
    @NonNull
    public String toString() {
        return "SoundWithSelectableSoundboards{" +
                "sound=" + sound +
                '}';
    }
}
