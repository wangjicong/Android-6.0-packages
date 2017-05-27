
package com.android.music;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.view.Menu;
import android.view.MenuItem;
import com.sprd.music.utils.SPRDMusicUtils;

/**
 * This activity provides a list view for all of the music .
 */
public class DelTrackChoiceActivity extends ListActivity implements
        OnItemClickListener, LoaderCallbacks<Cursor> {

    private static final String LOGTAG = "DelTrackChoiceActivity";
    private String mSortOrder;
    private String[] mCursorCols;

    private boolean mAdapterSent = false;
    private TrackListAdapter mAdapter;
    private Cursor mTrackCursor;

    // FIXME: Are they class memebers?
    private CheckBox all;
    private Button btnDel;
    private Button btnCancel;
    private Button btnCheckAll;
    private TextView checkBoxTitle;
    private RelativeLayout rl;
    private String mPlaylist;

    public static final int MENU_DEL = 2;
    public static final int MENU_CANCLE = 1;

    AsyncTask<Void, ProgressDialog, Void> task = null;

    private final static String PLAY_LIST = "playlist";
    private final static String FILE = "file";
    private final static String TRACK_ACTION = "vnd.android.cursor.dir/track";
    private final static String DISPLAY_HOME = "display_home";
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)
                    || Intent.ACTION_MEDIA_EJECT.equals(action)) {
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if(RequestPermissionsActivity.startPermissionActivity(this)){
            return;
        }

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        Intent intent = getIntent();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        if (icicle != null) {
            mPlaylist = icicle.getString(PLAY_LIST);
        } else {
            mPlaylist = intent.getStringExtra(PLAY_LIST);
        }
        setContentView(R.layout.del_track_choice);

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        f.addAction(Intent.ACTION_MEDIA_SHARED);
        f.addDataScheme(FILE);
        registerReceiver(mScanListener, f);

        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(false);
        getActionBar().setDisplayUseLogoEnabled(false);
        getActionBar().setElevation(8);
        mAdapter = (TrackListAdapter) getLastNonConfigurationInstance();
        // FIXME : Why using getLastNonConfigurationInstance ? If last
        // adapter's cursor has been close by activity and forget call
        // change cursor, it would be crashed.
        if (mAdapter != null) {
            // FIXME Try to close cursor and notify data changed here to avoid
            // crashing.
            mAdapter.swapCursor(null);
            mAdapter.notifyDataSetChanged();
            mAdapter.setActivity(this);
            mAdapter.reloadStringOnLocaleChanges();
            setListAdapter(mAdapter);
        }

        if (mAdapter == null) {
            mAdapter = new TrackListAdapter(getApplication(), // need to use
                                                              // application
                                                              // context to
                                                              // avoid leaks
                    this, null);
            setListAdapter(mAdapter);
        }
        // XXX It seems that CursorLoader query database is faster with
        // muti-task. But CursorAdapter
        // has its content observer, we must workaround about its observer (so
        // using swapCursor? but
        // observer was registered in super.swapCursor in framework... em.. )
        getLoaderManager().initLoader(0, null, this);
        mCursorCols = new String[] {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.DURATION
        };
        initializeViews();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_CANCLE, 1, R.string.cancel_add).setShowAsAction(
                MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, MENU_DEL, 1, R.string.confirm_delete).setShowAsAction(
                MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        updateMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MusicLog.i(LOGTAG, "click option menu id = " + item.getItemId());
        switch (item.getItemId()) {
            case MENU_DEL:
                delSong();
                return true;

            case MENU_CANCLE:
                finish();
                return true;
        }
        return false;
    }

    private void updateMenu(Menu menu) {
        // Hide "help" if we don't have a URI for it.
        MenuItem del = menu.findItem(MENU_DEL);
        //SPRD bug fix 519279 add null point judgment.
        if (mAdapter != null && mAdapter.hasCheckedItem()) {
            del.setEnabled(true);
        } else {
            del.setEnabled(false);
        }
    }

    private boolean mTaskIsRunning = false;

    private void delSong() {
        if (task != null) {
            MusicLog.d(LOGTAG, "task is not null, cancel it");
            task.cancel(false);
            mTaskIsRunning = false;
        }
        // AsyncQueryHandler could handle it either, because we won't use
        // onProgress...
        task = new AsyncTask<Void, ProgressDialog, Void>() {
            ProgressDialog mProgressDialog;

            @Override
            protected void onPreExecute() {
                mProgressDialog = new ProgressDialog(
                        DelTrackChoiceActivity.this);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialog.setMessage(getString(R.string.remove_track));
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
                mTaskIsRunning = true;
            }

            @Override
            protected Void doInBackground(Void... params) {
                delete();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                mProgressDialog.dismiss();
                mTaskIsRunning = false;
                Intent intent = new Intent(Intent.ACTION_EDIT);
                intent.setDataAndType(Uri.EMPTY, TRACK_ACTION);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(PLAY_LIST, mPlaylist);
                intent.putExtra(DISPLAY_HOME, true);
                startActivity(intent);
                finish();
                MusicLog.v(LOGTAG, "start and finish");
            }
        };
        task.execute((Void[]) null);

    }

    /* @} */
    /**
     * Initialize all the controls.
     */
    private void initializeViews() {
        all = (CheckBox) findViewById(R.id.checkbox_selected_all);
        all.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                for (int i = 0; i < mAdapter.getCount(); i++) {
                    mAdapter.setChecked(i, all.isChecked());
                }
                if (all.isChecked()) {
                    checkBoxTitle.setText(R.string.music_cancle_selected_all);
                } else {
                    checkBoxTitle.setText(R.string.music_selected_all);
                }
                mAdapter.notifyDataSetChanged();
                invalidateOptionsMenu();
            }
        });
        btnCheckAll = (Button) findViewById(R.id.btn_checkall);
        btnCheckAll.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                all.setChecked(!all.isChecked());
                for (int i = 0; i < mAdapter.getCount(); i++) {
                    mAdapter.setChecked(i, all.isChecked());
                }
                mAdapter.notifyDataSetChanged();
                invalidateOptionsMenu();
            }
        });
        invalidateOptionsMenu();
        checkBoxTitle = (TextView) findViewById(R.id.CheckBoxTitle);
        rl = (RelativeLayout) findViewById(R.id.CheckboxLinearLayout);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        // FIXME It is dangerous to retain a adapter with a close cursor.
        TrackListAdapter a = mAdapter;
        mAdapterSent = true;
        return a;
    }

    @Override
    public void onResume() {
        super.onResume();
        MusicLog.i(LOGTAG, "mTrackCursor: " + mTrackCursor + " mTaskIsRunning: "
                + mTaskIsRunning);
        if (mTrackCursor != null && !mTaskIsRunning) {
            getListView().invalidateViews();
        }
        MusicUtils.setSpinnerState(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        MusicLog.i(LOGTAG, "onPause");
    }

    public void onSaveInstanceState(Bundle outcicle) {
        outcicle.putString(PLAY_LIST, mPlaylist);
        super.onSaveInstanceState(outcicle);
    }

    @Override
    public void onDestroy() {
        MusicLog.i(LOGTAG, "onDestroy");
        // Because we pass the adapter to the next activity, we need to make
        // sure it doesn't keep a reference to this activity. We can do this
        // by clearing its DatasetObservers, which setListAdapter(null) does.
        /* SPRD bug fix 519279 add null point judgment. @{ */
        if (getListView() != null)
            setListAdapter(null);
        /* @} */

        if (!mAdapterSent && mAdapter != null) {
            if (null != mTrackCursor) {
                mTrackCursor.close();
            }
            mAdapter.swapCursor(null);
            mAdapter = null;
        }
        try{
            unregisterReceiver(mScanListener);
        }catch(Exception e){
            MusicLog.i(LOGTAG, "onDestroy unregisterReceiver error");
        }
        super.onDestroy();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        MusicLog.i(LOGTAG, "onCreateLoader");
        mSortOrder = MediaStore.Audio.Media.TITLE_KEY;
        StringBuilder where = new StringBuilder();
        where.append(MediaStore.Audio.Media.TITLE + " != ''");
        where.append(" AND " + MediaStore.Audio.Media.IS_MUSIC + "=1");
        // Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external",
                Long.parseLong(mPlaylist));
        CursorLoader loader = new CursorLoader(this, uri, mCursorCols,
                where.toString(), null, mSortOrder);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        MusicLog.i(LOGTAG, "onLoaderFinish");
        if (isFinishing()) {
            setListAdapter(null);
            if (mAdapter != null) {
                mAdapter.changeCursor(null);
            }
            return;
        }
        init(cursor, false);
        if (mAdapter.getCount() != 0
                && mAdapter.getCheckedCount() == mAdapter.getCount()) {
            checkBoxTitle.setText(R.string.music_cancle_selected_all);
        } else {
            checkBoxTitle.setText(R.string.music_selected_all);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        MusicLog.i(LOGTAG, "onLoaderReset");
        if (mAdapter != null) {
            int loaderID = loader.getId();
            if (loaderID == 0 && getLoaderManager().getLoader(1) == null) {
                mAdapter.swapCursor(null);
            } else if (loaderID == 1) {
                mAdapter.swapCursor(null);
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        mAdapter.setChecked(position, !mAdapter.isChecked(position));
        if (mAdapter.getCheckedCount() == mAdapter.getCount()) {
            all.setChecked(true);
        } else {
            all.setChecked(false);
        }
        mAdapter.notifyDataSetChanged();
        invalidateOptionsMenu();
    }

    private void updateDeleteButton() {
        if (mAdapter.hasCheckedItem()) {
            btnDel.setClickable(true);
            btnDel.setFocusable(true);
            btnDel.setEnabled(true);
        } else {
            btnDel.setClickable(false);
            btnDel.setFocusable(false);
            btnDel.setEnabled(false);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        mAdapter.setChecked(position, !mAdapter.isChecked(position));
        if (mAdapter.getCheckedCount() == mAdapter.getCount()) {
            all.setChecked(true);
            checkBoxTitle.setText(R.string.music_cancle_selected_all);
        } else {
            all.setChecked(false);
            checkBoxTitle.setText(R.string.music_selected_all);
        }
        mAdapter.notifyDataSetChanged();
        invalidateOptionsMenu();
    }

    public void init(Cursor newCursor, boolean isLimited) {

        if (mAdapter == null) {
            return;
        }
        if (newCursor != null && newCursor.isClosed()) {
            return;
        }
        mAdapter.swapCursor(newCursor); // also sets mTrackCursor
        if (mTrackCursor == null) {
            closeContextMenu();
            all.setVisibility(View.GONE);
            btnCheckAll.setVisibility(View.GONE);
            btnCheckAll.setEnabled(false);
            btnCheckAll.setClickable(false);
            checkBoxTitle.setVisibility(View.GONE);
            rl.setVisibility(View.GONE);
        } else {
            MusicUtils.hideDatabaseError(DelTrackChoiceActivity.this);
            all.setVisibility(View.VISIBLE);
            btnCheckAll.setVisibility(View.VISIBLE);
            btnCheckAll.setEnabled(false);
            btnCheckAll.setClickable(false);
            checkBoxTitle.setVisibility(View.VISIBLE);
            rl.setVisibility(View.VISIBLE);
            invalidateOptionsMenu();
        }
    }

    static class TrackListAdapter extends CursorAdapter {

        int mTitleIdx;
        int mArtistIdx;
        int mDurationIdx;
        int mAudioIdIdx;

        private final StringBuilder mBuilder = new StringBuilder();
        private String mUnknownArtist;
        private LayoutInflater mInflater;

        private DelTrackChoiceActivity mActivity = null;

        static class ViewHolder {
            TextView line1;
            TextView line2;
            TextView duration;
            CheckBox checkbox;
            CharArrayBuffer buffer1;
            char[] buffer2;
        }

        TrackListAdapter(Context context,
                DelTrackChoiceActivity currentactivity, Cursor cursor) {
            super(context, cursor, 0);
            mInflater = LayoutInflater.from(context);
            mActivity = currentactivity;
            getColumnIndices(cursor);
            mUnknownArtist = context.getString(R.string.unknown_artist_name);
        }

        public void setActivity(DelTrackChoiceActivity newactivity) {
            mActivity = newactivity;
        }

        private void getColumnIndices(Cursor cursor) {
            if (cursor != null) {
                mTitleIdx = cursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                mArtistIdx = cursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                mDurationIdx = cursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                try {
                    mAudioIdIdx = cursor
                            .getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID);
                } catch (IllegalArgumentException ex) {
                    mAudioIdIdx = cursor
                            .getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                }

            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return null;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            return;
        }

        // TODO: ViewHolder already prepared in {@code
        // CursorAdapter#getView(int,View,ViewGroup)}.
        @Override
        public View getView(final int position, View convertView,
                ViewGroup parent) {
            ViewHolder vh = new ViewHolder();
            if (convertView == null) {
                convertView = mInflater.inflate(
                        R.layout.multi_track_choice_list, null);
            }
            vh.line1 = (TextView) convertView.findViewById(R.id.line1);
            vh.line2 = (TextView) convertView.findViewById(R.id.line2);
            vh.duration = (TextView) convertView.findViewById(R.id.duration);
            vh.checkbox = (CheckBox) convertView
                    .findViewById(R.id.music_checkbox_selected);
            vh.buffer1 = new CharArrayBuffer(100);
            vh.buffer2 = new char[200];

            final Cursor cursor = (Cursor) getItem(position);
            cursor.copyStringToBuffer(mTitleIdx, vh.buffer1);
            vh.line1.setText(vh.buffer1.data, 0, vh.buffer1.sizeCopied);

            int millSecs = cursor.getInt(mDurationIdx);
            if (millSecs == 0) {
                vh.duration.setText("");
            } else {
                vh.duration.setText(MusicUtils.makeTimeString(
                        convertView.getContext(), millSecs/1000));
            }
            vh.checkbox.setVisibility(View.VISIBLE);
            vh.checkbox.setChecked(isChecked(position));
            vh.checkbox.setClickable(false);
            final StringBuilder builder = mBuilder;
            builder.delete(0, builder.length());

            String name = cursor.getString(mArtistIdx);
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
            convertView.setTag(getItemId(position));
            return convertView;
        }

        // TODO: How about old cursor? It has been closed? Too much swapCursor
        // called, please ensure
        // all old cursors has been closed, If we won't ensure, please use
        // {@code CursorAdapter#changeCursor(Cursor)}
        @Override
        public Cursor swapCursor(Cursor cursor) {
            mActivity.mTrackCursor = cursor;
            Cursor oldCursor = super.swapCursor(cursor);
            getColumnIndices(cursor);
            return oldCursor;
        }

        private HashMap<Long, Long> mCheckMap = new HashMap<Long, Long>();

        public void setCheckedArray(HashMap<Long, Long> checkedArray) {
            mCheckMap = checkedArray;
        }

        public long[] getCheckedIdArray() {
            long[] ids = new long[mCheckMap.size()];
            int pos = 0;
            for (int i = getCount() - 1; i >= 0; i--) {
                if (isChecked(i)) {
                    ids[pos++] = getItemId(i);
                }
            }
            return ids;
        }

        public boolean hasCheckedItem() {
            return mCheckMap.size() > 0;
        }

        public boolean isChecked(int position) {
            long id = getItemId(position);
            return mCheckMap.containsValue(id);
        }

        public int getCheckedCount() {
            return mCheckMap.size();
        }

        public void setChecked(int position, boolean checked) {
            long id = getItemId(position);
            if (checked) {
                mCheckMap.put(id, id);
            } else {
                mCheckMap.remove(id);
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
        }

    }

    private void delete() {
        long[] ids = mAdapter.getCheckedIdArray();
        if (ids != null && ids.length > 0) {
            Uri uri = MediaStore.Audio.Playlists.Members.getContentUri(
                    "external", Long.valueOf(mPlaylist));
            StringBuilder where = new StringBuilder("_id in (");
            for (long id : ids) {
                where.append(id);
                where.append(',');
            }
            where.deleteCharAt(where.length() - 1).append(')');
            getContentResolver().delete(uri, where.toString(), null);
        }
    }

}
