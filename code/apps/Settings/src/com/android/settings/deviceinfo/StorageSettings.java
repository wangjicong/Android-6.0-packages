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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
/* SPRD: add for physical internal SD */
import android.os.Environment;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.format.Formatter.BytesResult;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.Utils;
import com.sprd.settings.FeatureOption;
import com.sprd.settings.deviceinfo.RadioButtonPreference;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.sprd.android.config.OptConfig;//Kalyy

/**
 * Panel showing both internal storage (both built-in storage and private
 * volumes) and removable storage (public volumes).
 */
public class StorageSettings extends SettingsPreferenceFragment implements Indexable {
    static final String TAG = "StorageSettings";

    private static final String TAG_VOLUME_UNMOUNTED = "volume_unmounted";
    private static final String TAG_DISK_INIT = "disk_init";
    public static final String KEY_DEFAULT_WRITE_DISK = "default_write_disk";
    public static final String KEY_DEFAULT_WRITE_DISK_DEVICE = "default_write_disk_device";
    public static final String KEY_DEFAULT_WRITE_DISK_SDCARD = "default_write_disk_sdcard";
    private static boolean sHasOpened;

    static final int COLOR_PUBLIC = Color.parseColor("#ff9e9e9e");
    static final int COLOR_WARNING = Color.parseColor("#fff4511e");

    static final int[] COLOR_PRIVATE = new int[] {
            Color.parseColor("#ff26a69a"),
            Color.parseColor("#ffab47bc"),
            Color.parseColor("#fff2a600"),
            Color.parseColor("#ffec407a"),
            Color.parseColor("#ffc0ca33"),
    };

    private StorageManager mStorageManager;

    private PreferenceCategory mInternalCategory;
    private PreferenceCategory mExternalCategory;
    /* SPRD: add for physical internal SD */
    private PreferenceCategory mDeviceCategory;
    private Preference mTotalSpacePref;
    private Preference mSystemSizePref;
    private PreferenceCategory mDiskCategory;
    private RadioButtonPreference mDeafultWritePathDevicePref;
    private RadioButtonPreference mDeafultWritePathSDcardPref;

    private StorageSummaryPreference mInternalSummary;
    private static final String STORAGE_DEVICE = "storate_device";
    private static final String STORAGE_SDCARD = "storate_sdcard";
    private static final String DEFAULT_STORAGE_LOCATION = "persist.sys.storageLocation";

    private VolumeUnmountedFragment mDialog;
    // SPRD: Modified for bug 540667, dismiss the dialog when SD card removed.
    private DiskInitFragment mDiskInitDialog;
    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DEVICEINFO_STORAGE;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_storage;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Context context = getActivity();

        mStorageManager = context.getSystemService(StorageManager.class);
        mStorageManager.registerListener(mStorageListener);

        addPreferencesFromResource(R.xml.device_info_storage);

        mInternalCategory = (PreferenceCategory) findPreference("storage_internal");
        mExternalCategory = (PreferenceCategory) findPreference("storage_external");
        /* SPRD: add for physical internal SD */
        mDeviceCategory = (PreferenceCategory) findPreference("storage_device");

        mDiskCategory = (PreferenceCategory) findPreference(KEY_DEFAULT_WRITE_DISK);
        mDeafultWritePathDevicePref = (RadioButtonPreference) mDiskCategory
                .findPreference(KEY_DEFAULT_WRITE_DISK_DEVICE);
        mDeafultWritePathSDcardPref = (RadioButtonPreference) mDiskCategory
                .findPreference(KEY_DEFAULT_WRITE_DISK_SDCARD);
        mDiskCategory.addPreference(mDeafultWritePathDevicePref);
        if(mDiskCategory!= null && !Environment.MEDIA_MOUNTED.equals(Environment.getExternalStoragePathState())){
            mDiskCategory.removePreference(mDeafultWritePathSDcardPref);
        } else if(mDiskCategory!= null){
            mDiskCategory.addPreference(mDeafultWritePathSDcardPref);
        }
        updataRadio();
         initRomPreference(context);
        mInternalSummary = new StorageSummaryPreference(context);

        setHasOptionsMenu(true);
    }

    private void initRomPreference(Context context) {
        if(FeatureOption.SPRD_ROM_SYSTEM_SIZE){
            mTotalSpacePref = new Preference(context);
        //        mTotalSpacePref.setIcon(null);
            mTotalSpacePref.setSelectable(false);
            mTotalSpacePref.setTitle(R.string.device_total_size);
            //Kalyy add for SUN_MEMORY_INFO_32G
            long ROM_TOTAL = 8L*1024*1024*1024;
            long ROM_REAL = Utils.getTotalRomSize();
            if(OptConfig.SUN_MEMORY_INFO_32G){
                ROM_TOTAL = 32L*1024*1024*1024;
            }
            if(OptConfig.SUN_MEMORY_INFO_16G){
                ROM_TOTAL = 16L*1024*1024*1024;
            }
            if(OptConfig.SUN_MEMORY_INFO_32G||OptConfig.SUN_MEMORY_INFO_16G || OptConfig.SUN_MEMORY_INFO_8G){
                mTotalSpacePref.setSummary(Formatter.formatFileSize(context,ROM_TOTAL));
            }else{
                mTotalSpacePref.setSummary(Formatter.formatFileSize(context,Utils.getTotalRomSize()));
            }
            mSystemSizePref = new Preference(context);
        //        mSystemSizePref.setIcon(null);
            mSystemSizePref.setSelectable(false);
            mSystemSizePref.setTitle(R.string.device_system_memory);
            if(OptConfig.SUN_MEMORY_INFO_32G||OptConfig.SUN_MEMORY_INFO_16G || OptConfig.SUN_MEMORY_INFO_8G){
                mSystemSizePref.setSummary(Formatter.formatFileSize(context,ROM_TOTAL*(1000*(ROM_REAL - Environment.getDataDirectory().getTotalSpace())/ROM_REAL)/1000));
            }else{
                mSystemSizePref.setSummary(Formatter.formatFileSize(context,Utils.getTotalRomSize() - Environment.getDataDirectory().getTotalSpace()));
            }
            //Kalyy add for SUN_MEMORY_INFO_32G
        }

    }

    private final StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            if (isInteresting(vol)) {
                refresh();
            }
        }
      /// SPRD refresh UI when plug in or out SD card. @{
        @Override
        public void onDiskScanned(DiskInfo disk, int volumeCount) {
            refresh();
        }

        @Override
        public void onDiskDestroyed(DiskInfo disk) {
            refresh();
        }
        /// @}
        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            Log.d(TAG, "onStorageStateChanged---oldState:"+oldState+",,newState:"+newState);
            refresh();
        }
    };

    private static boolean isInteresting(VolumeInfo vol) {
        switch(vol.getType()) {
            case VolumeInfo.TYPE_PRIVATE:
            case VolumeInfo.TYPE_PUBLIC:
                return true;
            default:
                return false;
        }
    }

    private void updataRadio(){
        String selectStation = SystemProperties.get(DEFAULT_STORAGE_LOCATION, STORAGE_DEVICE);
        if(selectStation.equals(STORAGE_DEVICE) && mDeafultWritePathDevicePref != null){
            mDeafultWritePathDevicePref.setChecked(true);
            mDeafultWritePathSDcardPref.setChecked(false);
        } else if(mDeafultWritePathSDcardPref != null && selectStation.equals(STORAGE_SDCARD)){
            mDeafultWritePathSDcardPref.setChecked(true);
            mDeafultWritePathDevicePref.setChecked(false);
        }
    }
    private void refresh() {
        final Context context = getActivity();

        getPreferenceScreen().removeAll();
        mInternalCategory.removeAll();
        mExternalCategory.removeAll();

        /* SPRD: modify for physical internal SD @{ */
         getPreferenceScreen().addPreference(mDiskCategory);
        /* @} */
        if(mDiskCategory!= null && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStoragePathState())){
            mDiskCategory.addPreference(mDeafultWritePathSDcardPref);
        } else if(mDiskCategory!= null){
            mDiskCategory.removePreference(mDeafultWritePathSDcardPref);
        }
        updataRadio();
        /* SPRD: add for physical internal SD */
        mDeviceCategory.removeAll();

        if(FeatureOption.SPRD_ROM_SYSTEM_SIZE){
            mInternalCategory.addPreference(mTotalSpacePref);
            mInternalCategory.addPreference(mSystemSizePref);
        }
        mInternalCategory.addPreference(mInternalSummary);

        int privateCount = 0;
        long privateUsedBytes = 0;
        long privateTotalBytes = 0;

        final List<VolumeInfo> volumes = mStorageManager.getVolumes();
        Collections.sort(volumes, VolumeInfo.getDescriptionComparator());

        for (VolumeInfo vol : volumes) {
            if (vol.getType() == VolumeInfo.TYPE_PRIVATE) {
                final int color = COLOR_PRIVATE[privateCount++ % COLOR_PRIVATE.length];
                mInternalCategory.addPreference(
                        new StorageVolumePreference(context, vol, color));
                if (vol.isMountedReadable()) {
                    final File path = vol.getPath();
                //Kalyy add for SUN_MEMORY_INFO_32G
                if(VolumeInfo.ID_PRIVATE_INTERNAL.equals(vol.getId())&&(OptConfig.SUN_MEMORY_INFO_32G||OptConfig.SUN_MEMORY_INFO_16G || OptConfig.SUN_MEMORY_INFO_8G)){
                    long ROM_TOTAL = 32L*1024*1024*1024;
                    if(OptConfig.SUN_MEMORY_INFO_16G){
                        ROM_TOTAL = 16L*1024*1024*1024;
                    } else if(OptConfig.SUN_MEMORY_INFO_8G){
                        ROM_TOTAL = 8L*1024*1024*1024;
                    }
                    long ROM_REAL = Utils.getTotalRomSize();
                    long TotalSpace = path.getTotalSpace();
                    TotalSpace = ROM_TOTAL*(1000*TotalSpace/ROM_REAL)/1000;
                    privateUsedBytes += TotalSpace*(1000*(path.getTotalSpace() - path.getFreeSpace())/path.getTotalSpace())/1000;
                    privateTotalBytes += TotalSpace;
                }else{
                    privateUsedBytes += path.getTotalSpace() - path.getFreeSpace();
                    privateTotalBytes += path.getTotalSpace();
                }
                //Kalyy add for SUN_MEMORY_INFO_32G
                }
            } else if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                /* SPRD: modify for physical internal SD @{
                 * @orig
                mExternalCategory.addPreference(
                        new StorageVolumePreference(context, vol, COLOR_PUBLIC));
                 */
                StorageVolumePreference preference = new StorageVolumePreference(context, vol, COLOR_PUBLIC);
                if (!Environment.internalIsEmulated()
                        && vol.linkName.startsWith("sdcard0")) {
                    mDeviceCategory.addPreference(preference);
                } else {
                    mExternalCategory.addPreference(preference);
                }
                /* @} */
            }
        }

        // Show missing private volumes
        final List<VolumeRecord> recs = mStorageManager.getVolumeRecords();
        for (VolumeRecord rec : recs) {
            if (rec.getType() == VolumeInfo.TYPE_PRIVATE
                    && mStorageManager.findVolumeByUuid(rec.getFsUuid()) == null) {
                // TODO: add actual storage type to record
                final Drawable icon = context.getDrawable(R.drawable.ic_sim_sd);
                icon.mutate();
                icon.setTint(COLOR_PUBLIC);

                final Preference pref = new Preference(context);
                pref.setKey(rec.getFsUuid());
                pref.setTitle(rec.getNickname());
                pref.setSummary(com.android.internal.R.string.ext_media_status_missing);
                pref.setIcon(icon);
                mInternalCategory.addPreference(pref);
            }
        }

        // Show unsupported disks to give a chance to init
        final List<DiskInfo> disks = mStorageManager.getDisks();
        for (DiskInfo disk : disks) {
            if (disk.volumeCount == 0 && disk.size > 0) {
                final Preference pref = new Preference(context);
                pref.setKey(disk.getId());
                pref.setTitle(disk.getDescription());
                pref.setSummary(com.android.internal.R.string.ext_media_status_unsupported);
                pref.setIcon(R.drawable.ic_sim_sd);
                mExternalCategory.addPreference(pref);
            }
        }

        final BytesResult result = Formatter.formatBytes(getResources(), privateUsedBytes, 0);
        mInternalSummary.setTitle(TextUtils.expandTemplate(getText(R.string.storage_size_large),
                result.value, result.units));
        mInternalSummary.setSummary(getString(R.string.storage_volume_used_total,
                Formatter.formatFileSize(context, privateTotalBytes)));

        if (mInternalCategory.getPreferenceCount() > 0) {
            getPreferenceScreen().addPreference(mInternalCategory);
        }
        /* SPRD: modify for physical internal SD @{ */
        if (mDeviceCategory.getPreferenceCount() > 0) {
            getPreferenceScreen().addPreference(mDeviceCategory);
        }
        /* @} */
        if (mExternalCategory.getPreferenceCount() > 0) {
            getPreferenceScreen().addPreference(mExternalCategory);
        }

        if (mInternalCategory.getPreferenceCount() == 2
                /* SPRD: modify for physical internal SD @{ */
                && mDeviceCategory.getPreferenceCount() == 0
                /* @} */
                && mExternalCategory.getPreferenceCount() == 0 && !sHasOpened) {
            // Only showing primary internal storage, so just shortcut
            final Bundle args = new Bundle();
            args.putString(VolumeInfo.EXTRA_VOLUME_ID, VolumeInfo.ID_PRIVATE_INTERNAL);
            startFragment(this, PrivateVolumeSettings.class.getCanonicalName(),
                    -1, 0, args);
            sHasOpened = true;
            finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mStorageManager.registerListener(mStorageListener);
        sHasOpened = false;
        refresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        mStorageManager.unregisterListener(mStorageListener);
        /* SPRD: 536328 dissmiss the VolumeUnmountedFragment Dialog @{ */
        if (mDialog != null) {
            mDialog.dismiss();
        }
        /* @} */
        /* SPRD: Modified for bug 540667, dismiss the dialog when SD card removed.@{ */
        if (mDiskInitDialog != null) {
            mDiskInitDialog.dismiss();
        }
        /* @} */
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference pref) {
        final String key = pref.getKey();
        Log.d(TAG, "onPreferenceTreeClick--key:"+key+",,,pref:"+pref);
        if(pref instanceof RadioButtonPreference){
            String mStorageSelectedLocation = SystemProperties.get(DEFAULT_STORAGE_LOCATION, STORAGE_DEVICE);
            Log.d(TAG, "onPreferenceTreeClick--mStorageSelectedLocation:"+mStorageSelectedLocation );
            if (pref != null && key.equals(KEY_DEFAULT_WRITE_DISK_DEVICE)) {
                mDeafultWritePathDevicePref.setChecked(true);
                mDeafultWritePathSDcardPref.setChecked(false);
               SystemProperties.set(DEFAULT_STORAGE_LOCATION, STORAGE_DEVICE);
            } else if(pref != null && key.equals(KEY_DEFAULT_WRITE_DISK_SDCARD)){
                mDeafultWritePathSDcardPref.setChecked(true);
                mDeafultWritePathDevicePref.setChecked(false);
                SystemProperties.set(DEFAULT_STORAGE_LOCATION, STORAGE_SDCARD);
            }
            return true;
        } else if (pref instanceof StorageVolumePreference) {
            // Picked a normal volume
            final VolumeInfo vol = mStorageManager.findVolumeById(key);

            if (vol.getState() == VolumeInfo.STATE_UNMOUNTED) {
                mDialog = VolumeUnmountedFragment.show(this, vol.getId());
                return true;
            } else if (vol.getState() == VolumeInfo.STATE_UNMOUNTABLE) {
                // SPRD: Modified for bug 540667, dismiss the dialog when SD card removed.
                mDiskInitDialog = DiskInitFragment.show(this, R.string.storage_dialog_unmountable, vol.getDiskId());
                return true;
            }

            if (vol.getType() == VolumeInfo.TYPE_PRIVATE) {
                final Bundle args = new Bundle();
                args.putString(VolumeInfo.EXTRA_VOLUME_ID, vol.getId());
                startFragment(this, PrivateVolumeSettings.class.getCanonicalName(),
                        -1, 0, args);
                return true;

            } else if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                if (vol.isMountedReadable()) {
                    startActivity(vol.buildBrowseIntent());
                    return true;
                } else {
                    final Bundle args = new Bundle();
                    args.putString(VolumeInfo.EXTRA_VOLUME_ID, vol.getId());
                    startFragment(this, PublicVolumeSettings.class.getCanonicalName(),
                            -1, 0, args);
                    return true;
                }
            }

        } else if (key.startsWith("disk:")) {
            // Picked an unsupported disk
            mDiskInitDialog = DiskInitFragment.show(this, R.string.storage_dialog_unsupported, key);
            return true;

        } else {
            // Picked a missing private volume
            final Bundle args = new Bundle();
            args.putString(VolumeRecord.EXTRA_FS_UUID, key);
            startFragment(this, PrivateVolumeForget.class.getCanonicalName(),
                    R.string.storage_menu_forget, 0, args);
            return true;
        }

        return false;
    }

    public static class MountTask extends AsyncTask<Void, Void, Exception> {
        private final Context mContext;
        private final StorageManager mStorageManager;
        private final String mVolumeId;
        private final String mDescription;

        public MountTask(Context context, VolumeInfo volume) {
            mContext = context.getApplicationContext();
            mStorageManager = mContext.getSystemService(StorageManager.class);
            mVolumeId = volume.getId();
            mDescription = mStorageManager.getBestVolumeDescription(volume);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                mStorageManager.mount(mVolumeId);
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception e) {
            if (e == null) {
                Toast.makeText(mContext, mContext.getString(R.string.storage_mount_success,
                        mDescription), Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Failed to mount " + mVolumeId, e);
                Toast.makeText(mContext, mContext.getString(R.string.storage_mount_failure,
                        mDescription), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static class UnmountTask extends AsyncTask<Void, Void, Exception> {
        private final Context mContext;
        private final StorageManager mStorageManager;
        private final String mVolumeId;
        private final String mDescription;

        public UnmountTask(Context context, VolumeInfo volume) {
            mContext = context.getApplicationContext();
            mStorageManager = mContext.getSystemService(StorageManager.class);
            mVolumeId = volume.getId();
            mDescription = mStorageManager.getBestVolumeDescription(volume);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                mStorageManager.unmount(mVolumeId);
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception e) {
            if (e == null) {
                Toast.makeText(mContext, mContext.getString(R.string.storage_unmount_success,
                        mDescription), Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Failed to unmount " + mVolumeId, e);
                Toast.makeText(mContext, mContext.getString(R.string.storage_unmount_failure,
                        mDescription), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static class VolumeUnmountedFragment extends DialogFragment {
        public static VolumeUnmountedFragment show(Fragment parent, String volumeId) {
            final Bundle args = new Bundle();
            args.putString(VolumeInfo.EXTRA_VOLUME_ID, volumeId);

            final VolumeUnmountedFragment dialog = new VolumeUnmountedFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_VOLUME_UNMOUNTED);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final StorageManager sm = context.getSystemService(StorageManager.class);

            final String volumeId = getArguments().getString(VolumeInfo.EXTRA_VOLUME_ID);
            final VolumeInfo vol = sm.findVolumeById(volumeId);

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(TextUtils.expandTemplate(
                    getText(R.string.storage_dialog_unmounted), vol.getDisk().getDescription()));

            builder.setPositiveButton(R.string.storage_menu_mount,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    new MountTask(context, vol).execute();
                }
            });
            builder.setNegativeButton(R.string.cancel, null);

            return builder.create();
        }
    }

    public static class DiskInitFragment extends DialogFragment {
        // SPRD: Modified for bug 540667, dismiss the dialog when SD card removed.
        public static DiskInitFragment show(Fragment parent, int resId, String diskId) {

            final Bundle args = new Bundle();
            args.putInt(Intent.EXTRA_TEXT, resId);
            args.putString(DiskInfo.EXTRA_DISK_ID, diskId);
            final DiskInitFragment dialog = new DiskInitFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_DISK_INIT);
            // SPRD: Modified for bug 540667, dismiss the dialog when SD card removed.
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final StorageManager sm = context.getSystemService(StorageManager.class);

            final int resId = getArguments().getInt(Intent.EXTRA_TEXT);
            final String diskId = getArguments().getString(DiskInfo.EXTRA_DISK_ID);
            final DiskInfo disk = sm.findDiskById(diskId);

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            /* SPRD: Modified for bug 540667, dismiss the dialog when SD card removed. @{ */
            if (disk != null) {
                builder.setMessage(TextUtils.expandTemplate(getText(resId), disk.getDescription()));
            }
            /* @} */
            builder.setPositiveButton(R.string.storage_menu_set_up,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final Intent intent = new Intent(context, StorageWizardInit.class);
                    intent.putExtra(DiskInfo.EXTRA_DISK_ID, diskId);
                    startActivity(intent);
                }
            });
            builder.setNegativeButton(R.string.cancel, null);

            return builder.create();
        }
    }

    /**
     * Enable indexing of searchable data
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();

                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.storage_settings);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.internal_storage);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                final StorageManager storage = context.getSystemService(StorageManager.class);
                final List<VolumeInfo> vols = storage.getVolumes();
                for (VolumeInfo vol : vols) {
                    if (isInteresting(vol)) {
                        data.title = storage.getBestVolumeDescription(vol);
                        data.screenTitle = context.getString(R.string.storage_settings);
                        result.add(data);
                    }
                }

                /* SPRD 493229 @{ */
                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.storage_usb_settings);
                data.screenTitle = context.getString(R.string.storage_usb_settings);
                result.add(data);
                /* @} */

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_size);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_available);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_apps_usage);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_dcim_usage);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_music_usage);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_downloads_usage);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_media_cache_usage);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_media_misc_usage);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                return result;
            }
        };
}
