package de.soundboardcrafter.activity.common.audioloader;

import static com.google.common.base.Preconditions.checkNotNull;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.google.common.collect.ImmutableList;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import de.soundboardcrafter.model.FileSystemFolderAudioLocation;
import de.soundboardcrafter.model.audio.AudioFolder;
import de.soundboardcrafter.model.audio.FullAudioModel;

/**
 * Loader for audio files, can load audio files from the device.
 *
 * @see AssetsAudioLoader
 */
class FileSystemAudioLoader {

    /**
     * Loads all audio files from the device.
     */
    @RequiresPermission("android.permission.READ_EXTERNAL_STORAGE")
    ImmutableList<FullAudioModel> getAudiosFromDevice(Context context) {
        final ImmutableList.Builder<FullAudioModel> res = ImmutableList.builder();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        // MediaStore.Audio.AudioColumns.DURATION has "always" been there and
        // works fine
        @SuppressLint("InlinedApi") String[] projection = {MediaStore.Audio.AudioColumns.DATA,
                MediaStore.Audio.AudioColumns.TITLE,
                MediaStore.Audio.ArtistColumns.ARTIST,
                MediaStore.Audio.AudioColumns.DATE_ADDED,
                MediaStore.Audio.AudioColumns.DURATION};

        try (Cursor c = context.getContentResolver().query(uri,
                projection, null, null, null)) {
            if (c != null) {
                while (c.moveToNext()) {
                    String path = c.getString(0);
                    res.add(createAudioModelOnDevice(
                            path, c.getString(1), c.getString(2),
                            c.getInt(3), c.getLong(4)));
                }
            }
        }

        return res.build();
    }

    FullAudioModel getAudioFromDevice(Context context, String path) {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        // MediaStore.Audio.AudioColumns.DURATION has "always" been there and
        // works fine
        @SuppressLint("InlinedApi") String[] projection = {MediaStore.Audio.AudioColumns.DATA,
                MediaStore.Audio.AudioColumns.TITLE,
                MediaStore.Audio.ArtistColumns.ARTIST,
                MediaStore.Audio.AudioColumns.DATE_ADDED,
                MediaStore.Audio.AudioColumns.DURATION};

        try (Cursor c = context.getContentResolver().query(uri,
                projection,
                MediaStore.Audio.Media.DATA + " = ? ",
                new String[]{path}, null)) {
            if (c != null) {
                if (!c.moveToNext()) {
                    return null;
                }

                return createAudioModelOnDevice(
                        path, c.getString(1), c.getString(2),
                        c.getInt(3), c.getLong(4));
            }
        }

        return null;
    }

    /**
     * Loads all audio files and subFolders in a given folder <i>on the device</i>.
     *
     * @return The audio files and the subFolders
     */
    @RequiresPermission("android.permission.READ_EXTERNAL_STORAGE")
    Pair<ImmutableList<FullAudioModel>, ImmutableList<AudioFolder>> getAudiosFromDevice(
            final Context context, @Nonnull String folder) {
        checkNotNull(folder, "folder was null");

        if (!folder.endsWith("/")) {
            folder += "/";
        }

        final List<FullAudioModel> audioFileList = new ArrayList<>();
        final Map<String, Integer> subFoldersAndAudioFileCounts = new HashMap<>();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        // DURATION has "always" been there and is working fine
        @SuppressLint("InlinedApi") String[] projection = {MediaStore.Audio.AudioColumns.DATA,
                MediaStore.Audio.AudioColumns.TITLE,
                MediaStore.Audio.ArtistColumns.ARTIST,
                MediaStore.Audio.AudioColumns.DATE_ADDED,
                MediaStore.Audio.AudioColumns.DURATION};

        try (Cursor c = context.getContentResolver().query(uri,
                projection,
                MediaStore.Audio.Media.DATA + " like ? ",
                new String[]{folder + "%"},
                null)) {
            if (c != null) {
                while (c.moveToNext()) {
                    String path = c.getString(0);
                    if (AudioLoaderUtil.isInFolder(path, folder)) {
                        audioFileList.add(createAudioModelOnDevice(
                                path, c.getString(1), c.getString(2),
                                c.getInt(3), c.getLong(4)));
                    }

                    incrementSubfolderAudioCountIfIsDescendant(folder,
                            subFoldersAndAudioFileCounts, path);
                }
            }
        }


        return toAudioFilesAndSubFoldersFromDevice(audioFileList, subFoldersAndAudioFileCounts);
    }

    /**
     * Checks whether the <code>audioFilePath</code> is in a subfolder of the folder - if it is,
     * add the folder to the map - or increment the count held in the map.
     */
    private void incrementSubfolderAudioCountIfIsDescendant(
            @Nonnull String folder, @Nonnull Map<String, Integer> subFoldersAndAudioFileCounts,
            @Nonnull String audioFilePath) {
        for (String ancestor : calcAncestorFolders(audioFilePath)) {
            if (AudioLoaderUtil.isInFolder(ancestor, folder)) {
                subFoldersAndAudioFileCounts.merge(ancestor, 1, Integer::sum);
            }
        }
    }

    /**
     * Converts a list of audio files, and some subFolders with the number of
     * audio file contained therein to a {@link Pair} of audio files and
     * {@link AudioFolder}s. Assuming, everything is located on the device.
     */
    private Pair<ImmutableList<FullAudioModel>, ImmutableList<AudioFolder>>
    toAudioFilesAndSubFoldersFromDevice(
            List<FullAudioModel> audioListIn, @NonNull Map<String, Integer> audioFolderMapIn) {
        ImmutableList.Builder<AudioFolder> audioFolders = ImmutableList.builder();
        for (Map.Entry<String, Integer> subfolderAndCount : audioFolderMapIn.entrySet()) {
            audioFolders.add(new AudioFolder(
                    new FileSystemFolderAudioLocation(subfolderAndCount.getKey()),
                    subfolderAndCount.getValue()));
        }

        return Pair.create(ImmutableList.copyOf(audioListIn), audioFolders.build());
    }

    private ImmutableList<String> calcAncestorFolders(@NonNull String path) {
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

    @NonNull
    @Contract("_, _, _, _, _ -> new")
    private FullAudioModel createAudioModelOnDevice(String path, String name,
                                                    String artistRaw,
                                                    int dateAddedMillis, long durationMillis) {
        return new FullAudioModel(new FileSystemFolderAudioLocation(path),
                name,
                AudioLoaderUtil.formatArtist(artistRaw),
                new Date(dateAddedMillis * 1000L),
                (long) Math.ceil(durationMillis / 1000f));
    }
}
