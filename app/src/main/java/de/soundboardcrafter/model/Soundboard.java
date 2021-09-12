package de.soundboardcrafter.model;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.text.CollationKey;
import java.util.Comparator;
import java.util.UUID;

import javax.annotation.Nonnull;

/**
 * A soundboard, that is a keyboard you can play sounds with.
 * The application supports several soundboards.
 * <p></p>
 * <code>Soundboards</code>s are not thread-safe. So it might be necessary to use
 * appropriate synchronization.
 */
public class Soundboard extends AbstractEntity {
    public static final Comparator<Soundboard> PROVIDED_LAST_THEN_BY_COLLATION_KEY =
            (one, other) -> {
                if (!one.isProvided() && other.isProvided()) {
                    return -1;
                }

                if (one.isProvided() && !other.isProvided()) {
                    return 1;
                }

                return one.collationKey.compareTo(other.collationKey);
            };

    private static final ThreadSafeCollator nameCollator =
            ThreadSafeCollator.getInstance();

    @NonNull
    private String name;

    /**
     * Whether the soundboard has been built automatically from provided sounds.
     * A <i>provided</i> soundboard cannot be deleted.
     */
    private final boolean provided;

    // CollationKeys may not be serializable
    @NonNull
    private transient CollationKey collationKey;

    public Soundboard(@NonNull String name, boolean provided) {
        this(UUID.randomUUID(), name, provided);
    }

    public Soundboard(UUID id, @NonNull String name, boolean provided) {
        super(id);
        setName(checkNotNull(name, "name is null"));
        this.provided = provided;
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

    public boolean isProvided() {
        return provided;
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
    public @Nonnull
    String toString() {
        return "Soundboard{" +
                "id=" + getId() +
                ", name='" + name +
                (provided ? " (provided)" : "") +
                '}';
    }
}
