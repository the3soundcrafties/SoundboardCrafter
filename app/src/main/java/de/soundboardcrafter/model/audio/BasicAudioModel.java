package de.soundboardcrafter.model.audio;

import static com.google.common.base.Preconditions.checkNotNull;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.text.CollationKey;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import de.soundboardcrafter.model.AbstractAudioLocation;
import de.soundboardcrafter.model.ThreadSafeCollator;

/**
 * Basic audio file data / metadata: name and location.
 */
@ParametersAreNonnullByDefault
public class BasicAudioModel implements Parcelable {
    public static final Parcelable.Creator<BasicAudioModel> CREATOR
            = new Parcelable.Creator<BasicAudioModel>() {
        @Override
        public BasicAudioModel createFromParcel(@NonNull Parcel in) {
            return new BasicAudioModel(in);
        }

        @Override
        public BasicAudioModel[] newArray(int size) {
            return new BasicAudioModel[size];
        }
    };

    private static final ThreadSafeCollator nameCollator =
            ThreadSafeCollator.getInstance();
    @NonNull
    private final AbstractAudioLocation audioLocation;
    @NonNull
    protected final String name;
    // May not be serializable.
    @NonNull
    private transient CollationKey collationKey;

    private BasicAudioModel(@NonNull Parcel in) {
        this(in.readParcelable(BasicAudioModel.class.getClassLoader()),
                in.readString());
    }

    public BasicAudioModel(AbstractAudioLocation audioLocation, String name) {
        this.audioLocation = checkNotNull(audioLocation, "audioLocation is null");
        this.name = checkNotNull(name, "name is null");

        setCollationKey();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(audioLocation, 0);
        out.writeString(name);
    }

    @NonNull
    public AbstractAudioLocation getAudioLocation() {
        return audioLocation;
    }

    /**
     * Returns the name.
     * <p></p>
     * For  sorting purposes better use {@link #getCollationKey()}.
     */
    @NonNull
    public String getName() {
        return name;
    }

    final void setCollationKey() {
        collationKey = nameCollator.getCollationKey(getName());
    }

    @NonNull
    public CollationKey getCollationKey() {
        return collationKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BasicAudioModel that = (BasicAudioModel) o;
        return audioLocation.equals(that.audioLocation) &&
                name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    @Nonnull
    public String toString() {
        return "BasicAudioModel{" +
                "audioLocation=" + audioLocation +
                ", name='" + name + '\'' +
                '}';
    }
}
