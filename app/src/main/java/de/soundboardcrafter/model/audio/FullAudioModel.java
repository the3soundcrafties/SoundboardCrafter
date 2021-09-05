package de.soundboardcrafter.model.audio;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import de.soundboardcrafter.model.AbstractAudioLocation;

/**
 * Full audio file data / metadata, including during, e.g.
 */
@ParametersAreNonnullByDefault
public
class FullAudioModel extends BasicAudioModel {
    public static final Parcelable.Creator<FullAudioModel> CREATOR
            = new Parcelable.Creator<FullAudioModel>() {
        @Override
        public FullAudioModel createFromParcel(@NonNull Parcel in) {
            return new FullAudioModel(in);
        }

        @Override
        public FullAudioModel[] newArray(int size) {
            return new FullAudioModel[size];
        }
    };

    @Nullable
    private final String artist;

    @Nullable
    private final Date dateAdded;

    private final long durationSecs;

    private FullAudioModel(@NonNull Parcel in) {
        this(in.readParcelable(FullAudioModel.class.getClassLoader()),
                in.readString(), in.readString(),
                readDate(in),
                in.readLong());
    }

    @Nullable
    private static Date readDate(@NonNull Parcel in) {
        long tmpDate = in.readLong();
        return tmpDate == Long.MIN_VALUE ? null : new Date(tmpDate);
    }

    public FullAudioModel(AbstractAudioLocation audioLocation, String name,
                          @Nullable String artist, long durationSecs) {
        this(audioLocation, name, artist, null, durationSecs);
    }

    public FullAudioModel(AbstractAudioLocation audioLocation, String name, @Nullable String artist,
                          @Nullable Date dateAdded, long durationSecs) {
        super(audioLocation, name);
        this.artist = artist;
        this.dateAdded = dateAdded;
        this.durationSecs = durationSecs;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(getAudioLocation(), 0);
        out.writeString(getName());
        out.writeString(artist);
        writeDate(out, dateAdded);
        out.writeLong(durationSecs);
    }

    private void writeDate(Parcel out, @Nullable Date date) {
        out.writeLong(date != null ? date.getTime() : Long.MIN_VALUE);
    }

    @Nullable
    public String getArtist() {
        return artist;
    }

    @Nullable
    Date getDateAdded() {
        return dateAdded;
    }

    public long getDurationSecs() {
        return durationSecs;
    }

    public BasicAudioModel toBasic() {
        return new BasicAudioModel(getAudioLocation(), getName());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        FullAudioModel that = (FullAudioModel) o;
        return durationSecs == that.durationSecs &&
                Objects.equals(artist, that.artist) &&
                Objects.equals(dateAdded, that.dateAdded);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), artist);
    }

    @Override
    @Nonnull
    public String toString() {
        return "FullAudioModel{" +
                "artist='" + artist + '\'' +
                ", dateAdded=" + dateAdded +
                ", durationSecs=" + durationSecs +
                ", name='" + name + '\'' +
                '}';
    }
}
