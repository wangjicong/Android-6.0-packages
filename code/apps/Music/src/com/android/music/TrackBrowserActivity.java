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
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.AbstractCursor;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Playlists;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AlphabetIndexer;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.SearchableInfo;
import android.widget.SearchView;

import com.sprd.music.utils.SPRDMusicUtils;
import com.sprd.music.drm.*;
import com.sprd.music.plugin.*;
/* SPRD bug fix 519898 need check runtime permisssion before use @{ */
import android.content.pm.PackageManager;
import android.widget.Toast;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
/* @} */
//jxl add for share music 20160107
import android.content.ActivityNotFoundException;
import java.io.File;
import android.widget.Toast;
import com.sprd.android.config.OptConfig;

public class TrackBrowserActivity extends ListActivity
        implements View.OnCreateContextMenuListener, MusicUtils.Defs, ServiceConnection
{
    private static final int Q_SELECTED = CHILD_MENU_BASE;
    private static final int Q_ALL = CHILD_MENU_BASE + 1;
    private static final int SAVE_AS_PLAYLIST = CHILD_MENU_BASE + 2;
    private static final int PLAY_ALL = CHILD_MENU_BASE + 3;
    private static final int CLEAR_PLAYLIST = CHILD_MENU_BASE + 4;
    private static final int REMOVE = CHILD_MENU_BASE + 5;
    private static final int SEARCH = CHILD_MENU_BASE + 6;
    /* SPRD 476975 @{ */
    private static final int ADD_MUSIC = CHILD_MENU_BASE +7;
    private static final int DELETE_ALL_MUSIC = CHILD_MENU_BASE + 8;
    /* @} */
	//jxl add for share music 20160107
    private static final int SHARE_MUSIC = CHILD_MENU_BASE + 10;


    private static final String LOGTAG = "TrackBrowser";

    private String[] mCursorCols;
    private String[] mPlaylistMemberCols;
    private boolean mDeletedOneRow = false;
    private boolean mEditMode = false;
    private String mCurrentTrackName;
    private String mCurrentAlbumName;
    private String mCurrentArtistNameForAlbum;
    private ListView mTrackList;
    private Cursor mTrackCursor;
    private TrackListAdapter mAdapter;
    private boolean mAdapterSent = false;
    private String mAlbumId;
    private String mArtistId;
    private String mPlaylist;
    private String mGenre;
    private String mSortOrder;
    private int mSelectedPosition;
    private long mSelectedId;
    private static int mLastListPosCourse = -1;
    private static int mLastListPosFine = -1;
    private boolean mUseLastListPos = false;
    private ServiceToken mToken;
    /* SPRD 476969 @{ */
    private MenuItem mSearchItem;
    private SearchView mSearchView;
    /* @} */
    /* SPRD 475999 @{ */
    private static boolean mIsShowAlbumBg = false;
    private final int TRACK_ACTIONBAR_ELEVATION_EIGHT = 8;
    private final int TRACK_ACTIONBAR_ELEVATION_ZERO = 0;
    private final String TITLE_IS_SHOW_ALBUMBG = "is_show_albumbg";
    /* @} */
    //SPRD:add for bug 510548 save mIsShowAlbumBg in own activity
    private boolean mSaveIsShowAlbumBg;
    /* SPRD 476972 @{ */
    private String mStrNewAdded;
    private boolean mIsPlayAll;
    /* @} */
    // SPRD 523924 add flag for showing quit menuitem
    private boolean mNeedShowQuitMenuitem = false;
	//jxl add for share music 20160107
	private static final String AUDIO_SHARE_TYPE = "audio/*";
		

    public TrackBrowserActivity()
    {
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        Log.i(LOGTAG,"onCreate");

        if(RequestPermissionsActivity.startPermissionActivity(this)){
            return;
        }

        //SPRD 476978
        MusicDRM.getInstance().initDRM(TrackBrowserActivity.this);
        Log.d("DRM", "after call  MusicDRM.getInstance().initDRM");
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        /* SPRD 475999 @{ */
        ActionBar actionbar = getActionBar();
        actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
        actionbar.setDisplayUseLogoEnabled(false);
        actionbar.setDisplayShowHomeEnabled(false);
        actionbar.setElevation(TRACK_ACTIONBAR_ELEVATION_EIGHT);
        /* @} */
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.getBooleanExtra("withtabs", false)) {
                /* SPRD 475999 @{ */
                // SPRD 520090 music title translation error
                actionbar.setTitle(getResources().getString(R.string.musicbrowserlabel));
                actionbar.setElevation(TRACK_ACTIONBAR_ELEVATION_ZERO);
                /* @} */
            }
            Log.i(LOGTAG,"intent.getAction() = " +intent.getAction());
        }
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        if (icicle != null) {
            mSelectedId = icicle.getLong("selectedtrack");
            mAlbumId = icicle.getString("album");
            mArtistId = icicle.getString("artist");
            mPlaylist = icicle.getString("playlist");
            mGenre = icicle.getString("genre");
            /* SPRD 476972 @{ */
            mStrNewAdded = icicle.getString("newplaylist");
            mEditMode = icicle.getBoolean("editmode", false);
            // SPRD 475999
            mIsShowAlbumBg = icicle.getBoolean(TITLE_IS_SHOW_ALBUMBG, false);
        } else {
            mAlbumId = intent.getStringExtra("album");
            // If we have an album, show everything on the album, not just stuff
            // by a particular artist.
            mArtistId = intent.getStringExtra("artist");
            mPlaylist = intent.getStringExtra("playlist");
            /* SPRD 476972 @{ */
            mStrNewAdded = intent.getStringExtra("newplaylist");
            mGenre = intent.getStringExtra("genre");
            /* SPRD 499645 @{ */
            //mEditMode = intent.getAction().equals(Intent.ACTION_EDIT);
            mEditMode = Intent.ACTION_EDIT.equals(intent.getAction());
            /* @} */
            // SPRD 475999
            mIsShowAlbumBg = false;
        }

        mCursorCols = new String[] {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.DURATION
        };
        mPlaylistMemberCols = new String[] {
                MediaStore.Audio.Playlists.Members._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Playlists.Members.PLAY_ORDER,
                MediaStore.Audio.Playlists.Members.AUDIO_ID,
                MediaStore.Audio.Media.IS_MUSIC
        };

        setContentView(R.layout.media_picker_activity);
        mUseLastListPos = MusicUtils.updateButtonBar(this, R.id.songtab);
        // SPRD 523924 get flag whether tab show
        mNeedShowQuitMenuitem = mUseLastListPos;

        mTrackList = getListView();
        mTrackList.setOnCreateContextMenuListener(this);
        mTrackList.setCacheColorHint(0);
        if (mEditMode) {
            ((TouchInterceptor) mTrackList).setDropListener(mDropListener);
            ((TouchInterceptor) mTrackList).setRemoveListener(mRemoveListener);
            mTrackList.setDivider(null);
            mTrackList.setSelector(R.drawable.list_selector_background);
        } else {
            mTrackList.setTextFilterEnabled(true);
        }
        mAdapter = (TrackListAdapter) getLastNonConfigurationInstance();
        
        if (mAdapter != null) {
            mAdapter.setActivity(this);
            /* SPRD 476972 @{ */
            mAdapter.reloadStringOnLocaleChanges();
            setListAdapter(mAdapter);
        }
        mToken = MusicUtils.bindToService(this, this);
        // don't set the album art until after the view has been layed out
        mTrackList.post(new Runnable() {

            public void run() {
                setAlbumArtBackground();
            }
        });
    }

    public void onServiceConnected(ComponentName name, IBinder service)
    {
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        /* SPRD 476972 @{ */
        f.addAction(Intent.ACTION_MEDIA_SHARED);
        f.addDataScheme("file");
        registerReceiver(mScanListener, f);

        if (mAdapter == null) {
            //Log.i("@@@", "starting query");
            mAdapter = new TrackListAdapter(
                    getApplication(), // need to use application context to avoid leaks
                    this,
                    mEditMode ? R.layout.edit_track_list_item : R.layout.track_list_item,
                    null, // cursor
                    new String[] {},
                    new int[] {},
                    "nowplaying".equals(mPlaylist),
                    mPlaylist != null &&
                    /* SPRD 476972 @{ */
                    !(mPlaylist.equals("podcasts") || mPlaylist.equals("recentlyadded")
                    ||(mStrNewAdded != null && mStrNewAdded.equals("newAdded"))));
                    /* @} */
            setListAdapter(mAdapter);
            /* SPRD 476972 @{ */
            setTitle(R.string.tracks_title);
            getTrackCursor(mAdapter.getQueryHandler(), null, true);
        } else {
            mTrackCursor = mAdapter.getCursor();
            // If mTrackCursor is null, this can be because it doesn't have
            // a cursor yet (because the initial query that sets its cursor
            // is still in progress), or because the query failed.
            // In order to not flash the error dialog at the user for the
            // first case, simply retry the query when the cursor is null.
            // Worst case, we end up doing the same query twice.
            if (mTrackCursor != null) {
                init(mTrackCursor, false);
            } else {
                /* SPRD 476972 @{ */
                setTitle(R.string.tracks_title);
                getTrackCursor(mAdapter.getQueryHandler(), null, true);
            }
        }
        if (!mEditMode) {
            MusicUtils.updateNowPlaying(this);
        }
    }
    
    public void onServiceDisconnected(ComponentName name) {
        // we can't really function without the service, so don't
        finish();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        TrackListAdapter a = mAdapter;
        mAdapterSent = true;
        return a;
    }
    
    @Override
    public void onDestroy() {
        Log.i(LOGTAG,"onDestroy");
        ListView lv = getListView();
        if (lv != null) {
            if (mUseLastListPos) {
                mLastListPosCourse = lv.getFirstVisiblePosition();
                View cv = lv.getChildAt(0);
                if (cv != null) {
                    mLastListPosFine = cv.getTop();
                }
            }
            if (mEditMode) {
                // clear the listeners so we won't get any more callbacks
                ((TouchInterceptor) lv).setDropListener(null);
                ((TouchInterceptor) lv).setRemoveListener(null);
            }
        }
        // SPRD 524518
        MusicUtils.unbindFromService(mToken, this);
        try {
            if ("nowplaying".equals(mPlaylist)) {
                unregisterReceiverSafe(mNowPlayingListener);
            } else {
                unregisterReceiverSafe(mTrackListListener);
            }
        } catch (IllegalArgumentException ex) {
            // we end up here in case we never registered the listeners
        }
        
        // If we have an adapter and didn't send it off to another activity yet, we should
        // close its cursor, which we do by assigning a null cursor to it. Doing this
        // instead of closing the cursor directly keeps the framework from accessing
        // the closed cursor later.
        if (!mAdapterSent && mAdapter != null) {
            /* SPRD 476972 @{ */
            if (mTrackCursor != null) {
                mTrackCursor.close();
            }
            mAdapter.changeCursor(null);
            /* @} */
        }
        // Because we pass the adapter to the next activity, we need to make
        // sure it doesn't keep a reference to this activity. We can do this
        // by clearing its DatasetObservers, which setListAdapter(null) does.
        if(lv != null){
            setListAdapter(null);
        }
        mAdapter = null;
        unregisterReceiverSafe(mScanListener);
        // SPRD 476978
        MusicDRM.getInstance().destroyDRM();
        super.onDestroy();
    }
    
    /**
     * Unregister a receiver, but eat the exception that is thrown if the
     * receiver was never registered to begin with. This is a little easier
     * than keeping track of whether the receivers have actually been
     * registered by the time onDestroy() is called.
     */
    private void unregisterReceiverSafe(BroadcastReceiver receiver) {
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.i(LOGTAG,"onResume");
        //SPRD:add for bug 510548 save mIsShowAlbumBg in own activity
        mIsShowAlbumBg = mSaveIsShowAlbumBg;
        if (mTrackCursor != null) {
            ((TrackListAdapter)getListAdapter()).notifyDataSetChanged();
            getListView().invalidateViews();
        }

        MusicUtils.setSpinnerState(this);
    }
    @Override
    public void onPause() {
        Log.i(LOGTAG,"onPause");
        //SPRD:add for bug 510548 save mIsShowAlbumBg in own activity
        mSaveIsShowAlbumBg = mIsShowAlbumBg;
        mReScanHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    /*
     * This listener gets called when the media scanner starts up or finishes, and
     * when the sd card is unmounted.
     */
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_MEDIA_SCANNER_STARTED.equals(action) ||
                    Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)) {
                MusicUtils.setSpinnerState(TrackBrowserActivity.this);
            }
            mReScanHandler.sendEmptyMessage(0);
        }
    };
    
    private Handler mReScanHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mAdapter != null) {
                getTrackCursor(mAdapter.getQueryHandler(), null, true);
            }
            // if the query results in a null cursor, onQueryComplete() will
            // call init(), which will post a delayed message to this handler
            // in order to try again.
        }
    };
    
    public void onSaveInstanceState(Bundle outcicle) {
        // need to store the selected item so we don't lose it in case
        // of an orientation switch. Otherwise we could lose it while
        // in the middle of specifying a playlist to add the item to.
        outcicle.putLong("selectedtrack", mSelectedId);
        outcicle.putString("artist", mArtistId);
        outcicle.putString("album", mAlbumId);
        outcicle.putString("playlist", mPlaylist);
        outcicle.putString("genre", mGenre);
        /* SPRD 476972 @{ */
        outcicle.putString("newplaylist", mStrNewAdded);
        outcicle.putBoolean("editmode", mEditMode);
        // SPRD 475999
        outcicle.putBoolean(TITLE_IS_SHOW_ALBUMBG, mIsShowAlbumBg);
        super.onSaveInstanceState(outcicle);
    }
    
    public void init(Cursor newCursor, boolean isLimited) {
        Log.i(LOGTAG,"init");
        if (mAdapter == null) {
            return;
        }
        mAdapter.changeCursor(newCursor); // also sets mTrackCursor
        
        if (mTrackCursor == null) {
            MusicUtils.displayDatabaseError(this);
            closeContextMenu();
            mReScanHandler.sendEmptyMessageDelayed(0, 1000);
            return;
        }

        MusicUtils.hideDatabaseError(this);
        mUseLastListPos = MusicUtils.updateButtonBar(this, R.id.songtab);
        // SPRD 523924 get flag whether tab show
        mNeedShowQuitMenuitem = mUseLastListPos;

        setTitle();

        // Restore previous position
        if (mLastListPosCourse >= 0 && mUseLastListPos) {
            ListView lv = getListView();
            // this hack is needed because otherwise the position doesn't change
            // for the 2nd (non-limited) cursor
            lv.setAdapter(lv.getAdapter());
            lv.setSelectionFromTop(mLastListPosCourse, mLastListPosFine);
            if (!isLimited) {
                mLastListPosCourse = -1;
            }
        }

        // When showing the queue, position the selection on the currently playing track
        // Otherwise, position the selection on the first matching artist, if any
        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.META_CHANGED);
        f.addAction(MediaPlaybackService.QUEUE_CHANGED);
        if ("nowplaying".equals(mPlaylist)) {
            try {
                int cur = MusicUtils.sService.getQueuePosition();
                setSelection(cur);
                registerReceiver(mNowPlayingListener, new IntentFilter(f));
                mNowPlayingListener.onReceive(this, new Intent(MediaPlaybackService.META_CHANGED));
            } catch (RemoteException ex) {
            }
        } else {
            String key = getIntent().getStringExtra("artist");
            if (key != null) {
                int keyidx = mTrackCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID);
                mTrackCursor.moveToFirst();
                while (! mTrackCursor.isAfterLast()) {
                    String artist = mTrackCursor.getString(keyidx);
                    if (artist.equals(key)) {
                        setSelection(mTrackCursor.getPosition());
                        break;
                    }
                    mTrackCursor.moveToNext();
                }
            }
            registerReceiver(mTrackListListener, new IntentFilter(f));
            mTrackListListener.onReceive(this, new Intent(MediaPlaybackService.META_CHANGED));
        }
    }

    private void setAlbumArtBackground() {
        if (!mEditMode) {
            try {
                long albumid = Long.valueOf(mAlbumId);
                Bitmap bm = MusicUtils.getArtwork(TrackBrowserActivity.this, -1, albumid, false);
                if (bm != null) {
                    MusicUtils.setBackground(mTrackList, bm);
                    mTrackList.setCacheColorHint(0);
                    // SPRD 475999
                    mIsShowAlbumBg = true;
                    return;
                } else {
                    //SPRD 475999
                    mIsShowAlbumBg = false;
                }
            } catch (Exception ex) {
            }
        }
        // SPRD 475999
        //mTrackList.setBackgroundColor(0xff000000);
        mTrackList.setCacheColorHint(0);
    }

    private void setTitle() {

        CharSequence fancyName = null;
        if (mAlbumId != null) {
            int numresults = mTrackCursor != null ? mTrackCursor.getCount() : 0;
            if (numresults > 0) {
                mTrackCursor.moveToFirst();
                int idx = mTrackCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                fancyName = mTrackCursor.getString(idx);
                // For compilation albums show only the album title,
                // but for regular albums show "artist - album".
                // To determine whether something is a compilation
                // album, do a query for the artist + album of the
                // first item, and see if it returns the same number
                // of results as the album query.
                String where = MediaStore.Audio.Media.ALBUM_ID + "='" + mAlbumId +
                        "' AND " + MediaStore.Audio.Media.ARTIST_ID + "=" + 
                        mTrackCursor.getLong(mTrackCursor.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.ARTIST_ID));
                Cursor cursor = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[] {MediaStore.Audio.Media.ALBUM}, where, null, null);
                if (cursor != null) {
                    if (cursor.getCount() != numresults) {
                        // compilation album
                        fancyName = mTrackCursor.getString(idx);
                    }    
                    cursor.deactivate();
                    /* SPRD 476972 @{ */
                    cursor.close();
                }
                if (fancyName == null || fancyName.equals(MediaStore.UNKNOWN_STRING)) {
                    fancyName = getString(R.string.unknown_album_name);
                }
            }
        } else if (mPlaylist != null) {
            if (mPlaylist.equals("nowplaying")) {
                if (MusicUtils.getCurrentShuffleMode() == MediaPlaybackService.SHUFFLE_AUTO) {
                    fancyName = getText(R.string.partyshuffle_title);
                } else {
                    fancyName = getText(R.string.nowplaying_title);
                }
            } else if (mPlaylist.equals("podcasts")){
                fancyName = getText(R.string.podcasts_title);
            } else if (mPlaylist.equals("recentlyadded")){
                fancyName = getText(R.string.recentlyadded_title);
            } else {
                String [] cols = new String [] {
                MediaStore.Audio.Playlists.NAME
                };
                Cursor cursor = MusicUtils.query(this,
                        ContentUris.withAppendedId(Playlists.EXTERNAL_CONTENT_URI, Long.valueOf(mPlaylist)),
                        cols, null, null, null);
                if (cursor != null) {
                    if (cursor.getCount() != 0) {
                        cursor.moveToFirst();
                        fancyName = cursor.getString(0);
                    }
                    cursor.deactivate();
                    /* SPRD 476972 @{ */
                    cursor.close();
                }
            }
        } else if (mGenre != null) {
            String [] cols = new String [] {
            MediaStore.Audio.Genres.NAME
            };
            Cursor cursor = MusicUtils.query(this,
                    ContentUris.withAppendedId(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, Long.valueOf(mGenre)),
                    cols, null, null, null);
            if (cursor != null) {
                if (cursor.getCount() != 0) {
                    cursor.moveToFirst();
                    fancyName = cursor.getString(0);
                }
                cursor.deactivate();
                /* SPRD 476972 @{ */
                cursor.close();
            }
        }

        if (fancyName != null) {
            setTitle(fancyName);
        } else {
            /* SPRD 476972 @{ */
            if (mIsPlayAll) {
                setTitle(R.string.play_all);
            } else {
                setTitle(R.string.tracks_title);
            }
            /* @} */
        }
    }
    
    private TouchInterceptor.DropListener mDropListener =
        new TouchInterceptor.DropListener() {
        public void drop(int from, int to) {
            Log.i(LOGTAG,"DropListener drop");
            if (mTrackCursor instanceof NowPlayingCursor) {
                // update the currently playing list
                NowPlayingCursor c = (NowPlayingCursor) mTrackCursor;
                c.moveItem(from, to);
                ((TrackListAdapter)getListAdapter()).notifyDataSetChanged();
                getListView().invalidateViews();
                mDeletedOneRow = true;
            } else {
                // update a saved playlist
                MediaStore.Audio.Playlists.Members.moveItem(getContentResolver(),
                        Long.valueOf(mPlaylist), getPlayOrder(from), getPlayOrder(to));
            }
        }
    };
    
    /* SPRD: Modify for bug 523319 update PlayOrder */
    private int getPlayOrder(int index) {
        try {
            int column = mTrackCursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.PLAY_ORDER);
            mTrackCursor.moveToPosition(index);
            return mTrackCursor.getInt(column);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    private TouchInterceptor.RemoveListener mRemoveListener =
        new TouchInterceptor.RemoveListener() {
        public void remove(int which) {
            Log.i(LOGTAG,"RemoveListener remove which:"+which);
            removePlaylistItem(which);
        }
    };

    private void removePlaylistItem(int which) {
        Log.i(LOGTAG,"removePlaylistItem which:"+which);
        View v = mTrackList.getChildAt(which - mTrackList.getFirstVisiblePosition());
        if (v == null) {
            Log.d(LOGTAG, "No view when removing playlist item " + which);
            return;
        }
        try {
            if (MusicUtils.sService != null
                    && which != MusicUtils.sService.getQueuePosition()) {
                mDeletedOneRow = true;
            }
        } catch (RemoteException e) {
            // Service died, so nothing playing.
            mDeletedOneRow = true;
        }
        v.setVisibility(View.GONE);
        ((TrackListAdapter)getListAdapter()).notifyDataSetChanged();
        mTrackList.invalidateViews();
        if (mTrackCursor instanceof NowPlayingCursor) {
            ((NowPlayingCursor)mTrackCursor).removeItem(which);
        } else {
            int colidx = mTrackCursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Playlists.Members._ID);
            mTrackCursor.moveToPosition(which);
            long id = mTrackCursor.getLong(colidx);
            Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external",
                    Long.valueOf(mPlaylist));
            getContentResolver().delete(
                    ContentUris.withAppendedId(uri, id), null, null);
        }
        v.setVisibility(View.VISIBLE);
        ((TrackListAdapter)getListAdapter()).notifyDataSetChanged();
        mTrackList.invalidateViews();
    }
    
    private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ((TrackListAdapter)getListAdapter()).notifyDataSetChanged();
            getListView().invalidateViews();
            if (!mEditMode) {
                MusicUtils.updateNowPlaying(TrackBrowserActivity.this);
            }
        }
    };

    private BroadcastReceiver mNowPlayingListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MediaPlaybackService.META_CHANGED)) {
                ((TrackListAdapter)getListAdapter()).notifyDataSetChanged();
                getListView().invalidateViews();
                /* SPRD 476972 @{ */
                if (!mEditMode) {
                    MusicUtils.updateNowPlaying(TrackBrowserActivity.this);
                }
                /* @} */
            } else if (intent.getAction().equals(MediaPlaybackService.QUEUE_CHANGED)) {
                if (mDeletedOneRow) {
                    // This is the notification for a single row that was
                    // deleted previously, which is already reflected in
                    // the UI.
                    mDeletedOneRow = false;
                    return;
                }
                // The service could disappear while the broadcast was in flight,
                // so check to see if it's still valid
                if (MusicUtils.sService == null) {
                    finish();
                    return;
                }
                if (mAdapter != null) {
                    Cursor c = new NowPlayingCursor(MusicUtils.sService, mCursorCols);
                    if (c.getCount() == 0) {
                        /* SPRD 524518 @{ */
                        c.close();
                        c = null;
                        /* @} */
                        finish();
                        return;
                    }
                    mAdapter.changeCursor(c);
                }
            }
        }
    };

    // Cursor should be positioned on the entry to be checked
    // Returns false if the entry matches the naming pattern used for recordings,
    // or if it is marked as not music in the database.
    private boolean isMusic(Cursor c) {
        int titleidx = c.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int albumidx = c.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int artistidx = c.getColumnIndex(MediaStore.Audio.Media.ARTIST);

        String title = c.getString(titleidx);
        String album = c.getString(albumidx);
        String artist = c.getString(artistidx);
        if (MediaStore.UNKNOWN_STRING.equals(album) &&
                MediaStore.UNKNOWN_STRING.equals(artist) &&
                title != null &&
                title.startsWith("recording")) {
            // not music
            return false;
        }

        int ismusic_idx = c.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC);
        boolean ismusic = true;
        if (ismusic_idx >= 0) {
            ismusic = mTrackCursor.getInt(ismusic_idx) != 0;
        }
        return ismusic;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfoIn) {
        menu.add(0, PLAY_SELECTION, 0, R.string.play_selection);
        SubMenu sub = menu.addSubMenu(0, ADD_TO_PLAYLIST, 0, R.string.add_to_playlist);
        MusicUtils.makePlaylistMenu(this, sub);
        if (mEditMode) {
            menu.add(0, REMOVE, 0, R.string.remove_from_playlist);
        }
        /* SPRD: Add for bug 540629 @{ */
        if (MusicUtils.isSystemUser(this)) {
            menu.add(0, USE_AS_RINGTONE, 0, R.string.ringtone_menu);
        }
        /* @} */
        menu.add(0, DELETE_ITEM, 0, R.string.delete_item);
		
		//if(OptConfig.SUNVOV_SUBCUSTOM_S7316_XLL_S51_WVGA){//jxl add for share music 20160107
        menu.add(0, SHARE_MUSIC, 0, R.string.menu_context_share_music);
		//}
        AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfoIn;
        mSelectedPosition =  mi.position;
        mTrackCursor.moveToPosition(mSelectedPosition);
        try {
            int id_idx = mTrackCursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Playlists.Members.AUDIO_ID);
            mSelectedId = mTrackCursor.getLong(id_idx);
        } catch (IllegalArgumentException ex) {
            mSelectedId = mi.id;
        }
        // only add the 'search' menu if the selected item is music
        if (isMusic(mTrackCursor)) {
            menu.add(0, SEARCH, 0, R.string.search_title);
        }
        mCurrentAlbumName = mTrackCursor.getString(mTrackCursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.ALBUM));
        mCurrentArtistNameForAlbum = mTrackCursor.getString(mTrackCursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.ARTIST));
        mCurrentTrackName = mTrackCursor.getString(mTrackCursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.TITLE));
        menu.setHeaderTitle(mCurrentTrackName);
        //SPRD 476978
        MusicDRM.getInstance().onCreateDRMTrackBrowserContextMenu(menu, mTrackCursor);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case PLAY_SELECTION: {
                // play the track
                int position = mSelectedPosition;
                /* SPRD 476978 @{ */
                if (MusicDRM.getInstance().isDRM(mTrackCursor,position)) {
                    MusicDRM.getInstance().onListItemClickDRM(TrackBrowserActivity.this, mTrackCursor, position);
                } else {
                    MusicUtils.playAll(this, mTrackCursor, position);
                }
                /* @} */
                return true;
            }

            case QUEUE: {
                long [] list = new long[] { mSelectedId };
                MusicUtils.addToCurrentPlaylist(this, list);
                return true;
            }

            case NEW_PLAYLIST: {
                Intent intent = new Intent();
                intent.setClass(this, CreatePlaylist.class);
                startActivityForResult(intent, NEW_PLAYLIST);
                return true;
            }

            case PLAYLIST_SELECTED: {
                long [] list = new long[] { mSelectedId };
                long playlist = item.getIntent().getLongExtra("playlist", 0);
                MusicUtils.addToPlaylist(this, list, playlist);
                return true;
            }

            case USE_AS_RINGTONE:
                // Set the system setting to make this the current ringtone
                //MusicUtils.setRingtone(this, mSelectedId);
                //SPRD 494136
                SPRDMusicUtils.doChoiceRingtone(this, mSelectedId);
                return true;

            case DELETE_ITEM: {
                long [] list = new long[1];
                list[0] = (int) mSelectedId;
                Bundle b = new Bundle();
                /* SPRD 476965 @{ */
                b.putString(MusicUtils.DeleteMode.CURRENT_TRACK_NAME, mCurrentTrackName);
                b.putInt(MusicUtils.DeleteMode.DELETE_MODE, MusicUtils.DeleteMode.DELETE_SONG);
                /* @} */
                b.putLongArray("items", list);
                Intent intent = new Intent();
                intent.setClass(this, DeleteItems.class);
                intent.putExtras(b);
                startActivityForResult(intent, -1);
                return true;
            }
            case SHARE_MUSIC:{//jxl add for share music 20160107
            	Intent intent = new Intent();
            	intent.setAction(Intent.ACTION_SEND);
            	String fileName = mTrackCursor.getString(mTrackCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
            	File file = new File(fileName);
            	final Uri uri = Uri.fromFile(file);
            	if(uri != null){
            		intent.putExtra(Intent.EXTRA_STREAM,uri);
            	}
            	intent.setType(AUDIO_SHARE_TYPE);
            	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            	intent = Intent.createChooser(intent, getText(R.string.menu_context_share_music));
            	try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(TrackBrowserActivity.this,getString(R.string.menu_context_share_error), Toast.LENGTH_SHORT).show();
			    }
            	return true;
            }
            case REMOVE:
                removePlaylistItem(mSelectedPosition);
                return true;
                
            case SEARCH:
                doSearch();
                return true;
        }
        //SPRD 476978
        MusicDRM.getInstance().onContextDRMTrackBrowserItemSelected(item,TrackBrowserActivity.this);
        return super.onContextItemSelected(item);
    }

    void doSearch() {
        CharSequence title = null;
        String query = null;
        
        Intent i = new Intent();
        i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        title = mCurrentTrackName;
        if (MediaStore.UNKNOWN_STRING.equals(mCurrentArtistNameForAlbum)) {
            query = mCurrentTrackName;
        } else {
            query = mCurrentArtistNameForAlbum + " " + mCurrentTrackName;
            i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, mCurrentArtistNameForAlbum);
        }
        if (MediaStore.UNKNOWN_STRING.equals(mCurrentAlbumName)) {
            i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, mCurrentAlbumName);
        }
        i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "audio/*");
        title = getString(R.string.mediasearch, title);
        i.putExtra(SearchManager.QUERY, query);

        startActivity(Intent.createChooser(i, title));
    }

    // In order to use alt-up/down as a shortcut for moving the selected item
    // in the list, we need to override dispatchKeyEvent, not onKeyDown.
    // (onKeyDown never sees these events, since they are handled by the list)
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        /* SPRD 476972 @{ */
        if(!this.isResumed()){
            return true;
        }
        /* @} */
        /*
         * SPRD: Modify for bug517192,,ListView(mTrackList) value change to Null because of rotate
         * the phone and Screen pinning{@
         */
        if (mTrackList == null){
            return false;
        }
        /* @} */
        int curpos = mTrackList.getSelectedItemPosition();
        if (mPlaylist != null && !mPlaylist.equals("recentlyadded") && curpos >= 0 &&
                event.getMetaState() != 0 && event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    moveItem(true);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    moveItem(false);
                    return true;
                case KeyEvent.KEYCODE_DEL:
                    removeItem();
                    return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    private void removeItem() {
        int curcount = mTrackCursor.getCount();
        int curpos = mTrackList.getSelectedItemPosition();
        if (curcount == 0 || curpos < 0) {
            return;
        }
        
        if ("nowplaying".equals(mPlaylist)) {
            // remove track from queue

            // Work around bug 902971. To get quick visual feedback
            // of the deletion of the item, hide the selected view.
            try {
                if (curpos != MusicUtils.sService.getQueuePosition()) {
                    mDeletedOneRow = true;
                }
            } catch (RemoteException ex) {
            }
            View v = mTrackList.getSelectedView();
            v.setVisibility(View.GONE);
            ((TrackListAdapter)getListAdapter()).notifyDataSetChanged();
            mTrackList.invalidateViews();
            ((NowPlayingCursor)mTrackCursor).removeItem(curpos);
            v.setVisibility(View.VISIBLE);
            ((TrackListAdapter)getListAdapter()).notifyDataSetChanged();
            mTrackList.invalidateViews();
        } else {
            // remove track from playlist
            int colidx = mTrackCursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Playlists.Members._ID);
            mTrackCursor.moveToPosition(curpos);
            long id = mTrackCursor.getLong(colidx);
            Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external",
                    Long.valueOf(mPlaylist));
            getContentResolver().delete(
                    ContentUris.withAppendedId(uri, id), null, null);
            curcount--;
            if (curcount == 0) {
                finish();
            } else {
                mTrackList.setSelection(curpos < curcount ? curpos : curcount);
            }
        }
    }
    
    private void moveItem(boolean up) {
        int curcount = mTrackCursor.getCount(); 
        int curpos = mTrackList.getSelectedItemPosition();
        if ( (up && curpos < 1) || (!up  && curpos >= curcount - 1)) {
            return;
        }

        if (mTrackCursor instanceof NowPlayingCursor) {
            NowPlayingCursor c = (NowPlayingCursor) mTrackCursor;
            c.moveItem(curpos, up ? curpos - 1 : curpos + 1);
            ((TrackListAdapter)getListAdapter()).notifyDataSetChanged();
            getListView().invalidateViews();
            mDeletedOneRow = true;
            if (up) {
                mTrackList.setSelection(curpos - 1);
            } else {
                mTrackList.setSelection(curpos + 1);
            }
        } else {
            int colidx = mTrackCursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Playlists.Members.PLAY_ORDER);
            mTrackCursor.moveToPosition(curpos);
            int currentplayidx = mTrackCursor.getInt(colidx);
            Uri baseUri = MediaStore.Audio.Playlists.Members.getContentUri("external",
                    Long.valueOf(mPlaylist));
            ContentValues values = new ContentValues();
            String where = MediaStore.Audio.Playlists.Members._ID + "=?";
            String [] wherearg = new String[1];
            ContentResolver res = getContentResolver();
            if (up) {
                values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, currentplayidx - 1);
                wherearg[0] = mTrackCursor.getString(0);
                res.update(baseUri, values, where, wherearg);
                mTrackCursor.moveToPrevious();
            } else {
                values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, currentplayidx + 1);
                wherearg[0] = mTrackCursor.getString(0);
                res.update(baseUri, values, where, wherearg);
                mTrackCursor.moveToNext();
            }
            values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, currentplayidx);
            wherearg[0] = mTrackCursor.getString(0);
            res.update(baseUri, values, where, wherearg);
        }
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        if (mTrackCursor.getCount() == 0) {
            return;
        }
        // When selecting a track from the queue, just jump there instead of
        // reloading the queue. This is both faster, and prevents accidentally
        // dropping out of party shuffle.
        if (mTrackCursor instanceof NowPlayingCursor) {
            if (MusicUtils.sService != null) {
                try {
                    MusicUtils.sService.setQueuePosition(position);
                    /* SPRD 476972 @{ */
                    if (!mEditMode) {
                        MusicUtils.updateNowPlaying(TrackBrowserActivity.this);
                    }
                    /* @} */
                    return;
                } catch (RemoteException ex) {
                }
            }
        }
        /* SPRD 476978 @{ */
        if(MusicDRM.getInstance().isDRM(mTrackCursor,position)) {
            MusicDRM.getInstance().onListItemClickDRM(TrackBrowserActivity.this,mTrackCursor,position);
        } else {
            MusicUtils.playAll(this, mTrackCursor, position);

            /* SPRD 530259 finish if start from search UI @{ */
            if(MusicUtils.isSearchResult(getIntent())){
               finish();
            }
            /* @} */
        }
        /* @} */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* This activity is used for a number of different browsing modes, and the menu can
         * be different for each of them:
         * - all tracks, optionally restricted to an album, artist or playlist
         * - the list of currently playing songs
         */
        super.onCreateOptionsMenu(menu);

        /* SPRD 476969 @{ */
        getMenuInflater().inflate(R.menu.options_menu_overlay, menu);
        mSearchItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) mSearchItem.getActionView();
        mSearchView.setQueryHint(getResources().getString(R.string.search_title_hint));
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
        if (mPlaylist == null) {
            menu.add(0, PLAY_ALL, 0, R.string.play_all).setIcon(R.drawable.ic_menu_play_clip);
        }
        menu.add(0, PARTY_SHUFFLE, 0, R.string.party_shuffle); // icon will be set in onPrepareOptionsMenu()
        menu.add(0, SHUFFLE_ALL, 0, R.string.shuffle_all).setIcon(R.drawable.ic_menu_shuffle);
        if (mPlaylist != null) {
            menu.add(0, SAVE_AS_PLAYLIST, 0, R.string.save_as_playlist).setIcon(android.R.drawable.ic_menu_save);
            /* SPRD 476975 @{ */
            if (!mPlaylist.equals("podcasts")&& !mPlaylist.equals("recentlyadded")&& !mPlaylist.equals("nowplaying")) {
                 menu.add(0, DELETE_ALL_MUSIC, 0, R.string.delete_all_music).setIcon(
                       R.drawable.ic_menu_clear_playlist);
            }
            /* @} */
            if (mPlaylist.equals("nowplaying")) {
                menu.add(0, CLEAR_PLAYLIST, 0, R.string.clear_playlist).setIcon(R.drawable.ic_menu_clear_playlist);
            }
        }
        // SPRD 476973
        menu.add(0, QUIT_MUSIC, 0, R.string.quit);
        /* SPRD bug fix 541220 @{ */
        if (AddMusicForCMCC.getInstance().isCMCCVersion()) {
            menu.add(0, ADD_MUSIC, 0, R.string.add_music).setIcon(android.R.drawable.ic_menu_add);
        }
        /* SPRD 476978 @{ */
        if(MusicUtils.isSearchResult(getIntent())){
            menu.removeItem(R.id.search);
        }
        /* @} */
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item;
        /* SPRD 476972 @{ */
        if(mPlaylist!=null && !mPlaylist.equals("nowplaying") && !mPlaylist.equals("podcasts")
                && !mPlaylist.equals("recentlyadded")){
            item = menu.findItem(DELETE_ALL_MUSIC);
            if(item != null) {
                if(mTrackCursor == null || mTrackCursor.getCount() == 0) {
                    item.setEnabled(false);
                } else {
                    item.setEnabled(true);
                }
            }
        }
        /* @} */
        MusicUtils.setPartyShuffleMenuIcon(menu);
        /* SPRD 476973 @{ */
        item = menu.findItem(QUIT_MUSIC);
        if (item != null) {
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        /* SPRD bug fix 521467 @{ */
        // SPRD 523924 check flag to remove menuitem of quit
        if(mEditMode || !mNeedShowQuitMenuitem){
            menu.removeItem(QUIT_MUSIC);
        }
        /* @} */
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* SPRD bug fix 519898 need check runtime permisssion before use @{ */
        if (checkSelfPermission(READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.error_permissions, Toast.LENGTH_SHORT).show();
            return true;
        }
        /* @} */
        Intent intent;
        Cursor cursor;
        switch (item.getItemId()) {
            case PLAY_ALL: {
                MusicUtils.playAll(this, mTrackCursor);
                /* SPRD 476972 @{ */
                if (mPlaylist != null && mPlaylist.equals("nowplaying")) {
                    finish();
                }
                /* @} */
                return true;
            }

            case PARTY_SHUFFLE:
                MusicUtils.togglePartyShuffle();
                break;
                
            case SHUFFLE_ALL:
                /* SPRD 476972 @{ */
                MusicUtils.shuffleAll(this, mTrackCursor);
                if (mPlaylist != null && mPlaylist.equals("nowplaying")) {
                    finish();
                }
                /* @} */
                return true;
                
            case SAVE_AS_PLAYLIST:
                intent = new Intent();
                intent.setClass(this, CreatePlaylist.class);
                startActivityForResult(intent, SAVE_AS_PLAYLIST);
                return true;
                
            case CLEAR_PLAYLIST:
                // We only clear the current playlist
                MusicUtils.clearQueue();
                /* SPRD 496677  @{ */
                setResult(RESULT_OK);
                /* @} */
                return true;
            /* SPRD 476973 @{ */
            case QUIT_MUSIC:
                SPRDMusicUtils.quitservice(this);
                return true;
            /* @} */
            /* SPRD 476975 @{ */
            case DELETE_ALL_MUSIC:
                Intent i = new Intent();
                i.setClass(TrackBrowserActivity.this, DelTrackChoiceActivity.class);
                i.putExtra("playlist", mPlaylist);
                TrackBrowserActivity.this.startActivity(i);
                return true;
            /* @} */
            /* SPRD bug fix 541220 @{ */
            case ADD_MUSIC:
                SPRDMusicUtils.doChoiceAddMusicDialog(this, mPlaylist);
                return true;
            /* @} */
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case SCAN_DONE:
                if (resultCode == RESULT_CANCELED) {
                    finish();
                } else {
                    getTrackCursor(mAdapter.getQueryHandler(), null, true);
                }
                break;
                
            case NEW_PLAYLIST:
                if (resultCode == RESULT_OK) {
                    Uri uri = intent.getData();
                    if (uri != null) {
                        long [] list = new long[] { mSelectedId };
                        MusicUtils.addToPlaylist(this, list, Integer.valueOf(uri.getLastPathSegment()));
                    }
                }
                break;

            case SAVE_AS_PLAYLIST:
                if (resultCode == RESULT_OK) {
                    Uri uri = intent.getData();
                    if (uri != null) {
                        long [] list = MusicUtils.getSongListForCursor(mTrackCursor);
                        int plid = Integer.parseInt(uri.getLastPathSegment());
                        MusicUtils.addToPlaylist(this, list, plid);
                    }
                }
                break;
        }
    }
    
    private Cursor getTrackCursor(TrackListAdapter.TrackQueryHandler queryhandler, String filter,
            boolean async) {

        if (queryhandler == null) {
            throw new IllegalArgumentException();
        }

        Cursor ret = null;
        mSortOrder = MediaStore.Audio.Media.TITLE_KEY;
        StringBuilder where = new StringBuilder();
        where.append(MediaStore.Audio.Media.TITLE + " != ''");
        /* SPRD 508522  @{ */
        where.append(" AND " + MediaStore.Audio.Media.IS_MUSIC + "=1");
        /* @} */

        if (mGenre != null) {
            Uri uri = MediaStore.Audio.Genres.Members.getContentUri("external",
                    Integer.valueOf(mGenre));
            if (!TextUtils.isEmpty(filter)) {
                uri = uri.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
            }
            mSortOrder = MediaStore.Audio.Genres.Members.DEFAULT_SORT_ORDER;
            ret = queryhandler.doQuery(uri,
                    mCursorCols, where.toString(), null, mSortOrder, async);
        } else if (mPlaylist != null) {
            if (mPlaylist.equals("nowplaying")) {
                if (MusicUtils.sService != null) {
                    ret = new NowPlayingCursor(MusicUtils.sService, mCursorCols);
                    if (ret.getCount() == 0) {
                        finish();
                    }
                } else {
                    // Nothing is playing.
                }
            } else if (mPlaylist.equals("podcasts")) {
                where.append(" AND " + MediaStore.Audio.Media.IS_PODCAST + "=1");
                Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                if (!TextUtils.isEmpty(filter)) {
                    uri = uri.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
                }
                ret = queryhandler.doQuery(uri,
                        mCursorCols, where.toString(), null,
                        MediaStore.Audio.Media.DEFAULT_SORT_ORDER, async);
            } else if (mPlaylist.equals("recentlyadded")) {
                // do a query for all songs added in the last X weeks
                Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                if (!TextUtils.isEmpty(filter)) {
                    uri = uri.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
                }
                int X = MusicUtils.getIntPref(this, "numweeks", 2) * (3600 * 24 * 7);
                where.append(" AND " + MediaStore.MediaColumns.DATE_ADDED + ">");
                where.append(System.currentTimeMillis() / 1000 - X);
                ret = queryhandler.doQuery(uri,
                        mCursorCols, where.toString(), null,
                        MediaStore.Audio.Media.DEFAULT_SORT_ORDER, async);
            } else {
                Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external",
                        Long.valueOf(mPlaylist));
                if (!TextUtils.isEmpty(filter)) {
                    uri = uri.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
                }
                mSortOrder = MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER;
                ret = queryhandler.doQuery(uri, mPlaylistMemberCols,
                        where.toString(), null, mSortOrder, async);
            }
        } else {
            if (mAlbumId != null) {
                where.append(" AND " + MediaStore.Audio.Media.ALBUM_ID + "=" + mAlbumId);
                mSortOrder = MediaStore.Audio.Media.TRACK + ", " + mSortOrder;
            }
            if (mArtistId != null) {
                where.append(" AND " + MediaStore.Audio.Media.ARTIST_ID + "=" + mArtistId);
            }
            where.append(" AND " + MediaStore.Audio.Media.IS_MUSIC + "=1");
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            if (!TextUtils.isEmpty(filter)) {
                uri = uri.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
            }
            ret = queryhandler.doQuery(uri,
                    mCursorCols, where.toString() , null, mSortOrder, async);
        }
        
        // This special case is for the "nowplaying" cursor, which cannot be handled
        // asynchronously using AsyncQueryHandler, so we do some extra initialization here.
        if (ret != null && async) {
            init(ret, false);
            setTitle();
        }
        return ret;
    }
    //SPRD: bug fix 476978
    public class NowPlayingCursor extends AbstractCursor
    {
        public NowPlayingCursor(IMediaPlaybackService service, String [] cols)
        {
            mCols = cols;
            mService  = service;
            makeNowPlayingCursor();
        }

        private void makeNowPlayingCursor() {
            /*SPRD bug fix 505833@{*/
            if (mCurrentPlaylistCursor != null) {
                mCurrentPlaylistCursor.close();
                mCurrentPlaylistCursor = null;
            }
	    /* @} */

            try {
                mNowPlaying = mService.getQueue();
            } catch (RemoteException ex) {
                mNowPlaying = new long[0];
            }
            mSize = mNowPlaying.length;
            if (mSize == 0) {
                return;
            }

            StringBuilder where = new StringBuilder();
            where.append(MediaStore.Audio.Media._ID + " IN (");
            for (int i = 0; i < mSize; i++) {
                where.append(mNowPlaying[i]);
                if (i < mSize - 1) {
                    where.append(",");
                }
            }
            where.append(")");

            mCurrentPlaylistCursor = MusicUtils.query(TrackBrowserActivity.this,
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    mCols, where.toString(), null, MediaStore.Audio.Media._ID);

            if (mCurrentPlaylistCursor == null) {
                mSize = 0;
                return;
            }
            
            int size = mCurrentPlaylistCursor.getCount();
            mCursorIdxs = new long[size];
            mCurrentPlaylistCursor.moveToFirst();
            int colidx = mCurrentPlaylistCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            for (int i = 0; i < size; i++) {
                mCursorIdxs[i] = mCurrentPlaylistCursor.getLong(colidx);
                mCurrentPlaylistCursor.moveToNext();
            }
            mCurrentPlaylistCursor.moveToFirst();
            mCurPos = -1;
            
            // At this point we can verify the 'now playing' list we got
            // earlier to make sure that all the items in there still exist
            // in the database, and remove those that aren't. This way we
            // don't get any blank items in the list.
            try {
                int removed = 0;
                for (int i = mNowPlaying.length - 1; i >= 0; i--) {
                    long trackid = mNowPlaying[i];
                    int crsridx = Arrays.binarySearch(mCursorIdxs, trackid);
                    if (crsridx < 0) {
                        //Log.i("@@@@@", "item no longer exists in db: " + trackid);
                        removed += mService.removeTrack(trackid);
                    }
                }
                if (removed > 0) {
                    mNowPlaying = mService.getQueue();
                    mSize = mNowPlaying.length;
                    if (mSize == 0) {
                        mCursorIdxs = null;
                        return;
                    }
                }
            } catch (RemoteException ex) {
                mNowPlaying = new long[0];
            }
        }

        @Override
        public int getCount()
        {
            return mSize;
        }

        @Override
        public boolean onMove(int oldPosition, int newPosition)
        {
            if (oldPosition == newPosition)
                return true;
            
            if (mNowPlaying == null || mCursorIdxs == null || newPosition >= mNowPlaying.length) {
                return false;
            }

            // The cursor doesn't have any duplicates in it, and is not ordered
            // in queue-order, so we need to figure out where in the cursor we
            // should be.
           
            long newid = mNowPlaying[newPosition];
            int crsridx = Arrays.binarySearch(mCursorIdxs, newid);
            mCurrentPlaylistCursor.moveToPosition(crsridx);
            mCurPos = newPosition;
            
            return true;
        }

        public boolean removeItem(int which)
        {
            try {
                if (mService.removeTracks(which, which) == 0) {
                    return false; // delete failed
                }
                int i = (int) which;
                mSize--;
                while (i < mSize) {
                    mNowPlaying[i] = mNowPlaying[i+1];
                    i++;
                }
                onMove(-1, (int) mCurPos);
            } catch (RemoteException ex) {
            }
            return true;
        }
        
        public void moveItem(int from, int to) {
            try {
                mService.moveQueueItem(from, to);
                /* SPRD 476972 @{ */
                int prevNowPlayingLength = mNowPlaying.length;
                mNowPlaying = mService.getQueue();
                int curNowPlayingLength = mNowPlaying.length;
                if (prevNowPlayingLength != curNowPlayingLength) {
                    Log.d(LOGTAG, "music crash Cursor need update!");
                    makeNowPlayingCursor();
                }
                /* @} */
                onMove(-1, mCurPos); // update the underlying cursor
            } catch (RemoteException ex) {
            }
        }

        private void dump() {
            String where = "(";
            for (int i = 0; i < mSize; i++) {
                where += mNowPlaying[i];
                if (i < mSize - 1) {
                    where += ",";
                }
            }
            where += ")";
            Log.i("NowPlayingCursor: ", where);
        }

        @Override
        public String getString(int column)
        {
            try {
                return mCurrentPlaylistCursor.getString(column);
            } catch (Exception ex) {
                onChange(true);
                return "";
            }
        }

        @Override
        public short getShort(int column)
        {
            return mCurrentPlaylistCursor.getShort(column);
        }

        @Override
        public int getInt(int column)
        {
            try {
                return mCurrentPlaylistCursor.getInt(column);
            } catch (Exception ex) {
                onChange(true);
                return 0;
            }
        }

        @Override
        public long getLong(int column)
        {
            try {
                return mCurrentPlaylistCursor.getLong(column);
            } catch (Exception ex) {
                onChange(true);
                return 0;
            }
        }

        @Override
        public float getFloat(int column)
        {
            return mCurrentPlaylistCursor.getFloat(column);
        }

        @Override
        public double getDouble(int column)
        {
            return mCurrentPlaylistCursor.getDouble(column);
        }

        @Override
        public int getType(int column) {
            return mCurrentPlaylistCursor.getType(column);
        }

        @Override
        public boolean isNull(int column)
        {
            return mCurrentPlaylistCursor.isNull(column);
        }

        @Override
        public String[] getColumnNames()
        {
            return mCols;
        }
        
        @Override
        public void deactivate()
        {
            if (mCurrentPlaylistCursor != null)
                mCurrentPlaylistCursor.deactivate();
        }

        @Override
        public boolean requery()
        {
            makeNowPlayingCursor();
            return true;
        }
        /* SPRD 524518 @{ */
        @Override
        public void close(){
            super.close();
            if(mCurrentPlaylistCursor != null){
                mCurrentPlaylistCursor.close();
                mCurrentPlaylistCursor = null;
            }
        }
        /* @} */
        private String [] mCols;
        private Cursor mCurrentPlaylistCursor;     // updated in onMove
        private int mSize;          // size of the queue
        private long[] mNowPlaying;
        private long[] mCursorIdxs;
        private int mCurPos;
        private IMediaPlaybackService mService;
    }
    //SPRD 476978
    public static class TrackListAdapter extends SimpleCursorAdapter implements SectionIndexer {
        boolean mIsNowPlaying;
        boolean mDisableNowPlayingIndicator;

        int mTitleIdx;
        int mArtistIdx;
        int mDurationIdx;
        int mAudioIdIdx;
        // SPRD 476978
        public int mDataIdx;

        private final StringBuilder mBuilder = new StringBuilder();
        /* SPRD 476972 @{ */
        private String mUnknownArtist;
        private String mUnknownAlbum;
        /* @} */
        private AlphabetIndexer mIndexer;
        
        private TrackBrowserActivity mActivity = null;
        private TrackQueryHandler mQueryHandler;
        private String mConstraint = null;
        private boolean mConstraintIsValid = false;
        /* SPRD 476972 @{ */
        private int mLayout;
        private Context mContext;
        /* @} */
        public static class ViewHolder {
            TextView line1;
            TextView line2;
            TextView duration;
            //SPRD 476978
            public ImageView drmIcon;
            ImageView play_indicator;
            CharArrayBuffer buffer1;
            char [] buffer2;
        }

        class TrackQueryHandler extends AsyncQueryHandler {

            class QueryArgs {
                public Uri uri;
                public String [] projection;
                public String selection;
                public String [] selectionArgs;
                public String orderBy;
            }

            TrackQueryHandler(ContentResolver res) {
                super(res);
            }
            
            public Cursor doQuery(Uri uri, String[] projection,
                    String selection, String[] selectionArgs,
                    String orderBy, boolean async) {
                if (async) {
                    // Get 100 results first, which is enough to allow the user to start scrolling,
                    // while still being very fast.
                    Uri limituri = uri.buildUpon().appendQueryParameter("limit", "100").build();
                    QueryArgs args = new QueryArgs();
                    args.uri = uri;
                    args.projection = projection;
                    args.selection = selection;
                    args.selectionArgs = selectionArgs;
                    args.orderBy = orderBy;

                    startQuery(0, args, limituri, projection, selection, selectionArgs, orderBy);
                    return null;
                }
                return MusicUtils.query(mActivity,
                        uri, projection, selection, selectionArgs, orderBy);
            }

            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                //Log.i("@@@", "query complete: " + cursor.getCount() + "   " + mActivity);
                mActivity.init(cursor, cookie != null);
                if (token == 0 && cookie != null && cursor != null &&
                    !cursor.isClosed() && cursor.getCount() >= 100) {
                    QueryArgs args = (QueryArgs) cookie;
                    startQuery(1, null, args.uri, args.projection, args.selection,
                            args.selectionArgs, args.orderBy);
                }
            }
        }
        
        TrackListAdapter(Context context, TrackBrowserActivity currentactivity,
                int layout, Cursor cursor, String[] from, int[] to,
                boolean isnowplaying, boolean disablenowplayingindicator) {
            super(context, layout, cursor, from, to);
            mActivity = currentactivity;
            getColumnIndices(cursor);
            mIsNowPlaying = isnowplaying;
            mDisableNowPlayingIndicator = disablenowplayingindicator;
            mUnknownArtist = context.getString(R.string.unknown_artist_name);
            mUnknownAlbum = context.getString(R.string.unknown_album_name);
            
            mQueryHandler = new TrackQueryHandler(context.getContentResolver());
            /* SPRD 476972 @{ */
            mLayout = layout;
            mContext = context;
            /* @} */
        }
        
        public void setActivity(TrackBrowserActivity newactivity) {
            mActivity = newactivity;
        }
        
        public TrackQueryHandler getQueryHandler() {
            return mQueryHandler;
        }
        
        private void getColumnIndices(Cursor cursor) {
            if (cursor != null) {
                mTitleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                mArtistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                mDurationIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                //SPRD 476978
                mDataIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                try {
                    mAudioIdIdx = cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Playlists.Members.AUDIO_ID);
                } catch (IllegalArgumentException ex) {
                    mAudioIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                }
            }
        }
        /* SPRD 476972 @{ */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh = null;
            if(convertView == null){
                convertView = LayoutInflater.from(mContext).inflate(mLayout, parent, false);

                ImageView iv = (ImageView) convertView.findViewById(R.id.icon);
                iv.setVisibility(View.GONE);

                vh = new ViewHolder();
                vh.line1 = (TextView) convertView.findViewById(R.id.line1);
                vh.line2 = (TextView) convertView.findViewById(R.id.line2);
                vh.duration = (TextView) convertView.findViewById(R.id.duration);
                vh.drmIcon = (ImageView) convertView.findViewById(R.id.drm_icon);
                vh.play_indicator = (ImageView) convertView.findViewById(R.id.play_indicator);
                vh.buffer1 = new CharArrayBuffer(100);
                vh.buffer2 = new char[200];

                convertView.setTag(vh);
            }else{
                vh = (ViewHolder) convertView.getTag();
            }
            if(getCursor() == null || !getCursor().moveToPosition(position)){
                return convertView;
            }
            if(getCursor() != null){
                vh = MusicDRM.getInstance().bindViewDrm(getCursor(),mDataIdx,vh);
                getCursor().copyStringToBuffer(mTitleIdx, vh.buffer1);
                vh.line1.setText(vh.buffer1.data, 0, vh.buffer1.sizeCopied);

                int secs = Math.round((float) getCursor().getInt(mDurationIdx) / 1000);
                if(getCursor().getInt(mDurationIdx) == 0){
                    vh.duration.setText("");
                }else if (getCursor().getInt(mDurationIdx) != 0 && secs == 0) {
                    vh.duration.setText(MusicUtils.makeTimeString(mContext, 1));
                } else {
                    vh.duration.setText(MusicUtils.makeTimeString(mContext, secs));
                }

                final StringBuilder builder = mBuilder;
                builder.delete(0, builder.length());

                String name = getCursor().getString(mArtistIdx);
                if (name == null || name.equals(MediaStore.UNKNOWN_STRING)) {
                    builder.append(mUnknownArtist);
                } else {
                    builder.append(name);
                }
                int len = builder.length();
                if (vh.buffer2.length < len) {
                    vh.buffer2 = new char[len];
                }
                builder.getChars(0, len, vh.buffer2, 0);
                vh.line2.setText(vh.buffer2, 0, len);
            }
            ImageView mIndicaterView = vh.play_indicator;
            long id = -1;
            if (MusicUtils.sService != null) {
                try {
                    if (mIsNowPlaying) {
                        id = MusicUtils.sService.getQueuePosition();
                    } else {
                        id = MusicUtils.sService.getAudioId();
                    }

                } catch (RemoteException ex) {
                }
            }
            if ((mIsNowPlaying && getCursor().getLong(mAudioIdIdx) == id) ||
                    (!mIsNowPlaying && !mDisableNowPlayingIndicator && getCursor().getLong(mAudioIdIdx) == id)) {
                mIndicaterView.setImageResource(R.drawable.indicator_ic_mp_playing_list);
                mIndicaterView.setVisibility(View.VISIBLE);
            } else {
                mIndicaterView.setVisibility(View.GONE);
            }
            /* SPRD 525421 characters of some songs in tracklist are white @{ */
            if(mIsShowAlbumBg){
                vh.line1.setTextColor(Color.WHITE);
                vh.line2.setTextColor(Color.WHITE);
                vh.duration.setTextColor(Color.WHITE);
            } else {
                vh.line1.setTextColor(Color.BLACK);
                vh.line2.setTextColor(Color.BLACK);
                vh.duration.setTextColor(Color.BLACK);
            }
            /* @} */
            return convertView;
        }
        /* @} */
        @Override
        public void changeCursor(Cursor cursor) {
            if (mActivity.isFinishing() && cursor != null) {
                cursor.close();
                cursor = null;
            }
            if (cursor != mActivity.mTrackCursor) {
                mActivity.mTrackCursor = cursor;
                super.changeCursor(cursor);
                getColumnIndices(cursor);
            }
            /* SPRD bug fix 519451 because the content is modified from a background
               thread [runQueryOnBackgroundThread],so need to notifyDataSetChanged
               when change cursor @{ */
            notifyDataSetChanged();
            /* @} */
        }
        
        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            String s = constraint.toString();
            if (mConstraintIsValid && (
                    (s == null && mConstraint == null) ||
                    (s != null && s.equals(mConstraint)))) {
                return getCursor();
            }
            Cursor c = mActivity.getTrackCursor(mQueryHandler, s, false);
            mConstraint = s;
            mConstraintIsValid = true;
            return c;
        }
        
        // SectionIndexer methods
        
        public Object[] getSections() {
            if (mIndexer != null) { 
                return mIndexer.getSections();
            } else {
                return new String [] { " " };
            }
        }
        
        public int getPositionForSection(int section) {
            if (mIndexer != null) {
                return mIndexer.getPositionForSection(section);
            }
            return 0;
        }
        
        public int getSectionForPosition(int position) {
            return 0;
        }
        /* SPRD 476972 @{ */
        public void reloadStringOnLocaleChanges() {
           String unknownArtist = mActivity
                   .getString(R.string.unknown_artist_name);
           String unknownAlbum = mActivity
                   .getString(R.string.unknown_album_name);
           if (mUnknownArtist != null && !mUnknownArtist.equals(unknownArtist)) {
               mUnknownArtist = unknownArtist;
           }
           if (mUnknownAlbum != null && !mUnknownAlbum.equals(unknownAlbum)) {
               mUnknownAlbum = unknownAlbum;
           }
        }
    }
    /* SPRD 475999 @{ */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mTrackCursor != null && mTrackCursor.getCount() >= 0) {
            /* SPRD 497265  when change configuraion select right tab@{ */
            // SPRD 523924 get flag whether tab show
            mNeedShowQuitMenuitem = MusicUtils.updateButtonBar(this, R.id.songtab, newConfig.orientation);
            /* @}  */
            /* SPRD:500690 Hide the now playing bar @{ */
            if (!mEditMode) {
                MusicUtils.updateNowPlaying(TrackBrowserActivity.this);
            }
            /* @} */
            // SPRD: Delete for bug 504766
            //invalidateOptionsMenu();
            mTrackList = getListView();
            mTrackList.setOnCreateContextMenuListener(this);
            if (mEditMode) {
                ((TouchInterceptor) mTrackList).setDropListener(mDropListener);
                ((TouchInterceptor) mTrackList).setRemoveListener(mRemoveListener);
            } else {
                mTrackList.setTextFilterEnabled(true);
            }
            if (mTrackList != null) {
                mTrackList.post(new Runnable() {
                    @Override
                    public void run() {
                        setAlbumArtBackground();
                    }
                });
            }
        }
    }
    /* @} */
    /* SPRD 476972 @{ */
    public String getCurTrackMode() {
        return mPlaylist;
    }
    /* @} */
}

