package de.soundboardcrafter.de.soundboardcrafter.widget;

import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A {@link GridLayoutManager} which emulates a {@link android.widget.GridView}'s
 * <code>auto_fit</code> functionality, automatically choosing the appropriate
 * number of columns based on column width and screen size.
 */
// See https://stackoverflow.com/questions/26666143/recyclerview-gridlayoutmanager-how-to-auto-detect-span-count
public class GridAutofitLayoutManager extends GridLayoutManager {
    private int columnWidth;
    private final int spacing;
    private boolean isColumnWidthChanged = true;
    private int lastWidth;
    private int lastHeight;

    public GridAutofitLayoutManager(@NonNull final Context context, final int columnWidth) {
        this(context, columnWidth, 0);
    }


    public GridAutofitLayoutManager(@NonNull final Context context, final int columnWidth,
                                    final int spacing) {
        /* Initially set spanCount to 1, will be changed automatically later. */
        super(context, 1);
        this.spacing = spacing;
        setColumnWidth(checkedColumnWidth(context, columnWidth));
    }

    public GridAutofitLayoutManager(
            @NonNull final Context context,
            final int columnWidth,
            final int orientation,
            final boolean reverseLayout) {
        this(context, columnWidth, orientation, reverseLayout, 0);
    }

    private GridAutofitLayoutManager(
            @NonNull final Context context,
            final int columnWidth,
            final int orientation,
            final boolean reverseLayout,
            final int spacing) {

        /* Initially set spanCount to 1, will be changed automatically later. */
        super(context, 1, orientation, reverseLayout);
        this.spacing = spacing;
        setColumnWidth(checkedColumnWidth(context, columnWidth));
    }

    private int checkedColumnWidth(@NonNull final Context context, int columnWidth) {
        if (columnWidth <= 0) {
            /* Set default columnWidth value (48dp here). It is better to move this constant
            to static constant on top, but we need context to convert it to dp, so can't really
            do so. */
            columnWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                    context.getResources().getDisplayMetrics());
        }
        return columnWidth;
    }

    private void setColumnWidth(final int newColumnWidth) {
        if (newColumnWidth > 0 && newColumnWidth != columnWidth) {
            columnWidth = newColumnWidth;
            isColumnWidthChanged = true;
        }
    }

    @Override
    public void onLayoutChildren(@NonNull final RecyclerView.Recycler recycler, @NonNull final RecyclerView.State state) {
        final int width = getWidth();
        final int height = getHeight();
        if (columnWidth > 0 && width > 0 && height > 0 && (isColumnWidthChanged || lastWidth != width || lastHeight != height)) {
            // spanCount * (columnWidth + spacing) + paddingRight + paddingLeft + spacing = width
            // => spanCount * (columnWidth + spacing) = width - paddingRight - paddingLeft - spacing
            // => spanCount = (width - paddingRight - paddingLeft - spacing) / (columnWidth + spacing)

            final int totalSpace;
            if (getOrientation() == RecyclerView.VERTICAL) {
                totalSpace = width - getPaddingRight() - getPaddingLeft() - spacing;
            } else {
                totalSpace = height - getPaddingTop() - getPaddingBottom() - spacing;
            }
            final int spanCount = Math.max(1, totalSpace / (columnWidth + spacing));

            setSpanCount(spanCount);
            isColumnWidthChanged = false;
        }
        lastWidth = width;
        lastHeight = height;
        super.onLayoutChildren(recycler, state);
    }
}