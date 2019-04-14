package de.soundboardcrafter.activity.audiofile.list;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

class AudioLoader {
    ImmutableList<AudioModel> getAllAudioFromDevice(final Context context) {

        final List<AudioModel> tempAudioList = new ArrayList<>();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.AudioColumns.DATA,
                MediaStore.Audio.AudioColumns.TITLE,
                MediaStore.Audio.AudioColumns.ALBUM, MediaStore.Audio.ArtistColumns.ARTIST,};
//        Cursor c = context.getContentResolver().query(uri, projection, MediaStore.Audio.Media.DATA + " like ? ", new String[]{"%yourFolderName%"}, null);
        Cursor c = context.getContentResolver().query(uri,
                projection,
                null,
                null,
                null);
        if (c != null) {
            while (c.moveToNext()) {

                AudioModel audioModel = new AudioModel();
                String path = c.getString(0);
                String name = c.getString(1);
                String album = c.getString(2);
                String artist = c.getString(3);

                audioModel.setName(name);
                audioModel.setAlbum(album);
                audioModel.setArtist(artist);
                audioModel.setPath(path);

                Log.d("Name :" + name, " Album :" + album);
                Log.d("Path :" + path, " Artist :" + artist);

                tempAudioList.add(audioModel);
            }
            c.close();
        }

        return ImmutableList.copyOf(tempAudioList);
    }
}
