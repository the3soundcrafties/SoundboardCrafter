package de.soundboardcrafter.activity.audiofile.list;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AudioLoader {
    Pair<ImmutableList<AudioModel>, ImmutableList<AudioFolder>> getAudioFromDevice(
            final Context context, @Nullable String folder) {
        if (folder != null && !folder.endsWith("/")) {
            folder += "/";
        }

        final List<AudioModel> tempAudioList = new ArrayList<>();
        final Map<String, Integer> tempAudioMap = new HashMap<>();

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

                    if (folder != null) {
                        for (String ancestor : calcAncestorFolders(path)) {
                            if (isInFolder(ancestor, folder)) {
                                Integer old = tempAudioMap.get(ancestor);
                                if (old == null) {
                                    tempAudioMap.put(ancestor, 1);
                                } else {
                                    tempAudioMap.put(ancestor, old + 1);
                                }
                            }
                        }
                    }
                }
            }
        }


        ImmutableList.Builder audioFolders = ImmutableList.builder();
        if (folder != null) {
            for (Map.Entry<String, Integer> pathAndCount : tempAudioMap.entrySet()) {
                audioFolders.add(new AudioFolder(pathAndCount.getKey(), pathAndCount.getValue()));
            }
        }

        return Pair.create(ImmutableList.copyOf(tempAudioList), audioFolders.build());
    }

    private ImmutableList<String> calcAncestorFolders(String path) {
        ImmutableList.Builder<String> res = ImmutableList.builder();

        String[] pathElements = path.split("\\/");

        res.add("/");
        String ancestor = "";
        for (int i = 1; i < pathElements.length - 1; i++) {
            ancestor = ancestor + "/" + pathElements[i];
            res.add(ancestor);
        }

        return res.build();
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

        if (path.equals(folder)) {
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
