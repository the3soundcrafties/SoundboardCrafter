package de.soundboardcrafter.model;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import de.soundboardcrafter.util.UuidUtil;

/**
 * A {@link Soundboard} with sounds.
 * <p></p>
 * <code>SoundboardWithSounds</code>s are not thread-safe. So it might be necessary to use
 * appropriate synchronization.
 */
public class SoundboardWithSounds implements Serializable {
    public static final Comparator<SoundboardWithSounds> PROVIDED_LAST_THEN_BY_COLLATION_KEY =
            Comparator.comparing(SoundboardWithSounds::getSoundboard,
                    Soundboard.PROVIDED_LAST_THEN_BY_COLLATION_KEY);

    @NonNull
    private final Soundboard soundboard;

    /**
     * The sounds on their position on the <code>Soundboard</code>.
     * Sounds can be shared between soundboards, and there are no empty
     * positions.
     */
    private final ArrayList<Sound> sounds;

    public SoundboardWithSounds(@NonNull Soundboard soundboard, @NonNull List<Sound> sounds) {
        this.soundboard = soundboard;
        this.sounds = Lists.newArrayList(checkNotNull(sounds, "sound is null"));
    }

    /**
     * This hash value stays the same for the same soundboard - as long as the included
     * sounds (IDs) are not changed (added, removed) and not reordered.
     * <p>
     * For different soundboards - or when the sounds (IDs) are changed (added, removed) or
     * reordered -, this hash is very likely to change.
     */
    public long getLongHashForSoundboardIdAndSoundIds() {
        return Objects.hash(
                UuidUtil.toLong(soundboard.getId()),
                sounds.stream()
                        .map(s -> UuidUtil.toLong(s.getId()))
                        .collect(Collectors.toList()));
    }

    @NonNull
    public UUID getId() {
        return soundboard.getId();
    }

    @NonNull
    public String getName() {
        return soundboard.getName();
    }

    public boolean isProvided() {
        return soundboard.isProvided();
    }

    @NonNull
    public Soundboard getSoundboard() {
        return soundboard;
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
     * Moves this sound in the soundboard from the <code>oldIndex</code> to
     * the <code>newIndex</code>
     */
    public void moveSound(int oldIndex, int newIndex) {
        Sound sound = sounds.get(oldIndex);
        removeSound(oldIndex);
        sounds.add(newIndex, sound);
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
