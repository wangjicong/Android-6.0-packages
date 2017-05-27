/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.app;

import android.content.Context;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import com.android.gallery3d.R;

/**
 * The playback controller for the Movie Player.
 */
public class MovieControllerOverlay extends CommonControllerOverlay implements
        AnimationListener {

    private boolean hidden;

    private final Handler handler;
    private final Runnable startHidingRunnable;
    private final Animation hideAnimation;
    private static String TAG="MovieControllerOverlay";
    public MovieControllerOverlay(Context context) {
        super(context);

        handler = new Handler();
        startHidingRunnable = new Runnable() {
                @Override
            public void run() {
                startHiding();
            }
        };

        hideAnimation = AnimationUtils.loadAnimation(context, R.anim.player_out);
        hideAnimation.setAnimationListener(this);

        hide();
    }

    @Override
    protected void createTimeBar(Context context) {
        mTimeBar = new TimeBar(context, this);
    }

    @Override
    public void hide() {
        Log.d(TAG, "Hide is work!");
        boolean wasHidden = hidden;
        hidden = true;
        super.hide();
        if (mListener != null && wasHidden != hidden) {
            mListener.onHidden();
        }
        Log.d(TAG, "Hide is work end!");
    }


    @Override
    public void show() {
        Log.d(TAG, "show() is work start!");
        boolean wasHidden = hidden;
        hidden = false;
        super.show();
        /** SPRD:Bug474600  improve video control functions @{ */
        updateViews();
        setVisibility(View.VISIBLE);
        setFocusable(false);
        /** @} */
        if (mListener != null && wasHidden != hidden) {
            mListener.onShown();
        }
        maybeStartHiding();
        Log.d(TAG, "show() is work end!");
    }

    private void maybeStartHiding() {
        Log.d(TAG, "maybeStartHiding() is work start!");
        cancelHiding();
        if (mState == State.PLAYING) {
            handler.postDelayed(startHidingRunnable, 2500);
        }
        Log.d(TAG, "maybeStartHiding() is work end!");
    }

    private void startHiding() {
        Log.d(TAG, "startHiding() is work start!");
        startHideAnimation(mBackground);
        startHideAnimation(mTimeBar);
        startHideAnimation(mPlayPauseReplayView);
        /** SPRD:bug 474614: porting float play @{ */
        // old bug info:339523
        if (floatPlayButton != null) {
            startHideAnimation(floatPlayButton);
        }
        /** @} */
//        hidden = true;
        Log.d(TAG, "startHiding() is work end!");
    }

    private void startHideAnimation(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.startAnimation(hideAnimation);
        }
    }

    private void cancelHiding() {
        Log.d(TAG, "cancelHiding() is work start!");
        handler.removeCallbacks(startHidingRunnable);
        mBackground.setAnimation(null);
        mTimeBar.setAnimation(null);
        mPlayPauseReplayView.setAnimation(null);
//      /** SPRD: add the view @{ */
//      playControllerView.setAnimation(null);
//      volumeBar.setAnimation(null);
//      volumeButton.setAnimation(null);
//      /** @ } */
      if(floatPlayButton != null)
          floatPlayButton.setAnimation(null);
      Log.d(TAG, "cancelHiding() is work start end!");
    }

    @Override
    public void onAnimationStart(Animation animation) {
        // Do nothing.
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
        // Do nothing.
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        hide();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (hidden) {
            show();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (super.onTouchEvent(event)) {
            return true;
        }

        if (hidden) {
            show();
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                cancelHiding();
                if (mState == State.PLAYING || mState == State.PAUSED) {
                    mListener.onPlayPause();
                }
                break;
            case MotionEvent.ACTION_UP:
                maybeStartHiding();
                break;
        }
        return true;
    }

    @Override
    protected void updateViews() {
        if (hidden) {
            return;
        }
        super.updateViews();
    }

    // TimeBar listener

    @Override
    public void onScrubbingStart() {
        cancelHiding();
        super.onScrubbingStart();
    }

    @Override
    public void onScrubbingMove(int time) {
        cancelHiding();
        super.onScrubbingMove(time);
    }

    @Override
    public void onScrubbingEnd(int time, int trimStartTime, int trimEndTime) {
        maybeStartHiding();
        super.onScrubbingEnd(time, trimStartTime, trimEndTime);
    }
   /**SPRD:Bug474600 improve video control functions@{*/
    @Override
    public void setControlButtonEnableForStop(boolean enable) {
        // TODO Auto-generated method stub
        mNextVideoView.setEnabled(enable);
        mPrevVideoView.setEnabled(enable);
    }

    @Override
    public void clearPlayState() {
        // TODO Auto-generated method stub
        mPlayPauseReplayView.setVisibility(View.INVISIBLE);
        mLoadingView.setVisibility(View.INVISIBLE);
        show();
    }

    @Override
    public void setLiveMode() {
        // TODO Auto-generated method stub
        mRewButton.setImageResource(R.drawable.ic_vidcontrol_rew_false);
        mRewButton.setClickable(false);
        mFfwdButton.setImageResource(R.drawable.ic_vidcontrol_ffwd_false);
        mFfwdButton.setClickable(false);
        mTimeBar.setEnabled(false);
        isLiveMode = true;
    }
    public void setNotLiveMode() {
        isLiveMode = false;
    }

    /** SPRD:bug 474614: porting float play @{ */
    // old bug info:339523 begin
    public void showFloatPlayerButton(boolean show) {
        if (floatPlayButton == null)
            return;
        if (show) {
            floatPlayButton.setVisibility(View.VISIBLE);
        } else {
            floatPlayButton.setVisibility(View.INVISIBLE);
        }
    }
    // bug 339523 end
    /** @} */

    public void setStopButton(boolean enable) {
        mStopButView.setEnabled(enable);
    }

    public void setscrubbing() {
        mTimeBar.setscrubbing();
    }

    public void hideToShow() {
        maybeStartHiding();
    }
    /**   @}*/
}
