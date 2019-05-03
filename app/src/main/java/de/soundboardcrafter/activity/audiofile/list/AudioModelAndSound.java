package de.soundboardcrafter.activity.audiofile.list;

import java.util.Objects;

import androidx.annotation.Nullable;
import de.soundboardcrafter.model.Sound;

/**
 * Metadateien einer Audio-Datei, ggf. mit einem Sound.
 */
public class AudioModelAndSound {
    private AudioModel audioModel;
    private @Nullable
    Sound sound;

    public AudioModelAndSound(AudioModel audioModel) {
        this(audioModel, null);
    }

    AudioModelAndSound(AudioModel audioModel, @Nullable Sound sound) {
        this.audioModel = audioModel;
        this.sound = sound;
    }

    public AudioModel getAudioModel() {
        return audioModel;
    }

    @Nullable
    public Sound getSound() {
        return sound;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AudioModelAndSound that = (AudioModelAndSound) o;
        return Objects.equals(audioModel, that.audioModel) &&
                Objects.equals(sound, that.sound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(audioModel, sound);
    }

    @Override
    public String toString() {
        return "AudioModelAndSound{" +
                "audioModel=" + audioModel +
                ", sound=" + sound +
                '}';
    }
}
