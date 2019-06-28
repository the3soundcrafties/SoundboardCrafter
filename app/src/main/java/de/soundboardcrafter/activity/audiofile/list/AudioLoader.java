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
    /**
     * Loads all audio files and subfolders in a given folder - or loads all audio files
     * if no folder is given.
     *
     * @return The audio files and the subfolders (empty in the case of all audio files)
     */
    Pair<ImmutableList<AudioModel>, ImmutableList<AudioFolder>> getAudioFromDevice(
            final Context context, @Nullable String folder) {
        if (folder != null && !folder.endsWith("/")) {
            folder += "/";
        }

        final List<AudioModel> audioFileList = new ArrayList<>();
        final Map<String, Integer> subfoldersAndAudioFileCounts = new HashMap<>();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.AudioColumns.DATA,
                MediaStore.Audio.AudioColumns.TITLE,
                MediaStore.Audio.ArtistColumns.ARTIST,
                MediaStore.Audio.AudioColumns.DATE_ADDED,
                MediaStore.Audio.AudioColumns.DURATION};

        try (Cursor c = context.getContentResolver().query(uri,
                projection,
                folder != null ? MediaStore.Audio.Media.DATA + " like ? " : null,
                folder != null ? new String[]{folder + "%"} : null,
                null)) {
            if (c != null) {
                while (c.moveToNext()) {
                    String path = c.getString(0);
                    if (folder == null || isInFolder(path, folder)) {
                        audioFileList.add(createAudioModel(
                                path, c.getString(1), c.getString(2),
                                c.getInt(3), c.getLong(4)));
                    }

                    if (folder != null) {
                        incrementSubfolderAudioCountIfIsDescendant(folder,
                                subfoldersAndAudioFileCounts, path);
                    }
                }
            }
        }


        return toAudioFilesAndSubFolders(audioFileList, subfoldersAndAudioFileCounts);
    }

    private AudioModel createAudioModel(String path, String name, String artist, int dateAddedMillis, long durationMillis) {
        return new AudioModel(path,
                name,
                artist,
                new Date(dateAddedMillis * 1000L),
                (long) Math.ceil(durationMillis / 1000f));
    }

    /**
     * Checks whether the <code>audioFilePath</code> is in a subfolder of the folder - if it is,
     * add the folder to the map - or increment the count held in the map.
     */
    private void incrementSubfolderAudioCountIfIsDescendant(
            @NonNull String folder, @NonNull Map<String, Integer> subfoldersAndAudioFileCounts,
            @NonNull String audioFilePath) {
        for (String ancestor : calcAncestorFolders(audioFilePath)) {
            if (isInFolder(ancestor, folder)) {
                Integer old = subfoldersAndAudioFileCounts.get(ancestor);
                if (old == null) {
                    subfoldersAndAudioFileCounts.put(ancestor, 1);
                } else {
                    subfoldersAndAudioFileCounts.put(ancestor, old + 1);
                }
            }
        }
    }

    /**
     * Converts a list of audio files and some subfolders with the number of
     * audio file contained therein to a {@link Pair} of audio files and
     * {@link AudioFolder}s.
     */
    private Pair<ImmutableList<AudioModel>, ImmutableList<AudioFolder>> toAudioFilesAndSubFolders(
            List<AudioModel> audioListIn, Map<String, Integer> audioFolderMapIn) {
        ImmutableList.Builder<AudioFolder> audioFolders = ImmutableList.builder();
        for (Map.Entry<String, Integer> subfolderAndCount : audioFolderMapIn.entrySet()) {
            audioFolders.add(new AudioFolder(subfolderAndCount.getKey(), subfolderAndCount.getValue()));
        }

        return Pair.create(ImmutableList.copyOf(audioListIn), audioFolders.build());
    }

    private ImmutableList<String> calcAncestorFolders(String path) {
        ImmutableList.Builder<String> res = ImmutableList.builder();

        String[] pathElements = path.split("/");

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

        return path.indexOf("/", folder.length()) < 0;
    }
}
