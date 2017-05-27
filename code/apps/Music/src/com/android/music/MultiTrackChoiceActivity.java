/*
 * Name        : MultiTrackChoiceActivity.java
 * Author      : zbk
 * Copyright   : Copyright (c)
 * Description : MultiTrackChoiceActivity.java -
 * Review      :
 */

package com.android.music;

import java.util.HashMap;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.sprd.music.utils.SPRDMusicUtils;

/**
 * This activity provides a list view for all of the music .
 */
public class MultiTrackChoiceActivity extends ListActivity implements
        OnItemClickListener {

    private static final String LOGTAG = "MultiTrackChoiceActivity";
    private String mSortOrder;
    private String[] mCursorCols;

    private boolean mAdapterSent = false;
    private TrackListAdapter mAdapter;
    private Cursor mTrackCursor;

    private CheckBox all;
    private Button btnAdd;
    private Button btnCancel;
    private Button btnCheckAll;
    private TextView checkBoxTitle;
    private RelativeLayout rl;
    private String mPlaylist;
    public static final int MENU_ADD = 2;
    public static final int MENU_CANCLE = 1;
    private static boolean mRequeryMode = false;
    private ProgressDialog dialog = null;

    private final static String PLAY_LIST = "playlist";
    private final static String FILE = "file";
    private final static String TRACK_ACTION = "vnd.android.cursor.dir/track";
    private final static String DISPLAY_HOME = "display_home";

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
        setContentView(R.layout.multi_track_choice);

        mAdapter = (TrackListAdapter) getLastNonConfigurationInstance();

        if (mAdapter != null) {
            mAdapter.setActivity(this);
            mAdapter.reloadStringOnLocaleChanges();
            setListAdapter(mAdapter);
        }
        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(false);
        getActionBar().setDisplayUseLogoEnabled(false);
        getActionBar().setElevation(8);
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        f.addDataScheme(FILE);
        registerReceiver(mScanListener, f);

        if (mAdapter == null) {
            mAdapter = new TrackListAdapter(getApplication(), // need to use
                                                              // application
                                                              // context to
                                                              // avoid leaks
                    this, null);
        }
        setListAdapter(mAdapter);
        getTrackCursor(mAdapter.getQueryHandler());

        mCursorCols = new String[] {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.DURATION
        };
        initializeViews();
        MusicLog.d(LOGTAG, "Activity create end");
    }

    /* SPRD: add menu for bug 266633 @{ */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_CANCLE, 1, R.string.cancel_add).setShowAsAction(
                MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, MENU_ADD, 1, R.string.confirm_add).setShowAsAction(
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
            case MENU_ADD:
                addSong();
                return true;

            case MENU_CANCLE:
                finish();
                return true;
        }
        return false;
    }

    private void updateMenu(Menu menu) {
        // Hide "help" if we don't have a URI for it.
        MenuItem add = menu.findItem(MENU_ADD);
        if (mAdapter.hasCheckedItem()) {
            add.setEnabled(true);
        } else {
            add.setEnabled(false);
        }
    }

    private void addSong() {
        final long[] ids = mAdapter.getCheckedIdArray();
        if (mPlaylist != null) {
            if (mPlaylist.equals("nowplaying")) {
                MusicUtils.addToCurrentPlaylist(MultiTrackChoiceActivity.this,
                        ids);
            } else {
                final long playlist = Long.valueOf(mPlaylist);
                if (ids.length < 50) {
                    MusicUtils.addToPlaylist(MultiTrackChoiceActivity.this,
                            ids, playlist);
                } else {
                    AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {

                        @Override
                        protected Integer doInBackground(Void... params) {
                            int numInserted = SPRDMusicUtils.addToPlaylistNoToast(
                                    MultiTrackChoiceActivity.this, ids,
                                    playlist);
                            return numInserted;
                        }

                        @Override
                        protected void onPreExecute() {
                            dialog = new ProgressDialog(
                                    MultiTrackChoiceActivity.this);
                            dialog.setMessage(getString(R.string.being_add));
                            dialog.setCancelable(false);
                            dialog.setCanceledOnTouchOutside(false);
                            dialog.show();
                        }

                        @Override
                        protected void onPostExecute(Integer result) {
                            /*  SPRD: Bug 434008 Pluging the USB and choosing USB storage during adding music,music will stop running @{ */
                            if(dialog != null && dialog.isShowing()){
                                dialog.cancel();
                            }
                            /* @} */
                            /* SPRD: add for bug 405864 @{ */
                            /*String message = MultiTrackChoiceActivity.this
                                    .getResources().getQuantityString(
                                            R.plurals.NNNtrackstoplaylist,
                                            result, result);
                            Toast.makeText(MultiTrackChoiceActivity.this,
                                    message, Toast.LENGTH_SHORT).show();*/
                            MusicUtils.showAddedSongsDialog(MultiTrackChoiceActivity.this, (ids.length - result), result);
                            /* @} */
                            startActivityAndFinish();
                        }
                    };
                    task.execute((Void[]) null);
                    return;
                }
            }
            startActivityAndFinish();
        }

    }

    private void startActivityAndFinish() {
        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setDataAndType(Uri.EMPTY, TRACK_ACTION);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(PLAY_LIST, mPlaylist);
        intent.putExtra(DISPLAY_HOME, true);
        startActivity(intent);
        finish();
    }

    /* @} */
    /**
     * Initialize all the controls.
     */
    private void initializeViews() {
        all = (CheckBox) findViewById(R.id.checkbox_selected_all);
        btnCheckAll = (Button) findViewById(R.id.btn_checkall);
        // SPRD: Add for bug 309889
        btnCheckAll.setClickable(false);
        all.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                for (int i = 0; i < mAdapter.getCount(); i++) {
                    mAdapter.setChecked(i, all.isChecked());
                }
                /* SPRD: modify all text for bug 266633 @{ */
                if (all.isChecked()) {
                    checkBoxTitle.setText(R.string.music_cancle_selected_all);
                } else {
                    checkBoxTitle.setText(R.string.music_selected_all);
                }
                /* @} */
                mAdapter.notifyDataSetChanged();
                invalidateOptionsMenu();
            }
        });
        invalidateOptionsMenu();
        checkBoxTitle = (TextView) findViewById(R.id.CheckBoxTitle);
        /* SPRD: reset CheckBoxTitle for bug 279232 @{ */
        if (mAdapter.getCount() != 0
                && mAdapter.getCheckedCount() == mAdapter.getCount()) {
            checkBoxTitle.setText(R.string.music_cancle_selected_all);
        } else {
            checkBoxTitle.setText(R.string.music_selected_all);
        }
        /* @} */
        rl = (RelativeLayout) findViewById(R.id.CheckboxLinearLayout);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        TrackListAdapter a = mAdapter;
        mAdapterSent = true;
        return a;
    }

    @Override
    public void onResume() {
        MusicLog.d(LOGTAG, "Activity resumed start");
        super.onResume();
        if (mTrackCursor != null) {
            getListView().invalidateViews();
        }
        MusicUtils.setSpinnerState(this);
        MusicLog.d(LOGTAG, "Activity resumed end");
    }

    @Override
    public void onPause() {
        Log.d(LOGTAG, "Activity pause start");
        mReScanHandler.removeCallbacksAndMessages(null);
        mRequeryMode = false;
        super.onPause();
        MusicLog.d(LOGTAG, "Activity pause end");
    }

    public void onSaveInstanceState(Bundle outcicle) {
        outcicle.putString("playlist", mPlaylist);
        super.onSaveInstanceState(outcicle);
    }

    @Override
    public void onDestroy() {
        MusicLog.d(LOGTAG, "Activity destroy start");

        // If we have an adapter and didn't send it off to another activity yet,
        // we should
        // close its cursor, which we do by assigning a null cursor to it. Doing
        // this
        // instead of closing the cursor directly keeps the framework from
        // accessing
        // the closed cursor later.
        if (!mAdapterSent && mAdapter != null) {
            if (mTrackCursor != null) {
                mTrackCursor.close();
            }// add to close cursor
            mAdapter.changeCursor(null);
        }
        // Because we pass the adapter to the next activity, we need to make
        // sure it doesn't keep a reference to this activity. We can do this
        // by clearing its DatasetObservers, which setListAdapter(null) does.
        setListAdapter(null);
        mAdapter = null;
        unregisterReceiverSafe(mScanListener);
        /*  SPRD: Bug 434008 Pluging the USB and choosing USB storage during adding music,music will stop running @{ */
        if(dialog != null && dialog.isShowing()){
            dialog.cancel();
        }
        /* @} */
        super.onDestroy();
        MusicLog.d(LOGTAG, "Activity destroy end");
    }

    /**
     * Unregister a receiver, but eat the exception that is thrown if the
     * receiver was never registered to begin with. This is a little easier than
     * keeping track of whether the receivers have actually been registered by
     * the time onDestroy() is called.
     */
    private void unregisterReceiverSafe(BroadcastReceiver receiver) {
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }

    private void getTrackCursor(TrackListAdapter.TrackQueryHandler queryhandler) {
        if (queryhandler == null) {
            throw new IllegalArgumentException();
        }
        mSortOrder = MediaStore.Audio.Media.TITLE_KEY;
        StringBuilder where = new StringBuilder();
        where.append(MediaStore.Audio.Media.TITLE + " != ''");
        where.append(" AND " + MediaStore.Audio.Media.IS_MUSIC + "=1");
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        queryhandler.doQuery(uri, mCursorCols, where.toString(), null,
                mSortOrder);
    }

    /*
     * This listener gets called when the media scanner starts up or finishes,
     * and when the sd card is unmounted.
     */
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_MEDIA_SCANNER_STARTED.equals(action)
                    || Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)) {
                MusicUtils.setSpinnerState(MultiTrackChoiceActivity.this);
            }
            if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)
                    || Intent.ACTION_MEDIA_EJECT.equals(action)) {
                finish();
                return;
            }
            mReScanHandler.sendEmptyMessage(0);
        }
    };

    private Handler mReScanHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mAdapter != null) {
                Log.d(LOGTAG, "mReScanHandler---handleMessage");
                getTrackCursor(mAdapter.getQueryHandler());
            }
        }
    };

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

    private void updateAddButton() {
        if (mAdapter.hasCheckedItem()) {
            btnAdd.setClickable(true);
            btnAdd.setFocusable(true);
            btnAdd.setEnabled(true);
        } else {
            btnAdd.setClickable(false);
            btnAdd.setFocusable(false);
            btnAdd.setEnabled(false);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        mAdapter.setChecked(position, !mAdapter.isChecked(position));
        MusicLog.d(LOGTAG,
                "mAdapter.getCheckedCount = " + mAdapter.getCheckedCount());
        MusicLog.d(LOGTAG, "mAdapter.getCount = " + mAdapter.getCount());
        if (mAdapter.getCheckedCount() == mAdapter.getCount()) {
            all.setChecked(true);
            /* SPRD: modify all text for bug 266633 @{ */
            checkBoxTitle.setText(R.string.music_cancle_selected_all);
            /* @} */
        } else {
            all.setChecked(false);
            /* SPRD: modify all text for bug 266633 @{ */
            checkBoxTitle.setText(R.string.music_selected_all);
            /* @} */
        }
        mAdapter.notifyDataSetChanged();
        invalidateOptionsMenu();
    }

    public void init(Cursor newCursor, boolean isLimited) {
        if (mAdapter == null) {
            return;
        }
        mAdapter.changeCursor(newCursor); // also sets mTrackCursor
        btnCheckAll.setEnabled(false);
        btnCheckAll.setClickable(false);
        if (mTrackCursor == null || mAdapter.getCount()==0) {
            Log.i(LOGTAG,"set view gone");
            closeContextMenu();
            all.setVisibility(View.GONE);
            btnCheckAll.setVisibility(View.GONE);
            checkBoxTitle.setVisibility(View.GONE);
            rl.setVisibility(View.GONE);
            mReScanHandler.sendEmptyMessageDelayed(0, 1000);
        } else {
            MusicUtils.hideDatabaseError(MultiTrackChoiceActivity.this);
            all.setVisibility(View.VISIBLE);
            btnCheckAll.setVisibility(View.VISIBLE);
            checkBoxTitle.setVisibility(View.VISIBLE);
            rl.setVisibility(View.VISIBLE);
            invalidateOptionsMenu();
            if (mRequeryMode) {
                updateCheckedArray();
            }
        }

    }

    /* SPRD: add update method for bug 266848 @{ */
    private void updateCheckedArray() {
        if (mAdapter == null) {
            return;
        }
        HashMap<Long, Long> cursor = new HashMap<Long, Long>();
        if (mTrackCursor.moveToFirst()) {
            do {
                long id = mTrackCursor.getLong(mTrackCursor
                        .getColumnIndex(MediaStore.MediaColumns._ID));
                cursor.put(id, id);
            } while (mTrackCursor.moveToNext());
        }
        long[] checkedIdArray = mAdapter.getCheckedIdArray();
        HashMap<Long, Long> newCheckedArray = new HashMap<Long, Long>();
        for (int i = 0; i < checkedIdArray.length; i++) {
            if (cursor.containsValue(checkedIdArray[i])) {
                newCheckedArray.put(checkedIdArray[i], checkedIdArray[i]);
            }
        }
        mAdapter.setCheckedArray(newCheckedArray);
    }

    /* @} */

    static class TrackListAdapter extends CursorAdapter {

        int mTitleIdx;
        int mArtistIdx;
        int mDurationIdx;
        int mAudioIdIdx;

        private final StringBuilder mBuilder = new StringBuilder();
        private String mUnknownArtist;
        private LayoutInflater mInflater;

        private MultiTrackChoiceActivity mActivity = null;
        private TrackQueryHandler mQueryHandler;

        static class ViewHolder {
            TextView line1;
            TextView line2;
            TextView duration;
            CheckBox checkbox;
            CharArrayBuffer buffer1;
            char[] buffer2;
        }

        class TrackQueryHandler extends AsyncQueryHandler {

            class QueryArgs {
                public Uri uri;
                public String[] projection;
                public String selection;
                public String[] selectionArgs;
                public String orderBy;
            }

            TrackQueryHandler(ContentResolver res) {
                super(res);
            }

            public void doQuery(Uri uri, String[] projection, String selection,
                    String[] selectionArgs, String orderBy) {
                MusicLog.d(LOGTAG, "doQuery");
                // Get 100 results first, which is enough to allow the user to
                // start scrolling,
                // while still being very fast.
                Uri limituri = uri.buildUpon()
                        .appendQueryParameter("limit", "100").build();
                QueryArgs args = new QueryArgs();
                args.uri = uri;
                args.projection = projection;
                args.selection = selection;
                args.selectionArgs = selectionArgs;
                args.orderBy = orderBy;

                startQuery(0, args, limituri, projection, selection,
                        selectionArgs, orderBy);
            }

            @Override
            protected void onQueryComplete(int token, Object cookie,
                    Cursor cursor) {
                MusicLog.d(LOGTAG, "onQueryComplete");
                mActivity.init(cursor, cookie != null);
                if (token == 0 && cookie != null && cursor != null
                        && !cursor.isClosed() && cursor.getCount() >= 100) {
                    cursor.close();
                    QueryArgs args = (QueryArgs) cookie;
                    startQuery(1, null, args.uri, args.projection,
                            args.selection, args.selectionArgs, args.orderBy);
                }
            }
        }

        TrackListAdapter(Context context,
                MultiTrackChoiceActivity currentactivity, Cursor cursor) {
            super(context, cursor, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            mInflater = LayoutInflater.from(context);
            mActivity = currentactivity;
            getColumnIndices(cursor);
            mUnknownArtist = context.getString(R.string.unknown_artist_name);
            mQueryHandler = new TrackQueryHandler(context.getContentResolver());
        }

        public void setActivity(MultiTrackChoiceActivity newactivity) {
            mActivity = newactivity;
        }

        public TrackQueryHandler getQueryHandler() {
            return mQueryHandler;
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
        protected void onContentChanged() {
            MusicLog.d(LOGTAG, "onContentChanged---requery");
            mActivity.mReScanHandler.removeMessages(0);
            mActivity.mReScanHandler.sendEmptyMessage(0);
            mRequeryMode = true;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return null;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            return;
        }

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

            // modified for bug 144473 start, 2013-04-02
            int millSecs = cursor.getInt(mDurationIdx);
            if (millSecs == 0) {
                vh.duration.setText("");
            } else {
                vh.duration.setText(MusicUtils.makeTimeString(
                        convertView.getContext(), millSecs/1000));
            }
            // modified for bug 144473 end, 2013-04-02

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

        @Override
        public void changeCursor(Cursor cursor) {
            Log.d(LOGTAG, "changeCursor");
            if (mActivity.isFinishing() && cursor != null) {
                cursor.close();
                cursor = null;
                return;
            }
            if (cursor != mActivity.mTrackCursor) {
                mActivity.mTrackCursor = cursor;
                super.changeCursor(cursor);
                getColumnIndices(cursor);
            }
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

        /*
         * SPRD: add @{ bug 343333 when switch language ,the Activity will be
         * killed by the system,then enter the activity,should reload the
         * unknown_artist_name and unknown_album_name.
         */
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
}
