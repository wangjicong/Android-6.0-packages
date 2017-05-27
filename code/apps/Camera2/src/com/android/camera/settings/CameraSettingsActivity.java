/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.camera.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.camera.FatalErrorHandler;
import com.android.camera.FatalErrorHandlerImpl;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import com.android.camera.debug.Log;
import com.android.camera.device.CameraId;
import com.android.camera.one.OneCamera.Facing;
import com.android.camera.one.OneCameraAccessException;
import com.android.camera.one.OneCameraCharacteristics;
import com.android.camera.one.OneCameraException;
import com.android.camera.one.OneCameraManager;
import com.android.camera.one.OneCameraModule;
import com.android.camera.settings.PictureSizeLoader.PictureSizes;
import com.android.camera.settings.SettingsUtil.SelectedVideoQualities;
import com.android.camera.util.CameraSettingsActivityHelper;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.GoogleHelpHelper;
import com.android.camera.util.Size;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgentFactory;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.sprd.camera.storagepath.MultiStorage;
import com.sprd.camera.storagepath.StorageUtil;
import com.ucamera.ucam.modules.utils.UCamUtill;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ucamera.ucam.modules.utils.UCamUtill;

import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import com.android.camera.util.CameraUtil;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.SystemProperties;

import com.sprd.android.config.OptConfig;
/**
 * Provides the settings UI for the Camera app.
 */
public class CameraSettingsActivity extends FragmentActivity {

    /**
     * Used to denote a subsection of the preference tree to display in the
     * Fragment. For instance, if 'Advanced' key is provided, the advanced
     * preference section will be treated as the root for display. This is used
     * to enable activity transitions between preference sections, and allows
     * back/up stack to operate correctly.
     */
    public static final String PREF_SCREEN_EXTRA = "pref_screen_extra";
    public static final String HIDE_ADVANCED_SCREEN = "hide_advanced";
    public static final String PERSIST_CAMERA_SMILE = "persist.sys.cam.smile";//SPRD：Add for ai detect
    private OneCameraManager mOneCameraManager;
    public static Context mContext;//SPRD: fix bug473462
    public static ArrayList<Context> contexts = new ArrayList<Context>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = CameraSettingsActivity.this;

        // SPRD Bug:474694 Feature:Reset Settings.
        contexts.add(mContext);

        FatalErrorHandler fatalErrorHandler = new FatalErrorHandlerImpl(this);
        boolean hideAdvancedScreen = false;

        try {
            mOneCameraManager = OneCameraModule.provideOneCameraManager();
        } catch (OneCameraException e) {
            // Log error and continue. Modules requiring OneCamera should check
            // and handle if null by showing error dialog or other treatment.
            fatalErrorHandler.onGenericCameraAccessFailure();
        }

        // Check if manual exposure is available, so we can decide whether to
        // display Advanced screen.
        /**
         * SPRD BUG 506348: clicking "advanced setting" under setting UI make error.
         * according to current advanced setting logic, the advanced screen always be shown@{
         * Original Code

        try {
            CameraId frontCameraId = mOneCameraManager.findFirstCameraFacing(Facing.FRONT);
            CameraId backCameraId = mOneCameraManager.findFirstCameraFacing(Facing.BACK);

            // The exposure compensation is supported when both of the following conditions meet
            //   - we have the valid camera, and
            //   - the valid camera supports the exposure compensation
            boolean isExposureCompensationSupportedByFrontCamera = (frontCameraId != null) &&
                    (mOneCameraManager.getOneCameraCharacteristics(frontCameraId)
                            .isExposureCompensationSupported());
            boolean isExposureCompensationSupportedByBackCamera = (backCameraId != null) &&
                    (mOneCameraManager.getOneCameraCharacteristics(backCameraId)
                            .isExposureCompensationSupported());

            // Hides the option if neither front and back camera support exposure compensation.
            if (!isExposureCompensationSupportedByFrontCamera &&
                    !isExposureCompensationSupportedByBackCamera) {
                hideAdvancedScreen = true;
            }
        } catch (OneCameraAccessException e) {
            fatalErrorHandler.onGenericCameraAccessFailure();
        }
         */

        hideAdvancedScreen = false;
        /* @} */

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.mode_settings);

        String prefKey = getIntent().getStringExtra(PREF_SCREEN_EXTRA);

        /*
         * SPRD Bug:474694 Feature:Reset Settings. @{
         * Original Android code:

        CameraSettingsFragment dialog = new CameraSettingsFragment();

         */
        String intentCameraScope = getIntent().getStringExtra(CAMERA_SCOPE);
        if ((prefKey == null && intentCameraScope != null)
                || (prefKey != null && CameraSettingsFragment.PREF_CATEGORY_ADVANCED
                        .equals(prefKey))) {
            this.mCameraScope = intentCameraScope;
        }

        CameraSettingsFragment dialog = new CameraSettingsFragment(mCameraScope);
        /* @} */
        //SPRD:fix bug537963 pull sd card when lock screen
        dialog.installIntentFilter();

        Bundle bundle = new Bundle(1);
        bundle.putString(PREF_SCREEN_EXTRA, prefKey);
        bundle.putBoolean(HIDE_ADVANCED_SCREEN, hideAdvancedScreen);
        dialog.setArguments(bundle);
        getFragmentManager().beginTransaction().replace(android.R.id.content, dialog).commit();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        return true;
    }

    public static class CameraSettingsFragment extends PreferenceFragment implements
            OnSharedPreferenceChangeListener {

        public static final String PREF_CATEGORY_RESOLUTION = "pref_category_resolution";
        public static final String PREF_CATEGORY_ADVANCED = "pref_category_advanced";
        public static final String PREF_LAUNCH_HELP = "pref_launch_help";
        public static final String PREF_CAMERA_STORAGE_PATH = "pref_camera_storage_path";
        private static final Log.Tag TAG = new Log.Tag("SettingsFragment");
        private static DecimalFormat sMegaPixelFormat = new DecimalFormat("##0.0");
        private String[] mCamcorderProfileNames;
        //SPRD:add for smile capture Bug548832
        private CameraDeviceInfo mInfos;
        private CameraDeviceInfo mInfos2;
        private String mPrefKey;
        private boolean mHideAdvancedScreen;
        private boolean mGetSubPrefAsRoot = true;
        private List<String> mSupportedStorage;
        private boolean isSupportGps = false;// SPRD: fix for bug 499642 delete location save  function
        /*
         * SPRD: mutex - Premise: Exposure =3, HDR = on; Action: Set sceneMode = action; Result:
         * HDR off. And Exposure = 0. Expected: Exposure = 3.
         */
        public static boolean mNeedCheckMutex = false;

        // Selected resolutions for the different cameras and sizes.
        private PictureSizes mPictureSizes;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle arguments = getArguments();
            if (arguments != null) {
                mPrefKey = arguments.getString(PREF_SCREEN_EXTRA);
                mHideAdvancedScreen = arguments.getBoolean(HIDE_ADVANCED_SCREEN);
            }
            Context context = this.getActivity().getApplicationContext();
            addPreferencesFromResource(R.xml.camera_preferences);
            PreferenceScreen advancedScreen =
                    (PreferenceScreen) findPreference(PREF_CATEGORY_ADVANCED);

            // If manual exposure not enabled, hide the Advanced screen.
            if (mHideAdvancedScreen) {
                PreferenceScreen root = (PreferenceScreen) findPreference("prefscreen_top");
                root.removePreference(advancedScreen);
            }

            // Allow the Helper to edit the full preference hierarchy, not the
            // sub tree we may show as root. See {@link #getPreferenceScreen()}.
            mGetSubPrefAsRoot = false;
            CameraSettingsActivityHelper.addAdditionalPreferences(this, context);
            mGetSubPrefAsRoot = true;

            mCamcorderProfileNames = getResources().getStringArray(R.array.camcorder_profile_names);
            mInfos = CameraAgentFactory
                    .getAndroidCameraAgent(context, CameraAgentFactory.CameraApi.API_1)
                    .getCameraDeviceInfo();
            //SPRD:add for smile capture Bug548832
            mInfos2 = CameraAgentFactory
                    .getAndroidCameraAgent(context, CameraAgentFactory.CameraApi.API_2)
                    .getCameraDeviceInfo();
            // SPRD: Fix bug 545710 The sAndroidCameraAgentClientCount is
            // keeping increase.
            CameraAgentFactory.recycle(CameraAgentFactory.CameraApi.API_1);
            CameraAgentFactory.recycle(CameraAgentFactory.CameraApi.API_2);
        }

        @Override
        public void onResume() {
            super.onResume();
            /*
             * SPRD modify for Coverity 109125@{
             * Original Android code:
            final Activity activity = this.getActivity();
            @}*/

            // Load the camera sizes.
            loadSizes();
            loadStoageDirectories();

            // Send loaded sizes to additional preferences.
            CameraSettingsActivityHelper.onSizesLoaded(this, mPictureSizes.backCameraSizes,
                    new ListPreferenceFiller() {
                        @Override
                        public void fill(List<Size> sizes, ListPreference preference) {
                            setEntriesForSelection(sizes, preference);
                        }
                    });

            // Make sure to hide settings for cameras that don't exist on this
            // device.
            setVisibilities();

            // Put in the summaries for the currently set values.
            final PreferenceScreen resolutionScreen =
                    (PreferenceScreen) findPreference(PREF_CATEGORY_RESOLUTION);
            fillEntriesAndSummaries(resolutionScreen);
            setPreferenceScreenIntent(resolutionScreen);

            final ListPreference cameraStoragePath = (ListPreference)findPreference(PREF_CAMERA_STORAGE_PATH);
            /*SPRD:fix bug537963 pull sd card when lock screen@{
             */
            if (cameraStoragePath!=null) {
            SettingsManager settingsManager = new SettingsManager(mContext);
            if ("External".equals(settingsManager.getString(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_CAMERA_STORAGE_PATH))
                    && !StorageUtil.getStoragePathState(StorageUtil.KEY_DEFAULT_EXTERNAL)) {
                Log.d(TAG, "sd card is unmounted");
                cameraStoragePath.setValue(mContext
                        .getString(R.string.storage_path_internal_default));
                settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_STORAGE_PATH,
                        MultiStorage.KEY_DEFAULT_INTERNAL);
            }
            /*@}*/
            cameraStoragePath.setSummary(cameraStoragePath.getEntry());
            setEntries(cameraStoragePath);
            }

            final PreferenceScreen advancedScreen =
                    (PreferenceScreen) findPreference(PREF_CATEGORY_ADVANCED);

            if (!mHideAdvancedScreen) {
                fillEntriesAndSummaries(advancedScreen);
                setPreferenceScreenIntent(advancedScreen);
            }

            /*
             * SPRD Bug:474694 Feature:Reset Settings. @{
             */
            Preference resetCamera = findPreference(Keys.KEY_CAMER_RESET);
            resetCamera.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showAlertDialog(true);
                    return true;
                }
            });

            Preference resetVideo = findPreference(Keys.KEY_VIDEO_RESET);
            resetVideo.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showAlertDialog(false);
                    return true;
                }
            });
            /* @} */

            /*
             * SPRD Bug:488399 Remove Google Help and Feedback. @{
             * Original Android code:

            Preference helpPref = findPreference(PREF_LAUNCH_HELP);
            helpPref.setOnPreferenceClickListener(
                    new OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            new GoogleHelpHelper(activity).launchGoogleHelp();
                            return true;
                        }
                    });

             */

            if (UCamUtill.isGifEnable()) {
                Preference resetGif = findPreference(Keys.KEY_GIF_RESET);
                resetGif.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showGifAlertDialog();
                        return true;
                    }
                });
            }
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        /**
         * Configure home-as-up for sub-screens.
         */
        private void setPreferenceScreenIntent(final PreferenceScreen preferenceScreen) {
            Intent intent = new Intent(getActivity(), CameraSettingsActivity.class);
            intent.putExtra(PREF_SCREEN_EXTRA, preferenceScreen.getKey());

            // SPRD Bug:474694 Feature:Reset Settings.
            if (PREF_CATEGORY_ADVANCED.equals(preferenceScreen.getKey())) {
                if (mCameraScope != null) {
                    intent.putExtra(CAMERA_SCOPE, mCameraScope);
                }
            }

            preferenceScreen.setIntent(intent);
        }

        /**
         * This override allows the CameraSettingsFragment to be reused for
         * different nested PreferenceScreens within the single camera
         * preferences XML resource. If the fragment is constructed with a
         * desired preference key (delivered via an extra in the creation
         * intent), it is used to look up the nested PreferenceScreen and
         * returned here.
         */
        @Override
        public PreferenceScreen getPreferenceScreen() {
            PreferenceScreen root = super.getPreferenceScreen();
            if (!mGetSubPrefAsRoot || mPrefKey == null || root == null) {
                return root;
            } else {
                PreferenceScreen match = findByKey(root, mPrefKey);
                if (match != null) {
                    return match;
                } else {
                    throw new RuntimeException("key " + mPrefKey + " not found");
                }
            }
        }

        private PreferenceScreen findByKey(PreferenceScreen parent, String key) {
            if (key.equals(parent.getKey())) {
                return parent;
            } else {
                for (int i = 0; i < parent.getPreferenceCount(); i++) {
                    Preference child = parent.getPreference(i);
                    if (child instanceof PreferenceScreen) {
                        PreferenceScreen match = findByKey((PreferenceScreen) child, key);
                        if (match != null) {
                            return match;
                        }
                    }
                }
                return null;
            }
        }

        /**
         * Depending on camera availability on the device, this removes settings
         * for cameras the device doesn't have.
         */
        private void setVisibilities() {
            /* PRD: fix for bug 499642 delete location save function @{ */
            PreferenceGroup resolutions_gategory =
                    (PreferenceScreen) findPreference("prefscreen_top");
            PreferenceGroup resolutions_advanced =
                    (PreferenceGroup) findPreference(PREF_CATEGORY_ADVANCED);
            if (!isSupportGps) {
                recursiveDelete(resolutions_gategory, findPreference(Keys.KEY_RECORD_LOCATION));
            }
            /* @} */
            PreferenceGroup resolutions =
                    (PreferenceGroup) findPreference(PREF_CATEGORY_RESOLUTION);

            if (mPictureSizes.backCameraSizes.isEmpty()) {
                recursiveDelete(resolutions,
                        findPreference(Keys.KEY_PICTURE_SIZE_BACK));
                recursiveDelete(resolutions,
                        findPreference(Keys.KEY_VIDEO_QUALITY_BACK));
            }
            if (mPictureSizes.frontCameraSizes.isEmpty()) {
                recursiveDelete(resolutions,
                        findPreference(Keys.KEY_PICTURE_SIZE_FRONT));
                recursiveDelete(resolutions,
                        findPreference(Keys.KEY_VIDEO_QUALITY_FRONT));
            }
            /*sunvov:dlj add for 151014 start*/
            if(!OptConfig.SUN_CAMERA_SCREEN){
                recursiveDelete(resolutions,findPreference(Keys.KEY_CAMERA_FULL_PREVIEW));
                recursiveDelete(resolutions,findPreference(Keys.KEY_VIDEO_FULL_PREVIEW));
            }            
            if(!OptConfig.SUNVOV_SPECIAL_FOCUS){
                 recursiveDelete(resolutions_gategory, findPreference(Keys.KEY_CAMERA_FOCUS_SOUND));
            }          
            /* SPRD:Fix bug 447953,548832 @{ */
            if (mInfos2 != null && !mInfos2.getSmileEnable()) {
                ListPreference keyPreference =
                        (ListPreference) findPreference(Keys.KEY_CAMERA_AI_DATECT);
                keyPreference.setEntries(R.array.pref_camera_ai_detect_entries_removesmile);
            }
            /* @} */
            if(!UCamUtill.isTimeStampEnable()) {
                recursiveDelete(resolutions_advanced,
                        findPreference(Keys.KEY_CAMERA_TIME_STAMP));
            }

            if(!CameraUtil.isZslEnable()) {
                recursiveDelete(resolutions_advanced,
                        findPreference(Keys.KEY_CAMERA_ZSL_DISPLAY));
            }

            if (!UCamUtill.isGifEnable()) {
                recursiveDelete(resolutions_advanced, findPreference(Keys.KEY_GIF_ADVANCED_SETTINGS));
            }

            /* SPRD:Fix bug 531780 @{ */
            SettingsManager settingsManager = new SettingsManager(mContext);
            if (settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_BEAUTY_ENTERED)) {
                recursiveDelete(resolutions_advanced, findPreference(Keys.KEY_CAMERA_CONTINUE_CAPTURE));
            }
            /* @} */

            /* SPRD:Fix bug 500099 front camera need mirror function @{ */
            if(!CameraUtil.isFrontCameraMirrorEnable()) {
                recursiveDelete(resolutions_advanced,
                findPreference(Keys.KEY_FRONT_CAMERA_MIRROR));
                }
            /* @} */

            recursiveDelete(resolutions_gategory, findPreference(Keys.KEY_CAMERA_STORAGE_PATH));//Kalyy Bug 46703
        }

        /**
         * Recursively go through settings and fill entries and summaries of our
         * preferences.
         */
        private void fillEntriesAndSummaries(PreferenceGroup group) {
            for (int i = 0; i < group.getPreferenceCount(); ++i) {
                Preference pref = group.getPreference(i);
                if (pref instanceof PreferenceGroup) {
                    fillEntriesAndSummaries((PreferenceGroup) pref);
                }
                setSummary(pref);
                setEntries(pref);
            }
        }

        /**
         * Recursively traverses the tree from the given group as the route and
         * tries to delete the preference. Traversal stops once the preference
         * was found and removed.
         */
        private boolean recursiveDelete(PreferenceGroup group, Preference preference) {
            if (group == null) {
                Log.d(TAG, "attempting to delete from null preference group");
                return false;
            }
            if (preference == null) {
                Log.d(TAG, "attempting to delete null preference");
                return false;
            }
            if (group.removePreference(preference)) {
                // Removal was successful.
                return true;
            }

            for (int i = 0; i < group.getPreferenceCount(); ++i) {
                Preference pref = group.getPreference(i);
                if (pref instanceof PreferenceGroup) {
                    if (recursiveDelete((PreferenceGroup) pref, preference)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void onPause() {
            super.onPause();

            // SPRD Bug:474694 Feature:Reset Settings.
            mResetCamera = false;

            ListPreference storagePreference = (ListPreference) findPreference(Keys.KEY_CAMERA_STORAGE_PATH);
            Log.d(TAG," "+storagePreference);
            /*SPRD:fix bug537963 pull sd card when lock screen@*/
            if (storagePreference != null){
                if (storagePreference.getDialog() != null) {
                    storagePreference.getDialog().dismiss();
                }
            }
            /*@}*/
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            setSummary(findPreference(key));
            setMutexPreference(key);
        }

        /**
         * Set the entries for the given preference. The given preference needs
         * to be a {@link ListPreference}
         */
        private void setEntries(Preference preference) {
            if (!(preference instanceof ListPreference)) {
                return;
            }

            ListPreference listPreference = (ListPreference) preference;
            if (listPreference.getKey().equals(Keys.KEY_PICTURE_SIZE_BACK)) {
                /*
                 * SPRD Bug:474694 Feature:Reset Settings. @{
                 * Original Android code:

                setEntriesForSelection(mPictureSizes.backCameraSizes, listPreference);

                 */
                setEntriesForSelection(mPictureSizes.backCameraSizes, listPreference,
                        Keys.KEY_PICTURE_SIZE_BACK);
                /* @} */
            } else if (listPreference.getKey().equals(Keys.KEY_PICTURE_SIZE_FRONT)) {
                /*
                 * SPRD Bug:474694 Feature:Reset Settings. @{
                 * Original Android code:

                setEntriesForSelection(mPictureSizes.frontCameraSizes, listPreference);

                 */
                setEntriesForSelection(mPictureSizes.frontCameraSizes, listPreference,
                        Keys.KEY_PICTURE_SIZE_FRONT);
                /* @} */
            } else if (listPreference.getKey().equals(Keys.KEY_VIDEO_QUALITY_BACK)) {
                setEntriesForSelection(mPictureSizes.videoQualitiesBack.orNull(), listPreference);
            } else if (listPreference.getKey().equals(Keys.KEY_VIDEO_QUALITY_FRONT)) {
                setEntriesForSelection(mPictureSizes.videoQualitiesFront.orNull(), listPreference);
            } else if (listPreference.getKey().equals(Keys.KEY_CAMERA_STORAGE_PATH)) {
                setEntriesForSelectionStorage(mSupportedStorage, listPreference);
            /**
             * SPRD:fix bug 473462 add for burst capture @{
            }
            */
            } else if (listPreference.getKey().equals(Keys.KEY_CAMERA_CONTINUE_CAPTURE)) {
                SettingsManager settingsManager = new SettingsManager(mContext);//SPRD:fix bug474672
                if (!CameraUtil.isNinetyNineBurstEnabled()
                        || settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL,
                                Keys.KEY_CAMERA_BEAUTY_ENTERED)) {
                    setEntriesForBurst(listPreference);
                }
            }
            /**
             * @}
             */
        }

        /**
         * Set the summary for the given preference. The given preference needs
         * to be a {@link ListPreference}.
         */
        private void setSummary(Preference preference) {
            if (!(preference instanceof ListPreference)) {
                return;
            }

            ListPreference listPreference = (ListPreference) preference;
            if (listPreference.getKey().equals(Keys.KEY_PICTURE_SIZE_BACK)) {
                setSummaryForSelection(mPictureSizes.backCameraSizes,
                        listPreference);
            } else if (listPreference.getKey().equals(Keys.KEY_PICTURE_SIZE_FRONT)) {
                setSummaryForSelection(mPictureSizes.frontCameraSizes,
                        listPreference);
            } else if (listPreference.getKey().equals(Keys.KEY_VIDEO_QUALITY_BACK)) {
                setSummaryForSelection(mPictureSizes.videoQualitiesBack.orNull(), listPreference);
            } else if (listPreference.getKey().equals(Keys.KEY_VIDEO_QUALITY_FRONT)) {
                setSummaryForSelection(mPictureSizes.videoQualitiesFront.orNull(), listPreference);
            } else {
                listPreference.setSummary(listPreference.getEntry());
            }
        }

        /**
         * Sets the entries for the given list preference.
         *
         * @param selectedSizes The possible S,M,L entries the user can choose
         *            from.
         * @param preference The preference to set the entries for.
         */
        private void setEntriesForSelection(List<Size> selectedSizes,
                ListPreference preference) {
            if (selectedSizes == null) {
                return;
            }

            String[] entries = new String[selectedSizes.size()];
            String[] entryValues = new String[selectedSizes.size()];
            for (int i = 0; i < selectedSizes.size(); i++) {
                Size size = selectedSizes.get(i);
                entries[i] = getSizeSummaryString(size);
                entryValues[i] = SettingsUtil.sizeToSettingString(size);
            }
            preference.setEntries(entries);
            preference.setEntryValues(entryValues);
        }

        /**
         * Sets the entries for the given list preference.
         *
         * @param selectedQualities The possible S,M,L entries the user can
         *            choose from.
         * @param preference The preference to set the entries for.
         */
        private void setEntriesForSelection(SelectedVideoQualities selectedQualities,
                ListPreference preference) {
            if (selectedQualities == null) {
                return;
            }

            // Avoid adding double entries at the bottom of the list which
            // indicates that not at least 3 qualities are supported.
            ArrayList<String> entries = new ArrayList<String>();
            /*SUN:jicong.wang add for R6 WINDS custom start {@*/
            if (OptConfig.SUN_SUBCUSTOM_C7367_HWD_FWVGA_R6_WINDS){
                if (preference.getKey().equals(Keys.KEY_VIDEO_QUALITY_BACK)){
                     entries.add(mCamcorderProfileNames[/*selectedQualities.large*/4]);
                     
                    if (selectedQualities.medium != selectedQualities.large) {
                        entries.add(mCamcorderProfileNames[/*selectedQualities.medium*/3]);
                    }
                    if (selectedQualities.small != selectedQualities.medium) {
                        entries.add(mCamcorderProfileNames[/*selectedQualities.small*/2]);
                    }                     
                } else if (preference.getKey().equals(Keys.KEY_VIDEO_QUALITY_FRONT)){
                
                    entries.add(mCamcorderProfileNames[/*selectedQualities.large*/5]);
                    if (selectedQualities.medium != selectedQualities.large) {
                        entries.add(mCamcorderProfileNames[/*selectedQualities.medium*/4]);
                    }
                    if (selectedQualities.small != selectedQualities.medium) {                   
                        entries.add(mCamcorderProfileNames[/*selectedQualities.small*/3]);
                    }
                }

            } else {
                entries.add(mCamcorderProfileNames[selectedQualities.large]);
                if (selectedQualities.medium != selectedQualities.large) {
                    entries.add(mCamcorderProfileNames[selectedQualities.medium]);
                }
                if (selectedQualities.small != selectedQualities.medium) {
                    entries.add(mCamcorderProfileNames[selectedQualities.small]);
                }
            }
            /*SUN:jicong.wang add for R6 WINDS custom end @}*/
            preference.setEntries(entries.toArray(new String[0]));
        }

        /**
         * Sets the summary for the given list preference.
         *
         * @param displayableSizes The human readable preferred sizes
         * @param preference The preference for which to set the summary.
         */
        private void setSummaryForSelection(List<Size> displayableSizes,
                                            ListPreference preference) {
            String setting = preference.getValue();
            if (setting == null || !setting.contains("x")) {
                return;
            }
            Size settingSize = SettingsUtil.sizeFromSettingString(setting);
            if (settingSize == null || settingSize.area() == 0) {
                return;
            }
            preference.setSummary(getSizeSummaryString(settingSize));
        }

        /**
         * Sets the summary for the given list preference.
         *
         * @param selectedQualities The selected video qualities.
         * @param preference The preference for which to set the summary.
         */
        private void setSummaryForSelection(SelectedVideoQualities selectedQualities,
                ListPreference preference) {
            if (selectedQualities == null) {
                return;
            }
            int selectedQuality = selectedQualities.getFromSetting(preference.getValue());
             /*SUN:jicong.wang add for R6 WINDS custom start {@*/
            if (OptConfig.SUN_SUBCUSTOM_C7367_HWD_FWVGA_R6_WINDS){
                if (preference.getKey().equals(Keys.KEY_VIDEO_QUALITY_BACK)){
                    selectedQuality -=1;
                } else if (preference.getKey().equals(Keys.KEY_VIDEO_QUALITY_FRONT)){
                    selectedQuality +=1;
                }
            }      
             /*SUN:jicong.wang add for R6 WINDS custom start {@*/
            preference.setSummary(mCamcorderProfileNames[selectedQuality]);
        }

        /**
         * This method gets the selected picture sizes for S,M,L and populates
         * {@link #mPictureSizes} accordingly.
         */
        private void loadSizes() {
            if (mInfos == null) {
                Log.w(TAG, "null deviceInfo, cannot display resolution sizes");
                return;
            }
            PictureSizeLoader loader = new PictureSizeLoader(getActivity().getApplicationContext());
            mPictureSizes = loader.computePictureSizes();
        }

        /**
         * @param size The photo resolution.
         * @return A human readable and translated string for labeling the
         *         picture size in megapixels.
         */
        private String getSizeSummaryString(Size size) {
            Size approximateSize = ResolutionUtil.getApproximateSize(size);
            String megaPixels = sMegaPixelFormat.format((size.width() * size.height()) / 1e6);
            int numerator = ResolutionUtil.aspectRatioNumerator(approximateSize);
            int denominator = ResolutionUtil.aspectRatioDenominator(approximateSize);
            
            /* wangxing 20160704 add  start @{ */
            String result;
            if(true){//wangxing add 20160704
            	android.util.Log.i("wangxing","size.width()="+size.width()+"  size.height()="+size.height());
                if ((size.width()==3264 && size.height()==2448) || (size.width()==3584 && size.height()==2016)){
                    megaPixels = "8";
                }else if ((size.width()==2592 && size.height()==1944) || (size.width()==2988 && size.height()==1680)){
                    megaPixels = "5";
                }else if ((size.width()==2048 && size.height()==1536) || (size.width()==2400 && size.height()==1350)){
                    if(OptConfig.SUNVOV_CUSTOM_C7356_TYC_WVGA){
                        megaPixels = "3.0";
                    }else{
                        megaPixels = "3.2";
                    }
                }else if ((size.width()==1600 && size.height()==1200) || (size.width()==1920 && size.height()==1080)){
                    megaPixels = "2";
                }else if ((size.width()==1280 && size.height()==960) || (size.width()==1520 && size.height()==854)){
                    megaPixels = "1.3";
                }else if (size.width()==640 && size.height()==480){
                    return  "VGA";
                }else if (size.width()==320 && size.height()==240){
                    return  "QVGA";
                }
                /*SUN:jicong.wang add for r6 winds start {@*/
                if (OptConfig.SUN_SUBCUSTOM_C7367_HWD_FWVGA_R6_WINDS){

                    if (size.width()==1280 && size.height()==720){
                        megaPixels = "0.3";
                    }
                    if(isNumeric(megaPixels) && !megaPixels.contains(".")){
                        megaPixels +=".0";
                    }
                }
                /*SUN;jicogng.ass for r6 winds end @}*/
            	result = getResources().getString(
                    R.string.setting_summary_aspect_ratio_and_mp, numerator, denominator,
                    megaPixels);
            }else{
                result = getResources().getString(
                    R.string.setting_summary_aspect_ratio_and_megapixels, numerator, denominator,
                    megaPixels);
            }
            
            /* wangxing 20160704 add end @} */

            return result;
        }
        
        /*SUN:jicong.wang add for R6 winds start {@*/
        public static boolean isNumeric(String str){
            for (int i = 0; i < str.length(); i++){
               if (!Character.isDigit(str.charAt(i))){
                return false;
               }
            }
            return true;
        }
        /*SUN:jicong.wang add for r6 winds end @}*/
        
        /*SPRD: Add Storage check API for supportedStorage */
        public void loadStoageDirectories() {
            List<String> supportedStorage = MultiStorage.getSupportedStorage();
            if (supportedStorage != null) {
                mSupportedStorage = supportedStorage;
            }
        }

        /* SPRD: Add Storage Entries&EntrayValues API for storage setting list */
        public void setEntriesForSelectionStorage(List<String> supportedValue,
                ListPreference preference) {
            if (supportedValue == null) {
                return;
            }
            String[] entries = new String[supportedValue.size()];
            String[] entryValues = new String[supportedValue.size()];
            for (int i = 0; i < supportedValue.size(); i++) {
                String value = supportedValue.get(i);
                entries[i] = getStorageSummeryString(value);
                entryValues[i] = value;
            }
            preference.setEntries(entries);
            preference.setEntryValues(entryValues);
        }

        public String getStorageSummeryString(String value) {
            String entry = null;
            if (MultiStorage.KEY_DEFAULT_INTERNAL.equals(value)) {
                entry = getResources().getString(R.string.storage_path_internal);
            } else if (MultiStorage.KEY_DEFAULT_EXTERNAL.equals(value)) {
                entry = getResources().getString(R.string.storage_path_external);
            }
            return entry;
	}
        /**
         * SPRD: fix bug 473462 add for burst capture
         */
        private void setEntriesForBurst(ListPreference preference) {
            SettingsManager sm = new SettingsManager(mContext);

            String[] burstEntries = mContext.getResources().getStringArray(R.array.pref_camera_burst_entries);
            if (burstEntries != null) {
                String[] entries = new String[burstEntries.length - 1];
                int i = 0;
                for (String entry : burstEntries) {
                    if (!"99".equals(entry)) {
                        entries[i] = entry;
                        i++;
                    }
                }
                preference.setEntries(entries);
            }

            String[] burstCount = mContext.getResources().getStringArray(R.array.pref_camera_burst_entryvalues);
            if (burstCount != null) {
                String[] entryValues = new String[burstCount.length - 1];
                int i = 0;
                for (String entryValue : burstCount) {
                    if (!"ninetynine".equals(entryValue)) {
                        entryValues[i] = entryValue;
                        i++;
                    }
                }
                preference.setEntryValues(entryValues);
                sm.setDefaults(Keys.KEY_CAMERA_CONTINUE_CAPTURE,
                        mContext.getString(R.string.pref_camera_burst_entry_defaultvalue), entryValues);
            }
        }

        /* SPRD: setMutexPreference method is for Setting function Mutex in setting List @{ */
        public void setMutexPreference(String key) {
            Preference preference = findPreference(key);
            Log.d(TAG, "setMutexPreference key=" + key + ",preference=" + preference);
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
            }
            if (preference instanceof ManagedSwitchPreference) {
                ManagedSwitchPreference switchPreference = (ManagedSwitchPreference) preference;
            }
            SettingsManager settingsManager = new SettingsManager(mContext);
            if (preference == null || key == null) {
                return;
            }

            String toastString = null;

            // SCENE MODE MUTEX WITH HDR, WHITE BALANCE, and FLASH MODE
            if (key.equals(Keys.KEY_SCENE_MODE) && preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;

                if (!mContext.getString(R.string.pref_camera_scenemode_default)
                        .equals(listPreference.getValue())) {
                    // SCENE MODE - HDR
                    if (Keys.isHdrOn(settingsManager)) {
                        //settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SCENE_MODE,listPreference.getValue());
                        settingsManager.set(SettingsManager.SCOPE_GLOBAL,Keys.KEY_CAMERA_HDR, false);
                        toastString = mContext.getString(R.string.scene_mutex);
                        mNeedCheckMutex = true;
                    }

                    // SCENE MODE - FLASH MODE
                    //SPRD: Fix bug 542565
                    String cameraScope = "_preferences_camera_0";
                    if (!"off".equals(settingsManager.getString(cameraScope,
                            Keys.KEY_FLASH_MODE))) {
                        settingsManager.set(cameraScope, Keys.KEY_FLASH_MODE, "off");
                        toastString = mContext.getString(R.string.scene_mutex);
                    }

                    // SCENE MODE - WHITE BALANCE
                    ListPreference whiteBalancePreference = (ListPreference) findPreference(Keys.KEY_WHITE_BALANCE);
                    whiteBalancePreference.setValue(mContext
                            .getString(R.string.pref_camera_whitebalance_default));

                    /* SPRD:fix bug534665 add some mutex about scene mode@{ */
                    // SCENE MODE - EXPOSURE
                    if (0 != settingsManager.getInteger(mCameraScope,
                            Keys.KEY_EXPOSURE)) {
                        settingsManager.set(mCameraScope, Keys.KEY_EXPOSURE, "0");
                        toastString = mContext.getString(R.string.scene_mutex);
                    }

                    //SPRD:fix bug534665 add some mutex about scene mode
                    // SCENE MODE - ISO
                    ListPreference ISOPreference = (ListPreference) findPreference(Keys.KEY_CAMERA_ISO);
                    ISOPreference.setValue(mContext.getString(R.string.pref_entry_value_auto));
                    // SCENE MODE - METER
                    String sceneMode = settingsManager.getString(SettingsManager.SCOPE_GLOBAL,Keys.KEY_SCENE_MODE);
                    ListPreference meteringPreference = (ListPreference) findPreference(Keys.KEY_CAMER_METERING);
                    if(sceneMode.equals("landscape")){
                        meteringPreference.setValue(mContext
                                .getString(R.string.pref_camera_metering_entry_value_frame_average));
                    }
                    if(sceneMode.equals("portrait")){
                        meteringPreference.setValue(mContext
                                .getString(R.string.pref_camera_metering_entry_value_center_weighted));
                    }
                    /*@}*/
                    if (toastString != null) {
                        showMutexToast(toastString);
                    }
                }
                return;
            }

            //SPRD:fix bug534665 add some mutex about scene mode
            // METER - SCENE MODE
            if (key.equals(Keys.KEY_CAMER_METERING) && preference instanceof ListPreference) {
                String sceneMode = settingsManager.getString(SettingsManager.SCOPE_GLOBAL,
                        Keys.KEY_SCENE_MODE);
                String meterMode = settingsManager.getString(SettingsManager.SCOPE_GLOBAL,
                        Keys.KEY_CAMER_METERING);
                if ((sceneMode.equals("landscape") && !meterMode.equals("frameaverage"))
                        || (sceneMode.equals("portrait") && !meterMode.equals("centerweighted"))) {
                    ListPreference sceneModePreference = (ListPreference) findPreference(Keys.KEY_SCENE_MODE);
                    sceneModePreference.setValue(mContext
                            .getString(R.string.pref_camera_scenemode_default));
                }
                return;
            }

            //SPRD:fix bug534665 add some mutex about scene mode
            // ISO - SCENE MODE, HDR
            if (key.equals(Keys.KEY_CAMERA_ISO) && preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;

                if (!mContext.getString(R.string.pref_entry_value_auto)
                        .equals(listPreference.getValue())) {
                    ListPreference sceneModePreference = (ListPreference) findPreference(Keys.KEY_SCENE_MODE);
                    sceneModePreference.setValue(mContext
                            .getString(R.string.pref_camera_scenemode_default));
                    /* SPRD: Add for bug 549909, mutex ISO - HDR @{ */
                    // ISO - HDR
                    if (Keys.isHdrOn(settingsManager)) {
                        settingsManager.set(SettingsManager.SCOPE_GLOBAL,Keys.KEY_CAMERA_HDR, false);
                        toastString = mContext.getString(R.string.mutex_hdr);
                        mNeedCheckMutex = true;
                    }
                    if (toastString != null) {
                        showMutexToast(toastString);
                    }
                    /* @} */
                }
                return;
            }

            // WHITE_BALANCE - HDR, SCENE MODE
            if (key.equals(Keys.KEY_WHITE_BALANCE) && preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                if (!mContext.getString(R.string.pref_camera_whitebalance_default)
                        .equals(listPreference.getValue())) {
                    // WHITE_BALANCE - HDR
                    if (Keys.isHdrOn(settingsManager)) {
                        // settingsManager.set(mCameraScope,
                        // Keys.KEY_SCENE_MODE,listPreference.getValue());
                        settingsManager.set(SettingsManager.SCOPE_GLOBAL,Keys.KEY_CAMERA_HDR, false);
                        toastString = mContext.getString(R.string.mutex_hdr);
                        mNeedCheckMutex = true;
                    }
                    // WHITE_BALANCE - SCENE MODE
                    ListPreference sceneModePreference = (ListPreference) findPreference(Keys.KEY_SCENE_MODE);
                    sceneModePreference.setValue(mContext
                            .getString(R.string.pref_camera_scenemode_default));

                    if (toastString != null) {
                        showMutexToast(toastString);
                    }
                }
                return;
            }

            // CONTINUE_CAPTURE MUTEX WITH HDR, KEY_FREEZE_FRAME_DISPLAY, and FLASH MODE, SMILE, TIMESTAMPS
            if (key.equals(Keys.KEY_CAMERA_CONTINUE_CAPTURE)
                    && preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                if (!mContext.getString(R.string.pref_camera_burst_entry_defaultvalue)
                        .equals(listPreference.getValue())) {

                    // CONTINUE_CAPTURE - HDR
                    String cameraScope = "_preferences_camera_0";
                    if (Keys.isHdrOn(settingsManager)) {
                        // settingsManager.set(mCameraScope,
                        // Keys.KEY_SCENE_MODE,listPreference.getValue());
                        //SPRD: Fix bug 572156
                        String PREF_BEFORE="PREF_BEFORE";
                        settingsManager.set(cameraScope, Keys.KEY_FLASH_MODE+ PREF_BEFORE, "off");
                        settingsManager.set(SettingsManager.SCOPE_GLOBAL,Keys.KEY_CAMERA_HDR, false);
                        toastString = mContext.getString(R.string.countine_mutex);
                        mNeedCheckMutex = true;
                    }

                    // CONTINUE_CAPTURE - KEY_FREEZE_FRAME_DISPLAY
                    ManagedSwitchPreference freezeFrameDisplayPreference =
                            (ManagedSwitchPreference) findPreference(Keys.KEY_FREEZE_FRAME_DISPLAY);
                    freezeFrameDisplayPreference.setChecked(false);

                    // CONTINUE_CAPTURE - FLASH MODE
                    //SPRD: Fix bug 542565
                    if (!"off".equals(settingsManager.getString(cameraScope, Keys.KEY_FLASH_MODE))) {
                        // flash auto or on
                        settingsManager.set(cameraScope, Keys.KEY_FLASH_MODE, "off");
                        toastString = mContext.getString(R.string.countine_mutex);
                    }

                    // CONTINUE_CAPTURE - SMILE
                    if ("smile".equals(settingsManager.getString(SettingsManager.SCOPE_GLOBAL,
                            Keys.KEY_CAMERA_AI_DATECT))) {
                        ListPreference aiPreference = (ListPreference) findPreference(Keys.KEY_CAMERA_AI_DATECT);
                        aiPreference.setValue(Keys.CAMERA_AI_DATECT_VAL_OFF);
                        toastString = mContext.getString(R.string.countine_mutex);
                    }

                    // CONTINUE_CAPTURE - TIMESTAMPS
                    if (settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_TIME_STAMP)) {
                        ManagedSwitchPreference tsPreference = (ManagedSwitchPreference) findPreference(Keys.KEY_CAMERA_TIME_STAMP);
                        if (tsPreference != null) {
                            tsPreference.setChecked(false);
                            toastString = mContext.getString(R.string.countine_mutex);
                        }
                    }

                    // CONTINUE_CAPTURE - MIRROR - Bug 545447
                    if (settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL,
                            Keys.KEY_FRONT_CAMERA_MIRROR)) {
                        ManagedSwitchPreference mirrPreference = (ManagedSwitchPreference) findPreference(Keys.KEY_FRONT_CAMERA_MIRROR);
                        if (mirrPreference != null) {
                            mirrPreference.setChecked(false);
                            toastString = mContext.getString(R.string.countine_mutex);
                        }
                    }

                    /*SUN:jicong.wang add for sub camera start {@ */
                    String cameraScope_sub = "_preferences_camera_1";
                    
                    if (!"off".equals(settingsManager.getString(cameraScope_sub,
                            Keys.KEY_FLASH_MODE))) {
                        settingsManager.set(cameraScope_sub, Keys.KEY_FLASH_MODE, "off");
                        toastString = mContext.getString(R.string.scene_mutex);
                    }

                    /*SUN:jicong.wang add for sub camera end @}*/                    
                    if (toastString != null) {
                        showMutexToast(toastString);
                    }
                }
                return;
            }
            // KEY_FREEZE_FRAME_DISPLAY - CONTINUE_CAPTURE
            if (key.equals(Keys.KEY_FREEZE_FRAME_DISPLAY)
                    && preference instanceof ManagedSwitchPreference) {
                ManagedSwitchPreference switchPreference = (ManagedSwitchPreference) preference;
                if (switchPreference.isChecked()) {
                    ListPreference burstPreference = (ListPreference) findPreference(Keys.KEY_CAMERA_CONTINUE_CAPTURE);
                    /* SPRD add for bug 533661 @{ */
                    if (burstPreference == null) {
                        Log.e(TAG, "burstPreference is null !");
                        return;
                    }
                    /* @} */
                    burstPreference.setValue(mContext
                            .getString(R.string.pref_camera_burst_entry_defaultvalue));
                }
                return;
            }

            //ZSL MUTEX COUNTDOWN FLASH BEGIN
            if (key.equals(Keys.KEY_CAMERA_ZSL_DISPLAY)
                    && preference instanceof ManagedSwitchPreference) {
                ManagedSwitchPreference switchPreference = (ManagedSwitchPreference) preference;
                if(!switchPreference.isChecked()){
                    return;
                }
                //SPRD:fix bug545327 flash is not restored when zsl on and reset photomodule 
                if(1 == settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL,Keys.KEY_CAMERA_ZSL_DISPLAY)){
                    int countDownDuration = settingsManager.getInteger(
                            SettingsManager.SCOPE_GLOBAL,
                            Keys.KEY_COUNTDOWN_DURATION);
                    //SPRD: Fix bug 542565
                    String cameraScope = "_preferences_camera_0";
                    String flash = settingsManager.getString(cameraScope,
                            Keys.KEY_FLASH_MODE);
                    //SPRD Bug:542367 HDR MUTEX WITH ZSL BEGIN
                    // SPRD: Fix bug 564279, remove mutex of zsl and flash
                    /*
                    if (countDownDuration > 0 || !"off".equals(flash)||Keys.isHdrOn(settingsManager)) {
                    */
                    if (countDownDuration > 0 || Keys.isHdrOn(settingsManager)) {
                        settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                                Keys.KEY_COUNTDOWN_DURATION);
                        /*
                        settingsManager.set(cameraScope, Keys.KEY_FLASH_MODE, "off");
                        */
                        settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                                Keys.KEY_CAMERA_HDR);
                        /*Toast.makeText(mContext, R.string.zsl_on_countdown_off,
                                Toast.LENGTH_SHORT).show();*/
                        Toast.makeText(mContext, R.string.zsl_mutex,
                                Toast.LENGTH_SHORT).show();
                    }//SPRD Bug:542367 HDR MUTEX WITH ZSL END
                }
                return;
            }
            //ZSL MUTEX COUNTDOWN FLASH END

            //SMILE MUTEX COUNTDOWN , CONTINUE_CAPTURE BEGIN
            if (key.equals(Keys.KEY_CAMERA_AI_DATECT) && preference instanceof ListPreference){
                String mface = settingsManager.getString(SettingsManager.SCOPE_GLOBAL,
                        Keys.KEY_CAMERA_AI_DATECT);
                if ("smile".equals(mface)) {
                    // SMILE - COUNTDOWN
                    if (!"0".equals(settingsManager.getString(SettingsManager.SCOPE_GLOBAL,
                            Keys.KEY_COUNTDOWN_DURATION))) {
                        settingsManager.set(SettingsManager.SCOPE_GLOBAL,
                                Keys.KEY_COUNTDOWN_DURATION, "0");
                        toastString = mContext.getString(R.string.smile_mutex);
                    }

                    // SMILE - CONTINUE_CAPTURE
                    ListPreference burstPreference = (ListPreference) findPreference(Keys.KEY_CAMERA_CONTINUE_CAPTURE);
                    /* SPRD add for bug 533661 @{ */
                    if (burstPreference == null) {
                        Log.e(TAG, "burstPreference is null !");
                        return;
                    }
                    /* @} */
                    if (!mContext.getString(R.string.pref_camera_burst_entry_defaultvalue)
                            .equals(burstPreference.getValue())) {
                        burstPreference.setValue(mContext
                                .getString(R.string.pref_camera_burst_entry_defaultvalue));
                        toastString = mContext.getString(R.string.smile_mutex);
                    }
                    if (toastString != null) {
                        showMutexToast(toastString);
                    }
                }
                return;
            }
            // SMILE MUTEX COUNTDOWN , CONTINUE_CAPTURE END

            // COLOR EFFECT - HDR
            if (key.equals(Keys.KEY_CAMERA_COLOR_EFFECT) && preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                if (!mContext.getString(R.string.pref_camera_color_effect_entry_value_none)
                        .equals(listPreference.getValue())) {
                    // COLOR EFFECT - HDR
                    if (Keys.isHdrOn(settingsManager)) {
                        // settingsManager.set(mCameraScope,
                        // Keys.KEY_SCENE_MODE,listPreference.getValue());
                        settingsManager.set(SettingsManager.SCOPE_GLOBAL,Keys.KEY_CAMERA_HDR, false);
                        toastString = mContext.getString(R.string.mutex_hdr);
                        mNeedCheckMutex = true;
                    }
                    if (toastString != null) {
                        showMutexToast(toastString);
                    }
                }
                return;
            }

            // ISO - HDR
            if (key.equals(Keys.KEY_CAMERA_ISO) && preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                if (!mContext.getString(R.string.pref_entry_value_auto)
                        .equals(listPreference.getValue())) {
                    if (Keys.isHdrOn(settingsManager)) {
                        // settingsManager.set(mCameraScope,
                        // Keys.KEY_SCENE_MODE,listPreference.getValue());
                        settingsManager.set(SettingsManager.SCOPE_GLOBAL,Keys.KEY_CAMERA_HDR, false);
                        toastString = mContext.getString(R.string.mutex_hdr);
                        mNeedCheckMutex = true;
                    }
                    if (toastString != null) {
                        showMutexToast(toastString);
                    }
                }
                return;
            }

            // CONTRAST - HDR
            if (key.equals(Keys.KEY_CAMERA_CONTRAST) && preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                if (!mContext.getString(R.string.pref_contrast_entry_defaultvalue)
                        .equals(listPreference.getValue())) {
                    if (Keys.isHdrOn(settingsManager)) {
                        // settingsManager.set(mCameraScope,
                        // Keys.KEY_SCENE_MODE,listPreference.getValue());
                        settingsManager.set(SettingsManager.SCOPE_GLOBAL,Keys.KEY_CAMERA_HDR, false);
                        toastString = mContext.getString(R.string.mutex_hdr);
                        mNeedCheckMutex = true;
                    }
                    if (toastString != null) {
                        showMutexToast(toastString);
                    }
                }
                return;
            }

            // SATURATION - HDR
            if (key.equals(Keys.KEY_CAMERA_SATURATION) && preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                if (!mContext.getString(R.string.pref_saturation_entry_defaultvalue)
                        .equals(listPreference.getValue())) {
                    if (Keys.isHdrOn(settingsManager)) {
                        // settingsManager.set(mCameraScope,
                        // Keys.KEY_SCENE_MODE,listPreference.getValue());
                        settingsManager.set(SettingsManager.SCOPE_GLOBAL,Keys.KEY_CAMERA_HDR, false);
                        toastString = mContext.getString(R.string.mutex_hdr);
                        mNeedCheckMutex = true;
                    }
                    if (toastString != null) {
                        showMutexToast(toastString);
                    }
                }
                return;
            }

            // BRIGHTNESS - HDR
            if (key.equals(Keys.KEY_CAMERA_BRIGHTNESS) && preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                if (!mContext.getString(R.string.pref_brightness_entry_defaultvalue)
                        .equals(listPreference.getValue())) {
                    if (Keys.isHdrOn(settingsManager)) {
                        // settingsManager.set(mCameraScope,
                        // Keys.KEY_SCENE_MODE,listPreference.getValue());
                        settingsManager.set(SettingsManager.SCOPE_GLOBAL,Keys.KEY_CAMERA_HDR, false);
                        toastString = mContext.getString(R.string.mutex_hdr);
                        mNeedCheckMutex = true;
                    }
                    if (toastString != null) {
                        showMutexToast(toastString);
                    }
                }
                return;
            }

            // SLOW MOTION - TIME LAPSE
            if (key.equals(Keys.KEY_VIDEO_SLOW_MOTION)
                    && preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                if (!mContext.getString(R.string.pref_entry_value_one).equals(
                        listPreference.getValue())) {
                    ListPreference timeLapsePreference = (ListPreference) findPreference(Keys.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL);
                    timeLapsePreference
                            .setValue(mContext.getString(R.string.pref_timelapse_entry_value_default));
                }
                return;
            }

            // TIME LAPSE - SLOW MOTION
            if (key.equals(Keys.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL)
                    && preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                if (!mContext.getString(
                        R.string.pref_timelapse_entry_value_default).equals(
                        listPreference.getValue())) {
                    ListPreference slowMotionPreference = (ListPreference) findPreference(Keys.KEY_VIDEO_SLOW_MOTION);
                    slowMotionPreference.setValue(mContext.getString(R.string.pref_entry_value_one));
                }
                return;
            }

            // TIMESTAMPS - CONTINUE_CAPTURE
            if (key.equals(Keys.KEY_CAMERA_TIME_STAMP) && preference instanceof ManagedSwitchPreference) {
                ManagedSwitchPreference switchPreference = (ManagedSwitchPreference) preference;
                if (switchPreference.isChecked()) {
                    ListPreference burstPreference = (ListPreference) findPreference(Keys.KEY_CAMERA_CONTINUE_CAPTURE);
                    if (burstPreference == null) return;

                    if (!mContext.getString(R.string.pref_camera_burst_entry_defaultvalue)
                            .equals(burstPreference.getValue())) {
                        burstPreference.setValue(mContext
                                .getString(R.string.pref_camera_burst_entry_defaultvalue));
                        toastString = mContext.getString(R.string.timestamps_mutex);
                    }
                    if (toastString != null) {
                        showMutexToast(toastString);
                    }
                }
                return;
            }
            // MIRROR - CONTINUE_CAPTURE - Bug 545447
            if (key.equals(Keys.KEY_FRONT_CAMERA_MIRROR)&& preference instanceof ManagedSwitchPreference) {
                ManagedSwitchPreference switchPreference = (ManagedSwitchPreference) preference;
                if (switchPreference.isChecked()) {
                    ListPreference burstPreference = (ListPreference) findPreference(Keys.KEY_CAMERA_CONTINUE_CAPTURE);
                    if (burstPreference == null)
                        return;

                    if (!mContext.getString(R.string.pref_camera_burst_entry_defaultvalue)
                            .equals(burstPreference.getValue())) {
                        burstPreference.setValue(mContext
                                .getString(R.string.pref_camera_burst_entry_defaultvalue));
                        toastString = mContext.getString(R.string.mirror_mutex);
                    }
                    if (toastString != null) {
                        showMutexToast(toastString);
                    }
                }
                return;
            }

        }

        private void showMutexToast(String toastString) {
            Toast.makeText(mContext, toastString, Toast.LENGTH_LONG).show();
        }
        /* @}*/

        /*
         * SPRD Bug:474694 Feature:Reset Settings. @{
         */
        public String mCameraScope;

        public CameraSettingsFragment(String mCameraScope) {
            this.mCameraScope = mCameraScope;
        }

        public CameraSettingsFragment() {
            mPrefKey = null;
        }

        public void showAlertDialog(final boolean isCamera) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            final AlertDialog alertDialog = builder.create();
            builder.setTitle(isCamera ? mContext.getString(R.string.pref_restore_detail) : mContext
                    .getString(R.string.pref_video_restore_detail));
            builder.setMessage(isCamera ? mContext.getString(R.string.restore_message) : mContext
                    .getString(R.string.video_restore_message));
            builder.setPositiveButton(mContext.getString(R.string.restore_done),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mResetCamera = true;
                            SettingsManager sm = new SettingsManager(mContext);
                            if (isCamera) {
                                sm.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMER_RESET, true);
                                resetCameraSettings(sm);
                            } else {
                                sm.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_VIDEO_RESET, true);
                                resetVideoSettings(sm);
                            }
                            for (int i = 0; i < contexts.size(); i++) {
                                Context context = contexts.get(i);
                                ((CameraSettingsActivity) context).finish();
                            }
                            contexts.clear();
                           mResetCamera = false;
                        }
                    });
            builder.setNegativeButton(mContext.getString(R.string.restore_cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            alertDialog.dismiss();
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        private void resetGifSettings(SettingsManager settingsManager) {
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_GIF_MODE_PIC_SIZE);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_GIF_MODE_NUM_SIZE);
            /* SPRD: fix bug540582 set default for flash @{ */
            if (mCameraScope != null) {
                settingsManager.setToDefault("_preferences_camera_0", Keys.KEY_GIF_FLASH_MODE);
                settingsManager.setToDefault("_preferences_camera_1", Keys.KEY_GIF_FLASH_MODE);
            }
            /* @} */
        }

        public void showGifAlertDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            final AlertDialog alertDialog = builder.create();
            builder.setTitle(mContext.getString(R.string.pref_gif_restore_detail));
            builder.setMessage(mContext.getString(R.string.gif_restore_message));
            builder.setPositiveButton(mContext.getString(R.string.restore_done),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SettingsManager sm = new SettingsManager(mContext);
                            sm.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_GIF_RESET, true);
                            resetGifSettings(sm);
                            for (int i = 0; i < contexts.size(); i++) {
                                Context context = contexts.get(i);
                                ((CameraSettingsActivity) context).finish();
                            }
                            contexts.clear();
                        }
                    });
            builder.setNegativeButton(mContext.getString(R.string.restore_cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub 236
                            alertDialog.dismiss();
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        private void resetCameraSettings(SettingsManager settingsManager) {
            // SPRD Bug:494930 Do not show Location Dialog when resetting settings.
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION,
                    Keys.RECORD_LOCATION_OFF);//SPRD: fix for bug 499642 delete location save function
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_PICTURE_SIZE_FRONT);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_PICTURE_SIZE_BACK);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_COUNTDOWN_DURATION);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_CAMER_ANTIBANDING);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SCENE_MODE);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_FOCUS_MODE);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_WHITE_BALANCE);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_JPEG_QUALITY);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_CAMERA_GRID_LINES);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_SHOULD_SHOW_REFOCUS_VIEWER_CLING);
            settingsManager
                    .setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HDR_PLUS_FLASH_MODE);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_SHOULD_SHOW_SETTINGS_BUTTON_CLING);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_FREEZE_FRAME_DISPLAY);
            settingsManager
                    .setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_COLOR_EFFECT);
            settingsManager
                    .setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_CONTINUE_CAPTURE);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ISO);
            Keys.setManualExposureCompensation(settingsManager, false);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_JPEG_QUALITY);
            Map<String, String> mStorage = StorageUtil.supportedRootDirectory();
            String external = mStorage.get(StorageUtil.KEY_DEFAULT_EXTERNAL);
            if (null == external) {
                settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_STORAGE_PATH,
                        MultiStorage.KEY_DEFAULT_INTERNAL);
            } else {
                settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_STORAGE_PATH,
                        MultiStorage.KEY_DEFAULT_EXTERNAL);
            }
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMER_METERING);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_CONTRAST);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_CAMERA_SATURATION);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_CAMERA_BRIGHTNESS);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_CAMERA_AI_DATECT);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_CAMERA_SHUTTER_SOUND);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_CAMERA_FOCUS_SOUND);        
            // SPRD Bug:513927 reset Makeup
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_CAMERA_BEAUTY_ENTERED);
            settingsManager.set(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_MAKEUP_MODE_LEVEL, mContext.getResources().getInteger(R.integer.ucam_makup_default_value));
            // SPRD bug:528308 reset time_stamp
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_TIME_STAMP);

            if (mCameraScope != null) {
                settingsManager.setToDefault("_preferences_camera_0", Keys.KEY_FLASH_MODE);
                /**
                 * SPRD: mutex - pay attention to specialMutexDefault() which said the reason.
                settingsManager.setToDefault("_preferences_camera_1", Keys.KEY_FLASH_MODE);
                */
                Keys.specialMutexDefault(settingsManager);
            }

            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ZSL_DISPLAY);
            //SPRD:fix bug545312 reset mirror
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_FRONT_CAMERA_MIRROR);
            // settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
            // Keys.KEY_TOUCH_TO_CAPTURE);
            // settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_VGESTURE);
			
            /*SUN:jicong.wang modify for default size start {@*/
            String default_back_picturesize_size = getResources().getString(R.string.pref_camera_back_picturesize_default);
            if(default_back_picturesize_size.split("x").length == 2){
                 settingsManager.set(SettingsManager.SCOPE_GLOBAL,Keys.KEY_PICTURE_SIZE_BACK,default_back_picturesize_size);              
             }
            String default_front_picturesize_size = getResources().getString(R.string.pref_camera_front_picturesize_default);
            if(default_front_picturesize_size.split("x").length == 2){
                 settingsManager.set(SettingsManager.SCOPE_GLOBAL,Keys.KEY_PICTURE_SIZE_FRONT,default_front_picturesize_size);                  
             }            
            /*SUN:jicong.wang modify for default size end @}*/    			
        }

        private void resetVideoSettings(SettingsManager settingsManager) {
            // SPRD Bug:494930 Do not show Location Dialog when resetting settings.
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION,
                    Keys.RECORD_LOCATION_OFF);//SPRD: fix for bug 499642 delete location save function
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_VIDEO_QUALITY_BACK);
            settingsManager
                    .setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_VIDEO_QUALITY_FRONT);
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_VIDEO_ENCODE_TYPE);

            // SPRD: for bug 509708 add time lapse
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL);

            // SPRD: for bug 532100 set default slow for DV
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_VIDEO_SLOW_MOTION);

            if (mCameraScope != null) {
                settingsManager.setToDefault("_preferences_camera_0",
                        Keys.KEY_VIDEOCAMERA_FLASH_MODE);
                settingsManager.setToDefault("_preferences_camera_1",
                        Keys.KEY_VIDEOCAMERA_FLASH_MODE);
            }
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_CAMERA_GRID_LINES);
            Map<String, String> mStorage = StorageUtil.supportedRootDirectory();
            String external = mStorage.get(StorageUtil.KEY_DEFAULT_EXTERNAL);
            if (null == external) {
                settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_STORAGE_PATH,
                        MultiStorage.KEY_DEFAULT_INTERNAL);
            } else {
                settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_STORAGE_PATH,
                        MultiStorage.KEY_DEFAULT_EXTERNAL);
            }

            /* SPRD Bug:495676 set default antibanding for DV */
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_VIDEO_ANTIBANDING);

            // settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL,
            // Keys.KEY_VIDEO_SLOW_MOTION);
        }

        private void setEntriesForSelection(List<Size> selectedSizes,
                ListPreference preference, String key) {
            if (selectedSizes == null) {
                return;
            }

            SettingsManager sm = new SettingsManager(mContext);
            String mDefault = null;

            String[] entries = new String[selectedSizes.size()];
            String[] entryValues = new String[selectedSizes.size()];
            for (int i = 0; i < selectedSizes.size(); i++) {
                Size size = selectedSizes.get(i);
                entries[i] = getSizeSummaryString(size);
                entryValues[i] = SettingsUtil.sizeToSettingString(size);
                if (i == 0) {
                    mDefault = entryValues[0];
                }
            }
            preference.setEntries(entries);
            preference.setEntryValues(entryValues);
            sm.setDefaults(key, mDefault, entryValues);
        }
        /* @} */
        /*SPRD:fix bug537963 pull sd card when lock screen
         * @{
         */
        private void installIntentFilter() {
            // install an intent filter to receive SD card related events.
            IntentFilter intentFilter =
                    new IntentFilter(Intent.ACTION_MEDIA_EJECT);
            intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
            intentFilter.addDataScheme("file");
            mReceiver = new MyBroadcastReceiver();
            mContext.registerReceiver(mReceiver, intentFilter);
        }

        private BroadcastReceiver mReceiver = null;
        private class MyBroadcastReceiver extends BroadcastReceiver {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "onReceive action="+action);
                if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                    ListPreference storagePreference = (ListPreference) findPreference(Keys.KEY_CAMERA_STORAGE_PATH);
                    
                    //qiuyaobo,20170511,for Bug 53329, begin
                    if(storagePreference != null){ 
                    //qiuyaobo,20170511,for Bug 53329, end
                    
		                    if (storagePreference.getDialog() != null) {
		                        storagePreference.getDialog().dismiss();
		                    }
		                    loadStoageDirectories();
		                    if (mSupportedStorage.size() == 1) {
		                        storagePreference.setValue(mContext
		                                .getString(R.string.storage_path_internal_default));
		                        SettingsManager settingsManager = new SettingsManager(mContext);
		                        settingsManager.set(SettingsManager.SCOPE_GLOBAL,
		                                Keys.KEY_CAMERA_STORAGE_PATH,
		                                MultiStorage.KEY_DEFAULT_INTERNAL);
		                        setEntriesForSelectionStorage(mSupportedStorage, storagePreference);
		                    } else if (mSupportedStorage.size() == 0) {
		                        storagePreference.setValue("");
		                        SettingsManager settingsManager = new SettingsManager(mContext);
		                        settingsManager.set(SettingsManager.SCOPE_GLOBAL,
		                                Keys.KEY_CAMERA_STORAGE_PATH, "");
		                        setEntriesForSelectionStorage(mSupportedStorage, storagePreference);
		                    }
		                    
		                //qiuyaobo,20170511,for Bug 53329, begin    
		                }    
		                //qiuyaobo,20170511,for Bug 53329, end
                }
            }
        }
        /*@}*/
    }

    /*
     * SPRD Bug:474694 Feature:Reset Settings. @{
     */
    public static final String CAMERA_SCOPE = "camera_scope";
    public String mCameraScope;
    private static boolean mResetCamera = false;
    /* @} */
}
