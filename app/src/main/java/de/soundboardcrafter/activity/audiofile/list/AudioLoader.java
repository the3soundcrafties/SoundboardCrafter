package de.soundboardcrafter.activity.audiofile.list;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.soundboardcrafter.R;
import de.soundboardcrafter.model.AssetAudioLocation;
import de.soundboardcrafter.model.FileSystemAudioLocation;

import static com.google.common.base.Preconditions.checkNotNull;

class AudioLoader {
    /**
     * Path inside the assets directory where the sounds are located.
     */
    static final String ASSET_SOUND_PATH = "sounds";

    /**
     * Loads all audio files from the device and the assets.
     */
    ImmutableList<AudioModel> getAllAudios(final Context context) {
        final ImmutableList.Builder<AudioModel> res = ImmutableList.builder();

        res.addAll(getAudiosFromDevice(context));
        res.addAll(getAudiosFromAssets(context));

        return res.build();
    }

    /**
     * Loads all audio files from the device.
     */
    private ImmutableList<AudioModel> getAudiosFromDevice(Context context) {
        final ImmutableList.Builder<AudioModel> res = ImmutableList.builder();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.AudioColumns.DATA,
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
                            context,
                            path, c.getString(1), c.getString(2),
                            c.getInt(3), c.getLong(4)));
                }
            }
        }

        return res.build();
    }


    /**
     * Loads all audio files and subFolders in a given folder <i>on the device</i>.
     *
     * @return The audio files and the subFolders
     */
    Pair<ImmutableList<AudioModel>, ImmutableList<AudioFolder>> getAudioFromDevice(
            final Context context, @NonNull String folder) {
        checkNotNull(folder, "folder was null");

        if (!folder.endsWith("/")) {
            folder += "/";
        }

        final List<AudioModel> audioFileList = new ArrayList<>();
        final Map<String, Integer> subFoldersAndAudioFileCounts = new HashMap<>();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.AudioColumns.DATA,
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
                    if (isInFolder(path, folder)) {
                        audioFileList.add(createAudioModelOnDevice(
                                context,
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
     * Loads all audio files from the assets.
     */
    private ImmutableList<AudioModel> getAudiosFromAssets(final Context context) {
        try {
            AssetManager assets = context.getAssets();
            return getAudios(context, assets, ASSET_SOUND_PATH);
        } catch (IOException e) {
            Log.w("IOException while loading assets: " + e, e);
            return ImmutableList.of();
        }
    }

    /**
     * Loads all audio files from the assets in this directory, including all sub directories
     *
     * @param directory directory in the assets folder, neither starting nor ending with a slash
     */
    private ImmutableList<AudioModel> getAudios(Context context,
                                                AssetManager assets, String directory)
            throws IOException {
        @Nullable String[] fileNames = assets.list(directory);
        if (fileNames == null) {
            return ImmutableList.of();
        }

        final ImmutableList.Builder<AudioModel> res = ImmutableList.builder();
        for (String fileName : fileNames) {
            String assetPath =
                    Joiner.on("/").skipNulls().join(Strings.emptyToNull(directory), fileName);

            if (fileName.contains(".")) {
                // It's a sound file.
                AudioModel audioModel =
                        createAudioModelFromAsset(context, assets, assetPath, fileName);

                res.add(audioModel);
            } else {
                // It's a sub directory.
                res.addAll(getAudios(context, assets, assetPath));
            }
        }

        return res.build();
    }


    /**
     * Loads all audio files and subFolders in a given folder <i>from the assets</i>.
     *
     * @return The audio files and the subFolders
     */
    Pair<ImmutableList<AudioModel>, ImmutableList<AudioFolder>> getAudioFromAssets(
            final Context context, @NonNull String folder) {
        checkNotNull(folder, "folder was null");

        if (folder.endsWith("/")) {
            folder = folder.substring(0, folder.length() - 1);
        }

        if (folder.startsWith("/")) {
            folder = folder.substring(1);
        }

        try {
            AssetManager assets = context.getAssets();
            return getAudiosAndDirectSubFolders(context, assets, folder);
        } catch (IOException e) {
            Log.w("IOException while loading assets: " + e, e);
            return Pair.create(ImmutableList.of(), ImmutableList.of());
        }
    }

    /**
     * Loads all audio files from the assets in this directory, including all sub directories,
     * also creates all direct subFolders.
     *
     * @param directory directory in the assets folder, neither starting nor ending with slash
     * @return The audio files and the direct subFolders
     */
    private Pair<ImmutableList<AudioModel>, ImmutableList<AudioFolder>>
    getAudiosAndDirectSubFolders(Context context, AssetManager assets, String directory)
            throws IOException {
        @Nullable String[] fileNames = assets.list(directory);

        if (fileNames == null) {
            return Pair.create(ImmutableList.of(), ImmutableList.of());
        }

        final ImmutableList.Builder<AudioModel> audioFileList = ImmutableList.builder();
        final ImmutableList.Builder<AudioFolder> directSubFolders = ImmutableList.builder();

        for (String fileName : fileNames) {
            String assetPath =
                    Joiner.on("/").skipNulls().join(Strings.emptyToNull(directory), fileName);

            if (fileName.contains(".")) {
                // It's a sound file.
                AudioModel audioModel =
                        createAudioModelFromAsset(context, assets, assetPath, fileName);
                audioFileList.add(audioModel);
            } else {
                // It's a sub directory.
                AudioFolder audioFolder =
                        new AudioFolder(
                                new AssetAudioLocation(assetPath),
                                getAudios(context, assets, assetPath).size());
                directSubFolders.add(audioFolder);
            }
        }

        return Pair.create(audioFileList.build(), directSubFolders.build());
    }

    private AudioModel createAudioModelFromAsset(Context context,
                                                 AssetManager assets,
                                                 String assetPath,
                                                 String filename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(assetPath);

        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        metadataRetriever.setDataSource(fileDescriptor.getFileDescriptor(),
                fileDescriptor.getStartOffset(),
                fileDescriptor.getLength());

        @NonNull String name = extractName(filename);
        long durationSecs = extractDurationSecs(metadataRetriever);
        @NonNull String artist = extractArtist(context, metadataRetriever);

        return new AudioModel(
                new AssetAudioLocation(assetPath),
                name,
                artist,
                durationSecs);
    }

    @NonNull
    private String extractArtist(Context context, MediaMetadataRetriever metadataRetriever) {
        @Nullable String raw =
                metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        return formatArtist(context, raw);
    }

    private String formatArtist(Context context, @Nullable String raw) {
        if (Strings.isNullOrEmpty(raw) || raw.equals("<unknown>")) {
            return context.getString(R.string.artist_unknown);
        }

        return raw;
    }

    private String extractName(String filename) {
        int indexOfDot = filename.lastIndexOf(".");
        if (indexOfDot <= 0) {
            return filename;
        }

        return filename.substring(0, indexOfDot);
    }

    private long extractDurationSecs(MediaMetadataRetriever metadataRetriever) {
        @Nullable String durationMillisString =
                metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if (durationMillisString == null) {
            return 0;
        }
        try {
            return (long) Math.ceil(Long.parseLong(durationMillisString) / 1000f);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private AudioModel createAudioModelOnDevice(Context context,
                                                String path, String name,
                                                String artistRaw,
                                                int dateAddedMillis, long durationMillis) {
        return new AudioModel(new FileSystemAudioLocation(path),
                name,
                formatArtist(context, artistRaw),
                new Date(dateAddedMillis * 1000L),
                (long) Math.ceil(durationMillis / 1000f));
    }

    /**
     * Checks whether the <code>audioFilePath</code> is in a subfolder of the folder - if it is,
     * add the folder to the map - or increment the count held in the map.
     */
    private void incrementSubfolderAudioCountIfIsDescendant(
            @NonNull String folder, @NonNull Map<String, Integer> subFoldersAndAudioFileCounts,
            @NonNull String audioFilePath) {
        for (String ancestor : calcAncestorFolders(audioFilePath)) {
            if (isInFolder(ancestor, folder)) {
                Integer old = subFoldersAndAudioFileCounts.get(ancestor);
                if (old == null) {
                    subFoldersAndAudioFileCounts.put(ancestor, 1);
                } else {
                    subFoldersAndAudioFileCounts.put(ancestor, old + 1);
                }
            }
        }
    }

    /**
     * Converts a list of audio files and some subFolders with the number of
     * audio file contained therein to a {@link Pair} of audio files and
     * {@link AudioFolder}s. Assuming, everything is located on the device.
     */
    private Pair<ImmutableList<AudioModel>, ImmutableList<AudioFolder>>
    toAudioFilesAndSubFoldersFromDevice(
            List<AudioModel> audioListIn, Map<String, Integer> audioFolderMapIn) {
        ImmutableList.Builder<AudioFolder> audioFolders = ImmutableList.builder();
        for (Map.Entry<String, Integer> subfolderAndCount : audioFolderMapIn.entrySet()) {
            audioFolders.add(new AudioFolder(
                    new FileSystemAudioLocation(subfolderAndCount.getKey()),
                    subfolderAndCount.getValue()));
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
