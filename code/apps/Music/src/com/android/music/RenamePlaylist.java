/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class RenamePlaylist extends Activity
{
    private EditText mPlaylist;
    private TextView mPrompt;
    private Button mSaveButton;
    private long mRenameId;
    private String mOriginalName;
    /* SPRD 476972 @{ */
    private Intent mIntent = null;
    private Context mContext;
    private static final int MAX_NAME_LENGTH = 50;
    /* @} */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (MusicUtils.checkPermission(this) != MusicUtils.PERMISSION_ALL_ALLOWED){
            finish();
            return;
        }
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.create_playlist);
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                                    WindowManager.LayoutParams.WRAP_CONTENT);
        /* SPRD 476972 @{ */
        mContext = getApplicationContext();
        mPrompt = (TextView)findViewById(R.id.prompt);
        mPlaylist = (EditText)findViewById(R.id.playlist);
        mSaveButton = (Button) findViewById(R.id.create);
        mSaveButton.setOnClickListener(mOpenClicked);

        ((Button)findViewById(R.id.cancel)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        mRenameId = icicle != null ? icicle.getLong("rename")
                : getIntent().getLongExtra("rename", -1);
        mOriginalName = nameForId(mRenameId);
        String defaultname = icicle != null ? icicle.getString("defaultname") : mOriginalName;
        
        if (mRenameId < 0 || mOriginalName == null || defaultname == null) {
            Log.i("@@@@", "Rename failed: " + mRenameId + "/" + defaultname);
            finish();
            return;
        }
        
        String promptformat;
        if (mOriginalName.equals(defaultname)) {
            promptformat = getString(R.string.rename_playlist_same_prompt);
        } else {
            promptformat = getString(R.string.rename_playlist_diff_prompt);
        }
        /* SPRD 476972 @{ */
        mPlaylist.setFilters( new InputFilter[] { new InputFilter.LengthFilter(MAX_NAME_LENGTH) });

        String prompt = String.format(promptformat, mOriginalName, defaultname);
        mPrompt.setText(prompt);
        mPlaylist.setText(defaultname);
        mPlaylist.setSelection(defaultname.length());
        mPlaylist.addTextChangedListener(mTextWatcher);
        setSaveButton();
        /* SPRD 476972 @{ */
        registerSDListener();
    }
    
    TextWatcher mTextWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // don't care about this one
        }
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // check if playlist with current name exists already, and warn the user if so.
            setSaveButton();
        };
        public void afterTextChanged(Editable s) {
            // don't care about this one
        	/* SPRD 476972 @{ */
            int length = s.toString().length();
            if (length >= MAX_NAME_LENGTH) {
                Toast.makeText(mContext, R.string.length_limited, Toast.LENGTH_SHORT).show();
            }
            /* @} */
        }
    };
    
    private void setSaveButton() {
        /* SPRD: modify for bug 383380 when rename playlist, remove space */
        // String typedname = mPlaylist.getText().toString();
        String typedname = mPlaylist.getText().toString().trim();
        /* @} */
        if (typedname.trim().length() == 0) {
            mSaveButton.setEnabled(false);
        } else {
            mSaveButton.setEnabled(true);
            if (idForplaylist(typedname) >= 0
                    && ! mOriginalName.equals(typedname)) {
                mSaveButton.setText(R.string.create_playlist_overwrite_text);
            } else {
                mSaveButton.setText(R.string.create_playlist_create_text);
            }
        }

    }
    
    private int idForplaylist(String name) {
        Cursor c = MusicUtils.query(this, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Audio.Playlists._ID },
                MediaStore.Audio.Playlists.NAME + "=?",
                new String[] { name },
                MediaStore.Audio.Playlists.NAME);
        int id = -1;
        if (c != null) {
            c.moveToFirst();
            if (!c.isAfterLast()) {
                id = c.getInt(0);
            }
        }
        c.close();
        return id;
    }
    
    private String nameForId(long id) {
        Cursor c = MusicUtils.query(this, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Audio.Playlists.NAME },
                MediaStore.Audio.Playlists._ID + "=?",
                new String[] { Long.valueOf(id).toString() },
                MediaStore.Audio.Playlists.NAME);
        String name = null;
        if (c != null) {
            c.moveToFirst();
            if (!c.isAfterLast()) {
                name = c.getString(0);
            }
            /* SPRD 476972 @{ */
            c.close();
        }
        c.close();
        return name;
    }
    
    
    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        outcicle.putString("defaultname", mPlaylist.getText().toString());
        outcicle.putLong("rename", mRenameId);
    }
    
    @Override
    public void onResume() {
        super.onResume();
    }

    private View.OnClickListener mOpenClicked = new View.OnClickListener() {
        public void onClick(View v) {
        	/* SPRD 476972 @{ */
            String name = mPlaylist.getText().toString().trim();
            if (name != null && name.length() > 0) {
            	/* SPRD 476972 @{ */
                if (name.equals(getString(R.string.recentlyadded))) {
                    Toast.makeText(RenamePlaylist.this,
                            getString(R.string.create_playlist_warning), Toast.LENGTH_LONG).show();
                    return;
                }
                if (mOriginalName == null || !mOriginalName.equals(name)) {
                    int id = idForplaylist(name);
                    if (id >= 0) {
                        showRenameDialog(name, id);
                        return;
                    }
                }
                /* @} */
                ContentResolver resolver = getContentResolver();
                ContentValues values = new ContentValues(1);
                values.put(MediaStore.Audio.Playlists.NAME, name);
                resolver.update(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                        values,
                        MediaStore.Audio.Playlists._ID + "=?",
                        new String[] { Long.valueOf(mRenameId).toString()});
                
                setResult(RESULT_OK);
                Toast.makeText(RenamePlaylist.this, R.string.playlist_renamed_message, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    };
    /* SPRD 476972 @{ */
    @Override
    public void onDestroy() {
        if (mIntent != null) {
            unregisterReceiver(mScanListener);
        }
        super.onDestroy();
    }

    /**
     * SPRD:register receiver about scanning sdcard
     */
    private void registerSDListener() {
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_MEDIA_EJECT);
        f.addAction(Intent.ACTION_MEDIA_REMOVED);
        f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        f.addAction(Intent.ACTION_MEDIA_SHARED);
        f.addDataScheme("file");
        mIntent = registerReceiver(mScanListener, f);
    }

    /**
     * SPRD:Finish rename playlist activity when sdcard has been unmounted.
     */
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    private void renamePlaylist(String name, int id) {
        ContentResolver resolver = getContentResolver();
        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,id);
        resolver.delete(uri, null, null);
        ContentValues values = new ContentValues(1);
        values.put(MediaStore.Audio.Playlists.NAME, name);
        resolver.update(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                values, MediaStore.Audio.Playlists._ID + "=?",
                new String[] {
                    Long.valueOf(mRenameId).toString()
                });
        Toast.makeText(RenamePlaylist.this, R.string.playlist_renamed_message, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void showRenameDialog(String playlistName, int playlistID) {
        DialogFragment dialogFragment = new RenameDialogFragment(playlistName, playlistID);
        dialogFragment.show(getFragmentManager(), "first");
    }

    class RenameDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
        private String mPlaylistName;
        private int mPlaylistId;

        public RenameDialogFragment(String playlistName, int playlistID) {
            mPlaylistName = playlistName;
            mPlaylistId = playlistID;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_DARK);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setTitle(android.R.string.dialog_alert_title);
            String message = getActivity().getString(R.string.overwrite_playlist_alert_rename,
                    mPlaylistName);
            builder.setMessage(message);
            builder.setNegativeButton(android.R.string.cancel, this);
            builder.setPositiveButton(android.R.string.ok, this);
            return builder.create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    renamePlaylist(mPlaylistName, mPlaylistId);
                    break;
                default:
                    break;
            }
        }
    }
    /* @} */
}
