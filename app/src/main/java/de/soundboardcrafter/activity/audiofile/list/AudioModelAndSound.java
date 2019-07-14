package de.soundboardcrafter.activity.audiofile.list;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.CollationKey;
import java.util.Date;
import java.util.Objects;

import de.soundboardcrafter.model.Sound;

/**
 * Meta data of an audio file, perhaps with a sound.
 */
class AudioModelAndSound extends AbstractAudioFolderEntry {
    private final AudioModel audioModel;

    @Nullable
    private final Sound sound;

    AudioModelAndSound(AudioModel audioModel, @Nullable Sound sound) {
        this.audioModel = audioModel;
        this.sound = sound;
    }

    @NonNull
    AudioModel getAudioModel() {
        return audioModel;
    }

    @NonNull
    CollationKey getCollationKey() {
        // This only works because AudioModel and Sound internally use
        // the same Collator object.
        // (I am assuming, that the user does not fiddle with his
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
    public Sound getSound() {
        return sound;
    }

    Date getDateAdded() {
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
