/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import java.io.IOException;

/**
 * Dialog that comes up in response to various music-related VIEW intents.
 */
public class AudioPreview extends Activity implements OnPreparedListener, OnErrorListener, OnCompletionListener
{
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1;
    private final static String TAG = "AudioPreview";
    private PreviewPlayer mPlayer;
    private TextView mTextLine1;
    private TextView mTextLine2;
    private TextView mLoadingText;
    private SeekBar mSeekBar;
    private Handler mProgressRefresher;
    private boolean mSeeking = false;
    private boolean mUiPaused = true;
    private int mDuration;
    private Uri mUri;
    private long mMediaId = -1;
    private static final int OPEN_IN_MUSIC = 1;
    private AudioManager mAudioManager;
    private boolean mPausedByTransientLossOfFocus;
    /*Bug51634:add to check whether the file complete be complete.--up170412@{*/
    private boolean mIsComplete = false;
	/*Bug51634:add to check whether the file complete be complete.--up170412@}*/

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        /* SPRD: bug 503293 @{ */
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mHomeKeyEventReceive, filter);
        /* @} */
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        mUri = intent.getData();
        if (mUri == null) {
            finish();
            return;
        }
        
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.audiopreview);

        mTextLine1 = (TextView) findViewById(R.id.line1);
        mTextLine2 = (TextView) findViewById(R.id.line2);
        mLoadingText = (TextView) findViewById(R.id.loading);

        mSeekBar = (SeekBar) findViewById(R.id.progress);
        mProgressRefresher = new Handler();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        PreviewPlayer player = (PreviewPlayer) getLastNonConfigurationInstance();
        if (player == null) {
            mPlayer = new PreviewPlayer();
            mPlayer.setActivity(this);
        } else {
            mPlayer = player;
            mPlayer.setActivity(this);
            // onResume will update the UI
        }
        if (checkSelfPermission(READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            initPreview();
        } else {
            requestPermissions(new String[] { READ_EXTERNAL_STORAGE }, STORAGE_PERMISSION_REQUEST_CODE);
        }
    }

    /* SPRD: Fix bug 494581 @{ */
    private boolean requestAudioFocus() {
        int audioFocus = mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
                             AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        if(audioFocus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return true;
        }
        return false;
    }
    /* @} */

    private void initPreview() {
        String scheme = mUri.getScheme();
        if (scheme.equals("http")) {
            String msg = getString(R.string.streamloadingtext, mUri.getHost());
            mLoadingText.setText(msg);
        } else {
            mLoadingText.setVisibility(View.GONE);
        }
        try {
            mPlayer.setDataSourceAndPrepare(mUri);
        } catch (Exception ex) {
            // catch generic Exception, since we may be called with a media
            // content URI, another content provider's URI, a file URI,
            // an http URI, and there are different exceptions associated
            // with failure to open each of those.
            Log.d(TAG, "Failed to open file: " + ex);
            Toast.makeText(this, R.string.playback_failed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        AsyncQueryHandler mAsyncQueryHandler = new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                if (cursor != null && cursor.moveToFirst()) {

                    int titleIdx = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                    int artistIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                    int idIdx = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                    int displaynameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

                    if (idIdx >=0) {
                        mMediaId = cursor.getLong(idIdx);
                    }

                    if (titleIdx >= 0) {
                        String title = cursor.getString(titleIdx);
                        mTextLine1.setText(title);
                        if (artistIdx >= 0) {
                            String artist = cursor.getString(artistIdx);
                            mTextLine2.setText(artist);
                        }
                    } else if (displaynameIdx >= 0) {
                        String name = cursor.getString(displaynameIdx);
                        mTextLine1.setText(name);
                    } else {
                        // Couldn't find anything to display, what to do now?
                        Log.w(TAG, "Cursor had no names for us");
                    }
                } else {
                    Log.w(TAG, "empty cursor");
                }

                if (cursor != null) {
                    cursor.close();
                }
                setNames();
            }
        };

        if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
            if (mUri.getAuthority() == MediaStore.AUTHORITY) {
                // try to get title and artist from the media content provider
                mAsyncQueryHandler.startQuery(0, null, mUri, new String [] {
                        MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST},
                        null, null, null);
            } else {
                // Try to get the display name from another content provider.
                // Don't specifically ask for the display name though, since the
                // provider might not actually support that column.
                mAsyncQueryHandler.startQuery(0, null, mUri, null, null, null, null);
            }
        } else if (scheme.equals("file")) {
            // check if this file is in the media database (clicking on a download
            // in the download manager might follow this path
            String path = mUri.getPath();
            mAsyncQueryHandler.startQuery(0, null,  MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String [] {MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST},
                    MediaStore.Audio.Media.DATA + "=?", new String [] {path}, null);
        } else {
            // We can't get metadata from the file/stream itself yet, because
            // that API is hidden, so instead we display the URI being played
            if (mPlayer.isPrepared()) {
                setNames();
            }
        }
    }

    private BroadcastReceiver mHomeKeyEventReceive = new BroadcastReceiver() {
        String SYSTEM_REASON = "reason";
        String SYSTEM_HOME_KEY = "homekey";
        String SYSTEM_HOME_KEY_RECENT = "recentapps";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_REASON);
                if (TextUtils.equals(reason, SYSTEM_HOME_KEY)
                        || TextUtils.equals(reason, SYSTEM_HOME_KEY_RECENT)) {
                    stopPlayback();
                    finish();
                }
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                //ljb add 20160926 for cq:SPCSS00319326 
                //stopPlayback();
                //finish();
                //ljb add 20160926 for cq:SPCSS00319326 
            }
        }
    };

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
        case STORAGE_PERMISSION_REQUEST_CODE:
            if (grantResults.length > 0){
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initPreview();
                } else {
                    showConfirmDialog();
                }
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

    @Override
    public void onPause() {
        super.onPause();
        mUiPaused = true;
        if (mProgressRefresher != null) {
            mProgressRefresher.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mUiPaused = false;
        /* SPRD: add for bug512107 add the selection .
        set PreviewPlayer to null on some code,
        and monkey click is too quick,
        so add null pointer protect for PreviewPlayer@{ */
        if (mPlayer != null && mPlayer.isPrepared()) {
            showPostPrepareUI();
        }
        /* @} */
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        PreviewPlayer player = mPlayer;
        mPlayer = null;
        return player;
    }

    @Override
    public void onDestroy() {
        stopPlayback();
        unregisterReceiver(mHomeKeyEventReceive);
        super.onDestroy();
    }

    private void stopPlayback() {
        if (mProgressRefresher != null) {
            mProgressRefresher.removeCallbacksAndMessages(null);
        }
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
            mAudioManager.abandonAudioFocus(mAudioFocusListener);
        }
    }

    @Override
    public void onUserLeaveHint() {
        /*stopPlayback();
        finish();*/

        /*SPRD: 507205 if permission is granted, finish @{ */
        if (checkSelfPermission(READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            stopPlayback();
            finish();
        }
        /* @} */

        super.onUserLeaveHint();
    }

    public void onPrepared(MediaPlayer mp) {
        if (isFinishing()) return;
        mPlayer = (PreviewPlayer) mp;
        setNames();
        /* SPRD 518236 play music files in calling process @{ */
        if(!requestAudioFocus()){
            Toast.makeText(this.getApplicationContext(), R.string.no_allow_play_calling, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        /* @} */
        mPlayer.start();
        showPostPrepareUI();
    }

    private void showPostPrepareUI() {
        ProgressBar pb = (ProgressBar) findViewById(R.id.spinner);
        pb.setVisibility(View.GONE);
        mDuration = mPlayer.getDuration();
        if (mDuration != 0) {
            mSeekBar.setMax(mDuration);
            mSeekBar.setVisibility(View.VISIBLE);
			/*Bug51634:Do not set to the current point when finished.--up170412@{*/
//            if (!mSeeking) {
            if (!mSeeking && !mIsComplete) {
			/*Bug51634:Do not set to the current point when finished.--up170412@}*/
                mSeekBar.setProgress(mPlayer.getCurrentPosition());
            }
        }
        mSeekBar.setOnSeekBarChangeListener(mSeekListener);
        mLoadingText.setVisibility(View.GONE);
        View v = findViewById(R.id.titleandbuttons);
        v.setVisibility(View.VISIBLE);
        /* SPRD 518236 play music files in calling process @{
        mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        @{ */
        if (mProgressRefresher != null) {
            mProgressRefresher.removeCallbacksAndMessages(null);
            mProgressRefresher.postDelayed(new ProgressRefresher(), 200);
        }
        updatePlayPause();
    }
    
    private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (mPlayer == null) {
                // this activity has handed its MediaPlayer off to the next activity
                // (e.g. portrait/landscape switch) and should abandon its focus
                mAudioManager.abandonAudioFocus(this);
                return;
            }
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    mPausedByTransientLossOfFocus = false;
                    mPlayer.pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (mPlayer.isPlaying()) {
                        mPausedByTransientLossOfFocus = true;
                        mPlayer.pause();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (mPausedByTransientLossOfFocus) {
                        mPausedByTransientLossOfFocus = false;
                        start();
                    }
                    break;
            }
            updatePlayPause();
        }
    };
    
    private void start() {
        /* SPRD: Fix bug 513925  optimize the requestAudioFocus logic @{ */
        //mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
        //        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        if(!requestAudioFocus()){
            Toast.makeText(this.getApplicationContext(), R.string.no_allow_play_calling, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        /* @} */
        mPlayer.start();
        mProgressRefresher.postDelayed(new ProgressRefresher(), 200);
    }
    
    public void setNames() {
        if (TextUtils.isEmpty(mTextLine1.getText())) {
            mTextLine1.setText(mUri.getLastPathSegment());
        }
        if (TextUtils.isEmpty(mTextLine2.getText())) {
            mTextLine2.setVisibility(View.GONE);
        } else {
            mTextLine2.setVisibility(View.VISIBLE);
        }
    }

    class ProgressRefresher implements Runnable {

        @Override
        public void run() {
            if (mPlayer != null && !mSeeking && mDuration != 0) {
                mSeekBar.setProgress(mPlayer.getCurrentPosition());
            }
            mProgressRefresher.removeCallbacksAndMessages(null);
            if (!mUiPaused) {
                mProgressRefresher.postDelayed(new ProgressRefresher(), 200);
            }
        }
    }
    
    private void updatePlayPause() {
        ImageButton b = (ImageButton) findViewById(R.id.playpause);
        if (b != null && mPlayer != null) {
            if (mPlayer.isPlaying()) {
                b.setImageResource(R.drawable.btn_playback_ic_pause_small);
            } else {
                b.setImageResource(R.drawable.btn_playback_ic_play_small);
                mProgressRefresher.removeCallbacksAndMessages(null);
            }
        }
    }

    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            mSeeking = true;
        }
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) {
                return;
            }
            // Protection for case of simultaneously tapping on seek bar and exit
            if (mPlayer == null) {
                return;
            }
            mPlayer.seekTo(progress);
        }
        public void onStopTrackingTouch(SeekBar bar) {
            mSeeking = false;
			/*Bug51634:This variable is invalid when you click the status bar manually.--up170412@{*/
            mIsComplete = false;
			/*Bug51634:This variable is invalid when you click the status bar manually.--up170412@}*/
        }
    };

    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(this, R.string.playback_failed, Toast.LENGTH_SHORT).show();
        finish();
        return true;
    }

    public void onCompletion(MediaPlayer mp) {
    	/*Bug51634:The file is finished playing.--up170412@{*/
		mIsComplete = true;
		/*Bug51634:The file is finished playing.--up170412@}*/
		mSeekBar.setProgress(mDuration);
        updatePlayPause();
    }

    public void playPauseClicked(View v) {
        // Protection for case of simultaneously tapping on play/pause and exit
        if (mPlayer == null) {
            return;
        }
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            start();
        }
        /*Bug51634:The variable is disabled when the button is pressed manually.--up170412@{*/
        mIsComplete = false;
		/*Bug51634:The variable is disabled when the button is pressed manually.--up170412@}*/
        updatePlayPause();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // TODO: if mMediaId != -1, then the playing file has an entry in the media
        // database, and we could open it in the full music app instead.
        // Ideally, we would hand off the currently running mediaplayer
        // to the music UI, which can probably be done via a public static
        menu.add(0, OPEN_IN_MUSIC, 0, "open in music");
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(OPEN_IN_MUSIC);
        if (mMediaId >= 0) {
            item.setVisible(true);
            return true;
        }
        item.setVisible(false);
        return false;
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (mPlayer.isPlaying()) {
                    mPlayer.pause();
                } else {
					/*Bug51634:The variable is disabled when the button is pressed manually.--up170412@{*/
                	mIsComplete = false;
					/*Bug51634:The variable is disabled when the button is pressed manually.--up170412@}*/
                    start();
                }
                updatePlayPause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                start();
                updatePlayPause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (mPlayer.isPlaying()) {
                    mPlayer.pause();
                }
                updatePlayPause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                return true;
            case KeyEvent.KEYCODE_MEDIA_STOP:
            case KeyEvent.KEYCODE_BACK:
                stopPlayback();
                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /*
     * Wrapper class to help with handing off the MediaPlayer to the next instance
     * of the activity in case of orientation change, without losing any state.
     */
    private static class PreviewPlayer extends MediaPlayer implements OnPreparedListener {
        AudioPreview mActivity;
        boolean mIsPrepared = false;

        public void setActivity(AudioPreview activity) {
            mActivity = activity;
            setOnPreparedListener(this);
            setOnErrorListener(mActivity);
            setOnCompletionListener(mActivity);
        }

        public void setDataSourceAndPrepare(Uri uri) throws IllegalArgumentException,
                        SecurityException, IllegalStateException, IOException {
            setDataSource(mActivity,uri);
            prepareAsync();
        }

        /* (non-Javadoc)
         * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
         */
        @Override
        public void onPrepared(MediaPlayer mp) {
            mIsPrepared = true;
            mActivity.onPrepared(mp);
        }

        boolean isPrepared() {
            return mIsPrepared;
        }
    }

}
