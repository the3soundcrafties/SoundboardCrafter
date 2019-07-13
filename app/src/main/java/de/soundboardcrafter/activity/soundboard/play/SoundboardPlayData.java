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
    private final String gameName;

    private final ImmutableList<SoundboardWithSounds> soundboards;

    public SoundboardPlayData(@Nullable String gameName, ImmutableList<SoundboardWithSounds> soundboards) {
        this.gameName = gameName;
        this.soundboards = soundboards;
    }

    @Nullable
    public String getGameName() {
        return gameName;
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
        return Objects.equals(gameName, that.gameName) &&
                soundboards.equals(that.soundboards);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameName, soundboards);
    }

    @Override
    public String toString() {
        return "SoundboardPlayData{" +
                "gameName='" + gameName + '\'' +
                ", " + soundboards.size() + " soundboard(s)}";
    }
}
