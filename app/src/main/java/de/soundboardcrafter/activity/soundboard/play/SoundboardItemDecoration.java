package de.soundboardcrafter.activity.soundboard.play;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SoundboardItemDecoration extends RecyclerView.ItemDecoration {
    private final int verticalSpacing;
    private final int horizontalSpacing;

    SoundboardItemDecoration(int verticalSpacing, int horizontalSpacing) {
        this.verticalSpacing = verticalSpacing;
        this.horizontalSpacing = horizontalSpacing;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        int columns = 1;
        if (parent.getLayoutManager() instanceof GridLayoutManager) {
            GridLayoutManager gridLayoutManager = (GridLayoutManager) parent.getLayoutManager();
            columns = gridLayoutManager.getSpanCount();
        }

        int position = parent.getChildAdapterPosition(view);
        if (position < columns) {
            outRect.top = verticalSpacing;
        }

        if (position % columns == 0) {
            outRect.left = horizontalSpacing;
        }

        outRect.right = horizontalSpacing;
        outRect.bottom = verticalSpacing;
    }
}
