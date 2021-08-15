package de.soundboardcrafter.activity.soundboard.play;

import androidx.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import java.util.Objects;

import de.soundboardcrafter.model.SoundboardWithSounds;

/**
 * Data show in the {@link SoundboardPlayActivity}.
 */
class SoundboardPlayData {
    @Nullable
    private final String favoritesName;

    private final ImmutableList<SoundboardWithSounds> soundboards;

    public SoundboardPlayData(@Nullable String favoritesName,
                              ImmutableList<SoundboardWithSounds> soundboards) {
        this.favoritesName = favoritesName;
        this.soundboards = soundboards;
    }

    @Nullable
    public String getFavoritesName() {
        return favoritesName;
    }

    public ImmutableList<SoundboardWithSounds> getSoundboards() {
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
        SoundboardPlayData that = (SoundboardPlayData) o;
        return Objects.equals(favoritesName, that.favoritesName) &&
                soundboards.equals(that.soundboards);
    }

    @Override
    public int hashCode() {
        return Objects.hash(favoritesName, soundboards);
    }

    @Override
    public String toString() {
        return "SoundboardPlayData{" +
                "favoritesName='" + favoritesName + '\'' +
                ", " + soundboards.size() + " soundboard(s)}";
    }
}
