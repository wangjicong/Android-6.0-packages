
package com.android.gallery3d.app;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.app.KeyguardManager;
import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.Metadata;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Build;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.MediaStore.Video.VideoColumns;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.ui.FloatFrameLayout;
import com.android.gallery3d.ui.FloatFrameLayout.OnConfigurationChangedListener;
import com.sprd.gallery3d.app.VideoActivity;
/**SPRD:Bug474646 Add Drm feature @{ @{ */
import com.sprd.gallery3d.drm.VideoDrmUtils;//SPRD:bug 474646 Add Drm feature
/** @ } */
public class FloatMoviePlayer implements OnBufferingUpdateListener,
        OnCompletionListener, MediaPlayer.OnPreparedListener, OnTouchListener,
        SurfaceHolder.Callback, OnClickListener,MediaPlayer.OnErrorListener {//SPRD:Bug531341 add
    public static final String TAG = "FloatMoviePlayer";
    public static final int STATE_ERROR = -1;
    public static final int STATE_IDLE = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_PREPARED = 2;
    public static final int STATE_PLAYING = 3;
    public static final int STATE_PAUSED = 4;
    public static final int STATE_PLAYBACK_COMPLETED = 5;

    //SPRD: fix the bug 531300
    /**
     * the minimum value with player window's width in portrait orientation;
     */
    private static final int MIN_WIDTH_IN_PORTRAIT = 216;
    /**
     * the maximum value with player window's height in landscape orientation;
     */
    private static final int MIN_HEIGHT_IN_LANDSCAPE = 280;
    private static final float MIN_SCALE = 0.4f;
    private static final float MAX_SCALE = 1.0f;

    /** modify by old Bug493063 @{ */
    private int currentPlayBackMode = 0;
    public final int SINGLE_PLAYBACK = 1;
    public final int ALL_PLAYBACK = 2;
    private Dialog alertDialog;//SPRD:modify by old Bug493063
    /**@}*/

    private int mVideoWidth;
    private int mVideoHeight;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mWindowWidth;
    private int mWindowHeight;
    private MediaPlayer mMediaPlayer;
    private SurfaceHolder mSurfaceHolder;
    private SeekBar skbProgress;

    private float mTouchStartX;
    private float mTouchStartY;
    private float mLastX;
    private float mLastY;
    private float mX;
    private float mY;

    private WindowManager mWm;
    private WindowManager.LayoutParams mWmParams;
    private Context mContext;
    private int mStatusBarHeight;
    private long mLastTouchDownTime;
    private long mCurTouchDownTime;
    public FloatFrameLayout mFloatLayout;
    private ImageView mPausePlayBtn;
    private ImageView mBackToNormalBtn;
    private ImageView mCloseWindowBtn;

    private ImageView mPreVideoBtn;
    private ImageView mNextVideoBtn;
    private TextView mVideoTileTextView;
    private TextView mVideoLoadingTextView;
    private boolean mHideTitle;

    private Uri mUri;
    private ScaleGestureDetector mScaleGestureDetector;
    private int mPosition;
    private int mSeekPositionWhenPrepared;
    private int mCurrentState;
    private int mTargetState;
    private int mState;

    private final Handler mHandler;
    private final Runnable startHidingRunnable;
    private AudioManager mAm;

    private Display mDisplay;

    private Intent mDataIntent;
    private LinkedList<MovieInfo> mPlayList;
    private int curVideoIndex;
    private Uri videoListUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    private SurfaceView mSurfaceView;
    private String mTitle = "";
    private boolean mIsTitleShort = false;// bug 355537
    private boolean mCanSeekBack;
    private boolean mCanSeekForward;
    private float mScale = (float) 2 / 3;
    private boolean isPhoneCallActivityTop = false;
    private String scheme;//SPRD:modify by old Bug515022
    private static final String FLAG_GALLERY="startByGallery";
    private String mAlbum;
    private boolean mIsStartByGallery;
    OnAudioFocusChangeListener afChangeListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            Log.d(TAG, "focusChange:" + focusChange);
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS:
                    mState = mCurrentState;
                    if (mCurrentState == STATE_PLAYING) {
                        pause();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                case AudioManager.AUDIOFOCUS_GAIN:
                    KeyguardManager keyguardManager = (KeyguardManager) mContext
                            .getSystemService(Service.KEYGUARD_SERVICE);
                    if (!keyguardManager.isKeyguardLocked()) {
                        if (mCurrentState != STATE_PLAYING && mState == STATE_PLAYING) {
                            start();
                            hiddenBtn();
                            mState = 0;
                        }
                    }
                    break;

                default:
                    break;
            }
        }
    };

    public void setState(int state){
        mState = state;
    }
    public FloatMoviePlayer(Context context) {
        mContext = context;
        mWm = (WindowManager) context.getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);
        mStatusBarHeight = getStatusBarHeight();

        mDisplay = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();

        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureListener());

        mHandler = new Handler() {
            public void handleMessage(Message msg) {

                int position = mMediaPlayer.getCurrentPosition();
                int duration = mMediaPlayer.getDuration();

                if (duration > 0) {
                    long pos = skbProgress.getMax() * position / duration;
                    skbProgress.setProgress((int) pos);
                }
            };
        };
        startHidingRunnable = new Runnable() {
            public void run() {
                hiddenBtn();
            }
        };
        mMediaPlayer = new MediaPlayer();
        /*
         * SPRD:Bug531341 remove
         * @{
        mMediaPlayer.setOnErrorListener( new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
                    errorDialogCheckAndDismiss();// SPRD:modify by old Bug493063
                    int messageId;
                    if (framework_err == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
                        messageId = com.android.internal.R.string.VideoView_error_text_invalid_progressive_playback;
                    } else {
                        messageId = com.android.internal.R.string.VideoView_error_text_unknown;
                    }
                    alertVideoError(messageId);
                    return true;
                }
            }
        );
        *@}
        */
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setWakeMode(mContext, PowerManager.FULL_WAKE_LOCK);
        Log.d(TAG, "new FloatMoviePlayer");
    }
    /** modify by old Bug493063 @{ */
    public void errorDialogCheckAndDismiss() {
        if (alertDialog != null && alertDialog.isShowing()) {
            Log.d(TAG, "oNError alertDialog");
            alertDialog.dismiss();
        }
    }
    public void alertVideoError(int messageId) {
        Log.d(TAG, "messageId=" + messageId);
        if (currentPlayBackMode == ALL_PLAYBACK) {
            alertDialog = new AlertDialog.Builder(mContext)
                    .setMessage(messageId)
                    .setPositiveButton(com.android.internal.R.string.VideoView_error_button,
                            new android.content.DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // TODO Auto-generated method stub
                                    nextVideo();
                                }
                            }).setCancelable(false)
                    .create();
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            if (alertDialog.isShowing()) {
                alertDialog.dismiss();
            }
            alertDialog.show();
        } else {
            alertDialog = new AlertDialog.Builder(mContext)
                    .setMessage(messageId)
                    .setPositiveButton(com.android.internal.R.string.VideoView_error_button,
                            new android.content.DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // TODO Auto-generated method stub
                                    closeWindow();
                                }
                            }).setCancelable(false)
                    .create();
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            if (alertDialog.isShowing()) {
                alertDialog.dismiss();
            }
            alertDialog.show();
        }
    }
    private void allPlayBack() {
        // TODO Auto-generated method stub
        /** modify by old Bug503434 @{ */
       // nextVideo();
        Log.d(TAG, "mPlayList.size() "+mPlayList.size());
        /**SPRD:Bug474646 Add Drm feature @{ @{ */
        if (VideoDrmUtils.getInstance().isDrmFile()) {
            mMediaPlayer.setNeedToConsume(true);
        }
        /**@}*/
        int n = mPlayList.size();
        if (n == 0) {
            return;
        }
        if (++curVideoIndex >= n) {
            curVideoIndex = 0;
        }
        Log.d(TAG,"curVideoIndex "+curVideoIndex);
        isFileExistCheck("allPlayBack");//SPRD:Bug527349 add
        changeVideo();
        /**@}*/
    }
    private void singlePlayBack() {
        // TODO Auto-generated method stub
        /** modify by old Bug503434 @{ */
       // start();
        /**SPRD:Bug474646 Add Drm feature @{ @{ */
        if (VideoDrmUtils.getInstance().isDrmFile()) {
            mMediaPlayer.setNeedToConsume(true);
        }
        /**@}*/
        int n = mPlayList.size();
        if (n == 0) {
            return;
        }
        /** SPRD:Bug527349 the gallery is crashed then click the next video @{ */
        boolean exist=checkVideoExitOrNot();
        if(!exist){
            closeWindow();
            Toast.makeText(mContext, R.string.video_file_does_not_exist, Toast.LENGTH_SHORT).show( );
        }
        /**@}*/
        changeVideo();
        /**@}*/
    }
    /**@}*/
    public void seekTo(int position) {
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(position);
            mSeekPositionWhenPrepared = 0;
        } else {
            mSeekPositionWhenPrepared = position;
        }
        mPosition = position;
    }

    private boolean isInPlaybackState() {
        Log.d(TAG, "isInPlaybackState mCurrentState " + mCurrentState);
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE && mCurrentState != STATE_PREPARING);
    }

    public void setPhoneState(boolean phoneState) {
        isPhoneCallActivityTop = phoneState;
    }

    public boolean getPhoneState(){
        return isPhoneCallActivityTop;
    }

    public void start() {
        Log.d(TAG, "start");
        /**SPRD:Bug531341 click the play button,the player is not work again @{ */
        if (mMediaPlayer == null) {
            openVideoIfNeed();
        }
        /**@}*/
        if (isInPlaybackState()) {
            mMediaPlayer.start();
            setCurrentState(STATE_PLAYING);
            updateBtnState();
        }
        showBtn();
        mTargetState = STATE_PLAYING;
        if (!mAm.isAudioFocusExclusive()) {
            mAm.requestAudioFocus(
                    afChangeListener, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    /**SPRD:Bug531341 click the play button,the player is not work again @{ */
    public void openVideoIfNeed() {
        if (mUri == null) {
            Log.w(TAG, "openVideo mUri or mSurfaceHolder is null");
            return;
        }
        try {
            if (mMediaPlayer == null) {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setDisplay(mSurfaceHolder);
                mMediaPlayer.setOnErrorListener(this);
                mMediaPlayer.setWakeMode(mContext, PowerManager.FULL_WAKE_LOCK);
            }
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnBufferingUpdateListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(mContext, mUri);
            mMediaPlayer.prepareAsync();
            setCurrentState(STATE_PREPARING);
            mSeekPositionWhenPrepared = 0;
            Log.d(TAG, "setVideoUri end");
            String uriString = mUri.toString();
            if (uriString.startsWith("https://") || uriString.startsWith("http://")
                    || uriString.startsWith("rtsp://")) {
                // mSurfaceView.destroyDrawingCache();
                mSurfaceView.refreshDrawableState();
                mPausePlayBtn.setVisibility(View.INVISIBLE);
                mVideoLoadingTextView.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "error", e);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
        }
    }
    /**@}*/
    public void initTitle() {
        // if(mPlayList == null){
        // return;
        // }
        // MovieInfo movieInfo = mPlayList.get(curVideoIndex);
        // String path = movieInfo.mPath;
        // sprd:change for bug 361907
        getVideoName(mDataIntent);
        String uriString = mUri.toString();
        if (uriString.startsWith("http://") || uriString.startsWith("https://")
                || uriString.startsWith("rtsp://")) {
            mTitle = "";
        }
        Log.e(TAG, "mike  initTitle  mTitle = " + mTitle);
        changedVideoTitle(mTitle);
    }

    private void changedVideoTitle(String path) {
        mTitle = path;
        // bug 355537 begin
        if (path != null) {
            if (mIsTitleShort && path.length() > 8) {
                path = path.substring(0, 4) + "...";
            }
            // bug 355537 end
            Log.e(TAG, "mTitle= " + mTitle);
            mVideoTileTextView.setText(path);
        }
    }

    public void setVideoUri(Uri videoUri) {
        try {
            mUri = videoUri;
            openVideo();
            if (mMediaPlayer != null) {
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(mContext, videoUri);
                mMediaPlayer.prepareAsync();
                setCurrentState(STATE_PREPARING);
                mSeekPositionWhenPrepared = 0;
                Log.d(TAG, "setVideoUri end");
                String uriString = mUri.toString();
                if (uriString.startsWith("https://") || uriString.startsWith("http://")
                        || uriString.startsWith("rtsp://")) {
                    // mSurfaceView.destroyDrawingCache();
                    mSurfaceView.refreshDrawableState();
                    mPausePlayBtn.setVisibility(View.INVISIBLE);
                    mVideoLoadingTextView.setVisibility(View.VISIBLE);
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
       //add for bug532214  java.lang.NullPointerException
        } catch(Exception ex){
            Log.w(TAG, "Unable to open the video" + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
        }
    }

    public void pause() {
        if (isInPlaybackState()) {
            mPosition = mMediaPlayer.getCurrentPosition();
            mMediaPlayer.pause();
            setCurrentState(STATE_PAUSED);
            updateBtnState();
            showBtn();
        }
        mTargetState = STATE_PAUSED;
    }

    public void stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            setCurrentState(STATE_IDLE);
            mTargetState = STATE_IDLE;
        }
    }

    public void openVideo() {
        Log.d(TAG, "openVideo()");
        if (mUri == null) {
            Log.w(TAG, "openVideo mUri or mSurfaceHolder is null");
            return;
        }
        try {
            if (mMediaPlayer == null) {
                mMediaPlayer = new MediaPlayer();
                /**SPRD:Bug531341 click the play button,the player is not work again @{ */
                mMediaPlayer.setDisplay(mSurfaceHolder);
                mMediaPlayer.setOnErrorListener(this);
                mMediaPlayer.setWakeMode(mContext, PowerManager.FULL_WAKE_LOCK);
                /**@}*/
            }
            // mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnBufferingUpdateListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
        } catch (Exception e) {
            Log.e(TAG, "error", e);
        }

    }

    public void release() {
        MediaPlayer tempPlayer;
        tempPlayer = mMediaPlayer;
        mMediaPlayer = null;
        if (tempPlayer != null) {
            tempPlayer.reset();
            tempPlayer.release();
            tempPlayer = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int width, int height) {
        Log.d(TAG, "surface changed" + arg0 + " " + arg1 + " width=" + width + " height=" + height);
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        if (mSeekPositionWhenPrepared > 0) {
            seekTo(mSeekPositionWhenPrepared);
        }
        if (mTargetState == STATE_PLAYING && mCurrentState != STATE_PLAYING) {
            start();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surface created");
        mSurfaceHolder = holder;
        // openVideo();
        if (mMediaPlayer != null) {
            mMediaPlayer.setDisplay(mSurfaceHolder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        Log.d(TAG, "surface destroyed");
        release();
    }

    @Override
    public void onPrepared(MediaPlayer player) {
        setCurrentState(STATE_PREPARED);
        Metadata data = player.getMetadata(MediaPlayer.METADATA_ALL,
                MediaPlayer.BYPASS_METADATA_FILTER);
        if (data != null) {
            mCanSeekBack = !data.has(Metadata.SEEK_BACKWARD_AVAILABLE)
                    || data.getBoolean(Metadata.SEEK_BACKWARD_AVAILABLE);
            mCanSeekForward = !data.has(Metadata.SEEK_FORWARD_AVAILABLE)
                    || data.getBoolean(Metadata.SEEK_FORWARD_AVAILABLE);
        }
        mVideoWidth = mMediaPlayer.getVideoWidth();
        mVideoHeight = mMediaPlayer.getVideoHeight();
        Log.d(TAG, "onPrepared   mUri = " + mUri);
        String uriString = mUri.toString();
        // if(uriString.startsWith("http://") || uriString.startsWith("https://") ||
        // uriString.startsWith("rtsp://")){
        if (mVideoHeight == 0 || mVideoWidth == 0) {
            mSurfaceView.setBackgroundColor(Color.BLACK);
            mVideoWidth = 480;
            mVideoHeight = 320;
        } else {
            mSurfaceView.setBackgroundColor(Color.TRANSPARENT);
        }
        // }
        if (mVideoHeight != 0 && mVideoWidth != 0) {
            if (mSeekPositionWhenPrepared > 0) {
                seekTo(mSeekPositionWhenPrepared);
            }
            Log.d(TAG, "onPrepared mTargetState=" + mTargetState + " mCurrentState="
                    + mCurrentState);
            if (mTargetState == STATE_PLAYING && mCurrentState != STATE_PLAYING) {
                start();
            }
        }
        // preparedLocation();
        reSize();
        // bug 357447 end
        Log.d(TAG, "onPrepared mVideoWidth=" + mVideoWidth + " mVideoHeight=" + mVideoHeight);
        if (uriString.startsWith("https://") || uriString.startsWith("http://")
                || uriString.startsWith("rtsp://")) {
            mPausePlayBtn.setVisibility(View.VISIBLE);
            mVideoLoadingTextView.setVisibility(View.INVISIBLE);
        }
    }

    /* SPRD: add uselessness method
    // bug 357447 begin
    private void preparedLocation() {
        mWindowWidth = mDisplay.getWidth();
        mWindowHeight = mDisplay.getHeight();
        int targetWidth = mVideoWidth;
        int targetHeight = mVideoHeight;
        float widRate = (float) mWindowWidth / mVideoWidth;
        float heiRate = (float) mWindowHeight / mVideoHeight;
        float videoRate = (float) mVideoHeight / mVideoWidth;
        if (widRate > heiRate) {
            targetHeight = (int) (mWindowHeight * mScale);
            if (targetHeight < 216) {
                targetHeight = 216;
            } else if (targetHeight > mWindowHeight) {
                targetHeight = mWindowHeight;
            }
            targetWidth = (int) (targetHeight / videoRate);
        } else if (widRate <= heiRate) {
            targetWidth = (int) (mWindowWidth * mScale);
            if (targetWidth < 216) {
                targetWidth = 216;
            } else if (targetWidth > mWindowWidth) {
                targetWidth = mWindowWidth;
            }
            targetHeight = (int) (targetWidth * videoRate);
        }
        mWmParams.x = (int) ((mWindowWidth - targetWidth) / 2);
        mWmParams.y = (int) ((mWindowHeight - targetHeight) / 2);
    }

    // bug 357447 end   */
    @Override
    public void onCompletion(MediaPlayer arg0) {
        // TODO Auto-generated method stub
        /** modify by old Bug515022 @{ */
        Log.d(TAG, "mUri=" + mUri);
        scheme = ( (mUri == null)? null : mUri.getScheme() );
        if (mPlayList != null){
            if (currentPlayBackMode == SINGLE_PLAYBACK) {
                Log.d(TAG, "single playback mode");
                singlePlayBack();
            }else if(currentPlayBackMode == ALL_PLAYBACK){
                Log.d(TAG, "all playback mode");
                allPlayBack();
            }else{
             // the video is over normally
                onCompletion();
            }
        }else if("file".equalsIgnoreCase(scheme) ||
                "content".equalsIgnoreCase(scheme)){
            if(currentPlayBackMode == SINGLE_PLAYBACK || currentPlayBackMode == ALL_PLAYBACK){
                setVideoUri(mUri);
                seekTo(0);
                start();
            }else{
             // the video is over normally
                onCompletion();
            }
        }else{
            // the video is over normally
            onCompletion();
        }
        /**  @}{ */
    }
    /** modify by old Bug515022 @{ */
    public void onCompletion(){
        Log.d(TAG, "onCompletion");
        /**SPRD:Bug474646 Add Drm feature @{ @{ */
        if (VideoDrmUtils.getInstance().isDrmFile()) {
            mMediaPlayer.setNeedToConsume(true);
        }
        /**@}*/
        release();//SPRD:Bug531341 add
        setCurrentState(STATE_PLAYBACK_COMPLETED);
        mTargetState = STATE_PLAYBACK_COMPLETED;
        updateBtnState();
        mPosition = -1;
    }
    /**  @}{ */
    @Override
    public void onBufferingUpdate(MediaPlayer arg0, int bufferingProgress) {
        if (skbProgress == null) {
            return;
        }
        skbProgress.setSecondaryProgress(bufferingProgress);
        int currentProgress = skbProgress.getMax() * mMediaPlayer.getCurrentPosition()
                / mMediaPlayer.getDuration();
        Log.d(currentProgress + "% play", bufferingProgress + "% buffer");

    }

    public int getCurrentState() {
        return mCurrentState;
    }

    public void setCurrentState(int state) {
        Log.d(TAG, "setCurrentState " + state);
        mCurrentState = state;
    }

    public void updateBtnState() {
        if (mCurrentState == STATE_PLAYING) {
            mPausePlayBtn.setImageResource(R.drawable.tp_btn_float_pause_selector);
        } else {
            mPausePlayBtn.setImageResource(R.drawable.tp_btn_float_play_selector);
        }
    }

    public void addToWindow() {
        Log.d(TAG, "addToWindow");
        mWm = (WindowManager) mContext.getApplicationContext().getSystemService(
                Context.WINDOW_SERVICE);
        mWmParams = new WindowManager.LayoutParams();

        mWmParams.type = LayoutParams.TYPE_PHONE; // set window type
        mWmParams.format = PixelFormat.RGBA_8888;

        // set Window flag
        mWmParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL
                | LayoutParams.FLAG_NOT_FOCUSABLE;

        mWmParams.gravity = Gravity.LEFT | Gravity.TOP;

        mWmParams.x = 0;
        mWmParams.y = 0;

        // set window size
        /**SPRD:modify by old Bug515672 @{*/
        //mWindowWidth = mDisplay.getWidth();
        //mWindowHeight = mDisplay.getHeight();
        mWindowWidth=0;
        mWindowHeight=0;
        /**@}*/
        mWmParams.width = mWindowWidth;
        mWmParams.height = mWindowHeight;

        LayoutInflater inflater = LayoutInflater.from(mContext);
        mFloatLayout = (FloatFrameLayout) inflater.inflate(R.layout.float_movie_view, null);

        mSurfaceView = (SurfaceView) mFloatLayout.findViewById(R.id.float_surfaceview);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mFloatLayout.setOnTouchListener(this);
        mFloatLayout.setOnConfigurationChangedListener(new OnConfigurationChangedListener() {
            @Override
            public boolean onConfigurationChanged(Configuration newConfig) {
                Log.d(TAG,"onConfigurationChanged");
                // preparedLocation();
                reSize();
                return false;
            }
        });

        mPausePlayBtn = (ImageView) mFloatLayout.findViewById(R.id.btn_pause);
        mPausePlayBtn.setOnClickListener(this);
        mBackToNormalBtn = (ImageView) mFloatLayout.findViewById(R.id.btn_back_to_normal);
        mBackToNormalBtn.setOnClickListener(this);
        mCloseWindowBtn = (ImageView) mFloatLayout.findViewById(R.id.btn_close_win);
        mCloseWindowBtn.setOnClickListener(this);
        mPreVideoBtn = (ImageView) mFloatLayout.findViewById(R.id.btn_pre_video);
        mPreVideoBtn.setOnClickListener(this);
        mNextVideoBtn = (ImageView) mFloatLayout.findViewById(R.id.btn_next_video);
        mNextVideoBtn.setOnClickListener(this);
        mVideoTileTextView = (TextView) mFloatLayout.findViewById(R.id.video_title);
        mVideoLoadingTextView = (TextView) mFloatLayout.findViewById(R.id.video_load);

        mWm.addView(mFloatLayout, mWmParams);

        mAm = ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE));
        mAm.requestAudioFocus(
                afChangeListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
    }

    public void removeFromWindow() {
        Log.d(TAG, "removeFromWindow");
        if (mFloatLayout != null) {
            mWm.removeView(mFloatLayout);
            mFloatLayout = null;
        }
        mAm.abandonAudioFocus(afChangeListener);
    }

    public void showBtn() {
        Log.d(TAG, "showBtn");
        Boolean state = mPlayList == null?true :false;
        Log.d(TAG, "mPlayList  is "+state);
        int visibility = mVideoLoadingTextView.getVisibility();
        if (visibility == View.INVISIBLE || visibility == View.GONE) {
            mPausePlayBtn.setVisibility(View.VISIBLE);
        } else {
            mPausePlayBtn.setVisibility(View.INVISIBLE);
        }
        mBackToNormalBtn.setVisibility(View.VISIBLE);
        mCloseWindowBtn.setVisibility(View.VISIBLE);
        String uriString = mUri.toString();
        if (uriString.startsWith("https://") || uriString.startsWith("http://")
                || uriString.startsWith("rtsp://")) {
            mPreVideoBtn.setVisibility(View.INVISIBLE);
            mNextVideoBtn.setVisibility(View.INVISIBLE);
        } else {
            if (mPlayList != null && mPlayList.size() > 1) {
                mPreVideoBtn.setVisibility(View.VISIBLE);
                mNextVideoBtn.setVisibility(View.VISIBLE);
            } else {
                mPreVideoBtn.setVisibility(View.INVISIBLE);
                mNextVideoBtn.setVisibility(View.INVISIBLE);
            }
        }
        if (!mHideTitle) {
            mVideoTileTextView.setVisibility(View.VISIBLE);
        }
        if (mCurrentState == STATE_PLAYBACK_COMPLETED && mFloatLayout != null) {
            mWm.updateViewLayout(mFloatLayout, mWmParams);
        }
        mHandler.removeCallbacks(startHidingRunnable);
        if (mCurrentState == STATE_PLAYING || mCurrentState == STATE_PREPARED) {
            mHandler.postDelayed(startHidingRunnable, 7000);
        }
    }

    public void hiddenBtn() {
        Log.d(TAG, "hiddenBtn");
        mPausePlayBtn.setVisibility(View.INVISIBLE);
        mBackToNormalBtn.setVisibility(View.INVISIBLE);
        mCloseWindowBtn.setVisibility(View.INVISIBLE);
        mPreVideoBtn.setVisibility(View.INVISIBLE);
        mNextVideoBtn.setVisibility(View.INVISIBLE);
        mVideoTileTextView.setVisibility(View.INVISIBLE);
    }

    public void toggleBtn() {
        if (mPausePlayBtn.getVisibility() == View.VISIBLE) {
            hiddenBtn();
        } else {
            showBtn();
        }
    }

    public boolean handleTouchEvent(MotionEvent event) {
        mLastX = mX;
        mLastY = mY;
        // get coordinate on window
        mX = event.getRawX();
        mY = event.getRawY() - mStatusBarHeight;

        // Log.d(TAG, "x=" + x + " y=" + y);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // get coordinate on view
                mTouchStartX = event.getX();
                mTouchStartY = event.getY();

                mLastTouchDownTime = mCurTouchDownTime;
                mCurTouchDownTime = System.currentTimeMillis();
                if (mCurTouchDownTime - mLastTouchDownTime < 400) {
                    // DoubleTap
                    // goToNormalPlay();
                    mCurTouchDownTime = 0;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mTouchStartX != 0) {
                    if (Math.abs(mLastY - mY) > 10 || Math.abs(mLastX - mX) > 10) {
                        updateViewPosition();
                        showBtn();
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                mTouchStartX = mTouchStartY = 0;

                if (mCurTouchDownTime - mLastTouchDownTime > 400
                        && System.currentTimeMillis() - mCurTouchDownTime < 500) {
                    toggleBtn();
                }
                break;
        }
        return true;
    }

    private void updateViewPosition() {
        if (mFloatLayout != null) {
            mWmParams.x = (int) (mX - mTouchStartX);
            mWmParams.y = (int) (mY - mTouchStartY);
            mWm.updateViewLayout(mFloatLayout, mWmParams);
        }
    }

    private void goToNormalPlay() {
        int state = mCurrentState == STATE_PLAYING ? STATE_PLAYING : STATE_PAUSED;
        if (mCurrentState == STATE_PLAYING) {
            mPosition = mMediaPlayer.getCurrentPosition();
        }
        removeFromWindow();
        Intent intent;
        if (mCurrentState == STATE_PLAYBACK_COMPLETED) {
            intent = new Intent(Intent.ACTION_MAIN);
            intent.setClass(mContext, VideoActivity.class);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
        } else {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.putExtras(mDataIntent);
            intent.setClassName("com.android.gallery3d", "com.sprd.gallery3d.app.Video");
            intent.setData(mUri);
            if (mCanSeekBack && mCanSeekForward) {
                intent.putExtra("position", mPosition);
            } else {
                intent.putExtra("position", 0);
            }
            intent.putExtra("currentstate", state);
            intent.putExtra("clearDialog", true);
            intent.putExtra("isConsumed", true);
            intent.putExtra(FLAG_GALLERY,mIsStartByGallery);
            intent.putExtra(Intent.EXTRA_TITLE, mTitle);
        }
        /**SPRD:532462
         * SPRD:add Intent.FLAG_GRANT_READ_URI_PERMISSION to grant read EmlAttachmentProvider permission
         */
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

        mContext.startActivity(intent);
        ((Service) mContext).stopSelf();

    }

    public void closeWindow() {
        removeFromWindow();
        ((Service) mContext).stopSelf();
    }

    public void setDataIntent(Intent intent) {
        mDataIntent = intent;
        mIsStartByGallery=mDataIntent.getBooleanExtra(FLAG_GALLERY, false);
        currentPlayBackMode = intent.getIntExtra("currentPlaybackMode", 0);//SPRD:modify by old bug496063
        initPlayList(mIsStartByGallery);
    }

    private void initPlayList(Boolean isStartByGallery) {
        mPlayList = null;
        if(!isStartByGallery){
            return;
        }
        Uri uri = mDataIntent.getData();
        if(uri == null){
            return;
        }
        Log.d(TAG, "initPlayList");
        String UriID = uri.toString().substring(
                uri.toString().lastIndexOf("/") + 1, uri.toString().length());
        Cursor cursorbucket_id = mContext.getContentResolver().query(videoListUri, new String[] {
                    "bucket_id,bucket_display_name"}, "_id=?", new String[]{UriID}, null);
        String bucket_id = null;
        try {
            if (cursorbucket_id != null) {
                cursorbucket_id.moveToFirst();
                bucket_id = cursorbucket_id.getString(0);
                mAlbum = cursorbucket_id.getString(1);
                Log.d(TAG, "initPlayList   mAlbum = "+mAlbum);
            }
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            if (cursorbucket_id != null) {
                cursorbucket_id.close();
                cursorbucket_id = null;
            }
        }

        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(videoListUri, new String[] {
                    "_display_name", "_id", "_data" }, "bucket_id=?",
                    new String[] { bucket_id }, VideoColumns.DATE_TAKEN
                            + " DESC, " + VideoColumns._ID + " DESC");
            Log.d(TAG, "cursor ");
            if (cursor != null) {

                int n = cursor.getCount();
                Log.d(TAG, "cursor.getCount " + n);
                cursor.moveToFirst();
                LinkedList<MovieInfo> playList2 = new LinkedList<MovieInfo>();
                for (int i = 0; i < n; i++) {
                    MovieInfo mInfo = new MovieInfo();
                    mInfo.mAlbum = cursor.getString(2);
                    mInfo.mID = cursor.getString(1);
                    mInfo.mPath = cursor.getString(0);
                    playList2.add(mInfo);
                    cursor.moveToNext();
                }
                mPlayList = playList2;

            } else {
                Log.i(TAG, "cursor == null");
            }
        } catch (Exception e) {
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
        String scheme = uri.getScheme();
        if ("content".equalsIgnoreCase(scheme)) {
            if(mPlayList != null){
                for (int i = 0; i < mPlayList.size(); i++) {
                    if (UriID.equalsIgnoreCase(mPlayList.get(i).mID)) {
                        curVideoIndex = i;
                        break;
                    }
                }
            }
        }
    }

    class MovieInfo {
        String mAlbum;
        String mID;
        String mPath;
    }

    /**
     * get state bar height
     *
     * @return
     */
    public int getStatusBarHeight() {
        Class<?> c = null;
        Object obj = null;
        java.lang.reflect.Field field = null;
        int x = 0;
        int statusBarHeight = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            statusBarHeight = mContext.getResources().getDimensionPixelSize(x);
            return statusBarHeight;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusBarHeight;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getPointerCount() > 1) {
            mScaleGestureDetector.onTouchEvent(event);
            mTouchStartX = mTouchStartY = 0;
            return true;
        }
        handleTouchEvent(event);
        return false;
    }

    public int getPosition() {
        if (mMediaPlayer != null
                && (mCurrentState == STATE_PAUSED || mCurrentState == STATE_PLAYING)) {
            mPosition = mMediaPlayer.getCurrentPosition();
            return mPosition;
        } else if (mMediaPlayer == null) {
            return mPosition;
        } else {
            return 0;
        }
    }

    public void reSize() {
        if (mMediaPlayer == null || mFloatLayout == null) {
            return;
        }
        Point point = new Point();
        mDisplay.getSize(point);
        mWindowWidth = point.x;
        mWindowHeight = point.y;
        int stateHeight = mFloatLayout.getStateHeight();
        int statusBarHeight = getStatusBarHeight();
        int targetWidth = mVideoWidth;
        int targetHeight = mVideoHeight;
        float widRate = (float) mWindowWidth / mVideoWidth;
        float heiRate = (float) mWindowHeight / mVideoHeight;
        // the ratio of height to width;
        float videoAspectRatio = (float) mVideoHeight / mVideoWidth;
        // bug 355537 begin
        // targetWidth = (int) (mSurfaceWidth * scale);
        // if (targetWidth < 216) {
        // targetWidth = 216;
        // } else if (targetWidth > mWindowWidth) {
        // targetWidth = mWindowWidth;
        // }
        Log.e(TAG, "mScale= " + mScale);
        if (widRate > heiRate) {
            targetHeight = (int) (mWindowHeight * mScale);
            // SPRD: add fix the bug 531300
            if (targetHeight < MIN_HEIGHT_IN_LANDSCAPE) {
                targetHeight = MIN_HEIGHT_IN_LANDSCAPE;
            } else if (targetHeight > mWindowHeight - statusBarHeight) {

                targetHeight = mWindowHeight - statusBarHeight;
            }
            targetWidth = (int) (targetHeight / videoAspectRatio);
        } else if (widRate <= heiRate) {
            targetWidth = (int) (mWindowWidth * mScale);
            if (targetWidth < MIN_WIDTH_IN_PORTRAIT) {
                targetWidth = MIN_WIDTH_IN_PORTRAIT;
            } else if (targetWidth > mWindowWidth) {
                targetWidth = mWindowWidth;
            }
            targetHeight = (int) (targetWidth * videoAspectRatio);
            if (targetHeight > mWindowHeight - statusBarHeight) {
                targetHeight = mWindowHeight - statusBarHeight;
                targetWidth = (int) (targetHeight / videoAspectRatio);
            }
        }
        // bug 355537 end

        Log.d(TAG, "reSize  targetWidth=" + targetWidth + " targetHeight=" + targetHeight);
        if (Math.abs(mWmParams.width - targetWidth) > 5
                || Math.abs(mWmParams.height - targetHeight) > 5) {
            mWmParams.width = targetWidth;
            mWmParams.height = targetHeight;
            if (targetWidth < 288 || targetHeight < 288) {
                // bug 355537 begin
                if (mTitle != null && mTitle.length() > 8) {
                    String title = mTitle.substring(0, 4) + "...";
                    mIsTitleShort = true;
                    mVideoTileTextView.setText(title);
                }
                // bug 355537 end
            } else {
                // bug 355537 begin
                if (mIsTitleShort) {
                    mIsTitleShort = false;
                    mVideoTileTextView.setText(mTitle);
                }
                // bug 355537 end
            }
            mWm.updateViewLayout(mFloatLayout, mWmParams);
        }

    }

    public class ScaleGestureListener implements ScaleGestureDetector.OnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            float scale = detector.getScaleFactor();
            /* SPRD: add for modify the value of mScale out of range float possibility @{ */
            if ( MIN_SCALE <= mScale && mScale <= MAX_SCALE) {
                mScale = mScale * scale;
                reSize();
            } else if ( mScale <= MIN_SCALE) {
                // modify value
                mScale = MIN_SCALE;
            } else if ( mScale >= MAX_SCALE ) {
                // modify value
                mScale = MAX_SCALE;
            }
            /* @} */

            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            // TODO Auto-generated method stub
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            // TODO Auto-generated method stub

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_pause:
                if (!isPhoneCallActivityTop) {
                    if (mCurrentState == STATE_PLAYING) {
                        pause();
                    } else {
                        start();
                    }
                }
                break;
            case R.id.btn_close_win:
                closeWindow();
                break;
            case R.id.btn_back_to_normal:
                if (!isPhoneCallActivityTop) {
                    goToNormalPlay();
                }
                break;
            case R.id.btn_pre_video:
                if (!isPhoneCallActivityTop) {
                    Log.d(TAG, "mike  onClick   btn_pre_video");
                    preVideo();
                }
                break;
            case R.id.btn_next_video:
                if (!isPhoneCallActivityTop) {
                    Log.d(TAG, "mike  onClick   btn_next_video");
                    nextVideo();
                }
                break;
            default:
                break;
        }
    }

    private void nextVideo() {
        Log.d(TAG, "nextVideo");
        /**SPRD:Bug474646 Add Drm feature @{ @{ */
        if (VideoDrmUtils.getInstance().isDrmFile()) {
            mMediaPlayer.setNeedToConsume(true);
        }
        /**@}*/
        if (mPlayList != null) {
            if (curVideoIndex == mPlayList.size() - 1) {
                curVideoIndex = 0;
            } else {
                ++curVideoIndex;
            }
            isFileExistCheck("nextVideo");
            changeVideo();
        }
    }

    private void preVideo() {
        /**SPRD:Bug474646 Add Drm feature @{ @{ */
        if (VideoDrmUtils.getInstance().isDrmFile()) {
            mMediaPlayer.setNeedToConsume(true);
        }
        /**@}*/
        if (mPlayList != null) {
            if (curVideoIndex <= 0) {
                curVideoIndex = mPlayList.size() - 1;
            } else {
                --curVideoIndex;
            }
            isFileExistCheck("preVideo");
            changeVideo();
        }
    }

    private void isFileExistCheck(String options) {
        MovieInfo movieInfo = mPlayList.get(curVideoIndex);
        String moviePath = movieInfo.mAlbum;
        File file = new File(moviePath);
        int count=0;//SPRD:Bug527349 add
        while (file == null || !file.exists()) {
            if (options.equals("preVideo")) {
                if (curVideoIndex <= 0) {
                    curVideoIndex = mPlayList.size() - 1;
                } else {
                    --curVideoIndex;
                }
            } else {
                if (curVideoIndex == mPlayList.size() - 1) {
                    curVideoIndex = 0;
                } else {
                    ++curVideoIndex;
                }
            }
            movieInfo = mPlayList.get(curVideoIndex);
            moviePath = movieInfo.mAlbum;
            file = new File(moviePath);
            /** SPRD:Bug527349 the gallery is crashed then click the next video @{ */
            count++;
            if(mPlayList.size() == count){
                closeWindow();
                Toast.makeText(mContext, R.string.video_file_does_not_exist, Toast.LENGTH_SHORT).show( );
                break;
            }
            /**@}*/
        }
    }

    /** SPRD:Bug527349 the gallery is crashed then click the next video @{ */
    public boolean checkVideoExitOrNot() {
        MovieInfo movieInfo = mPlayList.get(curVideoIndex);
        String moviePath = movieInfo.mAlbum;
        File file = new File(moviePath);
        if (!file.exists()) {
            return false;
        }
        return true;
    }
    /**@}*/

    private void changeVideo() {
        MovieInfo movieInfo = mPlayList.get(curVideoIndex);
        String id = movieInfo.mID;
        Uri uri = videoListUri.buildUpon().appendPath(id).build();
        Log.d(TAG, "changeVideo   uri = " + uri);
        changedVideoTitle(movieInfo.mPath);
        setVideoUri(uri);
        seekTo(0);
        errorDialogCheckAndDismiss();//SPRD:Bug515022
        start();
    }

    private void getVideoName(Intent intent) {
        if (mAlbum != null && mDataIntent.getData() != null && mPlayList != null) {
            MovieInfo movieInfo = mPlayList.get(curVideoIndex);
            mTitle = movieInfo.mPath;
        } else {
            mTitle = mDataIntent.getStringExtra(Intent.EXTRA_TITLE);
        }
        if (mTitle != null) {
            return;
        } else if ("file".equals(mUri.getScheme())) {
            mTitle = mUri.getLastPathSegment();
        } else if ("http".equalsIgnoreCase(mUri.getScheme())
                || "rtsp".equalsIgnoreCase(mUri.getScheme())) {
            mTitle = mUri.getLastPathSegment();
        } else {
            AsyncQueryHandler queryHandler =
                    new AsyncQueryHandler(mContext.getContentResolver()) {
                        @Override
                        protected void onQueryComplete(int token, Object cookie,
                                Cursor cursor) {
                            try {
                                if ((cursor != null) && cursor.moveToFirst()) {
                                    String displayName = cursor.getString(0);
                                    mTitle = displayName;
                                }
                            } finally {
                                Utils.closeSilently(cursor);
                            }
                        }
                    };
            queryHandler.startQuery(0, null, mUri,
                    new String[] {
                        OpenableColumns.DISPLAY_NAME
                    }, null, null,
                    null);
        }
    }
    @Override
    public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
        // TODO Auto-generated method stub
        errorDialogCheckAndDismiss();// SPRD:modify by old Bug493063
        int messageId;
        if (framework_err == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
            messageId = com.android.internal.R.string.VideoView_error_text_invalid_progressive_playback;
        } else {
            messageId = com.android.internal.R.string.VideoView_error_text_unknown;
        }
        alertVideoError(messageId);
        return true;
    }
}
