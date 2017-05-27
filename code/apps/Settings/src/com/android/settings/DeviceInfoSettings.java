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

package com.android.settings;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SELinux;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.sprd.settings.RecoverySystemUpdatePreference;
import android.telephony.TelephonyManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//fota start
import android.content.pm.PackageInfo;
//fota end


/* Sprd:for bug 460925 @{ */
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.LocalSocketAddress.Namespace;
import java.io.OutputStream;
import java.io.InputStream;
import android.text.TextUtils;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.nio.charset.StandardCharsets;
import android.os.Handler;
import android.os.Message;
import android.content.Context;
/* @} */
import com.sprd.android.config.OptConfig;

/*SUN:jicong.wang for SUN_EDITABLE_CUSTOM_MODEL @{ */
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.widget.EditText;
import android.text.InputFilter;
/*SUN:jicong.wang for SUN_EDITABLE_CUSTOM_MODEL @} */

public class DeviceInfoSettings extends SettingsPreferenceFragment implements Indexable {

    private static final String LOG_TAG = "DeviceInfoSettings";
    private static final String FILENAME_PROC_VERSION = "/proc/version";
    private static final String FILENAME_MSV = "/sys/board_properties/soc/msv";

    private static final String KEY_REGULATORY_INFO = "regulatory_info";
    private static final String KEY_SYSTEM_UPDATE_SETTINGS = "system_update_settings";
    private static final String PROPERTY_URL_SAFETYLEGAL = "ro.url.safetylegal";
    private static final String PROPERTY_SELINUX_STATUS = "ro.build.selinux";
    private static final String KEY_KERNEL_VERSION = "kernel_version";
    private static final String KEY_BUILD_NUMBER = "build_number";
    private static final String KEY_DEVICE_MODEL = "device_model";
    private static final String KEY_SELINUX_STATUS = "selinux_status";
    private static final String KEY_BASEBAND_VERSION = "baseband_version";
    private static final String KEY_FIRMWARE_VERSION = "firmware_version";
    private static final String KEY_SECURITY_PATCH = "security_patch";
    private static final String KEY_UPDATE_SETTING = "additional_system_update_settings";
    private static final String KEY_EQUIPMENT_ID = "fcc_equipment_id";
    private static final String PROPERTY_EQUIPMENT_ID = "ro.ril.fccid";
    private static final String KEY_DEVICE_FEEDBACK = "device_feedback";
    private static final String KEY_SAFETY_LEGAL = "safetylegal";
    private static final String KEY_DEVICE_BRAND = "device_brand";//jxl add
    /* redstone add:sunvov hj 20170428 add for redstone fota support start @{ */
    private static final String KEY_REDSTONE_FOTA_UPDATE_SETTINGS = "redstone_updates";
    /* redstone add:sunvov hj 20170428 add for redstone fota support end @} */

    static final int TAPS_TO_BE_A_DEVELOPER = OptConfig.SUN_CUSTOM_C7356_XT_HVGA_SUPPORT ? 8 : 7;

    long[] mHits = new long[3];
    int mDevHitCountdown;
    Toast mDevHitToast;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DEVICEINFO;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_about;
    }

    /* Sprd:for bug 460925 @{ */
    private Runnable mBasedVerRunnable;
    private static final int MSG_UPDATE_BASED_VERSION_SUMMARY = 1;
    /* @} */
	/*SUN:jicong.wang for SUN_EDITABLE_CUSTOM_MODEL @{ */
    private static final int MAX_MODEL_NAME_LENGTH = 30;
    private EditText mSignText;
	/*SUN:jicong.wang for SUN_EDITABLE_CUSTOM_MODEL @} */

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.device_info_settings);
        // SPRD:for Bug379376 when BroadcastReceiver is already unregistered mustn't unregister it again
        initRecoverySystemUpdatePreference();
        setStringSummary(KEY_FIRMWARE_VERSION, Build.VERSION.RELEASE);
        /*SUN:jicong.wang add start @{*/
        if(OptConfig.SUN_SUBCUSTOM_C7367_HWD_FWVGA_R6_WINDS){
            setStringSummary(KEY_FIRMWARE_VERSION,"6.0 Marshmallow");            
        }    
        /*SUN:jicong.wang add end @}*/
        findPreference(KEY_FIRMWARE_VERSION).setEnabled(true);
        String patch = Build.VERSION.SECURITY_PATCH;
        if (!"".equals(patch)) {
            try {
                SimpleDateFormat template = new SimpleDateFormat("yyyy-MM-dd");
                Date patchDate = template.parse(patch);
                String format = DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMMyyyy");
                patch = DateFormat.format(format, patchDate).toString();
            } catch (ParseException e) {
                // broken parse; fall through and use the raw string
            }
            setStringSummary(KEY_SECURITY_PATCH, patch);
        } else {
            getPreferenceScreen().removePreference(findPreference(KEY_SECURITY_PATCH));

        }
        /* Sprd:for bug 460925 @{ */
        //setValueSummary(KEY_BASEBAND_VERSION, "gsm.version.baseband");
        /* @} */
        setStringSummary(KEY_DEVICE_MODEL, Build.MODEL + getMsvSuffix());
        setValueSummary(KEY_EQUIPMENT_ID, PROPERTY_EQUIPMENT_ID);
        setStringSummary(KEY_DEVICE_MODEL, Build.MODEL);
        
        /*SUN:jxl add devices brand info @{ */
        setStringSummary(KEY_DEVICE_BRAND, Build.BRAND);
        if(!OptConfig.SUNVOV_CUSTOM_C7301_YSF_W20_FWVGA){
            getPreferenceScreen().removePreference(findPreference(KEY_DEVICE_BRAND));
        }
        /*SUN:jxl add devices brand info @} */
		/*SUN:jxl add to remove legal @{ */
        if(OptConfig.SUN_CUSTOM_C7356_XT_HVGA_SUPPORT){
        getPreferenceScreen().removePreference(findPreference("container"));
      }
        /*SUN:jxl add to remove legal @} */
        /*SUN:jicong.wang for SUN_EDITABLE_CUSTOM_MODEL @{ */	
        if(OptConfig.SUN_EDITABLE_CUSTOM_MODEL){
            setStringSummary(KEY_DEVICE_MODEL, SystemProperties.get("persist.sys.custom.model",Build.MODEL));
            findPreference(KEY_DEVICE_MODEL).setEnabled(true);
		}
        /*SUN:jicong.wang for SUN_EDITABLE_CUSTOM_MODEL @} */	
        /// SUN:Kalyy 20160817 modify for 7359 start @{
        if(OptConfig.SUN_C7359_C5D_FWVGA_CHERRY){
            setStringSummary(KEY_DEVICE_MODEL, "H540");
        }
        /// SUN:Kalyy 20160817 modify for 7359 end @}
        if(OptConfig.SUN_S7358_HWD_HD_V19_TWZ){//Kalyy
            if(Build.DISPLAY.startsWith("TWZ_V19-1")){
                setStringSummary(KEY_DEVICE_MODEL, "Y63i");
            }
            else{
                setStringSummary(KEY_DEVICE_MODEL, "Y63");
            }
        }
        /*SUN:jicong.wang add start {@*/
        if(OptConfig.SUN_SUBCUSTOM_C7367_HWD_FWVGA_R6_WINDS){
            setStringSummary(KEY_DEVICE_MODEL, "Winds Mobile Note Max II");
        }
        /*SUN:jicong.wang add end @}*/

        //setStringSummary(KEY_BUILD_NUMBER, Build.DISPLAY);
        /* SPRD: change version for bug 441029 @{ */
        boolean operatorVersion = SystemProperties.get("ro.operator.version").equals("enabled");

        Log.d(LOG_TAG, " ro.operator.display.version = " + SystemProperties.get("ro.operator.display.version", "unknown"));
        if (operatorVersion) {
            Log.d(LOG_TAG, " enter display cucc version, it's work ");
            setStringSummary(KEY_BUILD_NUMBER, SystemProperties.get("ro.operator.display.version", "unknown"));
        } else {
        	  //qiuyaobo,20170419,begin
        	  /*
        	  if(OptConfig.SUN_SUBCUSTOM_C7367_HWD_QHD_R10_MGT)
        	  		setStringSummary(KEY_BUILD_NUMBER, "MGT-T3(plus)");
        	  else
        	  */
        	  //qiuyaobo,20170419,end
            setStringSummary(KEY_BUILD_NUMBER, Build.DISPLAY);
        }
        /* @} */

        findPreference(KEY_BUILD_NUMBER).setEnabled(true);
	
        /// SUN:jiazhenl 20170106 @{
        //findPreference(KEY_KERNEL_VERSION).setSummary(getFormattedKernelVersion());
        setStringSummary(KEY_KERNEL_VERSION, "3.10.65");
        /// SUN:jiazhenl 20170106 @}
        if(OptConfig.SUN_C7359_C5D_FWVGA_CHERRY){//Kalyy
            setStringSummary(KEY_KERNEL_VERSION, "3.10.65");
            setStringSummary(KEY_BASEBAND_VERSION, Build.DISPLAY);
        }
        /*SUN:Kalyy SUNVOV_CPU_INFO for show cpu start {@*/
        if(!OptConfig.SUNVOV_CPU_INFO){
            getPreferenceScreen().removePreference(findPreference("CPU_info"));
        }
        /*SUN:Kalyy SUNVOV_CPU_INFO for show cpu end @}*/
		/*SUN:jicong.wang SUN_MEMORY_INFO for show memory start {@*/
		if(OptConfig.SUN_MEMORY_INFO_8G){
		    getPreferenceScreen().removePreference(findPreference("device_memory_info_4g"));
		    getPreferenceScreen().removePreference(findPreference("device_memory_info_16g"));
		    getPreferenceScreen().removePreference(findPreference("device_memory_info_32g"));
		} else if(OptConfig.SUN_MEMORY_INFO_16G) {
		    getPreferenceScreen().removePreference(findPreference("device_memory_info_4g"));
		    getPreferenceScreen().removePreference(findPreference("device_memory_info_8g"));
		    getPreferenceScreen().removePreference(findPreference("device_memory_info_32g"));
		} else if(OptConfig.SUN_MEMORY_INFO_4G) {
		    getPreferenceScreen().removePreference(findPreference("device_memory_info_8g"));
		    getPreferenceScreen().removePreference(findPreference("device_memory_info_16g"));
		    getPreferenceScreen().removePreference(findPreference("device_memory_info_32g"));
		}else if(OptConfig.SUN_MEMORY_INFO_32G) {
		    getPreferenceScreen().removePreference(findPreference("device_memory_info_4g"));
		    getPreferenceScreen().removePreference(findPreference("device_memory_info_8g"));
		    getPreferenceScreen().removePreference(findPreference("device_memory_info_16g"));
		} else {
		    getPreferenceScreen().removePreference(findPreference("device_memory_info_4g"));
		    getPreferenceScreen().removePreference(findPreference("device_memory_info_8g"));
		    getPreferenceScreen().removePreference(findPreference("device_memory_info_16g"));
		    getPreferenceScreen().removePreference(findPreference("device_memory_info_32g"));
		}
		/*SUN:jicong.wang SUN_MEMORY_INFO for show memory end @}*/

		/*SUN:jicong SUN_RAM_INFO for show ram start {@*/
		if(!OptConfig.SUN_RAM_INFO_1G){
		    getPreferenceScreen().removePreference(findPreference("device_RAM_1g"));
		}
		if(!OptConfig.SUN_RAM_INFO_2G){
		    getPreferenceScreen().removePreference(findPreference("device_RAM_2g"));
		}
		if(!OptConfig.SUN_RAM_INFO_3G){
		    getPreferenceScreen().removePreference(findPreference("device_RAM_3g"));
		}
		//qiuyaobo, 20170223,begin
		if(!OptConfig.SUN_RAM_INFO_512M){
		    getPreferenceScreen().removePreference(findPreference("device_RAM_512m"));
		}		
		//qiuyaobo, 20170223,end
		/*SUN:jicong.wang SUN_RAM_INFO for show ram end @}*/
        if (!SELinux.isSELinuxEnabled()) {
            String status = getResources().getString(R.string.selinux_status_disabled);
            setStringSummary(KEY_SELINUX_STATUS, status);
        } else if (!SELinux.isSELinuxEnforced()) {
            String status = getResources().getString(R.string.selinux_status_permissive);
            setStringSummary(KEY_SELINUX_STATUS, status);
        }

        // Remove selinux information if property is not present
        removePreferenceIfPropertyMissing(getPreferenceScreen(), KEY_SELINUX_STATUS,
                PROPERTY_SELINUX_STATUS);

        // Remove Safety information preference if PROPERTY_URL_SAFETYLEGAL is not set
        removePreferenceIfPropertyMissing(getPreferenceScreen(), KEY_SAFETY_LEGAL,
                PROPERTY_URL_SAFETYLEGAL);

        // Remove Equipment id preference if FCC ID is not set by RIL
        removePreferenceIfPropertyMissing(getPreferenceScreen(), KEY_EQUIPMENT_ID,
                PROPERTY_EQUIPMENT_ID);

        // Remove Baseband version if wifi-only device
        if (Utils.isWifiOnly(getActivity())) {
            getPreferenceScreen().removePreference(findPreference(KEY_BASEBAND_VERSION));
        }

        // Dont show feedback option if there is no reporter.
        if (TextUtils.isEmpty(getFeedbackReporterPackage(getActivity()))) {
            getPreferenceScreen().removePreference(findPreference(KEY_DEVICE_FEEDBACK));
        }

        /*
         * Settings is a generic app and should not contain any device-specific
         * info.
         */
        final Activity act = getActivity();

        // These are contained by the root preference screen
        PreferenceGroup parentPreference = getPreferenceScreen();

        /* redstone add:sunvov hj 20170428 add for redstone fota support start @{ */
        Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference, KEY_REDSTONE_FOTA_UPDATE_SETTINGS ,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        /* redstone add:sunvov hj 20170428 add for redstone fota support end @} */

      /* wangxing remove 20160713 @{ */  
      //  if (UserHandle.myUserId() == UserHandle.USER_OWNER) {
      //      Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference,
      //              KEY_SYSTEM_UPDATE_SETTINGS,
      //              Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
      //  } else {
            // Remove for secondary users
            removePreference(KEY_SYSTEM_UPDATE_SETTINGS);
      //  }
      /** @} */
      
        // Read platform settings for additional system update setting
        removePreferenceIfBoolFalse(KEY_UPDATE_SETTING,
                R.bool.config_additional_system_update_setting_enable);

        // Remove regulatory information if none present.
        final Intent intent = new Intent(Settings.ACTION_SHOW_REGULATORY_INFO);
        if (getPackageManager().queryIntentActivities(intent, 0).isEmpty()) {
            Preference pref = findPreference(KEY_REGULATORY_INFO);
            if (pref != null) {
                getPreferenceScreen().removePreference(pref);
            }
        }

        /* SPRD: add fota support { */
        if(isApkExist(act, "com.adups.fota") == false){
            if(findPreference("fota_update_settings") != null){
              getPreferenceScreen().removePreference(findPreference("fota_update_settings"));
            }
        } else {
            Preference preference = findPreference("fota_update_settings");
            if (preference != null) {
                preference.setTitle(getAppName(act, "com.adups.fota"));
            }
        }
        /* } */

        /* Sprd:for bug 460925 @{ */
        mBasedVerRunnable = new Runnable() {
            public void run() {
                getBasedSummary("gsm.version.baseband");
            }
        };
        new Thread(mBasedVerRunnable).start();
        /* @} */
    }

    @Override
    public void onResume() {
        super.onResume();
        mDevHitCountdown = getActivity().getSharedPreferences(DevelopmentSettings.PREF_FILE,
                Context.MODE_PRIVATE).getBoolean(DevelopmentSettings.PREF_SHOW,
                        android.os.Build.TYPE.equals("eng")) ? -1 : TAPS_TO_BE_A_DEVELOPER;
        mDevHitToast = null;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals(KEY_FIRMWARE_VERSION)) {
            System.arraycopy(mHits, 1, mHits, 0, mHits.length-1);
            mHits[mHits.length-1] = SystemClock.uptimeMillis();
            if (mHits[0] >= (SystemClock.uptimeMillis()-500)) {
                UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
                if (um.hasUserRestriction(UserManager.DISALLOW_FUN)) {
                    Log.d(LOG_TAG, "Sorry, no fun for you!");
                    return false;
                }

                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("android",
                        com.android.internal.app.PlatLogoActivity.class.getName());
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Unable to start activity " + intent.toString());
                }
            }
        } else if (preference.getKey().equals(KEY_BUILD_NUMBER)) {
            // Don't enable developer options for secondary users.
            if (UserHandle.myUserId() != UserHandle.USER_OWNER) return true;

            // Don't enable developer options until device has been provisioned
            if (Settings.Global.getInt(getActivity().getContentResolver(),
                    Settings.Global.DEVICE_PROVISIONED, 0) == 0) {
                return true;
            }

            final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
            if (um.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES)) return true;

            if (mDevHitCountdown > 0) {
                mDevHitCountdown--;
                if (mDevHitCountdown == 0) {
                    getActivity().getSharedPreferences(DevelopmentSettings.PREF_FILE,
                            Context.MODE_PRIVATE).edit().putBoolean(
                                    DevelopmentSettings.PREF_SHOW, true).apply();
                    if (mDevHitToast != null) {
                        mDevHitToast.cancel();
                    }
                    mDevHitToast = Toast.makeText(getActivity(), R.string.show_dev_on,
                            Toast.LENGTH_LONG);
                    mDevHitToast.show();
                    // This is good time to index the Developer Options
                    Index.getInstance(
                            getActivity().getApplicationContext()).updateFromClassNameResource(
                                    DevelopmentSettings.class.getName(), true, true);

                } else if (mDevHitCountdown > 0
                        && mDevHitCountdown < (TAPS_TO_BE_A_DEVELOPER-2)) {
                    if (mDevHitToast != null) {
                        mDevHitToast.cancel();
                    }
                    mDevHitToast = Toast.makeText(getActivity(), getResources().getQuantityString(
                            R.plurals.show_dev_countdown, mDevHitCountdown, mDevHitCountdown),
                            Toast.LENGTH_SHORT);
                    mDevHitToast.show();
                }
            } else if (mDevHitCountdown < 0) {
                if (mDevHitToast != null) {
                    mDevHitToast.cancel();
                }
                mDevHitToast = Toast.makeText(getActivity(), R.string.show_dev_already,
                        Toast.LENGTH_LONG);
                mDevHitToast.show();
            }
        } else if (preference.getKey().equals(KEY_DEVICE_FEEDBACK)) {
            sendFeedback();
		/*SUN:jicong.wang for SUN_EDITABLE_CUSTOM_MODEL start @{ */
        }else if (preference.getKey().equals(KEY_DEVICE_MODEL)) {
			if(OptConfig.SUN_EDITABLE_CUSTOM_MODEL){
				showEditModelNameDialog();
			}
		/*SUN:jicong.wang for SUN_EDITABLE_CUSTOM_MODEL end @} */
        } else if(preference.getKey().equals(KEY_SYSTEM_UPDATE_SETTINGS)) {
            CarrierConfigManager configManager =
                    (CarrierConfigManager) getSystemService(Context.CARRIER_CONFIG_SERVICE);
            PersistableBundle b = configManager.getConfig();
            if (b.getBoolean(CarrierConfigManager.KEY_CI_ACTION_ON_SYS_UPDATE_BOOL)) {
                ciActionOnSysUpdate(b);
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    /**
     * Trigger client initiated action (send intent) on system update
     */
    private void ciActionOnSysUpdate(PersistableBundle b) {
        String intentStr = b.getString(CarrierConfigManager.
                KEY_CI_ACTION_ON_SYS_UPDATE_INTENT_STRING);
        if (!TextUtils.isEmpty(intentStr)) {
            String extra = b.getString(CarrierConfigManager.
                    KEY_CI_ACTION_ON_SYS_UPDATE_EXTRA_STRING);
            String extraVal = b.getString(CarrierConfigManager.
                    KEY_CI_ACTION_ON_SYS_UPDATE_EXTRA_VAL_STRING);

            Intent intent = new Intent(intentStr);
            if (!TextUtils.isEmpty(extra)) {
                intent.putExtra(extra, extraVal);
            }
            Log.d(LOG_TAG, "ciActionOnSysUpdate: broadcasting intent " + intentStr +
                    " with extra " + extra + ", " + extraVal);
            getActivity().getApplicationContext().sendBroadcast(intent);
        }
    }

    private void removePreferenceIfPropertyMissing(PreferenceGroup preferenceGroup,
            String preference, String property ) {
        if (SystemProperties.get(property).equals("")) {
            // Property is missing so remove preference from group
            try {
                preferenceGroup.removePreference(findPreference(preference));
            } catch (RuntimeException e) {
                Log.d(LOG_TAG, "Property '" + property + "' missing and no '"
                        + preference + "' preference");
            }
        }
    }

    private void removePreferenceIfBoolFalse(String preference, int resId) {
        if (!getResources().getBoolean(resId)) {
            Preference pref = findPreference(preference);
            if (pref != null) {
                getPreferenceScreen().removePreference(pref);
            }
        }
    }

    private void setStringSummary(String preference, String value) {
        try {
            findPreference(preference).setSummary(value);
        } catch (RuntimeException e) {
            findPreference(preference).setSummary(
                getResources().getString(R.string.device_info_default));
        }
    }

    private void setValueSummary(String preference, String property) {
        try {
            findPreference(preference).setSummary(
                    SystemProperties.get(property,
                            getResources().getString(R.string.device_info_default)));
        } catch (RuntimeException e) {
            // No recovery
        }
    }

    private void sendFeedback() {
        String reporterPackage = getFeedbackReporterPackage(getActivity());
        if (TextUtils.isEmpty(reporterPackage)) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_BUG_REPORT);
        intent.setPackage(reporterPackage);
        startActivityForResult(intent, 0);
    }

    /**
     * Reads a line from the specified file.
     * @param filename the file to read from
     * @return the first line, if any.
     * @throws IOException if the file couldn't be read
     */
    private static String readLine(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }

    public static String getFormattedKernelVersion() {
        try {
            return formatKernelVersion(readLine(FILENAME_PROC_VERSION));

        } catch (IOException e) {
            Log.e(LOG_TAG,
                "IO Exception when getting kernel version for Device Info screen",
                e);

            return "Unavailable";
        }
    }

    public static String formatKernelVersion(String rawKernelVersion) {
        // Example (see tests for more):
        // Linux version 3.0.31-g6fb96c9 (android-build@xxx.xxx.xxx.xxx.com) \
        //     (gcc version 4.6.x-xxx 20120106 (prerelease) (GCC) ) #1 SMP PREEMPT \
        //     Thu Jun 28 11:02:39 PDT 2012

        final String PROC_VERSION_REGEX =
            "Linux version (\\S+) " + /* group 1: "3.0.31-g6fb96c9" */
            "\\((\\S+?)\\) " +        /* group 2: "x@y.com" (kernel builder) */
            "(?:\\(gcc.+? \\)) " +    /* ignore: GCC version information */
            "(#\\d+) " +              /* group 3: "#1" */
            "(?:.*?)?" +              /* ignore: optional SMP, PREEMPT, and any CONFIG_FLAGS */
            "((Sun|Mon|Tue|Wed|Thu|Fri|Sat).+)"; /* group 4: "Thu Jun 28 11:02:39 PDT 2012" */

        Matcher m = Pattern.compile(PROC_VERSION_REGEX).matcher(rawKernelVersion);
        if (!m.matches()) {
            Log.e(LOG_TAG, "Regex did not match on /proc/version: " + rawKernelVersion);
            return "Unavailable";
        } else if (m.groupCount() < 4) {
            Log.e(LOG_TAG, "Regex match on /proc/version only returned " + m.groupCount()
                    + " groups");
            return "Unavailable";
        }
        return m.group(1) + "\n" +                 // 3.0.31-g6fb96c9
            m.group(2) + " " + m.group(3) + "\n" + // x@y.com #1
            m.group(4);                            // Thu Jun 28 11:02:39 PDT 2012
    }

    /**
     * Returns " (ENGINEERING)" if the msv file has a zero value, else returns "".
     * @return a string to append to the model number description.
     */
    private String getMsvSuffix() {
        // Production devices should have a non-zero value. If we can't read it, assume it's a
        // production device so that we don't accidentally show that it's an ENGINEERING device.
        try {
            String msv = readLine(FILENAME_MSV);
            // Parse as a hex number. If it evaluates to a zero, then it's an engineering build.
            if (Long.parseLong(msv, 16) == 0) {
                return " (ENGINEERING)";
            }
        } catch (IOException ioe) {
            // Fail quietly, as the file may not exist on some devices.
        } catch (NumberFormatException nfe) {
            // Fail quietly, returning empty string should be sufficient
        }
        return "";
    }

    private static String getFeedbackReporterPackage(Context context) {
        final String feedbackReporter =
                context.getResources().getString(R.string.oem_preferred_feedback_reporter);
        if (TextUtils.isEmpty(feedbackReporter)) {
            // Reporter not configured. Return.
            return feedbackReporter;
        }
        // Additional checks to ensure the reporter is on system image, and reporter is
        // configured to listen to the intent. Otherwise, dont show the "send feedback" option.
        final Intent intent = new Intent(Intent.ACTION_BUG_REPORT);

        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolvedPackages =
                pm.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);
        for (ResolveInfo info : resolvedPackages) {
            if (info.activityInfo != null) {
                if (!TextUtils.isEmpty(info.activityInfo.packageName)) {
                    try {
                        ApplicationInfo ai = pm.getApplicationInfo(info.activityInfo.packageName, 0);
                        if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                            // Package is on the system image
                            if (TextUtils.equals(
                                        info.activityInfo.packageName, feedbackReporter)) {
                                return feedbackReporter;
                            }
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                         // No need to do anything here.
                    }
                }
            }
        }
        return null;
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {

            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(
                    Context context, boolean enabled) {
                final SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.device_info_settings;
                return Arrays.asList(sir);
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> keys = new ArrayList<String>();
                if (isPropertyMissing(PROPERTY_SELINUX_STATUS)) {
                    keys.add(KEY_SELINUX_STATUS);
                }
                if (isPropertyMissing(PROPERTY_URL_SAFETYLEGAL)) {
                    keys.add(KEY_SAFETY_LEGAL);
                }
                if (isPropertyMissing(PROPERTY_EQUIPMENT_ID)) {
                    keys.add(KEY_EQUIPMENT_ID);
                }
                // Remove Baseband version if wifi-only device
                if (Utils.isWifiOnly(context)) {
                    keys.add((KEY_BASEBAND_VERSION));
                }
                // Dont show feedback option if there is no reporter.
                if (TextUtils.isEmpty(getFeedbackReporterPackage(context))) {
                    keys.add(KEY_DEVICE_FEEDBACK);
                }
                if (UserHandle.myUserId() != UserHandle.USER_OWNER) {
                    keys.add(KEY_SYSTEM_UPDATE_SETTINGS);
                }
                if (!context.getResources().getBoolean(
                        R.bool.config_additional_system_update_setting_enable)) {
                    keys.add(KEY_UPDATE_SETTING);
                }
                return keys;
            }

            private boolean isPropertyMissing(String property) {
                return SystemProperties.get(property).equals("");
            }
        };

    /*
     * SPRD: for Bug379376，when BroadcastReceiver is already unregistered
     * mustn't unregister it again @{
     */
    private BroadcastReceiver mBatteryLevelRcvr;
    private IntentFilter mBatteryLevelFilter;
    private int mLevelPower;
    private static String KEY_RECOVERY_SYSTEM_UPDATE = "RecoverySystemUpdate";

    private void monitorBatteryState() {
        mBatteryLevelRcvr = new BroadcastReceiver() {

            public void onReceive(Context context, Intent intent) {
                int rawlevel = intent.getIntExtra("level", -1);
                int scale = intent.getIntExtra("scale", -1);
                int level = -1; // percentage, or -1 for unknown
                if (rawlevel >= 0 && scale > 0) {
                    level = (rawlevel * 100) / scale;
                }
                mLevelPower = level;
            }

        };
        mBatteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        getActivity().registerReceiver(mBatteryLevelRcvr, mBatteryLevelFilter);
    }

    private void unregisterBatteryReceiver() {
        getActivity().unregisterReceiver(mBatteryLevelRcvr);
    }

    private void initRecoverySystemUpdatePreference() {
        monitorBatteryState();
        RecoverySystemUpdatePreference rsup = (RecoverySystemUpdatePreference) findPreference(KEY_RECOVERY_SYSTEM_UPDATE);
        /* SPRD: bug 534345 delete RecoverySystemUpdate func in Guest Mode */
        /* remove by Kalyy start
        if (UserHandle.myUserId() == UserHandle.USER_OWNER) {
            if (rsup != null) {
                rsup.setBatteryCallBack(new RecoverySystemUpdatePreference.BatteryCallBack() {
                    @Override
                    public int getBatteryLevel() {
                        return mLevelPower;
                    }
                });
            }
        } else remove by Kalyy end*/{
            if (rsup != null) {
                getPreferenceScreen().removePreference(rsup);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterBatteryReceiver();
    }
    /* @} */

    /* Sprd:for bug 460925 @{ */
    private void getBasedSummary(String property) {
        try {
            String pro = SystemProperties.get(property,
                    getResources().getString(R.string.device_info_default));
            String cp2 = "";
            String temp;
            String summary = pro;
            temp = getCp2Version();
            if (temp != null) {
                Log.d(LOG_TAG, " temp = " + temp);
                temp = temp.replaceAll("\\s+", "");
                if (temp.startsWith("Platform")) {
                    final String PROC_VERSION_REGEX =
                            "PlatformVersion:(\\S+)" + "ProjectVersion:(\\S+)" + "HWVersion:(\\S+)";
                    Matcher m = Pattern.compile(PROC_VERSION_REGEX).matcher(temp);
                    if (!m.matches()) {
                        Log.e(LOG_TAG, "Regex did not match on cp2 version: ");
                    } else {
                        String dateTime = m.group(3);
                        String modem = "modem";
                        int endIndex = dateTime.indexOf(modem) + modem.length();
                        String subString1 = dateTime.substring(0, endIndex);
                        String subString2 = dateTime.substring(endIndex);
                        String time = subString2.substring(10);
                        String date = subString2.substring(0, 10);
                        cp2 = m.group(1) + "|" + m.group(2) + "|" + subString1 + "|" + date + " "
                                + time;
                    }
                } else {
                    Log.e(LOG_TAG, "cp2 version is error");
                }
            }
            if (!TextUtils.isEmpty(cp2)) {
                summary = pro + "\n" + cp2;
            }
            
            if(OptConfig.SUNVOV_CUSTOM_C7301_XLL_FWVGA){//wangxing add
                summary = "TM_BASE_W16.22.2";
            }
            if(OptConfig.SUN_C7359_C5D_FWVGA_CHERRY){//Kalyy
                summary = Build.DISPLAY;
            }
            
            Log.d(LOG_TAG, "pro = " + pro + " cp2 = " + cp2);
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_UPDATE_BASED_VERSION_SUMMARY;
            msg.obj = summary;
            mHandler.sendMessage(msg);

        } catch (RuntimeException e) {
            // No recovery
        }
    }
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_BASED_VERSION_SUMMARY:
                    findPreference(KEY_BASEBAND_VERSION).setSummary((CharSequence) msg.obj);
                    break;
            }
        }
    };
    public static String getCp2Version() {
        LocalSocket socket = null;
        final String socketName = "wcnd";
        String result = null;
        byte[] buf = new byte[255];
        OutputStream outputStream = null;
        InputStream inputStream = null;

        try {
            socket = new LocalSocket();
            LocalSocketAddress address = new LocalSocketAddress(socketName,
                    LocalSocketAddress.Namespace.ABSTRACT);
            socket.connect(address);
            outputStream = socket.getOutputStream();
            if (outputStream != null) {
                String strcmd = "wcn at+spatgetcp2info";
                StringBuilder cmdBuilder = new StringBuilder(strcmd).append('\0');
                String cmd = cmdBuilder.toString(); /* Cmd + \0 */
                try {
                    outputStream.write(cmd.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Failed wrirting output stream: " + e);
                }
            }
            inputStream = socket.getInputStream();
            int count = inputStream.read(buf, 0, 255);
            result = new String(buf, "utf-8");
            Log.d(LOG_TAG,"count = "+count);
            if (result.startsWith("Fail")) {
                Log.d(LOG_TAG,"cp2 no data available");
                return null;
            }
        } catch (Exception e) {
            Log.i(LOG_TAG, " get socket info fail about:" + e.toString());
        } finally {
            try {
                buf = null;
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                socket.close();
            } catch (Exception e) {
                Log.i(LOG_TAG, "socket fail about:" + e.toString());
            }
        }
        return result;
    }
    /* @} */

    /* SPRD: add fota support { */
    private boolean isApkExist(Context ctx, String packageName){
        PackageManager pm = ctx.getPackageManager();
        PackageInfo packageInfo = null;
        String versionName = null;
        try {
            packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("FotaUpdate", "isApkExist not found");
            return false;
        }

        if (versionName != null) {
            String[] names = versionName.split("\\.");
            if (names.length >= 4 && ("9".equals(names[3]) || "9".equals(names[2]))) {
                return false;
            }
        }
        Log.i("FotaUpdate", "isApkExist = true");
        return true;
    }

    public String getAppName(Context ctx, String packageName) {
        PackageManager pm = ctx.getPackageManager();
        ApplicationInfo appInfo = null;
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            appInfo = pm.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            appInfo = null;
        }

        return (String) pm.getApplicationLabel(appInfo);
    }
    /* } */
	/*SUN:jicong.wang for SUN_EDITABLE_CUSTOM_MODEL start @{ */
	private void showEditModelNameDialog(){
		AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
		mSignText = new EditText(dialog.getContext());
		mSignText.computeScroll();
		mSignText.setSelectAllOnFocus(true);              
		mSignText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(MAX_MODEL_NAME_LENGTH)});
		mSignText.setText(SystemProperties.get("persist.sys.custom.model",Build.MODEL));
		mSignText.setTextColor(R.color.black);
    
		AlertDialog mSignTextDialog = dialog.setTitle(R.string.device_name_title)
			.setView(mSignText)
			.setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {
					if(null != mSignText.getText().toString() && mSignText.getText().toString() != ""){
						String mNewSign = mSignText.getText().toString();
						SystemProperties.set("persist.sys.custom.model",mNewSign);//ro.product.model
						setStringSummary(KEY_DEVICE_MODEL, mNewSign);
					}
				}
			})
			.setNegativeButton(R.string.dlg_cancel, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			}).show(); 
		}
	/*SUN:jicong.wang for SUN_EDITABLE_CUSTOM_MODEL end @} */
}

