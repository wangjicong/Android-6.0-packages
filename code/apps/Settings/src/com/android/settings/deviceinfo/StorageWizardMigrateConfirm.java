/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import static com.android.settings.deviceinfo.StorageSettings.TAG;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.storage.VolumeInfo;
import android.util.Log;

/* SPRD: add for storage manage */
import java.util.Objects;

import com.android.settings.R;

public class StorageWizardMigrateConfirm extends StorageWizardBase {
    private MigrateEstimateTask mEstimate;
    /* SPRD: add for storage manage @{ */
    private Thread mTask;
    private boolean mSetPrimary;
    public static final String SET_PRIMARY = "com.android.settings.deviceinfo.SET_PRIMARY";
    /* @} */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.storage_wizard_generic);

        /* SPRD: add for storage manage @{ */
        mSetPrimary = getIntent().getBooleanExtra(SET_PRIMARY, false);
        /* @} */

        // When called with just disk, find the first private volume
        if (mVolume == null) {
            mVolume = findFirstVolume(VolumeInfo.TYPE_PRIVATE);
        }

        /* SPRD: modify for storage manage @{
         * @orig
        final VolumeInfo sourceVol = getPackageManager().getPrimaryStorageCurrentVolume();
        if (sourceVol == null || mVolume == null) {
            Log.d(TAG, "Missing either source or target volume");
            finish();
            return;
        }
         */
        final VolumeInfo sourceVol;
        if (mVolume != null && mVolume.type == VolumeInfo.TYPE_PRIVATE) {
            final VolumeInfo primaryEmulatedVol = getPackageManager().getPrimaryEmulatedStorageCurrentVolume();
            if (!Objects.equals(mVolume,primaryEmulatedVol)) {
                sourceVol = primaryEmulatedVol;
            } else {
                sourceVol = getPackageManager().getPrimaryStorageCurrentVolume();
            }
        } else {
            sourceVol = getPackageManager().getPrimaryStorageCurrentVolume();
        }
        if ((!mSetPrimary && sourceVol == null) || mVolume == null) {
            Log.d(TAG, "Missing either source or target volume");
            finish();
            return;
        }

        if (sourceVol == null
                || mVolume.type == VolumeInfo.TYPE_PUBLIC
                || sourceVol.type == VolumeInfo.TYPE_PUBLIC) {
            // just set primary no migrate data
            final String targetDescrip = mStorage.getBestVolumeDescription(mVolume);

            setIllustrationInternal(true);
            setHeaderText(R.string.storage_wizard_set_primary_confirm_title, targetDescrip);
            setBodyText(R.string.storage_wizard_set_primary_confirm_body, targetDescrip);
            setSecondaryBodyText(R.string.storage_wizard_set_primary_details, targetDescrip);
            getNextButton().setText(R.string.storage_wizard_set_primary_confirm_next);
        } else {
        /* @} */

        final String sourceDescrip = mStorage.getBestVolumeDescription(sourceVol);
        final String targetDescrip = mStorage.getBestVolumeDescription(mVolume);

        setIllustrationInternal(true);
        setHeaderText(R.string.storage_wizard_migrate_confirm_title, targetDescrip);
        setBodyText(R.string.memory_calculating_size);
        setSecondaryBodyText(R.string.storage_wizard_migrate_details, targetDescrip);

        mEstimate = new MigrateEstimateTask(this) {
            @Override
            public void onPostExecute(String size, String time) {
                setBodyText(R.string.storage_wizard_migrate_confirm_body, time, size,
                        sourceDescrip);
            }
        };

        mEstimate.copyFrom(getIntent());
        mEstimate.execute();

        getNextButton().setText(R.string.storage_wizard_migrate_confirm_next);
        /* SPRD: add for storage manage @{ */
        }
        /* @} */
    }

    @Override
    public void onNavigateNext() {
        /* SPRD: modify for emulated storage @{
         * @orig
        final int moveId = getPackageManager().movePrimaryStorage(mVolume);

        final Intent intent = new Intent(StorageWizardMigrateConfirm.this, StorageWizardMigrateProgress.class);
        intent.putExtra(VolumeInfo.EXTRA_VOLUME_ID, mVolume.getId());
        intent.putExtra(PackageManager.EXTRA_MOVE_ID, moveId);
        startActivity(intent);
        finishAffinity();
         */
        if (mTask != null) {
            Log.w(TAG, "Task thread already exist.");
            return;
        }
        mTask = new Thread() {
            @Override
            public void run() {

                final int moveId;
                try {
                    if (mSetPrimary) {
                        moveId = getPackageManager().movePrimaryStorage(mVolume);
                    } else {
                        moveId = getPackageManager().movePrimaryEmulatedStorage(mVolume);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "set primary or migrate data get an exception", e);
                    return;
                }

                final Intent intent = new Intent(StorageWizardMigrateConfirm.this,
                        StorageWizardMigrateProgress.class);
                intent.putExtra(VolumeInfo.EXTRA_VOLUME_ID, mVolume.getId());
                intent.putExtra(PackageManager.EXTRA_MOVE_ID, moveId);
                startActivity(intent);
                finishAffinity();
                mTask = null;
            }
        };
        mTask.start();
        /* @} */
    }
}
