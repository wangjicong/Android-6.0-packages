package com.android.dialer.settings;

import static android.Manifest.permission.READ_PHONE_STATE;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Process;
import android.os.UserManager;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.CarrierConfigManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import android.app.AlertDialog;
import android.content.DialogInterface;
import com.android.contacts.common.util.PermissionsUtil;
import com.android.dialer.R;
import com.sprd.dialer.settings.CallConnectionSettingsFragment;
import com.sprd.dialer.settings.CallRecordingSettingsFragment;
import com.sprd.dialer.settings.FlippingSilenceSettingsFragment;
import com.sprd.android.config.OptConfig;

import java.util.List;
import android.os.SystemProperties;//qiuyaobo,20160712
public class DialerSettingsActivity extends PreferenceActivity {

    protected SharedPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        Header displayOptionsHeader = new Header();
        displayOptionsHeader.titleRes = R.string.display_options_title;
        displayOptionsHeader.fragment = DisplayOptionsSettingsFragment.class.getName();
        target.add(displayOptionsHeader);

        /* SPRD: modify for bug501899 @{
        Header soundSettingsHeader = new Header();
        soundSettingsHeader.titleRes = R.string.sounds_and_vibration_title;
        soundSettingsHeader.fragment = SoundSettingsFragment.class.getName();
        soundSettingsHeader.id = R.id.settings_header_sounds_and_vibration;
        target.add(soundSettingsHeader);
        @} */
        /* SPRD: modify for bug507993 @{
        Header quickResponseSettingsHeader = new Header();
        Intent quickResponseSettingsIntent =
                new Intent(TelecomManager.ACTION_SHOW_RESPOND_VIA_SMS_SETTINGS);
        quickResponseSettingsHeader.titleRes = R.string.respond_via_sms_setting_title;
        quickResponseSettingsHeader.intent = quickResponseSettingsIntent;
        target.add(quickResponseSettingsHeader);
        @} */
        /* SPRD: bug#498473, check permissin for IP DIAL feature @{ */
        /* SPRD: bug#474283, add IP DIAL feature @{ */
        final boolean hasReadPhoneStatePermission = PermissionsUtil.hasPermission(this, READ_PHONE_STATE);
        if (hasReadPhoneStatePermission) {
            CarrierConfigManager configManager =(CarrierConfigManager)
                    getSystemService(Context.CARRIER_CONFIG_SERVICE);
            if (configManager != null && configManager.getConfigForDefaultPhone() != null) {
                boolean isIpEnabled = configManager.getConfigForDefaultPhone().getBoolean(
                        CarrierConfigManager.KEY_FEATURE_IP_DIAL_ENABLED_BOOL);
                if (isIpEnabled) {
                    Header ipDialSettingsHeader = new Header();
                    Intent ipDialSettingsIntent = new Intent("android.intent.action.MAIN");
                    ipDialSettingsIntent.setClassName("com.android.phone",
                        "com.sprd.phone.settings.ipdial.IpNumberListActivity");
                    ipDialSettingsHeader.titleRes = R.string.ip_dialing_list_title;
                    ipDialSettingsHeader.intent = ipDialSettingsIntent;
                    target.add(ipDialSettingsHeader);
                }
            }
        } else {
            new AlertDialog.Builder(this)
                .setMessage(R.string.dialog_phone_permission_deny)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                })
                .show();
        }
        /* @} */
        /* @} */

        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        // SPRD: add for bug 512372
        SubscriptionManager subscriptionManager = SubscriptionManager.from(this);

        // Only show call setting menus if the current user is the primary/owner user.
        if (isPrimaryUser()) {
            /* SPRD: add for bug 512372 @{ */
            List<SubscriptionInfo> availableSubInfoList = subscriptionManager
                    .getActiveSubscriptionInfoList();
            // subCount == 0 means sim card absent
            int subCount = (availableSubInfoList == null) ? 0 : availableSubInfoList.size();
            /* @} */

            // Show "Call Settings" if there is one SIM and "Phone Accounts" if there are more.
            if (telephonyManager.getPhoneCount() <= 1) {
                Header callSettingsHeader = new Header();
                Intent callSettingsIntent = new Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS);
                /* SPRD: add for bug 512372 @{ */
                if (subCount > 0) {
                    SubscriptionInfo subscription = availableSubInfoList.get(0);
                    callSettingsIntent.putExtra(
                            "com.android.phone.settings.SubscriptionInfoHelper.SubscriptionId",
                            subscription.getSubscriptionId());
                }
                /* @} */
                callSettingsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                callSettingsHeader.titleRes = R.string.call_settings_label;
                callSettingsHeader.intent = callSettingsIntent;
                target.add(callSettingsHeader);
            } else {
                Header phoneAccountSettingsHeader = new Header();
                Intent phoneAccountSettingsIntent =
                        new Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS);
                phoneAccountSettingsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                phoneAccountSettingsHeader.titleRes = R.string.phone_account_settings_label;
                phoneAccountSettingsHeader.intent = phoneAccountSettingsIntent;
                target.add(phoneAccountSettingsHeader);
            }
            /* SPRD: show quickResponseSetting for bug507993 @{ */
            Header quickResponseSettingsHeader = new Header();
            Intent quickResponseSettingsIntent =
                    new Intent(TelecomManager.ACTION_SHOW_RESPOND_VIA_SMS_SETTINGS);
            quickResponseSettingsHeader.titleRes = R.string.respond_via_sms_setting_title;
            quickResponseSettingsHeader.intent = quickResponseSettingsIntent;
            target.add(quickResponseSettingsHeader);
            /* @} */

            /* SPRD: Bug#474289, FAST DIAL FEATURE @{ */
            Header fastDialSettingsHeader = new Header();
            Intent fastDialSettingsIntent = new Intent("android.intent.action.FASTDIAL");
            fastDialSettingsIntent.setClassName("com.android.phone",
                "com.sprd.phone.settings.fastdial.FastDialSettingActivity");
            fastDialSettingsHeader.titleRes = R.string.fast_dial_title;
            fastDialSettingsHeader.intent = fastDialSettingsIntent;
            target.add(fastDialSettingsHeader);
            /* @} */

            if (telephonyManager.isTtyModeSupported()
                    || telephonyManager.isHearingAidCompatibilitySupported()) {
                Header accessibilitySettingsHeader = new Header();
                Intent accessibilitySettingsIntent =
                        new Intent(TelecomManager.ACTION_SHOW_CALL_ACCESSIBILITY_SETTINGS);
                accessibilitySettingsHeader.titleRes = R.string.accessibility_settings_title;
                accessibilitySettingsHeader.intent = accessibilitySettingsIntent;
                target.add(accessibilitySettingsHeader);
            }
        }
        /* SPRD: AUTOMATIC RECORD FEATURE. @{ */
        Header recordSettingsHeader = new Header();
        recordSettingsHeader.titleRes = R.string.call_recording_setting_title;
        recordSettingsHeader.fragment = CallRecordingSettingsFragment.class.getName();
        target.add(recordSettingsHeader);
        /* @} */

        /* SPRD: vibration feedback for call connection. See bug#505177 @{ */
        Header callConnectionSettingsHeader = new Header();
        callConnectionSettingsHeader.titleRes =
                R.string.vibration_feedback_for_call_connection_setting_title;
        callConnectionSettingsHeader.fragment = CallConnectionSettingsFragment.class.getName();
        target.add(callConnectionSettingsHeader);
        /* @} */

        /* SPRD: Flip to silence from incoming calls. See bug473877 @{ */
        //qiuyaobo,20160712,begin
        //if(!shouldHideFlipSilentIncomingCall()) {
        if(!shouldHideFlipSilentIncomingCall() && SystemProperties.getBoolean("ro.flipToSilence.enabled", false)) {
        //qiuyaobo,20160712,end	
            Header incomingCallsFlippingSilenceSettingsHeader = new Header();
            incomingCallsFlippingSilenceSettingsHeader.titleRes =
                    R.string.incomingcall_flipping_silence_title;
            incomingCallsFlippingSilenceSettingsHeader.fragment =
                    FlippingSilenceSettingsFragment.class.getName();
            target.add(incomingCallsFlippingSilenceSettingsHeader);
        }
        /* @} */
    }

    @Override
    public void onHeaderClick(Header header, int position) {
        if (header.id == R.id.settings_header_sounds_and_vibration) {
            // If we don't have the permission to write to system settings, go to system sound
            // settings instead. Otherwise, perform the super implementation (which launches our
            // own preference fragment.
            if (!Settings.System.canWrite(this)) {
                Toast.makeText(
                        this,
                        getResources().getString(R.string.toast_cannot_write_system_settings),
                        Toast.LENGTH_SHORT).show();
                startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS));
                return;
            }
        }
        super.onHeaderClick(header, position);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    /**
     * @return Whether the current user is the primary user.
     */
    private boolean isPrimaryUser() {
        final UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
        return userManager.isSystemUser();
    }

    /* SPRD: Flip to silence from incoming calls. See bug473877 @{ */
    private boolean shouldHideFlipSilentIncomingCall() {
        CarrierConfigManager configManager = (CarrierConfigManager) getSystemService(
                Context.CARRIER_CONFIG_SERVICE);
        return !configManager.getConfig().getBoolean(
                CarrierConfigManager.KEY_FEATURE_FLIP_SILENT_INCOMING_CALL_ENABLED_BOOL);
    }
    /* @} */
}
