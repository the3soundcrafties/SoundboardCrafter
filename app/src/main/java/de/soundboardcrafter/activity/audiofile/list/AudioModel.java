package de.soundboardcrafter.activity.audiofile.list;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.text.CollationKey;
import java.util.Date;
import java.util.Objects;

import javax.annotation.Nullable;

import de.soundboardcrafter.model.IAudioLocation;
import de.soundboardcrafter.model.ThreadSafeCollator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An audio file data / metadata.
 */
class AudioModel {
    private static final ThreadSafeCollator nameCollator =
            ThreadSafeCollator.getInstance();

    @NonNull
    private final IAudioLocation audioLocation;

    @NonNull
    private final String name;

    // May not be seriablizable.
    @NonNull
    private transient CollationKey collationKey;

    private final String artist;

    @Nullable
    private final Date dateAdded;

    private final long durationSecs;

    AudioModel(@NonNull IAudioLocation audioLocation, @NonNull String name, String artist,
               long durationSecs) {
        this(audioLocation, name, artist, null, durationSecs);
    }

    AudioModel(@NonNull IAudioLocation audioLocation, @NonNull String name, String artist,
               @Nullable Date dateAdded, long durationSecs) {
        this.audioLocation = checkNotNull(audioLocation, "audioLocation is null");
        this.name = checkNotNull(name, "name is null");
        this.artist = artist;
        this.dateAdded = dateAdded;
        this.durationSecs = durationSecs;

        setCollationKey();
    }

    private void setCollationKey() {
        collationKey = nameCollator.getCollationKey(getName());
    }

    @NonNull
    IAudioLocation getAudioLocation() {
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

    @NonNull
    public CollationKey getCollationKey() {
        return collationKey;
    }

    String getArtist() {
        return artist;
    }

    @Nullable
    Date getDateAdded() {
        return dateAdded;
    }

    long getDurationSecs() {
        return durationSecs;
    }

    private void writeObject(final java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(final java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        setCollationKey();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AudioModel that = (AudioModel) o;
        return Objects.equals(audioLocation, that.audioLocation) &&
                Objects.equals(name, that.name) &&
                Objects.equals(artist, that.artist) &&
                Objects.equals(dateAdded, that.dateAdded) &&
                Objects.equals(durationSecs, that.durationSecs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(audioLocation, name, artist, dateAdded, durationSecs);
    }

    @Override
    @NonNull
    public String toString() {
        return "AudioModel{" +
                "audioLocation='" + audioLocation + '\'' +
                ", name='" + name + '\'' +
                ", artist='" + artist + '\'' +
                ", dateAdded='" + dateAdded + '\'' +
                ", durationSecs='" + durationSecs + '\'' +
                '}';
    }
}
