package de.soundboardcrafter.model;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractEntity implements Serializable {
    private final @NonNull
    UUID id;

    AbstractEntity(UUID id) {
        this.id = checkNotNull(id, "id is null");
    }

    @Nonnull
    public UUID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractEntity that = (AbstractEntity) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
