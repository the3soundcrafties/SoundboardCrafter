package de.soundboardcrafter.model;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.text.CollationKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A soundboard, that is a keyboard you can play sounds with.
 * The application supports several soundboards.
 * <p></p>
 * <code>SoundboardWithSounds</code>s are not thread-safe. So it might be necessary to use
 * appropriate synchronization.
 */
public class SoundboardWithSounds implements Serializable {
    private final @NonNull
    Soundboard soundboard;

    /**
     * The sounds on their position on the <code>Soundboard</code>.
     * Sounds can be shared between soundboards, and there are no empty
     * positions.
     */
    private final ArrayList<Sound> sounds;

    public SoundboardWithSounds(@NonNull Soundboard soundboard, @NonNull ArrayList<Sound> sounds) {
        this.soundboard = soundboard;
        this.sounds = checkNotNull(sounds, "sound is null");
    }

    @NonNull
    public Soundboard getSoundboard() {
        return soundboard;
    }

    public @NonNull
    UUID getId() {
        return soundboard.getId();
    }

    @NonNull
    public CollationKey getCollationKey() {
        return soundboard.getCollationKey();
    }

    @NonNull
    public String getName() {
        return soundboard.getName();
    }

    /**
     * Returns the sounds in order, unmodifiable.
     */
    public List<Sound> getSounds() {
        return Collections.unmodifiableList(sounds);
    }

    public void sortSoundsBy(Comparator<Sound> comparator) {
        sounds.sort(comparator);
    }

    /**
     * Replaces the sound with this <code>index</code> with the new
     * <code>sound</code>.
     */
    public void setSound(int index, Sound sound) {
        sounds.set(index, sound);
    }

    /**
     * Removes this sound from the soundboard.
     */
    public void removeSound(int index) {
        sounds.remove(index);
    }

    @Override
    public @Nonnull
    String toString() {
        return "SoundboardWithSounds{" +
                "soundboard=" + soundboard +
                '}';
    }


}
