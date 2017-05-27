/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.Virtualizer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.VideoView;
import com.android.gallery3d.R;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.BlobCache;
import com.android.gallery3d.util.CacheManager;
import com.android.gallery3d.util.GalleryUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import com.sprd.gallery3d.cmcc.VideoCmccUtils;
/**SPRD:Bug474646 Add Drm feature @{  @{ */
import com.sprd.gallery3d.drm.VideoDrmUtils;//SPRD:bug 474646 Add Drm feature
/** @ } */
import java.io.IOException;
import java.io.File;
import java.util.LinkedList;
/**SPRD:Bug474600 import MovieInfo class from the MovieActivity @{*/
import com.android.gallery3d.app.MovieActivity.MovieInfo;
import com.sprd.gallery3d.app.MoviePlayerVideoView;
/** @}*/
/** SPRD:Bug 511468 add new sensor feature for MoviePlayer @{ */
import android.provider.Settings;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.provider.Settings;
/**@}*/
public class MoviePlayer implements
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener,MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnVideoSizeChangedListener,
        ControllerOverlay.Listener {//SRPD:Bug474621 screen size setting add OnVideoSizeChangedListener
    @SuppressWarnings("unused")
    private static final String TAG = "MoviePlayer";

    private static final String KEY_VIDEO_POSITION = "video-position";
    private static final String KEY_RESUMEABLE_TIME = "resumeable-timeout";
    private static final String KEY_PLAY_PAUST_STATE = "playpause_state";
    private static final String KEY_DRM_VIDEO_CONSUMED = "drm_consumed"; //SPRD:bug 506989 Add Drm feature
    public static final String AUDIO_SERVICE = "audio";
    // These are constants in KeyEvent, appearing on API level 11.
    private static final int KEYCODE_MEDIA_PLAY = 126;
    private static final int KEYCODE_MEDIA_PAUSE = 127;

    /**SPRD:Bug502125 FM can't stop normally, when play the 3gpp format recording by the video player
     * @orig
     * modify by old bug379538
     * @{
    // Copied from MediaPlaybackService in the Music Player app.
    private static final String SERVICECMD = "com.android.music.musicservicecommand";
    private static final String CMDNAME = "command";
    private static final String CMDPAUSE = "pause";
     *@}
     */

    private static final String VIRTUALIZE_EXTRA = "virtualize";
    private static final long BLACK_TIMEOUT = 500;

    // If we resume the acitivty with in RESUMEABLE_TIMEOUT, we will keep playing.
    // Otherwise, we pause the player.
    private static final long RESUMEABLE_TIMEOUT = 3 * 60 * 1000; // 3 mins

    private Context mContext;
    private final View mRootView;
    private final Bookmarker mBookmarker;
    /**SPRD:Bug474600 improve video control functions
     * remove @{
     * @orig
    private final Uri mUri;
    private final VideoView mVideoView;
    *@}
    /
    /**SPRD:Bug474600 improve video control functions
     * remove @{
     * @orig
    private final Handler mHandler = new Handler();
    *@}
    */
    private final AudioBecomingNoisyReceiver mAudioBecomingNoisyReceiver;
    private final MovieControllerOverlay mController;

    private long mResumeableTime = Long.MAX_VALUE;
    private int mVideoPosition = 0;
    private boolean mHasPaused = false;
    private int mLastSystemUiVis = 0;
    /**SPRD:Bug474646 Add Drm feature @{  @{ */
    private String mFilePath;// SPRD:bug 474646 Add Drm feature old bug 318505
    /** @ } */
    /**
     * SPRD:Bug500699 After waiting for the play after the manual drive,click Start,Continue to play
     * video tips modify by old bug429923 @{
     */
    public boolean mComplete = false;
    /** @ } */
    /** SRPD:Bug474600 improve video control functions
     * add new parameters @{ */
    private static final int CMD_STOP_BEGIN = 1;
    private static final int CMD_STOP_END = 0;
    private int curVideoIndex = 0;
    private String scheme;
    private Uri mBookMarkPath;
    private LinkedList<MovieInfo> mPlayList;
    private Bundle msavedInstance = null;
    private MovieActivity mMovieActivity = null;
    private int mVideoDuration = 0;
    private boolean isControlPause = false;
    private Uri mUri;
    /**SPRD:Bug474600 improve video control functions
     * remove @{
     * @orig
      private VideoView mVideoView=null;
     *@}
     */
    private MoviePlayerVideoView mVideoView=null;
    private boolean mIsChanged = false;
    private boolean isStop = false;
    private boolean mIsError;
    public boolean misLiveStreamUri = false;
    private String mAlbum;
    private final ActionBar mActionBar;
    private long mClickTime = 0;
    private boolean mIsCanSeek = true;
    private int mBeforeSeekPosition = 0;
    /**  @}*/
    /** SPRD:Bug474600 improve video control functions
     * user the Custom Handler class to replace the original Handler class @{ */
    private final MyHandler mHandler = new MyHandler();
    /** @} */
    /** SPRD:Bug 474639 add phone call reaction @{ */
    public int mMaxVolume = 0;
    public int mCurVolume;
    private AudioManager mAudioManager = null;
    private boolean isBookmark = false;
    private boolean mIsPhonePause;
    private long currentTime;
    private AlertDialog mTimeOutDialog;
    /** @} */
    /** SPRD:Bug 511468 add new sensor feature for MoviePlayer @{ */
    private SensorManager mSensorManager = null;
    private Sensor mSensor=null;
    private float mXaxis;
    private KeyguardManager mKeyguardManager;
    private boolean mIsSettingOpen=false;
    /** @ } */
    // If the time bar is being dragged.
    private boolean mDragging;

    // If the time bar is visible.
    private boolean mShowing;

    private Virtualizer mVirtualizer;
    /** SPRD:bug 474614: porting float play @{ */
    private Intent mServiceIntent; // old bug info:339523
    /** @} */
    /** SPRD:Bug474615 Playback loop mode
     * add new parameters @{ */
    private int currentPlayBackMode = 0;
    public final int SINGLE_PLAYBACK = 1;
    public final int ALL_PLAYBACK = 2;
    /** @} */
    /** SPRD:Bug474609 video playback resume after interruption @{ */
    private AlertDialog mResumeDialog;
    private boolean isResumeDialogShow = false;
    /** @} */
    /** SPRD:Bug474618 Channel volume settings
     * add new parameters @{ */
    private float mLeftVolume = 1, mRightVolume = 1;
    /**
     * SPRD:Bug 474631 add headset Control
     * old bug info: add for bug 327112 when long press
     * KeyEvent.KEYCODE_HEADSETHOOK or KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE the MoviePlayer do
     * nothing @{
     */
    private long mKeyDownEventTime = 0;
    /** @} */
    private final Runnable mPlayingChecker = new Runnable() {
        @Override
        public void run() {
            if (mVideoView!=null && mVideoView.isPlaying()) {//SRPD:Bug272737 add mVideoView!=null to check null pointer
                /**SPRD:Bug474600 improve video control functions
                 * add to check the video is playing@{*/
                Log.d(TAG, "mPlayingChecker isPlaying");
                if(misLiveStreamUri){
                    mController.setLiveMode();
                }
                /** @} */
                showControllerPlaying();
            /**SPRD:Bug474600 improve video control functions
             * add to check whether the video player is paused or not @{*/
            } else if (mHasPaused ) {
                if(misLiveStreamUri){
                    mController.setLiveMode();
                }
                showControllerPaused();
                mController.showPaused();
            /** @} */
            }else {
                /**SPRD:Bug474600 improve video control functions
                 * add to show loading@{*/
                Log.d(TAG, "mPlayingChecker isLoading");
                mController.showLoading();
                /** @} */
                mHandler.postDelayed(mPlayingChecker, 250);
            }
        }
    };

    private final Runnable mProgressChecker = new Runnable() {
        @Override
        public void run() {
            int pos = setProgress();
            mHandler.postDelayed(mProgressChecker, 1000 - (pos % 1000));
        }
    };
    /** SPRD:Bug474600 improve video control functions
     * add handler @{ */
    private class MyHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case CMD_STOP_BEGIN:
                mController.showLoading();
                break;

            case CMD_STOP_END:
                mController.showPaused();
                break;

            default:
                break;
            }
            super.handleMessage(msg);
        }
    }

    /** @} */
    /**SPRD:Bug474600 improve video control functions
     * add new parameter "playList"*/
    public MoviePlayer(View rootView, final MovieActivity movieActivity,
            Uri videoUri, LinkedList<MovieInfo> playList, Bundle savedInstance, boolean canReplay) {

        /** SPRD:Bug474600 improve video control functions
         * add new value @{ */
        mMovieActivity = movieActivity;
        mActionBar = movieActivity.getActionBar();
        /** @} */
        mContext = movieActivity.getApplicationContext();
        mRootView = rootView;
        /**SPRD:Bug474600 improve video control functions
         * remove this value,
         * because mVideoView is initialized in initVideoView() method @{
         * @orig
         mVideoView = (VideoView) rootView.findViewById(R.id.surface_view);
         *@}
         */
        mBookmarker = new Bookmarker(movieActivity);
        mUri = videoUri;
        /** SPRD:bug 474614: porting float play @{ */
        // old bug info:339523 begin
        int position = mMovieActivity.getIntent().getIntExtra("position", 0);
        if(position >= 0){
            mVideoPosition = position;
        }
        // bug 339523 end
        /** @} */
        /** SPRD:Bug474600 improve video control functions
         * add new values @{ */
      /*
       * SPRD:Bug540206 remove @{
        scheme = mUri.getScheme();
        if ("content".equalsIgnoreCase(scheme)) {
            if (playList != null) {
                String UriID = mUri.toString().substring(
                        mUri.toString().lastIndexOf("/") + 1,
                        mUri.toString().length());
                for (int i = 0; i < playList.size(); i++) {
                    if (UriID.equalsIgnoreCase(playList.get(i).mID)) {
                        mBookMarkPath = Uri.parse(playList.get(i).mPath);
                        Log.d(TAG, "mBookMarkPath " + mBookMarkPath);
                        curVideoIndex = i;
                        break;
                    }
                }
            }
        }
        *@}
        */
        mPlayList = playList;
        changeBookMarkerPathUri(mUri);//SPRD:Bug540206 Add
        Log.d(TAG,"curVideoIndex is "+curVideoIndex);
        /** @} */
        /** SPRD:Bug 474639 add phone call reaction @{ */
        mAudioManager = (AudioManager) movieActivity
                .getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = mAudioManager
                .getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        msavedInstance =savedInstance;
        /** @} */
        mController = new MovieControllerOverlay(mContext);
        ((ViewGroup) rootView).addView(mController.getView());
        mController.setListener(this);
        mController.setCanReplay(canReplay);
        /**SPRD:Bug531418 modify by old Bug531418 @{*/
        // bug 353383 begin
        if (isStreamUri()) {
            mController.showFloatPlayerButton(false);
        }
        // bug 353383 end
        /**@}*/
        /** SPRD:Bug474600 improve video control functions
         * add new values @{ */
        if (mPlayList == null) {
            Log.d(TAG, "mPlayList is null");
        }
        if (mPlayList == null || mPlayList.size() <= 1) {
            mController.showNextPrevBtn(false);
        } else {
            mController.showNextPrevBtn(true);
        }
        /** @} */
        if (mVirtualizer != null) {
            mVirtualizer.release();
            mVirtualizer = null;
        }
     /**SPRD:Bug474600 improve video control functions
      * remove to initVideoView() method @{
      *@orig
      * mVideoView.setOnErrorListener(this);
        mVideoView.setOnCompletionListener(this);
        mVideoView.setVideoURI(mUri);
        Intent ai = movieActivity.getIntent();
        boolean virtualize = ai.getBooleanExtra(VIRTUALIZE_EXTRA, false);
        if (virtualize) {
            int session = mVideoView.getAudioSessionId();
            if (session != 0) {
                mVirtualizer = new Virtualizer(0, session);
                mVirtualizer.setEnabled(true);
            } else {
                Log.w(TAG, "no audio session to virtualize");
            }
        }         
      * mVideoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mController.show();
                return true;
            }
        });
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer player) {
                if (!mVideoView.canSeekForward() || !mVideoView.canSeekBackward()) {
                    mController.setSeekable(false);
                } else {
                    mController.setSeekable(true);
                }
                setProgress();
            }
        });
        // The SurfaceView is transparent before drawing the first frame.
        // This makes the UI flashing when open a video. (black -> old screen
        // -> video) However, we have no way to know the timing of the first
        // frame. So, we hide the VideoView for a while to make sure the
        // video has been drawn on it.
        mVideoView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mVideoView.setVisibility(View.VISIBLE);
            }
        }, BLACK_TIMEOUT);

        setOnSystemUiVisibilityChangeListener();
        // Hide system UI by default
        showSystemUi(false);
       *@}
       */
        mAudioBecomingNoisyReceiver = new AudioBecomingNoisyReceiver();
        mAudioBecomingNoisyReceiver.register();
        /**SPRD:Bug502125 FM can't stop normally, when play the 3gpp format recording by the video player
         * @orig
         * modify by old bug379538
         * @{
         * @orig
        Intent i = new Intent(SERVICECMD);
        i.putExtra(CMDNAME, CMDPAUSE);
        movieActivity.sendBroadcast(i);
         *@}
         */

       /*SPRD:Bug474600 improve video control functions
        * remove to onResume() method @{
        * @orig
        * if (savedInstance != null) { // this is a resumed activity
            mVideoPosition = savedInstance.getInt(KEY_VIDEO_POSITION, 0);
            mResumeableTime = savedInstance.getLong(KEY_RESUMEABLE_TIME, Long.MAX_VALUE);
            *//** SPRD:bug 506989 Add Drm feature @{*//*
            if (VideoDrmUtils.getInstance().isDrmFile()) {
                VideoDrmUtils.getInstance().needToConsume(!savedInstance.getBoolean(KEY_DRM_VIDEO_CONSUMED,false));
            }
            *//** @} *//*
            mVideoView.start();
            mVideoView.suspend();
            mController.showPlaying();
            mHasPaused = true;
        } else {
            final Integer bookmark = mBookmarker.getBookmark(mUri);
            if (bookmark != null) {
                showResumeDialog(movieActivity, bookmark);
            } else {
                startVideo();
            }
        }
        */
    }
    /**SPRD:Bug474600 improve video control functions
     * add new method @{*/
    private void initVideoView() {
        mVideoView = (MoviePlayerVideoView) mRootView.findViewById(R.id.surface_view);
        mVideoView.setOnErrorListener(this);
        mVideoView.setOnCompletionListener(this);
        mVideoView.setOnPreparedListener(this);
        mVideoView.setOnInfoListener(this);
        mVideoView.setOnSeekCompleteListener(this);
        /**SPRD:474621 screen size setting @{*/
        mVideoView.setOnVideoSizeChangedListener(this);
        /**@}*/
        mVideoView.resize(mMovieActivity.getIsFullScreen());
        mVideoView.setVideoURI(mUri);
        if(!misLiveStreamUri){
            mVideoView.setLastInterruptPosition(Integer.valueOf(mVideoPosition).longValue());
        }
        // When the user touches the screen or uses some hard key, the framework
        // will change system ui visibility from invisible to visible. We show
        // the media control at this point.
        setOnSystemUiVisibilityChangeListener();
        mVideoView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mVideoView != null) {
                    mVideoView.setVisibility(View.VISIBLE);
                }
            }
        }, BLACK_TIMEOUT);
        Intent ai = mMovieActivity.getIntent();
        boolean virtualize = ai.getBooleanExtra(VIRTUALIZE_EXTRA, false);
        if (virtualize) {
            int session = mVideoView.getAudioSessionId();
            if (session != 0) {
                Virtualizer virt = new Virtualizer(0, session);
                AudioEffect.Descriptor descriptor = virt.getDescriptor();
                String uuid = descriptor.uuid.toString();
                if (uuid.equals("36103c52-8514-11e2-9e96-0800200c9a66")
                        || uuid.equals("36103c50-8514-11e2-9e96-0800200c9a66")) {
                    mVirtualizer = virt;
                    mVirtualizer.setEnabled(true);
                } else {
                    // This is not the audio virtualizer we're looking for
                    virt.release();
                }
            } else {
                Log.w(TAG, "no session");
            }
        }
        mVideoView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mController.show();
                return true;
            }
        });
    }
    /** @}*/
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setOnSystemUiVisibilityChangeListener() {
        if (!ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_HIDE_NAVIGATION) return;

        // When the user touches the screen or uses some hard key, the framework
        // will change system ui visibility from invisible to visible. We show
        // the media control and enable system UI (e.g. ActionBar) to be visible at this point
        mVideoView.setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                int diff = mLastSystemUiVis ^ visibility;
                mLastSystemUiVis = visibility;
                if ((diff & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
                        && (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                    mController.show();
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void showSystemUi(boolean visible) {
        if (!ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_LAYOUT_STABLE)
            return;

        int flag = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        if (!visible) {
            // We used the deprecated "STATUS_BAR_HIDDEN" for unbundling
            flag |= View.STATUS_BAR_HIDDEN | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_SURFACE;
        }
        /**SPRD:BugBug474600 improve video control functions
         * add if statement to judge null pointer*/
        if (mVideoView != null) {
            mVideoView.setSystemUiVisibility(flag);
        }
    }
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_VIDEO_POSITION, mVideoPosition);
        outState.putLong(KEY_RESUMEABLE_TIME, mResumeableTime);
        /** SPRD:bug 506989 Add Drm feature @{*/
        if (VideoDrmUtils.getInstance().isDrmFile()) {
            outState.putBoolean(KEY_DRM_VIDEO_CONSUMED, VideoDrmUtils.getInstance().isConsumed());
        }
        /** @} */
        outState.putBoolean(KEY_PLAY_PAUST_STATE, isControlPause);
    }
    public void onRestoreInstanceState(Bundle outState) {
        mVideoPosition = outState.getInt(KEY_VIDEO_POSITION, 0);
        mResumeableTime = outState.getLong(KEY_RESUMEABLE_TIME, Long.MAX_VALUE);
        isControlPause = outState.getBoolean(KEY_PLAY_PAUST_STATE,false);
        Log.i(TAG, "onRestoreInstanceState:"+mVideoPosition);
    }
    private void showResumeDialog(Context context, final int bookmark) {
        /** SPRD:Bug474609 video playback resume after interruption @{ */
        if (isResumeDialogShow)
            return; // SPRD : add

        if (mResumeDialog == null) {
            mResumeDialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.resume_playing_title)
                    .setMessage(
                            String.format(
                                    context.getString(R.string.resume_playing_message),
                                    GalleryUtils.formatDuration(context,
                                            bookmark / 1000)))
                    .setOnCancelListener(new OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            isResumeDialogShow = false; //SPRD : add by old bug379259
                            onCompletion();
                        }
                    })
                    .setPositiveButton(R.string.resume_playing_resume,
                            new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    mVideoView.seekTo(bookmark);
                                    mVideoView.setVisibility(View.VISIBLE);
                                    startVideo();
                                    isControlPause = false;
                                    isResumeDialogShow = false;
                                    isBookmark = false;

                                }
                            })
                    .setNegativeButton(R.string.resume_playing_restart,
                            new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    mVideoView.seekTo(0); // SPRD: added for bug
                                                          // 278584
                                    mVideoView.setVisibility(View.VISIBLE);
                                    startVideo();
                                    mVideoPosition = 0;
                                    isControlPause = false;
                                    isResumeDialogShow = false;
                                    isBookmark = false;
                                }
                            }).create();
        }
        mResumeDialog.show();
        mResumeDialog.setCanceledOnTouchOutside(false);
        isResumeDialogShow = true; // SPRD : add
        /** @} */
    }

    /** SPRD:bug 474614: porting float play @{ */
    // old bug info:339523 begin
    public void setControlPause(boolean pause){
        isControlPause = pause;
    }
    // bug 339523 end
    /** @ } */

    public void onPause() {
        //mHasPaused = true;
        mHandler.removeCallbacksAndMessages(null);
        mDragging = false;
        mController.setscrubbing();
        /** SPRD:Bug 474639 add phone call reaction @{ */
        if (mMovieActivity.isCallingState()) {
            mIsPhonePause = true;
            currentTime = System.currentTimeMillis();
            showControllerPaused();
        }
        /** @ } */
        /**SPRD:Bug517497 video and music play together @{*/
        if (mContext != null) {
            ((AudioManager)mContext.getSystemService(AUDIO_SERVICE))
            .abandonAudioFocus(null);
        }
        /**@}*/
        /** SRPD:Bug474600 add @{ */

        if (!misLiveStreamUri && mVideoView !=null  && mVideoView.isPlay() && !isBookmark) {
            mVideoPosition = mVideoView.getCurrentPosition();
        }
        /** @ } */
        /**
         * SPRD:Bug500699 After waiting for the play after the manual drive,click Start,Continue to play
         * video tips modify by old bug429923 @{
         */
        if (mComplete) {
            mVideoPosition = 0;
        }
        /** @ } */
        /**
         * SPRD:Bug474600 improve video control functions remove @{
         * @orig
         * mVideoPosition = mVideoView.getCurrentPosition();
         * @}
         */
        mIsCanSeek = mVideoView.canSeekBackward() && mVideoView.canSeekForward();
        if (!isStreamUri() && mIsCanSeek) {
            mBookmarker.setBookmark(mBookMarkPath, mVideoPosition, mVideoDuration);
        }
        mResumeableTime = System.currentTimeMillis() + RESUMEABLE_TIMEOUT;
        mHandler.removeCallbacks(mProgressChecker);
        mSeekState = ESeekState.SEEKFORWARD;
        if (mMovieActivity.mPhoneState) {
            mVideoView.pause();
        } else {
            executeSuspend();
        }
        /** @ } */
        /** SRPD:Bug474600 add @{ */
        if (misLiveStreamUri || !mIsCanSeek) {
            mController.resetTime();
            mVideoPosition = 0;
        }
        /** @ } */
        /** SPRD:Bug474609 video playback resume after interruption @{ */
        if (mResumeDialog != null && mResumeDialog.isShowing()) {
            isResumeDialogShow = false;
            mResumeDialog.dismiss();
        }
        /** @ } */
        /** SPRD:Bug 511468 add new sensor feature for MoviePlayer @{ */
        unregisterSensorEventListener();
        /** @ } */
    }
    public void onResume() {
        /** SPRD:Bug 474639 add phone call reaction @{ */
        boolean isResumeFromPhone = !mIsError && isStreamUri() && mIsPhonePause
                && (mVideoPosition > 0);
        Log.d(TAG, "!mIsError is  "+!mIsError+"  isStreamUri() is  "+isStreamUri()+"  mIsPhonePause  is  "
                +mIsPhonePause+"  mVideoPosition > 0  is  "+(mVideoPosition > 0));
         // boolean isTimeOut = System.currentTimeMillis() - currentTime > 60 * 1000;
          boolean isTimeOut=VideoCmccUtils.getInstance().ifIsPhoneTimeout(currentTime);
        /** @ } */
        /**
         * SPRD:Bug474600 improve video control functions add new values @{
         */
		Log.d(TAG, "mPlayer's onResume method is work!");
        if (mVideoView == null) {
            initVideoView();
            /** SPRD:Bug 474639 add phone call reaction @{ */
        } else if (!isResumeFromPhone || !isTimeOut) {
            if (mTimeOutDialog != null && mTimeOutDialog.isShowing()) {
                return;
            }
            mVideoView.resume();
        }
        /** old bug info : BUG 261460 stream timeout when incall @{ */
        if (isResumeFromPhone) {
            mIsPhonePause = false;
            if (isTimeOut) {
                showTimeOutDialog();
            } else {
                mHandler.postDelayed(mResumeCallback, 1500);
                if (msavedInstance != null) { // this is a resumed activity
                    mVideoPosition = msavedInstance.getInt(KEY_VIDEO_POSITION, 0);
                    mResumeableTime = msavedInstance.getLong(KEY_RESUMEABLE_TIME, Long.MAX_VALUE);
                    msavedInstance = null;
                }
            }
        } else {
            /** SPRD :@} */
            if (isStreamUri()) {
                postResume();
            } else {
                /** SPRD add code to check show resume dialog @{ */
                if (isBookmark) {
                    final Integer bookmark = mBookmarker.getBookmark(mBookMarkPath);
                    if (bookmark != null) {
                        mVideoPosition = bookmark;
                        mVideoView.pause();
                        showResumeDialog(mMovieActivity, bookmark);
                    } else {
                        postResume();
                    }
                } else {
                    postResume();
                }
                /** @} */
            }
            /** @ } */
            /** @ } */
            if (msavedInstance != null) { // this is a resumed activity
                mVideoPosition = msavedInstance.getInt(KEY_VIDEO_POSITION, 0);
                mResumeableTime = msavedInstance.getLong(KEY_RESUMEABLE_TIME,
                        Long.MAX_VALUE);
                /**
                 * SPRD: remove @{ mVideoView.start(); mVideoView.suspend(); mHasPaused = true; @ }
                 */
                /** SPRD:Bug474609 video playback resume after interruption  @{ */
                isControlPause = msavedInstance.getBoolean(KEY_PLAY_PAUST_STATE,false);
                /** @ } */
                /** SPRD:Bug474646 Add Drm feature modify by old bug 506989 @{*/
                if (VideoDrmUtils.getInstance().isDrmFile()) {
                    VideoDrmUtils.getInstance().needToConsume(!msavedInstance.getBoolean(KEY_DRM_VIDEO_CONSUMED, false));
                }
                /** @ } */
            } else {
                final Integer bookmark = mBookmarker.getBookmark(mBookMarkPath);
                if (mMovieActivity.mShowResumeDialog && bookmark != null) { // SPRD:Bug 474639 add
                                                   // phone call reaction
                    /** SPRD: remove @{ */
                    mVideoPosition = bookmark;
                    isBookmark = true;
                    mVideoView.pause();
                    /** @ } */
                    showResumeDialog(mMovieActivity, bookmark);
                }
            }
            /** @}*/
            /** SPRD:Bug474646 Add Drm feature modify by old bug 506989@{*/
            if (VideoDrmUtils.getInstance().isDrmFile()) {
                  mVideoView.setNeedToConsume(!VideoDrmUtils.getInstance().isConsumed());
               // mVideoView.setNeedToConsume(false);
            }
            /** @} */
        }
        /** SPRD:Bug 511468 add new sensor feature for MoviePlayer @{ */
        registerSensorEventListener();
        /** @} */
    }
    /** SPRD:Bug 474639 add phone call reaction @{ */
    private void showTimeOutDialog(){
        if(mTimeOutDialog == null){
            mTimeOutDialog = new AlertDialog.Builder(mMovieActivity).setTitle(
                    R.string.time_out).setMessage(R.string.time_out_message).setPositiveButton(
                    R.string.resume_playing_resume, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //begin bug 201610
                            if(!misLiveStreamUri){
                                mVideoView.setLastInterruptPosition(Integer.valueOf(mVideoPosition).longValue());
                            }
                            //end bug 201610
                            mVideoView.seekTo(mVideoPosition);
                            isStop = false;
                            mVideoView.setVisibility(View.VISIBLE);
                            showControllerPlaying();
                            mVideoView.resume();
                            startVideo();
                            setProgress();
                        }
                    }).setNegativeButton(R.string.resume_playing_restart, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mVideoPosition = 0;
                    //begin bug 201610
                    if(!misLiveStreamUri){
                        mVideoView.setLastInterruptPosition(Integer.valueOf(mVideoPosition).longValue());
                    }
                    //end bug 201610
                    mVideoView.seekTo(mVideoPosition);
                    isStop = false;
                    mVideoView.setVisibility(View.VISIBLE);
                    showControllerPlaying();
                    mVideoView.resume();
                    startVideo();
                    setProgress();
                }
            }).create();
        }
        mTimeOutDialog.show();
    }
    /** @}*/
    public void onDestroy() {
        if (mVirtualizer != null) {
            mVirtualizer.release();
            mVirtualizer = null;
        }
        /**SPRD:Bug474600 improve video control functions
         * remove@{
         * @orig
        mVideoView.stopPlayback();
        @/
        /** SPRD:bug 506989 Add Drm feature @{*/
        if (VideoDrmUtils.getInstance().isDrmFile()) {
            VideoDrmUtils.getInstance().needToConsume(true);
        }
        /** @ } */
        /** SPRD:Bug474600 improve video control functions
         * old Bug 256898 add @{ */
        if (mVideoView != null) {
            mVideoView.stopPlayback();
            mVideoView = null;
        }
        /** @} */
        /** SPRD:Bug 474600 improve video control functions@{ */
        mIsError = false;
        /** @} */
        if (mAudioBecomingNoisyReceiver != null) {
            mAudioBecomingNoisyReceiver.unregister();
        }
    }

    /** SPRD:bug 474614: porting float play @{ */
    public void releaseView(View rootView) {
        if (rootView != null && mController != null) {
            ((ViewGroup) rootView).removeView(mController.getView());
        }
    }
    /** @} */

    // This updates the time bar display (if necessary). It is called every
    // second by mProgressChecker and also from places where the time bar needs
    // to be updated immediately.
    private int setProgress() {
        if (mDragging || !mShowing||mVideoView == null) {//SPRD:Bug474600 add mVideoView Check
            return 0;
        }
        /**SPRD:Bug474600 remove @{
         * @Orig
         int position = mVideoView.getCurrentPosition();
         *@}
         */
        int position = 0;
        if(mVideoView.isPlay()){
            position = mVideoView.getCurrentPosition();
    //        Log.d(TAG, "getCurrentPosition() "+position);
            if(!mMovieActivity.isVolumeCorrect){
                changeVolume();
            }
        }else{
            position = mVideoPosition;
        }
        int duration = mVideoView.getDuration();
        /**SPRD:Bug474600  add @{  */
        if (duration == -1)
            duration = mVideoDuration;
        if (isControlPause) {
            position = mVideoPosition;
        }else{
            if (!misLiveStreamUri) {
                if (isStreamUri()) {
                    switch (mSeekState) {
                    case SEEKFORWARD:
                        if (mVideoView.isPlay() && position > mBeforeSeekPosition) {
                            mSeekState = ESeekState.NOSEEK;
                        }
                        if (position < mVideoPosition) {
                            position = mVideoPosition;
                        }
                        break;
                    case SEEKBACK:
                        Log.d(TAG, "mBeforeSeekPosition "+ mBeforeSeekPosition);
                        if (mVideoView.isPlay() && position < mBeforeSeekPosition) {
                            mSeekState = ESeekState.NOSEEK;
                        }
                        if (position > mVideoPosition) {
                            position = mVideoPosition;
                        }
                        break;
                    case NOSEEK:
                        break;

                    default:
                        break;
                    }
                } else {
                    if (mVideoPosition > 0 && position == 0)
                        position = mVideoPosition;
                }
            }
        }
        /** @} */
        mController.setTimes(position, duration, 0, 0);
        mVideoPosition = position;
        return position;
    }

    private void startVideo() {
        // For streams that we expect to be slow to start up, show a
        // progress spinner until playback starts.
        /**SPRD:Bug517497 video and music play together@{*/
        if (mContext != null) {
            ((AudioManager) mContext.getSystemService(AUDIO_SERVICE))
                    .requestAudioFocus(null, AudioManager.STREAM_MUSIC,
                            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
        /** @ } */
        /**
         * SPRD:Bug500699 After waiting for the play after the manual drive,click Start,Continue to play
         * video tips modify by old bug429923 @{
         */
        mComplete = false;
        /** @ } */
        Log.e(TAG , "startVideo()");
        String scheme = mUri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "rtsp".equalsIgnoreCase(scheme)) {
            mController.showLoading();
            mHandler.removeCallbacks(mPlayingChecker);
            mHandler.postDelayed(mPlayingChecker, 250);
        } else {
            mController.showPlaying();
       //   mController.hide();
        }
        if(mIsChanged){
            if (!mMovieActivity.isFirst || isStop) {
                mVideoView.setVisibility(View.VISIBLE);
                mIsChanged = false;
            }
        }
        mVideoView.start();
        mVideoView.setVisibility(View.VISIBLE);
        setProgress();
        /**SPRD:Bug474600 add  @{*/
        mHandler.post(mProgressChecker);
        isStop = false;
        /**@}*/
        /**SPRD:Bug474621 screen size setting add  @{*/
        mMovieActivity.showActionVedioOriginalsize();
        /**@}*/
    }

    private void playVideo() {
        /**SPRD:Bug474600 add @{  */
        isControlPause = false;
        /**  @} */
        mVideoView.start();
        mController.showPlaying();

        mHasPaused = false;//SPRD:modify by old Bug468361
        setProgress();
        /**SPRD:Bug474600 add @{  */
        isStop = false;
        mVideoView.setVisibility(View.VISIBLE);
        EnableControlButton();
        mHandler.post(mProgressChecker);
        /**  @} */
        /**SPRD:Bug474621 screen size setting add  @{*/
        mMovieActivity.showActionVedioOriginalsize();
        /**@}*/
    }

    private void pauseVideo() {
        /**SRPD:Bug474600 improve video control functions
         * remove @{
         * @orig
         mVideoView.pause();
         mController.showPaused();
         *@}
         */
        Log.d(TAG, "pauseVideo()");
        /** SPRD:Bug474600 improve video control functions
         * add for remove progress check @{ */
        if (!misLiveStreamUri) {
            isControlPause = true;
            Log.d(TAG,"mVideoView.isPlay() "+mVideoView.isPlay());
            if(mVideoView.isPlay() && mVideoView.canSeekForward() && mVideoView.canSeekBackward()){
                mVideoPosition = mVideoView.getCurrentPosition();
            }
            mHandler.removeCallbacksAndMessages(null);
            mHandler.removeCallbacks(mProgressChecker);
            mVideoView.pause();
        }
        mController.showPaused();
        showControllerPaused();
        mHasPaused = true;//SPRD:modify by old Bug468361
        /**  @} */
    }

    public void pause(){
        if(mVideoView != null){
            mVideoView.pause();
            showControllerPaused();
        }
    }

    // Below are notifications from VideoView
    @Override
    public boolean onError(MediaPlayer player, int arg1, int arg2) {
        mHandler.removeCallbacksAndMessages(null);
        // VideoView will show an error dialog if we return false, so no need
        // to show more message.\
        /**SPRD:Bug474600 improve video control functions
         * add @{  */
        mIsError = true;
        /**  @} */
        /** SPRD:Bug474615 Playback loop mode @} */
        if (currentPlayBackMode == SINGLE_PLAYBACK) {
            currentPlayBackMode = 0;
        }
        /** @} */
        mController.showErrorMessage("");
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        /**
         * SPRD:Bug500699 After waiting for the play after the manual drive,click Start,Continue to play
         * video tips modify by old bug429923 @{
         */
    //    mComplete = true;  SPRD:remove by Bug474615
        /** @ } */
    //    mController.showEnded(); SPRD:remove by Bug474615
    //    onCompletion();SPRD:remove by Bug474615
        /** SPRD: Bug474615 Playback loop mode
         * change for PlayBackMode @{ */
        Log.d(TAG, "currentPlayBackMode " + currentPlayBackMode);
        scheme = ((mUri == null) ? null : mUri.getScheme()); // SPRD: Defensive code
        mComplete = true;
        if (mPlayList != null) {
            if (currentPlayBackMode == SINGLE_PLAYBACK) {
                singlePlayBack();
            } else if (currentPlayBackMode == ALL_PLAYBACK) {
                allPlayBack();
            } else {
                mController.showEnded();
                onCompletion();
            }

        } else if ("file".equalsIgnoreCase(scheme)
                || "content".equalsIgnoreCase(scheme)) {// SPRD:add 255423;avoid null pointer for
                                                        // 262119
            if (currentPlayBackMode == SINGLE_PLAYBACK || currentPlayBackMode == ALL_PLAYBACK) {
                mVideoView.setVideoURI(mUri);
                mVideoPosition = 0;
                mVideoDuration = 0;
                isControlPause = false;
                startVideo();
                showControllerPlaying();
                EnableControlButton();
            } else {
                mController.showEnded();
                onCompletion();
            }
        } else {
            mController.showEnded();
            onCompletion();
        }
        /** @} */
    }

    public void onCompletion() {
        /** SPRD:Bug474600 improve video control functions
          reset value @{ */
        mVideoDuration = 0;
        mVideoPosition = 0;
        /** @ } */
        /** SPRD:bug 506989 Add Drm feature @{*/
        if (VideoDrmUtils.getInstance().isDrmFile()) {
            VideoDrmUtils.getInstance().needToConsume(true);
        }
        /** @ } */
    }

    /** SPRD:Bug521702 the music player and the video player can play at the same time @{ */
    public void onPauseVideo() {
        if(mMovieActivity.isCallingState()){
            return;
        }
        if (mVideoView.isPlaying()) {
            if(misLiveStreamUri){
                stopPlaybackInRunnable();
                mVideoView.setVisibility(View.INVISIBLE);
                mVideoPosition = 0;
                isStop = true;
                mController.resetTime();
                mController.timeBarEnable(false);
                mController.showFfwdButton(false);
                mController.showRewButton(false);
                mController.showStopButton(false);
                mMovieActivity.removeActionVedioOriginalsize();
            } else {
                pauseVideo();
                mHasPaused = true;
                isControlPause = false;//add for bug521106 for video resume
            }
        }
    }

    /** SPRD:Bug531413 modify by old bug521106 for video replay @{ */
    public void onPlayVideo() {
        if (VideoDrmUtils.getInstance().isDrmFile()) {
            Log.d(TAG, "VideoDrmUtils.getInstance().isDrmFile() return true");
            VideoDrmUtils.getInstance().checkRightBeforePlay(mMovieActivity, this);
        } else {
            // If not a drm file, just follow oringinal logic
            playVideo();
            mHasPaused = false;
        }
    }
    /**@}*/

    public boolean isPlaying() {
        if (mVideoView != null) {
            return mVideoView.isPlaying();
        } else {
            return false;
        }
    }
    /** @ } */
    // Below are notifications from ControllerOverlay
    @Override
    public void onPlayPause() {
        /** SPRD:Bug 474639 add phone call reaction @{ */
        if(mMovieActivity.isCallingState()){
            return;
        }
        /** @ } */
        /**SPRD:Bug474600 improve video control functions
         * add to check video playing*/
        if (mVideoView.isPlaying()) {
            if(misLiveStreamUri){
                stopPlaybackInRunnable();
                mVideoView.setVisibility(View.INVISIBLE);
                mVideoPosition = 0;
                isStop = true;
                mController.resetTime();
                mController.timeBarEnable(false);
                mController.showFfwdButton(false);
                mController.showRewButton(false);
                mController.showStopButton(false);
                /**SPRD:Bug474621 screen size setting*/
                mMovieActivity.removeActionVedioOriginalsize();
                /** @}*/
                /**SPRD:Bug474600 improve video control functions
                 * remove @{
                pauseVideo();
                *@}
                */
            } else {
                pauseVideo();
                mHasPaused = true;
            }
        } else {
            /** SPRD:Bug474646 Add Drm feature @{ @{ */
            // playVideo(); SPRD:Add Drm feature remove
            if (VideoDrmUtils.getInstance().isDrmFile()) {
                Log.d(TAG, "VideoDrmUtils.getInstance().isDrmFile() return true");
                VideoDrmUtils.getInstance().checkRightBeforePlay(mMovieActivity, this);
            } else {
                // If not a drm file, just follow oringinal logic
                playVideo();
                mHasPaused = false;
            }
            /** @ } */
        }
    }

    @Override
    public void onSeekStart() {
        mDragging = true;
    }

    @Override
    public void onSeekMove(int time) {
     /**
      *SPRD:Bug520393 there are some noise when seek the video modify by old Bug376820
      * @orig
      * remove @{
        mVideoView.seekTo(time);
      *@}
      */
    }

    @Override
    public void onSeekEnd(int time, int start, int end) {
        mDragging = false;
        /**
         * SPRD:Bug474600 improve video control functions
         * remove @{
         * @orig mVideoView.seekTo(time);
         */
        /** SPRD:Bug474600 add @{ */
        if (mVideoView != null) {
            mBeforeSeekPosition = mVideoView.getCurrentPosition();
            Log.d(TAG, "onSeekEnd mBeforeSeekPosition " + mBeforeSeekPosition);
            mVideoPosition = time;
            seek(time);
        /** @} */
            setProgress();
        }
    }

    @Override
    public void onShown() {
        mShowing = true;
        setProgress();
        showSystemUi(true);
        /**SRPD:Bug474600 improve video control functions
         * show the ActionBar @{*/
        mActionBar.show();
        /** @}*/
    }

    @Override
    public void onHidden() {
        mShowing = false;
        showSystemUi(false);
        /**SRPD:Bug474600 improve video control functions
         *  hide the ActionBar @{*/
        mActionBar.hide();
        /** @}*/
    }

    @Override
    public void onReplay() {
        startVideo();
    }

    /** SPRD:bug 474614: porting float play @{ */
    // old bug info:339523 begin
    @Override
    public void onStartFloatPlay() {
        //add for bug502230,play videos,call in,start the float player
        //the float video playing and the calling run at the same time
        if (mMovieActivity.isCallingState()) {
            return;
        }
        MediaPlayer mediaPlayer = null;
        try {
            mediaPlayer = new MediaPlayer();// check whether the file is exist
                                            // or not
            mediaPlayer.setDataSource(mContext, mUri);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(mContext, R.string.video_file_does_not_exist,
                    Toast.LENGTH_SHORT).show();
            return;
        } finally {
            if (mediaPlayer != null) {
                mediaPlayer.reset();
                mediaPlayer.release();
            }
        }
        int position = mVideoView.getCurrentPosition();
        int state = mVideoView.isPlaying() ? FloatMoviePlayer.STATE_PLAYING
                : FloatMoviePlayer.STATE_PAUSED;
        Log.d(TAG, "onStartFloatPlay position=" + position + " state=" + state);
        mVideoView.stopPlayback();

        mServiceIntent = new Intent(mMovieActivity, FloatPlayerService.class);
        mServiceIntent.putExtras(mMovieActivity.getIntent());
        mServiceIntent.setData(mUri);
        mServiceIntent.putExtra("position", position);
        mServiceIntent.putExtra("currentstate", state);
        mServiceIntent.putExtra("currentPlaybackMode", currentPlayBackMode);
        mServiceIntent.putExtra("FullScreen", mMovieActivity.getIsFullScreen());
        mServiceIntent.putExtra("left", mLeftVolume);
        mServiceIntent.putExtra("right", mRightVolume);
        mServiceIntent.putExtra("LoudSpeakerOn", mMovieActivity.getIsLoudSpeakerOn());
        mServiceIntent.putExtra("optionsSelectedLoudSpeakerOn", mMovieActivity.
                getOptionsSelectedLoudSpeakerOn());
        /**SPRD:532462
         * SPRD:add Intent.FLAG_GRANT_READ_URI_PERMISSION to grant read EmlAttachmentProvider permission
         */
        mServiceIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        mMovieActivity.startService(mServiceIntent);
        mMovieActivity.finish();

    }

    // bug 339523 end
    /** @} */
    // Below are key events passed from MovieActivity.
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        // Some headsets will fire off 7-10 events on a single click
        if (event.getRepeatCount() > 0) {
            return isMediaKey(keyCode);
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                /**
                 * SPRD:Bug 474631 add headset Control
                 * old bug info: add for bug 327112 when long
                 * press KeyEvent.KEYCODE_HEADSETHOOK or KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE the
                 * MoviePlayer do nothing
                 * ori:
                 * if (mVideoView.isPlaying()) {
                 *     pauseVideo();
                 * } else {
                 *     playVideo();
                 * }
                 *
                 * @{
                 */
                mKeyDownEventTime = event.getEventTime();
                /**  @} */
                return true;
            case KEYCODE_MEDIA_PAUSE:
                if (mVideoView.isPlaying()) {
                     /**SPRD:531424
                      * press KEYCODE_MEDIA_PAUSE, the Live stream has been stopped
                      */
                     if (misLiveStreamUri) {
                         onPauseVideo();
                     } else {
                         pauseVideo();
                     }
                }
                return true;
            case KEYCODE_MEDIA_PLAY:
                if (!mVideoView.isPlaying()) {
                    playVideo();
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                // TODO: Handle next / previous accordingly, for now we're
                // just consuming the events.
                return true;
        }
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        /**
         * SPRD:Bug 474631 add headset Control
         * old bug info: add for bug 327112 when long press
         * KeyEvent.KEYCODE_HEADSETHOOK or KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE the MoviePlayer do
         * nothing @{
         */
        if(keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE){
            if(event.getEventTime() - mKeyDownEventTime <= 500){
                if (mVideoView.isPlaying()) {
                    /**SPRD:531424
                     * press KEYCODE_MEDIA_PAUSE, the Live stream has been stopped
                    */
                     if (misLiveStreamUri) {
                        Log.d(TAG,"KEYCODE_MEDIA_PLAY_PAUSE Live Stream onPauseVideo()");
                        onPauseVideo();
                     } else {
                        Log.d(TAG, "KEYCODE_MEDIA_PLAY_PAUSE pauseVideo()");
                        pauseVideo();
                     }
                } else {
                    playVideo();
                }
            }
            mKeyDownEventTime = 0;
            return true;
        }
        /**  @} */
        return isMediaKey(keyCode);
    }

    private static boolean isMediaKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS
                || keyCode == KeyEvent.KEYCODE_MEDIA_NEXT
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE;
    }

    // We want to pause when the headset is unplugged.
    private class AudioBecomingNoisyReceiver extends BroadcastReceiver {

        public void register() {
            mContext.registerReceiver(this,
                    new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        }

        public void unregister() {
            mContext.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            /**SPRD:231424,bug537283
             * SPRD:when receive broadcast of ACTION_AUDIO_BECOMING_NOISY, the live stream does not response
             */
            if (mVideoView != null && mVideoView.isPlaying() && !misLiveStreamUri)
                pauseVideo();
            }
    }
    /**
     * SPRD:bug 474646 Add Drm feature @{
     * SPRD: Add these ugly public wrappers so that private methods can be invoked
     * in drm plugin
     */
    public void executeSuspend() {
        /** SPRD:Bug474646 Add Drm feature modify by old bug 506989@{*/
        if (VideoDrmUtils.getInstance().isDrmFile() && !VideoDrmUtils.getInstance().isConsumed()) {
            VideoDrmUtils.getInstance().needToConsume(false);
        }
        /** @} */
        // SPRD: Add for Drm feature
        if (mVideoView == null) {
            return;
        }
        mVideoView.suspend();
    }
    public void playVideoWrapper() {
        playVideo();
        mHasPaused = false;
    }
    public void changeVideoWrapper() {
        _changeVideo();
    }
    /**@}*/
    /** SPRD:Bug474600 improve video control functions
     * add new private methods @{ */
    public boolean isStreamUri() {
        return ("http".equalsIgnoreCase(scheme) || "rtsp"
                .equalsIgnoreCase(scheme));
    }
    private void nextVideo() {
        Log.d(TAG, "nextVideo()");
        /** SPRD:Bug474646 Add Drm feature modify by old bug 506989@{*/
        if (VideoDrmUtils.getInstance().isDrmFile()) {
            VideoDrmUtils.getInstance().needToConsume(true);
        }
        /** @} */
        if (mPlayList != null) {
            if (curVideoIndex == mPlayList.size() - 1) {
                curVideoIndex = 0;
            } else {
                ++curVideoIndex;
            }
            changeVideo();
        } else {
            if (mVideoView != null) {
                mVideoView.stopPlayback();
                mVideoView.setVideoURI(mUri);
                mVideoPosition = 0;
                mVideoDuration = 0;
                isControlPause = false;
                startVideo();
                showControllerPlaying();
                EnableControlButton();
            }
        }
    }
    private void prevVideo() {
        Log.d(TAG, "prevVideo()");
        /** SPRD:Bug474646 Add Drm feature modify by old bug 506989@{*/
        if (VideoDrmUtils.getInstance().isDrmFile()) {
            VideoDrmUtils.getInstance().needToConsume(true);
        }
        /** @} */
        if (mPlayList != null) {
            if (curVideoIndex <= 0) {
                curVideoIndex = mPlayList.size() - 1;
            } else {
                --curVideoIndex;
            }
            changeVideo();
        } else {
            if (mVideoView != null) {
                mVideoView.stopPlayback();
                mVideoView.setVideoURI(mUri);
                mVideoPosition = 0;
                mVideoDuration = 0;
                isControlPause = false;
                startVideo();
                showControllerPlaying();
                EnableControlButton();
            }
        }
    }

    private void changeVideo() {
        Log.d(TAG, "changeVideo() ");
        misLiveStreamUri = false;
        mController.setNotLiveMode();
        if (isStreamUri()) {
            stopPlaybackInRunnable();
            changeVideoInRunable();
        } else {
            if (!isStreamUri() && mVideoView.canSeekBackward()
                    && mVideoView.canSeekForward()) {
                mBookmarker.setBookmark(mBookMarkPath, mVideoPosition,
                        mVideoDuration);
            }
            mController.showLoading();
            mVideoView.stopPlayback();
            mVideoView.setVisibility(View.INVISIBLE);
         //   _changeVideo();
            // SPRD: Add for Drm feature
            VideoDrmUtils.getInstance().setStopState(true);
            // Before check right, file path must be updated
            MovieInfo item = mPlayList.get(curVideoIndex);
            String id = item.mID;
            mUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(id).build();
            VideoDrmUtils.getInstance().getFilePathByUri(mUri, mMovieActivity);
            if (VideoDrmUtils.getInstance().isDrmFile()) {
                VideoDrmUtils.getInstance().checkRightBeforeChange(mMovieActivity, this);
            } else {
                _changeVideo();
            }
        }
    }

    private void _changeVideo() {
        Log.d(TAG, "_changeVideo() ");
        mIsChanged = true;
        mMovieActivity.mShowResumeDialog = true;//SPRD: bug 526178 just show resume dialog once.
        MovieInfo item = mPlayList.get(curVideoIndex);
        /**
         * SPRD:Bug474605 video sharing change by bug old 276956 @{
         */
        /**
         * SPRD:Bug474605 video sharing change by old Bug339523 @{
         */
        // mUri = Uri.fromFile(new File(item.mAlbum));
        String id = item.mID;
        mUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(id).build();
        /**@}*/
        mVideoView.setVideoURI(mUri);
        /**@}*/
        changeBookMarkerPathUri(mUri);//SPRD:Bug540206 add
        /**
         * SPRD:Bug474605 video sharing add by old Bug304358
         */
        Uri nUri = Uri.parse("content://media/external/video/media");
        Uri mVideoUri = null;
        String picpath = item.mPath;
        Cursor cursor = null;
        try {
            cursor = mMovieActivity.getContentResolver().query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null,
                    MediaStore.Video.Media.DEFAULT_SORT_ORDER);
            if (cursor != null) {
                Log.d(TAG, "cursor: " + cursor);
                cursor.moveToNext();
                while (!cursor.isAfterLast()) {
                    String data = cursor.getString(cursor
                            .getColumnIndex(MediaStore.MediaColumns.DATA));
                    if (picpath.equals(data)) {
                        int RTID = cursor.getInt(cursor
                                .getColumnIndex(MediaStore.MediaColumns._ID));
                        mVideoUri = Uri.withAppendedPath(nUri, "" + RTID);
                        break;
                    }
                    cursor.moveToNext();
                }
            }
            mMovieActivity.onVideoUriChange(mVideoUri);
            mAlbum = item.mAlbum;
            mActionBar.setTitle(mAlbum);
            mVideoPosition = 0;
            mVideoDuration = 0;
            isControlPause = false;
            if (mVideoView != null) {
                mVideoView.errorDialogCheckAndDismiss();
            }
            startVideo();
            showControllerPlaying();
            EnableControlButton();
        } catch (Exception e) {
            Log.i(TAG, "Exception" + e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        /**@}*/
    }
    private void stopPlaybackInRunnable() {
        isStop = true;
        Message msgBegin = mHandler.obtainMessage(CMD_STOP_BEGIN);
        mHandler.removeMessages(CMD_STOP_BEGIN);
        mHandler.sendMessage(msgBegin);

        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    if (mVideoView != null) {
                        mVideoView.stopPlayback();
                    }
                    Message msgEnd = mHandler.obtainMessage(CMD_STOP_END);
                    mHandler.removeMessages(CMD_STOP_END);
                    mHandler.sendMessage(msgEnd);
                } catch (NullPointerException e) {

                }
            }
        }).start();

        mController.timeBarEnable(false);
        mController.showRewButton(false);
        mController.showFfwdButton(false);
    }

    private void changeVideoInRunable() {
        if (!mVideoView.isStopPlaybackCompleted()) {
            mHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    changeVideoInRunable();
                }
            }, 100);
        } else {
            _changeVideo();
        }
    }

    private void singlePlayBack() {
        int n = mPlayList.size();
        if (n == 0) {
            return;
        }
        changeVideo();
    }
    private void allPlayBack() {
        Log.d(TAG, "mPlayList.size() " + mPlayList.size());
        /** SPRD:Bug474646 Add Drm feature modify by old bug 506989@{*/
        if (VideoDrmUtils.getInstance().isDrmFile()) {
            VideoDrmUtils.getInstance().needToConsume(true);
        }
        /** @} */
        int n = mPlayList.size();
        if (n == 0) {
            return;
        }
        if (++curVideoIndex >= n) {
            curVideoIndex = 0;
        }
        Log.d(TAG, "curVideoIndex " + curVideoIndex);
        changeVideo();
    }
    private enum ESeekState {
        SEEKFORWARD, NOSEEK, SEEKBACK
    }
    ESeekState mSeekState = ESeekState.SEEKFORWARD;
    private void seek(int time) {
        if (mVideoView != null) {
            if (mVideoView.getCurrentPosition() > time) {
                mSeekState = ESeekState.SEEKBACK;
            } else if (mVideoView.getCurrentPosition() < time) {
                mSeekState = ESeekState.SEEKFORWARD;
            } else {
                mSeekState = ESeekState.NOSEEK;
            }
            mVideoPosition = time;
            mVideoView.seekTo(time);
            if (isStreamUri()) {
                mController.showLoading();
                mHandler.removeCallbacks(mPlayingChecker);
                mHandler.postDelayed(mPlayingChecker, 250);
            }// else {
                // not show play state, it can show right state when seek complete
                // showControllerPlaying();
           // }
            setProgress();
        }
    }
    private void showControllerPlaying() {
        if (!misLiveStreamUri) {
            mController.showPlaying();
        } else {
            mController.clearPlayState();
         }
    }
    private void EnableControlButton() {
        if(isStop){
            mController.timeBarEnable(false);
            mController.showFfwdButton(false);
            mController.showRewButton(false);
            mController.showStopButton(false);
        }else{
            mController.timeBarEnable(mVideoView.canSeekBackward());
            mController.showFfwdButton(mVideoView.canSeekForward());
            mController.showRewButton(mVideoView.canSeekBackward());
            mController.showStopButton(true);
        }
        mController.setControlButtonEnableForStop(true);
        if (misLiveStreamUri) {
            mController.setLiveMode();
        }
    }
    private void showControllerPaused() {
        if (!misLiveStreamUri) {
            mController.showPaused();
        } else {
            mController.clearPlayState();
        }
    }
    private void postResume() {
        if (!mIsCanSeek && !misLiveStreamUri) {
            Toast.makeText(mMovieActivity, R.string.special_video_restart_playing,
                    Toast.LENGTH_LONG)
                    .show();
        }
        if (isControlPause) {
            mVideoView.seekTo(mVideoPosition);
            /** SPRD new code for bug 255338 @{ */
            if (!isStreamUri()) {
                mVideoView.setVisibility(View.VISIBLE);
            }
            /** @} */
            Log.d(TAG, "postResume() pauseVideo()");
            pauseVideo();
        } else if (!isStop && !isResumeDialogShow) {//SPRD:Bug474609 video playback resume after interruption add
            mVideoView.seekTo(mVideoPosition);
            isStop = false;
            // if(!misLiveStreamUri){
            // mVideoView.setLastInterruptPosition(Integer.valueOf(mVideoPosition).longValue());
            // }
            // mVideoView.setVisibility(View.VISIBLE);
            showControllerPlaying();
            startVideo();
        }
    }
    private Runnable mResumeCallback = new Runnable() {

        @Override
        public void run() {
            postResume();
        }
    };
    public void onConfigurationChanged(Configuration newConfig){
        mDragging = false;
        mController.setscrubbing();
        if (isStreamUri()) {
            if(!isStop){
                mController.showLoading();
                mHandler.removeCallbacks(mPlayingChecker);
                mHandler.postDelayed(mPlayingChecker, 250);
            }
        } else {
            /** SPRD new code @{ */
            if (mVideoView != null) {
                if (mVideoView.isPlaying()) {
                    showControllerPlaying();
                } else if (mHasPaused || isStop) { //SPRD:add for 251812
                    showControllerPaused();
                }// else {
//                    mController.showLoading(); //SPRD: bug 275424,local video fast enough, need not load.
                    //for reserved.
               // }
            }
            /** @} */
        }
    }
    /** @}*/
    /**SPRD:Bug474600 improve video control functions
     *  add new public methods @{*/
    @Override
    public void onStopVideo() {
        // TODO Auto-generated method stub
        long currentTime = System.currentTimeMillis();
        if (currentTime - mClickTime < 350)
            return;
        mClickTime = currentTime;
        if (isStop || mVideoView.isStopPlaybackCompleted()) {
            return;
        }
        mController.timeBarEnable(false);
        mController.showFfwdButton(false);
        mController.showRewButton(false);
        mController.showStopButton(false);
        stopPlaybackInRunnable();
        mVideoView.setVisibility(View.INVISIBLE);
        mVideoPosition = 0;
        setProgress();
        isStop = true;
        /**SPRD:Bug474621 screen size setting*/
        mMovieActivity.removeActionVedioOriginalsize();
        /** @}*/
    }

    @Override
    public void onNext() {
        // TODO Auto-generated method stub
        nextVideo();
    }

    @Override
    public void onPrev() {
        // TODO Auto-generated method stub
        prevVideo();
    }

    @Override
    public void onFfwd() {
        // TODO Auto-generated method stub
        Log.d(TAG, "onFfwd ");
        int pos = mVideoView.getCurrentPosition();
        pos += 15000; // milliseconds
        if (pos > mVideoDuration) {
            pos = mVideoDuration;
        }
        Log.d(TAG, "onFfwd pos " + pos);
        seek(pos);
        setProgress();
    }

    @Override
    public void onRew() {
        // TODO Auto-generated method stub
        Log.d(TAG, "onRew ");
        int pos = mVideoView.getCurrentPosition();
        mBeforeSeekPosition = pos;
        pos -= 15000; // milliseconds
        if (pos <= 0) {
            pos = 1;
        }
        Log.d(TAG, "onRew pos " + pos);
        seek(pos);
        setProgress();
    }

    @Override
    public void onSeekComplete(MediaPlayer arg0) {
        // TODO Auto-generated method stub
        /** SPRD: new code @{ */
        Log.d(TAG,"onSeekComplete");
        if (isControlPause) {
            showControllerPaused();
        } else {
            showControllerPlaying();
        }
        /** @} */
    }

    @Override
    public void onPrepared(MediaPlayer arg0) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onPrepared isplaying=" + mVideoView.isPlaying());
        Log.d("CanseekForward", "mVideoView.canSeekForward is "+mVideoView.canSeekForward());
        mVideoDuration = mVideoView.getDuration();
        /** SPRD: new code @{ */
        if(!misLiveStreamUri && !isStop){
            mController.timeBarEnable(mVideoView.canSeekBackward());
            mController.showStopButton(true);
            mController.showFfwdButton(mVideoView.canSeekForward());
            mController.showRewButton(mVideoView.canSeekBackward());
        } else if (isStop){
                mController.timeBarEnable(false);
                mController.showFfwdButton(false);
                mController.showRewButton(false);
                mController.showStopButton(false);
        }
        if (isStreamUri()) {
            Log.d(TAG, "startVideo() http rtsp");
            mHandler.removeCallbacks(mProgressChecker);
            mHandler.post(mProgressChecker);
            EnableControlButton();
            mHandler.removeCallbacks(mPlayingChecker);
            mHandler.postDelayed(mPlayingChecker, 250);
            mVideoView.start();
        }
    }
    public void setVideoViewInvisible() {
        if (mVideoView != null) {
            mVideoView.setVisibility(View.INVISIBLE);
        }
    }
    public void hideToShow(){
        this.mController.hideToShow();
     }
    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        // TODO Auto-generated method stub
        /** SPRD:Bug474600 improve video control functions
         * add media buffering start and end process@{ */
        Log.d(TAG, "onInfo " + what);
        if (what == MediaPlayer.MEDIA_INFO_NOT_SEEKABLE) {
            /**SPRD:modify by old bug515315*/
            //misLiveStreamUri = true;
            if(isStreamUri()){
                misLiveStreamUri = true;
            }
        } //else {
         //   misLiveStreamUri = false;
       // }
            /**@}*/
        if (isStreamUri()) {
            if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                mController.showLoading();
            } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                if (!mVideoView.isPlay() && !isControlPause) {
                    mVideoView.start();
                }
                if (mVideoView.isPlaying()) {
                    mController.showPlaying();
                }
            }
            mController.showFfwdButton(!misLiveStreamUri);
            mController.showRewButton(!misLiveStreamUri);
        }
        return false;
    }
    /**  @} */
    /** SPRD:Bug474615 Playback loop mode
     *  new method @{ */
    public void setPlayBackMode(int mode) {
        currentPlayBackMode = mode;
    }
    /** @} */
    public void setChannel(float left, float right){
        mLeftVolume = left;
        mRightVolume = right;
    }
    /**SPRD:Bug474621 screen size setting
     *  add new public method @{*/
    public void resize(boolean isFit) {
        if(mVideoView != null){
            mVideoView.resize(isFit);
        }
    }
    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        // TODO Auto-generated method stub
        setProgress();
        mHandler.removeCallbacks(mProgressChecker);
        mHandler.post(mProgressChecker);
        /**SPRD:Bug474618 Channel volume settings @{*/
        Log.d(TAG,"mLeftVolume=="+mLeftVolume+"mRightVolume"+mRightVolume);
        setChannelVolume(mLeftVolume, mRightVolume);
        /**@}*/
        EnableControlButton();
        if (isStreamUri()) {
            Log.d(TAG, "startVideo() http rtsp");
            mHandler.removeCallbacks(mPlayingChecker);
            mHandler.postDelayed(mPlayingChecker, 250);
        } else {
            if(isControlPause){
                showControllerPaused();
            }else{
                showControllerPlaying();
            }
        }
    }
    /**  @} */
    /** SPRD:Bug474618 Channel volume settings
     *  new method @{ */
    public void setChannelVolume(float left, float right) {
        mLeftVolume = left;
        mRightVolume = right;
        if(mVideoView != null){
            mVideoView.setChannelVolume(left, right);
        }
    }
    /** @} */
    /** SPRD:Bug 474639 add phone call reaction @{ */
    public void changeVolume() {
        int nvolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        Log.d(TAG, "nvolume =" + nvolume);
        if (mCurVolume != nvolume) {
            mCurVolume = nvolume;
            // updateVolumeIcon(nvolume);
        }
        mMovieActivity.isVolumeCorrect = true;
    }

    public void updateVolume(int volume) {
        Log.d(TAG, "updateVolume volume " + volume);
        mCurVolume = volume;
        if (volume > mMaxVolume || volume < 0) {
            return;
        }
        if (mAudioManager != null) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        }
        // updateVolumeIcon(volume);
    }
    /**  @} */
    /** SPRD:Bug 511468 add new sensor feature for MoviePlayer @{ */
    private SensorEventListener mSensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if (mKeyguardManager.isKeyguardLocked()) {
                return;
            }
            mXaxis=(int)event.values[SensorManager.DATA_X];
            Log.d(TAG, "event:" + mXaxis);
            if (mXaxis == 0) {
                if (mHasPaused) {
                    playVideo();
                    mHasPaused = false;
                }
            } else if (mXaxis == 1) {
                if (mVideoView.isPlaying()) {
                    pauseVideo();
                    mHasPaused = true;
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    public void registerSensorEventListener() {
        mKeyguardManager = (KeyguardManager)mContext.getSystemService(Service.KEYGUARD_SERVICE);
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_SPRDHUB_FACE_UP_DOWN);
        mIsSettingOpen=Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.PLAY_CONTROL, 0) != 0;
        Log.d(TAG, "mIsSettingOpen "+mIsSettingOpen);
        if (!mIsSettingOpen) {
            return;
        }
        Log.d(TAG, "mSensorManager  "+(mSensorManager != null)+"   mSensor  "+(mSensor!= null));
        if (mSensorManager != null && mSensor != null) {
        Log.d(TAG, "mSensor is registered");
            mSensorManager.registerListener(mSensorListener, mSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void unregisterSensorEventListener() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mSensorListener);
            mSensorManager =null;
        }
    }
    /**@}*/
    /** SPRD:Bug538278 when you unplug the SD card, a error dialog always shows @{ */
    public void disableMediaPlayer() {
        if (mMovieActivity != null) {
            mMovieActivity.finish();
        }
    }
    /**@}*/
    /** SPRD:Bug540206 the Video can't resume normally, when press the next and pre button  @{ */
    public void changeBookMarkerPathUri(Uri uri){
        if(uri == null)return;
        scheme = uri.getScheme();
        if ("content".equalsIgnoreCase(scheme)) {
            if (mPlayList != null) {
                String UriID = uri.toString().substring(
                        uri.toString().lastIndexOf("/") + 1,
                        uri.toString().length());
                for (int i = 0; i < mPlayList.size(); i++) {
                    if (UriID.equalsIgnoreCase(mPlayList.get(i).mID)) {
                        mBookMarkPath = Uri.parse(mPlayList.get(i).mPath);
                        curVideoIndex = i;
                        Log.d(TAG, "mBookMarkPath in changePath method: " + mBookMarkPath);
                        break;
                    }
                }
            }
        }
    }
    /**@}*/
}

class Bookmarker {
    private static final String TAG = "Bookmarker";

    private static final String BOOKMARK_CACHE_FILE = "bookmark";
    private static final int BOOKMARK_CACHE_MAX_ENTRIES = 100;
    private static final int BOOKMARK_CACHE_MAX_BYTES = 10 * 1024;
    private static final int BOOKMARK_CACHE_VERSION = 1;

    private static final int HALF_MINUTE = 30 * 1000;
    private static final int TWO_MINUTES = 4 * HALF_MINUTE;

    private final Context mContext;

    public Bookmarker(Context context) {
        mContext = context;
    }

    public void setBookmark(Uri uri, int bookmark, int duration) {
        try {
            BlobCache cache = CacheManager.getCache(mContext,
                    BOOKMARK_CACHE_FILE, BOOKMARK_CACHE_MAX_ENTRIES,
                    BOOKMARK_CACHE_MAX_BYTES, BOOKMARK_CACHE_VERSION);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(uri.toString());
            dos.writeInt(bookmark);
            dos.writeInt(duration);
            dos.flush();
            cache.insert(uri.hashCode(), bos.toByteArray());
        } catch (Throwable t) {
            Log.w(TAG, "setBookmark failed", t);
        }
    }

    public Integer getBookmark(Uri uri) {
        try {
            BlobCache cache = CacheManager.getCache(mContext,
                    BOOKMARK_CACHE_FILE, BOOKMARK_CACHE_MAX_ENTRIES,
                    BOOKMARK_CACHE_MAX_BYTES, BOOKMARK_CACHE_VERSION);

            byte[] data = cache.lookup(uri.hashCode());
            if (data == null) return null;

            DataInputStream dis = new DataInputStream(
                    new ByteArrayInputStream(data));

            String uriString = DataInputStream.readUTF(dis);
            int bookmark = dis.readInt();
            int duration = dis.readInt();

            if (!uriString.equals(uri.toString())) {
                return null;
            }
            Log.d(TAG, "bookamrk is "+bookmark/1000);
            Log.d(TAG, "duration is  "+ duration/1000);
            if ((bookmark < HALF_MINUTE) || (duration < TWO_MINUTES)
                    || (bookmark > (duration - HALF_MINUTE))) {
                return null;
            }
            return Integer.valueOf(bookmark);
        } catch (Throwable t) {
            Log.w(TAG, "getBookmark failed", t);
        }
        return null;
    }
}
