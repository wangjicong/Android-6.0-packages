
package com.sprd.music.filemanager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.music.*;
import com.sprd.music.filemanager.FileListAdapter.LoadingFileListener;
import com.sprd.music.filemanager.FileListAdapter.OnPtahChangeListener;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.app.ActionBar;
import android.view.MenuItem;
import android.os.Environment;

public class FileManager extends Activity implements
        AdapterView.OnItemClickListener, FileListAdapter.OnTrackClickListener {

    private static final String LOGTAG = "FileManager";
    static private final HashMap<Context, MediaScannerConnection> mConnectionMap = new HashMap<Context, MediaScannerConnection>();

    private ImageButton backImageButton;
    private ListView mListView;
    private TextView mPathView;
    private View mStandByView;
    private RelativeLayout fileslayout;
    private FileListAdapter mAdapter = null;
    private String mPlaylist;
    private String fileAbsolutePath;

    private Handler mMainThreadHandler;

    private OnPtahChangeListener mOnPtahChangeListener = new FileListAdapter.OnPtahChangeListener() {

        @Override
        public void onPathChange(String path) {
            mPathView.setText(path);
        }
    };

    private Runnable mShowStandByRunnable = new Runnable() {
        @Override
        public void run() {
            mListView.setVisibility(View.GONE);
            mStandByView.setVisibility(View.VISIBLE);
        }
    };

    private LoadingFileListener mLoadingFileListener = new LoadingFileListener() {

        @Override
        public void onLoadFileStart() {
            if (mMainThreadHandler != null) {
                mMainThreadHandler.postDelayed(mShowStandByRunnable, 800);
            }
        }

        @Override
        public void onLoadFileFinished() {
            if (mMainThreadHandler != null) {
                mMainThreadHandler.removeCallbacks(mShowStandByRunnable);
            }
            if (mAdapter == null || mStandByView == null
                    || mListView == null) {
                return;
            }
            mStandByView.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.filemanager);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
            getActionBar().setElevation(8);
        }
        Intent intent = getIntent();
        if (savedInstanceState != null) {
            mPlaylist = savedInstanceState.getString("playlist");
        } else {
            mPlaylist = intent.getStringExtra("playlist");
        }

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_MEDIA_EJECT);
        f.addDataScheme("file");
        registerReceiver(mExternalStorageListener, f);
        mMainThreadHandler = new Handler(this.getMainLooper());
        fileslayout = (RelativeLayout) findViewById(R.id.files_layout);
        mPathView = (TextView) findViewById(R.id.file_path);
        View v = getLayoutInflater().inflate(R.layout.list_file,
                fileslayout);
        mStandByView = findViewById(R.id.filemanager_standby_layout);
        mAdapter = new FileListAdapter(this, FileUtil.getRootPath());
        mAdapter.setOnTrackClickListener(this);
        mAdapter.setLoadingFileListener(mLoadingFileListener);
        mAdapter.setOnPathChangeListener(mOnPtahChangeListener);
        mListView = (ListView) v.findViewById(R.id.list_files);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        // need to store the selected item so we don't lose it in case
        // of an orientation switch. Otherwise we could lose it while
        // in the middle of specifying a playlist to add the item to.
        outcicle.putString("playlist", mPlaylist);
        super.onSaveInstanceState(outcicle);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mExternalStorageListener);
        mAdapter.destroyThread();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        backup();
    }


    private void backup() {
        if (mAdapter == null) return;
        mAdapter.mCancelLoading = true;
        mAdapter.popupFolder();
    }

    private Handler addToPlaylistHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            MusicLog.d(LOGTAG, "addToPlaylistHandler");
            long songId = getSongIdForData(FileManager.this,
                    fileAbsolutePath);
            long[] ids = new long[] {
                songId
            };
            if (mPlaylist != null) {
                if (mPlaylist.equals("nowplaying")) {
                    MusicUtils.addToCurrentPlaylist(FileManager.this, ids);
                } else {
                    long playlist = Long.valueOf(mPlaylist);
                    MusicUtils.addToPlaylist(FileManager.this, ids, playlist);
                }
            }
        }
    };

    private BroadcastReceiver mExternalStorageListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_MEDIA_EJECT)) {
                finish();
            }
        }
    };

    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        Log.d(LOGTAG, "onItemClick-position : " + position);
        if (!mMainThreadHandler.hasCallbacks(mShowStandByRunnable)){
            mAdapter.execute(position, mListView.getFirstVisiblePosition());
        }
    }

    private void addToPlaylist(final String path) {
        MusicLog.d(LOGTAG, "path is :" + path);
        if (path == null) {
            return;
        }
        long songId = getSongIdForData(FileManager.this, path);
        if (songId == -1) {
            addToPlaylistHandler.post(new Runnable() {
                public void run() {
                    scanFile(path);
                }
            });
        } else {
            long[] ids = new long[] {
                songId
            };
            if (mPlaylist != null) {
                if (mPlaylist.equals("nowplaying")) {
                    MusicUtils.addToCurrentPlaylist(FileManager.this, ids);
                } else {
                    long playlist = Long.valueOf(mPlaylist);
                    MusicUtils.addToPlaylist(FileManager.this, ids, playlist);
                }
            }
        }
    }

    private void scanFile(String path) {
        this.fileAbsolutePath = path;
        MediaScannerConnection connection = new MediaScannerConnection(
                FileManager.this, client);
        mConnectionMap.put(FileManager.this, connection);
        connection.connect();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                backup();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private MediaScannerConnection.MediaScannerConnectionClient client = new MediaScannerConnection.MediaScannerConnectionClient() {
        public void onMediaScannerConnected() {
            MediaScannerConnection connection = mConnectionMap
                    .get(FileManager.this);
            if (connection != null) {
                try {
                    if (fileAbsolutePath != null) {
                        MusicLog.d(LOGTAG, "start to scan a file and file path is"
                                + fileAbsolutePath);
                        connection.scanFile(fileAbsolutePath, null);
                    } else {
                        MusicLog.d(LOGTAG, "file path is null");
                        disconnect();
                    }
                } catch (Exception e) {
                    MusicLog.d(LOGTAG, "exception in the progress of scanning");
                    disconnect();
                }
            }
        }

        public void onScanCompleted(String path, Uri uri) {
            MusicLog.d(LOGTAG, "file scan completed");
            addToPlaylistHandler.removeCallbacksAndMessages(null);
            Message msg = addToPlaylistHandler.obtainMessage();
            addToPlaylistHandler.sendMessageDelayed(msg, 500);
            disconnect();
        }

        public void disconnect() {
            MediaScannerConnection connection = mConnectionMap
                    .get(FileManager.this);
            if (connection != null) {
                connection.disconnect();
                mConnectionMap.put(FileManager.this, null);
            }
        }
    };

    private long getSongIdForData(Context context, String data) {
        String[] mCols = new String[] {
            MediaStore.Audio.Media._ID
        };

        String where = MediaStore.Audio.Media.DATA + "='" + convert(data) + "'";
        Cursor cursor = MusicUtils.query(context,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mCols, where,
                null, MediaStore.Audio.Media._ID);
        try {
            if (cursor == null || cursor.getCount() == 0) {
                return -1;
            }
            cursor.moveToFirst();
            int mIdIdx = cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            return cursor.getLong(mIdIdx);
        } catch (Exception ex) {
            return -1;
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
    }

    private String convert(String data) {
        if (data == null) {
            return data;
        }

        char[] arry = data.toCharArray();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < arry.length; i++) {
            if (arry[i] == 39) {
                result.append("''");
            } else {
                result.append(arry[i]);
            }
        }

        return result.toString();

    }

    @Override
    public void onTrackClick(String path) {
        addToPlaylist(path);
    }

}
