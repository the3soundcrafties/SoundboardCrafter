package de.soundboardcrafter.model;

import androidx.annotation.NonNull;

import java.text.CollationKey;
import java.text.Collator;
import java.util.Objects;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Can compare {@link String}s based on the system's {@link java.util.Locale}, like
 * a {@link Collator} does - however, this class is thread-safe.
 * Does not take into account leading and trailing whitespace.
 */
@ThreadSafe
public class ThreadSafeCollator implements java.util.Comparator<String> {
    private static final ThreadSafeCollator INSTANCE
            = new ThreadSafeCollator();

    @NonNull
    private Collator notThreadSafe = Collator.getInstance();

    public static ThreadSafeCollator getInstance() {
        return INSTANCE;
    }

    private ThreadSafeCollator() {
    }

    @Override
    public synchronized int compare(@NonNull String one, @NonNull String other) {
        return notThreadSafe.compare(one.trim(), other.trim());
    }

    /**
     * Returns a  {@link CollationKey} for fast {@link java.util.Locale}-aware comparisons.
     * {@link CollationKey}s might <i>not</i> be {@link java.io.Serializable}.
     * {@link CollationKey}s are only valid for comparison when they are based on then same
     * {@link Collator}.
     */
    public synchronized CollationKey getCollationKey(@NonNull String source) {
        return notThreadSafe.getCollationKey(source.trim());
    }

    @Override
    public synchronized boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ThreadSafeCollator that = (ThreadSafeCollator) o;
        return Objects.equals(notThreadSafe, that.notThreadSafe);
    }

    @Override
    public synchronized int hashCode() {
        return Objects.hash(notThreadSafe);
    }
}
