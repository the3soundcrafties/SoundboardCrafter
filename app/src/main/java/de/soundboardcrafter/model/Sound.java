package de.soundboardcrafter.model;

import com.google.common.base.Preconditions;

import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;

import androidx.annotation.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A sound that could be played from a soundboard.
 * <p></p>
 * A <code>Sound</code> can be a single short event or a long,
 * ever-repeated song.
 * <p></p>
 * <code>Sound</code>s are basically links to some audio file on the device.
 */
public class Sound {
    private final @NonNull UUID id;

    /**
     * Path to the audio file
     */
    @NonNull private String path;

    /**
     * Display name
     */
    private @NonNull String name;

    /**
     * Volume when the sound is played. <code>1.0</code> is the original volume.
     */
    private float relativeVolume;

    /**
     * Whether the sound shall be played in a loop.
     */
    private boolean loop;

    public Sound(@NonNull String path, @NonNull String name, float relativeVolume, boolean loop) {
        id = UUID.randomUUID();
        this.path = checkNotNull(path, "path is null");
        this.name = checkNotNull(name, "name is null");
        this.relativeVolume = relativeVolume;
        this.loop = loop;
    }

    public @NonNull UUID getId() {
        return id;
    }

    @NonNull
    public String getPath() {
        return path;
    }

    public void setPath(@NonNull String path) {
        checkNotNull(path,
                "path is null");
        this.path = path;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        checkNotNull(path,
                "path is null");

        this.name = name;
    }

    public float getRelativeVolume() {
        return relativeVolume;
    }

    public void setRelativeVolume(float relativeVolume) {
        this.relativeVolume = relativeVolume;
    }

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    @Override
    public String toString() {
        return "Sound{" +
                "id=" + id +
                ", path=" + path +
                ", name='" + name + '\'' +
                ", relativeVolume=" + relativeVolume +
                ", loop=" + loop +
                '}';
    }
}
