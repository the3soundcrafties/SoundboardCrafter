package de.soundboardcrafter.activity.audiofile.list;

/**
 * An audio file data / metadata.
 */
public class AudioModel {
    private String path;
    private String name;
    private String album;
    private String artist;

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
}
