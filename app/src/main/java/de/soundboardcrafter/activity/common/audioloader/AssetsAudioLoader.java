package de.soundboardcrafter.activity.common.audioloader;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.jetbrains.annotations.Contract;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;

import de.soundboardcrafter.model.AssetFolderAudioLocation;
import de.soundboardcrafter.model.PathUtil;
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
    private static final String TRANSLATIONS_FILE_EXTENSION = "txt";
    private static final String TRANSLATIONS_FILE_PREFIX = "translations.";

    private final String TAG = AssetsAudioLoader.class.getName();

    /**
     * Returns a map, containing all audio files from the assets as a map, mapping
     * the audio path to the localized audio name.
     */
    public ImmutableMap<String, String> getAllAudioNamesByPath(Context context) {
        return getAllAudiosByTopFolderName(context).values().stream()
                .flatMap(Collection::stream)
                .collect(toImmutableMap(
                        a -> a.getAudioLocation().getInternalPath(),
                        BasicAudioModel::getName));
    }

    /**
     * Retrieves all audio files from the assets, returns a map the maps the top folder
     * name to the audio files recursively contained.
     */
    Map<String, List<BasicAudioModel>> getAllAudiosByTopFolderName(
            Context context) {
        final ImmutableList<Pair<String, String>> topLevelFolders = getTopLevelFolders(context);

        ImmutableMap.Builder<String, List<BasicAudioModel>> res = ImmutableMap.builder();
        for (Pair<String, String> topLevelFolder : topLevelFolders) {
            res.put(topLevelFolder.second, // internationalized folder name
                    getAudiosRecursively(context, topLevelFolder.first)); // folder path
        }

        return res.build();
    }

    /**
     * Returns the folders (path, name) right below the sound asset root.
     */
    private ImmutableList<Pair<String, String>> getTopLevelFolders(@NonNull Context context) {
        try {
            return getTopLevelFolders(context.getAssets());
        } catch (IOException e) {
            Log.w("IOException while loading assets: " + e, e);
            return ImmutableList.of();
        }
    }

    /**
     * Returns the folders (path, name) right below the sound asset root.
     */
    private ImmutableList<Pair<String, String>> getTopLevelFolders(@NonNull AssetManager assets)
            throws IOException {
        List<String> fileNames = toSortedList(assets.list(ASSET_SOUND_PATH));

        Map<String, String> translations = getTranslations(assets, ASSET_SOUND_PATH, fileNames);

        return fileNames.stream()
                .filter(fn -> !fn.contains("."))
                // It's a subdirectory.
                .map(fn -> new Pair<>(ASSET_SOUND_PATH + "/" + fn,
                        translate(translations, fn)))
                .collect(ImmutableList.toImmutableList());
    }

    @NonNull
    private List<String> toSortedList(@Nullable String[] strings) {
        if (strings == null || strings.length == 0) {
            return ImmutableList.of();
        }

        ArrayList<String> orderedFileNames = new ArrayList<>(Arrays.asList(strings));
        Collections.sort(orderedFileNames);
        return orderedFileNames;
    }

    Pair<ImmutableList<FullAudioModel>, ImmutableList<AudioFolder>> loadAudioFolderEntries(
            Context context, @NonNull AssetFolderAudioLocation assetFolderAudioLocation) {
        return getAudiosAndDirectSubFolders(context, assetFolderAudioLocation.getInternalPath());
    }

    /**
     * Loads all audio files directly or recursively contained in a given folder.
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
     * Loads all audio files and subFolders in a given folder.
     *
     * @return The audio files and the subFolders
     */
    private Pair<ImmutableList<FullAudioModel>, ImmutableList<AudioFolder>>
    getAudiosAndDirectSubFolders(@NonNull final Context context, @Nonnull String folder) {
        try {
            return getAudiosAndDirectSubFolders(context.getAssets(), normalizeFolder(folder));
        } catch (IOException e) {
            Log.w("IOException while loading assets: " + e, e);
            return Pair.create(ImmutableList.of(), ImmutableList.of());
        }
    }

    private ImmutableList<BasicAudioModel> getAudiosRecursively(
            @NonNull AssetManager assets, String directory) throws IOException {
        List<String> fileNames = toSortedList(assets.list(directory));

        Map<String, String> translations = getTranslations(assets, directory, fileNames);

        final ImmutableList.Builder<BasicAudioModel> res = ImmutableList.builder();

        for (String fileName : fileNames) {
            String assetPath =
                    Joiner.on("/").skipNulls().join(emptyToNull(directory), fileName);

            if (!fileName.contains(".")) {
                // It's a subdirectory.
                res.addAll(getAudiosRecursively(assets, directory + "/" + fileName));
            } else if (!isATranslationsFile(fileName)) {
                // It's a sound file.
                BasicAudioModel audioModel =
                        createBasicAudioModel(assetPath,
                                translate(translations,
                                        pathOrFileNameToInternationalName(fileName)));
                res.add(audioModel);
            }
        }

        return res.build();
    }

    private Pair<ImmutableList<FullAudioModel>, ImmutableList<AudioFolder>>
    getAudiosAndDirectSubFolders(@NonNull AssetManager assets, String directory)
            throws IOException {
        List<String> fileNames = toSortedList(assets.list(directory));

        Map<String, String> translations = getTranslations(assets, directory, fileNames);

        final ImmutableList.Builder<FullAudioModel> audioFileList = ImmutableList.builder();
        final ImmutableList.Builder<AudioFolder> directSubFolders = ImmutableList.builder();

        for (String fileName : fileNames) {
            String assetPath =
                    Joiner.on("/").skipNulls().join(emptyToNull(directory), fileName);

            if (!fileName.contains(".")) {
                // It's a subdirectory.

                final AssetFolderAudioLocation folderLocation =
                        new AssetFolderAudioLocation(assetPath);
                AudioFolder audioFolder = new AudioFolder(folderLocation,
                        translate(translations, fileName),
                        getNumAudioFiles(assets, assetPath));
                directSubFolders.add(audioFolder);
            } else if (!isATranslationsFile(fileName)) {
                // It's a sound file.
                FullAudioModel audioModel =
                        createFullAudioModel(assets, assetPath,
                                translate(translations,
                                        pathOrFileNameToInternationalName(fileName)));
                audioFileList.add(audioModel);
            }
        }

        return Pair.create(audioFileList.build(), directSubFolders.build());
    }

    @Nonnull
    private Map<String, String> getTranslations(AssetManager assets, String directory,
                                                Collection<String> fileNames) throws IOException {
        final LocaleListCompat locales =
                ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration());

        for (int i = 0; i < locales.size(); i++) {
            final Locale locale = locales.get(i);

            @Nullable
            Map<String, String> translations =
                    getTranslations(assets, directory, fileNames, locale);
            if (translations != null) {
                return translations;
            }
        }

        return ImmutableMap.of();
    }

    @Nullable
    private Map<String, String> getTranslations(AssetManager assets, String directory,
                                                Collection<String> fileNames, Locale locale)
            throws IOException {
        @Nullable
        Map<String, String> forLocale =
                getTranslations(assets, directory, fileNames, locale.toLanguageTag());
        if (forLocale != null) {
            return forLocale;
        }

        @Nullable
        Map<String, String> forLanguage =
                getTranslations(assets, directory, fileNames, locale.getLanguage().toLowerCase());
        return forLanguage;
    }

    @Nullable
    private Map<String, String> getTranslations(AssetManager assets, String directory,
                                                Collection<String> fileNames, String localeMarker)
            throws IOException {
        String fileName =
                TRANSLATIONS_FILE_PREFIX + localeMarker + "." + TRANSLATIONS_FILE_EXTENSION;

        if (fileNames.contains(fileName)) {
            return getTranslations(assets, directory, fileName);
        }

        return null;
    }

    private Map<String, String> getTranslations(AssetManager assets, String directory,
                                                String fileName) throws IOException {
        String path = directory + "/" + fileName;

        ImmutableMap.Builder<String, String> result = ImmutableMap.builder();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(assets.open(path), StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                @Nullable final Pair<String, String> translationPair =
                        parseTranslationLine(path, line);
                if (translationPair != null) {
                    result.put(translationPair.first, translationPair.second);
                }
            }
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close translations file " + path, e);
            }
        }

        return result.build();
    }

    @Nullable
    private Pair<String, String> parseTranslationLine(String path, String line) {
        if (line.trim().isEmpty()) {
            return null;
        }

        final String[] parts = line.split("=");
        if (parts.length != 2) {
            throw new IllegalStateException("Wrong translation line format in " + path
                    + ": '" + line + "'");
        }

        return new Pair<>(parts[0].trim(), parts[1].trim());
    }

    private String translate(Map<String, String> translations, String internationalName) {
        return translations.getOrDefault(internationalName, internationalName);
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
        List<String> fileNames = toSortedList(assets.list(directory));

        int res = 0;
        for (String fileName : fileNames) {
            String assetPath = Joiner.on("/").skipNulls().join(emptyToNull(directory), fileName);

            if (!fileName.contains(".")) {
                // It's a subdirectory.
                res += getNumAudioFiles(assets, assetPath);
            } else if (!isATranslationsFile(fileName)) {
                // It's a sound file.
                res++;
            }
        }
        return res;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isATranslationsFile(String fileName) {
        return fileName.startsWith(TRANSLATIONS_FILE_PREFIX)
                && fileName.toLowerCase().endsWith("." + TRANSLATIONS_FILE_EXTENSION);
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
                                                  String name) {
        return new BasicAudioModel(new AssetFolderAudioLocation(assetPath), name);
    }

    @Nullable
    private String extractArtist(@NonNull MediaMetadataRetriever metadataRetriever) {
        @Nullable String raw =
                metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        return AudioLoaderUtil.formatArtist(raw);
    }

    @NonNull
    public static String pathOrFileNameToInternationalName(@NonNull String pathOrFileName) {
        return PathUtil.removeExtension(PathUtil.extractFileName(pathOrFileName));
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
