package com.sprd.phone;

import com.android.ims.ImsManager;
import com.android.internal.telephony.OperatorInfo;

import android.app.AddonManager;
import android.content.Context;
import android.preference.ListPreference;
import android.util.Log;
import com.android.phone.R;

public class TeleServicePluginsHelper {

    public TeleServicePluginsHelper() {
    }

    static TeleServicePluginsHelper mInstance;
    public static final String PREFERRED_NETWORK_MODE_4G_3G_2G = "0";
    public static final String PREFERRED_NETWORK_MODE_3G_2G = "1";
    public static final String PREFERRED_NETWORK_MODE_4G_ONLY="2";
    public static final String PREFERRED_NETWORK_MODE_3G_ONLY="3";
    public static final String PREFERRED_NETWORK_MODE_2G_ONLY="4";
    public ListPreference mButtonLtePreferredNetworkMode;
    public boolean mIsSupport3GOnly2GOnly = false;
    private static boolean mIsVolteEnable;
    private static String LOG_TAG = "TeleServicePluginsHelper";

    public static TeleServicePluginsHelper getInstance(Context context) {
        if (mInstance != null)
            return mInstance;
            mInstance = (TeleServicePluginsHelper) new AddonManager(context).getAddon(R.string.feature_addon_for_teleService, TeleServicePluginsHelper.class);
            return mInstance;
    }

    public boolean getDisplayNetworkList(OperatorInfo ni,int phoneId) {
        return true;
    }

    public boolean isSupportPlmn(){
        return true;
    }

    /* SPRD: Add a dialog in NetworkSetting for bug 531844 in reliance case. @{ */
    public boolean showDataOffWarning() {
        return false;
    }
    /* @} */

    public boolean needSetByPrimaryCardUsim(int primaryCard) {
        return true;
    }

    public void set3GOnly2GOnly(boolean isSupport) {
        mIsSupport3GOnly2GOnly = isSupport;
    }

    public ListPreference setLtePreferenceValues(ListPreference buttonLtePreferredNetworkMode) {
        if (mIsSupport3GOnly2GOnly) {
            buttonLtePreferredNetworkMode.setEntries(R.array.lte_network_mode_choices);
            buttonLtePreferredNetworkMode.setEntryValues(R.array.lte_network_mode_choices_values);
        } else if (mIsVolteEnable) {
            buttonLtePreferredNetworkMode.setEntries(R.array.lte_network_mode_choices_contains_lte_only);
            buttonLtePreferredNetworkMode.setEntryValues(R.array.lte_network_mode_choices_values_contains_lte_only);
        } else {
            buttonLtePreferredNetworkMode.setEntries(R.array.lte_preferred_networks_choices_for_common);
            buttonLtePreferredNetworkMode.setEntryValues(R.array.lte_network_mode_choices_values_for_common);
        }
        mButtonLtePreferredNetworkMode = buttonLtePreferredNetworkMode;
        return buttonLtePreferredNetworkMode;
    }

    public ListPreference updateLtePreferenceSummary(String preferredNetworkMode) {
        if (mButtonLtePreferredNetworkMode != null) {
            if (mIsSupport3GOnly2GOnly) {
                if (String.valueOf(PREFERRED_NETWORK_MODE_4G_3G_2G).equals(preferredNetworkMode)) {
                    mButtonLtePreferredNetworkMode.setValueIndex(3);
                    mButtonLtePreferredNetworkMode.setSummary(mButtonLtePreferredNetworkMode.getEntry());
                } else if (String.valueOf(PREFERRED_NETWORK_MODE_3G_2G).equals(preferredNetworkMode)) {
                    mButtonLtePreferredNetworkMode.setValueIndex(2);
                    mButtonLtePreferredNetworkMode.setSummary(mButtonLtePreferredNetworkMode.getEntry());
                } else if (String.valueOf(PREFERRED_NETWORK_MODE_3G_ONLY).equals(preferredNetworkMode)) {
                    mButtonLtePreferredNetworkMode.setValueIndex(1);
                    mButtonLtePreferredNetworkMode.setSummary(mButtonLtePreferredNetworkMode.getEntry());
                } else if (String.valueOf(PREFERRED_NETWORK_MODE_2G_ONLY).equals(preferredNetworkMode)) {
                    mButtonLtePreferredNetworkMode.setValueIndex(0);
                    mButtonLtePreferredNetworkMode.setSummary(mButtonLtePreferredNetworkMode.getEntry());
                }
            } else if (mIsVolteEnable) {
                if (String.valueOf(PREFERRED_NETWORK_MODE_4G_3G_2G).equals(preferredNetworkMode)) {
                    mButtonLtePreferredNetworkMode.setValueIndex(4);
                    mButtonLtePreferredNetworkMode.setSummary(mButtonLtePreferredNetworkMode.getEntry());
                } else if (String.valueOf(PREFERRED_NETWORK_MODE_4G_ONLY).equals(preferredNetworkMode)) {
                    mButtonLtePreferredNetworkMode.setValueIndex(3);
                    mButtonLtePreferredNetworkMode.setSummary(mButtonLtePreferredNetworkMode.getEntry());
                } else if (String.valueOf(PREFERRED_NETWORK_MODE_3G_2G).equals(preferredNetworkMode)) {
                    mButtonLtePreferredNetworkMode.setValueIndex(2);
                    mButtonLtePreferredNetworkMode.setSummary(mButtonLtePreferredNetworkMode.getEntry());
                } else if (String.valueOf(PREFERRED_NETWORK_MODE_3G_ONLY).equals(preferredNetworkMode)) {
                    mButtonLtePreferredNetworkMode.setValueIndex(1);
                    mButtonLtePreferredNetworkMode.setSummary(mButtonLtePreferredNetworkMode.getEntry());
                } else if (String.valueOf(PREFERRED_NETWORK_MODE_2G_ONLY).equals(preferredNetworkMode)) {
                    mButtonLtePreferredNetworkMode.setValueIndex(0);
                    mButtonLtePreferredNetworkMode.setSummary(mButtonLtePreferredNetworkMode.getEntry());
                }
            } else {
                if (String.valueOf(PREFERRED_NETWORK_MODE_4G_3G_2G).equals(preferredNetworkMode)) {
                    mButtonLtePreferredNetworkMode.setValueIndex(0);
                    mButtonLtePreferredNetworkMode.setSummary(mButtonLtePreferredNetworkMode.getEntry());
                } else if (String.valueOf(PREFERRED_NETWORK_MODE_3G_2G).equals(preferredNetworkMode)) {
                    mButtonLtePreferredNetworkMode.setValueIndex(1);
                    mButtonLtePreferredNetworkMode.setSummary(mButtonLtePreferredNetworkMode.getEntry());
                } else if (String.valueOf(PREFERRED_NETWORK_MODE_2G_ONLY).equals(preferredNetworkMode)) {
                    mButtonLtePreferredNetworkMode.setValueIndex(2);
                    mButtonLtePreferredNetworkMode.setSummary(mButtonLtePreferredNetworkMode.getEntry());
                } else {
                    /* SPRD: modify for bug511452 @{ */
                    mButtonLtePreferredNetworkMode.setSummary(R.string.invalid_network_mode);
                    /* @} */
                }
            }
        }
        return mButtonLtePreferredNetworkMode;
    }

    /**
     * CMCC new case : Not allow user to set network type to 3g2g ;
     * When insert SIM card,remove MobileNetworkSetting preference
     * see bug522182
     */
    public boolean isMainSlotInsertSIMCard(int phoneId) {
        return false;
    }

    /**
     * CMCC new case : allow user to set call time forward in 3g2g ;
     * see bug552776
     */
    public boolean isCallTimeForwardSupport() {
        Log.d(LOG_TAG , "isCallTimeForwardSupport");
        return false;
    }

    public void setVolteEnable(boolean volteEnable) {
        mIsVolteEnable = volteEnable;
    }

    /*
     * SPRD: CMCC FR: Disable the call out option when UE campe on IMS service.
     */
    public boolean callOutOptionEnable() {
        Log.d(LOG_TAG, "callOutOptionEnable true.");
        return true;
    }
}
