package de.soundboardcrafter.activity.audiofile.list;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class AudioLoader {
    ImmutableList<AudioModel> getAllAudioFromDevice(
            final Context context) {
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
                null,
                null,
                null)) {
            if (c != null) {
                while (c.moveToNext()) {

                    AudioModel audioModel = new AudioModel();

                    audioModel.setName(c.getString(1));
                    audioModel.setAlbum(c.getString(2));

                    int rawDateAdded = c.getInt(3);
                    audioModel.setDateAdded(new Date(rawDateAdded * 1000L));

                    audioModel.setArtist(c.getString(4));
                    audioModel.setPath(c.getString(0));

                    long durationMillis = c.getLong(5);
                    audioModel.setDurationSecs((long) Math.ceil(durationMillis / 1000f));

                    tempAudioList.add(audioModel);
                }
            }
        }

        return ImmutableList.copyOf(tempAudioList);
    }
}
