package de.soundboardcrafter.activity.common.audioloader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import de.soundboardcrafter.model.AbstractAudioLocation;
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
    private final AssetsAudioLoader assetsAudioLoader = new AssetsAudioLoader();
    private final FileSystemAudioLoader fileSystemAudioLoader = new FileSystemAudioLoader();

    /**
     * Retrieves the audio files (and audio folders) from
     * the selected folder in the file system or assets.
     */
    @SuppressLint("MissingPermission")
    @WorkerThread
    public Pair<ImmutableList<FullAudioModel>, ImmutableList<AudioFolder>>
    loadAudioFolderEntriesWithoutSounds(Context context, IAudioFileSelection selection) {
        if (selection instanceof AnywhereInTheFileSystemAudioLocation) {
            return new Pair<>(fileSystemAudioLoader.getAudios(context),
                    ImmutableList.of());
        }

        if (selection instanceof FileSystemFolderAudioLocation) {
            FileSystemFolderAudioLocation fileSystemFolder =
                    (FileSystemFolderAudioLocation) selection;
            return fileSystemAudioLoader
                    .getAudios(context, fileSystemFolder.getInternalPath());
        }

        if (selection instanceof AssetFolderAudioLocation) {
            AssetFolderAudioLocation assetFolder = (AssetFolderAudioLocation) selection;
            return assetsAudioLoader.loadAudioFolderEntries(context, assetFolder);
        }

        throw new IllegalStateException(
                "folder instance of unexpected class: " + selection.getClass());
    }

    @Nullable
    public FullAudioModel getAudio(Context context, AbstractAudioLocation audioLocation,
                                   String name) {
        if (audioLocation instanceof FileSystemFolderAudioLocation) {
            return fileSystemAudioLoader
                    .getAudio(context, audioLocation.getInternalPath(), name);
        } else if (audioLocation instanceof AssetFolderAudioLocation) {
            try {
                return assetsAudioLoader
                        .getAudio(context, audioLocation.getInternalPath(), name);
            } catch (IOException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Loads all audio files from the assets, returns a map the maps the top folder
     * name to the audio files recursively contained.
     */
    public Map<String, List<BasicAudioModel>> getAllAudiosFromAssetsByTopFolderName(
            Context appContext) {
        return assetsAudioLoader.getAllAudiosByTopFolderName(appContext);
    }
}
