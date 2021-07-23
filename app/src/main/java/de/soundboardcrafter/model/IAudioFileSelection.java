package de.soundboardcrafter.model;

import java.io.Serializable;

/**
 * A selection for audio files:
 * <ul>
 * <li>All files from the file system
 * <li>Only files from a certain folder in the file system
 * <li>Only files from a certain folder in the assets
 * </ul>>
 * <p/>
 * Every subclass needs to provide {@link #equals(Object)} and {@link #hashCode()}
 * as appropriate for a <i>value object</i>.
 */
public interface IAudioFileSelection extends Serializable {
}
