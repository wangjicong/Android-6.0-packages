package com.sprd.ext.gestures;

import android.graphics.PointF;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;

import com.android.sprdlauncher3.Launcher;

import java.util.ArrayList;

public class LauncherRootViewGestures {

    private Launcher mLauncher;

    private static final float MIN_IDENTIFY_DISTANCE = 10f;
    private static final float ZOOM_DISTANCE = 100f;
    private enum FingerMode {NONE, ONE_FINGER_DRAG, TWO_FINGERS_DRAG, TWO_FINGERS_ZOOM};
    private FingerMode mode = FingerMode.NONE;
    //You can add different gestures here
    public static enum Gesture {NONE, ONE_FINGER_DRAG_UP, ONE_FINGER_DRAG_DOWN, ONE_FINGER_DRAG_LEFT, ONE_FINGER_DRAG_RIGHT,
                        TWO_FINGER_DRAG_UP, TWO_FINGER_DRAG_DOWN, TWO_FINGER_DRAG_LEFT, TWO_FINGER_DRAG_RIGHT,
                        TWO_FINGER_ZOOM_IN, TWO_FINGER_ZOOM_OUT};

    private PointF startPoint = new PointF();
    private PointF midPoint = new PointF();
    private float oriDis = 0f;
    private ArrayList<OnGestureListener> mOnGestureListeners = new ArrayList<OnGestureListener>();

    public interface OnGestureListener{
        boolean onGesture(Gesture gesture);
    }

    public LauncherRootViewGestures(Launcher mLauncher) {
        this.mLauncher = mLauncher;
    }

    public boolean onTouchEvent(MotionEvent event) {
        //Do nothing
        if(!mLauncher.isIdleMode()){
            return false;
        }

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
            startPoint.set(event.getX(), event.getY());
            mode = FingerMode.ONE_FINGER_DRAG;
            break;
        case MotionEvent.ACTION_POINTER_DOWN:
            oriDis = distance(event);
            midPoint = middle(event);
            if (oriDis > MIN_IDENTIFY_DISTANCE) {
                mode = FingerMode.TWO_FINGERS_ZOOM;
            }else{
                mode = FingerMode.TWO_FINGERS_DRAG;
            }
            break;
        case MotionEvent.ACTION_UP:
            mode = FingerMode.NONE;
            break;
        case MotionEvent.ACTION_POINTER_UP:
            mode = FingerMode.ONE_FINGER_DRAG;
            break;
        case MotionEvent.ACTION_MOVE:
            Gesture gesture = Gesture.NONE;
            if (mode == FingerMode.ONE_FINGER_DRAG) {
                //TODO one finger drag
            } else if (mode == FingerMode.TWO_FINGERS_DRAG) {
                //TODO two fingers drag
            } else if (mode == FingerMode.TWO_FINGERS_ZOOM) {
                float newDist = distance(event);
                if ((oriDis - newDist) > ZOOM_DISTANCE) {
                    //TODO two fingers zoom in
                    gesture = Gesture.TWO_FINGER_ZOOM_IN;

                }else if((newDist - oriDis) > ZOOM_DISTANCE){
                    //TODO two fingers zoom out
                    gesture = Gesture.TWO_FINGER_ZOOM_OUT;
                }
            }
            return notifyListeners(gesture);
        }
        return false;
    }

    private boolean notifyListeners(Gesture gesture) {
        boolean result = false;
        for (OnGestureListener listener : mOnGestureListeners) {
            if (listener != null) {
                result |= listener.onGesture(gesture);
            }
        }
        return result;
    }

    public void registerOnGestureListener(OnGestureListener listener) {
        if (mOnGestureListeners.contains(listener)) {
            return;
        }
        mOnGestureListeners.add(listener);
    }

    public void unregisterOnGestureListener(OnGestureListener listener) {
        mOnGestureListeners.remove(listener);
    }

    private float distance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }

    private PointF middle(MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        return new PointF(x / 2, y / 2);
    }
}