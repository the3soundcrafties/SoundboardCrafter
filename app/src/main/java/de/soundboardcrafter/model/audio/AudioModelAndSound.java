package de.soundboardcrafter.model.audio;

import androidx.annotation.NonNull;

import java.text.CollationKey;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

import de.soundboardcrafter.model.Sound;

/**
 * Metadata of an audio file, perhaps with a sound.
 */
public class AudioModelAndSound extends AbstractAudioFolderEntry {
    public enum SortOrder {
        BY_NAME(Comparator.comparing(AudioModelAndSound::getCollationKey)),
        BY_DATE(Comparator.comparing(AudioModelAndSound::getDateAdded,
                Comparator.nullsFirst(Comparator.reverseOrder())));

        private final Comparator<AudioModelAndSound> comparator;

        SortOrder(Comparator<AudioModelAndSound> comparator) {
            this.comparator = comparator;
        }

        Comparator<AudioModelAndSound> getComparator() {
            return comparator;
        }
    }

    private final FullAudioModel audioModel;

    @Nullable
    private final Sound sound;

    public AudioModelAndSound(FullAudioModel audioModel, @Nullable Sound sound) {
        this.audioModel = audioModel;
        this.sound = sound;
    }

    @NonNull
    public FullAudioModel getAudioModel() {
        return audioModel;
    }

    @NonNull
    public CollationKey getCollationKey() {
        // This only works because AudioModel and Sound internally use
        // the same Collator object.
        // (I assume, that the user does not fiddle with his
        // location settings while using the app.)

        if (sound == null) {
            return audioModel.getCollationKey();
        }

        return sound.getCollationKey();

    }

    /**
     * Returns the name of the sound - or the audio name.
     * <p></p>
     * For  sorting purposes better use {@link #getCollationKey()}.
     */
    @NonNull
    public final String getName() {
        if (sound == null) {
            return audioModel.getName();
        }

        return sound.getName();
    }


    @Nullable
    public UUID getSoundId() {
        if (sound == null) {
            return null;
        }

        return sound.getId();
    }

    @Nullable
    public Sound getSound() {
        return sound;
    }

    @Nullable
    public Date getDateAdded() {
        return audioModel.getDateAdded();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        AudioModelAndSound that = (AudioModelAndSound) o;
        return Objects.equals(audioModel, that.audioModel) &&
                Objects.equals(sound, that.sound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), audioModel, sound);
    }

    @Override
    @NonNull
    public String toString() {
        return "AudioModelAndSound{" +
                "audioModel=" + audioModel +
                ", sound=" + sound +
                '}';
    }
}
