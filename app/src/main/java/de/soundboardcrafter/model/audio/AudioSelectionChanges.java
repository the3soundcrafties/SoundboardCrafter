package de.soundboardcrafter.model.audio;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import de.soundboardcrafter.model.AbstractAudioLocation;

/**
 * Changes the user has made to the audio files chosen for a soundboard:
 * She has added some and removed others (and left the rest as they are).
 */
@ParametersAreNonnullByDefault
public class AudioSelectionChanges implements Parcelable {
    public static final Parcelable.Creator<AudioSelectionChanges> CREATOR
            = new Parcelable.Creator<AudioSelectionChanges>() {
        @Override
        public AudioSelectionChanges createFromParcel(@NonNull Parcel in) {
            return new AudioSelectionChanges(in);
        }

        @Override
        public AudioSelectionChanges[] newArray(int size) {
            return new AudioSelectionChanges[size];
        }
    };

    /**
     * Audios the user has added. Kept in sync with {@link #removals}.
     */
    private final Set<BasicAudioModel> additions;

    /**
     * Audios the user has removed. Kept in sync with {@link #additions}.
     */
    private final Set<AbstractAudioLocation> removals;

    public AudioSelectionChanges(Parcel in) {
        this(readList(in), readList(in));
    }

    public AudioSelectionChanges() {
        this(new HashSet<>(), new HashSet<>());
    }

    private AudioSelectionChanges(Collection<BasicAudioModel> additions,
                                  Collection<AbstractAudioLocation> removals) {
        this.additions = new HashSet<>(additions);
        this.removals = new HashSet<>(removals);
    }

    private static <T> List<T> readList(Parcel in) {
        List<T> res = new LinkedList<>();
        in.readList(res, AudioSelectionChanges.class.getClassLoader());
        return res;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeList(ImmutableList.copyOf(additions));
        out.writeList(ImmutableList.copyOf(removals));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public ImmutableList<BasicAudioModel> getImmutableAdditions() {
        // TODO Sort be location? By folder and name?
        return ImmutableList.copyOf(additions);
    }

    public ImmutableList<AbstractAudioLocation> getImmutableRemovals() {
        return ImmutableList.copyOf(removals);
    }

    public void addAll(Iterable<BasicAudioModel> iterable) {
        for (BasicAudioModel audio : iterable) {
            add(audio);
        }
    }

    public void removeAll(Iterable<AbstractAudioLocation> iterable) {
        for (AbstractAudioLocation audioLocation : iterable) {
            remove(audioLocation);
        }
    }

    private void add(BasicAudioModel audio) {
        additions.add(audio);
        removals.remove(audio.getAudioLocation());
    }

    private void remove(AbstractAudioLocation audioLocation) {
        removals.add(audioLocation);
        additions.removeIf(audio -> audio.getAudioLocation().equals(audioLocation));
    }

    public boolean isAdded(BasicAudioModel audio) {
        return additions.contains(audio);
    }

    public boolean isRemoved(AbstractAudioLocation audioLocation) {
        return removals.contains(audioLocation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AudioSelectionChanges that = (AudioSelectionChanges) o;
        return additions.equals(that.additions) && removals.equals(that.removals);
    }

    @Override
    public int hashCode() {
        return Objects.hash(additions, removals);
    }
}
