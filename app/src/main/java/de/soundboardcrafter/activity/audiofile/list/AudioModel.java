package de.soundboardcrafter.activity.audiofile.list;

import java.util.Date;
import java.util.Objects;

/**
 * An audio file data / metadata.
 */
public class AudioModel {
    private String path;
    private String name;
    private String album;
    private String artist;
    private Date dateAdded;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlbum() {
        return album;
    }

    void setAlbum(String album) {
        this.album = album;
    }

    String getArtist() {
        return artist;
    }

    void setArtist(String artist) {
        this.artist = artist;
    }

    Date getDateAdded() {
        return dateAdded;
    }

    void setDateAdded(Date dateAdded) {
        this.dateAdded = dateAdded;
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
                Objects.equals(album, that.album) &&
                Objects.equals(artist, that.artist) &&
                Objects.equals(dateAdded, that.dateAdded);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, name, album, artist, dateAdded);
    }

    @Override
    public String toString() {
        return "AudioModel{" +
                "path='" + path + '\'' +
                ", name='" + name + '\'' +
                ", album='" + album + '\'' +
                ", artist='" + artist + '\'' +
                ", dateAdded='" + dateAdded + '\'' +
                '}';
    }
}
