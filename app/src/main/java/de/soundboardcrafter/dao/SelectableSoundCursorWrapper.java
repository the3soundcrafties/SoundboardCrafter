package de.soundboardcrafter.dao;

import android.database.Cursor;
import android.database.CursorWrapper;

import java.util.UUID;

import de.soundboardcrafter.dao.DBSchema.SoundTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardSoundTable;
import de.soundboardcrafter.model.IAudioLocation;
import de.soundboardcrafter.model.SelectableModel;
import de.soundboardcrafter.model.Sound;

/**
 * Essentially a cursor over sounds that might or might not be linked to one specific soundboard.
 */
public class SelectableSoundCursorWrapper extends CursorWrapper {
    static String queryString() {
        return "SELECT s." + SoundTable.Cols.ID
                + ", s." + SoundTable.Cols.NAME
                + ", s." + SoundTable.Cols.LOCATION_TYPE
                + ", s." + SoundTable.Cols.PATH
                + ", s." + SoundTable.Cols.VOLUME_PERCENTAGE
                + ", s." + SoundTable.Cols.LOOP
                + ", sbs." + SoundboardSoundTable.Cols.SOUNDBOARD_ID + " "
                + "FROM " + SoundTable.NAME + " s "
                + "LEFT JOIN " + SoundboardSoundTable.NAME + " sbs "
                + "ON sbs." + SoundboardSoundTable.Cols.SOUND_ID
                + " = s." + SoundTable.Cols.ID + " "
                + "AND sbs." + SoundboardSoundTable.Cols.SOUNDBOARD_ID + " = ?";
    }

    static String[] selectionArgs(UUID soundboardId) {
        return new String[]{soundboardId.toString()};
    }

    SelectableSoundCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    SelectableModel<Sound> getSelectableSound() {
        Sound sound = getSound();
        boolean selected = !isNull(getColumnIndex(SoundboardSoundTable.Cols.SOUNDBOARD_ID));

        return new SelectableModel<>(sound, selected);
    }

    private Sound getSound() {
        final UUID uuid = UUID.fromString(getString(getColumnIndex(SoundTable.Cols.ID)));
        final String name = getString(getColumnIndex(SoundTable.Cols.NAME));
        final IAudioLocation location = getLocation();
        final int volumePercentage = getInt(getColumnIndex(SoundTable.Cols.VOLUME_PERCENTAGE));
        final boolean loop = getInt(getColumnIndex(SoundTable.Cols.LOOP)) != 0;

        return new Sound(uuid, location, name, volumePercentage, loop);
    }

    /**
     * Retrieves the sound's location (in the file system or the asset folder).
     */
    private IAudioLocation getLocation() {
        final SoundTable.LocationType locationType =
                SoundTable.LocationType.valueOf(
                        getString(getColumnIndex(SoundTable.Cols.LOCATION_TYPE)));
        final String path = getString(getColumnIndex(SoundTable.Cols.PATH));

        return SoundTable.toAudioLocation(locationType, path);
    }
}
