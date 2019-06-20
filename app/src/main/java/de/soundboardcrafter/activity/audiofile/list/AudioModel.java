package de.soundboardcrafter.activity.audiofile.list;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.Objects;

/**
 * An audio file data / metadata.
 */
class AudioModel {
    private final String path;

    @NonNull
    private final String name;

    private final String artist;
    private final Date dateAdded;
    private final
    long durationSecs;

    AudioModel(String path, @NonNull String name, String artist, Date dateAdded, long durationSecs) {
        this.path = path;
        this.name = name;
        this.artist = artist;
        this.dateAdded = dateAdded;
        this.durationSecs = durationSecs;
    }

    public String getPath() {
        return path;
    }

    @NonNull
    public String getName() {
        return name;
    }

    String getArtist() {
        return artist;
    }

    Date getDateAdded() {
        return dateAdded;
    }

    long getDurationSecs() {
        return durationSecs;
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
        return Objects.equals(path, that.path) &&
                Objects.equals(name, that.name) &&
                Objects.equals(artist, that.artist) &&
                Objects.equals(dateAdded, that.dateAdded) &&
                Objects.equals(durationSecs, that.durationSecs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, name, artist, dateAdded, durationSecs);
    }

    @Override
    @NonNull
    public String toString() {
        return "AudioModel{" +
                "path='" + path + '\'' +
                ", name='" + name + '\'' +
                ", artist='" + artist + '\'' +
                ", dateAdded='" + dateAdded + '\'' +
                ", durationSecs='" + durationSecs + '\'' +
                '}';
    }
}
