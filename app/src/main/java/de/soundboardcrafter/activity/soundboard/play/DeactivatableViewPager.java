package de.soundboardcrafter.activity.soundboard.play;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

/**
 * A {@link ViewPager} in which the paging can be disabled.
 */
public class DeactivatableViewPager extends ViewPager {
    // TODO Replace with solution without subclassing (ViewPager2 will be final.)
    // TODO Replace with ViewPager2

    private boolean pagingEnabled = true;

    public DeactivatableViewPager(Context context) {
        super(context);
    }

    public DeactivatableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return pagingEnabled && super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return pagingEnabled && super.onInterceptTouchEvent(event);
    }

    void setPagingEnabled(boolean b) {
        pagingEnabled = b;
    }
}
