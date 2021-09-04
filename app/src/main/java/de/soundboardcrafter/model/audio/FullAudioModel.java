package de.soundboardcrafter.model.audio;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import de.soundboardcrafter.model.AbstractAudioLocation;

/**
 * Full audio file data / metadata, including during, e.g.
 */
@ParametersAreNonnullByDefault
public
class FullAudioModel extends BasicAudioModel {

    @Nullable
    private final String artist;

    @Nullable
    private final Date dateAdded;

    private final long durationSecs;

    public FullAudioModel(AbstractAudioLocation audioLocation, String name,
                          @Nullable String artist, long durationSecs) {
        this(audioLocation, name, artist, null, durationSecs);
    }

    public FullAudioModel(AbstractAudioLocation audioLocation, String name, @Nullable String artist,
                          @Nullable Date dateAdded, long durationSecs) {
        super(audioLocation, name);
        this.artist = artist;
        this.dateAdded = dateAdded;
        this.durationSecs = durationSecs;
    }

    @Nullable
    public String getArtist() {
        return artist;
    }

    @Nullable
    Date getDateAdded() {
        return dateAdded;
    }

    public long getDurationSecs() {
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
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        FullAudioModel that = (FullAudioModel) o;
        return durationSecs == that.durationSecs &&
                Objects.equals(artist, that.artist) &&
                Objects.equals(dateAdded, that.dateAdded);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), artist);
    }

    @Override
    @Nonnull
    public String toString() {
        return "FullAudioModel{" +
                "artist='" + artist + '\'' +
                ", dateAdded=" + dateAdded +
                ", durationSecs=" + durationSecs +
                ", name='" + name + '\'' +
                '}';
    }
}
