/*:
 * Copyright (C) 2007 The Android Open Source Project
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

import java.util.LinkedList;
import java.util.List;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.KeyguardManager;
import android.app.Service;
import android.app.ActionBar.OnMenuVisibilityListener;
import android.app.ActivityManager;
import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnKeyListener;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.Video.VideoColumns;
import android.provider.OpenableColumns;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.GalleryUtils;
/** SPRD:Bug474632 bookmark setting @{ */
import com.sprd.gallery3d.app.MovieBookmarks;
import com.sprd.gallery3d.app.MovieViewContentProvider;
import com.sprd.gallery3d.app.PermissionsActivity;
import com.sprd.gallery3d.cmcc.VideoCmccUtils;
/** @ } */
/**SPRD:Bug474646 Add Drm feature @{  @{ */
import com.sprd.gallery3d.drm.VideoDrmUtils;
/** @ } */
import com.sprd.gallery3d.app.MovieViewProxySet;
import com.sprd.gallery3d.app.VideoActivity;
import com.android.gallery3d.util.GalleryUtils;//SPRD:Bug510007  check storage permission

import com.sprd.android.config.OptConfig;

/**
 * This activity plays a video from a specified URI.
 *
 * The client of this activity can pass a logo bitmap in the intent (KEY_LOGO_BITMAP)
 * to set the action bar logo so the playback process looks more seamlessly integrated with
 * the original activity.
 */
public class MovieActivity extends Activity {
    @SuppressWarnings("unused")
    private static final String TAG = "MovieActivity";
    public static final String KEY_LOGO_BITMAP = "logo-bitmap";
    public static final String KEY_TREAT_UP_AS_BACK = "treat-up-as-back";

    private MoviePlayer mPlayer;
    private boolean mFinishOnCompletion;
    private Uri mUri;
    private boolean mTreatUpAsBack;
    /** SPRD:Bug474632 bookmark setting @{ */
    //  private final Handler mHandler = new Handler(); //SRPD:Bug474639 remove
    /** @ } */

    /* SPRD: add for mutex between float window and camera @{ */
    public static MovieActivity mMovieActivity = null;
    public boolean mIsFloatWindowDisabled = false;
    /* @} */

    /**SPRD:Bug474646 Add Drm feature @{  @{ */
    public String mFilePath = null;// SPRD:bug 474646 add Drm feature old bug 318505
    /** @ } */
    /** SPRD:Bug521702 the music player and the video player can play at the same time @{ */
    private boolean mFocused = false;
    private AudioManager mAudioManager;
    /** @ } */
    /** SPRD:Bug474591 add new parameters @{ */
    private Menu mMenu = null;
    private Intent mIntent;
    private View mRootView;
    /** @}*/
    /** SPRD:Bug474600 improve video control functions
     * add new parameters @{ */
    private Uri videoListUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    private ContentResolver mContentResolver;
    private LinkedList<MovieInfo> mPlayList;
    private Bundle mSavedInstanceState;
    public boolean isFirst = true;
    /**@}*/
    /** Bug474615 Playback loop mode
     * add new parameters @{ */
    private String[] playbackMode = null;
    private int currentPlaybackMode = 0;
    private static final String FLAG_GALLERY="startByGallery";
    /**@}*/
    //SPRD: bug 526178 just show resume dialog once.
    public boolean mShowResumeDialog = true;
    //wait for Bug474630 commit
    //private boolean optionsSelectedLoudSpeakerOn = false;
    private boolean mControlResumed = false;//SPRD:Bug528547 add
    /** Bug 474610 imput url for go-to research
     * add new parameters @{ */
    private MenuItem shareItem = null;
    /** @}*/
    /** Bug474621 screen size setting
     * add new parameters @{ */
    private boolean isFullScreen = true;
    /**@}*/
    /**SPRD:Bug474618 Channel volume settings
     * add new parameters*/
    private float[] mVolumes = new float[] { 1, 1 };
    private float[] mVolumesTmp = new float[] { 1, 1 };
    private static final int VOLUME_LEFT = 0;
    private static final int VOLUME_RIGHT = 1;
    /**@}*/
    /** SPRD:Bug 474639 add phone call reaction @{ */
    private static final int SHOW_CALLING_DIALOG = 1;
    private static final int MOVIEVIEW_ON_RESUME = 2;
    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;
    private AlertDialog mCallingDialog;
    public boolean mPhoneState = false;
    //private boolean mIsMute = false; SPRD:Bug474639 remove for Bug540980
    //public int mPreVolume = 0; SPRD:Bug474639 remove for Bug540980
    private boolean isheadSetmount = false;
    public boolean isVolumeCorrect = true;
    private boolean mIsPaused = false;
    private final Handler mHandler = new MovePlayerHandler();
    private boolean mIsOnPause = false;//SPRD:Bug534454 modify by old Bug517683
    /** @} */
    /** SPRD:Bug474630 switching voice devices @{ */
    private MenuItem audioDeviceMenu = null;
    private boolean isLoudSpeakerOn = false;
    private boolean interruptState = false;
    private boolean optionsSelectedLoudSpeakerOn = false;
    private boolean mIsPausedByFocusLoss = false; //SPRD:add for bug555909
    /** @} */
    private ExternalMountedReceiver mExternalMountedReceiver;//SPRD:Bug538278 add
    private BroadcastReceiver mShutReceiver;//SPRD:Bug537666 add
    private BroadcastReceiver mHallOnReceiver;//Bug50066
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setSystemUiVisibility(View rootView) {
        if (ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_LAYOUT_STABLE) {
            rootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }
    /** SPRD:Bug521702  the music player and the video player can play at the same time @{ */
  //modify by old bug497432
    OnAudioFocusChangeListener afChangeListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            Log.d(TAG, "audiofocusChange:" + focusChange);
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
    //                break;
                case AudioManager.AUDIOFOCUS_LOSS:
                   if(mPlayer != null && mPlayer.isPlaying() && !mPhoneState){
                      mPlayer.onPauseVideo();
                      mIsPausedByFocusLoss = true;//SPRD:add for bug555909
                   }
                    break;
                /**SPRD:Bug531413  modify by old bug521106 for video replay @{ */
                case AudioManager.AUDIOFOCUS_GAIN://add for bug521106 when alarm clock comes
                    //stop the video player and resume playing when the alarm is stopped or postponed
                    Log.d(TAG, "AUDIOFOCUS_GAIN");
                    //SPRD:add for bug555909 pause the video,call in,get the phone call.when dismissing the call,the video will be playing in background for seconds
                    if(mPlayer != null && !mPlayer.isPlaying() && !mPhoneState && mIsPausedByFocusLoss){
                        mPlayer.onPlayVideo();
                        mIsPausedByFocusLoss = false;//SPRD:add for bug555909
                        Log.d(TAG, "onPlayVideo");
                    }
                    break;
                /**@}*/
                default:
                    break;
            }
        }
    };
    /** @ } */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // SPRD: add for mutex between float window and camera
        mMovieActivity = this;

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        /** SPRD:Bug474591 improve video control functions
         * modify and add some parameters @{ */
        mIntent = getIntent();
        mUri = mIntent.getData();
        if (mUri == null) {
            Log.d(TAG, "mUri->Empty uri,finish.");
            finish();
            return;
        }
        /** @}*/
        /** SPRD:bug 474614: porting float play @{ */
        // old bug info:339523 begin
        if (isServiceRunning(this, FloatPlayerService.class.getName())) {
            //wait for phone call feature merge
            //if (!isPhoneCallActivityTop()) {
            // SPRD: Add for bug526156, check extra permissions for MovieActvity @{
            if (!(GalleryUtils.checkStoragePermissions(this) && GalleryUtils.checkReadPhonePermissions(this))) {
                // do nothing
             // @}
            } else {
                String scheme = mUri.getScheme();
                Intent serviceIntent = new Intent(this, FloatPlayerService.class);
                if ("http".equalsIgnoreCase(scheme)
                        || "rtsp".equalsIgnoreCase(scheme)) {
                    stopService(serviceIntent);
                } else {
                    Log.d(TAG, "MovieActivity onResume Uri = " + mUri);
                    int position = mIntent.getIntExtra("position", 0);
                    Log.d(TAG, "oncreate, position=" + position);
                    serviceIntent.putExtras(mIntent);
                    serviceIntent.setData(mUri);
                    serviceIntent.putExtra("position", position);
                    // SPRD: Modify for bug552803, need add permission to read URI for FloatPlayerService
                    serviceIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    getApplicationContext().startService(serviceIntent);
                    finish();
                    return;
                }
            }
            /* wait for phone call feature merge
            } else {
                finish();
                return;
            }*/
        }

        /** SPRD: bug 526178 just show resume dialog once. @ {*/
        if (getIntent().getBooleanExtra("clearDialog", false)) {
            mShowResumeDialog = false;
        }
        /** @ }*/
        // bug 339523 end
        /** @} */
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        /**SPRD:Bug474600 improve video control functions
         * add new value @{*/
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        /**@}*/
        setContentView(R.layout.movie_view);
        /**
         *SPRD:Bug474591 remove rootView @{
         *@orig
         View rootView = findViewById(R.id.movie_view_root);
         setSystemUiVisibility(rootView);
         * @}
         */
        mRootView = findViewById(R.id.movie_view_root);

        setSystemUiVisibility(mRootView);
        Log.d(TAG, "onCreate");
        /** SPRD:bug 474614: porting float play @{ */
        currentPlaybackMode = mIntent.getIntExtra("currentPlaybackMode", 0);
        isLoudSpeakerOn = mIntent.getBooleanExtra("LoudSpeakerOn",false);
        isFullScreen = mIntent.getBooleanExtra("FullScreen",true);
        mVolumes[VOLUME_LEFT]=mVolumesTmp[VOLUME_LEFT] = mIntent.getFloatExtra("left",1.0f);
        mVolumes[VOLUME_RIGHT]=mVolumesTmp[VOLUME_RIGHT] = mIntent.getFloatExtra("right",1.0f);
        /**  @}*/

        // SPRD: add for mutex between float window and camera
        mIsFloatWindowDisabled = mIntent.getBooleanExtra("disable-float-window", false);

        /**
         * SPRD:Bug474591 remove rootView @{
         * @orig
         * Intent intent = getIntent();
         * initializeActionBar(intent); @}
         */
        /** SPRD:Bug474600 improve video control functions
         * modify and add new parameters @{ */
        mContentResolver = getContentResolver();
        mSavedInstanceState = savedInstanceState;
        /** @}*/
        initializeActionBar(mIntent);
        mFinishOnCompletion = mIntent.getBooleanExtra(
                MediaStore.EXTRA_FINISH_ON_COMPLETION, true);
        mTreatUpAsBack = mIntent.getBooleanExtra(KEY_TREAT_UP_AS_BACK, false);
        // SPRD:modify for bug533589:change the place where we add the listener in case of the wrong initial of the MoviePlayer @{
        /** SPRD:Bug 474639 add phone call reaction @{ */
        // wait for SPRD: Bug 281076 Listening the call state
        mTelephonyManager = TelephonyManager.from(this);
        mSubscriptionManager = SubscriptionManager.from(this);
        mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        /** @ } */
        //@}
     /**SPRD:Bug474600 improve video control functions
      * remove @{
      * @orig
        mPlayer = new MoviePlayer(mRootView, this, mIntent.getData(),
                savedInstanceState, !mFinishOnCompletion) {
            @Override
            public void onCompletion() {
                if (mFinishOnCompletion) {
                    finish();
                }
            }
        };
       *@}
       */

        /**
         * SRRD:Bug474600 improve video control functions
         * add to initialize the mPlayer's parameters
         * and get the target video
         * information list
         * @{
         */
        /** SPRD:Bug 474639 add phone call reaction @{ */
        //wait for  SPRD: Bug 281076 Listening the call state
        //mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        /** @ } */
        /**SPRD:Bug474615 Playback loop mode @{*/
        Boolean isStartByGallery=mIntent.getBooleanExtra(FLAG_GALLERY, false);
        initPlayList(isStartByGallery);
        /**  @}*/
        initPlayerIfNeed();
        /**  @}*/

        if (mIntent.hasExtra(MediaStore.EXTRA_SCREEN_ORIENTATION)) {
            int orientation = mIntent.getIntExtra(
                    MediaStore.EXTRA_SCREEN_ORIENTATION,
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            if (orientation != getRequestedOrientation()) {
                setRequestedOrientation(orientation);
            }
        }
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
        winParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        win.setAttributes(winParams);

        // We set the background in the theme to have the launching animation.
        // But for the performance (and battery), we remove the background here.
        win.setBackgroundDrawable(null);

        registerShutReceiver();//SPRD:Bug537666 add
        registerExternalMountedReceiver();//SPRD:Bug538278 add
        registerHallOn();//Bug49967
        /* SPRD: Modify for bug544367, modify permission check behavior @{ */
        /** SPRD:Bug510007 check storage & phone permission @{ */
        if (!(GalleryUtils.checkReadPhonePermissions(this) && GalleryUtils
                .checkStoragePermissions(this))) {
            Intent startIntent = new Intent(this, PermissionsActivity.class);
            int startby = mIntent.getIntExtra(PermissionsActivity.UI_START_BY,
                    PermissionsActivity.START_FROM_MOVIE);
            startIntent.putExtra(PermissionsActivity.UI_START_BY, startby);
            if (savedInstanceState != null) {
                startIntent.putExtras(savedInstanceState);
            }
            startActivityForResult(startIntent, PermissionsActivity.RESULT_FOR_MOVIE);
            return;
        }
        /**@}*/
        /* @} */
    }

    /** SPRD:bug 474614: porting float play @{ */
    @Override
    protected void onNewIntent(Intent intent) {
        // TODO Auto-generated method stub
        super.onNewIntent(intent);
        setIntent(intent);
        /** SPRD:Bug474591 improve video control functions
         * modify and add some parameters @{ */
        mIntent = getIntent();
        mUri = mIntent.getData();
        Log.d(TAG,"MovieActivity onNewIntent onCreate Uri = " + mUri);
        if (mUri == null) {
            Log.d(TAG, "onNewIntent mUri->Empty uri,finish.");
            finish();
            return;
        }
        /** @}*/
        /** SPRD:bug 474614: porting float play @{ */
        // old bug info:339523 begin
        if (isServiceRunning(this, FloatPlayerService.class.getName())) {
            String scheme = mUri.getScheme();
            Intent serviceIntent = new Intent(this, FloatPlayerService.class);
            if ("http".equalsIgnoreCase(scheme)
                    || "rtsp".equalsIgnoreCase(scheme)) {
                stopService(serviceIntent);
            } else {
                Log.d(TAG,"MovieActivity onResume Uri = " + mUri);
                int position = mIntent.getIntExtra("position", 0);
                Log.d(TAG, "oncreate, position=" + position);
                serviceIntent.putExtras(mIntent);
                serviceIntent.setData(mUri);
                serviceIntent.putExtra("position", position);
                getApplicationContext().startService(serviceIntent);
                finish();
                return;
            }
        }
        /** SPRD: bug 526178 just show resume dialog once. @ {*/
        if (getIntent().getBooleanExtra("clearDialog", false)) {
            mShowResumeDialog = false;
        }
        /** @ }*/
        // bug 339523 end
        initializeActionBar(mIntent);
        mFinishOnCompletion = mIntent.getBooleanExtra(
                MediaStore.EXTRA_FINISH_ON_COMPLETION, true);
        mTreatUpAsBack = mIntent.getBooleanExtra(KEY_TREAT_UP_AS_BACK, false);
        /**SPRD:Bug474615 Playback loop mode @{*/
        Boolean isStartByGallery=mIntent.getBooleanExtra(FLAG_GALLERY, false);
        initPlayList(isStartByGallery);
        /**  @}*/
        initPlayerIfNeed(mIntent);
        /**  @}*/
        if (mIntent.hasExtra(MediaStore.EXTRA_SCREEN_ORIENTATION)) {
            int orientation = mIntent.getIntExtra(
                    MediaStore.EXTRA_SCREEN_ORIENTATION,
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            if (orientation != getRequestedOrientation()) {
                setRequestedOrientation(orientation);
            }
        }

    }

    private void initPlayerIfNeed(Intent intent) {
        Log.d(TAG, "mPlayer is Create!");
        if (mPlayer != null) {
            mPlayer.onDestroy();
            mPlayer.releaseView(mRootView);
            mPlayer = null;
        }
        mPlayer = new MoviePlayer(mRootView, this, intent.getData(),
               mPlayList, mSavedInstanceState, !mFinishOnCompletion) {
            @Override
            public void onCompletion() {
                if (mFinishOnCompletion) {
                    Log.e(TAG, "XX@@finish when completeXX@@");
                    finish();
                }
            }
        };
        /** SPRD:bug 474614: porting float play @{ */
        mPlayer.setPlayBackMode(currentPlaybackMode);
        /** @} */
        mPlayer.setChannel(mVolumesTmp[VOLUME_LEFT],
        mVolumesTmp[VOLUME_RIGHT]);
        //wait for /** SPRD:Bug 474641 add reciver MMS&SMS Control @{ */
        VideoCmccUtils.getInstance().initPlayer(mPlayer);
        /** @} */
        /**SPRD:Bug474646 Add Drm feature @{  @{ */
        VideoDrmUtils.getInstance().getFilePathByUri(mUri, this);// SPRD:bug 474646 Add Drm feature
        VideoDrmUtils.getInstance().needToConsume(!mIntent.getBooleanExtra("isConsumed", false));
        /** @ } */
    }
    /** @ } */
    /** SPRD:Bug474600 improve video control functions
     * new method for init player so can reuse@{ */
    private void initPlayerIfNeed() {
        Log.d(TAG, "mPlayer is Create!");
        if (mPlayer == null) {
            mPlayer = new MoviePlayer(mRootView, this, mUri,
                    mPlayList, mSavedInstanceState, !mFinishOnCompletion) {
                @Override
                public void onCompletion() {
                    if (mFinishOnCompletion) {
                        Log.e(TAG, "XX@@finish when completeXX@@");
                        finish();
                    }
                }
            };
        }
        /** SPRD:bug 474614: porting float play @{ */
        mPlayer.setPlayBackMode(currentPlaybackMode);
        mPlayer.setChannel(mVolumesTmp[VOLUME_LEFT],
        mVolumesTmp[VOLUME_RIGHT]);
        /** @} */
        // SPRD: Add for bug531277, check storge permissions also
        if (!GalleryUtils.checkStoragePermissions(this)) {
            return;
        }
        // @}
        /** SPRD:Bug 474641 add reciver MMS&SMS Control @{ */
        VideoCmccUtils.getInstance().initPlayer(mPlayer);
        /** @} */
        /**SPRD:Bug474646 Add Drm feature @{  @{ */
        VideoDrmUtils.getInstance().getFilePathByUri(mUri, this);// SPRD:bug 474646 Add Drm feature
        VideoDrmUtils.getInstance().needToConsume(!mIntent.getBooleanExtra("isConsumed", false));
        /** @ } */
    }

    /** @} */
    /**SPRD：Bug474600 improve video control functions
     * add new methods @{*/
    private void initPlayList(Boolean isStartByGallery) {
        /** SPRD:bug 474614: porting float play @{ */
        if(mPlayList != null && mPlayList.size() != 0){
            mPlayList.clear();
        }
        /** @} */
        /** Bug474615 Playback loop mode @{ */
        if(!isStartByGallery){
            return;
        }
        /** @}*/
        // SPRD: Add for bug531277, checkPermissions before query cursor @{
        if (!GalleryUtils.checkStoragePermissions(this)) {
            return;
        }
        // @}
        String UriID = mUri.toString().substring(
                mUri.toString().lastIndexOf("/") + 1, mUri.toString().length());
        Cursor cursorbucket_id = mContentResolver.query(videoListUri, new String[] {
                    "bucket_id"}, "_id=?", new String[]{UriID}, null);
        String bucket_id = null;
        try {
            if (cursorbucket_id != null) {
                cursorbucket_id.moveToFirst();
                bucket_id = cursorbucket_id.getString(0);
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
            cursor = mContentResolver.query(videoListUri, new String[] {
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
                    mInfo.mAlbum=cursor.getString(0) ;
                    mInfo.mID=cursor.getString(1);
                    mInfo.mPath=cursor.getString(2);
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
    }
    /**@  }*/

    private void setActionBarLogoFromIntent(Intent intent) {
        Bitmap logo = intent.getParcelableExtra(KEY_LOGO_BITMAP);
        if (logo != null) {
            getActionBar().setLogo(
                    new BitmapDrawable(getResources(), logo));
        }
    }

    private void initializeActionBar(Intent intent) {
        mUri = intent.getData();
        final ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            return;
        }
        setActionBarLogoFromIntent(intent);
        /**
         * SPRD:Bug474591 remove actionBar.setDisplayOptions @{
         * @orig
        actionBar.setDisplayOptions(
                ActionBar.DISPLAY_HOME_AS_UP,
                ActionBar.DISPLAY_HOME_AS_UP);
         *@}
         */
        actionBar.addOnMenuVisibilityListener(new OnMenuVisibilityListener() {
            @Override
            public void onMenuVisibilityChanged(boolean isVisible) {
                // TODO Auto-generated method stub
                if(isVisible){
                    MovieActivity.this.mPlayer.hideToShow();
                }
                onPrepareOptionsMenu(mMenu);
            }
        });
        String title = intent.getStringExtra(Intent.EXTRA_TITLE);
        if (title != null) {
            /** SPRD:Bug474591 add new function @} */
            setDisplayOptions(title != null, true, actionBar);
            actionBar.setTitle(title);
        }else if("file".equals(mUri.getScheme())){
            title = mUri.getLastPathSegment();
            setDisplayOptions(title != null, true, actionBar);
            actionBar.setTitle(title);
        /**SRPD:Bug 492389 use the browser to play streaming media,
         * click the left arrow,back to the gallery
         * similar changes were made in old bug 413313
         @{
         */
        } else if ("http".equalsIgnoreCase(mUri.getScheme())
                || "rtsp".equalsIgnoreCase(mUri.getScheme())
                || mUri.toString().contains("android.messaging.datamodel")//SPRD:Bug508566 play video in mms,should not share
                || mUri.toString().contains("/mms/part")) {               //SPRD:Bug508566 play video in mms,should not share
            title = mUri.getLastPathSegment();
            setDisplayOptions(title != null, true, actionBar);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
            /**   @}*/
            /**   @}*/
            actionBar.setTitle(title);
        } else {
            // Displays the filename as title, reading the filename from the
            // interface: {@link android.provider.OpenableColumns#DISPLAY_NAME}.
            AsyncQueryHandler queryHandler =
                    new AsyncQueryHandler(getContentResolver()) {
                @Override
                protected void onQueryComplete(int token, Object cookie,
                        Cursor cursor) {
                    try {
                        if ((cursor != null) && cursor.moveToFirst()) {
                            String displayName = cursor.getString(0);

                            // Just show empty title if other apps don't set
                            // DISPLAY_NAME
                            actionBar.setTitle((displayName == null) ? "" :
                                    displayName);
                        }
                    } finally {
                        Utils.closeSilently(cursor);
                    }
                }
            };
            queryHandler.startQuery(0, null, mUri,
                    new String[] {OpenableColumns.DISPLAY_NAME}, null, null,
                    null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.movie, menu);
        Log.d(TAG, " onCreateOptionsMenu");
        mMenu = menu;
        /** SPRD:Bug474615 Playback loop mode @{ */
        MenuItem playbackMenu = mMenu.findItem(R.id.action_vedio_playback);
        if ("content".equalsIgnoreCase(mUri.getScheme()) || "file".equalsIgnoreCase(mUri.getScheme())) {
            playbackMenu.setEnabled(true);
        } else {
            playbackMenu.setEnabled(false);
        }
        /** @}*/
        /** SPRD:Bug474630 switching voice devices @{ */
        if (mAudioManager.isBluetoothA2dpOn() == true) {
            isheadSetmount = true;
        }
        audioDeviceMenu = mMenu.findItem(R.id.action_vedio_headset);
        if (isheadSetmount) {
            audioDeviceMenu.setTitle(R.string.loud_speaker);
            audioDeviceMenu.setIcon(R.drawable.ic_menu_loud_speaker);
            audioDeviceMenu.setEnabled(true);
//            mPlayer.setMaxVolume(MAX_VOLUME_HEADSET_ON);
            setLoudSpeakerOn();
        } else {
            audioDeviceMenu.setEnabled(false);
        }
        /** @} */
        // Document says EXTRA_STREAM should be a content: Uri
        // So, we only share the video if it's "content:".
        /** SPRD:Bug474632 bookmark setting  @{ */
        boolean showBookmark = false;
        if ("http".equalsIgnoreCase(mUri.getScheme()) || "rtsp".equalsIgnoreCase(mUri.getScheme())) {
            showBookmark = true;
        }
        MenuItem save = menu.findItem(R.id.action_vedio_save_bookmark);
        if (save != null) {
            save.setVisible(showBookmark);
        }
        MenuItem view = menu.findItem(R.id.action_vedio_view_bookmarks);
        if (view != null) {
            view.setVisible(showBookmark);
        }
        /** @} */
        /**SPRD:Bug 474610 imput url for go-to research remove @{
         * @orig
        MenuItem shareItem = menu.findItem(R.id.action_share);
        */
        
        /** wangxing 20160630 remove parameter settings @{ */ 
        if(OptConfig.SUNVOV_CUSTOM_C7301_XLL_FWVGA || OptConfig.SUN_CUSTOM_C7356_XT_HVGA_SUPPORT){
            MenuItem parameterMenu = mMenu.findItem(R.id.action_vedio_parameter);
            parameterMenu.setVisible(false);
        }
        /** @} */
        
        shareItem = menu.findItem(R.id.action_share);
        if ("file".equalsIgnoreCase(mUri.getScheme()) //SPRD:Bug517925 video can't share in the FileExplorer
                || ContentResolver.SCHEME_CONTENT.equals(mUri.getScheme())
                && !mUri.toString().contains("android.messaging.datamodel")
                && !mUri.toString().contains("/mms/part")) {//SPRD:Bug508566 play video in mms,should not share
            /** SPRD:Bug474646 Add Drm feature @{ @{ */     //modify in the old Bug375887 is not the same as this bug
            if (VideoDrmUtils.getInstance().disableShareMenu(shareItem)) {
                return true;
            }
            /** @ } */
            shareItem.setVisible(true);
            ((ShareActionProvider) shareItem.getActionProvider())
                    .setShareIntent(createShareIntent());
        } else {
            shareItem.setVisible(false);
        }
        setFullScreen();
        return true;
    }

    private Intent createShareIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_STREAM, mUri);
        return intent;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

     /*Bug474615 Playback loop mode
      *@orig
      *int id = item.getItemId();
        if (id == android.R.id.home) {
            if (mTreatUpAsBack) {
                finish();
            } else {
                 SPRD: bug 473267 add video entrance @{
                //startActivity(new Intent(this, GalleryActivity.class));
                startActivity(new Intent(this, VideoActivity.class));
                 @}
                 *//**SPRD: bug 473267 add video entrance @{*//*
                //startActivity(new Intent(this, GalleryActivity.class));
                startActivity(new Intent(this, VideoActivity.class));
                *//** @}*//*
                finish();
            }
            return true;
        } else if (id == R.id.action_share) {
            startActivity(Intent.createChooser(createShareIntent(),
                    getString(R.string.share)));
            return true;
        }
        */

        /* SPRD: add new menu */
        /* SPRD: add video Proxy Set menu @{ */
        switch (item.getItemId()) {
            /**SPRD:474610: imput url for go-to research modify by old Bug490923 @{*/
            case R.id.action_vedio_goto: {
                try {
                    Intent startIntent = new Intent();
                    // startIntent.setClassName("com.android.quicksearchbox",
                    // "com.android.quicksearchbox.SearchActivity");
                    startIntent.setAction(SearchManager.INTENT_ACTION_GLOBAL_SEARCH);
                    startActivity(startIntent);
                } catch (ActivityNotFoundException ex) {
                    Log.d(TAG, "onOptionsItemSelected,No search Activity.");
                    Toast.makeText(MovieActivity.this, R.string.no_search_app, Toast.LENGTH_SHORT)
                            .show();
                }
            }
                break;
            /**  @}*/
            /** SPRD:Bug474643 add video Proxy Set menu @{ */
            case R.id.action_vedio_parameter: {
                closeOptionsMenu();
                closeContextMenu();
                Intent intent = new Intent();
                intent.setClass(MovieActivity.this, MovieViewProxySet.class);
                startActivity(intent);

            }
                break;
           /**  @}*/
           /** SPRD:Bug474615 Playback loop mode @{ */
            case R.id.action_vedio_playback: {
                AlertDialog.Builder PlayBackSetDialog = new AlertDialog.Builder(
                        this)
                        .setIcon(R.drawable.ic_playback_setting)
                        .setTitle(R.string.playback_setting)
                        .setSingleChoiceItems(playbackMode, currentPlaybackMode,
                                new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog,
                                            int which) {
                                        currentPlaybackMode = which;
                                        mPlayer.setPlayBackMode(currentPlaybackMode);
                                        dialog.dismiss();
                                    }
                                });
                PlayBackSetDialog.show();
            }
                break;
           /**  @}*/
            /**SPRD:Bug474621 screen size setting @{*/
            case R.id.action_vedio_original_size: {
                isFullScreen = !isFullScreen;
                MenuItem menuItem = mMenu.findItem(R.id.action_vedio_original_size);
                if (isFullScreen) {
                    menuItem.setTitle(R.string.movie_view_original_size);
                    menuItem.setIcon(R.drawable.ic_menu_movie_original);
                } else {
                    menuItem.setTitle(R.string.movie_view_full_screen);
                    menuItem.setIcon(R.drawable.ic_menu_movie_full_window);
                }
                mPlayer.resize(isFullScreen);
            }
                break;
            /**@}*/
           /** SPRD:Bug474618 Channel volume settings @{ */
            case R.id.action_vedio_channel_setting:
                showChannelSettingDialog();
                break;
           /**  @}*/
            /** SPRD:Bug474630 switching voice devices @{ */
            case R.id.action_vedio_headset: {
                if (isLoudSpeakerOn) {
//                    mPlayer.setMaxVolume(MAX_VOLUME_HEADSET_ON);
                    mAudioManager.setSpeakerMediaOn(false);
                    mPlayer.updateVolume(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                    isLoudSpeakerOn = false;
                    optionsSelectedLoudSpeakerOn= false;
                } else {
//                    mPlayer.setMaxVolume(MAX_VOLUME_HEADSET_OFF);
                    mAudioManager.setSpeakerMediaOn(true);
                    mPlayer.updateVolume(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                    isLoudSpeakerOn = true;
                    optionsSelectedLoudSpeakerOn= true;
                }
                audioDeviceMenu.setEnabled(true);
            }
                break;
            /**  @}*/
            case android.R.id.home: {
                if (mTreatUpAsBack) {
                    finish();
                } else {
                     /**SPRD: bug 473267 add video entrance @{*/
                    //startActivity(new Intent(this, GalleryActivity.class));
                    startActivity(new Intent(this, VideoActivity.class));
                    /** @}*/
                    finish();
                }
            }
                break;
            case R.id.action_share: {
                startActivity(Intent.createChooser(createShareIntent(),
                        getString(R.string.share)));
            }
                break;
            /** SPRD:Bug474632 bookmark setting  @{ */
            case R.id.action_vedio_view_bookmarks: {
                closeOptionsMenu();
                closeContextMenu();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent();
                        intent.setClass(MovieActivity.this, MovieBookmarks.class);
                        startActivity(intent);
                    }
                }, 100);
            }
                break;
            case R.id.action_vedio_save_bookmark:
                saveToBookmark();
                break;
            /** @}*/
            default:
                break;
        }
        /* @} */
        return false;
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart");
        /** SPRD: bug#488330 keep same audiofocustype with videoview from framework.
         * ori-code:
        ((AudioManager) getSystemService(AUDIO_SERVICE))
                .requestAudioFocus(null, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
         *@{
         */
        ((AudioManager) getSystemService(AUDIO_SERVICE))
                .requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);//SPRD:Bug528834 modify
        //SPRD:Bug521702  modify by old bug497432
        /** @} */
        /** SPRD:Bug474630 switching voice devices  @{ */
        /** SPRD: register receiver @{ */
        ComponentName componentName = new ComponentName(this.getPackageName(),MovieActivity.class.getName());
        mAudioManager.registerMediaButtonEventReceiver(componentName);
        /** @} */
        /** @} */
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        /**
         * SPRD:Bug474600 improve video control functions
         * add to set the Video View invisible when the Activity is stop
         * @{
         */
        Log.d(TAG, "onStop()");
        /**SPRD:Bug534454 modify by old Bug517683 @{*/
   //     if (mPlayer != null) {
   //        mPlayer.setVideoViewInvisible();
            mControlResumed = false;//SPRD:Bug528547 add
   //     }
        /**@}*/
        /**@}*/
        ((AudioManager) getSystemService(AUDIO_SERVICE))
                .abandonAudioFocus(afChangeListener);//SPRD:Bug521702  modify by old bug497432
        ((AudioManager) getSystemService(AUDIO_SERVICE))
               .abandonAudioFocus(null);//SPRD:Bug528834 modify by old Bug524033
        /** SPRD:Bug474630 switching voice devices  @{ */
        /** SPRD: unregister receiver @{ */
        ComponentName componentName = new ComponentName(this.getPackageName(),MovieActivity.class.getName());
        mAudioManager.unregisterMediaButtonEventReceiver(componentName);
        if (mAudioManager.isWiredHeadsetOn() == true && isLoudSpeakerOn) {
            Log.d(TAG, "isWiredHeadsetOn");
            mAudioManager.setSpeakerMediaOn(false);
        }
        /** @} */
        /** @} */
        super.onStop();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        mIsOnPause = true;//SPRD:Bug534454 modify by old Bug517683
        /** SPRD:Bug474630 switching voice devices  @{ */
        unregisterReceiver(mHeadsetReceiver);
        /** @} */
        /** SPRD:Bug 474639 add phone call reaction @{ */
        pauseVideo();
        if (mCallingDialog != null) {
            mCallingDialog.dismiss();
        }
        mShowResumeDialog = false;//SPRD: bug 526178 just show resume dialog once.
        /** @} */
        /** SPRD:Bug 474641 add reciver MMS&SMS Control @{ */
        VideoCmccUtils.getInstance().releaseMessagingUtils();
        /** @} */
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        /**SPRD:Bug542760 the MovieActivity and the floatwindow play together@{*/
        if (isServiceRunning(this, FloatPlayerService.class.getName())) {
            Intent intent = new Intent();
            intent.setAction("com.android.gallery3d.app.SHUT_FLOATWINDOW");
            sendBroadcast(intent);
        }
        /**@}*/
        /** SPRD:Bug474600 to initialize the Video player when mPlayer is null@{ */
        Log.d(TAG, "Activity is onResume!");
        mIsOnPause = false;//SPRD:Bug534454 modify by old Bug517683
        /** SPRD:Bug 474639 add phone call reaction @{ */
        mIsPaused = false;
        /** SPRD:Bug474630 switching voice devices  @{ */
        if (interruptState == true && isheadSetmount) {
            mAudioManager.setSpeakerMediaOn(true);
            mPlayer.updateVolume(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
            isLoudSpeakerOn = true;
        }
        IntentFilter headsetFilter = new IntentFilter();
        headsetFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        headsetFilter
                .addAction("android.bluetooth.headset.action.STATE_CHANGED");
        headsetFilter.addAction("android.media.AUDIO_BECOMING_NOISY");
        registerReceiver(mHeadsetReceiver, headsetFilter);
        /** @} */
        if (!mControlResumed) {
            Log.d(TAG, "onResume mPlayer.onResume()");
            if (isCallingState()) {
                /**SPRD:Bug534454 modify by old Bug517683 @{*/
                if (mPlayer != null) {
                    mPlayer.setVideoViewInvisible();
                }
                /**@}*/
                showCallingMsg();
            } else if(!isKeyguardOn()){
                /** SPRD:Bug474600 to initialize the Video player when mPlayer is null@{ */
                if(mPlayer == null){
                    initPlayerIfNeed();
                    Log.d(TAG, "initPlayerIfNeed() is Called!");
                }
                /** @} */
                mPlayer.onResume();
                mControlResumed = true;
            }
        }
        /** @} */
        /** SPRD:Bug474615 Playback loop mode
         * change by old bug 270824 @{ */
        String scheme = mUri.getScheme();
        Boolean isStartByGallery = mIntent.getBooleanExtra(FLAG_GALLERY, false);
        Log.d(TAG, "scheme " + scheme);
        if (!isStartByGallery||"http".equalsIgnoreCase(scheme) || "rtsp".equalsIgnoreCase(scheme)
                || mUri.toString().contains("android.messaging.datamodel")
                || mUri.toString().contains("/mms/part")
                || "https".equalsIgnoreCase(scheme)
                || "file".equalsIgnoreCase(scheme)) {
            playbackMode = new String[] { getString(R.string.none_playback),
                    getString(R.string.single_playback) };
        } else {
            playbackMode = new String[] { getString(R.string.none_playback),
                    getString(R.string.single_playback),
                    getString(R.string.all_playback) };
        }
        /** SPRD:Bug 474641 add reciver MMS&SMS Control @{ */
        VideoCmccUtils.getInstance().initMessagingUtils(this);
        /** @} */
        /* SPRD:fix bug 526519,Recent tasks to return to the video, share video error @{ */
        if(mMenu != null && mUri != null){
            sendShareUri(mMenu, mUri);
        }
        /* @} */
        super.onResume();
        /**Bug505902 Play any video files and press home key to exit, modify by old Bug472834 @{*/
        overridePendingTransition(0,0);
        Log.d(TAG, "onResume, cancel animal effect. ");
        /** @}*/
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mPlayer!= null) {
            mPlayer.onSaveInstanceState(outState);
        }
    }
    @Override
    public void onRestoreInstanceState(Bundle outState) {
        super.onRestoreInstanceState(outState);
        if (mPlayer != null) {
            mPlayer.onRestoreInstanceState(outState);
        }
    }
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestory");
        /** SPRD:bug 474614: porting float play @{ */
        // old bug info:339523 begin
        //wait for phone call feature merge
        /*
         * SPRD:Bug474639 remove for Bug540980@{
        if(mIsMute){
            Log.d(TAG , "onDestroys() mIsMute is true");
            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        }
        *@}
        */
        // bug 339523 end
        /** SPRD:Bug 474639 add phone call reaction @{ */
        if(mTelephonyManager != null){
            mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
        }
        /** @} */
        /** SPRD:Bug474600 improve video control functions
         * add@{ */
        if(mPlayer!=null){
          mPlayer.onDestroy();
        }
        mPlayList = null;
        /** @} */
        unRegisterShutReceiver();//SPRD:Bug537666 add
        unRegisterExternalMountedReceiver();//SPRD:Bug538278 add
        unregisterHallOn();//Bug49967
        /** SPRD:Bug 474641 add reciver MMS&SMS Control @{ */
        VideoCmccUtils.getInstance().destoryMessagingDialog();
        /** @} */
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(mPlayer != null){
            return mPlayer.onKeyDown(keyCode, event)
                    || super.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(mPlayer != null){
            return mPlayer.onKeyUp(keyCode, event)
                    || super.onKeyUp(keyCode, event);
        }
        return super.onKeyUp(keyCode, event);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PermissionsActivity.RESULT_FOR_MOVIE) {
            if (resultCode == RESULT_OK) {
                // do some like new movieplayer things
                Bundle savedInstanceState = null;
                if (data != null) {
                    savedInstanceState = data.getExtras();
                }
                /* SPRD:modify for bug538264
                 * play video attachment in the mms when the gallery has no
                 * access to the storage ,the controller cant be seen @{
                 mPlayer = new MoviePlayer(mRootView, this, mUri,mPlayList, savedInstanceState,
                        !mFinishOnCompletion) {
                    @Override
                    public void onCompletion() {
                        if (mFinishOnCompletion) {
                            finish();
                        }
                    }
                };
                */
                initPlayerIfNeed(mIntent);
                /* @}*/
                /** SPRD:Bug474646 Add Drm feature @{ @{ */
                VideoDrmUtils.getInstance().getFilePathByUri(mUri, this);// SPRD:bug 474646 Add Drm feature
                /** @ } */
                mPlayer.onResume();
            } else {
                finish();
            }
        }
    }

    /** SPRD:Bug474632 bookmark setting  @{ */
    private void saveToBookmark() {
         if ("http".equals(mUri.getScheme())
                 || "rtsp".equals(mUri.getScheme())
                 || "https".equals(mUri.getScheme())) {
             new AsyncTask<Void, Void, Boolean>() {

                 @Override
                 protected Boolean doInBackground(Void... params) {
                     // TODO Auto-generated method stub
                     ContentResolver resolver = MovieActivity.this.getContentResolver();
                     Cursor cursor = resolver.query(MovieViewContentProvider.BOOKMARK_CONTENT_URI,
                             null, MovieViewContentProvider.BOOKMARK_URL + " =? ", new String[] {
                                 mUri.toString()
                             }, null);
                     if (cursor != null && cursor.getCount() > 0) {
                         Log.d(TAG, "already exist in bookmarks: " + mUri);
                         cursor.close();
                         cursor = null;
                         return false;

                     } else {
                         if (cursor != null) {
                             cursor.close();
                             cursor = null;
                         }
                         ContentValues values = new ContentValues();
                         String title = getResources().getString(R.string.default_bookmark_title);
                         values.put(MovieViewContentProvider.BOOKMARK_TITLE, title);
                         values.put(MovieViewContentProvider.BOOKMARK_URL, mUri.toString());
                         resolver.insert(MovieViewContentProvider.BOOKMARK_CONTENT_URI, values);
                         Log.d(TAG, "insert completed: " + mUri.toString());
                         return true;
                     }
                 }

                 protected void onPostExecute(Boolean success) {
                     Log.d(TAG, "onPostExecute: " + success);
                     if (success) {
                         Toast.makeText(MovieActivity.this, R.string.bookmark_added,
                                 Toast.LENGTH_SHORT).show();
                     } else {
                         Toast.makeText(MovieActivity.this, R.string.bookmark_exist,
                                 Toast.LENGTH_SHORT).show();
                     }
                 }
             }.execute();
         }
    }
  /** @ } */

    /** SPRD:Bug474591 Add this method to instead setDisplayOptions(int options) @{ */
    private void setDisplayOptions(boolean displayHomeAsUp, boolean showTitle, ActionBar actionBar) {
        if (actionBar != null) {
            int options = (displayHomeAsUp ? ActionBar.DISPLAY_HOME_AS_UP : 0) |
                    (showTitle ? ActionBar.DISPLAY_SHOW_TITLE : 0);
            actionBar.setDisplayOptions(options,
                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setHomeButtonEnabled(displayHomeAsUp);
        }
    }
    /**@}*/

    /** SPRD:Bug474600 improve video control functions
     * Add this class to record video information @{ */
    /**
     * SPRD:Description of the MovieInfo class @{ mAlbum:name of the target video.such
     * as:UID20150410_1613376.mp4 mID：ID of the the target video give a Uri like this:
     * content://media/external/video/media/1329 1329 is the ID we want to get mPath：path of the
     * target video ,specifically described as follows:
     * /storage/emulate10/camera/UID20150410_1613376.mp4
     */
    class MovieInfo {
        String mAlbum;
        String mID;
        String mPath;
    }
    /**@}*/

   /** SPRD:bug 474614: porting float play @{ */
   // old bug info:339523 begin
   public static boolean isServiceRunning(Context mContext, String className) {
       boolean isRunning = false;
       ActivityManager activityManager = (ActivityManager) mContext
               .getSystemService(Context.ACTIVITY_SERVICE);
       List<ActivityManager.RunningServiceInfo> serviceList = activityManager
               .getRunningServices(200);
       if (!(serviceList.size() > 0)) {
           return false;
       }
       for (int i = 0; i < serviceList.size(); i++) {
           if (serviceList.get(i).service.getClassName().equals(className) == true) {
               isRunning = true;
               break;
           }
       }
       return isRunning;
   }
   // bug 339523 end
   public boolean getIsFullScreen() {
       return isFullScreen;
   }
   public boolean getIsLoudSpeakerOn() {
       return isLoudSpeakerOn;
   }

   public boolean getOptionsSelectedLoudSpeakerOn() {
       return optionsSelectedLoudSpeakerOn;
   }

   public void setLoudSpeakerOn() {
       optionsSelectedLoudSpeakerOn = isLoudSpeakerOn = mIntent.getBooleanExtra(
               "optionsSelectedLoudSpeakerOn", false);
       if (optionsSelectedLoudSpeakerOn) {
           mAudioManager.setSpeakerMediaOn(true);
           mPlayer.updateVolume(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
       } else {
           mAudioManager.setSpeakerMediaOn(false);
           mPlayer.updateVolume(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
       }
   }

    public void setFullScreen() {
        Log.d(TAG, "setFullScreen");
        MenuItem menuItem = mMenu.findItem(R.id.action_vedio_original_size);
        if (isFullScreen) {
            menuItem.setTitle(R.string.movie_view_original_size);
            menuItem.setIcon(R.drawable.ic_menu_movie_original);
        } else {
            menuItem.setTitle(R.string.movie_view_full_screen);
            menuItem.setIcon(R.drawable.ic_menu_movie_full_window);
        }
    }
   /** @} */
    /**
     * SPRD:Bug474605 video sharing add by old Bug 264046 @{
     */
    private Intent createShareIntent(Uri url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_STREAM, url);
        return intent;
    }

    private void sendShareUri(Menu menu, Uri uri) {
        String scheme = uri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "rtsp".equalsIgnoreCase(scheme)
                || mUri.toString().contains("android.messaging.datamodel")
                || mUri.toString().contains("/mms/part")
                || "https".equalsIgnoreCase(scheme)) {
            return;
        }
        MenuItem shareItem = menu.findItem(R.id.action_share);
        ((ShareActionProvider) shareItem.getActionProvider())
                .setShareIntent(createShareIntent(uri));
    }

    protected void onVideoUriChange(Uri uri) {
        if (uri == null || mMenu == null) {
            Log.d(TAG, "shared uri is null");
        } else {
            Log.d(TAG, "shared uri is " + uri.toString());
            mUri = uri;
            MenuItem shareItem = mMenu.findItem(R.id.action_share);
            sendShareUri(mMenu, uri);
        }
    }
    /** @} */
    /* SPRD: add for mutex between float window and camera @{ */
    public boolean isFloatWindowDisabled() {
        return mIsFloatWindowDisabled;
    }

    public static MovieActivity getInstance() {
        return mMovieActivity;
    }
    /* @} */

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mPlayer != null) {
            mPlayer.resize(isFullScreen);
            mPlayer.onConfigurationChanged(newConfig);
        }
    }
    /** SPRD: Bug474621 screen size setting@{ */
    public void removeActionVedioOriginalsize(){
        if(mMenu != null){
            MenuItem menuItem = mMenu.findItem(R.id.action_vedio_original_size);
            menuItem.setEnabled(false);
        }
    }

    public void showActionVedioOriginalsize(){
        if(mMenu != null){
            MenuItem menuItem = mMenu.findItem(R.id.action_vedio_original_size);
            menuItem.setEnabled(true);
        }
    }
    /**@}*/
    /** SPRD: Bug474618 Channel volume settings@{ */
    public void showChannelSettingDialog() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.movie_channel_setting,
                (ViewGroup) findViewById(R.id.channel_setting_root));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.channel_setting_title);
        builder.setPositiveButton(R.string.channel_setting_ok,
                new ButtonClickListener(AlertDialog.BUTTON_POSITIVE));
        builder.setNegativeButton(R.string.channel_setting_cancel,
                new ButtonClickListener(AlertDialog.BUTTON_NEGATIVE));
        builder.setView(layout);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(true);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                mVolumesTmp[VOLUME_LEFT] = mVolumes[VOLUME_LEFT];
                mVolumesTmp[VOLUME_RIGHT] = mVolumes[VOLUME_RIGHT];
                mPlayer.setChannelVolume(mVolumes[VOLUME_LEFT],
                        mVolumes[VOLUME_RIGHT]);
            }

        });

        SeekBar channelSeekbar = (SeekBar) layout
                .findViewById(R.id.channel_seekbar);
        channelSeekbar.setOnSeekBarChangeListener(new VolumeChangeListener());

        int max = channelSeekbar.getMax();
        if (mVolumes[VOLUME_LEFT] == 1.0f && mVolumes[VOLUME_RIGHT] == 1.0f) {
            channelSeekbar.setProgress(max / 2);
        } else if (mVolumes[VOLUME_LEFT] == 1.0f) {
            channelSeekbar
                    .setProgress((int) (mVolumes[VOLUME_RIGHT] * (max / 2)));
        } else {
            channelSeekbar
                    .setProgress((int) (max - (max / 2 * mVolumes[VOLUME_LEFT])));
        }

        dialog.show();
    }
    class ButtonClickListener implements DialogInterface.OnClickListener {
        private int mType;

        public ButtonClickListener(int type) {
            mType = type;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (mType) {
                case AlertDialog.BUTTON_POSITIVE:
                    mVolumes[VOLUME_LEFT] = mVolumesTmp[VOLUME_LEFT];
                    mVolumes[VOLUME_RIGHT] = mVolumesTmp[VOLUME_RIGHT];
                    break;
                case AlertDialog.BUTTON_NEGATIVE:
                    mVolumesTmp[VOLUME_LEFT] = mVolumes[VOLUME_LEFT];
                    mVolumesTmp[VOLUME_RIGHT] = mVolumes[VOLUME_RIGHT];
                    Log.d(TAG, "onClick mVolumes[VOLUME_LEFT] "+mVolumes[VOLUME_LEFT]);
                    Log.d(TAG, "onClick mVolumes[VOLUME_RIGHT] "+mVolumes[VOLUME_RIGHT]);
                    mPlayer.setChannelVolume(mVolumes[VOLUME_LEFT],
                            mVolumes[VOLUME_RIGHT]);
                    break;
                default:
                    break;
                }

        }
    }
    class VolumeChangeListener implements OnSeekBarChangeListener {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                boolean fromUser) {

            int max = seekBar.getMax();

            if (progress > (seekBar.getMax() / 2)) {
                // set right channel
                mVolumesTmp[VOLUME_LEFT]  = (max - progress) / (float) (max / 2);
                mVolumesTmp[VOLUME_RIGHT] = 1.0f;

            } else if (progress < (seekBar.getMax() / 2)) {
                // set left channel
                mVolumesTmp[VOLUME_LEFT]  = 1.0f;
                mVolumesTmp[VOLUME_RIGHT] = progress / (float) (max / 2);
            } else {
                mVolumesTmp[VOLUME_LEFT]  = 1.0f;
                mVolumesTmp[VOLUME_RIGHT] = 1.0f;
            }
            Log.d(TAG, "onProgressChanged mVolumesTmp[VOLUME_LEFT] "+mVolumesTmp[VOLUME_LEFT]);
            Log.d(TAG, "onProgressChanged mVolumesTmp[VOLUME_RIGHT] "+mVolumesTmp[VOLUME_RIGHT]);
            mPlayer.setChannelVolume(mVolumesTmp[VOLUME_LEFT], mVolumesTmp[VOLUME_RIGHT]);
        }
    }
    /**@}*/
    /** SPRD:Bug 474639 add phone call reaction @{ */
    //SPRD: Bug 281076 Listening the call state , if ringing mute the music, idle unmute the music
    PhoneStateListener mPhoneListener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                mPhoneState = true;
                if(mPlayer != null && mPlayer.isPlaying()){
                    pauseVideo();
                /*
                 * SPRD:Bug474639 remove for Bug540980
                    if(!mIsMute){
                        mPreVolume = mPlayer.mCurVolume;
                        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                        mIsMute = true;
                    }
                 */
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                /** SPRD:Bug534454 modify by old Bug517683 */
                if (!mIsOnPause) {
                    initPlayerIfNeed();
                    resumeIfNeed();
                }
                /**@}*/
                mPhoneState = false;
                Log.e(TAG , "TelephonyManager.CALL_STATE_OFFHOOK");
                /*SRPD:Bug474639 remove for Bug540980
                mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                */
                /*SPRD:Bug474639 modify:mIsMute && isheadSetmount is changed to isheadSetmount */
                if(isheadSetmount){
                    mAudioManager.setSpeakerMediaOn(optionsSelectedLoudSpeakerOn);
                    isLoudSpeakerOn = optionsSelectedLoudSpeakerOn;
                    /*SRPD:Bug474639 remove for Bug540980
                    mPlayer.updateVolume(mPreVolume);
                    */
                }
                /*SRPD:Bug474639 remove for Bug540980
                mIsMute = false;
                */
                isVolumeCorrect = false;
                break;
            default:
                break;
            }
       };
   };
   //SPRD: End

   /** old bug info: bug 261460 @{*/
   public boolean isPhoneCallActivityTop(){
          TelephonyManager mTelePhoneManager = (TelephonyManager) getSystemService("phone");
          int callState = mTelePhoneManager.getCallState();
          Log.d(TAG,"callState="+callState);
          if((TelephonyManager.CALL_STATE_IDLE!=callState)){
             return true;
          }else{
             return false;
          }
      }
   /** @} */
   private void pauseVideo(){
       mShowResumeDialog = false;
       mIsPaused = true;
       isFirst = false;
       /** SPRD: send broadcast to alarms @{ */
       Intent intent = new Intent("com.android.gallery3d.app.MovieActivity.onPause");
       if(mPlayer.misLiveStreamUri){
           intent.putExtra("isLiveStream", 1);
       }else{
           intent.putExtra("isLiveStream", 0);
       }
       this.sendBroadcast(intent);
       /** @} */
       if (mControlResumed) {
            if (mPlayer != null) {
                mPlayer.onPause();
            }
           mControlResumed = false;
           /** SPRD:Bug474630 switching voice devices @{ */
           if (isheadSetmount && isLoudSpeakerOn) {
               interruptState = true;
           } else {
               interruptState = false;
           }
           mAudioManager.setSpeakerMediaOn(false);
           isLoudSpeakerOn = false;
           /**  @} */
       }else if(mPhoneState){
            if (mPlayer != null) {
                mPlayer.executeSuspend();
            }
       }
   }

   private BroadcastReceiver mHeadsetReceiver = new BroadcastReceiver() {
       @Override
       public void onReceive(Context context, Intent intent) {
           onReceiveHeadsetReceiver(intent);
       }
   };
   private void onReceiveHeadsetReceiver(Intent intent) {
       String action = intent.getAction();
       Log.d(TAG, "onReceiveHeadsetBroadcast : action = " + action);
       if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
           int headsetState = intent.getIntExtra("state", -1);
           if (headsetState == 1) {
               if(mPlayer != null && !isheadSetmount){
                   /** SPRD:Bug474630 switching voice devices @{ */
                   mAudioManager.setSpeakerMediaOn(false);
                   isLoudSpeakerOn = false;
                   /**  @} */
                   try {
                       Thread.sleep(300);
                   } catch (InterruptedException e) {
                       e.printStackTrace();
                   }
                   if(mPlayer != null){
                       mPlayer.updateVolume(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                   }
               }
               isheadSetmount = true;
           } else {
               try {
                   Thread.sleep(300);
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
               if(isCallingState()){
                   return;
               }
               if(mPlayer != null){
                   mPlayer.updateVolume(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
               }
               isheadSetmount = false;
           }
       } else if (action
               .equals("android.bluetooth.headset.action.STATE_CHANGED")) {
           int bluetoothHeadsetState = intent.getIntExtra(
                   "android.bluetooth.headset.extra.STATE", -1);
           if (bluetoothHeadsetState == 2) {
               isheadSetmount = true;
           }
       } else if (action.equals("android.media.AUDIO_BECOMING_NOISY")) {
           isLoudSpeakerOn = false;
           isheadSetmount = false;
       }
       if (mMenu != null) {
           mMenu.close();
       }
   }

   private boolean resumeIfNeed() {
       Log.d(TAG, "mFocused  is "+mFocused);
       Log.d(TAG, "mControlResumed is "+mControlResumed);
       if (mFocused && !mControlResumed) {
           sendResumeMsg();
       }
       return true;
   }

   private void sendResumeMsg() {
       Message handldermsg = mHandler.obtainMessage();
       handldermsg.what = MOVIEVIEW_ON_RESUME;
       mHandler.sendMessage(handldermsg);
   }
   public boolean isCallingState() {
          if(!GalleryUtils.checkReadPhonePermissions(this)){
             return false;
          }
           boolean CallState = false;
            List<SubscriptionInfo> subInfoRecords = mSubscriptionManager
                    .getActiveSubscriptionInfoList();
            if (subInfoRecords != null) {
                for (int i = 0; i < subInfoRecords.size(); i++) {
                    SubscriptionInfo sir = subInfoRecords.get(i);
                    CallState = mTelephonyManager.getCallState(sir.getSubscriptionId()) != TelephonyManager.CALL_STATE_IDLE;
                    if (CallState == true) {
                        break;
                    }
                }
            }
            Log.e("TAG", "isCallingState()" + CallState);
            return CallState;
   }

    private void showCallingMsg() {
        if (mCallingDialog == null) {
            mCallingDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.movie_view_calling_title)
                    .setMessage(R.string.movie_view_calling_msg)
                    .setPositiveButton(R.string.movie_view_calling_try,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    dialog.dismiss();
                                    if (isCallingState()) {
                                        Message msg = new Message();
                                        msg.what = SHOW_CALLING_DIALOG;
                                        mHandler.sendMessage(msg);
                                    } else {
                                        Log.d(TAG, "dismmiss()");
                                        initPlayerIfNeed();
                                        resumeIfNeed();
                                    }
                                }
                            })
                    .setNegativeButton(R.string.movie_view_calling_cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    dialog.cancel();
                                    finish();
                                }
                            }).create();
        }

        mCallingDialog.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                // TODO Auto-generated method stub
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (mCallingDialog.isShowing()) {
                        mCallingDialog.dismiss();
                        finish();
                    }
                }
                return false;
            }
        });
        try {
            mCallingDialog.show();
            mCallingDialog.setCanceledOnTouchOutside(false);
        } catch (Exception e) {
        }
    }

   //SPRD: if returns true,ignore resuming the videoPlayer
    /**SPRD:Bug528547 After the lock screen, there will still be voices*/
    private boolean isKeyguardOn(){
    KeyguardManager keyguardManager = (KeyguardManager)getSystemService(Service.KEYGUARD_SERVICE);
    if(keyguardManager.isKeyguardLocked()){
          return true;
      }else{
        return false;
      }
   }
    /**@}*/
   //end
   /** SPRD: add method @{ */
   /** SPRD:Bug521702  the music player and the video player can play at the same time @{ */
   @Override
   public void onWindowFocusChanged(boolean hasFocus) {
       mFocused = hasFocus;
       Log.d(TAG, "onWindowFocusChanged "+mFocused);
       if (mFocused) {
           new Thread(new Runnable() {
               @Override
               public void run() {
                   // TODO Auto-generated method stub
                   boolean success = false;
                   int tryCount = 10;
                   for (int i = 0; i < tryCount; i++) {
                       success = mAudioManager.requestAudioFocus(afChangeListener,
                               AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) != AudioManager.AUDIOFOCUS_REQUEST_FAILED;//try it again
                       if (!success && i < tryCount - 1) {
                           try {
                               Thread.sleep(100);
                           } catch (InterruptedException e) {
                               e.printStackTrace();
                           }
                           continue;
                       }
                       break;
                   }
               }
           }).start();
       }
       /**modify by old Bug518657 @{*/
        if (isCallingState()) {
            mIsPaused = false;
        }
       /**@}*/
       /**SPRD:Bug528547 After the lock screen, there will still be voices*/
       if (mFocused && !mControlResumed && !mIsPaused) {
           Log.d(TAG, "onWindowFocusChanged mPlayer.onResume()");
           if (isCallingState()) {
               showCallingMsg();
           } else if(!isKeyguardOn()) {
               if(mPlayer == null){
                   initPlayerIfNeed();
               }
               mPlayer.onResume();
               mControlResumed = true;
           }
       }
       /**@}*/
       super.onWindowFocusChanged(hasFocus);
   }
   /** @ } */
   private class MovePlayerHandler extends Handler {
       @Override
       public void handleMessage(Message msg) {
           switch (msg.what) {
               case SHOW_CALLING_DIALOG:
                   showCallingMsg();
                   break;
               case MOVIEVIEW_ON_RESUME:
                   Log.d(TAG, "MOVIEVIEW_ON_RESUME");
                   /**SPRD:Bug534454 modify by old Bug517683*/
                    if (!mIsOnPause) {
                        mPlayer.onResume();
                        mControlResumed = true;
                    }
                   /**@}*/
                   break;
               default:
                   break;
           }
       }
   }
   /** @} */
   /** SPRD: Bug474630 switching voice devices  @{ */
   @Override
   public boolean onPrepareOptionsMenu(Menu menu) {
       // TODO Auto-generated method stub
       if (mAudioManager.isBluetoothA2dpOn() == true) {
           isheadSetmount = true;
       }
       if (isheadSetmount) {
           if (!isLoudSpeakerOn) {
               audioDeviceMenu.setTitle(R.string.loud_speaker);
               audioDeviceMenu.setIcon(R.drawable.ic_menu_loud_speaker);
           } else {
               audioDeviceMenu.setTitle(R.string.headset);
               audioDeviceMenu.setIcon(R.drawable.ic_menu_headset);
           }
           audioDeviceMenu.setEnabled(true);
       } else {
           audioDeviceMenu.setTitle(R.string.headset);
           audioDeviceMenu.setIcon(R.drawable.ic_menu_headset);
           audioDeviceMenu.setEnabled(false);
       }
       return super.onPrepareOptionsMenu(menu);
   }
   /** @} */
   /**SPRD:Bug538278 when you unplug the SD card, a error dialog always shows while the video player is in the allplayback mode*/
    public void registerExternalMountedReceiver() {
        if(mExternalMountedReceiver == null){
            mExternalMountedReceiver= new ExternalMountedReceiver();
        }
        IntentFilter intentFilter = new IntentFilter("android.intent.action.MEDIA_UNMOUNTED");
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SHARED);
        /** modify by old Bug487272 @{ */
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        /**@}*/
        intentFilter.setPriority(1000);
        intentFilter.addDataScheme("file");
        registerReceiver(mExternalMountedReceiver, intentFilter);
    }

    public void unRegisterExternalMountedReceiver() {
        if (mExternalMountedReceiver != null) {
            unregisterReceiver(mExternalMountedReceiver);
            mExternalMountedReceiver=null;
        }
    }
    private class ExternalMountedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, action);
            String path = getFilePathFromUri(mUri);
            String sdPath = Environment.getExternalStoragePath().toString();
            if (path == null || path.startsWith(sdPath)) {
                if (mPlayer != null) {
                    mPlayer.disableMediaPlayer();
                }
            }
            /**@}*/
        }
    };
    public String getFilePathFromUri(Uri uri) {
        /** modify by old Bug480381 @{ */
        if (uri.getScheme().toString().compareTo("file") == 0) {
            String path = uri.toString().replace("file://", "");
            return path;
        } else {
            /**@}*/
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(uri, null, null, null, null);
            // Log.d("info", ""+cursor.toString()+"---"+cursor.getCount()+"行");
            // int column_index=cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            if (cursor != null) {// SPRD:modify by old bug510904
                if (cursor.getCount() == 0) {
                    cursor.close();
                    return null;
                } else {

                    cursor.moveToFirst();
                    String path = cursor.getString(1);
                    cursor.close();
                    return path;
                }
            } else {
                return "uselessUri";
            }
        }
    }
    /** @} */
    /** SPRD:Bug537666 Destory the MovieActivity,when the phone shut down @{ */
    public void registerShutReceiver() {
        if(mShutReceiver == null){
            mShutReceiver=new ShutBroadcastReceiver();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SHUTDOWN);
        registerReceiver(mShutReceiver, intentFilter);
    }
    public void unRegisterShutReceiver() {
        if (mShutReceiver != null) {
            unregisterReceiver(mShutReceiver);
            mShutReceiver = null;
        }
    }
    private class ShutBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            Log.d(TAG, intent.getAction());
            if (mPlayer != null) {
                mPlayer.disableMediaPlayer();
            }
        }
    }
    /** @} */
    //Kalyy,Bug49967,begin
    private class HallOnReceiver extends BroadcastReceiver{//Bug 50066
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Hall onReceive(" + intent.getAction() + ") mControlResumed=" + mControlResumed);
            
            String str = intent.getAction();
            if("com.wx.hall".equals(str)){  
                int state = intent.getIntExtra("state", 0);
                if (state == 1){
                    // Only stop video.
                    if (mControlResumed) {
                        mPlayer.disableMediaPlayer();
                        mControlResumed = false;
                    }
                }    
            }
        }
    }

    private void registerHallOn() {
        if(mHallOnReceiver == null){
            mHallOnReceiver = new HallOnReceiver();
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.wx.hall");
        registerReceiver(mHallOnReceiver, filter);
    }

    private void unregisterHallOn() {
        if (mHallOnReceiver != null) {
            unregisterReceiver(mHallOnReceiver);
        }
    }
    //Kalyy,Bug49967,end
}
