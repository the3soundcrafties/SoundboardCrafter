package de.soundboardcrafter.model;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.text.CollationKey;
import java.util.Comparator;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A soundboard, that is a keyboard you can play sounds with.
 * The application supports several soundboards.
 * <p></p>
 * <code>Soundboards</code>s are not thread-safe. So it might be necessary to use
 * appropriate synchronization.
 */
@ParametersAreNonnullByDefault
public class Soundboard extends AbstractEntity {
    public static final Comparator<Soundboard> PROVIDED_LAST_THEN_BY_COLLATION_KEY =
            (one, other) -> {
                if (!one.isProvided() && other.isProvided()) {
                    return -1;
                }

                if (one.isProvided() && !other.isProvided()) {
                    return 1;
                }

                return one.collationKey.compareTo(other.collationKey);
            };

    private static final Pattern LEADING_NUMBERS_REGEX = Pattern.compile("(\\d*\\s*)(.*)");

    private static final ThreadSafeCollator nameCollator =
            ThreadSafeCollator.getInstance();

    /**
     * Name of the soundboard - including leading numbers.
     */
    @NonNull
    private String name;

    /**
     * Whether the soundboard has been built automatically from provided sounds.
     * A <i>provided</i> soundboard cannot be deleted.
     */
    private final boolean provided;

    // CollationKeys may not be serializable
    @NonNull
    private transient CollationKey collationKey;

    /**
     * Creates a new soundboard that is not provided (a <i>custom</i> soundboard).
     */
    public Soundboard(@NonNull String name) {
        this(name, false);
    }

    public Soundboard(@NonNull String name, boolean provided) {
        this(UUID.randomUUID(), name, provided);
    }

    public Soundboard(UUID id, @NonNull String name, boolean provided) {
        super(id);
        setName(checkNotNull(name, "name is null"));
        this.provided = provided;
    }

    /**
     * Returns the name for display - also leading numbers are skipped <i>for provided
     * soundboards</i>.
     * <p></p>
     * For  sorting purposes better use the {@link #collationKey}.
     *
     * @see #getFullName()
     */
    @NonNull
    public String getDisplayName() {
        return provided ? skipLeadingNumbers(name) : name;
    }

    @NonNull
    private String skipLeadingNumbers(@NonNull String name) {
        final Matcher matcher = LEADING_NUMBERS_REGEX.matcher(name);
        if (!matcher.find()) {
            return name;
        }

        return requireNonNull(matcher.group(2));
    }

    /**
     * Returns the full name - also includes leading numbers.
     * <p></p>
     * For  sorting purposes better use the {@link #collationKey}.
     *
     * @see #getDisplayName()
     */
    @NonNull
    public String getFullName() {
        return name;
    }

    public final void setName(@NonNull String name) {
        checkNotNull(name, "name is null");
        this.name = name;

        setCollationKey();
    }

    private void setCollationKey() {
        collationKey = nameCollator.getCollationKey(name);
    }

    public boolean isProvided() {
        return provided;
    }

    private void writeObject(final java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(final java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        setCollationKey();
    }

    @Override
    public @Nonnull
    String toString() {
        return "Soundboard{" +
                "id=" + getId() +
                ", name='" + name +
                (provided ? " (provided)" : "") +
                '}';
    }
}
