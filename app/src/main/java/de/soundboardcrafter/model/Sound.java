package de.soundboardcrafter.model;

import java.io.Serializable;
import java.util.UUID;

import javax.annotation.Nonnull;

import androidx.annotation.NonNull;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A sound that could be played from a soundboard.
 * <p></p>
 * A <code>Sound</code> can be a single short event or a long,
 * ever-repeated song.
 * <p></p>
 * <code>Sound</code>s are basically links to some audio file on the device.
 * <p></p>
 * <code>Sound</code>s are not thread-safe. So it might be necessary to use
 * appropriate synchronization.
 */
public class Sound implements Serializable {
    /**
     * Maximum volume percentage
     */
    // Changing this might need a database migration! -> DBHelper
    public static final int MAX_VOLUME_PERCENTAGE = 100;

    private final @NonNull
    UUID id;

    /**
     * Path to the audio file
     */
    @NonNull
    private String path;

    /**
     * Display name
     */
    private @NonNull
    String name;

    /**
     * Volume when the sound is played as a percentage. <code>100</code> is the original volume.
     */
    private int volumePercentage;

    /**
     * Whether the sound shall be played in a loop.
     */
    private boolean loop;

    public Sound(@NonNull String path, @NonNull String name, int volumePercentage, boolean loop) {
        this(UUID.randomUUID(), path, name, volumePercentage, loop);
    }

    public Sound(UUID id, @NonNull String path, @NonNull String name, int volumePercentage, boolean loop) {
        this.id = checkNotNull(id, "id is null");
        this.path = checkNotNull(path, "path is null");
        this.name = checkNotNull(name, "name is null");
        setVolumePercentage(volumePercentage);
        this.loop = loop;
    }

    public @NonNull
    UUID getId() {
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

    public int getVolumePercentage() {
        return volumePercentage;
    }

    public void setVolumePercentage(int volumePercentage) {
        checkArgument(volumePercentage >= 0, "volumePercentage < 0");
        checkArgument(volumePercentage <= MAX_VOLUME_PERCENTAGE,
                "volumePercentage > " + MAX_VOLUME_PERCENTAGE);

        this.volumePercentage = volumePercentage;
    }

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    @Override
    public @Nonnull
    String toString() {
        return "Sound{" +
                "id=" + id +
                ", path=" + path +
                ", name='" + name + '\'' +
                ", volumePercentage=" + volumePercentage +
                ", loop=" + loop +
                '}';
    }
}
