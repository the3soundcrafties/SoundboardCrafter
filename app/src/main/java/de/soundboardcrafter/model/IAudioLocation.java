package de.soundboardcrafter.model;

/**
 * A certain folder where an audio file may reside:
 * <ul>
 * <li>In a certain folder in the file system
 * <li>In a certain folder in the assets
 * </ul>>
 * <p/>
 * Every subclass needs to provide {@link #equals(Object)} and {@link #hashCode()}
 * as appropriate for a <i>value object</i>.
 */
public interface IAudioLocation extends IAudioFileSelection {
    /**
     * Returns the display name (not necessary the full path).
     */
    String getDisplayName();
}
