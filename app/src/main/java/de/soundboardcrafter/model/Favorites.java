package de.soundboardcrafter.model;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.text.CollationKey;
import java.util.UUID;

import javax.annotation.Nonnull;

/**
 * A single favorites instance to which a soundboard might be linked.
 * <p></p>
 * <code>Favorites</code> are not thread-safe. So it might be necessary to use
 * appropriate synchronization.
 */
public class Favorites extends AbstractEntity {
    private static final ThreadSafeCollator nameCollator =
            ThreadSafeCollator.getInstance();

    @NonNull
    private String name;

    // May not be serializable.
    @NonNull
    private transient CollationKey collationKey;

    Favorites(@NonNull String name) {
        this(UUID.randomUUID(), name);
    }


    public Favorites(@Nonnull UUID id, String name) {
        super(id);
        setName(checkNotNull(name, "name is null"));
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
        return "Favorites{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                '}';
    }
}
