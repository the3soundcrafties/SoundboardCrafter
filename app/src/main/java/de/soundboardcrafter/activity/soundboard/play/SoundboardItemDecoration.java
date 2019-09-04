package de.soundboardcrafter.activity.soundboard.play;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

// See https://stackoverflow.com/questions/30524599/items-are-not-the-same-width-when-using-recyclerview-gridlayoutmanager-to-make-c .
class SoundboardItemDecoration extends RecyclerView.ItemDecoration {
    private final int verticalSpacing;
    private final int horizontalSpacing;
    private boolean includeEdge;

    SoundboardItemDecoration(int verticalSpacing, int horizontalSpacing, boolean includeEdge) {
        this.verticalSpacing = verticalSpacing;
        this.horizontalSpacing = horizontalSpacing;
        this.includeEdge = includeEdge;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        // Only handle the vertical situation
        int position = parent.getChildAdapterPosition(view);
        if (parent.getLayoutManager() instanceof GridLayoutManager) {
            GridLayoutManager layoutManager = (GridLayoutManager) parent.getLayoutManager();
            int spanCount = layoutManager.getSpanCount();
            int column = position % spanCount;
            getGridItemOffsets(outRect, position, column, spanCount);
        } else if (parent.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) parent.getLayoutManager();
            int spanCount = layoutManager.getSpanCount();
            StaggeredGridLayoutManager.LayoutParams lp = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
            int column = lp.getSpanIndex();
            getGridItemOffsets(outRect, position, column, spanCount);
        } else if (parent.getLayoutManager() instanceof LinearLayoutManager) {
            outRect.left = horizontalSpacing;
            outRect.right = horizontalSpacing;
            if (includeEdge) {
                if (position == 0) {
                    outRect.top = verticalSpacing;
                }
                outRect.bottom = verticalSpacing;
            } else {
                if (position > 0) {
                    outRect.top = verticalSpacing;
                }
            }
        }
    }

    private void getGridItemOffsets(Rect outRect, int position, int column, int spanCount) {
        if (includeEdge) {
            outRect.left = horizontalSpacing * (spanCount - column) / spanCount;
            outRect.right = horizontalSpacing * (column + 1) / spanCount;
            if (position < spanCount) {
                outRect.top = verticalSpacing;
            }
            outRect.bottom = verticalSpacing;
        } else {
            outRect.left = horizontalSpacing * column / spanCount;
            outRect.right = horizontalSpacing * (spanCount - 1 - column) / spanCount;
            if (position >= spanCount) {
                outRect.top = verticalSpacing;
            }
        }
    }
}
