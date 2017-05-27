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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Adapter;
import android.widget.AlphabetIndexer;
import android.widget.CursorAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import com.sprd.music.album.bg.AlbumBGLoadTask;
import com.sprd.music.utils.SPRDMusicUtils;

import java.text.Collator;
import java.util.ArrayList;

import com.sprd.music.utils.SPRDMusicUtils;

import android.app.SearchableInfo;
import android.widget.SearchView;
import android.graphics.Color;

/* SPRD bug fix 519898 need check runtime permisssion before use @{ */
import android.content.pm.PackageManager;
import android.widget.Toast;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
/* @} */

public class AlbumBrowserActivity extends ListActivity
    implements View.OnCreateContextMenuListener, MusicUtils.Defs, ServiceConnection
{
    private String mCurrentAlbumId;
    private String mCurrentAlbumName;
    private String mCurrentArtistNameForAlbum;
    boolean mIsUnknownArtist;
    boolean mIsUnknownAlbum;
    private AlbumListAdapter mAdapter;
    private boolean mAdapterSent;
    private final static int SEARCH = CHILD_MENU_BASE;
    private static int mLastListPosCourse = -1;
    private static int mLastListPosFine = -1;
    private ServiceToken mToken;
    /* SPRD 476969 @{ */
    private MenuItem mSearchItem;
    private SearchView mSearchView;
    /* @} */
    /* SPRD 476972 @{ */
    private AudioContentObserver mAudioContentObserver;
    private static String TAG = "AlbumBrowserActivity";
    /* @} */
    // SPRD 523924 add flag for showing quit menuitem
    private boolean mNeedShowQuitMenuitem = false;

    public AlbumBrowserActivity()
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
        if (icicle != null) {
            mCurrentAlbumId = icicle.getString("selectedalbum");
            mArtistId = icicle.getString("artist");
        } else {
            mArtistId = getIntent().getStringExtra("artist");
        }

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        /* SPRD 475999 @{ */
        ActionBar actionbar = getActionBar();
        actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
        //SPRD 520090 music title translation error
        actionbar.setTitle(getResources().getString(R.string.musicbrowserlabel));
        actionbar.setDisplayUseLogoEnabled(false);
        /* @} */
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mToken = MusicUtils.bindToService(this, this);

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        f.addDataScheme("file");
        registerReceiver(mScanListener, f);

        setContentView(R.layout.media_picker_activity);
        // SPRD 523924  get flag whether tab show
        mNeedShowQuitMenuitem = MusicUtils.updateButtonBar(this, R.id.albumtab);
        ListView lv = getListView();
        lv.setOnCreateContextMenuListener(this);
        lv.setTextFilterEnabled(true);

        mAdapter = (AlbumListAdapter) getLastNonConfigurationInstance();
        if (mAdapter == null) {
            //Log.i("@@@", "starting query");
            mAdapter = new AlbumListAdapter(
                    getApplication(),
                    this,
                    R.layout.track_list_item,
                    mAlbumCursor,
                    new String[] {},
                    new int[] {});
            setListAdapter(mAdapter);
            setTitle(R.string.working_albums);
            getAlbumCursor(mAdapter.getQueryHandler(), null);
        } else {
            mAdapter.setActivity(this);
            // SPRD 476972
            mAdapter.reloadStringOnLocaleChanges();
            setListAdapter(mAdapter);
            mAlbumCursor = mAdapter.getCursor();
            if (mAlbumCursor != null) {
                init(mAlbumCursor);
            } else {
                getAlbumCursor(mAdapter.getQueryHandler(), null);
            }
        }
        /* SPRD 476972 @{ */
        mAudioContentObserver = new AudioContentObserver(this);
        mAudioContentObserver.registerObserver();
        /* @} */
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
        outcicle.putString("artist", mArtistId);
        super.onSaveInstanceState(outcicle);
    }

    @Override
    public void onDestroy() {
        ListView lv = getListView();
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
            /* SPRD 476972 @{ */
            if (mAlbumCursor != null) {
                mAlbumCursor.close();
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
        try{
            unregisterReceiver(mScanListener);
        } catch(Exception e) {
            e.printStackTrace();
        }
        /* SPRD 476972 @{ */
        if (mAdapter != null) {
            mAdapter.destroyThread();
        }
        /* @} */
        mAdapter = null;
//        unregisterReceiver(mScanListener);
        /* SPRD 476972 @{ */
        if (mAudioContentObserver != null) {
            mAudioContentObserver.unregisterObserver();
        }
        /* @} */
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
            getListView().invalidateViews();
            MusicUtils.updateNowPlaying(AlbumBrowserActivity.this);
        }
    };
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MusicUtils.setSpinnerState(AlbumBrowserActivity.this);
            mReScanHandler.sendEmptyMessage(0);
            if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                MusicUtils.clearAlbumArtCache();
            }
        }
    };
    
    private Handler mReScanHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mAdapter != null) {
                getAlbumCursor(mAdapter.getQueryHandler(), null);
            }
        }
    };

    @Override
    public void onPause() {
        unregisterReceiver(mTrackListListener);
        mReScanHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    public void init(Cursor c) {

        if (mAdapter == null) {
            return;
        }
        mAdapter.changeCursor(c); // also sets mAlbumCursor

        if (mAlbumCursor == null) {
            MusicUtils.displayDatabaseError(this);
            closeContextMenu();
            mReScanHandler.sendEmptyMessageDelayed(0, 1000);
            return;
        }

        // restore previous position
        if (mLastListPosCourse >= 0) {
            getListView().setSelectionFromTop(mLastListPosCourse, mLastListPosFine);
            mLastListPosCourse = -1;
        }

        MusicUtils.hideDatabaseError(this);
        // SPRD 523924 get flag whether tab show
        mNeedShowQuitMenuitem = MusicUtils.updateButtonBar(this, R.id.albumtab);
        setTitle();
    }

    private void setTitle() {
        CharSequence fancyName = "";
        if (mAlbumCursor != null && mAlbumCursor.getCount() > 0) {
            mAlbumCursor.moveToFirst();
            fancyName = mAlbumCursor.getString(
                    mAlbumCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
            if (fancyName == null || fancyName.equals(MediaStore.UNKNOWN_STRING))
                fancyName = getText(R.string.unknown_artist_name);
        }

        if (mArtistId != null && fancyName != null)
            setTitle(fancyName);
        else
            setTitle(R.string.albums_title);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfoIn) {
        menu.add(0, PLAY_SELECTION, 0, R.string.play_selection);
        SubMenu sub = menu.addSubMenu(0, ADD_TO_PLAYLIST, 0, R.string.add_to_playlist);
        MusicUtils.makePlaylistMenu(this, sub);
        menu.add(0, DELETE_ITEM, 0, R.string.delete_item);

        AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfoIn;
        mAlbumCursor.moveToPosition(mi.position);
        mCurrentAlbumId = mAlbumCursor.getString(mAlbumCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID));
        mCurrentAlbumName = mAlbumCursor.getString(mAlbumCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
        mCurrentArtistNameForAlbum = mAlbumCursor.getString(
                mAlbumCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST));
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

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case PLAY_SELECTION: {
                // play the selected album
                long [] list = MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
                MusicUtils.playAll(this, list, 0);
                return true;
            }

            case QUEUE: {
                long [] list = MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
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
                long [] list = MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
                long playlist = item.getIntent().getLongExtra("playlist", 0);
                MusicUtils.addToPlaylist(this, list, playlist);
                return true;
            }
            case DELETE_ITEM: {
                long [] list = MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
                String f;
                // SPRD 476972
                f = getString(R.string.delete_album);
                String desc = String.format(f, mCurrentAlbumName);
                Bundle b = new Bundle();
                b.putString("description", desc);
                b.putLongArray("items", list);
                /* SPRD 476972 @{ */
                b.putString(MusicUtils.DeleteMode.CURRENT_TRACK_NAME, mCurrentAlbumName);
                b.putInt(MusicUtils.DeleteMode.DELETE_MODE, MusicUtils.DeleteMode.DELETE_ABLUM);
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
        String query = "";
        
        Intent i = new Intent();
        i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        title = "";
        if (!mIsUnknownAlbum) {
            query = mCurrentAlbumName;
            i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, mCurrentAlbumName);
            title = mCurrentAlbumName;
        }
        if(!mIsUnknownArtist) {
            query = query + " " + mCurrentArtistNameForAlbum;
            i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, mCurrentArtistNameForAlbum);
            title = title + " " + mCurrentArtistNameForAlbum;
        }
        // Since we hide the 'search' menu item when both album and artist are
        // unknown, the query and title strings will have at least one of those.
        i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE);
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
                } else {
                    getAlbumCursor(mAdapter.getQueryHandler(), null);
                }
                break;

            case NEW_PLAYLIST:
                if (resultCode == RESULT_OK) {
                    Uri uri = intent.getData();
                    if (uri != null) {
                        long [] list = MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
                        MusicUtils.addToPlaylist(this, list, Long.parseLong(uri.getLastPathSegment()));
                    }
                }
                break;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        // SPRD 524518
        boolean mIsSearchResult = MusicUtils.isSearchResult(getIntent());
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
        intent.putExtra("album", Long.valueOf(id).toString());
        intent.putExtra("artist", mArtistId);
        // SPRD 524518
        intent.putExtra(IS_SEARCH_RESULT, mIsSearchResult);
        startActivity(intent);
        /* SPRD 524518 */
        if(mIsSearchResult){
            finish();
        }
        /* @} */
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
        if(MusicUtils.isSearchResult(getIntent())){
            menu.removeItem(R.id.search);
        }
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
        /* SPRD 523924 check flag to remove menuitem of quit @{ */
        if (!mNeedShowQuitMenuitem) {
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

    private Cursor getAlbumCursor(AsyncQueryHandler async, String filter) {
        String[] cols = new String[] {
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ART
        };


        Cursor ret = null;
        if (mArtistId != null) {
            Uri uri = MediaStore.Audio.Artists.Albums.getContentUri("external",
                    Long.valueOf(mArtistId));
            if (!TextUtils.isEmpty(filter)) {
                uri = uri.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
            }
            if (async != null) {
                async.startQuery(0, null, uri,
                        cols, null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
            } else {
                ret = MusicUtils.query(this, uri,
                        cols, null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
            }
        } else {
            Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
            if (!TextUtils.isEmpty(filter)) {
                uri = uri.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
            }
            if (async != null) {
                async.startQuery(0, null,
                        uri,
                        cols, null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
            } else {
                ret = MusicUtils.query(this, uri,
                        cols, null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
            }
        }
        return ret;
    }
    
    static class AlbumListAdapter extends SimpleCursorAdapter implements SectionIndexer {
        
        private final Drawable mNowPlayingOverlay;
        private final BitmapDrawable mDefaultAlbumIcon;
        private int mAlbumIdx;
        private int mArtistIdx;
        private int mAlbumArtIndex;
        private final Resources mResources;
        private final StringBuilder mStringBuilder = new StringBuilder();
        /* SPRD 476972 @{ */
        private String mUnknownAlbum;
        private String mUnknownArtist;
        /* @} */
        private final String mAlbumSongSeparator;
        private final Object[] mFormatArgs = new Object[1];
        private AlphabetIndexer mIndexer;
        private AlbumBrowserActivity mActivity;
        private AsyncQueryHandler mQueryHandler;
        private String mConstraint = null;
        private boolean mConstraintIsValid = false;
        /* SPRD 476972 @{ */
        HandlerThread mWorkThread = new HandlerThread("artWorkDeamon");
        private Handler mWorkHandler = null;
        private Handler mHandler = null;
        /* @} */
        static class ViewHolder {
            TextView line1;
            TextView line2;
            ImageView play_indicator;
            ImageView icon;
        }

        class QueryHandler extends AsyncQueryHandler {
            QueryHandler(ContentResolver res) {
                super(res);
            }
            
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                //Log.i("@@@", "query complete");
                mActivity.init(cursor);
            }
        }

        /* SPRD 476972 @{ */
        protected void onContentChanged() {
            Log.i("AlbumBrowserActivity", "AlbumActivity onContentChanged");
            mActivity.mReScanHandler.removeMessages(0);
            mActivity.mReScanHandler.sendEmptyMessage(0);
        };
        /* @} */

        AlbumListAdapter(Context context, AlbumBrowserActivity currentactivity,
                int layout, Cursor cursor, String[] from, int[] to) {
            /* SPRD 476972 @{ */
            // super(context, layout, cursor, from, to);
            super(context, layout, cursor, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            /* @} */
            mActivity = currentactivity;
            mQueryHandler = new QueryHandler(context.getContentResolver());
            
            mUnknownAlbum = context.getString(R.string.unknown_album_name);
            mUnknownArtist = context.getString(R.string.unknown_artist_name);
            mAlbumSongSeparator = context.getString(R.string.albumsongseparator);

            Resources r = context.getResources();
            mNowPlayingOverlay = r.getDrawable(R.drawable.indicator_ic_mp_playing_list);

            Bitmap b = BitmapFactory.decodeResource(r, R.drawable.albumart_mp_unknown_list);
            mDefaultAlbumIcon = new BitmapDrawable(context.getResources(), b);
            // no filter or dither, it's a lot faster and we can't tell the difference
            mDefaultAlbumIcon.setFilterBitmap(false);
            mDefaultAlbumIcon.setDither(false);
            getColumnIndices(cursor);
            mResources = context.getResources();

            /* SPRD 476972 @{ */
            mWorkThread.start();
            mWorkHandler = new Handler(mWorkThread.getLooper());
            mHandler = new Handler();
            /* @} */
        }

        private void getColumnIndices(Cursor cursor) {
            if (cursor != null) {
                mAlbumIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM);
                mArtistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST);
                mAlbumArtIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART);
            }
        }
        
        public void setActivity(AlbumBrowserActivity newactivity) {
            mActivity = newactivity;
        }
        
        public AsyncQueryHandler getQueryHandler() {
            return mQueryHandler;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
           View v = super.newView(context, cursor, parent);
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
        public void bindView(View view, Context context, Cursor cursor) {
            
            ViewHolder vh = (ViewHolder) view.getTag();

            String name = cursor.getString(mAlbumIdx);
            String displayname = name;
            boolean unknown = name == null || name.equals(MediaStore.UNKNOWN_STRING); 
            if (unknown) {
                displayname = mUnknownAlbum;
            }
            vh.line1.setText(displayname);
            
            name = cursor.getString(mArtistIdx);
            displayname = name;
            if (name == null || name.equals(MediaStore.UNKNOWN_STRING)) {
                displayname = mUnknownArtist;
            }
            vh.line2.setText(displayname);

            ImageView iv = vh.icon;
            // We don't actually need the path to the thumbnail file,
            // we just use it to see if there is album art or not
            String art = cursor.getString(mAlbumArtIndex);
            long aid = cursor.getLong(0);
            if (unknown || art == null || art.length() == 0) {
                iv.setImageDrawable(null);
            } else {
                /* SPRD 476972 @{ */
                mWorkHandler.post(new AlbumBGLoadTask(context, mHandler, aid, mDefaultAlbumIcon, iv));
                /* @} */
            }
            
            long currentalbumid = MusicUtils.getCurrentAlbumId();
            iv = vh.play_indicator;
            if (currentalbumid == aid) {
                iv.setImageDrawable(mNowPlayingOverlay);
            } else {
                iv.setImageDrawable(null);
            }
        }
        
        @Override
        public void changeCursor(Cursor cursor) {
            if (mActivity.isFinishing() && cursor != null) {
                cursor.close();
                cursor = null;
            }
            /* SPRD 476972 @{ */
            if (cursor != null && cursor.isClosed()) {
                cursor = null;
                Log.e(TAG, "newCursor has closed before change cursor");
            }
            /* @} */
            if (cursor != mActivity.mAlbumCursor) {
                mActivity.mAlbumCursor = cursor;
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
            Cursor c = mActivity.getAlbumCursor(null, s);
            mConstraint = s;
            mConstraintIsValid = true;
            return c;
        }
        
        public Object[] getSections() {
            /* SPRD 476972 @{ */
            if (mIndexer != null) {
                return mIndexer.getSections();
            } else {
                return new String [] { " " };
            }
            /* @} */
        }
        
        public int getPositionForSection(int section) {
            /* SPRD 476972 @{ */
            if (mIndexer != null) {
                return mIndexer.getPositionForSection(section);
            }
            return 0;
           /* @} */
        }
        
        public int getSectionForPosition(int position) {
            return 0;
        }
        /* SPRD 476972 @{ */
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

    private Cursor mAlbumCursor;
    private String mArtistId;

    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicUtils.updateNowPlaying(this);
    }

    public void onServiceDisconnected(ComponentName name) {
        finish();
    }
    /* SPRD 475999 @{ */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // SPRD 523924 get flag whether tab show
        mNeedShowQuitMenuitem = MusicUtils.updateButtonBar(this, R.id.albumtab, newConfig.orientation);
        ListView lv = getListView();
        lv.setOnCreateContextMenuListener(this);
        lv.setTextFilterEnabled(true);
        MusicUtils.updateNowPlaying(AlbumBrowserActivity.this);
    }
    /* @} */
    class AudioContentObserver extends ContentObserver {

        private ContentResolver mContentResolver;

        public AudioContentObserver(Context context) {
            super(mReScanHandler);
            mContentResolver = context.getContentResolver();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            mReScanHandler.removeMessages(0);
            mReScanHandler.sendEmptyMessage(0);
        }

        public void registerObserver() {
            mContentResolver.registerContentObserver(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    false, this);
        }

        public void unregisterObserver(){
            mContentResolver.unregisterContentObserver(this);
        }

    }
}

