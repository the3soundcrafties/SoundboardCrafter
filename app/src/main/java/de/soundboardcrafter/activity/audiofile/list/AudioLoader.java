package de.soundboardcrafter.activity.audiofile.list;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class AudioLoader {
    ImmutableList<AudioModel> getAudioFromDevice(
            final Context context, @Nullable String folder) {
        if (folder != null && !folder.endsWith("/")) {
            folder += "/";
        }

        final List<AudioModel> tempAudioList = new ArrayList<>();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.AudioColumns.DATA,
                MediaStore.Audio.AudioColumns.TITLE,
                MediaStore.Audio.AudioColumns.ALBUM,
                MediaStore.Audio.AudioColumns.DATE_ADDED,
                MediaStore.Audio.ArtistColumns.ARTIST,
                MediaStore.Audio.AudioColumns.DURATION};

//        Cursor c = context.getContentResolver().query(uri, projection, MediaStore.Audio.Media.DATA + " like ? ", new String[]{"%yourFolderName%"}, null);
        try (Cursor c = context.getContentResolver().query(uri,
                projection,
                folder != null ? MediaStore.Audio.Media.DATA + " like ? " : null,
                folder != null ? new String[]{folder + "%"} : null,
                null)) {
            if (c != null) {
                while (c.moveToNext()) {

                    String path = c.getString(0);
                    if (folder == null || isInFolder(path, folder)) {
                        AudioModel audioModel = new AudioModel();

                        audioModel.setPath(path);
                        audioModel.setName(c.getString(1));
                        audioModel.setAlbum(c.getString(2));

                        int rawDateAdded = c.getInt(3);
                        audioModel.setDateAdded(new Date(rawDateAdded * 1000L));

                        audioModel.setArtist(c.getString(4));

                        long durationMillis = c.getLong(5);
                        audioModel.setDurationSecs((long) Math.ceil(durationMillis / 1000f));

                        tempAudioList.add(audioModel);
                    }
                }
            }
        }

        return ImmutableList.copyOf(tempAudioList);
    }

    /**
     * Checks whether this <code>path</code> is in this <code>folder</code>
     * (not in a sub-folder).
     */
    private boolean isInFolder(@NonNull String path, @NonNull String folder) {
        if (!path.startsWith(folder)) {
            // /other/stuff
            return false;
        }

        if (path.indexOf("/", folder.length()) >= 0) {
            // /the/folder/subfolder/stuff
            return false;
        }

        // /the/folder/stuff
        return true;
    }
}
