package de.soundboardcrafter.activity.common.audioloader;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import de.soundboardcrafter.model.AssetFolderAudioLocation;
import de.soundboardcrafter.model.audio.AudioFolder;
import de.soundboardcrafter.model.audio.BasicAudioModel;
import de.soundboardcrafter.model.audio.FullAudioModel;

/**
 * Loader for audio files, can load audio files from the assets.
 *
 * @see FileSystemAudioLoader
 */
public class AssetsAudioLoader {
    /**
     * Path inside the assets directory where the sounds are located.
     */
    public static final String ASSET_SOUND_PATH = "sounds";

    Map<String, List<BasicAudioModel>> getAllAudiosByTopFolderName(
            Context context) {
        final ImmutableList<String> topLevelFolderNames = getTopLevelFolderNames(context);

        ImmutableMap.Builder<String, List<BasicAudioModel>> res = ImmutableMap.builder();
        for (String topLevelFolderName : topLevelFolderNames) {
            res.put(topLevelFolderName,
                    getAudiosRecursively(context,
                            ASSET_SOUND_PATH + "/" + topLevelFolderName));
        }

        return res.build();
    }

    private ImmutableList<String> getTopLevelFolderNames(@NonNull Context context) {
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

    Pair<ImmutableList<FullAudioModel>, ImmutableList<AudioFolder>> loadAudioFolderEntries(
            Context context, @NonNull AssetFolderAudioLocation assetFolderAudioLocation) {
        return getAudiosAndDirectSubFolders(context,
                assetFolderAudioLocation.getInternalPath());
    }

    /**
     * Loads all audio files directly or recursively contained in a given folder <i>from the
     * assets</i>.
     */
    private ImmutableList<BasicAudioModel> getAudiosRecursively(
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
    private Pair<ImmutableList<FullAudioModel>, ImmutableList<AudioFolder>> getAudiosAndDirectSubFolders(
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
                        createBasicAudioModel(assetPath, fileName);
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
                        createFullAudioModel(assets, assetPath, fileName);
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

    FullAudioModel getAudio(@NonNull Context context, String path, String name)
            throws IOException {
        return getAudio(context.getAssets(), path, name);
    }

    @NonNull
    @Contract("_, _, _ -> new")
    private FullAudioModel getAudio(@NonNull AssetManager assets, String assetPath,
                                    String name)
            throws IOException {
        return createFullAudioModel(assets, assetPath, name);
    }

    @NonNull
    @Contract("_, _, _ -> new")
    private FullAudioModel createFullAudioModel(AssetManager assets,
                                                String assetPath,
                                                String name)
            throws IOException {
        try (AssetFileDescriptor fileDescriptor = assets.openFd(assetPath)) {
            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            metadataRetriever.setDataSource(fileDescriptor.getFileDescriptor(),
                    fileDescriptor.getStartOffset(),
                    fileDescriptor.getLength());

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
    private BasicAudioModel createBasicAudioModel(String assetPath,
                                                  String filename) {
        @Nonnull String name = skipExtension(filename);
        return new BasicAudioModel(new AssetFolderAudioLocation(assetPath), name);
    }

    @Nullable
    private String extractArtist(@NonNull MediaMetadataRetriever metadataRetriever) {
        @Nullable String raw =
                metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        return AudioLoaderUtil.formatArtist(raw);
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
}
