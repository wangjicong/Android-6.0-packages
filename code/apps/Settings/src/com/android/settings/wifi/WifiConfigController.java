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

package com.android.settings.wifi;

import android.content.Context;
import android.content.res.Resources;
import android.net.IpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.LinkAddress;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.StaticIpConfiguration;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiConfiguration.Protocol;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiEnterpriseConfig.Eap;
import android.net.wifi.WifiEnterpriseConfig.Phase2;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.security.Credentials;
import android.security.KeyStore;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.settings.ProxySelector;
import com.android.settings.R;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settings.Utils;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.util.Iterator;

/**
 * The class for allowing UIs like {@link WifiDialog} and {@link WifiConfigUiBase} to
 * share the logic for controlling buttons, text fields, etc.
 */
public class WifiConfigController implements TextWatcher,
       AdapterView.OnItemSelectedListener, OnCheckedChangeListener {
    private static final String TAG = "WifiConfigController";

    private final WifiConfigUiBase mConfigUi;
    private final View mView;
    private final AccessPoint mAccessPoint;

    /* This value comes from "wifi_ip_settings" resource array */
    private static final int DHCP = 0;
    private static final int STATIC_IP = 1;

    /* These values come from "wifi_proxy_settings" resource array */
    public static final int PROXY_NONE = 0;
    public static final int PROXY_STATIC = 1;
    public static final int PROXY_PAC = 2;

    /* These values come from "wifi_eap_method" resource array */
    public static final int WIFI_EAP_METHOD_PEAP = 0;
    public static final int WIFI_EAP_METHOD_TLS  = 1;
    public static final int WIFI_EAP_METHOD_TTLS = 2;
    public static final int WIFI_EAP_METHOD_PWD  = 3;
    public static final int WIFI_EAP_METHOD_SIM  = 4;
    public static final int WIFI_EAP_METHOD_AKA  = 5;
    public static final int WIFI_EAP_METHOD_AKA_PRIME  = 6;

    /* These values come from "wifi_peap_phase2_entries" resource array */
    public static final int WIFI_PEAP_PHASE2_NONE 	    = 0;
    public static final int WIFI_PEAP_PHASE2_MSCHAPV2 	= 1;
    public static final int WIFI_PEAP_PHASE2_GTC        = 2;

    /* Phase2 methods supported by PEAP are limited */
    private final ArrayAdapter<String> PHASE2_PEAP_ADAPTER;
    /* Full list of phase2 methods */
    private final ArrayAdapter<String> PHASE2_FULL_ADAPTER;

    private final Handler mTextViewChangedHandler;

    // e.g. AccessPoint.SECURITY_NONE
    private int mAccessPointSecurity;
    private TextView mPasswordView;

    private String unspecifiedCert = "unspecified";
    private static final int unspecifiedCertIndex = 0;

    private Spinner mSecuritySpinner;
    private Spinner mEapMethodSpinner;
    private Spinner mEapCaCertSpinner;
    private Spinner mPhase2Spinner;
    // Associated with mPhase2Spinner, one of PHASE2_FULL_ADAPTER or PHASE2_PEAP_ADAPTER
    private ArrayAdapter<String> mPhase2Adapter;
    private Spinner mEapUserCertSpinner;
    private TextView mEapIdentityView;
    private TextView mEapAnonymousView;

    private Spinner mIpSettingsSpinner;
    private TextView mIpAddressView;
    private TextView mGatewayView;
    private TextView mNetworkPrefixLengthView;
    private TextView mDns1View;
    private TextView mDns2View;

    private Spinner mProxySettingsSpinner;
    private TextView mProxyHostView;
    private TextView mProxyPortView;
    private TextView mProxyExclusionListView;
    private TextView mProxyPacView;

    private IpAssignment mIpAssignment = IpAssignment.UNASSIGNED;
    private ProxySettings mProxySettings = ProxySettings.UNASSIGNED;
    private ProxyInfo mHttpProxy = null;
    private StaticIpConfiguration mStaticIpConfiguration = null;

    private String[] mLevels;
    private boolean mEdit;
    private boolean mModify;
    private TextView mSsidView;

    private Context mContext;

    private Spinner mWepKeyIndex;
    private static int mWepPosition = 0;
    /* SPRD: add this item for EAP-SIM/AKA @{ */
    private Spinner mEapSimSlotSpinner;
    private int mSim_slot = -1;
    /* @} */
    // Broadcom, WAPI
    private static final String KEYSTORE_SPACE = WifiConfiguration.KEYSTORE_URI;
    private static final int[] WAPI_PSK_TYPE_VALUES = {
            WifiConfiguration.WAPI_ASCII_PASSWORD,
            WifiConfiguration.WAPI_HEX_PASSWORD
    };
    private Spinner mWapiPskType;
    private Spinner mWapiAsCert;
    private Spinner mWapiUserCert;
    // Broadcom, WAPI

    // SPRD: wifi plugin
    private boolean supportCMCC = false;
    private SprdWifiSettingsAddonStub mAddonStub;
    private final ArrayAdapter<String> WIFI_EAP_METHOD_ADAPTER;
    private final ArrayAdapter<String> PEAP_AND_SIM_METHOD_ADAPTER;
    private static final String CMCC_SSID = "CMCC";

    public WifiConfigController(
            WifiConfigUiBase parent, View view, AccessPoint accessPoint, boolean edit,
            boolean modify) {
        mConfigUi = parent;

        mView = view;
        mAccessPoint = accessPoint;
        mAccessPointSecurity = (accessPoint == null) ? AccessPoint.SECURITY_NONE :
                accessPoint.getSecurity();
        mEdit = edit;
        mModify = modify;

        mTextViewChangedHandler = new Handler();
        mContext = mConfigUi.getContext();
        final Resources res = mContext.getResources();

        // SPRD: wifi plugin
        mAddonStub = SprdWifiSettingsAddonStub.getInstance(mContext);
        supportCMCC = mAddonStub.isSupportCmcc();

        WIFI_EAP_METHOD_ADAPTER = new ArrayAdapter<String>(
                mContext, android.R.layout.simple_spinner_item,
                res.getStringArray(R.array.wifi_eap_method));
        WIFI_EAP_METHOD_ADAPTER.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        PEAP_AND_SIM_METHOD_ADAPTER = new ArrayAdapter<String>(
                mContext, android.R.layout.simple_spinner_item,
                res.getStringArray(R.array.peap_and_sim_method));
        PEAP_AND_SIM_METHOD_ADAPTER.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mLevels = res.getStringArray(R.array.wifi_signal);
        PHASE2_PEAP_ADAPTER = new ArrayAdapter<String>(
            mContext, android.R.layout.simple_spinner_item,
            res.getStringArray(R.array.wifi_peap_phase2_entries));
        PHASE2_PEAP_ADAPTER.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        PHASE2_FULL_ADAPTER = new ArrayAdapter<String>(
                mContext, android.R.layout.simple_spinner_item,
                res.getStringArray(R.array.wifi_phase2_entries));
        PHASE2_FULL_ADAPTER.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        unspecifiedCert = mContext.getString(R.string.wifi_unspecified);
        mIpSettingsSpinner = (Spinner) mView.findViewById(R.id.ip_settings);
        mIpSettingsSpinner.setOnItemSelectedListener(this);
        mProxySettingsSpinner = (Spinner) mView.findViewById(R.id.proxy_settings);
        mProxySettingsSpinner.setOnItemSelectedListener(this);

        if (mAccessPoint == null) { // new network
            mConfigUi.setTitle(R.string.wifi_add_network);

            mSsidView = (TextView) mView.findViewById(R.id.ssid);
            mSsidView.addTextChangedListener(this);
            mSecuritySpinner = ((Spinner) mView.findViewById(R.id.security));
            mSecuritySpinner.setOnItemSelectedListener(this);
            mView.findViewById(R.id.type).setVisibility(View.VISIBLE);

            showIpConfigFields();
            showProxyFields();
            mView.findViewById(R.id.wifi_advanced_toggle).setVisibility(View.VISIBLE);
            ((CheckBox)mView.findViewById(R.id.wifi_advanced_togglebox))
                    .setOnCheckedChangeListener(this);


            mConfigUi.setSubmitButton(res.getString(R.string.wifi_save));
        } else {
            mConfigUi.setTitle(mAccessPoint.getSsid());

            ViewGroup group = (ViewGroup) mView.findViewById(R.id.info);

            boolean showAdvancedFields = false;
            if (mAccessPoint.isSaved()) {
                WifiConfiguration config = mAccessPoint.getConfig();
                if (config.getIpAssignment() == IpAssignment.STATIC) {
                    mIpSettingsSpinner.setSelection(STATIC_IP);
                    showAdvancedFields = true;
                    // Display IP address.
                    StaticIpConfiguration staticConfig = config.getStaticIpConfiguration();
                    if (staticConfig != null && staticConfig.ipAddress != null) {
                        addRow(group, R.string.wifi_ip_address,
                           staticConfig.ipAddress.getAddress().getHostAddress());
                    }
                } else {
                    mIpSettingsSpinner.setSelection(DHCP);
                }


                if (config.getProxySettings() == ProxySettings.STATIC) {
                    mProxySettingsSpinner.setSelection(PROXY_STATIC);
                    showAdvancedFields = true;
                } else if (config.getProxySettings() == ProxySettings.PAC) {
                    mProxySettingsSpinner.setSelection(PROXY_PAC);
                    showAdvancedFields = true;
                } else {
                    mProxySettingsSpinner.setSelection(PROXY_NONE);
                }
                if (config != null && config.isPasspoint()) {
                    addRow(group, R.string.passpoint_label, String.format(
                        mContext.getString(R.string.passpoint_content), config.providerFriendlyName));
                }
            }

            if ((!mAccessPoint.isSaved() && !mAccessPoint.isActive())
                    || mEdit) {
                showSecurityFields();
                showIpConfigFields();
                showProxyFields();
                mView.findViewById(R.id.wifi_advanced_toggle).setVisibility(View.VISIBLE);
                ((CheckBox)mView.findViewById(R.id.wifi_advanced_togglebox))
                    .setOnCheckedChangeListener(this);
                if (showAdvancedFields) {
                    ((CheckBox)mView.findViewById(R.id.wifi_advanced_togglebox)).setChecked(true);
                    mView.findViewById(R.id.wifi_advanced_fields).setVisibility(View.VISIBLE);
                }
            }

            if (mModify) {
                mConfigUi.setSubmitButton(res.getString(R.string.wifi_save));
            } else {
                final DetailedState state = mAccessPoint.getDetailedState();
                final String signalLevel = getSignalString();

                if ((state == null || state == DetailedState.DISCONNECTED) && signalLevel != null) {
                    mConfigUi.setSubmitButton(res.getString(R.string.wifi_connect));
                } else {
                    if (state != null) {
                        boolean isEphemeral = mAccessPoint.isEphemeral();
                        WifiConfiguration config = mAccessPoint.getConfig();
                        String providerFriendlyName = null;
                        if (config != null && config.isPasspoint()) {
                            providerFriendlyName = config.providerFriendlyName;
                        }
                        String summary = AccessPoint.getSummary(
                                mConfigUi.getContext(), state, isEphemeral, providerFriendlyName);
                        addRow(group, R.string.wifi_status, summary);
                    }

                    if (signalLevel != null) {
                        addRow(group, R.string.wifi_signal, signalLevel);
                    }

                    WifiInfo info = mAccessPoint.getInfo();
                    if (info != null && info.getLinkSpeed() != -1) {
                        addRow(group, R.string.wifi_speed, String.format(
                                res.getString(R.string.link_speed), info.getLinkSpeed()));
                    }

                    if (info != null && info.getFrequency() != -1) {
                        final int frequency = info.getFrequency();
                        String band = null;

                        if (frequency >= AccessPoint.LOWER_FREQ_24GHZ
                                && frequency < AccessPoint.HIGHER_FREQ_24GHZ) {
                            band = res.getString(R.string.wifi_band_24ghz);
                        } else if (frequency >= AccessPoint.LOWER_FREQ_5GHZ
                                && frequency < AccessPoint.HIGHER_FREQ_5GHZ) {
                            band = res.getString(R.string.wifi_band_5ghz);
                        } else {
                            Log.e(TAG, "Unexpected frequency " + frequency);
                        }
                        if (band != null) {
                            addRow(group, R.string.wifi_frequency, band);
                        }
                    }

                    addRow(group, R.string.wifi_security, mAccessPoint.getSecurityString(false));
                    mView.findViewById(R.id.ip_fields).setVisibility(View.GONE);
                }
                if (mAccessPoint.isSaved() || mAccessPoint.isActive()) {
                    /* SPRD: wifi plugin @{ */
                    if(supportCMCC && state == DetailedState.CONNECTED){
                        mConfigUi.setDisconnectButton(mContext.getString(R.string.wifi_disconnect));
                    }
                     /* @} */
                    mConfigUi.setForgetButton(res.getString(R.string.wifi_forget));
                }
            }
        }

        mConfigUi.setCancelButton(res.getString(R.string.wifi_cancel));
        if (mConfigUi.getSubmitButton() != null) {
            enableSubmitIfAppropriate();
        }
    }

    private void addRow(ViewGroup group, int name, String value) {
        View row = mConfigUi.getLayoutInflater().inflate(R.layout.wifi_dialog_row, group, false);
        ((TextView) row.findViewById(R.id.name)).setText(name);
        ((TextView) row.findViewById(R.id.value)).setText(value);
        group.addView(row);
    }

    private String getSignalString(){
        final int level = mAccessPoint.getLevel();

        return (level > -1 && level < mLevels.length) ? mLevels[level] : null;
    }

    void hideForgetButton() {
        Button forget = mConfigUi.getForgetButton();
        if (forget == null) return;

        forget.setVisibility(View.GONE);
    }

    void hideSubmitButton() {
        Button submit = mConfigUi.getSubmitButton();
        if (submit == null) return;

        submit.setVisibility(View.GONE);
    }

    /* show submit button if password, ip and proxy settings are valid */
    void enableSubmitIfAppropriate() {
        Button submit = mConfigUi.getSubmitButton();
        if (submit == null) return;

        boolean enabled = false;
        boolean passwordInvalid = false;

        if (mPasswordView != null &&
            ((mAccessPointSecurity == AccessPoint.SECURITY_WEP && isWepPasswordInvalid()) ||
            ((mAccessPointSecurity == AccessPoint.SECURITY_PSK || mAccessPointSecurity == AccessPoint.SECURITY_FT_PSK) &&
            mPasswordView.length() < 8) ||
            (mAccessPointSecurity == AccessPoint.SECURITY_WAPI_PSK && mWapiPskType.getSelectedItemPosition() == 0
            && mPasswordView.length() < 8) || (mAccessPointSecurity == AccessPoint.SECURITY_WAPI_PSK &&
            mWapiPskType.getSelectedItemPosition() == 1 && mPasswordView.length() < 16))) {
            passwordInvalid = true;
        }

        if (mAccessPointSecurity == AccessPoint.SECURITY_WAPI_CERT
                && ((mWapiAsCert != null && mWapiAsCert.getSelectedItemPosition() == 0)
                        || (mWapiUserCert != null && mWapiUserCert.getSelectedItemPosition() == 0))) {
            passwordInvalid = true;
        }

        if ((mSsidView != null && mSsidView.length() == 0) ||
            ((mAccessPoint == null || !mAccessPoint.isSaved()) &&
            passwordInvalid)) {
            enabled = false;
        } else {
            if (ipAndProxyFieldsAreValid()) {
                enabled = true;
            } else {
                enabled = false;
            }
        }
        submit.setEnabled(enabled);
    }

    /* package */ WifiConfiguration getConfig() {
        if (!mEdit) {
            return null;
        }

        WifiConfiguration config = new WifiConfiguration();

        if (mAccessPoint == null) {
            config.SSID = AccessPoint.convertToQuotedString(
                    mSsidView.getText().toString());
            // If the user adds a network manually, assume that it is hidden.
            config.hiddenSSID = true;
        } else if (!mAccessPoint.isSaved()) {
            config.SSID = AccessPoint.convertToQuotedString(
                    mAccessPoint.getSsidStr());
        } else {
            config.networkId = mAccessPoint.getConfig().networkId;
        }

        switch (mAccessPointSecurity) {
            case AccessPoint.SECURITY_NONE:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                break;

            case AccessPoint.SECURITY_WEP:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
                if (mPasswordView.length() != 0) {
                    int length = mPasswordView.length();
                    String password = mPasswordView.getText().toString();
                    // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                    if ((length == 10 || length == 26 || length == 58) &&
                            password.matches("[0-9A-Fa-f]*")) {
                        // config.wepKeys[0] = password;
                        config.wepKeys[mWepPosition] = password;
                    } else {
                        //config.wepKeys[0] = '"' + password + '"';
                        config.wepKeys[mWepPosition] = '"' + password + '"';
                    }
                    config.wepTxKeyIndex = mWepPosition;
                }
                break;

            case AccessPoint.SECURITY_PSK:
            case AccessPoint.SECURITY_FT_PSK:
                if (mAccessPointSecurity == AccessPoint.SECURITY_FT_PSK) {
                    config.allowedKeyManagement.set(KeyMgmt.FT_PSK);
                } else {
                    config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
                }
                if (mPasswordView.length() != 0) {
                    String password = mPasswordView.getText().toString();
                    if (password.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = password;
                    } else {
                        config.preSharedKey = '"' + password + '"';
                    }
                }
                break;

            case AccessPoint.SECURITY_EAP:
            case AccessPoint.SECURITY_FT_EAP:
                if (mAccessPointSecurity == AccessPoint.SECURITY_FT_EAP) {
                    config.allowedKeyManagement.set(KeyMgmt.FT_EAP);
                } else {
                    config.allowedKeyManagement.set(KeyMgmt.WPA_EAP);
                    config.allowedKeyManagement.set(KeyMgmt.IEEE8021X);
                }
                config.enterpriseConfig = new WifiEnterpriseConfig();
                int eapMethod = mEapMethodSpinner.getSelectedItemPosition();
                if (isSsidCMCC() && eapMethod > 0) {
                    eapMethod = Eap.SIM;
                }
                int phase2Method = mPhase2Spinner.getSelectedItemPosition();
                config.enterpriseConfig.setEapMethod(eapMethod);
                switch (eapMethod) {
                    case Eap.PEAP:
                        // PEAP supports limited phase2 values
                        // Map the index from the PHASE2_PEAP_ADAPTER to the one used
                        // by the API which has the full list of PEAP methods.
                        switch(phase2Method) {
                            case WIFI_PEAP_PHASE2_NONE:
                                config.enterpriseConfig.setPhase2Method(Phase2.NONE);
                                break;
                            case WIFI_PEAP_PHASE2_MSCHAPV2:
                                config.enterpriseConfig.setPhase2Method(Phase2.MSCHAPV2);
                                break;
                            case WIFI_PEAP_PHASE2_GTC:
                                config.enterpriseConfig.setPhase2Method(Phase2.GTC);
                                break;
                            default:
                                Log.e(TAG, "Unknown phase2 method" + phase2Method);
                                break;
                        }
                        break;
                    /* SPRD: add for EAP-SIM and EAP_AKA @{ */
                    case Eap.SIM:
                    case Eap.AKA:
                    case Eap.AKA_PRIME:
                        /* SPRD: add for EAP-SIM and EAP_AKA @{ */
                        int length = ((String)mEapSimSlotSpinner.getSelectedItem()).length();
                        mSim_slot = ((mEapSimSlotSpinner.getSelectedItemPosition() == 0) ? 0 :
                            Integer.parseInt(((String)mEapSimSlotSpinner.getSelectedItem()).substring(length -1))) - 1;
                        Log.d(TAG, "mEapSimSlotSpinner.getSelectedItemPosition() " + mEapSimSlotSpinner.getSelectedItemPosition());
                        Log.d(TAG, "mEapSimSlotSpinner.getSelectedItem() " + mEapSimSlotSpinner.getSelectedItem());
                        if (mSim_slot != -1) {
                            config.eap_sim_slot = mSim_slot;
                        }
                        break;
                    /* @} */
                    default:
                        // The default index from PHASE2_FULL_ADAPTER maps to the API
                        config.enterpriseConfig.setPhase2Method(phase2Method);
                        break;
                }
                String caCert = (String) mEapCaCertSpinner.getSelectedItem();
                if (caCert.equals(unspecifiedCert)) caCert = "";
                config.enterpriseConfig.setCaCertificateAlias(caCert);
                String clientCert = (String) mEapUserCertSpinner.getSelectedItem();
                if (clientCert.equals(unspecifiedCert)) clientCert = "";
                config.enterpriseConfig.setClientCertificateAlias(clientCert);
                config.enterpriseConfig.setIdentity(mEapIdentityView.getText().toString());
                config.enterpriseConfig.setAnonymousIdentity(
                        mEapAnonymousView.getText().toString());

                if (mPasswordView.isShown()) {
                    // For security reasons, a previous password is not displayed to user.
                    // Update only if it has been changed.
                    if (mPasswordView.length() > 0) {
                        config.enterpriseConfig.setPassword(mPasswordView.getText().toString());
                    }
                } else {
                    // clear password
                    config.enterpriseConfig.setPassword(mPasswordView.getText().toString());
                }
                break;

            // Broadcom, WAPI
            case AccessPoint.SECURITY_WAPI_PSK:
                config.allowedKeyManagement.set(KeyMgmt.WAPI_PSK);
                config.allowedProtocols.set(Protocol.WAPI);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
                if (mPasswordView.length() != 0) {
                    String password = mPasswordView.getText().toString();
                    if (password.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = password;
                    } else {
                        config.preSharedKey = '"' + password + '"';
                    }
                }
                Log.d(TAG, "mWapiPskType.getSelectedItemPosition() " + mWapiPskType.getSelectedItemPosition());
                config.wapiPskType = WAPI_PSK_TYPE_VALUES[mWapiPskType.getSelectedItemPosition()];
                break;

            case AccessPoint.SECURITY_WAPI_CERT:
                config.allowedKeyManagement.set(KeyMgmt.WAPI_CERT);
                config.allowedProtocols.set(Protocol.WAPI);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
                Log.d(TAG, "mWapiAsCert.getSelectedItemPosition() " + mWapiAsCert.getSelectedItemPosition());
                Log.d(TAG, "mWapiAsCert.getSelectedItem() " + mWapiAsCert.getSelectedItem());
                Log.d(TAG, "mWapiUserCert.getSelectedItemPosition() " + mWapiUserCert.getSelectedItemPosition());
                Log.d(TAG, "mWapiUserCert.getSelectedItem() " + mWapiUserCert.getSelectedItem());
                config.wapiAsCert = ((mWapiAsCert.getSelectedItemPosition() == 0) ? "" :
                        AccessPoint.convertToQuotedString(KEYSTORE_SPACE + Credentials.WAPI_AS_CERTIFICATE + (String) mWapiAsCert.getSelectedItem()));
                config.wapiUserCert = ((mWapiUserCert.getSelectedItemPosition() == 0) ? "" :
                        AccessPoint.convertToQuotedString(KEYSTORE_SPACE + Credentials.WAPI_USER_CERTIFICATE + (String) mWapiUserCert.getSelectedItem()));
                break;
            // Broadcom, WAPI
            default:
                return null;
        }

        config.setIpConfiguration(
                new IpConfiguration(mIpAssignment, mProxySettings,
                                    mStaticIpConfiguration, mHttpProxy));

        return config;
    }

    private boolean ipAndProxyFieldsAreValid() {
        mIpAssignment = (mIpSettingsSpinner != null &&
                mIpSettingsSpinner.getSelectedItemPosition() == STATIC_IP) ?
                IpAssignment.STATIC : IpAssignment.DHCP;

        if (mIpAssignment == IpAssignment.STATIC) {
            mStaticIpConfiguration = new StaticIpConfiguration();
            int result = validateIpConfigFields(mStaticIpConfiguration);
            if (result != 0) {
                return false;
            }
        }

        final int selectedPosition = mProxySettingsSpinner.getSelectedItemPosition();
        mProxySettings = ProxySettings.NONE;
        mHttpProxy = null;
        if (selectedPosition == PROXY_STATIC && mProxyHostView != null) {
            mProxySettings = ProxySettings.STATIC;
            String host = mProxyHostView.getText().toString();
            String portStr = mProxyPortView.getText().toString();
            String exclusionList = mProxyExclusionListView.getText().toString();
            int port = 0;
            int result = 0;
            try {
                port = Integer.parseInt(portStr);
                result = ProxySelector.validate(host, portStr, exclusionList);
            } catch (NumberFormatException e) {
                result = R.string.proxy_error_invalid_port;
            }
            if (result == 0) {
                mHttpProxy = new ProxyInfo(host, port, exclusionList);
            } else {
                return false;
            }
        } else if (selectedPosition == PROXY_PAC && mProxyPacView != null) {
            mProxySettings = ProxySettings.PAC;
            CharSequence uriSequence = mProxyPacView.getText();
            if (TextUtils.isEmpty(uriSequence)) {
                return false;
            }
            Uri uri = Uri.parse(uriSequence.toString());
            if (uri == null) {
                return false;
            }
            mHttpProxy = new ProxyInfo(uri);
        }
        return true;
    }

    private Inet4Address getIPv4Address(String text) {
        try {
            return (Inet4Address) NetworkUtils.numericToInetAddress(text);
        } catch (IllegalArgumentException|ClassCastException e) {
            return null;
        }
    }

    private int validateIpConfigFields(StaticIpConfiguration staticIpConfiguration) {
        if (mIpAddressView == null) return 0;

        String ipAddr = mIpAddressView.getText().toString();
        if (TextUtils.isEmpty(ipAddr)) return R.string.wifi_ip_settings_invalid_ip_address;

        Inet4Address inetAddr = getIPv4Address(ipAddr);
        if (inetAddr == null) {
            return R.string.wifi_ip_settings_invalid_ip_address;
        }

        int networkPrefixLength = -1;
        try {
            networkPrefixLength = Integer.parseInt(mNetworkPrefixLengthView.getText().toString());
            if (networkPrefixLength < 0 || networkPrefixLength > 32) {
                return R.string.wifi_ip_settings_invalid_network_prefix_length;
            }
            staticIpConfiguration.ipAddress = new LinkAddress(inetAddr, networkPrefixLength);
        } catch (NumberFormatException e) {
            // Set the hint as default after user types in ip address
            mNetworkPrefixLengthView.setText(mConfigUi.getContext().getString(
                    R.string.wifi_network_prefix_length_hint));
        }

        String gateway = mGatewayView.getText().toString();
        if (TextUtils.isEmpty(gateway)) {
            try {
                //Extract a default gateway from IP address
                InetAddress netPart = NetworkUtils.getNetworkPart(inetAddr, networkPrefixLength);
                byte[] addr = netPart.getAddress();
                addr[addr.length-1] = 1;
                mGatewayView.setText(InetAddress.getByAddress(addr).getHostAddress());
            } catch (RuntimeException ee) {
            } catch (java.net.UnknownHostException u) {
            }
        } else {
            InetAddress gatewayAddr = getIPv4Address(gateway);
            if (gatewayAddr == null) {
                return R.string.wifi_ip_settings_invalid_gateway;
            }
            staticIpConfiguration.gateway = gatewayAddr;
        }

        String dns = mDns1View.getText().toString();
        InetAddress dnsAddr = null;

        if (TextUtils.isEmpty(dns)) {
            //If everything else is valid, provide hint as a default option
            mDns1View.setText(mConfigUi.getContext().getString(R.string.wifi_dns1_hint));
        } else {
            dnsAddr = getIPv4Address(dns);
            if (dnsAddr == null) {
                return R.string.wifi_ip_settings_invalid_dns;
            }
            staticIpConfiguration.dnsServers.add(dnsAddr);
        }

        if (mDns2View.length() > 0) {
            dns = mDns2View.getText().toString();
            dnsAddr = getIPv4Address(dns);
            if (dnsAddr == null) {
                return R.string.wifi_ip_settings_invalid_dns;
            }
            staticIpConfiguration.dnsServers.add(dnsAddr);
        }
        return 0;
    }

    private void showSecurityFields() {
        if (mAccessPointSecurity == AccessPoint.SECURITY_NONE) {
            mView.findViewById(R.id.security_fields).setVisibility(View.GONE);
            // SPRD: set eap field invisible in advanced field.
            mView.findViewById(R.id.eap).setVisibility(View.GONE);
            return;
        }
        mView.findViewById(R.id.security_fields).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.password_layout).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.show_password_layout).setVisibility(View.VISIBLE);

        if (mPasswordView == null) {
            mPasswordView = (TextView) mView.findViewById(R.id.password);
            mPasswordView.addTextChangedListener(this);
            ((CheckBox) mView.findViewById(R.id.show_password))
                .setOnCheckedChangeListener(this);

            if (mAccessPoint != null && mAccessPoint.isSaved()) {
                mPasswordView.setHint(R.string.wifi_unchanged);
            }
        }

        if(mAccessPointSecurity == AccessPoint.SECURITY_WEP){
            mView.findViewById(R.id.wep_layout).setVisibility(View.VISIBLE);
            mView.findViewById(R.id.wep_password_tip).setVisibility(View.VISIBLE);
            mWepKeyIndex = (Spinner) mView.findViewById(R.id.wep_key_index);
            mWepKeyIndex.setOnItemSelectedListener(this);

            Context context = mConfigUi.getContext();
            Resources resources = context.getResources();
            String[] wepkeyindex = resources.getStringArray(R.array.wep_key_index);

            ArrayAdapter<String> keyindexadapter = new ArrayAdapter<String>(
                    context, android.R.layout.simple_spinner_item, wepkeyindex);
            keyindexadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mWepKeyIndex.setAdapter(keyindexadapter);
        } else {
            mView.findViewById(R.id.wep_layout).setVisibility(View.GONE);
            mView.findViewById(R.id.wep_password_tip).setVisibility(View.GONE);
        }

        // Broadcom, WAPI
        if (mAccessPointSecurity != AccessPoint.SECURITY_WAPI_PSK) {
            mView.findViewById(R.id.wapi_psk).setVisibility(View.GONE);
        } else {
            mView.findViewById(R.id.wapi_psk).setVisibility(View.VISIBLE);
            mWapiPskType = (Spinner) mView.findViewById(R.id.wapi_psk_type);
            // SPRD: add the listener for mWapiPskType
            mWapiPskType.setOnItemSelectedListener(this);

            if (mAccessPoint != null && mAccessPoint.isSaved()) {
                WifiConfiguration config = mAccessPoint.getConfig();
                mWapiPskType.setSelection(config.wapiPskType);
            }
        }

        if (mAccessPointSecurity != AccessPoint.SECURITY_WAPI_CERT) {
            mView.findViewById(R.id.wapi_cert).setVisibility(View.GONE);
            mView.findViewById(R.id.password_layout).setVisibility(View.VISIBLE);
            mView.findViewById(R.id.show_password_layout).setVisibility(View.VISIBLE);
        } else {
            mView.findViewById(R.id.password_layout).setVisibility(View.GONE);
            mView.findViewById(R.id.show_password_layout).setVisibility(View.GONE);
            mView.findViewById(R.id.wapi_cert).setVisibility(View.VISIBLE);
            mWapiAsCert = (Spinner) mView.findViewById(R.id.wapi_as_cert);
            mWapiUserCert = (Spinner) mView.findViewById(R.id.wapi_user_cert);
            mWapiAsCert.setOnItemSelectedListener(this);
            mWapiUserCert.setOnItemSelectedListener(this);

            loadCertificates(mWapiAsCert, Credentials.WAPI_AS_CERTIFICATE);
            loadCertificates(mWapiUserCert, Credentials.WAPI_USER_CERTIFICATE);

            if (mAccessPoint != null && mAccessPoint.isSaved()) {
                WifiConfiguration config = mAccessPoint.getConfig();
                setCertificate(mWapiAsCert, Credentials.WAPI_AS_CERTIFICATE,
                        config.wapiAsCert);
                setCertificate(mWapiUserCert, Credentials.WAPI_USER_CERTIFICATE,
                        config.wapiUserCert);
            }
        }
        // Broadcom, WAPI

        if (mAccessPointSecurity != AccessPoint.SECURITY_EAP
                && mAccessPointSecurity != AccessPoint.SECURITY_FT_EAP) {
            mView.findViewById(R.id.eap).setVisibility(View.GONE);
            // SPRD: set l_identity field invisible when l_identity field is not in eap field.
            mView.findViewById(R.id.l_identity).setVisibility(View.GONE);
            return;
        }
        mView.findViewById(R.id.eap).setVisibility(View.VISIBLE);

        if (mEapMethodSpinner == null) {
            mEapMethodSpinner = (Spinner) mView.findViewById(R.id.method);
            /* SPRD: wifi plugin @{ */
            if (supportCMCC && isSsidCMCC()) {
                mEapMethodSpinner.setAdapter(PEAP_AND_SIM_METHOD_ADAPTER);
            } else {
                mEapMethodSpinner.setAdapter(WIFI_EAP_METHOD_ADAPTER);
            }
            /* @} */
            mEapMethodSpinner.setOnItemSelectedListener(this);
            if (Utils.isWifiOnly(mContext) || !mContext.getResources().getBoolean(
                    com.android.internal.R.bool.config_eap_sim_based_auth_supported)) {
                String[] eapMethods = mContext.getResources().getStringArray(
                        R.array.eap_method_without_sim_auth);
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(mContext,
                        android.R.layout.simple_spinner_item, eapMethods);
                spinnerAdapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item);
                mEapMethodSpinner.setAdapter(spinnerAdapter);
            }
            mPhase2Spinner = (Spinner) mView.findViewById(R.id.phase2);
            mEapCaCertSpinner = (Spinner) mView.findViewById(R.id.ca_cert);
            mEapUserCertSpinner = (Spinner) mView.findViewById(R.id.user_cert);
            mEapIdentityView = (TextView) mView.findViewById(R.id.identity);
            mEapAnonymousView = (TextView) mView.findViewById(R.id.anonymous);

            loadCertificates(mEapCaCertSpinner, Credentials.CA_CERTIFICATE);
            loadCertificates(mEapUserCertSpinner, Credentials.USER_PRIVATE_KEY);

            // Modifying an existing network
            if (mAccessPoint != null && mAccessPoint.isSaved()) {
                WifiEnterpriseConfig enterpriseConfig = mAccessPoint.getConfig().enterpriseConfig;
                int eapMethod = enterpriseConfig.getEapMethod();
                int phase2Method = enterpriseConfig.getPhase2Method();
                if (isSsidCMCC() && eapMethod > 0) {
                    mEapMethodSpinner.setSelection(1);
                } else {
                    mEapMethodSpinner.setSelection(eapMethod);
                }
                showEapFieldsByMethod(eapMethod);
                switch (eapMethod) {
                    case Eap.PEAP:
                        switch (phase2Method) {
                            case Phase2.NONE:
                                mPhase2Spinner.setSelection(WIFI_PEAP_PHASE2_NONE);
                                break;
                            case Phase2.MSCHAPV2:
                                mPhase2Spinner.setSelection(WIFI_PEAP_PHASE2_MSCHAPV2);
                                break;
                            case Phase2.GTC:
                                mPhase2Spinner.setSelection(WIFI_PEAP_PHASE2_GTC);
                                break;
                            default:
                                Log.e(TAG, "Invalid phase 2 method " + phase2Method);
                                break;
                        }
                        break;
                    default:
                        mPhase2Spinner.setSelection(phase2Method);
                        break;
                }
                setSelection(mEapCaCertSpinner, enterpriseConfig.getCaCertificateAlias());
                setSelection(mEapUserCertSpinner, enterpriseConfig.getClientCertificateAlias());
                mEapIdentityView.setText(enterpriseConfig.getIdentity());
                mEapAnonymousView.setText(enterpriseConfig.getAnonymousIdentity());
            } else {
                showEapFieldsByMethod(mEapMethodSpinner.getSelectedItemPosition());
            }
        } else {
            showEapFieldsByMethod(mEapMethodSpinner.getSelectedItemPosition());
        }
    }

    /**
     * EAP-PWD valid fields include
     *   identity
     *   password
     * EAP-PEAP valid fields include
     *   phase2: MSCHAPV2, GTC
     *   ca_cert
     *   identity
     *   anonymous_identity
     *   password
     * EAP-TLS valid fields include
     *   user_cert
     *   ca_cert
     *   identity
     * EAP-TTLS valid fields include
     *   phase2: PAP, MSCHAP, MSCHAPV2, GTC
     *   ca_cert
     *   identity
     *   anonymous_identity
     *   password
     */
    private void showEapFieldsByMethod(int eapMethod) {
        if (isSsidCMCC() && eapMethod > 0) {
            eapMethod = WIFI_EAP_METHOD_SIM;
        }
        // Common defaults
        mView.findViewById(R.id.l_method).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.l_identity).setVisibility(View.VISIBLE);

        // Defaults for most of the EAP methods and over-riden by
        // by certain EAP methods
        mView.findViewById(R.id.l_ca_cert).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.password_layout).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.show_password_layout).setVisibility(View.VISIBLE);

        mView.findViewById(R.id.l_sim_slot).setVisibility(View.GONE);

        switch (eapMethod) {
            case WIFI_EAP_METHOD_PWD:
                setPhase2Invisible();
                setCaCertInvisible();
                setAnonymousIdentInvisible();
                setUserCertInvisible();
                break;
            case WIFI_EAP_METHOD_TLS:
                mView.findViewById(R.id.l_user_cert).setVisibility(View.VISIBLE);
                setPhase2Invisible();
                setAnonymousIdentInvisible();
                setPasswordInvisible();
                break;
            case WIFI_EAP_METHOD_PEAP:
                // Reset adapter if needed
                if (mPhase2Adapter != PHASE2_PEAP_ADAPTER) {
                    mPhase2Adapter = PHASE2_PEAP_ADAPTER;
                    mPhase2Spinner.setAdapter(mPhase2Adapter);
                }
                mView.findViewById(R.id.l_phase2).setVisibility(View.VISIBLE);
                mView.findViewById(R.id.l_anonymous).setVisibility(View.VISIBLE);
                if (isSsidCMCC()) {
                    setPhase2Invisible();
                    setCaCertInvisible();
                    setAnonymousIdentInvisible();
                }
                setUserCertInvisible();
                break;
            case WIFI_EAP_METHOD_TTLS:
                // Reset adapter if needed
                if (mPhase2Adapter != PHASE2_FULL_ADAPTER) {
                    mPhase2Adapter = PHASE2_FULL_ADAPTER;
                    mPhase2Spinner.setAdapter(mPhase2Adapter);
                }
                mView.findViewById(R.id.l_phase2).setVisibility(View.VISIBLE);
                mView.findViewById(R.id.l_anonymous).setVisibility(View.VISIBLE);
                setUserCertInvisible();
                break;
            case WIFI_EAP_METHOD_SIM:
            case WIFI_EAP_METHOD_AKA:
            case WIFI_EAP_METHOD_AKA_PRIME:
                setPhase2Invisible();
                setAnonymousIdentInvisible();
                setCaCertInvisible();
                setUserCertInvisible();
                setPasswordInvisible();
                setIdentityInvisible();
                mView.findViewById(R.id.l_sim_slot).setVisibility(View.VISIBLE);
                mEapSimSlotSpinner = (Spinner) mView.findViewById(R.id.eap_sim_slots);
                mEapSimSlotSpinner.setVisibility(View.VISIBLE);
                loadEapSimSlots(mEapSimSlotSpinner);

                if (mAccessPoint != null && mAccessPoint.isSaved()) {
                    WifiConfiguration config = mAccessPoint.getConfig();
                    Log.d(TAG,"showSecurityFields() -> eap_sim_slot = " + config.eap_sim_slot);
                    setEapSimSlot(mEapSimSlotSpinner,config.eap_sim_slot);
                }
                break;
        }
    }

    private void setIdentityInvisible() {
        mView.findViewById(R.id.l_identity).setVisibility(View.GONE);
        mPhase2Spinner.setSelection(Phase2.NONE);
    }

    private void setPhase2Invisible() {
        mView.findViewById(R.id.l_phase2).setVisibility(View.GONE);
        mPhase2Spinner.setSelection(Phase2.NONE);
    }

    private void setCaCertInvisible() {
        mView.findViewById(R.id.l_ca_cert).setVisibility(View.GONE);
        mEapCaCertSpinner.setSelection(unspecifiedCertIndex);
    }

    private void setUserCertInvisible() {
        mView.findViewById(R.id.l_user_cert).setVisibility(View.GONE);
        mEapUserCertSpinner.setSelection(unspecifiedCertIndex);
    }

    private void setAnonymousIdentInvisible() {
        mView.findViewById(R.id.l_anonymous).setVisibility(View.GONE);
        mEapAnonymousView.setText("");
    }

    private void setPasswordInvisible() {
        mPasswordView.setText("");
        mView.findViewById(R.id.password_layout).setVisibility(View.GONE);
        mView.findViewById(R.id.show_password_layout).setVisibility(View.GONE);
    }

    private void showIpConfigFields() {
        WifiConfiguration config = null;

        mView.findViewById(R.id.ip_fields).setVisibility(View.VISIBLE);

        if (mAccessPoint != null && mAccessPoint.isSaved()) {
            config = mAccessPoint.getConfig();
        }

        if (mIpSettingsSpinner.getSelectedItemPosition() == STATIC_IP) {
            mView.findViewById(R.id.staticip).setVisibility(View.VISIBLE);
            if (mIpAddressView == null) {
                mIpAddressView = (TextView) mView.findViewById(R.id.ipaddress);
                mIpAddressView.addTextChangedListener(this);
                mGatewayView = (TextView) mView.findViewById(R.id.gateway);
                mGatewayView.addTextChangedListener(this);
                mNetworkPrefixLengthView = (TextView) mView.findViewById(
                        R.id.network_prefix_length);
                mNetworkPrefixLengthView.addTextChangedListener(this);
                mDns1View = (TextView) mView.findViewById(R.id.dns1);
                mDns1View.addTextChangedListener(this);
                mDns2View = (TextView) mView.findViewById(R.id.dns2);
                mDns2View.addTextChangedListener(this);
            }
            if (config != null) {
                StaticIpConfiguration staticConfig = config.getStaticIpConfiguration();
                if (staticConfig != null) {
                    if (staticConfig.ipAddress != null) {
                        mIpAddressView.setText(
                                staticConfig.ipAddress.getAddress().getHostAddress());
                        mNetworkPrefixLengthView.setText(Integer.toString(staticConfig.ipAddress
                                .getNetworkPrefixLength()));
                    }

                    if (staticConfig.gateway != null) {
                        mGatewayView.setText(staticConfig.gateway.getHostAddress());
                    }

                    Iterator<InetAddress> dnsIterator = staticConfig.dnsServers.iterator();
                    if (dnsIterator.hasNext()) {
                        mDns1View.setText(dnsIterator.next().getHostAddress());
                    }
                    if (dnsIterator.hasNext()) {
                        mDns2View.setText(dnsIterator.next().getHostAddress());
                    }
                }
            }
        } else {
            mView.findViewById(R.id.staticip).setVisibility(View.GONE);
        }
    }

    private void showProxyFields() {
        WifiConfiguration config = null;

        mView.findViewById(R.id.proxy_settings_fields).setVisibility(View.VISIBLE);

        if (mAccessPoint != null && mAccessPoint.isSaved()) {
            config = mAccessPoint.getConfig();
        }

        if (mProxySettingsSpinner.getSelectedItemPosition() == PROXY_STATIC) {
            setVisibility(R.id.proxy_warning_limited_support, View.VISIBLE);
            setVisibility(R.id.proxy_fields, View.VISIBLE);
            setVisibility(R.id.proxy_pac_field, View.GONE);
            if (mProxyHostView == null) {
                mProxyHostView = (TextView) mView.findViewById(R.id.proxy_hostname);
                mProxyHostView.addTextChangedListener(this);
                mProxyPortView = (TextView) mView.findViewById(R.id.proxy_port);
                mProxyPortView.addTextChangedListener(this);
                mProxyExclusionListView = (TextView) mView.findViewById(R.id.proxy_exclusionlist);
                mProxyExclusionListView.addTextChangedListener(this);
            }
            if (config != null) {
                ProxyInfo proxyProperties = config.getHttpProxy();
                if (proxyProperties != null) {
                    mProxyHostView.setText(proxyProperties.getHost());
                    mProxyPortView.setText(Integer.toString(proxyProperties.getPort()));
                    mProxyExclusionListView.setText(proxyProperties.getExclusionListAsString());
                }
            }
        } else if (mProxySettingsSpinner.getSelectedItemPosition() == PROXY_PAC) {
            setVisibility(R.id.proxy_warning_limited_support, View.GONE);
            setVisibility(R.id.proxy_fields, View.GONE);
            setVisibility(R.id.proxy_pac_field, View.VISIBLE);

            if (mProxyPacView == null) {
                mProxyPacView = (TextView) mView.findViewById(R.id.proxy_pac);
                mProxyPacView.addTextChangedListener(this);
            }
            if (config != null) {
                ProxyInfo proxyInfo = config.getHttpProxy();
                if (proxyInfo != null) {
                    mProxyPacView.setText(proxyInfo.getPacFileUrl().toString());
                }
            }
        } else {
            setVisibility(R.id.proxy_warning_limited_support, View.GONE);
            setVisibility(R.id.proxy_fields, View.GONE);
            setVisibility(R.id.proxy_pac_field, View.GONE);
        }
    }

    private void setVisibility(int id, int visibility) {
        final View v = mView.findViewById(id);
        if (v != null) {
            v.setVisibility(visibility);
        }
    }

    private void loadCertificates(Spinner spinner, String prefix) {
        final Context context = mConfigUi.getContext();

        String[] certs = KeyStore.getInstance().list(prefix, android.os.Process.WIFI_UID);
        if (certs == null || certs.length == 0) {
            certs = new String[] {unspecifiedCert};
        } else {
            final String[] array = new String[certs.length + 1];
            array[0] = unspecifiedCert;
            System.arraycopy(certs, 0, array, 1, certs.length);
            certs = array;
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                context, android.R.layout.simple_spinner_item, certs);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void setSelection(Spinner spinner, String value) {
        if (value != null) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
            for (int i = adapter.getCount() - 1; i >= 0; --i) {
                if (value.equals(adapter.getItem(i))) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }

    public boolean isEdit() {
        return mEdit;
    }

    public boolean isModify() {
        return mModify;
    }

    @Override
    public void afterTextChanged(Editable s) {
        mTextViewChangedHandler.post(new Runnable() {
                public void run() {
                    enableSubmitIfAppropriate();
                }
            });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // work done in afterTextChanged
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // work done in afterTextChanged
    }

    @Override
    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        if (view.getId() == R.id.show_password) {
            int pos = mPasswordView.getSelectionEnd();
            mPasswordView.setInputType(
                    InputType.TYPE_CLASS_TEXT | (isChecked ?
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                                InputType.TYPE_TEXT_VARIATION_PASSWORD));
            if (pos >= 0) {
                ((EditText)mPasswordView).setSelection(pos);
            }
        } else if (view.getId() == R.id.wifi_advanced_togglebox) {
            if (isChecked) {
                mView.findViewById(R.id.wifi_advanced_fields).setVisibility(View.VISIBLE);
            } else {
                mView.findViewById(R.id.wifi_advanced_fields).setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mSecuritySpinner) {
            mAccessPointSecurity = position;
            showSecurityFields();
        } else if (parent == mEapMethodSpinner) {
            showSecurityFields();
        } else if (parent == mProxySettingsSpinner) {
            showProxyFields();
        } else if(parent == mWepKeyIndex){
            mWepPosition = position;
        } else {
            showIpConfigFields();
        }
        enableSubmitIfAppropriate();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //
    }

    /**
     * Make the characters of the password visible if show_password is checked.
     */
    public void updatePassword() {
        TextView passwdView = (TextView) mView.findViewById(R.id.password);
        passwdView.setInputType(
                InputType.TYPE_CLASS_TEXT |
                (((CheckBox) mView.findViewById(R.id.show_password)).isChecked() ?
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                InputType.TYPE_TEXT_VARIATION_PASSWORD));
    }

    /* SPRD: add for EAP-SIM and EAP-AKA. */
    private void loadEapSimSlots(Spinner spinner) {
        final Context context = mConfigUi.getContext();
        final String unspecified = context.getString(R.string.wifi_unspecified);
        String[] certs;

        boolean slot1Enabled = false;
        boolean slot2Enabled = false;

        TelephonyManager mTelephonyManager = TelephonyManager.from(mContext);
        int num = mTelephonyManager.getPhoneCount();
        for (int i = 0; i < num; i++) {
            if (mTelephonyManager.hasIccCard(i)) {
                if (i == 0) {
                    slot1Enabled = true;
                } else if (i == 1) {
                    slot2Enabled = true;
                }
            }
        }

        if (slot1Enabled && slot2Enabled) {
            certs = new String[3];
            certs[0] = unspecified;
            certs[1] = context.getString(R.string.wifi_eap_sim_slot1);
            certs[2] = context.getString(R.string.wifi_eap_sim_slot2);
        } else if (slot1Enabled || slot2Enabled) {
            certs = new String[2];
            certs[0] = unspecified;
            if (slot1Enabled) {
                certs[1] = context.getString(R.string.wifi_eap_sim_slot1);
            } else {
                certs[1] = context.getString(R.string.wifi_eap_sim_slot2);
            }
        } else {
            certs = new String[] {unspecified};
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                context, android.R.layout.simple_spinner_item, certs);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    /**
     * SPRD: used to judge whether the password is valid for wep auth type.
     * @return true is invalid
     */
    private boolean isWepPasswordInvalid() {
        if (mPasswordView != null) {
            int length = mPasswordView.length();
            String password = mPasswordView.getText().toString();
            // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
            if ((length == 10 || length == 26 || length == 32) &&
                    password.matches("[0-9A-Fa-f]*")) {
                return false;
            } else if (length == 5 || length == 13 || length == 16) {
                byte[] bytePassword = password.getBytes();
                int asciiPassword = 0;
                for (byte b : bytePassword) {
                    asciiPassword = (int)b;
                    if (asciiPassword < 0 || asciiPassword > 127) return true;
                }
                return false;
            }
       }
       return true;
    }

    /* SPRD: add for EAP-SIM and EAP-AKA. */
    private void setEapSimSlot(Spinner spinner,int slot) {
        final Context context = mConfigUi.getContext();
        if (slot != -1) {
            if (slot == 0) {
                setSelection(spinner, context.getString(R.string.wifi_eap_sim_slot1));
            } else {
                setSelection(spinner, context.getString(R.string.wifi_eap_sim_slot2));
            }
        }
    }

    /* SPRD: add for Broadcom WAPI */
    private void setCertificate(Spinner spinner, String prefix, String cert) {
        prefix = KEYSTORE_SPACE + prefix;
        if (cert != null && cert.startsWith(prefix)) {
            setSelection(spinner, cert.substring(prefix.length()));
        }
    }

    private boolean isSsidCMCC() {
        if (supportCMCC && mAccessPoint != null) {
            return CMCC_SSID.equals(mAccessPoint.getSsidStr());
        } else {
            return false;
        }
    }
}
