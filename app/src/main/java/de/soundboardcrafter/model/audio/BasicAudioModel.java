package de.soundboardcrafter.model.audio;

import androidx.annotation.NonNull;

import java.text.CollationKey;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import de.soundboardcrafter.model.IAudioLocation;
import de.soundboardcrafter.model.ThreadSafeCollator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Basic audio file data / metadata: name and location.
 */
@ParametersAreNonnullByDefault
public class BasicAudioModel {
    private static final ThreadSafeCollator nameCollator =
            ThreadSafeCollator.getInstance();
    @NonNull
    private final IAudioLocation audioLocation;
    @NonNull
    protected final String name;
    // May not be seriablizable.
    @NonNull
    private transient CollationKey collationKey;

    public BasicAudioModel(IAudioLocation audioLocation, String name) {
        this.audioLocation = checkNotNull(audioLocation, "audioLocation is null");
        this.name = checkNotNull(name, "name is null");

        setCollationKey();
    }

    @NonNull
    public IAudioLocation getAudioLocation() {
        return audioLocation;
    }

    /**
     * Returns the name.
     * <p></p>
     * For  sorting purposes better use {@link #getCollationKey()}.
     */
    @NonNull
    public String getName() {
        return name;
    }

    final void setCollationKey() {
        collationKey = nameCollator.getCollationKey(getName());
    }

    @NonNull
    public CollationKey getCollationKey() {
        return collationKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BasicAudioModel that = (BasicAudioModel) o;
        return audioLocation.equals(that.audioLocation) &&
                name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    @Nonnull
    public String toString() {
        return "BasicAudioModel{" +
                "audioLocation=" + audioLocation +
                ", name='" + name + '\'' +
                '}';
    }
}
