package de.soundboardcrafter.model;

import java.io.Serializable;
import java.util.UUID;

import javax.annotation.Nonnull;

import androidx.annotation.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A soundboard, that is a keyboard you can play sounds with.
 * The application supports several soundboards.
 * <p></p>
 * <code>Soundboards</code>s are not thread-safe. So it might be necessary to use
 * appropriate synchronization.
 */
public class Soundboard implements Serializable {
    private final @NonNull
    UUID id;
    private @NonNull
    String name;

    public Soundboard(@NonNull String name) {
        this(UUID.randomUUID(), name);
    }

    public Soundboard(UUID id, @NonNull String name) {
        this.id = checkNotNull(id, "id is null");
        this.name = checkNotNull(name, "name is null");
    }

    public @NonNull
    UUID getId() {
        return id;
    }

    public @NonNull
    String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        checkNotNull(name, "name is null");

        this.name = name;
    }

    @Override
    public @Nonnull
    String toString() {
        return "Soundboard{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
