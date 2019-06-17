package de.soundboardcrafter.activity.audiofile.list;

import androidx.annotation.Nullable;

import java.util.Date;
import java.util.Objects;

import de.soundboardcrafter.model.Sound;

/**
 * Meta data of an audio file, perhaps with a sound.
 */
class AudioModelAndSound extends AbstractAudioFolderEntry {
    private AudioModel audioModel;
    private @Nullable
    Sound sound;

    AudioModelAndSound(AudioModel audioModel, @Nullable Sound sound) {
        this.audioModel = audioModel;
        this.sound = sound;
    }

    AudioModel getAudioModel() {
        return audioModel;
    }

    @Nullable
    public String getName() {
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
    public String toString() {
        return "AudioModelAndSound{" +
                "audioModel=" + audioModel +
                ", sound=" + sound +
                '}';
    }
}
