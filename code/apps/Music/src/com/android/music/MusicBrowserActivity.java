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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_PHONE_STATE;

public class MusicBrowserActivity extends Activity
    implements MusicUtils.Defs {
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1;

    private ServiceToken mToken;

    public MusicBrowserActivity() {
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        /*int activeTab = MusicUtils.getIntPref(this, "activetab", R.id.artisttab);
        if (activeTab != R.id.artisttab
                && activeTab != R.id.albumtab
                && activeTab != R.id.songtab
                && activeTab != R.id.playlisttab) {
            activeTab = R.id.artisttab;
        }
        if (MusicUtils.checkPermission(this)) {
            MusicUtils.activateTab(this, activeTab);

            String shuf = getIntent().getStringExtra("autoshuffle");
            if ("true".equals(shuf)) {
                mToken = MusicUtils.bindToService(this, autoshuffle);
            }
        } else {
            requestPermissions(new String[] { WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE,
                    READ_PHONE_STATE }, STORAGE_PERMISSION_REQUEST_CODE);
            finish();
        }*/
        /* SPRD: bug 503293 @{ */
        switch (MusicUtils.checkPermission(this)) {
        case MusicUtils.PERMISSION_ALL_ALLOWED:
            activateTab();
            break;
        case MusicUtils.PERMISSION_ALL_DENYED:
            requestPermissions(new String[] { READ_EXTERNAL_STORAGE,
                    READ_PHONE_STATE }, STORAGE_PERMISSION_REQUEST_CODE);
            break;
        case MusicUtils.PERMISSION_READ_PHONE_STATE_ONLY:
            requestPermissions(new String[] { READ_EXTERNAL_STORAGE }, STORAGE_PERMISSION_REQUEST_CODE);
            break;
        case MusicUtils.PERMISSION_READ_EXTERNAL_STORAGE_ONLY:
            requestPermissions(new String[] {READ_PHONE_STATE }, STORAGE_PERMISSION_REQUEST_CODE);
            break;
        default:
            break;
        }
        /* @} */
    }

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
                activateTab();
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

    private void activateTab() {
        int activeTab = MusicUtils.getIntPref(this, "activetab", R.id.artisttab);
        if (activeTab != R.id.artisttab
                && activeTab != R.id.albumtab
                && activeTab != R.id.songtab
                && activeTab != R.id.playlisttab) {
            activeTab = R.id.artisttab;
        }
        MusicUtils.activateTab(this, activeTab);

        String shuf = getIntent().getStringExtra("autoshuffle");
        if ("true".equals(shuf)) {
            mToken = MusicUtils.bindToService(this, autoshuffle);
        }
    }

    @Override
    public void onDestroy() {
        if (mToken != null) {
            // SPRD 524518
            MusicUtils.unbindFromService(mToken, this);
        }
        super.onDestroy();
    }

    private ServiceConnection autoshuffle = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            // we need to be able to bind again, so unbind
            try {
                unbindService(this);
            } catch (IllegalArgumentException e) {
            }
            IMediaPlaybackService serv = IMediaPlaybackService.Stub.asInterface(obj);
            if (serv != null) {
                try {
                    serv.setShuffleMode(MediaPlaybackService.SHUFFLE_AUTO);
                } catch (RemoteException ex) {
                }
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
        }
    };

    /* SPRD 532167 @{ */
    @Override
    public void onBackPressed() {
        if (isResumed()) {
            super.onBackPressed();
        }
    }
    /* @} */
}

