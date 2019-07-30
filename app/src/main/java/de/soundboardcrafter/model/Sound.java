package de.soundboardcrafter.model;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.text.CollationKey;
import java.util.UUID;

import javax.annotation.Nonnull;

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
public class Sound extends AbstractEntity {
    private static final ThreadSafeCollator nameCollator =
            ThreadSafeCollator.getInstance();

    /**
     * Maximum volume percentage
     */
    // Changing this might need a database migration! -> DBHelper
    public static final int MAX_VOLUME_PERCENTAGE = 100;

    /**
     * Location of the audio file
     */
    @NonNull
    private final IAudioLocation audioLocation;

    /**
     * Display name
     */
    @NonNull
    private String name;

    // CollationKeys may not be serializable
    @NonNull
    private transient CollationKey collationKey;

    /**
     * Volume when the sound is played as a percentage. <code>100</code> is the original volume.
     */
    private int volumePercentage;

    /**
     * Whether the sound shall be played in a loop.
     */
    private boolean loop;

    public Sound(@NonNull IAudioLocation audioLocation, @NonNull String name) {
        this(UUID.randomUUID(), audioLocation, name, 100, false);
    }

    public Sound(UUID id, @NonNull IAudioLocation audioLocation, @NonNull String name, int volumePercentage, boolean loop) {
        super(id);
        this.audioLocation = checkNotNull(audioLocation, "audioLocation is null");
        setName(checkNotNull(name, "name is null"));
        setVolumePercentage(volumePercentage);
        this.loop = loop;
    }

    @NonNull
    public IAudioLocation getAudioLocation() {
        return audioLocation;
    }

    @NonNull
    public CollationKey getCollationKey() {
        return collationKey;
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

    public void setName(@NonNull String name) {
        checkNotNull(name, "name is null");

        this.name = name;
        setCollationKey();
    }

    private void setCollationKey() {
        collationKey = nameCollator.getCollationKey(getName());
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

    private void writeObject(final java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(final java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        setCollationKey();
    }

    @Override
    public @Nonnull
    String toString() {
        return "Sound{" +
                "id=" + getId() +
                ", audioLocation=" + audioLocation +
                ", name='" + name + '\'' +
                ", volumePercentage=" + volumePercentage +
                ", loop=" + loop +
                '}';
    }
}
