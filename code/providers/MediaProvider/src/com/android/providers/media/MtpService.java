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

package com.android.providers.media;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.mtp.MtpDatabase;
import android.mtp.MtpServer;
import android.mtp.MtpStorage;
import android.os.Environment;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.ArrayList;

public class MtpService extends Service {
    private static final String TAG = "MtpService";
    private static final boolean LOGD = false;

    // We restrict PTP to these subdirectories
    private static final String[] PTP_DIRECTORIES = new String[] {
        Environment.DIRECTORY_DCIM,
        Environment.DIRECTORY_PICTURES,
    };

    private void addStorageDevicesLocked() {
        /* SPRD: Add, we support primary storage and second storage in PTP mode @{ */ 
        for (StorageVolume volume : mVolumeMap.values()) {
            final String path = volume.getPath();
            if (path != null) {
                String state = mStorageManager.getVolumeState(path);
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    addStorageLocked(volume);
                }
             }
        }
        /* @} */
        /* SPRD: Removed @{ 
        if (mPtpMode) {
            // In PTP mode we support only primary storage
            final StorageVolume primary = StorageManager.getPrimaryVolume(mVolumes);
            final String path = primary.getPath();
            if (path != null) {
                String state = mStorageManager.getVolumeState(path);
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    addStorageLocked(mVolumeMap.get(path));
                }
            }
        } else {
            for (StorageVolume volume : mVolumeMap.values()) {
                addStorageLocked(volume);
            }
        }
        @} */
    }

    private final StorageEventListener mStorageEventListener = new StorageEventListener() {
        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            synchronized (mBinder) {
                Log.d(TAG, "onStorageStateChanged " + path + " " + oldState + " -> " + newState);
                if (Environment.MEDIA_MOUNTED.equals(newState)) {
                    volumeMountedLocked(path);
                } else if (Environment.MEDIA_MOUNTED.equals(oldState)) {
                    StorageVolume volume = mVolumeMap.remove(path);
                    if (volume != null) {
                        removeStorageLocked(volume);
                    }
                }
            }
        }
    };

    private MtpDatabase mDatabase;
    private MtpServer mServer;
    private StorageManager mStorageManager;
    /** Flag indicating if MTP is disabled due to keyguard */
    private boolean mMtpDisabled;
    private boolean mUnlocked;
    private boolean mPtpMode;
    private final HashMap<String, StorageVolume> mVolumeMap = new HashMap<String, StorageVolume>();
    private final HashMap<String, MtpStorage> mStorageMap = new HashMap<String, MtpStorage>();
    private StorageVolume[] mVolumes;

    @Override
    public void onCreate() {
        mStorageManager = StorageManager.from(this);
        synchronized (mBinder) {
            updateDisabledStateLocked();
            mStorageManager.registerListener(mStorageEventListener);
            StorageVolume[] volumes = mStorageManager.getVolumeList();
            mVolumes = volumes;
            for (int i = 0; i < volumes.length; i++) {
                String path = volumes[i].getPath();
                String state = mStorageManager.getVolumeState(path);
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    volumeMountedLocked(path);
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mUnlocked = intent.getBooleanExtra(UsbManager.USB_DATA_UNLOCKED, false);
        if (LOGD) { Log.d(TAG, "onStartCommand intent=" + intent + " mUnlocked=" + mUnlocked); }
        synchronized (mBinder) {
            /* SPRD: avoid start MTP service when primary storage is not mounted @{ */
            if (mStorageMap.get(mVolumes[0].getPath()) == null) {
                Log.e(TAG, "WARNING, trying start Mtp service while primary external storage is not mounted! finish Mtp service");
            }
            /* @} */

            updateDisabledStateLocked();
            mPtpMode = (intent == null ? false
                    : intent.getBooleanExtra(UsbManager.USB_FUNCTION_PTP, false));
            String[] subdirs = null;
            ArrayList<String> dirs = new ArrayList<String>();
            Log.e(TAG, "ptp mode "+mPtpMode);
            if (mPtpMode) {
                /* SPRD: delete it, then change to chapter behind @{ 
                int count = PTP_DIRECTORIES.length;
                subdirs = new String[count];
                for (int i = 0; i < count; i++) {
                    File file =
                            Environment.getExternalStoragePublicDirectory(PTP_DIRECTORIES[i]);
                    // make sure this directory exists
                    file.mkdirs();
                    subdirs[i] = file.getPath();
                }
                @} */

                /* SPRD: Add @{ */
                int volumeNum = mVolumes.length;
                File file = null;
                for (int i = 0; i < volumeNum; i++) {
                    String path = mVolumes[i].getPath();
                    if (path == null) continue;
                    String state = mStorageManager.getVolumeState(path);
                    if (Environment.MEDIA_MOUNTED.equals(state)) {
                        for (int j = 0; j < PTP_DIRECTORIES.length; j++) {
                            file = new File(path, PTP_DIRECTORIES[j]);
                            // make sure this directory exists
                            if (file != null) {
                                file.mkdirs();
                                dirs.add(file.getPath());
                            }
                        }
                    }
                }
            }
            if (dirs.size() != 0) {
                subdirs = new String[dirs.size()];
                subdirs = dirs.toArray(subdirs);
            }
            /* @} */

            final StorageVolume primary = StorageManager.getPrimaryVolume(mVolumes);
            if (mDatabase != null) {
                mDatabase.setServer(null);
            }
            mDatabase = new MtpDatabase(this, MediaProvider.EXTERNAL_VOLUME,
                    primary.getPath(), subdirs);
            manageServiceLocked();
        }

        return START_REDELIVER_INTENT;
    }

    private void updateDisabledStateLocked() {
        final boolean isCurrentUser = UserHandle.myUserId() == ActivityManager.getCurrentUser();
        mMtpDisabled = !mUnlocked || !isCurrentUser;
        if (LOGD) {
            Log.d(TAG, "updating state; isCurrentUser=" + isCurrentUser + ", mMtpLocked="
                    + mMtpDisabled);
        }
    }

    /**
     * Manage {@link #mServer}, creating only when running as the current user.
     */
    private void manageServiceLocked() {
        final boolean isCurrentUser = UserHandle.myUserId() == ActivityManager.getCurrentUser();
        if (mServer == null && isCurrentUser) {
            Log.d(TAG, "starting MTP server in " + (mPtpMode ? "PTP mode" : "MTP mode"));
            mServer = new MtpServer(mDatabase, mPtpMode);
            mDatabase.setServer(mServer);
            if (!mMtpDisabled) {
                addStorageDevicesLocked();
            }
            mServer.start();
        } else if (mServer != null && !isCurrentUser) {
            Log.d(TAG, "no longer current user; shutting down MTP server");
            // Internally, kernel will close our FD, and server thread will
            // handle cleanup.
            mServer = null;
            mDatabase.setServer(null);
        }
    }

    @Override
    public void onDestroy() {
        mStorageManager.unregisterListener(mStorageEventListener);
        if (mDatabase != null) {
            mDatabase.setServer(null);
        }
    }

    private final IMtpService.Stub mBinder =
            new IMtpService.Stub() {
        public void sendObjectAdded(int objectHandle) {
            synchronized (mBinder) {
                if (mServer != null) {
                    mServer.sendObjectAdded(objectHandle);
                }
            }
        }

        public void sendObjectRemoved(int objectHandle) {
            synchronized (mBinder) {
                if (mServer != null) {
                    mServer.sendObjectRemoved(objectHandle);
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void volumeMountedLocked(String path) {
        for (int i = 0; i < mVolumes.length; i++) {
            StorageVolume volume = mVolumes[i];
            // SPRD: add this for bug 541885 to exclude invalid volume
            if (volume.getPath().equals(path)
                    && volume.getStorageId() != StorageVolume.STORAGE_ID_INVALID) {
                mVolumeMap.put(path, volume);
                if (!mMtpDisabled) {
                    /* SPRD: remove the feature @ {
                    // In PTP mode we support only primary storage
                    if (volume.isPrimary() || !mPtpMode) {
                        addStorageLocked(volume);
                    }
                    @} */
                    // SPRD: add the volume, regardless of the storage type
                    addStorageLocked(volume);
                }
                break;
            }
        }
    }

    private void addStorageLocked(StorageVolume volume) {
        MtpStorage storage = new MtpStorage(volume, getApplicationContext());
        mStorageMap.put(storage.getPath(), storage);

        if (storage.getStorageId() == StorageVolume.STORAGE_ID_INVALID) {
            Log.w(TAG, "Ignoring volume with invalid MTP storage ID: " + storage);
            return;
        } else {
            Log.d(TAG, "Adding MTP storage 0x" + Integer.toHexString(storage.getStorageId())
                    + " at " + storage.getPath());
        }

        if (mDatabase != null) {
            mDatabase.addStorage(storage);
        }
        if (mServer != null) {
            mServer.addStorage(storage);
        }
    }

    private void removeStorageLocked(StorageVolume volume) {
        MtpStorage storage = mStorageMap.remove(volume.getPath());
        if (storage == null) {
            Log.e(TAG, "Missing MtpStorage for " + volume.getPath());
            return;
        }

        Log.d(TAG, "Removing MTP storage " + Integer.toHexString(storage.getStorageId()) + " at "
                + storage.getPath());
        if (mDatabase != null) {
            mDatabase.removeStorage(storage);
        }
        if (mServer != null) {
            mServer.removeStorage(storage);
        }
    }
}
