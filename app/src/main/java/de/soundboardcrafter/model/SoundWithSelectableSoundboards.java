package de.soundboardcrafter.model;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import de.soundboardcrafter.activity.sound.edit.common.SelectableSoundboard;

/**
 * Sound with all soundboards, of which some may be selected.
 */
public class SoundWithSelectableSoundboards implements Iterable<SelectableSoundboard> {
    private Sound sound;
    private List<SelectableSoundboard> soundboards;

    public SoundWithSelectableSoundboards(Sound sound, List<SelectableSoundboard> soundboards) {
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
        SoundWithSelectableSoundboards that = (SoundWithSelectableSoundboards) o;
        return Objects.equals(sound, that.sound) &&
                Objects.equals(soundboards, that.soundboards);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sound, soundboards);
    }

    @Override
    public String toString() {
        return "SoundWithSelectableSoundboards{" +
                "sound=" + sound +
                '}';
    }
}
