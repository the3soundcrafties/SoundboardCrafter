package de.soundboardcrafter.dao;

import android.database.Cursor;

import androidx.annotation.WorkerThread;

import java.util.UUID;

import de.soundboardcrafter.dao.DBSchema.SoundTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardSoundTable;
import de.soundboardcrafter.model.SelectableModel;
import de.soundboardcrafter.model.Sound;

/**
 * Essentially a cursor over sounds that might or might not be linked to one specific soundboard.
 */
@WorkerThread
class SelectableSoundCursorWrapper extends AbstractSimpleSoundCursorWrapper {
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
        boolean selected = !isNull(getColumnIndex(SoundboardSoundTable.Cols.SOUNDBOARD_ID));

        return new SelectableModel<>(getSound(), selected);
    }
}
