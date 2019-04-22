package de.soundboardcrafter.activity.sound.edit.common;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import de.soundboardcrafter.model.Sound;

/**
 * Model for editing a sound - consists of the sound itself and the
 * soundboard of which some might be selected.
 */
class SoundEditModel implements Iterable<SelectableSoundboard> {
    private Sound sound;
    private List<SelectableSoundboard> soundboards;

    public SoundEditModel(Sound sound, List<SelectableSoundboard> soundboards) {
        this.sound = sound;
        this.soundboards = soundboards;
    }

    public Sound getSound() {
        return sound;
    }

    @NonNull
    @Override
    public Iterator<SelectableSoundboard> iterator() {
        return soundboards.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SoundEditModel that = (SoundEditModel) o;
        return Objects.equals(sound, that.sound) &&
                Objects.equals(soundboards, that.soundboards);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sound, soundboards);
    }

    @Override
    public String toString() {
        return "SoundEditModel{" +
                "sound=" + sound +
                '}';
    }
}
