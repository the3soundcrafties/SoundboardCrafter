package de.soundboardcrafter.model;

import java.io.Serializable;

/**
 * The location where an audio file may reside: In the file system
 * or in the assets folder
 * <p></p>
 * Every subclass needs to provide {@link #equals(Object)} and {@link #hashCode()}
 * as appropriate for a <i>value object</i>.
 */
public interface IAudioLocation extends Serializable {
    /**
     * Returns the name (not the full path).
     */
    String getName();
}
