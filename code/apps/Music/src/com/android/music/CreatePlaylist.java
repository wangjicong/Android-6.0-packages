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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
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
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.text.InputFilter;

public class CreatePlaylist extends Activity
{
    private EditText mPlaylist;
    private TextView mPrompt;
    private Button mSaveButton;
    /* SPRD 476972  @{*/
    private Context mContext;
    private static final int MAX_NAME_LENGTH = 50;
    private Intent mIntent = null;
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
        /* SPRD 476972  @{*/
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
        
        String defaultname = icicle != null ? icicle.getString("defaultname") : makePlaylistName();
        if (defaultname == null) {
            finish();
            return;
        }
        String promptformat = getString(R.string.create_playlist_create_text_prompt);
        String prompt = String.format(promptformat, defaultname);
        mPrompt.setText(prompt);
        mPlaylist.setText(defaultname);
        mPlaylist.setSelection(defaultname.length());
        mPlaylist.addTextChangedListener(mTextWatcher);
        //* SPRD 476972  @{*/
        mPlaylist.setFilters( new InputFilter[] { new InputFilter.LengthFilter(MAX_NAME_LENGTH) });
        registerSDListener();
        setSaveButton();
        /* @} */
    }
    
    TextWatcher mTextWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // don't care about this one
        }
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            /* SPRD 476972  @{*/
            String newText = mPlaylist.getText().toString().trim();
            if (newText.trim().length() == 0) {
                mSaveButton.setEnabled(false);
            } else {
                mSaveButton.setEnabled(true);
                // check if playlist with current name exists already, and warn the user if so.
                if (idForplaylist(newText) >= 0) {
                    mSaveButton.setText(R.string.create_playlist_overwrite_text);
                } else {
                    mSaveButton.setText(R.string.create_playlist_create_text);
                }
            }
        };
        public void afterTextChanged(Editable s) {
            /* SPRD 476972  @{*/
            int length = s.toString().length();
            if (length >= MAX_NAME_LENGTH) {
                Toast.makeText(mContext, R.string.length_limited, Toast.LENGTH_SHORT).show();
            }
            /* @} */
        }
    };
    
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
            c.close();
        }
        return id;
    }
    
    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        outcicle.putString("defaultname", mPlaylist.getText().toString());
    }
    
    @Override
    public void onResume() {
        super.onResume();
    }

    private String makePlaylistName() {

        String template = getString(R.string.new_playlist_name_template);
        int num = 1;

        String[] cols = new String[] {
                MediaStore.Audio.Playlists.NAME
        };
        ContentResolver resolver = getContentResolver();
        String whereclause = MediaStore.Audio.Playlists.NAME + " != ''";
        Cursor c = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
            cols, whereclause, null,
            MediaStore.Audio.Playlists.NAME);

        if (c == null) {
            return null;
        }
        
        String suggestedname;
        suggestedname = String.format(template, num++);
        
        // Need to loop until we've made 1 full pass through without finding a match.
        // Looping more than once shouldn't happen very often, but will happen if
        // you have playlists named "New Playlist 1"/10/2/3/4/5/6/7/8/9, where
        // making only one pass would result in "New Playlist 10" being erroneously
        // picked for the new name.
        boolean done = false;
        while (!done) {
            done = true;
            c.moveToFirst();
            while (! c.isAfterLast()) {
                String playlistname = c.getString(0);
                if (playlistname.compareToIgnoreCase(suggestedname) == 0) {
                    suggestedname = String.format(template, num++);
                    done = false;
                }
                c.moveToNext();
            }
        }
        c.close();
        return suggestedname;
    }
    
    private View.OnClickListener mOpenClicked = new View.OnClickListener() {
        public void onClick(View v) {
            /* SPRD 476972  @{*/
            String name = mPlaylist.getText().toString().trim();
            if (name != null && name.length() > 0) {
                /* SPRD 476972  @{*/
                if (name.equals(getString(R.string.recentlyadded))) {
                    Toast.makeText(CreatePlaylist.this,
                            getString(R.string.create_playlist_warning), Toast.LENGTH_LONG).show();
                    return;
                }
                /* @} */
                ContentResolver resolver = getContentResolver();
                int id = idForplaylist(name);
                Uri uri;
                if (id >= 0) {
                    /* SPRD 476972  @{*/
                    //uri = ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, id);
                    //MusicUtils.clearPlaylist(CreatePlaylist.this, id);
                    showAlertDialog(name, id);
                    return;
                } else {
                    ContentValues values = new ContentValues(1);
                    values.put(MediaStore.Audio.Playlists.NAME, name);
                    uri = resolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values);
                }
                /* SPRD 476972  @{*/
                Toast.makeText(CreatePlaylist.this, R.string.playlist_created_message,
                        Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK, (new Intent()).setData(uri));
                finish();
            }
        }
    };

    /* SPRD 476972  @{*/
    @Override
    public void onDestroy() {
        if (mIntent != null) {
            unregisterReceiver(mScanListener);
        }
        super.onDestroy();
    }

    private void setSaveButton() {
        String newText = mPlaylist.getText().toString().trim();
        if (newText.length() == 0) {
            mSaveButton.setEnabled(false);
        } else {
            mSaveButton.setEnabled(true);
            if (idForplaylist(newText) >= 0) {
                mSaveButton.setText(R.string.create_playlist_overwrite_text);
            } else {
                mSaveButton.setText(R.string.create_playlist_create_text);
            }
        }

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
     * SPRD:Finish create playlist activity when sdcard has been unmounted.
     */
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    class AlertDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
        private String mPlaylistName;
        private int mPlaylistId;

        public AlertDialogFragment(String playlistName, int playlistID) {
            mPlaylistName = playlistName;
            mPlaylistId = playlistID;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_DARK);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setTitle(android.R.string.dialog_alert_title);
            String message = getActivity().getString(R.string.overwrite_playlist_alert,
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
                    createPlayList(mPlaylistId);
                    break;
                default:
                    break;
            }
        }
    }

    private void showAlertDialog(String playlistName, int playlistID) {
        AlertDialogFragment alertdialogFragment = new AlertDialogFragment(playlistName, playlistID);
        alertdialogFragment.show(getFragmentManager(), "first");
    }

    private void createPlayList(int id) {
        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, id);
        MusicUtils.clearPlaylist(CreatePlaylist.this, id);
        setResult(RESULT_OK, (new Intent()).setData(uri));
        finish();
    }
    /* @} */
}
