package de.soundboardcrafter.activity.soundboard.edit;

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
class AudioSelectionChanges implements Parcelable {
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
    private final Set<AbstractAudioLocation> additions;

    /**
     * Audios the user has removed. Kept in sync with {@link #additions}.
     */
    private final Set<AbstractAudioLocation> removals;

    public AudioSelectionChanges(Parcel in) {
        this(readAudioLocationsList(in), readAudioLocationsList(in));
    }

    public AudioSelectionChanges() {
        this(new HashSet<>(), new HashSet<>());
    }

    private AudioSelectionChanges(Collection<AbstractAudioLocation> additions,
                                  Collection<AbstractAudioLocation> removals) {
        this.additions = new HashSet<>(additions);
        this.removals = new HashSet<>(removals);
    }

    private static List<AbstractAudioLocation> readAudioLocationsList(Parcel in) {
        List<AbstractAudioLocation> res = new LinkedList<>();
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

    public void addAll(Iterable<AbstractAudioLocation> iterable) {
        for (AbstractAudioLocation audioLocation : iterable) {
            add(audioLocation);
        }
    }

    public void removeAll(Iterable<AbstractAudioLocation> iterable) {
        for (AbstractAudioLocation audioLocation : iterable) {
            remove(audioLocation);
        }
    }

    private void add(AbstractAudioLocation audioLocation) {
        additions.add(audioLocation);
        removals.remove(audioLocation);
    }

    private void remove(AbstractAudioLocation audioLocation) {
        removals.add(audioLocation);
        additions.remove(audioLocation);
    }

    public boolean isAdded(AbstractAudioLocation audioLocation) {
        return additions.contains(audioLocation);
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
