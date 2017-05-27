/*
 * Copyright (C) 2012 The Android Open Source Project
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
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation.AnimationListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gallery3d.R;

/**
 * The common playback controller for the Movie Player or Video Trimming.
 */
public abstract class CommonControllerOverlay extends FrameLayout implements
        ControllerOverlay,
        OnClickListener,
        TimeBar.Listener,
        AnimationListener {

    protected enum State {
        PLAYING,
        PAUSED,
        ENDED,
        ERROR,
        LOADING
    }

    private static final float ERROR_MESSAGE_RELATIVE_PADDING = 1.0f / 6;
    protected Listener mListener;

    protected final View mBackground;
    protected TimeBar mTimeBar;

    protected View mMainView;
    protected final LinearLayout mLoadingView;
    protected final TextView mErrorView;
    protected final ImageView mPlayPauseReplayView;

    protected State mState;

    protected boolean mCanReplay = true;
    /** SPRD:bug 474614: porting float play @{ */
    // old bug info 339523 begin
    protected ImageView floatPlayButton;
    // bug 339523 end
    /** @} */
    public void setSeekable(boolean canSeek) {
        mTimeBar.setSeekable(canSeek);
    }
    /** SPRD:Bug474600 improve video control functions
     * add  new image parameters for Play @{ */
    protected LinearLayout playControllerView;
    protected ImageView mStopButView;
    protected ImageView mNextVideoView;
    protected ImageView mPrevVideoView;
    protected ImageView mFfwdButton;
    protected ImageView mRewButton;
    protected boolean isLiveMode = false;
    private boolean hidden;
    /**    @} */
    /**SPRD:Bug474600 improve video control functions
     * add new parameters @{*/
    private final String TAG = "CommonControllerOverlay";
    /**   @}*/
    public CommonControllerOverlay(Context context) {
        super(context);

        mState = State.LOADING;
        // TODO: Move the following layout code into xml file.
        LayoutParams wrapContent =
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        LayoutParams matchParent =
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        /* SPRD: bug 540192,let the layout adapt to the screen @} */
        LayoutParams buttonParam = new LayoutParams(
                getIntFromDimens(R.dimen.movie_button_size),
                getIntFromDimens(R.dimen.movie_button_size));
        /* @} */

        mBackground = new View(context);
        mBackground.setBackgroundColor(context.getResources().getColor(R.color.darker_transparent));
        addView(mBackground, matchParent);

        mLoadingView = new LinearLayout(context);
        mLoadingView.setOrientation(LinearLayout.VERTICAL);
        mLoadingView.setGravity(Gravity.CENTER_HORIZONTAL);
        ProgressBar spinner = new ProgressBar(context);
        spinner.setIndeterminate(true);
        mLoadingView.addView(spinner, wrapContent);
        TextView loadingText = createOverlayTextView(context);
        loadingText.setText(R.string.loading_video);
        mLoadingView.addView(loadingText, wrapContent);
        addView(mLoadingView, wrapContent);
        /** SPRD:Bug474600 improve video control functions @{ */
        playControllerView = new LinearLayout(context);
        playControllerView.setOrientation(LinearLayout.HORIZONTAL);
        playControllerView.setGravity(Gravity.CENTER_HORIZONTAL);
        playControllerView.setLayoutParams(wrapContent);
        playControllerView.setClickable(true);
        /** @} */
        /**SPRD:Bug545844 modify by old bug 539690,in Arabic system,the video player back & forward arrows are reversed */
        playControllerView.setLayoutDirection(LAYOUT_DIRECTION_LTR);
        /** @} */

        mPlayPauseReplayView = new ImageView(context);
        mPlayPauseReplayView.setImageResource(R.drawable.ic_vidcontrol_play);
        mPlayPauseReplayView.setContentDescription(
                context.getResources().getString(R.string.accessibility_play_video));
        mPlayPauseReplayView.setBackgroundResource(R.drawable.bg_vidcontrol);
        mPlayPauseReplayView.setScaleType(ScaleType.CENTER);
        mPlayPauseReplayView.setFocusable(true);
        mPlayPauseReplayView.setClickable(true);
        mPlayPauseReplayView.setOnClickListener(this);
        addView(mPlayPauseReplayView, wrapContent);
        /** SPRD:Bug474600 improve video control functions @{ */
        mStopButView = new ImageView(context);
        mStopButView.setImageResource(R.drawable.ic_vidcontrol_stop);
        /* SPRD: bug 540192,let the layout adapt to the screen @{ */
        mStopButView.setScaleType(ScaleType.FIT_CENTER);
        mStopButView.setLayoutParams(buttonParam);
        /* @} */
        mStopButView.setFocusable(true);
        mStopButView.setClickable(true);
        mStopButView.setOnClickListener(this);
        mStopButView.setOnClickListener(mStopListener);

        mNextVideoView = new ImageView(context);
        mNextVideoView.setImageResource(R.drawable.ic_vidcontrol_next);
        /* SPRD: bug 540192,let the layout adapt to the screen @{ */
        mNextVideoView.setScaleType(ScaleType.FIT_CENTER);
        mNextVideoView.setLayoutParams(buttonParam);
        /* @} */
        mNextVideoView.setFocusable(true);
        mNextVideoView.setClickable(true);
        mNextVideoView.setOnClickListener(nextListener);

        mPrevVideoView = new ImageView(context);
        mPrevVideoView.setImageResource(R.drawable.ic_vidcontrol_prev);
        /* SPRD: bug 540192,let the layout adapt to the screen @{ */
        mPrevVideoView.setScaleType(ScaleType.FIT_CENTER);
        mPrevVideoView.setLayoutParams(buttonParam);
        /* @} */
        mPrevVideoView.setFocusable(true);
        mPrevVideoView.setClickable(true);
        mPrevVideoView.setOnClickListener(prevListener);

        mFfwdButton = new ImageView(context);
        /* SPRD: bug 540192,let the layout adapt to the screen @{ */
        mFfwdButton.setScaleType(ScaleType.FIT_CENTER);
        mFfwdButton.setLayoutParams(buttonParam);
        /* @} */
        mFfwdButton.setFocusable(true);
        mFfwdButton.setClickable(true);
        mFfwdButton.setOnClickListener(mFfwdListener);

        mRewButton = new ImageView(context);
        /* SPRD: bug 540192,let the layout adapt to the screen @{ */
        mRewButton.setScaleType(ScaleType.FIT_CENTER);
        mRewButton.setLayoutParams(buttonParam);
        /* @} */
        mRewButton.setFocusable(true);
        mRewButton.setClickable(true);
        mRewButton.setOnClickListener(mRewListener);

        ImageView[] btns = new ImageView[]{mPrevVideoView, mRewButton, mStopButView, mFfwdButton, mNextVideoView};
        for(int i=0; i<btns.length; i++)
        {
            LinearLayout buttonLayout=new LinearLayout(context);
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
            buttonLayout.setGravity(Gravity.CENTER);
            buttonLayout.setLayoutParams(wrapContent);
            /*sunvov:dlj 20170511 add for Bug 53202  start*/
            buttonLayout.setPadding(10, 20, 10, 10);
            /*sunvov:dlj 20170511 add for Bug 53202  end*/
            buttonLayout.addView(btns[i]);
            playControllerView.addView(buttonLayout, wrapContent);
        }
        addView(playControllerView, wrapContent);
        /** @} */
        // Depending on the usage, the timeBar can show a single scrubber, or
        // multiple ones for trimming.
        createTimeBar(context);
        addView(mTimeBar, wrapContent);
        mTimeBar.setContentDescription(
                context.getResources().getString(R.string.accessibility_time_bar));
        /** SPRD:bug 474614: porting float play @{ */
        Boolean float_window = android.os.SystemProperties.getBoolean(
                "persist.sys.floating_window", true);
        Log.d(TAG, "float_window = " + float_window);
        boolean mIsFloatWindowDisabled = MovieActivity.getInstance().isFloatWindowDisabled();
        Log.d(TAG, "mIsFloatWindowDisabled="+mIsFloatWindowDisabled);
        if (float_window && !mIsFloatWindowDisabled) {
            // old bug info 339523 begin
            floatPlayButton = new ImageView(context);
            floatPlayButton.setImageResource(R.drawable.ic_media_start_float);
            floatPlayButton.setScaleType(ScaleType.CENTER);
            floatPlayButton.setFocusable(true);
            floatPlayButton.setClickable(true);
            floatPlayButton.setOnClickListener(floatPlayListener);
            addView(floatPlayButton, wrapContent);
        }
        // bug 339523 end
        /** @} */
        mErrorView = createOverlayTextView(context);
        addView(mErrorView, matchParent);

        RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        setLayoutParams(params);
        hide();
    }

    abstract protected void createTimeBar(Context context);

    private TextView createOverlayTextView(Context context) {
        TextView view = new TextView(context);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(0xFFFFFFFF);
        view.setPadding(0, 15, 0, 15);
        return view;
    }

    @Override
    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    @Override
    public void setCanReplay(boolean canReplay) {
        this.mCanReplay = canReplay;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void showPlaying() {
        /** SPRD:Bug474600 improve video control functions @{ */
        Log.d(TAG, "showPlaying");
        if(isLiveMode){
            mRewButton.setImageResource(R.drawable.ic_vidcontrol_rew_false);
            mRewButton.setClickable(false);
            mFfwdButton.setImageResource(R.drawable.ic_vidcontrol_ffwd_false);
            mFfwdButton.setClickable(false);
        }
        /** @} */
        mState = State.PLAYING;
        showMainView(mPlayPauseReplayView);
    }

    @Override
    public void showPaused() {
        Log.d(TAG, "showPaused");
        mState = State.PAUSED;
        showMainView(mPlayPauseReplayView);
    }

    @Override
    public void showEnded() {
        Log.d(TAG, "showEnded()");
        mState = State.ENDED;
        if (mCanReplay) showMainView(mPlayPauseReplayView);
    }

    @Override
    public void showLoading() {
        Log.d(TAG, "showLoading");
        mState = State.LOADING;
        showMainView(mLoadingView);
    }

    @Override
    public void showErrorMessage(String message) {
        mState = State.ERROR;
        int padding = (int) (getMeasuredWidth() * ERROR_MESSAGE_RELATIVE_PADDING);
        mErrorView.setPadding(
                padding, mErrorView.getPaddingTop(), padding, mErrorView.getPaddingBottom());
        mErrorView.setText(message);
        showMainView(mErrorView);
    }

    @Override
    public void setTimes(int currentTime, int totalTime,
            int trimStartTime, int trimEndTime) {
        /**SPRD:Bug474600 improve video control functions
         *  remove @{
        mTimeBar.setTime(currentTime, totalTime, trimStartTime, trimEndTime);
        *@}
        */
        /** SPRD:Bug474600 improve video control functions @{ */
        if (isLiveMode && totalTime == -1) {
            mTimeBar.setTimeTextOnly(currentTime, totalTime, trimStartTime, trimEndTime);
        } else {
            mTimeBar.setTime(currentTime, totalTime, trimStartTime, trimEndTime);
        }
        /** @} */
    }

    public void hide() {
        Log.d(TAG, "common hide is work");
        /** SPRD:Bug474600 improve video control functions @{ */
        boolean wasHidden = hidden;
        hidden = true;
        /** @} */
        mPlayPauseReplayView.setVisibility(View.INVISIBLE);
        /** SPRD:Bug474600 improve video control functions
         * add @{ */
        playControllerView.setVisibility(View.INVISIBLE);
        /** @} */
        mLoadingView.setVisibility(View.INVISIBLE);
        mBackground.setVisibility(View.INVISIBLE);
        mTimeBar.setVisibility(View.INVISIBLE);
        setVisibility(View.INVISIBLE);
        setFocusable(true);
        requestFocus();
        /** SPRD:Bug474600 improve video control functions @{ */
        if (mListener != null && wasHidden != hidden) {
            mListener.onHidden();
        }
        /** @} */
    }

    private void showMainView(View view) {
        mMainView = view;
        mErrorView.setVisibility(mMainView == mErrorView ? View.VISIBLE : View.INVISIBLE);
        mLoadingView.setVisibility(mMainView == mLoadingView ? View.VISIBLE : View.INVISIBLE);
        mPlayPauseReplayView.setVisibility(
                mMainView == mPlayPauseReplayView ? View.VISIBLE : View.INVISIBLE);
        show();
    }

    @Override
    public void show() {
        Log.d(TAG, "common show is work!");
        /** SPRD:Bug474600 improve video control functions @{ */
        boolean wasHidden = hidden;
        hidden = false;
        /** @} */
        updateViews();
        setVisibility(View.VISIBLE);
        setFocusable(false);
        /** SPRD:Bug474600 improve video control functions @{ */
        if (mListener != null && wasHidden != hidden) {
            mListener.onShown();
        }
        /** @} */
    }

    @Override
    public void onClick(View view) {
        if (mListener != null) {
            if (view == mPlayPauseReplayView) {
                if (mState == State.ENDED) {
                    if (mCanReplay) {
                        mListener.onReplay();
                    }
                } else if (mState == State.PAUSED || mState == State.PLAYING) {
                    mListener.onPlayPause();
                }
            }
        }
    }

    /**
     * SPRD:Bug474600 improve video control functions
     * add new Listeners, such as:nextListener,
     * mStopListener,mFfwdListener,prevListener,
     * mFfwdListener,mRewListener
     * @{
     */
    private View.OnClickListener nextListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mListener.onNext();
        }
    };
    private View.OnClickListener mStopListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mListener.onStopVideo();
        }
    };
    private View.OnClickListener prevListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mListener.onPrev();
        }
    };
    private View.OnClickListener mFfwdListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mListener.onFfwd();
        }
    };
    private View.OnClickListener mRewListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mListener.onRew();
        }
    };

    /** @} */
    /** SPRD:bug 474614: porting float play @{ */
    // old bug info:339523 begin 423
    private View.OnClickListener floatPlayListener = new View.OnClickListener() {
        @Override
       public void onClick(View v) {
            Context context = getContext();
            try {
                Context packageContext = context.createPackageContext(
                        "com.sprd.videoswallpapers",
                        Context.CONTEXT_IGNORE_SECURITY);
                SharedPreferences sp = packageContext.getSharedPreferences(
                        "wallpaper", Context.MODE_WORLD_READABLE
                                | Context.MODE_MULTI_PROCESS);
                boolean isVideoWallpaper = sp.getBoolean("videowallpaper",
                        false);
                Log.d(TAG, "onClick isVideoWallpaper = " + isVideoWallpaper
                        + " ss = ");
                if (isVideoWallpaper) {
                    Log.d(TAG, "onClick isVideoWallpaper true");
                    Toast.makeText(context, R.string.can_not_open_float_video,
                            Toast.LENGTH_LONG).show();
                } else {
                    mListener.onStartFloatPlay();
                }
            } catch (NameNotFoundException e) {
                Log.d(TAG, "onClick videoswallpapers NameNotFound");
                mListener.onStartFloatPlay();
            }
        }
    };
    // bug 339523 end
     /** @} */

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (super.onTouchEvent(event)) {
            return true;
        }
        return false;
    }

    // The paddings of 4 sides which covered by system components. E.g.
    // +-----------------+\
    // | Action Bar | insets.top
    // +-----------------+/
    // | |
    // | Content Area | insets.right = insets.left = 0
    // | |
    // +-----------------+\
    // | Navigation Bar | insets.bottom
    // +-----------------+/
    // Please see View.fitSystemWindows() for more details.
    private final Rect mWindowInsets = new Rect();

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        // We don't set the paddings of this View, otherwise,
        // the content will get cropped outside window
        mWindowInsets.set(insets);
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        Rect insets = mWindowInsets;
        int pl = insets.left; // the left paddings
        int pr = insets.right;
        int pt = insets.top;
        int pb = insets.bottom;
        /** SPRD:Bug474600  improve video control functions@{ */
        int bw;
        int bh;
        bw = mTimeBar.getBarHeight();
        bh = bw;
        /** @} */

        int h = bottom - top;
        int w = right - left;
        boolean error = mErrorView.getVisibility() == View.VISIBLE;
        /**SPRD:Bug474600 improve video control functions
         * remove @{
         * @orig
        int y = h - pb;
        // Put both TimeBar and Background just above the bottom system
        // component.
        // But extend the background to the width of the screen, since we don't
        // care if it will be covered by a system component and it looks better.
        mBackground.layout(0, y - mTimeBar.getBarHeight(), w, y);
        mTimeBar.layout(pl, y - mTimeBar.getPreferredHeight(), w - pr, y);
        *@}
        */
        /** SPRD:Bug474600 improve video control functions @{ */
        mTimeBar.layout(left, h - pb - mTimeBar.getPreferredHeight(), w - pr, h
                - pb);

        playControllerView
                .layout(left, h - pb - mTimeBar.getPreferredHeight() - bh, w - pr, h - pb);
        mBackground.layout(0, h - pb - mTimeBar.getPreferredHeight() - bh, w, h
                - pb);
        /** SPRD:bug 474614: porting float play @{ */
        // old bug info: bug 339523 and bug 540192 begin
        int floatTopMargin = getIntFromDimens(R.dimen.float_top_margin);
        int floatLeftMargin = getIntFromDimens(R.dimen.float_left_margin);
        int floatButtonSize = getIntFromDimens(R.dimen.float_button_size);
        if (floatPlayButton != null) {
            floatPlayButton.layout(left + floatLeftMargin, top + floatTopMargin, left
                    + floatLeftMargin + floatButtonSize, top + floatTopMargin + floatButtonSize);
        }
        // bug 339523 end
        /** @} */
        int cx = left + (w - pr) / 2; // center x
        int playbackButtonsCenterline = top + (h - pb) / 2;
        bw = mPlayPauseReplayView.getMeasuredWidth();
        bh = mPlayPauseReplayView.getMeasuredHeight();
        mPlayPauseReplayView.layout(cx - bw / 2, playbackButtonsCenterline - bh
                / 2, cx + bw / 2, playbackButtonsCenterline + bh / 2);
        /** @} */

        /**
         * SPRD:Bug474600 improve video control functions
         * remove @{
         * @orig // Put the play/pause/next/ previous button in the center of the screen
         *       layoutCenteredView(mPlayPauseReplayView, 0, 0, w, h); @}
         */
        if (mMainView != null) {
            /**SPRD:Bug474600 improve video control functions
             *  remove @{
             * @orig
            layoutCenteredView(mMainView, 0, 0, w, h);
            *@}
            */
            layoutCenteredView(mMainView, left, top, right, bottom);
        }
    }

    private void layoutCenteredView(View view, int l, int t, int r, int b) {
        int cw = view.getMeasuredWidth();
        int ch = view.getMeasuredHeight();
        int cl = (r - l - cw) / 2;
        int ct = (b - t - ch) / 2;
        view.layout(cl, ct, cl + cw, ct + ch);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }

    protected void updateViews() {
        Log.d(TAG, "updateViews is work!");
        /** SPRD:Bug474600 improve video control functions @{ */
        if (hidden) {
            return;
        }
        /** @} */
        /** SPRD: Bug474600 improve video control functions
         *  set playControllerView,volumeBar VISIBLE @{ */
        mBackground.setVisibility(View.VISIBLE);
        mTimeBar.setVisibility(View.VISIBLE);
        playControllerView.setVisibility(View.VISIBLE);
        /**SPRD: Bug474600 improve video control functions
         * remove @{
         * @orig
        Resources resources = getContext().getResources();
        int imageResource = R.drawable.ic_vidcontrol_reload;
        String contentDescription = resources.getString(R.string.accessibility_reload_video);
        if (mState == State.PAUSED) {
            imageResource = R.drawable.ic_vidcontrol_play;
            contentDescription = resources.getString(R.string.accessibility_play_video);
        } else if (mState == State.PLAYING) {
            imageResource = R.drawable.ic_vidcontrol_pause;
            contentDescription = resources.getString(R.string.accessibility_pause_video);
        }
        mPlayPauseReplayView.setImageResource(imageResource);
        mPlayPauseReplayView.setContentDescription(contentDescription);
        mPlayPauseReplayView.setVisibility(
                (mState != State.LOADING && mState != State.ERROR &&
                !(mState == State.ENDED && !mCanReplay))
                        ? View.VISIBLE : View.GONE);
                ? View.VISIBLE : View.GONE);
         *@}
         */
        /** SPRD:Bug474600 improve video control functions @{ */
        if (isLiveMode) {
            mPlayPauseReplayView
                    .setImageResource(mState == State.PAUSED ? R.drawable.ic_vidcontrol_play
                            : mState == State.PLAYING ? R.drawable.ic_vidcontrol_stop
                                    : R.drawable.ic_vidcontrol_reload);
        } else {
            mPlayPauseReplayView
                    .setImageResource(mState == State.PAUSED ? R.drawable.ic_vidcontrol_play
                            : mState == State.PLAYING ? R.drawable.ic_vidcontrol_pause
                                    : R.drawable.ic_vidcontrol_reload);
        }

        mPlayPauseReplayView
                .setVisibility((mState != State.LOADING
                        && mState != State.ERROR && !(mState == State.ENDED && !mCanReplay)) ? View.VISIBLE
                        : View.GONE);
        /** @{ */
        requestLayout();
    }
    /** SPRD:Bug474600 improve video control functions @{ */
    public void showNextPrevBtn(boolean show){
        if(!show){
            mNextVideoView.setVisibility(View.GONE);
            mPrevVideoView.setVisibility(View.GONE);
        }else{
            mNextVideoView.setVisibility(View.VISIBLE);
            mPrevVideoView.setVisibility(View.VISIBLE);
        }
    }

    public void showRewButton(boolean show){
        if(!show){
            mRewButton.setImageResource(R.drawable.ic_vidcontrol_rew_false);
            mRewButton.setClickable(false);
        }else{
            mRewButton.setImageResource(R.drawable.ic_vidcontrol_rew);
            mRewButton.setClickable(true);
        }
    }

    public void timeBarEnable(boolean enable){
        mTimeBar.setEnabled(enable);
    }
    public void showFfwdButton(boolean show){
        if(!show){
            mFfwdButton.setImageResource(R.drawable.ic_vidcontrol_ffwd_false);
            mFfwdButton.setClickable(false);
        }else{
            mFfwdButton.setImageResource(R.drawable.ic_vidcontrol_ffwd);
            mFfwdButton.setClickable(true);
        }
    }

    public void showStopButton(boolean show){
        if(!show){
            mStopButView.setImageResource(R.drawable.ic_vidcontrol_stop_unable);
            mStopButView.setClickable(false);
        } else {
            mStopButView.setImageResource(R.drawable.ic_vidcontrol_stop);
            mStopButView.setClickable(true);
        }
    }
    public void resetTime() {
        mTimeBar.resetTime();
      }

    public void setStopButton(boolean enable){
        mStopButView.setEnabled(enable);
    }
    /** @} */
    // TimeBar listener

    @Override
    public void onScrubbingStart() {
        mListener.onSeekStart();
    }

    @Override
    public void onScrubbingMove(int time) {
        mListener.onSeekMove(time);
    }

    @Override
    public void onScrubbingEnd(int time, int trimStartTime, int trimEndTime) {
        mListener.onSeekEnd(time, trimStartTime, trimEndTime);
    }

    /**
     * SPRD: This method is to get real pixels for different resolution
     */
    public int getIntFromDimens(int index) {
        int result = this.getResources().getDimensionPixelSize(index);
        return result;
    }
}
