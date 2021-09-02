package de.soundboardcrafter.activity.common.audioloader;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;

import android.annotation.SuppressLint;
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
import androidx.annotation.RequiresPermission;
import androidx.annotation.WorkerThread;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import de.soundboardcrafter.model.AnywhereInTheFileSystemAudioLocation;
import de.soundboardcrafter.model.AssetFolderAudioLocation;
import de.soundboardcrafter.model.FileSystemFolderAudioLocation;
import de.soundboardcrafter.model.IAudioFileSelection;
import de.soundboardcrafter.model.audio.AudioFolder;
import de.soundboardcrafter.model.audio.BasicAudioModel;
import de.soundboardcrafter.model.audio.FullAudioModel;

/**
 * Loader for audio files. Can load audio files from the assets as well as from the device.
 */
public class AudioLoader {
    /**
     * Path inside the assets directory where the sounds are located.
     */
    public static final String ASSET_SOUND_PATH = "sounds";

    /**
     * Retrieves the audio files (and audio folders) from
     * the selected folder in the file system or assets.
     */
    @SuppressLint("MissingPermission")
    @WorkerThread
    public Pair<ImmutableList<FullAudioModel>, ImmutableList<AudioFolder>>
    loadAudioFolderEntriesWithoutSounds(Context context, IAudioFileSelection selection) {
        if (selection instanceof AnywhereInTheFileSystemAudioLocation) {
            return new Pair<>(getAudiosFromDevice(context), ImmutableList.of());
        }

        if (selection instanceof FileSystemFolderAudioLocation) {
            FileSystemFolderAudioLocation fileSystemFolder =
                    (FileSystemFolderAudioLocation) selection;
            return getAudiosFromDevice(context, fileSystemFolder.getInternalPath());
        }

        if (selection instanceof AssetFolderAudioLocation) {
            AssetFolderAudioLocation assetFolder = (AssetFolderAudioLocation) selection;
            return loadAudioFolderEntriesWithoutSounds(context, assetFolder);
        }

        throw new IllegalStateException(
                "folder instance of unexpected class: " + selection.getClass());
    }

    /**
     * Loads all audio files from the device.
     */
    @RequiresPermission("android.permission.READ_EXTERNAL_STORAGE")
    private ImmutableList<FullAudioModel> getAudiosFromDevice(Context context) {
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

    /**
     * Loads all audio files and subFolders in a given folder <i>on the device</i>.
     *
     * @return The audio files and the subFolders
     */
    @RequiresPermission("android.permission.READ_EXTERNAL_STORAGE")
    private Pair<ImmutableList<FullAudioModel>, ImmutableList<AudioFolder>> getAudiosFromDevice(
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
                    if (isInFolder(path, folder)) {
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


    public Map<String, List<BasicAudioModel>> getAllAudiosFromAssetsByTopFolderName(
            Context context) {
        final ImmutableList<String> topLevelFolderNames = getTopLevelAssetFolderNames(context);

        ImmutableMap.Builder<String, List<BasicAudioModel>> res = ImmutableMap.builder();
        for (String topLevelFolderName : topLevelFolderNames) {
            res.put(topLevelFolderName,
                    getAudiosFromAssetsRecursively(context,
                            ASSET_SOUND_PATH + "/" + topLevelFolderName));
        }

        return res.build();
    }

    private ImmutableList<String> getTopLevelAssetFolderNames(@NonNull Context context) {
        try {
            return getTopLevelFolderNames(context.getAssets());
        } catch (IOException e) {
            Log.w("IOException while loading assets: " + e, e);
            return ImmutableList.of();
        }
    }

    /**
     * Returns the folder names right below the sound asset root.
     */
    private ImmutableList<String> getTopLevelFolderNames(@NonNull AssetManager assets)
            throws IOException {
        @Nullable String[] fileNames = assets.list(ASSET_SOUND_PATH);

        if (fileNames == null) {
            return ImmutableList.of();
        }

        return Stream.of(fileNames)
                .filter(n -> !n.contains("."))
                // It's a subdirectory.
                .collect(ImmutableList.toImmutableList());
    }


    private Pair<ImmutableList<FullAudioModel>, ImmutableList<AudioFolder>> loadAudioFolderEntriesWithoutSounds(
            Context context, @NonNull AssetFolderAudioLocation assetFolderAudioLocation) {
        return getAudiosAndDirectSubFoldersFromAssets(context,
                assetFolderAudioLocation.getInternalPath());
    }

    /**
     * Loads all audio files directly or recursively contained in a given folder <i>from the
     * assets</i>.
     */
    private ImmutableList<BasicAudioModel> getAudiosFromAssetsRecursively(
            @NonNull final Context context, @Nonnull String folder) {
        try {
            return getAudiosRecursively(context.getAssets(), normalizeFolder(folder));
        } catch (IOException e) {
            Log.w("IOException while loading assets: " + e, e);
            return ImmutableList.of();
        }
    }

    /**
     * Loads all audio files and subFolders in a given folder <i>from the assets</i>.
     *
     * @return The audio files and the subFolders
     */
    private Pair<ImmutableList<FullAudioModel>, ImmutableList<AudioFolder>> getAudiosAndDirectSubFoldersFromAssets(
            @NonNull final Context context, @Nonnull String folder) {
        try {
            return getAudiosAndDirectSubFolders(context.getAssets(), normalizeFolder(folder));
        } catch (IOException e) {
            Log.w("IOException while loading assets: " + e, e);
            return Pair.create(ImmutableList.of(), ImmutableList.of());
        }
    }

    private ImmutableList<BasicAudioModel> getAudiosRecursively(
            @NonNull AssetManager assets, String directory) throws IOException {
        @Nullable String[] fileNames = assets.list(directory);

        if (fileNames == null) {
            return ImmutableList.of();
        }

        final ImmutableList.Builder<BasicAudioModel> res = ImmutableList.builder();

        for (String fileName : fileNames) {
            String assetPath =
                    Joiner.on("/").skipNulls().join(emptyToNull(directory), fileName);

            if (fileName.contains(".")) {
                // It's a sound file.
                BasicAudioModel audioModel =
                        createBasicAudioModelFromAsset(assetPath, fileName);
                res.add(audioModel);
            } else {
                // It's a subdirectory.
                res.addAll(getAudiosRecursively(assets, directory + "/" + fileName));
            }
        }

        return res.build();
    }

    private Pair<ImmutableList<FullAudioModel>, ImmutableList<AudioFolder>>
    getAudiosAndDirectSubFolders(@NonNull AssetManager assets, String directory)
            throws IOException {
        @Nullable String[] fileNames = assets.list(directory);

        if (fileNames == null) {
            return Pair.create(ImmutableList.of(), ImmutableList.of());
        }

        final ImmutableList.Builder<FullAudioModel> audioFileList = ImmutableList.builder();
        final ImmutableList.Builder<AudioFolder> directSubFolders = ImmutableList.builder();

        for (String fileName : fileNames) {
            String assetPath =
                    Joiner.on("/").skipNulls().join(emptyToNull(directory), fileName);

            if (fileName.contains(".")) {
                // It's a sound file.
                FullAudioModel audioModel =
                        createFullAudioModelFromAsset(assets, assetPath, fileName);
                audioFileList.add(audioModel);
            } else {
                // It's a subdirectory.
                AudioFolder audioFolder = new AudioFolder(
                        new AssetFolderAudioLocation(assetPath),
                        getNumAudioFiles(assets, assetPath));
                directSubFolders.add(audioFolder);
            }
        }

        return Pair.create(audioFileList.build(), directSubFolders.build());
    }

    @Nonnull
    private String normalizeFolder(@Nonnull String folder) {
        checkNotNull(folder, "folder was null");

        if (folder.endsWith("/")) {
            folder = folder.substring(0, folder.length() - 1);
        }

        if (folder.startsWith("/")) {
            folder = folder.substring(1);
        }
        return folder;
    }

    /**
     * Retrieves the number of audio files in this asset directory, including all subdirectories
     *
     * @param directory directory in the assets folder, neither starting nor ending with a slash
     */
    private int getNumAudioFiles(@NonNull AssetManager assets, String directory)
            throws IOException {
        @Nullable String[] fileNames = assets.list(directory);
        if (fileNames == null) {
            return 0;
        }

        int res = 0;
        for (String fileName : fileNames) {
            String assetPath = Joiner.on("/").skipNulls().join(emptyToNull(directory), fileName);

            if (fileName.contains(".")) {
                // It's a sound file.
                res++;
            } else {
                // It's a subdirectory.
                res += getNumAudioFiles(assets, assetPath);
            }
        }
        return res;
    }

    @NonNull
    @Contract("_, _, _ -> new")
    private FullAudioModel createFullAudioModelFromAsset(AssetManager assets,
                                                         String assetPath,
                                                         String filename)
            throws IOException {
        try (AssetFileDescriptor fileDescriptor = assets.openFd(assetPath)) {
            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            metadataRetriever.setDataSource(fileDescriptor.getFileDescriptor(),
                    fileDescriptor.getStartOffset(),
                    fileDescriptor.getLength());

            @Nonnull String name = skipExtension(filename);
            long durationSecs = extractDurationSecs(metadataRetriever);
            @Nullable String artist = extractArtist(metadataRetriever);
            return new FullAudioModel(
                    new AssetFolderAudioLocation(assetPath),
                    name,
                    artist,
                    durationSecs);
        }
    }

    @NonNull
    private BasicAudioModel createBasicAudioModelFromAsset(String assetPath,
                                                           String filename) {
        @Nonnull String name = skipExtension(filename);
        return new BasicAudioModel(new AssetFolderAudioLocation(assetPath), name);
    }

    @Nullable
    private String extractArtist(@NonNull MediaMetadataRetriever metadataRetriever) {
        @Nullable String raw =
                metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        return formatArtist(raw);
    }

    @Nullable
    private String formatArtist(@Nullable String raw) {
        if (raw == null || raw.equals("<unknown>")) {
            return null;
        }

        return emptyToNull(raw.trim());
    }

    @NonNull
    private String skipExtension(@NonNull String filename) {
        int indexOfDot = filename.lastIndexOf(".");
        if (indexOfDot <= 0) {
            return filename;
        }

        return filename.substring(0, indexOfDot);
    }

    private long extractDurationSecs(@NonNull MediaMetadataRetriever metadataRetriever) {
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

    @NonNull
    @Contract("_, _, _, _, _ -> new")
    private FullAudioModel createAudioModelOnDevice(String path, String name,
                                                    String artistRaw,
                                                    int dateAddedMillis, long durationMillis) {
        return new FullAudioModel(new FileSystemFolderAudioLocation(path),
                name,
                formatArtist(artistRaw),
                new Date(dateAddedMillis * 1000L),
                (long) Math.ceil(durationMillis / 1000f));
    }

    /**
     * Checks whether the <code>audioFilePath</code> is in a subfolder of the folder - if it is,
     * add the folder to the map - or increment the count held in the map.
     */
    private void incrementSubfolderAudioCountIfIsDescendant(
            @Nonnull String folder, @Nonnull Map<String, Integer> subFoldersAndAudioFileCounts,
            @Nonnull String audioFilePath) {
        for (String ancestor : calcAncestorFolders(audioFilePath)) {
            if (isInFolder(ancestor, folder)) {
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

    /**
     * Checks whether this <code>path</code> is in this <code>folder</code>
     * (not in a sub-folder).
     */
    private boolean isInFolder(@Nonnull String path, @Nonnull String folder) {
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
