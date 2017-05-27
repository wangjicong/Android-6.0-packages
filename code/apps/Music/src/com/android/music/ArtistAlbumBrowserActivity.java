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
import com.android.music.QueryBrowserActivity.QueryListAdapter.QueryHandler;
import com.sprd.music.album.bg.AlbumBGLoadTask;

import android.app.ActionBar;
import android.app.ExpandableListActivity;
import android.app.SearchManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CursorTreeAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

import java.text.Collator;
import java.util.ArrayList;

import com.sprd.music.utils.SPRDMusicUtils;

//SPRD:add for searchview
import android.app.SearchableInfo;
import android.widget.SearchView;
import android.graphics.Color;

/* SPRD bug fix 519898 need check runtime permisssion before use @{ */
import android.content.pm.PackageManager;
import android.widget.Toast;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
/* @} */

public class ArtistAlbumBrowserActivity extends ExpandableListActivity
        // SPRD 402678
        implements View.OnCreateContextMenuListener, MusicUtils.Defs, ServiceConnection, LoaderCallbacks<Cursor>
{
	/* SPRD 476972  @{*/
    private static final String TAG = "ArtistAlbumBrowserActivity";
    private static final String[] COLS = new String[] {
        MediaStore.Audio.Artists._ID,
        MediaStore.Audio.Artists.ARTIST,
        MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
        MediaStore.Audio.Artists.NUMBER_OF_TRACKS
    };
    private static final Uri CONTENT_URI = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
    /* @} */
    private String mCurrentArtistId;
    private String mCurrentArtistName;
    private String mCurrentAlbumId;
    private String mCurrentAlbumName;
    private String mCurrentArtistNameForAlbum;
    boolean mIsUnknownArtist;
    boolean mIsUnknownAlbum;
    private ArtistAlbumListAdapter mAdapter;
    private boolean mAdapterSent;
    private final static int SEARCH = CHILD_MENU_BASE;
    private static int mLastListPosCourse = -1;
    private static int mLastListPosFine = -1;
    private ServiceToken mToken;
    /* SPRD 476969 @{ */
    private MenuItem mSearchItem;
    private SearchView mSearchView;
    /* @} */
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if(RequestPermissionsActivity.startPermissionActivity(this)){
            return;
        }

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        /* SPRD 476972 @{ */
        ActionBar actionbar = getActionBar();
        actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
        // SPRD 520090 music title translation error
        actionbar.setTitle(getResources().getString(R.string.musicbrowserlabel));
        actionbar.setDisplayUseLogoEnabled(false);
        actionbar.setDisplayShowHomeEnabled(false);
        /* @} */
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        if (icicle != null) {
            mCurrentAlbumId = icicle.getString("selectedalbum");
            mCurrentAlbumName = icicle.getString("selectedalbumname");
            mCurrentArtistId = icicle.getString("selectedartist");
            mCurrentArtistName = icicle.getString("selectedartistname");
        }
        mToken = MusicUtils.bindToService(this, this);

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        // SPRD 476972
        f.addAction(Intent.ACTION_MEDIA_SHARED);
        f.addDataScheme("file");
        registerReceiver(mScanListener, f);

        setContentView(R.layout.media_picker_activity_expanding);
        MusicUtils.updateButtonBar(this, R.id.artisttab);
        ExpandableListView lv = getExpandableListView();
        lv.setOnCreateContextMenuListener(this);
        lv.setTextFilterEnabled(true);

        mAdapter = (ArtistAlbumListAdapter) getLastNonConfigurationInstance();
        if (mAdapter == null) {
            //Log.i("@@@", "starting query");
            mAdapter = new ArtistAlbumListAdapter(
                    getApplication(),
                    this,
                    null, // cursor
                    R.layout.track_list_item_group,
                    /* SPRD 476972 @{ */
                    //new String[] {},
                    //new int[] {},
                    R.layout.track_list_item_child);
                    //new String[] {},
                    //new int[] {});
                    /* @} */
            setListAdapter(mAdapter);
            setTitle(R.string.working_artists);
            // SPRD 476972
            // getArtistCursor(mAdapter.getQueryHandler(), null);
        } else {
            mAdapter.setActivity(this);
            // SPRD 476972
            mAdapter.reloadStringOnLocaleChanges();
            setListAdapter(mAdapter);
            mArtistCursor = mAdapter.getCursor();
            if (mArtistCursor != null) {
                init(mArtistCursor);
                /* SPRD 476972  @{*/
                //} else {
                //getArtistCursor(mAdapter.getQueryHandler(), null);
                /* @} */
            }
        }

        // SPRD 476972
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        mAdapterSent = true;
        return mAdapter;
    }
    
    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        // need to store the selected item so we don't lose it in case
        // of an orientation switch. Otherwise we could lose it while
        // in the middle of specifying a playlist to add the item to.
        outcicle.putString("selectedalbum", mCurrentAlbumId);
        outcicle.putString("selectedalbumname", mCurrentAlbumName);
        outcicle.putString("selectedartist", mCurrentArtistId);
        outcicle.putString("selectedartistname", mCurrentArtistName);
        super.onSaveInstanceState(outcicle);
    }

    @Override
    public void onDestroy() {
        ExpandableListView lv = getExpandableListView();
        if (lv != null) {
            mLastListPosCourse = lv.getFirstVisiblePosition();
            View cv = lv.getChildAt(0);
            if (cv != null) {
                mLastListPosFine = cv.getTop();
            }
        }
        // SPRD 524518
        MusicUtils.unbindFromService(mToken, this);
        // If we have an adapter and didn't send it off to another activity yet, we should
        // close its cursor, which we do by assigning a null cursor to it. Doing this
        // instead of closing the cursor directly keeps the framework from accessing
        // the closed cursor later.
        if (!mAdapterSent && mAdapter != null) {
            /* SPRD 476972  @{*/
            if (mArtistCursor != null) {
                mArtistCursor.close();
            }
            /* @} */
            mAdapter.changeCursor(null);
        }
        // Because we pass the adapter to the next activity, we need to make
        // sure it doesn't keep a reference to this activity. We can do this
        // by clearing its DatasetObservers, which setListAdapter(null) does.

        if (lv != null) {
            setListAdapter(null);
        }

        /* SPRD 476972  @{*/
        if (mAdapter != null) {
            mAdapter.destroyThread();
        }
        /* @} */
        mAdapter = null;

        try {
            unregisterReceiver(mScanListener);
        } catch(Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.META_CHANGED);
        f.addAction(MediaPlaybackService.QUEUE_CHANGED);
        registerReceiver(mTrackListListener, f);
        mTrackListListener.onReceive(null, null);

        MusicUtils.setSpinnerState(this);
    }

    private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getExpandableListView().invalidateViews();
            MusicUtils.updateNowPlaying(ArtistAlbumBrowserActivity.this);
        }
    };
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MusicLog.i(TAG, "mScanListener intent-->" + (intent !=null ? intent.getAction():"IS NULL"));
            MusicUtils.setSpinnerState(ArtistAlbumBrowserActivity.this);
            /* SPRD 476972  @{*/
            //mReScanHandler.sendEmptyMessage(0);
            if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                MusicUtils.clearAlbumArtCache();
            }
        }
    };

    /* SPRD 476972  @{
    private Handler mReScanHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mAdapter != null) {
                getArtistCursor(mAdapter.getQueryHandler(), null);
            }
        }
    };
    @} */

    @Override
    public void onPause() {
        unregisterReceiver(mTrackListListener);
        // SPRD 476972
        //mReScanHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }
    
    public void init(Cursor c) {

        if (mAdapter == null) {
            return;
        }
        mAdapter.changeCursor(c); // also sets mArtistCursor

        if (mArtistCursor == null) {
            MusicUtils.displayDatabaseError(this);
            closeContextMenu();
            // SPRD 476972
            //mReScanHandler.sendEmptyMessageDelayed(0, 1000);
            return;
        }

        // restore previous position
        if (mLastListPosCourse >= 0) {
            ExpandableListView elv = getExpandableListView();
            elv.setSelectionFromTop(mLastListPosCourse, mLastListPosFine);
            mLastListPosCourse = -1;
        }

        MusicUtils.hideDatabaseError(this);
        MusicUtils.updateButtonBar(this, R.id.artisttab);
        setTitle();
    }

    private void setTitle() {
        setTitle(R.string.artists_title);
    }
    
    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

        mCurrentAlbumId = Long.valueOf(id).toString();
        
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
        intent.putExtra("album", mCurrentAlbumId);
        Cursor c = (Cursor) getExpandableListAdapter().getChild(groupPosition, childPosition);
        String album = c.getString(c.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
        if (album == null || album.equals(MediaStore.UNKNOWN_STRING)) {
            // unknown album, so we should include the artist ID to limit the songs to songs only by that artist 
            mArtistCursor.moveToPosition(groupPosition);
            mCurrentArtistId = mArtistCursor.getString(mArtistCursor.getColumnIndex(MediaStore.Audio.Artists._ID));
            intent.putExtra("artist", mCurrentArtistId);
        }
        startActivity(intent);
        return true;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
        menu.add(0, PARTY_SHUFFLE, 0, R.string.party_shuffle); // icon will be set in onPrepareOptionsMenu()
        menu.add(0, SHUFFLE_ALL, 0, R.string.shuffle_all).setIcon(R.drawable.ic_menu_shuffle);
        /* SPRD 476973 @{ */
        menu.add(0, QUIT_MUSIC, 0, R.string.quit);
        /* @} */
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MusicUtils.setPartyShuffleMenuIcon(menu);
        /* SPRD 476973 @{ */
        MenuItem item = menu.findItem(QUIT_MUSIC);
        if (item != null) {
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
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
            case PARTY_SHUFFLE:
                MusicUtils.togglePartyShuffle();
                break;
                
            case SHUFFLE_ALL:
                cursor = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        new String [] { MediaStore.Audio.Media._ID}, 
                        MediaStore.Audio.Media.IS_MUSIC + "=1", null,
                        MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
                if (cursor != null) {
                    MusicUtils.shuffleAll(this, cursor);
                    cursor.close();
                }
                return true;

            /* SPRD 476973 @{ */
            case QUIT_MUSIC:
                SPRDMusicUtils.quitservice(this);
                return true;
            /* @} */
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfoIn) {
        menu.add(0, PLAY_SELECTION, 0, R.string.play_selection);
        SubMenu sub = menu.addSubMenu(0, ADD_TO_PLAYLIST, 0, R.string.add_to_playlist);
        MusicUtils.makePlaylistMenu(this, sub);
        menu.add(0, DELETE_ITEM, 0, R.string.delete_item);
        
        ExpandableListContextMenuInfo mi = (ExpandableListContextMenuInfo) menuInfoIn;
        
        int itemtype = ExpandableListView.getPackedPositionType(mi.packedPosition);
        int gpos = ExpandableListView.getPackedPositionGroup(mi.packedPosition);
        int cpos = ExpandableListView.getPackedPositionChild(mi.packedPosition);
        if (itemtype == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            if (gpos == -1) {
                // this shouldn't happen
                Log.d("Artist/Album", "no group");
                return;
            }
            gpos = gpos - getExpandableListView().getHeaderViewsCount();
            mArtistCursor.moveToPosition(gpos);
            mCurrentArtistId = mArtistCursor.getString(mArtistCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID));
            mCurrentArtistName = mArtistCursor.getString(mArtistCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
            mCurrentAlbumId = null;
            mIsUnknownArtist = mCurrentArtistName == null ||
                    mCurrentArtistName.equals(MediaStore.UNKNOWN_STRING);
            mIsUnknownAlbum = true;
            if (mIsUnknownArtist) {
                menu.setHeaderTitle(getString(R.string.unknown_artist_name));
            } else {
                menu.setHeaderTitle(mCurrentArtistName);
                menu.add(0, SEARCH, 0, R.string.search_title);
            }
            return;
        } else if (itemtype == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            if (cpos == -1) {
                // this shouldn't happen
                Log.d("Artist/Album", "no child");
                return;
            }
            Cursor c = (Cursor) getExpandableListAdapter().getChild(gpos, cpos);
            c.moveToPosition(cpos);
            mCurrentArtistId = null;
            mCurrentAlbumId = Long.valueOf(mi.id).toString();
            mCurrentAlbumName = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
            gpos = gpos - getExpandableListView().getHeaderViewsCount();
            mArtistCursor.moveToPosition(gpos);
            mCurrentArtistNameForAlbum = mArtistCursor.getString(
                    mArtistCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
            mIsUnknownArtist = mCurrentArtistNameForAlbum == null ||
                    mCurrentArtistNameForAlbum.equals(MediaStore.UNKNOWN_STRING);
            mIsUnknownAlbum = mCurrentAlbumName == null ||
                    mCurrentAlbumName.equals(MediaStore.UNKNOWN_STRING);
            if (mIsUnknownAlbum) {
                menu.setHeaderTitle(getString(R.string.unknown_album_name));
            } else {
                menu.setHeaderTitle(mCurrentAlbumName);
            }
            if (!mIsUnknownAlbum || !mIsUnknownArtist) {
                menu.add(0, SEARCH, 0, R.string.search_title);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case PLAY_SELECTION: {
                // play everything by the selected artist
                long [] list =
                    mCurrentArtistId != null ?
                    MusicUtils.getSongListForArtist(this, Long.parseLong(mCurrentArtistId))
                    : MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
                        
                MusicUtils.playAll(this, list, 0);
                return true;
            }

            case QUEUE: {
                long [] list =
                    mCurrentArtistId != null ?
                    MusicUtils.getSongListForArtist(this, Long.parseLong(mCurrentArtistId))
                    : MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
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
                long [] list =
                    mCurrentArtistId != null ?
                    MusicUtils.getSongListForArtist(this, Long.parseLong(mCurrentArtistId))
                    : MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
                long playlist = item.getIntent().getLongExtra("playlist", 0);
                MusicUtils.addToPlaylist(this, list, playlist);
                return true;
            }
            
            case DELETE_ITEM: {
                long [] list;
                String desc;
                /* SPRD 476972  @{*/
                String itemName = "";
                int deleteMode = 0;
                /* @} */
                if (mCurrentArtistId != null) {
                    list = MusicUtils.getSongListForArtist(this, Long.parseLong(mCurrentArtistId));
                    String f;
                    /* SPRD 476972  @{*/
                    f = getString(R.string.delete_artist);
                    if (mIsUnknownArtist) {
                        itemName = getString(R.string.unknown_artist_name);
                    } else {
                        itemName = mCurrentArtistName;
                    }
                    desc = String.format(f, itemName);
                    deleteMode = MusicUtils.DeleteMode.DELETE_ARTSIT;
                    /* @} */
                } else {
                    list = MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
                    String f;
                    /* SPRD 476972  @{*/
                    f = getString(R.string.delete_album);
                    if (mIsUnknownAlbum) {
                        itemName = getString(R.string.unknown_album_name);
                    } else {
                        itemName = mCurrentAlbumName;

                    }
                    desc = String.format(f, itemName);
                    deleteMode = MusicUtils.DeleteMode.DELETE_ABLUM;
                    /* @} */
                }
                Bundle b = new Bundle();
                b.putString("description", desc);
                b.putLongArray("items", list);
                /* SPRD 476972  @{*/
                b.putString(MusicUtils.DeleteMode.CURRENT_TRACK_NAME, itemName);
                b.putInt(MusicUtils.DeleteMode.DELETE_MODE, deleteMode);
                /* @} */
                Intent intent = new Intent();
                intent.setClass(this, DeleteItems.class);
                intent.putExtras(b);
                startActivityForResult(intent, -1);
                return true;
            }
            
            case SEARCH:
                doSearch();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    void doSearch() {
        CharSequence title = null;
        String query = null;
        
        Intent i = new Intent();
        i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        if (mCurrentArtistId != null) {
            title = mCurrentArtistName;
            query = mCurrentArtistName;
            i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, mCurrentArtistName);
            i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE);
        } else {
            if (mIsUnknownAlbum) {
                title = query = mCurrentArtistNameForAlbum;
            } else {
                title = query = mCurrentAlbumName;
                if (!mIsUnknownArtist) {
                    query = query + " " + mCurrentArtistNameForAlbum;
                }
            }
            i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, mCurrentArtistNameForAlbum);
            i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, mCurrentAlbumName);
            i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE);
        }
        title = getString(R.string.mediasearch, title);
        i.putExtra(SearchManager.QUERY, query);

        startActivity(Intent.createChooser(i, title));
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case SCAN_DONE:
                if (resultCode == RESULT_CANCELED) {
                    finish();
                /* SPRD 476972  @{*/
                //} else {
                //    getArtistCursor(mAdapter.getQueryHandler(), null);
                /* @} */
                }
                break;

            case NEW_PLAYLIST:
                if (resultCode == RESULT_OK) {
                    Uri uri = intent.getData();
                    if (uri != null) {
                        long [] list = null;
                        if (mCurrentArtistId != null) {
                            list = MusicUtils.getSongListForArtist(this, Long.parseLong(mCurrentArtistId));
                        } else if (mCurrentAlbumId != null) {
                            list = MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
                        }
                        MusicUtils.addToPlaylist(this, list, Long.parseLong(uri.getLastPathSegment()));
                    }
                }
                break;
        }
    }
    /* SPRD 476972  @{
    private Cursor getArtistCursor(AsyncQueryHandler async, String filter) {

        String[] cols = new String[] {
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
                MediaStore.Audio.Artists.NUMBER_OF_TRACKS
        };

        Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
        if (!TextUtils.isEmpty(filter)) {
            uri = uri.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
        }

        Cursor ret = null;
        if (async != null) {
            async.startQuery(0, null, uri,
                    cols, null , null, MediaStore.Audio.Artists.ARTIST_KEY);
        } else {
            ret = MusicUtils.query(this, uri,
                    cols, null , null, MediaStore.Audio.Artists.ARTIST_KEY);
        }
        return ret;
    }
    @} */
    /* SPRD 476972  @{*/
    //static class ArtistAlbumListAdapter extends SimpleCursorTreeAdapter implements SectionIndexer {
      static class ArtistAlbumListAdapter extends CursorTreeAdapter implements SectionIndexer{
    /* @} */

        private final Drawable mNowPlayingOverlay;
        private final BitmapDrawable mDefaultAlbumIcon;
        private int mGroupArtistIdIdx;
        private int mGroupArtistIdx;
        private int mGroupAlbumIdx;
        private int mGroupSongIdx;
        private final Context mContext;
        private final Resources mResources;
        private final String mAlbumSongSeparator;
        /* SPRD 476972  @{*/
        private String mUnknownAlbum;
        private String mUnknownArtist;
        /* @} */
        private final StringBuilder mBuffer = new StringBuilder();
        private final Object[] mFormatArgs = new Object[1];
        private final Object[] mFormatArgs3 = new Object[3];
        private MusicAlphabetIndexer mIndexer;
        private ArtistAlbumBrowserActivity mActivity;
        private AsyncQueryHandler mQueryHandler;
        private String mConstraint = null;
        private boolean mConstraintIsValid = false;
        /* SPRD 476972  @{*/
        private HandlerThread mWorkThread = new HandlerThread("artWorkDeamon");
        private Handler mWorkHandler = null;
        private Handler mHandler = null;
        /* @} */
        static class ViewHolder {
            TextView line1;
            TextView line2;
            ImageView play_indicator;
            ImageView icon;
        }

        /* SPRD 476972  @{*/
        private int mGruopLayout, mChildLayout;
        private LayoutInflater mInflater;
        //class QueryHandler extends AsyncQueryHandler {
        //    QueryHandler(ContentResolver res) {
        //        super(res);
        //    }

        //    @Override
        //    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
        //        //Log.i("@@@", "query complete");
        //         mActivity.init(cursor);
        //    }
        //}

        //ArtistAlbumListAdapter(Context context, ArtistAlbumBrowserActivity currentactivity,
        //        Cursor cursor, int glayout, String[] gfrom, int[] gto,
        //        int clayout, String[] cfrom, int[] cto) {
        //    super(context, cursor, glayout, gfrom, gto, clayout, cfrom, cto);
        //    mActivity = currentactivity;
        //    mQueryHandler = new QueryHandler(context.getContentResolver());
        ArtistAlbumListAdapter(Context context, ArtistAlbumBrowserActivity currentactivity,
                  Cursor cursor, int glayout, int clayout) {
            super(cursor, context, false);
        /* @} */
            mActivity = currentactivity;
            /* SPRD 476972  @{*/
            mGruopLayout = glayout;
            mChildLayout = clayout;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            /* @} */
            Resources r = context.getResources();
            mNowPlayingOverlay = r.getDrawable(R.drawable.indicator_ic_mp_playing_list);
            mDefaultAlbumIcon = (BitmapDrawable) r.getDrawable(R.drawable.albumart_mp_unknown_list);
            // no filter or dither, it's a lot faster and we can't tell the difference
            mDefaultAlbumIcon.setFilterBitmap(false);
            mDefaultAlbumIcon.setDither(false);
            
            mContext = context;
            getColumnIndices(cursor);
            mResources = context.getResources();
            mAlbumSongSeparator = context.getString(R.string.albumsongseparator);
            mUnknownAlbum = context.getString(R.string.unknown_album_name);
            mUnknownArtist = context.getString(R.string.unknown_artist_name);
            /* SPRD 476972  @{*/
            mWorkThread.start();
            mWorkHandler = new Handler(mWorkThread.getLooper());
            mHandler = new Handler();
            /* @} */
        }
        
        private void getColumnIndices(Cursor cursor) {
            // SPRD 476972
            if (cursor != null && !cursor.isClosed()) {
                mGroupArtistIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID);
                mGroupArtistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST);
                mGroupAlbumIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS);
                mGroupSongIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS);
                /* SPRD 476972  @{*/
                /*if (mIndexer != null) {
                    mIndexer.setCursor(cursor);
                } else {
                    mIndexer = new MusicAlphabetIndexer(cursor, mGroupArtistIdx,
                           mResources.getString(R.string.fast_scroll_alphabet));
                }*/
                /* @} */
            }
        }
        
        public void setActivity(ArtistAlbumBrowserActivity newactivity) {
            mActivity = newactivity;
        }

        /* SPRD 476972  @{
        public AsyncQueryHandler getQueryHandler() {
            return mQueryHandler;
        }
        @} */

        @Override
        public View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
            /* SPRD 476972  @{*/
            //View v = super.newGroupView(context, cursor, isExpanded, parent);
            View v = mInflater.inflate(mGruopLayout, parent, false);
            /* @} */
            ImageView iv = (ImageView) v.findViewById(R.id.icon);
            ViewGroup.LayoutParams p = iv.getLayoutParams();
            p.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            p.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            ViewHolder vh = new ViewHolder();
            vh.line1 = (TextView) v.findViewById(R.id.line1);
            vh.line2 = (TextView) v.findViewById(R.id.line2);
            vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
            vh.icon = (ImageView) v.findViewById(R.id.icon);
            vh.icon.setPadding(0, 0, 1, 0);
            v.setTag(vh);
            return v;
        }

        @Override
        public View newChildView(Context context, Cursor cursor, boolean isLastChild,
                ViewGroup parent) {
            /* SPRD 476972  @{*/
            //View v = super.newChildView(context, cursor, isLastChild, parent);
            View v = mInflater.inflate(mChildLayout, parent, false);
            /* @} */
            ViewHolder vh = new ViewHolder();
            vh.line1 = (TextView) v.findViewById(R.id.line1);
            vh.line2 = (TextView) v.findViewById(R.id.line2);
            vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
            vh.icon = (ImageView) v.findViewById(R.id.icon);
            vh.icon.setBackgroundDrawable(mDefaultAlbumIcon);
            vh.icon.setPadding(0, 0, 1, 0);
            v.setTag(vh);
            return v;
        }
        
        @Override
        public void bindGroupView(View view, Context context, Cursor cursor, boolean isexpanded) {

            ViewHolder vh = (ViewHolder) view.getTag();

            String artist = cursor.getString(mGroupArtistIdx);
            String displayartist = artist;
            boolean unknown = artist == null || artist.equals(MediaStore.UNKNOWN_STRING);
            if (unknown) {
                displayartist = mUnknownArtist;
            }
            vh.line1.setText(displayartist);

            int numalbums = cursor.getInt(mGroupAlbumIdx);
            int numsongs = cursor.getInt(mGroupSongIdx);
            
            String songs_albums = MusicUtils.makeAlbumsLabel(context,
                    numalbums, numsongs, unknown);
            
            vh.line2.setText(songs_albums);
            
            long currentartistid = MusicUtils.getCurrentArtistId();
            long artistid = cursor.getLong(mGroupArtistIdIdx);
            if (currentartistid == artistid && !isexpanded) {
                vh.play_indicator.setImageDrawable(mNowPlayingOverlay);
            } else {
                vh.play_indicator.setImageDrawable(null);
            }
        }

        @Override
        public void bindChildView(View view, Context context, Cursor cursor, boolean islast) {

            ViewHolder vh = (ViewHolder) view.getTag();

            String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
            String displayname = name;
            boolean unknown = name == null || name.equals(MediaStore.UNKNOWN_STRING); 
            if (unknown) {
                displayname = mUnknownAlbum;
            }
            vh.line1.setText(displayname);

            int numsongs = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS));
            int numartistsongs = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST));

            final StringBuilder builder = mBuffer;
            builder.delete(0, builder.length());
            if (unknown) {
                numsongs = numartistsongs;
            }
              
            if (numsongs == 1) {
                builder.append(context.getString(R.string.onesong));
            } else {
                if (numsongs == numartistsongs) {
                    final Object[] args = mFormatArgs;
                    args[0] = numsongs;
                    builder.append(mResources.getQuantityString(R.plurals.Nsongs, numsongs, args));
                } else {
                    final Object[] args = mFormatArgs3;
                    args[0] = numsongs;
                    args[1] = numartistsongs;
                    args[2] = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
                    builder.append(mResources.getQuantityString(R.plurals.Nsongscomp, numsongs, args));
                }
            }
            vh.line2.setText(builder.toString());
            
            ImageView iv = vh.icon;
            // We don't actually need the path to the thumbnail file,
            // we just use it to see if there is album art or not
            String art = cursor.getString(cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Albums.ALBUM_ART));
            if (unknown || art == null || art.length() == 0) {
                iv.setBackgroundDrawable(mDefaultAlbumIcon);
                iv.setImageDrawable(null);
            } else {
                long artIndex = cursor.getLong(0);
                /* SPRD 476972  @{*/
                //Drawable d = MusicUtils.getCachedArtwork(context, artIndex, mDefaultAlbumIcon);
                //iv.setImageDrawable(d);
                mWorkHandler.post(new AlbumBGLoadTask(context, mHandler, artIndex, mDefaultAlbumIcon, iv));
                /* @} */
            }

            long currentalbumid = MusicUtils.getCurrentAlbumId();
            long aid = cursor.getLong(0);
            iv = vh.play_indicator;
            if (currentalbumid == aid) {
                iv.setImageDrawable(mNowPlayingOverlay);
            } else {
                iv.setImageDrawable(null);
            }
        }

        
        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {
            
            long id = groupCursor.getLong(groupCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID));
            
            String[] cols = new String[] {
                    MediaStore.Audio.Albums._ID,
                    MediaStore.Audio.Albums.ALBUM,
                    MediaStore.Audio.Albums.NUMBER_OF_SONGS,
                    MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST,
                    MediaStore.Audio.Albums.ALBUM_ART
            };
            Cursor c = MusicUtils.query(mActivity,
                    MediaStore.Audio.Artists.Albums.getContentUri("external", id),
                    cols, null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
            /* SPRD 476972  @{*/
            if (c == null) {
                return null;
            }
            /* @} */

            class MyCursorWrapper extends CursorWrapper {
                String mArtistName;
                int mMagicColumnIdx;
                MyCursorWrapper(Cursor c, String artist) {
                    super(c);
                    mArtistName = artist;
                    if (mArtistName == null || mArtistName.equals(MediaStore.UNKNOWN_STRING)) {
                        mArtistName = mUnknownArtist;
                    }
                    mMagicColumnIdx = c.getColumnCount();
                }
                
                @Override
                public String getString(int columnIndex) {
                    if (columnIndex != mMagicColumnIdx) {
                        return super.getString(columnIndex);
                    }
                    return mArtistName;
                }
                
                @Override
                public int getColumnIndexOrThrow(String name) {
                    if (MediaStore.Audio.Albums.ARTIST.equals(name)) {
                        return mMagicColumnIdx;
                    }
                    return super.getColumnIndexOrThrow(name); 
                }
                
                @Override
                public String getColumnName(int idx) {
                    if (idx != mMagicColumnIdx) {
                        return super.getColumnName(idx);
                    }
                    return MediaStore.Audio.Albums.ARTIST;
                }
                
                @Override
                public int getColumnCount() {
                    return super.getColumnCount() + 1;
                }
            }
            return new MyCursorWrapper(c, groupCursor.getString(mGroupArtistIdx));
        }

        @Override
        public void changeCursor(Cursor cursor) {
            if (mActivity.isFinishing() && cursor != null) {
                cursor.close();
                cursor = null;
            }
            /* SPRD 476972  @{*/
            if (cursor != null && cursor.isClosed()) {
                cursor = null;
                Log.e(TAG, "newCursor has closed before change cursor");
            }
            /* @} */
            if (cursor != mActivity.mArtistCursor) {
                mActivity.mArtistCursor = cursor;
                getColumnIndices(cursor);
                super.changeCursor(cursor);
            }
        }
        
        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            String s = constraint.toString();
            if (mConstraintIsValid && (
                    (s == null && mConstraint == null) ||
                    (s != null && s.equals(mConstraint)))) {
                return getCursor();
            }
            Cursor c = mActivity.getArtistCursor(null, s);
            mConstraint = s;
            mConstraintIsValid = true;
            return c;
        }

        public Object[] getSections() {
            /* SPRD 491550 @{ */
            if (mIndexer != null) {
                return mIndexer.getSections();
            } else {
                return new String [] { " " };
            }
            /* @} */
        }
        
        public int getPositionForSection(int sectionIndex) {
            /* SPRD: fix bug 505604  @{*/
            //return mIndexer.getPositionForSection(sectionIndex);
            if(mIndexer != null) {
                return mIndexer.getPositionForSection(sectionIndex);
            }
            return 0;
            /* @} */
        }
        
        public int getSectionForPosition(int position) {
            return 0;
        }

        /* SPRD 476972  @{*/
        public void destroyThread () {
            if(mWorkThread != null){
                mWorkThread.quit();
            }
        }

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
        /* @} */
    }
    
    private Cursor mArtistCursor;

    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicUtils.updateNowPlaying(this);
    }

    public void onServiceDisconnected(ComponentName name) {
        finish();
    }

    /* SPRD 476972  @{*/
    private Cursor getArtistCursor(AsyncQueryHandler async, String filter) {
        Uri uri = null;
        if (!TextUtils.isEmpty(filter)) {
            uri = CONTENT_URI.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
        }

        return  MusicUtils.query(this, uri,
                COLS, null , null, MediaStore.Audio.Artists.ARTIST_KEY);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, CONTENT_URI, COLS, null, null, MediaStore.Audio.Artists.ARTIST_KEY);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
        Log.i(TAG, "onLoadFinished");
        init(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        Log.i(TAG, "onLoaderReset");
        if (mAdapter == null) {
            return;
        }
        mAdapter.changeCursor(null);
    }
    /* @} */
    /* SPRD 475999 @{ */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        MusicUtils.updateButtonBar(this, R.id.artisttab, newConfig.orientation);
        ExpandableListView lv = getExpandableListView();
        lv.setOnCreateContextMenuListener(this);
        lv.setTextFilterEnabled(true);
        MusicUtils.updateNowPlaying(ArtistAlbumBrowserActivity.this);

    }
    /* @} */
}

