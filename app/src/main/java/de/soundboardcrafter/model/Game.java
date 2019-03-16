package de.soundboardcrafter.model;

import java.util.UUID;
import java.util.zip.Checksum;

import androidx.annotation.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A game to which a soundboard might be linked.
 */
public class Game {
    private final @NonNull UUID id;
    private @NonNull String name;

    public Game(@NonNull String name) {
        checkNotNull(name, "name is null");

        id = UUID.randomUUID();
        this.name = name;
    }

    public @NonNull UUID getId() {
        return id;
    }

    public @NonNull String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        checkNotNull(name, "name is null");

        this.name = name;
    }

    @Override
    public String toString() {
        return "Game{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
