/*
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

package com.android.music;

import com.android.music.MusicUtils.ServiceToken;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.audiofx.AudioEffect;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Layout;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

import android.app.SearchableInfo;
import android.widget.SearchView;
import android.graphics.Color;

import com.sprd.music.lrc.LRC;
import com.sprd.music.lrc.LRCParser;
import com.sprd.music.lrc.LyricConstants;
import com.sprd.music.lrc.LyricDataBean;
import com.sprd.music.lrc.LyricListView;
import com.sprd.music.lrc.StringConstant;
import com.sprd.music.utils.SPRDMusicUtils;
/* SPRD bug fix 508982@{ */
import com.sprd.music.utils.SPRDSwitchDetector;
import com.sprd.music.utils.SPRDShakeDetector;
import com.sprd.music.utils.SPRDSwitchDetector.OnSwitchListener;
import com.sprd.music.utils.SPRDShakeDetector.OnShakeListener;
/* @} */

import com.sprd.music.drm.*;

import java.io.File;
import java.util.List;

public class MediaPlaybackActivity extends Activity implements MusicUtils.Defs,
    View.OnTouchListener, View.OnLongClickListener, LRC.PositionProvider
{
    private static final String LOGTAG = "MediaPlaybackActivity";
    private static final int USE_AS_RINGTONE = CHILD_MENU_BASE;
    private static final String LRC_STATE = "com.android.music.LRC_STATE";
    //SPRD bugfix509392
    private static final String LRC_VISIBILITY = "com.android.music.LRC_VISIBILITY";
    private boolean mSeeking = false;
    private boolean mDeviceHasDpad;
    private long mStartSeekPos = 0;
    private long mLastSeekEventTime;
    private IMediaPlaybackService mService = null;
    private boolean playControlEnabledSetting = false;
    private boolean musicSwitchEnabledSetting = false;
    private RepeatingImageButton mPrevButton;
    private ImageButton mPauseButton;
    private RepeatingImageButton mNextButton;
    private ImageButton mRepeatButton;
    private ImageButton mShuffleButton;
    private ImageButton mQueueButton;
    private Worker mAlbumArtWorker;
    private AlbumArtHandler mAlbumArtHandler;
    private Toast mToast;
    private int mTouchSlop;
    private ServiceToken mToken;
    /* SPRD 476974 @{ */
    private TextView mLrcArtistName;
    private TextView mLrcTrackName;
    private LyricListView mList;
    private RelativeLayout mAlbumContainer;
    private ViewGroup mLyricContainer;  // Define as ViewGroup to adapt UUI and non-UUI for bug 295063
    private TextView mNolrcNotifier;
    private LinearLayout mLyricLayout;
    private RelativeLayout mTrackInfo;
    private ViewGroup mTrack;
    private ImageView mLyricIcon;
    private LyricDataBean lyricDataBean = null;
    private long mLastAudioId;
    private AlertDialog singersDialog;
    private boolean hideLyc = false;
    private static final int HIDE_LYC_SHOW_TRACK = 5;
    private Bitmap mAlbumBm = null;
    private int mShowType;
    /* @} */
    private final int MEDIA_PLAY_ACTIONBAR_ELEVATION = 8;
    /* SPRD 476969 @{ */
    private MenuItem mSearchItem;
    private SearchView mSearchView;
    private boolean mIsFirst = false;
    //SPRD bug fix 509392
    private boolean mLrc_Visible = false;
    /* @} */
    /* SPRD 476972 @{ */
    private static MediaPlaybackActivity mInstance = null;
    AlertDialog mDelDialog = null;
    public final static int PROTECT_MENU = 26;
    /* @} */

    /* SPRD 508982 @{ */
    private SPRDSwitchDetector mSwitchDetector;
    private SPRDShakeDetector mShakeDetector;
    private OnSwitchListener mSwitchListener = null;
    private OnShakeListener mShakeListener = null;
    /* @} */

    public MediaPlaybackActivity()
    {
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);

        if(RequestPermissionsActivity.startPermissionActivity(this)){
            return;
        }

        /* SPRD:494107 if icicle is null or the value from icicle is null,don't show lyric @{ */
        /* SPRD 499639 @{ */
        if (icicle != null) {
            mShowType = icicle.getInt(LRC_STATE,LyricListView.SHOW_TYPE_HIDE_AWALAYS);
            //SPRD bug fix 509392
            mLrc_Visible = icicle.getBoolean(LRC_VISIBILITY, true);
            mIsFirst = false;
        } else {
            //SPRD bug fix 509392
            mShowType = LyricListView.SHOW_TYPE_SHOW_AWALAYS;
            mIsFirst = true;
        }
        /* @} */
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mAlbumArtWorker = new Worker("album art worker");
        mAlbumArtHandler = new AlbumArtHandler(mAlbumArtWorker.getLooper());
        /* SPRD 476976 @{ */
        ActionBar actionbar = getActionBar();
        actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
        actionbar.setDisplayUseLogoEnabled(false);
        actionbar.setDisplayShowHomeEnabled(false);
        actionbar.setElevation(MEDIA_PLAY_ACTIONBAR_ELEVATION);
        /* @} */



        setContentView(R.layout.audio_player);
        /* SPRD 476974 @{ */
        initlrcViews();
        /* SPRD 506191 @{ */
        //getWindow().setStatusBarColor(R.color.media_play_back_statusbar);
        /* @} */
        // SPRD 476978
        MusicDRM.getInstance().initDRM(MediaPlaybackActivity.this);
        mCurrentTime = (TextView) findViewById(R.id.currenttime);
        mTotalTime = (TextView) findViewById(R.id.totaltime);
        mProgress = (ProgressBar) findViewById(android.R.id.progress);
        mAlbum = (ImageView) findViewById(R.id.album);
        mArtistName = (TextView) findViewById(R.id.artistname);
        mAlbumName = (TextView) findViewById(R.id.albumname);
        mTrackName = (TextView) findViewById(R.id.trackname);

        View v = (View)mArtistName.getParent(); 
        v.setOnTouchListener(this);
        v.setOnLongClickListener(this);

        v = (View)mAlbumName.getParent();
        v.setOnTouchListener(this);
        v.setOnLongClickListener(this);

        v = (View)mTrackName.getParent();
        v.setOnTouchListener(this);
        v.setOnLongClickListener(this);
        
        mPrevButton = (RepeatingImageButton) findViewById(R.id.prev);
        mPrevButton.setOnClickListener(mPrevListener);
        mPrevButton.setRepeatListener(mRewListener, 260);
        mPauseButton = (ImageButton) findViewById(R.id.pause);
        mPauseButton.requestFocus();
        mPauseButton.setOnClickListener(mPauseListener);
        mNextButton = (RepeatingImageButton) findViewById(R.id.next);
        mNextButton.setOnClickListener(mNextListener);
        mNextButton.setRepeatListener(mFfwdListener, 260);
        seekmethod = 1;

        mDeviceHasDpad = (getResources().getConfiguration().navigation ==
            Configuration.NAVIGATION_DPAD);
        
        mQueueButton = (ImageButton) findViewById(R.id.curplaylist);
        mQueueButton.setOnClickListener(mQueueListener);
        mShuffleButton = ((ImageButton) findViewById(R.id.shuffle));
        mShuffleButton.setOnClickListener(mShuffleListener);
        mRepeatButton = ((ImageButton) findViewById(R.id.repeat));
        mRepeatButton.setOnClickListener(mRepeatListener);
        
        if (mProgress instanceof SeekBar) {
            SeekBar seeker = (SeekBar) mProgress;
            seeker.setOnSeekBarChangeListener(mSeekListener);
        }
        mProgress.setMax(1000);

        mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
    }
    
    int mInitialX = -1;
    int mLastX = -1;
    int mTextWidth = 0;
    int mViewWidth = 0;
    boolean mDraggingLabel = false;
    
    TextView textViewForContainer(View v) {
        View vv = v.findViewById(R.id.artistname);
        if (vv != null) return (TextView) vv;
        vv = v.findViewById(R.id.albumname);
        if (vv != null) return (TextView) vv;
        vv = v.findViewById(R.id.trackname);
        if (vv != null) return (TextView) vv;
        return null;
    }
    
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        TextView tv = textViewForContainer(v);
        if (tv == null) {
            return false;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            // SPRD 476974
            v.setBackgroundColor(R.color.media_play_back_background);
            mInitialX = mLastX = (int) event.getX();
            mDraggingLabel = false;
        } else if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_CANCEL) {
            v.setBackgroundColor(0);
            if (mDraggingLabel) {
                Message msg = mLabelScroller.obtainMessage(0, tv);
                mLabelScroller.sendMessageDelayed(msg, 1000);
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (mDraggingLabel) {
                int scrollx = tv.getScrollX();
                int x = (int) event.getX();
                int delta = mLastX - x;
                if (delta != 0) {
                    mLastX = x;
                    scrollx += delta;
                    if (scrollx > mTextWidth) {
                        // scrolled the text completely off the view to the left
                        scrollx -= mTextWidth;
                        scrollx -= mViewWidth;
                    }
                    if (scrollx < -mViewWidth) {
                        // scrolled the text completely off the view to the right
                        scrollx += mViewWidth;
                        scrollx += mTextWidth;
                    }
                    tv.scrollTo(scrollx, 0);
                }
                return true;
            }
            int delta = mInitialX - (int) event.getX();
            if (Math.abs(delta) > mTouchSlop) {
                // start moving
                mLabelScroller.removeMessages(0, tv);
                
                // Only turn ellipsizing off when it's not already off, because it
                // causes the scroll position to be reset to 0.
                if (tv.getEllipsize() != null) {
                    tv.setEllipsize(null);
                }
                Layout ll = tv.getLayout();
                // layout might be null if the text just changed, or ellipsizing
                // was just turned off
                if (ll == null) {
                    return false;
                }
                // get the non-ellipsized line width, to determine whether scrolling
                // should even be allowed
                mTextWidth = (int) tv.getLayout().getLineWidth(0);
                mViewWidth = tv.getWidth();
                if (mViewWidth > mTextWidth) {
                    tv.setEllipsize(TruncateAt.END);
                    v.cancelLongPress();
                    return false;
                }
                mDraggingLabel = true;
                tv.setHorizontalFadingEdgeEnabled(true);
                v.cancelLongPress();
                return true;
            }
        }
        return false; 
    }

    Handler mLabelScroller = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            TextView tv = (TextView) msg.obj;
            int x = tv.getScrollX();
            x = x * 3 / 4;
            tv.scrollTo(x, 0);
            if (x == 0) {
                tv.setEllipsize(TruncateAt.END);
            } else {
                Message newmsg = obtainMessage(0, tv);
                mLabelScroller.sendMessageDelayed(newmsg, 15);
            }
        }
    };
    
    public boolean onLongClick(View view) {

        CharSequence title = null;
        String mime = null;
        String query = null;
        String artist;
        String album;
        String song;
        long audioid;
        
        try {
            artist = mService.getArtistName();
            album = mService.getAlbumName();
            song = mService.getTrackName();
            audioid = mService.getAudioId();
        } catch (RemoteException ex) {
            return true;
        } catch (NullPointerException ex) {
            // we might not actually have the service yet
            return true;
        }

        if (MediaStore.UNKNOWN_STRING.equals(album) &&
                MediaStore.UNKNOWN_STRING.equals(artist) &&
                song != null &&
                song.startsWith("recording")) {
            // not music
            return false;
        }

        if (audioid < 0) {
            return false;
        }

        Cursor c = MusicUtils.query(this,
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioid),
                new String[] {MediaStore.Audio.Media.IS_MUSIC}, null, null, null);
        boolean ismusic = true;
        if (c != null) {
            if (c.moveToFirst()) {
                ismusic = c.getInt(0) != 0;
            }
            c.close();
        }
        if (!ismusic) {
            return false;
        }

        boolean knownartist =
            (artist != null) && !MediaStore.UNKNOWN_STRING.equals(artist);

        boolean knownalbum =
            (album != null) && !MediaStore.UNKNOWN_STRING.equals(album);
        
        if (knownartist && view.equals(mArtistName.getParent())) {
            title = artist;
            query = artist;
            mime = MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE;
        } else if (knownalbum && view.equals(mAlbumName.getParent())) {
            title = album;
            if (knownartist) {
                query = artist + " " + album;
            } else {
                query = album;
            }
            mime = MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE;
        } else if (view.equals(mTrackName.getParent()) || !knownartist || !knownalbum) {
            if ((song == null) || MediaStore.UNKNOWN_STRING.equals(song)) {
                // A popup of the form "Search for null/'' using ..." is pretty
                // unhelpful, plus, we won't find any way to buy it anyway.
                return true;
            }

            title = song;
            if (knownartist) {
                query = artist + " " + song;
            } else {
                query = song;
            }
            mime = "audio/*"; // the specific type doesn't matter, so don't bother retrieving it
        } else {
            throw new RuntimeException("shouldn't be here");
        }
        title = getString(R.string.mediasearch, title);

        Intent i = new Intent();
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
        i.putExtra(SearchManager.QUERY, query);
        if(knownartist) {
            i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist);
        }
        if(knownalbum) {
            i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, album);
        }
        i.putExtra(MediaStore.EXTRA_MEDIA_TITLE, song);
        i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, mime);

        startActivity(Intent.createChooser(i, title));
        return true;
    }

    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        // SPRD 476974
        boolean isfromuser = false;
        public void onStartTrackingTouch(SeekBar bar) {
            mLastSeekEventTime = 0;
            mFromTouch = true;
        }
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            /* SPRD, Fix Bug 505234 @{ */
            // SPRD 476974
            /*
            isfromuser=fromuser;
            if (!fromuser || (mService == null)) return;
            long now = SystemClock.elapsedRealtime();
            if ((now - mLastSeekEventTime) > 250) {
                mLastSeekEventTime = now;
                mPosOverride = mDuration * progress / 1000;
                try {
                    mService.seek(mPosOverride);
                } catch (RemoteException ex) {
                }

                // trackball event, allow progress updates
                if (!mFromTouch) {
                    refreshNow();
                    mPosOverride = -1;
                }
            }*/

            isfromuser=fromuser;
            if (!fromuser || (mService == null)) return;
            long now = SystemClock.elapsedRealtime();
            mPosOverride = mDuration * progress / 1000;

            // trackball event, allow progress updates
            if((now - mLastSeekEventTime) > 250){
                mLastSeekEventTime = now;
                if (!mFromTouch) {
                    refreshNow();
                    mPosOverride = -1;
                }
            }
            /* @} */
        }
        public void onStopTrackingTouch(SeekBar bar) {
            /* SPRD 476974 @{ */
            if (mService != null && isfromuser) {
                 try {
                     mService.seek(mPosOverride);
                     refreshNow();
                 } catch (RemoteException ex) {
                     Log.e(LOGTAG, "Error:" + ex);
                 }
            }
            /* @} */
            mPosOverride = -1;
            mFromTouch = false;
        }
    };
    
    private View.OnClickListener mQueueListener = new View.OnClickListener() {
        public void onClick(View v) {
            /* SPRD 496677  @{ */
            startActivityForResult(
                    new Intent(Intent.ACTION_EDIT)
                    .setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track")
                    .putExtra("playlist", "nowplaying"),QUEUE
            );
            /* @} */
        }
    };
    
    private View.OnClickListener mShuffleListener = new View.OnClickListener() {
        public void onClick(View v) {
            toggleShuffle();
        }
    };

    private View.OnClickListener mRepeatListener = new View.OnClickListener() {
        public void onClick(View v) {
            cycleRepeat();
        }
    };

    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
        }
    };

    private View.OnClickListener mPrevListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mService == null) return;
            try {
                /* SPRD 476972 @{ */
                //if (mService.position() < 2000) {
                //    mService.prev();
                //} else {
                //    mService.seek(0);
                //    mService.play();
                //}
                mService.prev();
                /* @} */
            } catch (RemoteException ex) {
            }
        }
    };

    private View.OnClickListener mNextListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mService == null) return;
            try {
                mService.next();
            } catch (RemoteException ex) {
            }
        }
    };

    private RepeatingImageButton.RepeatListener mRewListener =
        new RepeatingImageButton.RepeatListener() {
        public void onRepeat(View v, long howlong, int repcnt) {
            scanBackward(repcnt, howlong);
        }
    };
    
    private RepeatingImageButton.RepeatListener mFfwdListener =
        new RepeatingImageButton.RepeatListener() {
        public void onRepeat(View v, long howlong, int repcnt) {
            scanForward(repcnt, howlong);
        }
    };
   
    @Override
    public void onStop() {
        paused = true;
        mHandler.removeMessages(REFRESH);
        unregisterReceiver(mStatusListener);

        /* SPRD 518033 unbindservice @{ */
        MusicUtils.unbindFromService(mToken, this);
        /* @} */

        mService = null;
        /* SPRD 476972 @{ */
        mPosOverride = -1;
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
        paused = false;

        mToken = MusicUtils.bindToService(this, osc);
        if (mToken == null) {
            // something went wrong
            mHandler.sendEmptyMessage(QUIT);
        }
        
        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
        f.addAction(MediaPlaybackService.META_CHANGED);
        /* SPRD 476972 @{ */
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        /* @} */
        registerReceiver(mStatusListener, new IntentFilter(f));
        updateTrackInfo();
        long next = refreshNow();
        queueNextRefresh(next);
    }
    
    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        /* SPRD 476969 @{ */
        // SPRD: Delete for bug 504766
        //if(mSearchItem != null)
        //    mSearchItem.collapseActionView();
        /* @} */
        /* SPRD bug fix 508982@{ */
        playControlEnabledSetting = Settings.Global.getInt(getContentResolver(),
                Settings.Global.PLAY_CONTROL, 0) != 0;
        musicSwitchEnabledSetting = Settings.Global.getInt(getContentResolver(),
                Settings.Global.MUSIC_SWITCH, 0) != 0;
        Log.e(LOGTAG, "playControlEnabledSetting == " + playControlEnabledSetting + " ;musicSwitchEnabledSetting == " + musicSwitchEnabledSetting);
        if (playControlEnabledSetting) {
            mSwitchDetector = new SPRDSwitchDetector(MediaPlaybackActivity.this);
            mSwitchDetector.start();
            mSwitchListener = new OnSwitchListener() {
                @Override
                public void onFlip() {
                    if (mService == null)
                        return;
                    try {
                        if (mService.isPlaying()) {
                            mService.pause();
                        } else {
                            mService.play();
                        }
                    } catch (RemoteException e) {
                    }
                }
            };
            mSwitchDetector.registerOnSwitchListener(mSwitchListener);
        }

        if (musicSwitchEnabledSetting) {
            mShakeDetector = new SPRDShakeDetector(MediaPlaybackActivity.this);
            mShakeDetector.start();
            mShakeListener = new OnShakeListener() {
                @Override
                public void onShake(int direction) {
                    if (direction == 1) {
                        if (mService == null)
                            return;
                        try {
                            mService.prev();
                        } catch (RemoteException ex) {
                        }
                    } else if (direction == 2) {
                        if (mService == null)
                            return;
                        try {
                            mService.next();
                        } catch (RemoteException ex) {
                        }
                    }
                }
            };
            mShakeDetector.registerOnShakeListener(mShakeListener);
        }
        /* @} */
        updateTrackInfo();
        setPauseButtonImage();
    }
    
    @Override
    public void onDestroy()
    {
        /* SPRD 476972 @{ */
        Log.i(LOGTAG,"onDestroy");
        if (mDelDialog != null && mDelDialog.isShowing()) {
            mDelDialog.dismiss();
        }
        /* @} */
        if(mAlbumArtWorker != null){
            mAlbumArtWorker.quit();
        }
        // SPRD 476978
        MusicDRM.getInstance().destroyDRM();
        super.onDestroy();
        //System.out.println("***************** playback activity onDestroy\n");
    }

    /* SPRD bug fix 508982 @{ */
    @Override
    public void onPause() {
        if (mSwitchDetector != null) {
            mSwitchDetector.unregisterOnSwitchListener(mSwitchListener);
            mSwitchDetector.stop();
        }
        if (mShakeDetector != null) {
            mShakeDetector.unregisterOnShakeListener(mShakeListener);
            mShakeDetector.stop();
        }
        super.onPause();
    }
    /* @} */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        /* SPRD 476969 @{ */
        getMenuInflater().inflate(R.menu.options_menu_overlay, menu);
        mSearchItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) mSearchItem.getActionView();
        mSearchView.setQueryHint(getResources().getString(R.string.search_title_hint));
        /* SPRD: Add for bug 504766 @{ */
        mSearchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int i) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int i) {
                if(mSearchItem != null)
                    mSearchItem.collapseActionView();
                return false;
            }
        });
        /* @} */
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            SearchableInfo info = searchManager.getSearchableInfo(this.getComponentName());
            mSearchView.setSearchableInfo(info);
        }
        int id = mSearchView.getContext().getResources()
                .getIdentifier("android:id/search_src_text", null, null);
        TextView textView = (TextView) mSearchView.findViewById(id);
        textView.setTextColor(Color.WHITE);
          /* @} */

        // Don't show the menu items if we got launched by path/filedescriptor, or
        // if we're in one shot mode. In most cases, these menu items are not
        // useful in those modes, so for consistency we never show them in these
        // modes, instead of tailoring them to the specific file being played.
        /* SPRD 476972 @{ */
        //if (MusicUtils.getCurrentAudioId() >= 0) {
        /* @} */
            menu.add(0, GOTO_START, 0, R.string.goto_start).setIcon(R.drawable.ic_menu_music_library);
            menu.add(0, PARTY_SHUFFLE, 0, R.string.party_shuffle); // icon will be set in onPrepareOptionsMenu()
            SubMenu sub = menu.addSubMenu(0, ADD_TO_PLAYLIST, 0,
                    R.string.add_to_playlist).setIcon(android.R.drawable.ic_menu_add);
            // these next two are in a separate group, so they can be shown/hidden as needed
            // based on the keyguard state
            /* SPRD: Add for bug 540629 @{ */
            if (MusicUtils.isSystemUser(this)) {
                menu.add(1, USE_AS_RINGTONE, 0, R.string.ringtone_menu_short)
                        .setIcon(R.drawable.ic_menu_set_as_ringtone);
            }
            /* @} */
            menu.add(1, DELETE_ITEM, 0, R.string.delete_item)
                    .setIcon(R.drawable.ic_menu_delete);

            Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
            if (getPackageManager().resolveActivity(i, 0) != null) {
                menu.add(0, EFFECTS_PANEL, 0, R.string.effectspanel).setIcon(R.drawable.ic_menu_eq);
            }

            /*SPRD bug 494035@{ */
            menu.add(0, QUIT_MUSIC, 1, R.string.quit);
            /* @} */
            return true;
        /* SPRD 476972 @{ */
        //}
        //return false;
        /* @} */
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // SPRD 476976
        //if (mService == null) return false;

        if(MusicUtils.checkPermission(this) != MusicUtils.PERMISSION_ALL_ALLOWED) {
            return false;
        }

        MenuItem item = menu.findItem(PARTY_SHUFFLE);
        if (item != null) {
            int shuffle = MusicUtils.getCurrentShuffleMode();
            if (shuffle == MediaPlaybackService.SHUFFLE_AUTO) {
                item.setIcon(R.drawable.ic_menu_party_shuffle);
                item.setTitle(R.string.party_shuffle_off);
            } else {
                item.setIcon(R.drawable.ic_menu_party_shuffle);
                item.setTitle(R.string.party_shuffle);
            }
        }

        item = menu.findItem(ADD_TO_PLAYLIST);
        if (item != null) {
            SubMenu sub = item.getSubMenu();
            MusicUtils.makePlaylistMenu(this, sub);
            /* SPRD 476972 @{ */
            sub.removeItem(QUEUE);
            /* @} */
        }
        /*SPRD bug 494035@{ */
        item = menu.findItem(QUIT_MUSIC);
        if (item != null) {
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        /* @} */
        Log.i("DRM","MusicDRM.getInstance().isDRM() =" +MusicDRM.getInstance().isDRM());
        /* SPRD 506342 @{ */
        menu.removeItem(PROTECT_MENU);
        /* SPRD 506449  @{ */
        menu.removeItem(USE_AS_RINGTONE);
        /* @} */
        /* SPRD 476978  @{ */
        if (MusicDRM.getInstance().isDRM()) {
            MusicDRM.getInstance().onPrepareDRMMediaplaybackOptionsMenu(menu);
        } else {
            /* SPRD 506449  @{ */
            /* SPRD: Add for bug 540629 @{ */
            if (MusicUtils.isSystemUser(this)) {
                menu.add(1, USE_AS_RINGTONE, 0, R.string.ringtone_menu_short)
                        .setIcon(R.drawable.ic_menu_set_as_ringtone);
            }
                /* @} */
            /* @} */
        }
        /* @} */
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        menu.setGroupVisible(1, !km.inKeyguardRestrictedInputMode());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        try {
            switch (item.getItemId()) {
                case GOTO_START:
                    intent = new Intent();
                    intent.setClass(this, MusicBrowserActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                    break;
                case USE_AS_RINGTONE: {
                    // Set the system setting to make this the current ringtone
                    if (mService != null) {
                        /* SPRD 502327 Click the menu to achieve the ring function.@{ */
                        //MusicUtils.setRingtone(this, mService.getAudioId());
                        SPRDMusicUtils.doChoiceRingtone(this,mService.getAudioId());
                        /* @} */
                    }
                    return true;
                }
                case PARTY_SHUFFLE:
                    MusicUtils.togglePartyShuffle();
                    setShuffleButtonImage();
                    /* SPRD 476972 @{ */
                    setRepeatButtonImage();
                    break;
                    
                case NEW_PLAYLIST: {
                    intent = new Intent();
                    intent.setClass(this, CreatePlaylist.class);
                    startActivityForResult(intent, NEW_PLAYLIST);
                    return true;
                }

                case PLAYLIST_SELECTED: {
                    long [] list = new long[1];
                    list[0] = MusicUtils.getCurrentAudioId();
                    long playlist = item.getIntent().getLongExtra("playlist", 0);
                    MusicUtils.addToPlaylist(this, list, playlist);
                    return true;
                }
                
                case DELETE_ITEM: {
                    if (mService != null) {
                        long [] list = new long[1];
                        list[0] = MusicUtils.getCurrentAudioId();

                        String f;
                        /* SPRD 476972 @{ */
                        f = getString(R.string.delete_song, mService.getTrackName());
                        mDelDialog = SPRDMusicUtils.showDeleteItemDialog(MediaPlaybackActivity.this, f, list);
                        /* @} */
                    }
                    return true;
                }

                case EFFECTS_PANEL: {
                    /* SPRD 476972 @{ */
                    if(mService ==null) {
                        return false;
                    }
                    /*@}*/
                    Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                    i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mService.getAudioSessionId());
                    startActivityForResult(i, EFFECTS_PANEL);
                    return true;
                }

                /*SPRD bug 494035@{ */
                case QUIT_MUSIC:
                    SPRDMusicUtils.quitservice(this);
                    return true;
                /*@}*/
            }
        } catch (RemoteException ex) {
        }
        // SPRD 476978
        MusicDRM.getInstance().onDRMMediaplaybackOptionsMenuSelected(MediaPlaybackActivity.this,item);
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case NEW_PLAYLIST:
                Uri uri = intent.getData();
                if (uri != null) {
                    long [] list = new long[1];
                    list[0] = MusicUtils.getCurrentAudioId();
                    int playlist = Integer.parseInt(uri.getLastPathSegment());
                    MusicUtils.addToPlaylist(this, list, playlist);
                }
                break;
           /* SPRD 496677  @{ */
           case QUEUE:
                finish();
                break;
           /* @} */
        }
    }
    private final int keyboard[][] = {
        {
            KeyEvent.KEYCODE_Q,
            KeyEvent.KEYCODE_W,
            KeyEvent.KEYCODE_E,
            KeyEvent.KEYCODE_R,
            KeyEvent.KEYCODE_T,
            KeyEvent.KEYCODE_Y,
            KeyEvent.KEYCODE_U,
            KeyEvent.KEYCODE_I,
            KeyEvent.KEYCODE_O,
            KeyEvent.KEYCODE_P,
        },
        {
            KeyEvent.KEYCODE_A,
            KeyEvent.KEYCODE_S,
            KeyEvent.KEYCODE_D,
            KeyEvent.KEYCODE_F,
            KeyEvent.KEYCODE_G,
            KeyEvent.KEYCODE_H,
            KeyEvent.KEYCODE_J,
            KeyEvent.KEYCODE_K,
            KeyEvent.KEYCODE_L,
            KeyEvent.KEYCODE_DEL,
        },
        {
            KeyEvent.KEYCODE_Z,
            KeyEvent.KEYCODE_X,
            KeyEvent.KEYCODE_C,
            KeyEvent.KEYCODE_V,
            KeyEvent.KEYCODE_B,
            KeyEvent.KEYCODE_N,
            KeyEvent.KEYCODE_M,
            KeyEvent.KEYCODE_COMMA,
            KeyEvent.KEYCODE_PERIOD,
            KeyEvent.KEYCODE_ENTER
        }

    };

    private int lastX;
    private int lastY;

    private boolean seekMethod1(int keyCode)
    {
        if (mService == null) return false;
        for(int x=0;x<10;x++) {
            for(int y=0;y<3;y++) {
                if(keyboard[y][x] == keyCode) {
                    int dir = 0;
                    // top row
                    if(x == lastX && y == lastY) dir = 0;
                    else if (y == 0 && lastY == 0 && x > lastX) dir = 1;
                    else if (y == 0 && lastY == 0 && x < lastX) dir = -1;
                    // bottom row
                    else if (y == 2 && lastY == 2 && x > lastX) dir = -1;
                    else if (y == 2 && lastY == 2 && x < lastX) dir = 1;
                    // moving up
                    else if (y < lastY && x <= 4) dir = 1; 
                    else if (y < lastY && x >= 5) dir = -1; 
                    // moving down
                    else if (y > lastY && x <= 4) dir = -1; 
                    else if (y > lastY && x >= 5) dir = 1; 
                    lastX = x;
                    lastY = y;
                    try {
                        mService.seek(mService.position() + dir * 5);
                    } catch (RemoteException ex) {
                    }
                    refreshNow();
                    return true;
                }
            }
        }
        lastX = -1;
        lastY = -1;
        return false;
    }

    private boolean seekMethod2(int keyCode)
    {
        if (mService == null) return false;
        for(int i=0;i<10;i++) {
            if(keyboard[0][i] == keyCode) {
                int seekpercentage = 100*i/10;
                try {
                    mService.seek(mService.duration() * seekpercentage / 100);
                } catch (RemoteException ex) {
                }
                refreshNow();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        try {
            switch(keyCode)
            {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (!useDpadMusicControl()) {
                        break;
                    }
                    if (mService != null) {
                        if (!mSeeking && mStartSeekPos >= 0) {
                            mPauseButton.requestFocus();
                            if (mStartSeekPos < 1000) {
                                mService.prev();
                            } else {
                                mService.seek(0);
                            }
                        } else {
                            scanBackward(-1, event.getEventTime() - event.getDownTime());
                            mPauseButton.requestFocus();
                            mStartSeekPos = -1;
                        }
                    }
                    mSeeking = false;
                    mPosOverride = -1;
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (!useDpadMusicControl()) {
                        break;
                    }
                    if (mService != null) {
                        if (!mSeeking && mStartSeekPos >= 0) {
                            mPauseButton.requestFocus();
                            mService.next();
                        } else {
                            scanForward(-1, event.getEventTime() - event.getDownTime());
                            mPauseButton.requestFocus();
                            mStartSeekPos = -1;
                        }
                    }
                    mSeeking = false;
                    mPosOverride = -1;
                    return true;
            }
        } catch (RemoteException ex) {
        }
        return super.onKeyUp(keyCode, event);
    }

    private boolean useDpadMusicControl() {
        if (mDeviceHasDpad && (mPrevButton.isFocused() ||
                mNextButton.isFocused() ||
                mPauseButton.isFocused())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        int direction = -1;
        int repcnt = event.getRepeatCount();

        if((seekmethod==0)?seekMethod1(keyCode):seekMethod2(keyCode))
            return true;

        switch(keyCode)
        {
/*
            // image scale
            case KeyEvent.KEYCODE_Q: av.adjustParams(-0.05, 0.0, 0.0, 0.0, 0.0,-1.0); break;
            case KeyEvent.KEYCODE_E: av.adjustParams( 0.05, 0.0, 0.0, 0.0, 0.0, 1.0); break;
            // image translate
            case KeyEvent.KEYCODE_W: av.adjustParams(    0.0, 0.0,-1.0, 0.0, 0.0, 0.0); break;
            case KeyEvent.KEYCODE_X: av.adjustParams(    0.0, 0.0, 1.0, 0.0, 0.0, 0.0); break;
            case KeyEvent.KEYCODE_A: av.adjustParams(    0.0,-1.0, 0.0, 0.0, 0.0, 0.0); break;
            case KeyEvent.KEYCODE_D: av.adjustParams(    0.0, 1.0, 0.0, 0.0, 0.0, 0.0); break;
            // camera rotation
            case KeyEvent.KEYCODE_R: av.adjustParams(    0.0, 0.0, 0.0, 0.0, 0.0,-1.0); break;
            case KeyEvent.KEYCODE_U: av.adjustParams(    0.0, 0.0, 0.0, 0.0, 0.0, 1.0); break;
            // camera translate
            case KeyEvent.KEYCODE_Y: av.adjustParams(    0.0, 0.0, 0.0, 0.0,-1.0, 0.0); break;
            case KeyEvent.KEYCODE_N: av.adjustParams(    0.0, 0.0, 0.0, 0.0, 1.0, 0.0); break;
            case KeyEvent.KEYCODE_G: av.adjustParams(    0.0, 0.0, 0.0,-1.0, 0.0, 0.0); break;
            case KeyEvent.KEYCODE_J: av.adjustParams(    0.0, 0.0, 0.0, 1.0, 0.0, 0.0); break;

*/

            case KeyEvent.KEYCODE_SLASH:
                seekmethod = 1 - seekmethod;
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (!useDpadMusicControl()) {
                    break;
                }
                if (!mPrevButton.hasFocus()) {
                    mPrevButton.requestFocus();
                }
                scanBackward(repcnt, event.getEventTime() - event.getDownTime());
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!useDpadMusicControl()) {
                    break;
                }
                if (!mNextButton.hasFocus()) {
                    mNextButton.requestFocus();
                }
                scanForward(repcnt, event.getEventTime() - event.getDownTime());
                return true;

            case KeyEvent.KEYCODE_S:
                toggleShuffle();
                return true;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_SPACE:
                doPauseResume();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private void scanBackward(int repcnt, long delta) {
        if(mService == null) return;
        try {
            if(repcnt == 0) {
                mStartSeekPos = mService.position();
                mLastSeekEventTime = 0;
                mSeeking = false;
            } else {
                mSeeking = true;
                if (delta < 5000) {
                    // seek at 10x speed for the first 5 seconds
                    delta = delta * 10; 
                } else {
                    // seek at 40x after that
                    delta = 50000 + (delta - 5000) * 40;
                }
                long newpos = mStartSeekPos - delta;
                if (newpos < 0) {
                    // move to previous track
                    mService.prev();
                    long duration = mService.duration();
                    mStartSeekPos += duration;
                    newpos += duration;
                }
                if (((delta - mLastSeekEventTime) > 250) || repcnt < 0){
                    mService.seek(newpos);
                    mLastSeekEventTime = delta;
                }
                if (repcnt >= 0) {
                    mPosOverride = newpos;
                } else {
                    mPosOverride = -1;
                }
                refreshNow();
            }
        } catch (RemoteException ex) {
        }
    }

    private void scanForward(int repcnt, long delta) {
        if(mService == null) return;
        try {
            if(repcnt == 0) {
                mStartSeekPos = mService.position();
                mLastSeekEventTime = 0;
                mSeeking = false;
            } else {
                mSeeking = true;
                if (delta < 5000) {
                    // seek at 10x speed for the first 5 seconds
                    delta = delta * 10; 
                } else {
                    // seek at 40x after that
                    delta = 50000 + (delta - 5000) * 40;
                }
                long newpos = mStartSeekPos + delta;
                long duration = mService.duration();
                if (newpos >= duration) {
                    // move to next track
                    mService.next();
                    mStartSeekPos -= duration; // is OK to go negative
                    newpos -= duration;
                }
                if (((delta - mLastSeekEventTime) > 250) || repcnt < 0){
                    mService.seek(newpos);
                    mLastSeekEventTime = delta;
                }
                if (repcnt >= 0) {
                    mPosOverride = newpos;
                } else {
                    mPosOverride = -1;
                }
                refreshNow();
            }
        } catch (RemoteException ex) {
        }
    }
    
    private void doPauseResume() {
        try {
            if(mService != null) {
                if (mService.isPlaying()) {
                    mService.pause();
                    // SPRD 476974
                    mList.pause();
                } else {
                    mService.play();
                    // SPRD 476974
                    mList.start();
                }
                refreshNow();
                setPauseButtonImage();
            }
        } catch (RemoteException ex) {
        }
    }
    
    private void toggleShuffle() {
        if (mService == null) {
            return;
        }
        try {
            int shuffle = mService.getShuffleMode();
            if (shuffle == MediaPlaybackService.SHUFFLE_NONE) {
                mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NORMAL);
                if (mService.getRepeatMode() == MediaPlaybackService.REPEAT_CURRENT) {
                    mService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
                    /* SPRD 476972 @{ */
                    //setRepeatButtonImage();
                }
                /* SPRD 476972 @{ */
                setRepeatButtonImage();
                showToast(R.string.shuffle_on_notif);
            } else if (shuffle == MediaPlaybackService.SHUFFLE_NORMAL ||
                    shuffle == MediaPlaybackService.SHUFFLE_AUTO) {
                mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
                showToast(R.string.shuffle_off_notif);
            } else {
                Log.e("MediaPlaybackActivity", "Invalid shuffle mode: " + shuffle);
            }
            setShuffleButtonImage();
        } catch (RemoteException ex) {
        }
    }
    
    private void cycleRepeat() {
        if (mService == null) {
            return;
        }
        try {
            int mode = mService.getRepeatMode();
            if (mode == MediaPlaybackService.REPEAT_NONE) {
                mService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
                showToast(R.string.repeat_all_notif);
            } else if (mode == MediaPlaybackService.REPEAT_ALL) {
                mService.setRepeatMode(MediaPlaybackService.REPEAT_CURRENT);
                if (mService.getShuffleMode() != MediaPlaybackService.SHUFFLE_NONE) {
                    mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
                    setShuffleButtonImage();
                }
                showToast(R.string.repeat_current_notif);
            } else {
                mService.setRepeatMode(MediaPlaybackService.REPEAT_NONE);
                showToast(R.string.repeat_off_notif);
            }
            setRepeatButtonImage();
        } catch (RemoteException ex) {
        }
        
    }
    
    private void showToast(int resid) {
        if (mToast == null) {
            mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        }
        mToast.setText(resid);
        mToast.show();
    }

    private void startPlayback() {

        if(mService == null)
            return;
        Intent intent = getIntent();
        String filename = "";
        Uri uri = intent.getData();
        if (uri != null && uri.toString().length() > 0) {
            // If this is a file:// URI, just use the path directly instead
            // of going through the open-from-filedescriptor codepath.
            String scheme = uri.getScheme();
            if ("file".equals(scheme)) {
                filename = uri.getPath();
            } else {
                filename = uri.toString();
            }
            try {
                mService.stop();
                mService.openFile(filename);
                mService.play();
                setIntent(new Intent());
            } catch (Exception ex) {
                Log.d("MediaPlaybackActivity", "couldn't start playback: " + ex);
            }
        }

        updateTrackInfo();
        long next = refreshNow();
        queueNextRefresh(next);
    }

    private ServiceConnection osc = new ServiceConnection() {
            public void onServiceConnected(ComponentName classname, IBinder obj) {
                mService = IMediaPlaybackService.Stub.asInterface(obj);
                startPlayback();
                try {
                    // Assume something is playing when the service says it is,
                    // but also if the audio ID is valid but the service is paused.
                    if (mService.getAudioId() >= 0 || mService.isPlaying() ||
                            mService.getPath() != null) {
                        // something is playing now, we're done
                        mRepeatButton.setVisibility(View.VISIBLE);
                        mShuffleButton.setVisibility(View.VISIBLE);
                        mQueueButton.setVisibility(View.VISIBLE);
                        setRepeatButtonImage();
                        setShuffleButtonImage();
                        setPauseButtonImage();
                        return;
                    }
                } catch (RemoteException ex) {
                }
                finish();
            }
            public void onServiceDisconnected(ComponentName classname) {
                mService = null;
            }
    };

    private void setRepeatButtonImage() {
        if (mService == null) return;
        try {
            switch (mService.getRepeatMode()) {
                case MediaPlaybackService.REPEAT_ALL:
                    mRepeatButton.setImageResource(R.drawable.ic_mp_repeat_all_btn);
                    break;
                case MediaPlaybackService.REPEAT_CURRENT:
                    mRepeatButton.setImageResource(R.drawable.ic_mp_repeat_once_btn);
                    break;
                default:
                    mRepeatButton.setImageResource(R.drawable.ic_mp_repeat_off_btn);
                    break;
            }
        } catch (RemoteException ex) {
        }
    }
    
    private void setShuffleButtonImage() {
        if (mService == null) return;
        try {
            switch (mService.getShuffleMode()) {
                case MediaPlaybackService.SHUFFLE_NONE:
                    mShuffleButton.setImageResource(R.drawable.ic_mp_shuffle_off_btn);
                    break;
                case MediaPlaybackService.SHUFFLE_AUTO:
                    mShuffleButton.setImageResource(R.drawable.ic_mp_partyshuffle_on_btn);
                    break;
                default:
                    mShuffleButton.setImageResource(R.drawable.ic_mp_shuffle_on_btn);
                    break;
            }
        } catch (RemoteException ex) {
        }
    }
    
    private void setPauseButtonImage() {
        try {
            if (mService != null && mService.isPlaying()) {
                // SPRD 475999
                mPauseButton.setImageResource(R.drawable.ic_media_pause);
                /* SPRD 476972 @{ */
                if (!mSeeking) {
                    mPosOverride = -1;
                }
                /* @} */
            } else {
                // SPRD 475999
                mPauseButton.setImageResource(R.drawable.ic_media_play);
            }
        } catch (RemoteException ex) {
        }
    }
    
    private ImageView mAlbum;
    private TextView mCurrentTime;
    private TextView mTotalTime;
    private TextView mArtistName;
    private TextView mAlbumName;
    private TextView mTrackName;
    private ProgressBar mProgress;
    private long mPosOverride = -1;
    private boolean mFromTouch = false;
    private long mDuration;
    private int seekmethod;
    private boolean paused;

    private static final int REFRESH = 1;
    private static final int QUIT = 2;
    private static final int GET_ALBUM_ART = 3;
    private static final int ALBUM_ART_DECODED = 4;

    private void queueNextRefresh(long delay) {
        if (!paused) {
            Message msg = mHandler.obtainMessage(REFRESH);
            mHandler.removeMessages(REFRESH);
            mHandler.sendMessageDelayed(msg, delay);
        }
    }

    private long refreshNow() {
        if(mService == null)
            return 500;
        try {
            long pos = mPosOverride < 0 ? mService.position() : mPosOverride;
            if ((pos >= 0) && (mDuration > 0)) {
                /* SPRD 476972 @{ */
                if (pos > mDuration) {
                    pos = mDuration;
                }
                /* @} */
                mCurrentTime.setText(MusicUtils.makeTimeString(this, pos / 1000));
                int progress = (int) (1000 * pos / mDuration);
                mProgress.setProgress(progress);
                
                if (mService.isPlaying()) {
                    mCurrentTime.setVisibility(View.VISIBLE);
                } else {
                    // blink the counter
                    int vis = mCurrentTime.getVisibility();
                    mCurrentTime.setVisibility(vis == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
                    return 500;
                }
            } else {
                mCurrentTime.setText("--:--");
                mProgress.setProgress(1000);
            }
            // calculate the number of milliseconds until the next full second, so
            // the counter can be updated at just the right time
            long remaining = 1000 - (pos % 1000);

            // approximate how often we would need to refresh the slider to
            // move it smoothly
            int width = mProgress.getWidth();
            if (width == 0) width = 320;
            long smoothrefreshtime = mDuration / width;

            if (smoothrefreshtime > remaining) return remaining;
            if (smoothrefreshtime < 20) return 20;
            return smoothrefreshtime;
        } catch (RemoteException ex) {
        }
        return 500;
    }
    
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ALBUM_ART_DECODED:
                    /* SPRD 476972 @{ */
                    if((Bitmap)msg.obj !=null){
                        mAlbum.setImageBitmap((Bitmap)msg.obj);
                        mAlbum.getDrawable().setDither(true);
                        msg.obj = null;
                    }else{
                        mAlbum.setImageResource(R.drawable.albumart_mp_unknown);
                    }
                    /* @} */
                    break;

                case REFRESH:
                    long next = refreshNow();
                    queueNextRefresh(next);
                    break;
                    
                case QUIT:
                    // This can be moved back to onCreate once the bug that prevents
                    // Dialogs from being started from onCreate/onResume is fixed.
                    new AlertDialog.Builder(MediaPlaybackActivity.this)
                            .setTitle(R.string.service_start_error_title)
                            .setMessage(R.string.service_start_error_msg)
                            .setPositiveButton(R.string.service_start_error_button,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            finish();
                                        }
                                    })
                            .setCancelable(false)
                            .show();
                    break;
                /* SPRD 476972 @{ */
                case HIDE_LYC_SHOW_TRACK:
                    setLycicVisiable(false);
                    break;
                /* @} */

                default:
                    break;
            }
        }
    };

    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MediaPlaybackService.META_CHANGED)) {
                // redraw the artist/title info and
                // set new max for progress bar
                updateTrackInfo();
                setPauseButtonImage();
                queueNextRefresh(1);
            } else if (action.equals(MediaPlaybackService.PLAYSTATE_CHANGED)) {
                setPauseButtonImage();
            /* SPRD 476974 @{ */
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                Log.d(LOGTAG, "onReceive, stop refreshing ...");
                mHandler.removeMessages(REFRESH);
                mList.setScreenOnFlag(false);
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                Log.d(LOGTAG, "onReceive, restore refreshing ...");
                long next = refreshNow();
                queueNextRefresh(next);
                mList.setScreenOnFlag(true);
            }
            /* @} */
        }
    };

    private static class AlbumSongIdWrapper {
        public long albumid;
        public long songid;
        AlbumSongIdWrapper(long aid, long sid) {
            albumid = aid;
            songid = sid;
        }
    }
    
    private void updateTrackInfo() {
        if (mService == null) {
            return;
        }
        try {
            String path = mService.getPath();
            if (path == null) {
                finish();
                return;
            }
            
            long songid = mService.getAudioId(); 
            if (songid < 0 && path.toLowerCase().startsWith("http://")) {
                // Once we can get album art and meta data from MediaPlayer, we
                // can show that info again when streaming.
                ((View) mArtistName.getParent()).setVisibility(View.INVISIBLE);
                ((View) mAlbumName.getParent()).setVisibility(View.INVISIBLE);
                mAlbum.setVisibility(View.GONE);
                mTrackName.setText(path);
                mAlbumArtHandler.removeMessages(GET_ALBUM_ART);
                mAlbumArtHandler.obtainMessage(GET_ALBUM_ART, new AlbumSongIdWrapper(-1, -1)).sendToTarget();
                /* SPRD 476974 @{ */
                if(mLrcArtistName != null) {
                    ((View) mLrcArtistName.getParent()).setVisibility(View.INVISIBLE);
                    mLrcTrackName.setText(path);
                }
                /* @} */
            } else {
                ((View) mArtistName.getParent()).setVisibility(View.VISIBLE);
                ((View) mAlbumName.getParent()).setVisibility(View.VISIBLE);
                String artistName = mService.getArtistName();
                if (MediaStore.UNKNOWN_STRING.equals(artistName)) {
                    artistName = getString(R.string.unknown_artist_name);
                }
                mArtistName.setText(artistName);
                String albumName = mService.getAlbumName();
                long albumid = mService.getAlbumId();
                if (MediaStore.UNKNOWN_STRING.equals(albumName)) {
                    albumName = getString(R.string.unknown_album_name);
                    albumid = -1;
                }
                mAlbumName.setText(albumName);
                mTrackName.setText(mService.getTrackName());
                mAlbumArtHandler.removeMessages(GET_ALBUM_ART);
                mAlbumArtHandler.obtainMessage(GET_ALBUM_ART, new AlbumSongIdWrapper(albumid, songid)).sendToTarget();
                mAlbum.setVisibility(View.VISIBLE);
                /* SPRD 476974 @{ */
                if(mLrcArtistName != null) {
                    ((View) mLrcArtistName.getParent()).setVisibility(View.VISIBLE);
                    mLrcArtistName.setText(artistName);
                    mLrcTrackName.setText(mService.getTrackName());
                }
                /* @} */
            }
            // SPRD 476974
            updateLyrics(songid);
            mDuration = mService.duration();
            /* SPRD 476972 @{ */
            int secs = Math.round((float) mDuration / 1000);
            if(mDuration > 0 && mDuration < 1000){
                mTotalTime.setText(MusicUtils.makeTimeString(this, 1));
            }else if(mDuration >= 1000 || mDuration == 0){
                mTotalTime.setText(MusicUtils.makeTimeString(this, secs));
            }
            /* @} */
        } catch (RemoteException ex) {
            finish();
        }
    }
    
    public class AlbumArtHandler extends Handler {
        private long mAlbumId = -1;
        
        public AlbumArtHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg)
        {
            long albumid = ((AlbumSongIdWrapper) msg.obj).albumid;
            long songid = ((AlbumSongIdWrapper) msg.obj).songid;
            if (msg.what == GET_ALBUM_ART && (mAlbumId != albumid || albumid < 0)) {
                // while decoding the new image, show the default album art
                Message numsg = mHandler.obtainMessage(ALBUM_ART_DECODED, null);
                /* SPRD 476972 @{ */
                //mHandler.removeMessages(ALBUM_ART_DECODED);
                //mHandler.sendMessageDelayed(numsg, 300);
                /* @} */
                // Don't allow default artwork here, because we want to fall back to song-specific
                // album art if we can't find anything for the album.
                Bitmap bm = MusicUtils.getArtwork(MediaPlaybackActivity.this, songid, albumid, false);
                if (bm == null) {
                    bm = MusicUtils.getArtwork(MediaPlaybackActivity.this, songid, -1);
                    albumid = -1;
                }
                if (bm != null) {
                    numsg = mHandler.obtainMessage(ALBUM_ART_DECODED, bm);
                    mHandler.removeMessages(ALBUM_ART_DECODED);
                    mHandler.sendMessage(numsg);
                    /* SPRD 476972 @{ */
                    mAlbumBm = bm;

                }
                /* SPRD 476972 @{ */
                if (bm == null) {
                    numsg = mHandler.obtainMessage(ALBUM_ART_DECODED, null);
                    mHandler.removeMessages(ALBUM_ART_DECODED);
                    mHandler.sendMessage(numsg);
                }
                /* @} */
                mAlbumId = albumid;
            }
        }
    }
    
    private static class Worker implements Runnable {
        private final Object mLock = new Object();
        private Looper mLooper;
        
        /**
         * Creates a worker thread with the given name. The thread
         * then runs a {@link android.os.Looper}.
         * @param name A name for the new thread
         */
        Worker(String name) {
            Thread t = new Thread(null, this, name);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
            synchronized (mLock) {
                while (mLooper == null) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }
        
        public Looper getLooper() {
            return mLooper;
        }
        
        public void run() {
            synchronized (mLock) {
                Looper.prepare();
                mLooper = Looper.myLooper();
                mLock.notifyAll();
            }
            Looper.loop();
        }
        
        public void quit() {
            mLooper.quit();
        }
    }
    /* SPRD 476974 @{ */
    public void onConfigurationChanged(Configuration newConfig) {
        boolean lycIsScroll = isLyricVisiable();
        super.onConfigurationChanged(newConfig);
        initViews();
        updateButton();
        setLrc(mCurrentLrcPath);
        //SPRD bug fix 509392
        //mList.setShowType(lycIsScroll ? LyricListView.SHOW_TYPE_SHOW_AWALAYS :
                //LyricListView.SHOW_TYPE_HIDE_AWALAYS);
        updateTrackInfo();
        Message numsg = mHandler.obtainMessage(ALBUM_ART_DECODED, mAlbumBm);
        mHandler.removeMessages(ALBUM_ART_DECODED);
        mHandler.sendMessage(numsg);
        long next = refreshNow();
        queueNextRefresh(next);
        mPosOverride = -1;
        invalidateOptionsMenu();
    }

    private void updateButton() {
        mRepeatButton.setVisibility(View.VISIBLE);
        mShuffleButton.setVisibility(View.VISIBLE);
        mQueueButton.setVisibility(View.VISIBLE);
        setRepeatButtonImage();
        setShuffleButtonImage();
        setPauseButtonImage();
    }

    private void initViews() {
        initlrcViews();
        mCurrentTime = (TextView) findViewById(R.id.currenttime);
        mTotalTime = (TextView) findViewById(R.id.totaltime);
        mProgress = (ProgressBar) findViewById(android.R.id.progress);
        mAlbum = (ImageView) findViewById(R.id.album);
        mArtistName = (TextView) findViewById(R.id.artistname);
        mTrackName = (TextView) findViewById(R.id.trackname);
        View v;
        v = (View) mAlbumName.getParent();
        v.setOnTouchListener(this);
        v.setOnLongClickListener(this);

        v = (View) mArtistName.getParent();
        v.setOnTouchListener(this);
        v.setOnLongClickListener(this);

        v = (View) mTrackName.getParent();
        v.setOnTouchListener(this);
        v.setOnLongClickListener(this);

        mPrevButton = (RepeatingImageButton) findViewById(R.id.prev);
        mPrevButton.setOnClickListener(mPrevListener);
        mPrevButton.setRepeatListener(mRewListener, 260);
        mPauseButton = (ImageButton) findViewById(R.id.pause);
        mPauseButton.requestFocus();
        mPauseButton.setOnClickListener(mPauseListener);
        mNextButton = (RepeatingImageButton) findViewById(R.id.next);
        mNextButton.setOnClickListener(mNextListener);
        mNextButton.setRepeatListener(mFfwdListener, 260);
        seekmethod = 1;

        mDeviceHasDpad = (getResources().getConfiguration().navigation ==
                Configuration.NAVIGATION_DPAD);

        mQueueButton = (ImageButton) findViewById(R.id.curplaylist);
        mQueueButton.setOnClickListener(mQueueListener);
        mShuffleButton = ((ImageButton) findViewById(R.id.shuffle));
        mShuffleButton.setOnClickListener(mShuffleListener);
        mRepeatButton = ((ImageButton) findViewById(R.id.repeat));
        mRepeatButton.setOnClickListener(mRepeatListener);

        if (mProgress instanceof SeekBar) {
            SeekBar seeker = (SeekBar) mProgress;
            seeker.setOnSeekBarChangeListener(mSeekListener);
        }
        mProgress.setMax(1000);
    }

    private void initlrcViews() {
        mLrcArtistName = (TextView) findViewById(R.id.lrc_artistname);
        mLrcTrackName = (TextView) findViewById(R.id.lrc_trackname);
        mArtistName = (TextView) findViewById(R.id.artistname);
        mAlbumName = (TextView) findViewById(R.id.albumname);
        mTrackName = (TextView) findViewById(R.id.trackname);
        mLyricIcon = (ImageView) findViewById(R.id.lyric_expend_icon);
        mTrack = (LinearLayout) findViewById(R.id.track);
        mTrackInfo = (RelativeLayout) findViewById(R.id.track_info);
        if (mLyricIcon != null) {
            mLyricIcon.setOnClickListener(toggoleLyricOnClickListener);
        }
        mLyricContainer = (LinearLayout) findViewById(R.id.lyric_container);
        mList = (LyricListView) findViewById(R.id.lyric_list);
        mList.setCacheColorHint(0);
        mList.setPositionProvider(this);
        mNolrcNotifier = (TextView) findViewById(R.id.nolrc_notifier);
        //SPRD bug fix 509392
        mList.setShowType(mShowType);
        //SPRD:494107
        /*if (mShowType == LyricListView.SHOW_TYPE_HIDE_AWALAYS) {
            setLycicVisiable(false);
            //SPRD bug fix 509392
            //mList.setShowType(LyricListView.SHOW_TYPE_HIDE_AWALAYS);
        } else if (mShowType == LyricListView.SHOW_TYPE_SHOW_AWALAYS) {
            setLycicVisiable(true);
            //SPRD bug fix 509392
            //mList.setShowType(LyricListView.SHOW_TYPE_SHOW_AWALAYS);
        }*/
    }

    public long getDuration() {
        long duration = 0;
        try {
            duration = mService.duration();
        } catch (Exception e) {

        }
        return duration;
    }

    public long getPosition() {
        long position = 0;
        try {
            position = mService.position();
        } catch (Exception e) {

        }
        return position;
    }

    private boolean isLyricVisiable() {
        return mLyricContainer.getVisibility() == View.VISIBLE;
    }

    private boolean isShowingLrcLastSong = false;
    private String mCurrentLrcPath = null;

    private void updateLyrics(long lastId) {
        isShowingLrcLastSong = isLyricVisiable();
        boolean changeAudioId = false;
        if (mLastAudioId != lastId) {
            changeAudioId = true;
            mLastAudioId = lastId;
            mCurrentLrcPath = getLyricPath();
            setLrc(mCurrentLrcPath);
            mLrc_Visible = false;
            // if change songs, set the lyc to show default.
            //SPRD:494107
            //mList.setShowType(LyricListView.SHOW_TYPE_DEFAULT);
        }

        mHandler.removeMessages(HIDE_LYC_SHOW_TRACK);
        boolean haveLyc = mList.isHaveLyc();
        int showType = mList.getShowType();
        // only call is changing to next song and the next song don't have lyc
        // but pre song is have.
        /* SPRD remove for 509392 @{ */
        if (!haveLyc) {
            mList.stop();
            if (mLrc_Visible && showType == LyricListView.SHOW_TYPE_SHOW_AWALAYS) {
                setLycicVisiable(true);
            } else {
                setLycicVisiable(false);
            }
            //mHandler.sendEmptyMessageDelayed(HIDE_LYC_SHOW_TRACK, 500);
            return;
        }
        /* @} */

        /*if (haveLyc) {
            mList.start();
            setLycicVisiable(true);
        } else {
            mList.stop();
            setLycicVisiable(false);
        }
         SPRD remove for 498300 @{
         SPRD 499639 @{
        if (showType == LyricListView.SHOW_TYPE_HIDE_AWALAYS && !mIsFirst) {
            setLycicVisiable(false);
        //SPRD bug fix 509392
        } else if (showType == LyricListView.SHOW_TYPE_SHOW_AWALAYS && mLrc_Visible) {
            setLycicVisiable(true);
        }*/
        /* @} */
        /* SPRD add for 509392 @{ */
        if (showType == LyricListView.SHOW_TYPE_HIDE_AWALAYS) {
            mList.stop();
            setLycicVisiable(false);
        } else {
            mList.start();
            setLycicVisiable(true);
        }
        /* @} */
    }

    private boolean setLrc(String lrcPath) {
        boolean ready = false;
        LRC lrc = null;
        try {
            lrc = LRCParser.parseFromFile(lrcPath, getCacheDir().toString());
            ready = true;
        } catch (Exception e) {
            MusicLog.e(LOGTAG, "SET LYRIC ERROR:" + e.getMessage());
        }
        mList.setLrc(lrcPath, lrc);
        return ready;
    }

    private void playLrc(String lrcPath) {
        boolean ready = true;
        ready = setLrc(lrcPath);
        if (ready) {
            mList.start();
        }
    }

    private String getLyricPath() {
        String path = null;
        try {
            path = mService.getPath();
        } catch (RemoteException e) {
            MusicLog.e(LOGTAG, "GET LRC PATH REMOTE ERROR:" + e.getMessage());
        }
        Log.i("wangkui","getLyricPath path:"+ path);
        return SPRDMusicUtils.getLrcPath(path, getContentResolver());
    }

    private String getTrackFileName() {
        String path = null;
        try {
            path = mService.getPath();
        } catch (RemoteException e) {
            MusicLog.e(LOGTAG, "GET TRACK FILE NAME REMOTE ERROR:" + e.getMessage());
        }
        return SPRDMusicUtils.getTrackFileName(path, getContentResolver());
    }

    private View.OnClickListener toggoleLyricOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            /* Add for bug 509765 @{*/
            if (mCurrentLrcPath != null) {
                setLrc(mCurrentLrcPath);
            }
            /* @} */

            int vis = mLyricContainer.getVisibility();
            if (vis == View.VISIBLE) {
                mList.pause();
                setLycicVisiable(false);
                mList.setShowType(LyricListView.SHOW_TYPE_HIDE_AWALAYS);
            } else {
                mList.start();
                setLycicVisiable(true);
                mList.setShowType(LyricListView.SHOW_TYPE_SHOW_AWALAYS);
            }
        }
    };

    private void setLycicVisiable(boolean visiable) {
        if (visiable) {
            mLyricContainer.setVisibility(View.VISIBLE);
            // SPRD 499639
            //SPRD bug fix 509392
            //mList.setShowType(LyricListView.SHOW_TYPE_SHOW_AWALAYS);
            if (!mList.isHaveLyc()) {
                mList.setVisibility(View.GONE);
                mNolrcNotifier.setVisibility(View.VISIBLE);
            } else {
                mList.setVisibility(View.VISIBLE);
                mNolrcNotifier.setVisibility(View.GONE);
            }
            mTrack.setVisibility(View.GONE);
            if (mLyricIcon != null) {
                mLyricIcon.setImageResource(R.drawable.lyric_display);
            }
        } else {
            // SPRD 499639
            //SPRD bug fix 509392
            //mList.setShowType(LyricListView.SHOW_TYPE_HIDE_AWALAYS);
            mLyricContainer.setVisibility(View.GONE);
            mTrack.setVisibility(View.VISIBLE);
            if (mLyricIcon != null) {
                mLyricIcon.setImageResource(R.drawable.lyric_hide);
            }
        }
    }
    /* @} */
    /* SPRD:494107 @{ */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        super.onSaveInstanceState(outState);
        outState.putInt(LRC_STATE, mList.getShowType());
        //SPRD bug fix 509392
        outState.putBoolean(LRC_VISIBILITY, isLyricVisiable());
    }
    /* @} */

    /* SPRD 512745 @{ */
    @Override
    public void onBackPressed() {
        if (isResumed()) {
            super.onBackPressed();
        }
    }
    /* @} */
}

