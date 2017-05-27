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
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class DeleteItems extends Activity
{
    private TextView mPrompt;
    private Button mButton;
    private long [] mItemList;
    //SPRD :add
    private Intent mIntent = null;
    AsyncTask<Void, Void, Void> task = null;

    /* SPRD: bug 399821 Exception, when two button are clicked at same time@{ */
    private boolean mIsButtonClicked = false;
    /* @} */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if(RequestPermissionsActivity.startPermissionActivity(this)){
            return;
        }

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.confirm_delete);
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                                    WindowManager.LayoutParams.WRAP_CONTENT);

        mPrompt = (TextView)findViewById(R.id.prompt);
        mButton = (Button) findViewById(R.id.delete);
        mButton.setOnClickListener(mButtonClicked);

        ((Button)findViewById(R.id.cancel)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                /* SPRD: bug 399821 Exception, when two button are clicked at same time@{ */
                if (mIsButtonClicked) {
                    mIsButtonClicked = false;
                    return;
                }
                mIsButtonClicked = true;
                /* @} */
                finish();
            }
        });

        Bundle b = getIntent().getExtras();
        /* SPRD 476965 @{ */
        int mDeleteMode = b.getInt(MusicUtils.DeleteMode.DELETE_MODE);
        String f = "";
        switch (mDeleteMode) {
           case MusicUtils.DeleteMode.DELETE_ABLUM:
                f = getString(R.string.delete_album);
                break;
           case MusicUtils.DeleteMode.DELETE_ARTSIT:
                f = getString(R.string.delete_artist);
                break;
           case MusicUtils.DeleteMode.DELETE_SONG:
                f = getString(R.string.delete_song);
                break;
        }
        String mCurrentTrackName = b.getString(MusicUtils.DeleteMode.CURRENT_TRACK_NAME);
        String desc = String.format(f, mCurrentTrackName);
        /* @} */
        mItemList = b.getLongArray("items");
        
        mPrompt.setText(desc);
        //SPRD:add register receiver about scanning sdcard
        registerSDListener();
    }
    
    private View.OnClickListener mButtonClicked = new View.OnClickListener() {
        public void onClick(View v) {
            // SPRD :remove Android Original Code for bug 299848 monkey ANR
            /*delete the selected item(s)
            MusicUtils.deleteTracks(DeleteItems.this, mItemList);
            finish();
            */
            /* SPRD: bug 399821 Exception, when two button are clicked at same time@{ */
            if (mIsButtonClicked) {
                mIsButtonClicked = false;
                return;
            }
            mIsButtonClicked = true;
            /* @} */

            /* SPRD 526153 add a toast for delete music success @{ */
            if (mItemList.length <= 1) {
                MusicUtils.deleteTracks(DeleteItems.this, mItemList);
                showDeleteToast();
                setResult(RESULT_OK);
                finish();
            } else {
                if (task != null) {
                    task.cancel(false);
                }
                task = new AsyncTask<Void, Void, Void>() {
                    private ProgressDialog dialog = null;

                    @Override
                    protected Void doInBackground(Void... params) {
                        MusicUtils.deleteTracks(DeleteItems.this, mItemList);
                        return null;
                    }

                    @Override
                    protected void onPreExecute() {
                        dialog = new ProgressDialog(DeleteItems.this);
                        dialog.setMessage(getString(R.string.del_track));
                        dialog.setCancelable(false);
                        dialog.setCanceledOnTouchOutside(false);
                        dialog.show();
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        dialog.cancel();
                        showDeleteToast();
                        DeleteItems.this.finish();
                    }
                };
                task.execute((Void[]) null);
            }
            /* @} */
        }
    };

    /* SPRD: add @{ */

    /**
     * SPRD: override onDestroy() for unregister scan sdcard recever
     */
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
     * SPRD:Finish create deleteItem activity when sdcard has been unmounted.
     */
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    private void showDeleteToast() {
        String message = DeleteItems.this.getResources().getQuantityString(
                R.plurals.NNNtracksdeleted, mItemList.length, Integer.valueOf(mItemList.length));
        Toast.makeText(DeleteItems.this, message, Toast.LENGTH_SHORT).show();
    }
    /* @} */
}
