
package com.android.camera.ui.focus;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import com.android.camera.debug.Log;
import com.android.camera.debug.Log.Tag;
import com.android.camera2.R;

public class RotateFocusRingView extends View implements FocusRing {
    private static final Tag TAG = new Tag("RotateFocusRingView");

    private final static int FOCUS_DURATION_MS = 500;
    private final static int FOCUS_HIDE_DURATION_MS = 500;
    private final static int FOCUS_INDICATOR_ROTATION_DEGREES = 50;

    private Drawable mFocusOuterRing;
    private Drawable mFocusIndicator;

    private int mFocusOuterRingSize;
    private int mFocusIndicatorSize;

    private ValueAnimator mPassiveFocusValueAnimator;
    private ValueAnimator mActiveFocusValueAnimator;

    private int mAngle;

    private RectF mPreviewSize;
    private Point mFocusCenter;
    private boolean isFirstDraw;
    private boolean mDrawFocus;

    private Handler mHandler;

    private Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            mDrawFocus = false;
            invalidate();
        }
    };

    public RotateFocusRingView(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        // TODO Auto-generated constructor stub
        init();
    }

    public RotateFocusRingView(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // TODO Auto-generated constructor stub
        init();
    }

    public RotateFocusRingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        init();
    }

    public RotateFocusRingView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        init();
    }

    private void init() {
        mFocusIndicator = getResources().getDrawable(R.drawable.focus_ring_touch_inner);
        mFocusIndicatorSize = getResources().getDimensionPixelSize(R.dimen.focus_inner_ring_size);
        mFocusOuterRing = getResources().getDrawable(R.drawable.focus_ring_touch_outer);
        mFocusOuterRingSize = getResources().getDimensionPixelSize(R.dimen.focus_outer_ring_size);
        isFirstDraw = true;

        mHandler = new Handler();
    }

    @Override
    public boolean isPassiveFocusRunning() {
        // TODO Auto-generated method stub
        Log.d(TAG, "isPassiveFocusRunning");
        return mPassiveFocusValueAnimator != null && mPassiveFocusValueAnimator.isRunning();
    }

    @Override
    public boolean isActiveFocusRunning() {
        // TODO Auto-generated method stub
        Log.d(TAG, "isActiveFocusRunning");
        return mActiveFocusValueAnimator != null && mActiveFocusValueAnimator.isRunning();
    }

    @Override
    public void startPassiveFocus() {
        // TODO Auto-generated method stub
        Log.d(TAG, "startPassiveFocus");
        mHandler.removeCallbacks(mHideRunnable);
        mDrawFocus = true;

        if (mActiveFocusValueAnimator != null && mActiveFocusValueAnimator.isRunning()) {
            mActiveFocusValueAnimator.cancel();
            mActiveFocusValueAnimator = null;
        }
        if (mPassiveFocusValueAnimator != null && mPassiveFocusValueAnimator.isRunning()) {
            mPassiveFocusValueAnimator.cancel();
            mPassiveFocusValueAnimator = null;
        }

        mPassiveFocusValueAnimator = ValueAnimator.ofInt(0, FOCUS_INDICATOR_ROTATION_DEGREES);
        mPassiveFocusValueAnimator.setDuration(FOCUS_DURATION_MS);
        mPassiveFocusValueAnimator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationCancel(Animator arg0) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onAnimationEnd(Animator arg0) {
                // TODO Auto-generated method stub
                hideFocus(FOCUS_HIDE_DURATION_MS);
            }

            @Override
            public void onAnimationRepeat(Animator arg0) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onAnimationStart(Animator arg0) {
                // TODO Auto-generated method stub
            }

        });

        mPassiveFocusValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                // TODO Auto-generated method stub
                mAngle = (Integer) animator.getAnimatedValue();
                invalidate();
            }
        });

        mPassiveFocusValueAnimator.start();
    }

    @Override
    public void startActiveFocus() {
        // TODO Auto-generated method stub
        Log.d(TAG, "startActiveFocus");
        mHandler.removeCallbacks(mHideRunnable);
        mDrawFocus = true;

        if (mPassiveFocusValueAnimator != null && mPassiveFocusValueAnimator.isRunning()) {
            mPassiveFocusValueAnimator.cancel();
            mPassiveFocusValueAnimator = null;
        }
        if (mActiveFocusValueAnimator != null && mActiveFocusValueAnimator.isRunning()) {
            mActiveFocusValueAnimator.cancel();
            mActiveFocusValueAnimator = null;
        }

        mActiveFocusValueAnimator = ValueAnimator.ofInt(0, FOCUS_INDICATOR_ROTATION_DEGREES);
        mActiveFocusValueAnimator.setDuration(FOCUS_DURATION_MS);
        mActiveFocusValueAnimator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationCancel(Animator arg0) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onAnimationEnd(Animator arg0) {
                // TODO Auto-generated method stub
                hideFocus(FOCUS_HIDE_DURATION_MS);
            }

            @Override
            public void onAnimationRepeat(Animator arg0) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onAnimationStart(Animator arg0) {
                // TODO Auto-generated method stub
            }

        });

        mActiveFocusValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                // TODO Auto-generated method stub
                mAngle = (Integer) animator.getAnimatedValue();
                invalidate();
            }
        });

        mActiveFocusValueAnimator.start();
    }

    @Override
    public void stopFocusAnimations() {
        // TODO Auto-generated method stub
        Log.d(TAG, "stopFocusAnimations");
        mHandler.removeCallbacks(mHideRunnable);

        if (mActiveFocusValueAnimator != null && mActiveFocusValueAnimator.isRunning()) {
            mActiveFocusValueAnimator.cancel();
            mActiveFocusValueAnimator = null;
        }
        if (mPassiveFocusValueAnimator != null && mPassiveFocusValueAnimator.isRunning()) {
            mPassiveFocusValueAnimator.cancel();
            mPassiveFocusValueAnimator = null;
        }
        mDrawFocus = false;
        invalidate();
    }

    @Override
    public void setFocusLocation(float viewX, float viewY) {
        // TODO Auto-generated method stub
        Log.d(TAG, "setFocusLocation");
        mFocusCenter.x = (int) viewX;
        mFocusCenter.y = (int) viewY;
        invalidate();
    }

    @Override
    public void centerFocusLocation() {
        // TODO Auto-generated method stub
        Log.d(TAG, "centerFocusLocation");
        mFocusCenter = computeCenter();
        invalidate();
    }

    @Override
    public void setRadiusRatio(float ratio) {
        // TODO Auto-generated method stub
        Log.d(TAG, "setRadiusRatio");
    }

    @Override
    public void configurePreviewDimensions(RectF previewArea) {
        // TODO Auto-generated method stub
        Log.d(TAG, "configurePreviewDimensions");
        mPreviewSize = previewArea;

        if (!isFirstDraw) {
            mFocusCenter = computeCenter();
        }

        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {

        if (!mDrawFocus) {
            return;
        }
        if (isFirstDraw) {
            isFirstDraw = false;
            mFocusCenter = computeCenter();
        }

        if (mPreviewSize != null) {
            canvas.clipRect(mPreviewSize, Region.Op.REPLACE);
        }

        calculateBounds();

        mFocusOuterRing.draw(canvas);
        canvas.save();
        canvas.rotate(mAngle, mFocusCenter.x, mFocusCenter.y);
        mFocusIndicator.draw(canvas);
        canvas.restore();

    }

    private Point computeCenter() {
        if (mPreviewSize != null && (mPreviewSize.width() * mPreviewSize.height() > 0.01f)) {
            return new Point((int) mPreviewSize.centerX(), (int) mPreviewSize.centerY());
        }
        return new Point(getWidth() / 2, getHeight() / 2);
    }

    private void calculateBounds() {
        mFocusOuterRing.setBounds(mFocusCenter.x - mFocusOuterRingSize / 2,
                mFocusCenter.y - mFocusOuterRingSize / 2, mFocusCenter.x + mFocusOuterRingSize / 2,
                mFocusCenter.y + mFocusOuterRingSize / 2);
        mFocusIndicator.setBounds(mFocusCenter.x - mFocusIndicatorSize / 2,
                mFocusCenter.y - mFocusIndicatorSize / 2, mFocusCenter.x + mFocusIndicatorSize / 2,
                mFocusCenter.y + mFocusIndicatorSize / 2);
    }

    private void hideFocus(long time) {
        mHandler.removeCallbacks(mHideRunnable);
        mHandler.postDelayed(mHideRunnable, time);
    }

}
