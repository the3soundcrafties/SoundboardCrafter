package de.soundboardcrafter.model;

import java.util.UUID;

import javax.annotation.Nonnull;

import androidx.annotation.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A game to which a soundboard might be linked.
 * <p></p>
 * <code>Games</code>s are not thread-safe. So it might be necessary to use
 * appropriate synchronization.
 */
public class Game {
    @NonNull
    private final UUID id;
    @NonNull
    private String name;

    public Game(@NonNull String name) {
        id = UUID.randomUUID();
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
        return "Game{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
