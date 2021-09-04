package de.soundboardcrafter.model;

import static com.google.common.collect.ImmutableList.toImmutableList;

import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableList;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * A soundboard that might be selected.
 */
public class SelectableModel<T> {
    private final T model;
    private boolean selected;

    public static <T> Comparator<SelectableModel<T>> byModel(Comparator<T> comparator) {
        return (one, other) -> {
            final int modelsCompared = comparator.compare(one.model, other.model);
            if (modelsCompared != 0) {
                return modelsCompared;
            }

            if (one.selected) {
                if (other.selected) {
                    return 0;
                }
                return -1;
            }

            if (other.selected) {
                return 1;
            }

            return 0;
        };
    }

    public static <T> ImmutableList<SelectableModel<T>> uncheckAll(List<? extends T> models) {
        return models.stream()
                .map(m -> new SelectableModel<T>(m, false))
                .collect(toImmutableList());
    }

    public SelectableModel(T model, boolean selected) {
        this.model = model;
        this.selected = selected;
    }

    public T getModel() {
        return model;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SelectableModel<?> that = (SelectableModel<?>) o;
        return selected == that.selected &&
                Objects.equals(model, that.model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(model, selected);
    }

    @Override
    @NonNull
    public String toString() {
        return "SelectableModel{" +
                "object=" + model +
                ", selected=" + selected +
                '}';
    }
}
