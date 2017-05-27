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
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteException;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TabWidget;
import java.text.Collator;
import java.util.ArrayList;

//SPRD:add for searchview
import android.app.SearchableInfo;
import android.widget.SearchView;
import android.graphics.Color;
import android.app.SearchManager;
import com.sprd.music.utils.SPRDMusicUtils;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_PHONE_STATE;

/* SPRD bug fix 519898 need check runtime permisssion before use @{ */
import android.content.pm.PackageManager;
import android.widget.Toast;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
/* @} */

public class PlaylistBrowserActivity extends ListActivity
    implements View.OnCreateContextMenuListener, MusicUtils.Defs
{
    private static final String TAG = "PlaylistBrowserActivity";
    private static final int DELETE_PLAYLIST = CHILD_MENU_BASE + 1;
    private static final int EDIT_PLAYLIST = CHILD_MENU_BASE + 2;
    private static final int RENAME_PLAYLIST = CHILD_MENU_BASE + 3;
    private static final int CHANGE_WEEKS = CHILD_MENU_BASE + 4;
    private static final long RECENTLY_ADDED_PLAYLIST = -1;
    private static final long ALL_SONGS_PLAYLIST = -2;
    private static final long PODCASTS_PLAYLIST = -3;
    private PlaylistListAdapter mAdapter;
    boolean mAdapterSent;
    private static int mLastListPosCourse = -1;
    private static int mLastListPosFine = -1;

    private boolean mCreateShortcut;
    private ServiceToken mToken;
    /* SPRD 476969 @{ */
    private MenuItem mSearchItem;
    private SearchView mSearchView;
    /* @} */
    private String mAction;
    private Intent intent;
    private final String UNKOWN_ACTION = "unkonwn_action";
    public PlaylistBrowserActivity()
    {
    }

    /* SPRD bugfix 516725 @{ */
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1;
    private static final int PHONE_PERMISSION_REQUEST_CODE = 2;
    /* @} */

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        /* SPRD bugfix 516725 @{ */
        /*if(RequestPermissionsActivity.startPermissionActivity(this)){
            return;
        }*/
        /* @} */

        intent = getIntent();
        /* SPRD 475999 @{ */
        //final String action = intent.getAction();
        if(intent != null){
            mAction = intent.getAction();
        }else{
            mAction = UNKOWN_ACTION;
        }
        Log.i(TAG,"onCreate action =" +mAction);
        /* @} */
        if (Intent.ACTION_CREATE_SHORTCUT.equals(mAction)) {
            mCreateShortcut = true;
        }

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        /* SPRD 475999 @{ */
        ActionBar actionbar = getActionBar();
        actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
        // SPRD 520090 music title translation error
        actionbar.setTitle(R.string.musicbrowserlabel);
        actionbar.setDisplayUseLogoEnabled(false);
        actionbar.setDisplayShowHomeEnabled(false);
        /* @} */
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.media_picker_activity);
        /* SPRD #521649 tabwidget NullPointerException */
        try {
            if (intent != null) {
                if (!intent.getBooleanExtra("withtabs", false)) {
                    TabWidget ll = (TabWidget) this.findViewById(R.id.buttonbar);
                    if (ll != null) {
                        Log.d(TAG, "Set currently tabwidget 0");
                        ll.setCurrentTab(0);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, " Set currently tabwidget 0 error");
        }
        /* @} */
        checkAndRequestPermission();
    }

    /* SPRD bugfix 522172 @{ */
    private boolean checkAndRequestPermission() {
        if (checkSelfPermission(READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            init();
            return true;
        } else if (checkSelfPermission(READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { READ_PHONE_STATE }, STORAGE_PERMISSION_REQUEST_CODE);
            return false;
        } else if (checkSelfPermission(READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { READ_EXTERNAL_STORAGE }, STORAGE_PERMISSION_REQUEST_CODE);
            return false;
        } else {
            requestPermissions(new String[] { READ_PHONE_STATE }, PHONE_PERMISSION_REQUEST_CODE);
            requestPermissions(new String[] { READ_EXTERNAL_STORAGE }, STORAGE_PERMISSION_REQUEST_CODE);
            return false;
        }
    }
    /* @} */

    /* SPRD bugfix 516725 @{ */
    private void init() {
        mToken = MusicUtils.bindToService(this, new ServiceConnection() {
            public void onServiceConnected(ComponentName classname, IBinder obj) {
                // SPRD 475999
                if (Intent.ACTION_VIEW.equals(mAction)) {
                    Bundle b = intent.getExtras();
                    if (b == null) {
                        Log.w(TAG, "Unexpected:getExtras() returns null.");
                    } else {
                        try {
                            long id = Long.parseLong(b.getString("playlist"));
                            Log.i(TAG,"onServiceConnected =" + id);
                            if (id == RECENTLY_ADDED_PLAYLIST) {
                                playRecentlyAdded();
                            } else if (id == PODCASTS_PLAYLIST) {
                                playPodcasts();
                            } else if (id == ALL_SONGS_PLAYLIST) {
                                long[] list = MusicUtils.getAllSongs(PlaylistBrowserActivity.this);
                                if (list != null) {
                                    MusicUtils.playAll(PlaylistBrowserActivity.this, list, 0);
                                }
                            } else {
                                MusicUtils.playPlaylist(PlaylistBrowserActivity.this, id);
                            }
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Playlist id missing or broken");
                        }
                    }
                    finish();
                    return;
                }
                MusicUtils.updateNowPlaying(PlaylistBrowserActivity.this);
            }

            public void onServiceDisconnected(ComponentName classname) {
                // SPRD 476973
                finish();
            }
        
        });
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        /* SPRD 476972 @{ */
        f.addAction(Intent.ACTION_MEDIA_SHARED);
        f.addDataScheme("file");
        registerReceiver(mScanListener, f);
        /* SPRD 476972 @{ */
        IntentFilter f2 = new IntentFilter();
        f2.addAction(Intent.ACTION_LOCALE_CHANGED);
        registerReceiver(mLocaleChangeListener, f2);
        /* @} */
        MusicUtils.updateButtonBar(this, R.id.playlisttab);
        ListView lv = getListView();
        lv.setOnCreateContextMenuListener(this);
        lv.setTextFilterEnabled(true);

        mAdapter = (PlaylistListAdapter) getLastNonConfigurationInstance();
        if (mAdapter == null) {
            //Log.i("@@@", "starting query");
            mAdapter = new PlaylistListAdapter(
                    getApplication(),
                    this,
                    R.layout.track_list_item,
                    mPlaylistCursor,
                    new String[] { MediaStore.Audio.Playlists.NAME},
                    new int[] { android.R.id.text1 });
            setListAdapter(mAdapter);
            setTitle(R.string.working_playlists);
            getPlaylistCursor(mAdapter.getQueryHandler(), null);
        } else {
            mAdapter.setActivity(this);
            setListAdapter(mAdapter);
            mPlaylistCursor = mAdapter.getCursor();
            // If mPlaylistCursor is null, this can be because it doesn't have
            // a cursor yet (because the initial query that sets its cursor
            // is still in progress), or because the query failed.
            // In order to not flash the error dialog at the user for the
            // first case, simply retry the query when the cursor is null.
            // Worst case, we end up doing the same query twice.
            if (mPlaylistCursor != null) {
                init(mPlaylistCursor);
            } else {
                setTitle(R.string.working_playlists);
                getPlaylistCursor(mAdapter.getQueryHandler(), null);
            }
        }
    }

    /* SPRD bugfix 516725 @{ */
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        boolean isPermitted = true;
        switch (requestCode) {
        case STORAGE_PERMISSION_REQUEST_CODE:
            if(grantResults.length > 0){
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        isPermitted = false;
                        showConfirmDialog();
                        break;
                    }
                }
            if (isPermitted)
                init();
            } else {
                showConfirmDialog();
            }
            break;
        default:
            break;
        }
    }

    public void showConfirmDialog() {
        AlertDialog builder = new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.toast_fileexplorer_internal_error))
                .setMessage(getResources().getString(R.string.error_permissions))
                .setCancelable(false)
                .setOnKeyListener(new Dialog.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            finish();
                        }
                        return true;
                    }
                })
                .setPositiveButton(getResources().getString(R.string.dialog_dismiss),
                       new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        }).show();
    }
    /* @} */

    @Override
    public Object onRetainNonConfigurationInstance() {
        PlaylistListAdapter a = mAdapter;
        mAdapterSent = true;
        return a;
    }
    
    @Override
    public void onDestroy() {
        Log.i(TAG,"onDestroy");
        ListView lv = getListView();
        if (lv != null) {
            mLastListPosCourse = lv.getFirstVisiblePosition();
            View cv = lv.getChildAt(0);
            if (cv != null) {
                mLastListPosFine = cv.getTop();
            }
        }
        // SPRD 524518
        MusicUtils.unbindFromService(mToken,this);
        // If we have an adapter and didn't send it off to another activity yet, we should
        // close its cursor, which we do by assigning a null cursor to it. Doing this
        // instead of closing the cursor directly keeps the framework from accessing
        // the closed cursor later.
        if (!mAdapterSent && mAdapter != null) {
            /* SPRD 476972 @{ */
            if (mPlaylistCursor != null) {
                mPlaylistCursor.close();
            }
            /* @} */
            mAdapter.changeCursor(null);
        }
        // Because we pass the adapter to the next activity, we need to make
        // sure it doesn't keep a reference to this activity. We can do this
        // by clearing its DatasetObservers, which setListAdapter(null) does.

        if(getListView() != null) {
            setListAdapter(null);
        }
        mAdapter = null;
        try{
            unregisterReceiver(mScanListener);
            unregisterReceiver(mLocaleChangeListener);
        } catch(Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        /* SPRD bug fix 522172 @{ */
        if (checkSelfPermission(READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        /* @} */
        /* SPRD 476972 @{ */
        if(mAdapter != null){
            mAdapter.notifyDataSetChanged();
        }
        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.META_CHANGED);
        f.addAction(MediaPlaybackService.QUEUE_CHANGED);
        registerReceiver(mTrackListListener, f);
        mTrackListListener.onReceive(null, null);
        /* @} */
        MusicUtils.setSpinnerState(this);
        MusicUtils.updateNowPlaying(PlaylistBrowserActivity.this);
    }
    @Override
    public void onPause() {
        mReScanHandler.removeCallbacksAndMessages(null);
        /* SPRD 476972 @{ */
        try {
            unregisterReceiver(mTrackListListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onPause();
    }
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MusicUtils.setSpinnerState(PlaylistBrowserActivity.this);
            //SPRD:add
            mReScanHandler.removeCallbacksAndMessages(null);
            mReScanHandler.sendEmptyMessage(0);
        }
    };
    
    private Handler mReScanHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mAdapter != null) {
                getPlaylistCursor(mAdapter.getQueryHandler(), null);
            }
        }
    };
    public void init(Cursor cursor) {

        if (mAdapter == null) {
            return;
        }
        mAdapter.changeCursor(cursor);

        if (mPlaylistCursor == null) {
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
        MusicUtils.updateButtonBar(this, R.id.playlisttab);
        setTitle();
    }

    private void setTitle() {
        setTitle(R.string.playlists_title);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* SPRD 476969 @{ */
        getMenuInflater().inflate(R.menu.options_menu_overlay, menu);
        mSearchItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) mSearchItem.getActionView();
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchView.setQueryHint(getResources().getString(R.string.search_title_hint));
        if (searchManager != null) {
            SearchableInfo info = searchManager.getSearchableInfo(this.getComponentName());
            mSearchView.setSearchableInfo(info);
        }
        int id = mSearchView.getContext().getResources()
                .getIdentifier("android:id/search_src_text", null, null);
        TextView textView = (TextView) mSearchView.findViewById(id);
        textView.setTextColor(Color.WHITE);
          /* @} */
        if (!mCreateShortcut) {
            menu.add(0, PARTY_SHUFFLE, 0, R.string.party_shuffle); // icon will be set in onPrepareOptionsMenu()
        }
        // SPRD 475999
        menu.add(0, NEW_PLAYLIST, 0, R.string.new_playlist).setIcon(android.R.drawable.ic_menu_add);
        // SPRD 476973
        menu.add(0, QUIT_MUSIC, 0, R.string.quit);
        return super.onCreateOptionsMenu(menu);
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
        if (checkSelfPermission(READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.error_permissions, Toast.LENGTH_SHORT).show();
            return true;
        }
        /* @} */
        Intent intent;
        switch (item.getItemId()) {
            case PARTY_SHUFFLE:
                MusicUtils.togglePartyShuffle();
                break;
            /* SPRD 475999 @{ */
            case NEW_PLAYLIST:
                intent = new Intent();
                intent.setClass(this, CreatePlaylist.class);
                startActivityForResult(intent, NEW_PLAYLIST);
                break;
            /* @} */
            /* SPRD 476973 @{ */
            case QUIT_MUSIC:
                SPRDMusicUtils.quitservice(this);
                break;
            /* @} */
        }
        return super.onOptionsItemSelected(item);
    }
    
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfoIn) {
        if (mCreateShortcut) {
            return;
        }

        AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfoIn;

        menu.add(0, PLAY_SELECTION, 0, R.string.play_selection);

        if (mi.id >= 0 /*|| mi.id == PODCASTS_PLAYLIST*/) {
            menu.add(0, DELETE_PLAYLIST, 0, R.string.delete_playlist_menu);
        }

        if (mi.id == RECENTLY_ADDED_PLAYLIST) {
            menu.add(0, EDIT_PLAYLIST, 0, R.string.edit_playlist_menu);
        }

        if (mi.id >= 0) {
            menu.add(0, RENAME_PLAYLIST, 0, R.string.rename_playlist_menu);
        }

        mPlaylistCursor.moveToPosition(mi.position);
        menu.setHeaderTitle(mPlaylistCursor.getString(mPlaylistCursor.getColumnIndexOrThrow(
                MediaStore.Audio.Playlists.NAME)));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo mi = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case PLAY_SELECTION:
                if (mi.id == RECENTLY_ADDED_PLAYLIST) {
                    playRecentlyAdded();
                } else if (mi.id == PODCASTS_PLAYLIST) {
                    playPodcasts();
                } else {
                    MusicUtils.playPlaylist(this, mi.id);
                }
                break;
            case DELETE_PLAYLIST:
                /* SPRD 476972 @{ */
               /*Uri uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mi.id);
                getContentResolver().delete(uri, null, null);
                Toast.makeText(this, R.string.playlist_deleted_message, Toast.LENGTH_SHORT).show();
                if (mPlaylistCursor.getCount() == 0) {
                   setTitle(R.string.no_playlists_title);
                }
                */
                deletePlaylistDialog(PlaylistBrowserActivity.this, mi);
                /*@} */
                break;
            case EDIT_PLAYLIST:
                if (mi.id == RECENTLY_ADDED_PLAYLIST) {
                    Intent intent = new Intent();
                    intent.setClass(this, WeekSelector.class);
                    startActivityForResult(intent, CHANGE_WEEKS);
                    return true;
                } else {
                    Log.e(TAG, "should not be here");
                }
                break;
            case RENAME_PLAYLIST:
                Intent intent = new Intent();
                intent.setClass(this, RenamePlaylist.class);
                intent.putExtra("rename", mi.id);
                startActivityForResult(intent, RENAME_PLAYLIST);
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case SCAN_DONE:
                if (resultCode == RESULT_CANCELED) {
                    finish();
                } else if (mAdapter != null) {
                    getPlaylistCursor(mAdapter.getQueryHandler(), null);
                }
                break;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        if (mCreateShortcut) {
            Log.i(TAG,"onListItemClick = "+mCreateShortcut);
            /* SPRD 476972 @{ */
            final Intent shortcut = new Intent();
            shortcut.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            shortcut.setClass(PlaylistBrowserActivity.this, TrackBrowserActivity.class);
            if (id == RECENTLY_ADDED_PLAYLIST) {
                shortcut.putExtra("playlist", "recentlyadded");
            } else if (id == PODCASTS_PLAYLIST) {
                shortcut.putExtra("playlist", "podcasts");
            } else if (id == ALL_SONGS_PLAYLIST) {
                shortcut.putExtra("playall", true);
            } else {
                shortcut.putExtra("playlist", Long.valueOf(id).toString());
            }
            /* @} */
            final Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcut);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, ((TextView) v.findViewById(R.id.line1)).getText());
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(
                    this, R.drawable.ic_launcher_shortcut_music_playlist));

            setResult(RESULT_OK, intent);
            finish();
            return;
        }
        if (id == RECENTLY_ADDED_PLAYLIST) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
            intent.putExtra("playlist", "recentlyadded");
            startActivity(intent);
        } else if (id == PODCASTS_PLAYLIST) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
            intent.putExtra("playlist", "podcasts");
            startActivity(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_EDIT);
            intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
            intent.putExtra("playlist", Long.valueOf(id).toString());
            startActivity(intent);
        }
    }

    private void playRecentlyAdded() {
        // do a query for all songs added in the last X weeks
        int X = MusicUtils.getIntPref(this, "numweeks", 2) * (3600 * 24 * 7);
        final String[] ccols = new String[] { MediaStore.Audio.Media._ID};
        String where = MediaStore.MediaColumns.DATE_ADDED + ">" + (System.currentTimeMillis() / 1000 - X);
        Cursor cursor = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                ccols, where, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        
        if (cursor == null) {
            // Todo: show a message
            return;
        }
        try {
            int len = cursor.getCount();
            long [] list = new long[len];
            for (int i = 0; i < len; i++) {
                cursor.moveToNext();
                list[i] = cursor.getLong(0);
            }
            MusicUtils.playAll(this, list, 0);
        } catch (SQLiteException ex) {
        } finally {
            cursor.close();
        }
    }

    private void playPodcasts() {
        // do a query for all files that are podcasts
        final String[] ccols = new String[] { MediaStore.Audio.Media._ID};
        Cursor cursor = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                ccols, MediaStore.Audio.Media.IS_PODCAST + "=1",
                null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        
        if (cursor == null) {
            // Todo: show a message
            return;
        }
        try {
            int len = cursor.getCount();
            long [] list = new long[len];
            for (int i = 0; i < len; i++) {
                cursor.moveToNext();
                list[i] = cursor.getLong(0);
            }
            MusicUtils.playAll(this, list, 0);
        } catch (SQLiteException ex) {
        } finally {
            cursor.close();
        }
    }

    
    String[] mCols = new String[] {
            MediaStore.Audio.Playlists._ID,
            MediaStore.Audio.Playlists.NAME
    };

    private Cursor getPlaylistCursor(AsyncQueryHandler async, String filterstring) {

        StringBuilder where = new StringBuilder();
        where.append(MediaStore.Audio.Playlists.NAME + " != ''");
        
        // Add in the filtering constraints
        String [] keywords = null;
        if (filterstring != null) {
            String [] searchWords = filterstring.split(" ");
            keywords = new String[searchWords.length];
            Collator col = Collator.getInstance();
            col.setStrength(Collator.PRIMARY);
            for (int i = 0; i < searchWords.length; i++) {
                keywords[i] = '%' + searchWords[i] + '%';
            }
            for (int i = 0; i < searchWords.length; i++) {
                where.append(" AND ");
                where.append(MediaStore.Audio.Playlists.NAME + " LIKE ?");
            }
        }
        
        String whereclause = where.toString();
        
        
        if (async != null) {
            async.startQuery(0, null, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    mCols, whereclause, keywords, MediaStore.Audio.Playlists.NAME);
            return null;
        }
        Cursor c = null;
        c = MusicUtils.query(this, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                mCols, whereclause, keywords, MediaStore.Audio.Playlists.NAME);
        
        return mergedCursor(c);
    }
    
    private Cursor mergedCursor(Cursor c) {
        if (c == null) {
            return null;
        }
        if (c instanceof MergeCursor) {
            // this shouldn't happen, but fail gracefully
            Log.d("PlaylistBrowserActivity", "Already wrapped");
            return c;
        }
        MatrixCursor autoplaylistscursor = new MatrixCursor(mCols);
        if (mCreateShortcut) {
            ArrayList<Object> all = new ArrayList<Object>(2);
            all.add(ALL_SONGS_PLAYLIST);
            all.add(getString(R.string.play_all));
            autoplaylistscursor.addRow(all);
        }
        ArrayList<Object> recent = new ArrayList<Object>(2);
        recent.add(RECENTLY_ADDED_PLAYLIST);
        recent.add(getString(R.string.recentlyadded));
        autoplaylistscursor.addRow(recent);
        
        // check if there are any podcasts
        Cursor counter = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[] {"count(*)"}, "is_podcast=1", null, null);
        if (counter != null) {
            counter.moveToFirst();
            int numpodcasts = counter.getInt(0);
            counter.close();
            if (numpodcasts > 0) {
                ArrayList<Object> podcasts = new ArrayList<Object>(2);
                podcasts.add(PODCASTS_PLAYLIST);
                podcasts.add(getString(R.string.podcasts_listitem));
                autoplaylistscursor.addRow(podcasts);
            }
        }

        Cursor cc = new MergeCursor(new Cursor [] {autoplaylistscursor, c});
        return cc;
    }
    
    static class PlaylistListAdapter extends SimpleCursorAdapter {
        int mTitleIdx;
        int mIdIdx;
        private PlaylistBrowserActivity mActivity = null;
        private AsyncQueryHandler mQueryHandler;
        private String mConstraint = null;
        private boolean mConstraintIsValid = false;
        /* SPRD 476972 @{ */
        private int mlayout;
        private Context mContext;
        /* @} */
        class QueryHandler extends AsyncQueryHandler {
            QueryHandler(ContentResolver res) {
                super(res);
            }
            
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                //Log.i("@@@", "query complete: " + cursor.getCount() + " " + mActivity);
                if (cursor != null) {
                    /* SPRD 476972 @{ */
                    final Cursor cur = cursor;
                    cursor = mActivity.mergedCursor(cursor);
                }
                mActivity.init(cursor);
            }
        }

        PlaylistListAdapter(Context context, PlaylistBrowserActivity currentactivity,
                int layout, Cursor cursor, String[] from, int[] to) {
            super(context, layout, cursor, from, to);
            /* SPRD 476972 @{ */
            mlayout = layout;
            mContext = context;
            /* @} */
            mActivity = currentactivity;
            getColumnIndices(cursor);
            mQueryHandler = new QueryHandler(context.getContentResolver());
        }
        private void getColumnIndices(Cursor cursor) {
            if (cursor != null) {
                mTitleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME);
                mIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID);
            }
        }

        public void setActivity(PlaylistBrowserActivity newactivity) {
            mActivity = newactivity;
        }
        
        public AsyncQueryHandler getQueryHandler() {
            return mQueryHandler;
        }
        /* SPRD 448806 @{ */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null){
                convertView = LayoutInflater.from(mContext).inflate(mlayout,parent,false);
            }
            Cursor cursor = getCursor();
            if (cursor == null || !cursor.moveToPosition(position)) {
                Log.i("TAG", "getView cursor error");
                return convertView;
            }
            long id = cursor.getLong(mIdIdx);
            ImageView iv = (ImageView) convertView.findViewById(R.id.icon);
            if (id == RECENTLY_ADDED_PLAYLIST) {
                iv.setImageResource(R.drawable.ic_mp_playlist_recently_added_list);
            } else {
                iv.setImageResource(R.drawable.ic_mp_playlist_list);
            }
            iv = (ImageView) convertView.findViewById(R.id.play_indicator);
            iv.setVisibility(View.GONE);

            convertView.findViewById(R.id.line2).setVisibility(View.GONE);
            if(cursor != null){
                TextView tv = (TextView) convertView.findViewById(R.id.line1);
                if(mTitleIdx > -1){
                    String name = cursor.getString(mTitleIdx);
                    tv.setText(name);
                }
            }
            return convertView;
        }
        /* @} */
        @Override
        public void changeCursor(Cursor cursor) {
            Log.i(TAG,"changeCursor ");
            if (mActivity.isFinishing() && cursor != null) {
                cursor.close();
                cursor = null;
            }
            if (cursor != mActivity.mPlaylistCursor) {
                mActivity.mPlaylistCursor = cursor;
                super.changeCursor(cursor);
                getColumnIndices(cursor);
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
            Cursor c = mActivity.getPlaylistCursor(null, s);
            mConstraint = s;
            mConstraintIsValid = true;
            return c;
        }
    }
    
    private Cursor mPlaylistCursor;
    /* SPRD 476972 @{ */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        /* SPRD 497265  when change configuraion select right tab@{ */
        MusicUtils.updateButtonBar(this, R.id.playlisttab, newConfig.orientation);
        /* @} */
        ListView lv = getListView();
        lv.setOnCreateContextMenuListener(this);
        lv.setTextFilterEnabled(true);
        MusicUtils.updateNowPlaying(PlaylistBrowserActivity.this);
    }
    /* SPRD 476972 @{ */
    private BroadcastReceiver mLocaleChangeListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(mAdapter != null) {
                getPlaylistCursor(mAdapter.getQueryHandler(), null);
            }
        }
    };

    private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getListView().invalidateViews();
            MusicUtils.updateNowPlaying(PlaylistBrowserActivity.this);
        }
    };

    private void deletePlaylistDialog(final Context context, final AdapterContextMenuInfo mi) {
        String listName = nameForId(mi.id);
        String message = context.getResources().getString(R.string.delete_playlist_message,
                listName);
        Dialog alertDialog = new AlertDialog.Builder(this).
                setTitle(R.string.delete_playlist_menu).
                setMessage(message).
                setIcon(null).
                setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri uri = ContentUris.withAppendedId(
                                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mi.id);
                        getContentResolver().delete(uri, null, null);
                        Toast.makeText(context, R.string.playlist_deleted_message,
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                }).
                setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).
                create();
        alertDialog.show();
    }

    private String nameForId(long id) {
        Cursor c = MusicUtils.query(this, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                new String[] {
                    MediaStore.Audio.Playlists.NAME
                },
                MediaStore.Audio.Playlists._ID + "=?",
                new String[] {
                    Long.valueOf(id).toString()
                },
                MediaStore.Audio.Playlists.NAME);
        String name = null;
        if (c != null) {
            c.moveToFirst();
            if (!c.isAfterLast()) {
                name = c.getString(0);
            }
            c.close();
        }
        return name;
    }
    /* @} */
}

