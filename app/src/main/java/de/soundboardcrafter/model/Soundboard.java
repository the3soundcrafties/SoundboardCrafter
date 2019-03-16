package de.soundboardcrafter.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import androidx.annotation.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A soundboard, that is a keyboard you can play sounds with.
 * The application supports several soundboards.
 */
public class Soundboard {
    private final @NonNull UUID id;
    private @NonNull
    String name;

    /**
     * The sounds on their position on the <code>Soundboard</code>.
     * Sounds can be shared between soundboards, and there are no empty
     * positions.
     */
    private ArrayList<Sound> sounds;

    public Soundboard(@NonNull String name, @NonNull ArrayList<Sound> sounds) {
        id = UUID.randomUUID();
        this.name =  checkNotNull(name, "name is null");;
        this.sounds = checkNotNull(sounds, "sound is null");
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

    /**
     * Returns the sounds in order, unmodifiable.
     */
    public List<Sound> getSounds() {
        return Collections.unmodifiableList(sounds);
    }

    @Override
    public String toString() {
        return "Soundboard{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
