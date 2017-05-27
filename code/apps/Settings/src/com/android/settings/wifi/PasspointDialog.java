/*
 *Copyright (C) 2015 SPRD Passpoint R1 Feature
 */

package com.android.settings.wifi;

import static android.net.wifi.WifiConfiguration.INVALID_NETWORK_ID;

import com.android.settings.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.IpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiConfiguration.Protocol;
import android.net.wifi.WifiEnterpriseConfig.Eap;
import android.net.wifi.WifiEnterpriseConfig.Phase2;
import android.os.Bundle;
import android.os.Handler;
import android.security.Credentials;
import android.security.KeyStore;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

class PasspointDialog extends AlertDialog implements AdapterView.OnItemSelectedListener,
                        OnCheckedChangeListener,TextWatcher {

    private final DialogInterface.OnClickListener mListener;

    private View mView;

    private Spinner mSecuritySpinner;
    private Spinner mTlsCaCertSpinner;
    private Spinner mTlsUserCertSpinner;
    private Spinner mTtlsCaCertSpinner;
    private Spinner mTtlsSecondAuthSpinner;
    private Spinner mSimSelectSpinner;

    private CheckBox mCheckboxPw;
    private TextView mPasswordView;
    private TextView mRealmView;
    private TextView mDomainView;
    private TextView mRoamView;
    private TextView mUsernameView;
    private TextView mPriorityView;
    private String unspecifiedCert = "unspecified";
    private final Handler mTextViewChangedHandler;
    private int mEAPSecurity;

    static final int SECURITY_EAP_TLS = 0;
    static final int SECURITY_EAP_TTLS = 1;
    static final int SECURITY_EAP_SIM = 2;
    static final int SECURITY_EAP_AKA = 3;

    public PasspointDialog(Context context, DialogInterface.OnClickListener listener, boolean edit) {
        super(context);
        mListener = listener;
        mTextViewChangedHandler = new Handler();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.passpoint_dialog, null);
        setView(mView);
        setInverseBackgroundForced(true);
        this.setTitle(R.string.passpoint_add_config);
        unspecifiedCert = getContext().getResources().getString(R.string.wifi_unspecified);
        mSecuritySpinner = ((Spinner) mView.findViewById(R.id.eap_method));
        mSecuritySpinner.setOnItemSelectedListener(this);
        mTlsCaCertSpinner = ((Spinner) mView.findViewById(R.id.tls_ca_cert));
        mTlsCaCertSpinner.setOnItemSelectedListener(this);
        mTlsUserCertSpinner = ((Spinner) mView.findViewById(R.id.tls_user_cert));
        mTlsUserCertSpinner.setOnItemSelectedListener(this);
        mTtlsCaCertSpinner = ((Spinner) mView.findViewById(R.id.ttls_ca_cert));
        mTtlsCaCertSpinner.setOnItemSelectedListener(this);
        mTtlsSecondAuthSpinner = ((Spinner) mView.findViewById(R.id.ttls_sec_auth));
        mSimSelectSpinner = ((Spinner) mView.findViewById(R.id.sim_select));
        mSimSelectSpinner.setOnItemSelectedListener(this);
        mCheckboxPw = ((CheckBox) mView.findViewById(R.id.show_password));
        mCheckboxPw.setOnCheckedChangeListener(this);
        mPasswordView = ((TextView) mView.findViewById(R.id.password));
        mPasswordView.addTextChangedListener(this);
        mRealmView = ((TextView) mView.findViewById(R.id.realm));
        mRealmView.addTextChangedListener(this);
        mDomainView = ((TextView) mView.findViewById(R.id.domain));
        mDomainView.addTextChangedListener(this);
        mRoamView = ((TextView) mView.findViewById(R.id.roam));
        mRoamView.addTextChangedListener(this);
        mUsernameView = ((TextView) mView.findViewById(R.id.username));
        mUsernameView.addTextChangedListener(this);
        mPriorityView = ((TextView) mView.findViewById(R.id.priority));
        mPriorityView.addTextChangedListener(this);

        loadCertificates(mTlsCaCertSpinner, Credentials.CA_CERTIFICATE);
        loadCertificates(mTlsUserCertSpinner, Credentials.USER_PRIVATE_KEY);
        loadCertificates(mTtlsCaCertSpinner, Credentials.CA_CERTIFICATE);
        loadEapSimSlots(mSimSelectSpinner);
        setButton(BUTTON_NEGATIVE,getContext().getResources().getString(R.string.wifi_cancel),mListener);
        setButton(BUTTON_POSITIVE,getContext().getResources().getString(R.string.wifi_save),mListener);

        if (getButton(BUTTON_POSITIVE) != null) {
            enableSubmitIfAppropriate();
        }
        super.onCreate(savedInstanceState);
    }

    private void loadCertificates(Spinner spinner, String prefix) {
        final Context context = getContext();

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

    private void loadEapSimSlots(Spinner spinner) {
        final Context context = getContext();
        final String unspecified = context.getString(R.string.wifi_unspecified);
        TelephonyManager mTelephonyManager = TelephonyManager.getDefault();
        String[] certs;
        int num = mTelephonyManager.getPhoneCount();
        boolean slot1Enabled = false;
        boolean slot2Enabled = false;
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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position,
            long id) {
        // TODO Auto-generated method stub
        if (parent == mSecuritySpinner) {
            mEAPSecurity = position;
            if (position != SECURITY_EAP_TLS) {
                mView.findViewById(R.id.eap_tls).setVisibility(View.GONE);
            }
            else {
                mView.findViewById(R.id.eap_tls).setVisibility(View.VISIBLE);
            }
            if (position != SECURITY_EAP_TTLS) {
                mView.findViewById(R.id.eap_ttls).setVisibility(View.GONE);
            }
            else {
                mView.findViewById(R.id.eap_ttls).setVisibility(View.VISIBLE);
            }
            if (position != SECURITY_EAP_SIM && position != SECURITY_EAP_AKA) {
                mView.findViewById(R.id.eap_sim).setVisibility(View.GONE);
            }
            else {
                mView.findViewById(R.id.eap_sim).setVisibility(View.VISIBLE);
            }
        }
        enableSubmitIfAppropriate();
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // TODO Auto-generated method stub
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
        }
    }
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
            int after) {
        // TODO Auto-generated method stub
    }
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // TODO Auto-generated method stub
    }
    @Override
    public void afterTextChanged(Editable s) {
        // TODO Auto-generated method stub
        mTextViewChangedHandler.post(new Runnable() {
            public void run() {
                enableSubmitIfAppropriate();
            }
        });
    }

    void enableSubmitIfAppropriate() {

        boolean enabled = false;
        boolean selectInvalid = false;
        if (mEAPSecurity == SECURITY_EAP_TLS
                && ((mTlsCaCertSpinner != null && mTlsCaCertSpinner.getSelectedItemPosition() == 0)
                        || (mTlsUserCertSpinner != null && mTlsUserCertSpinner.getSelectedItemPosition() == 0))) {
            selectInvalid = true;
        } else if ((mEAPSecurity == SECURITY_EAP_SIM || mEAPSecurity == SECURITY_EAP_AKA)
                && (mSimSelectSpinner != null) && (mSimSelectSpinner.getSelectedItemPosition() == 0)){
            selectInvalid = true;
        }

        if (mEAPSecurity == SECURITY_EAP_TLS) {
            if ((mRealmView != null && mRealmView.length() == 0) ||
                    (mPriorityView != null && mPriorityView.length() == 0) ||
                    (mDomainView != null && mDomainView.length() == 0) ||
                    (mUsernameView != null && mUsernameView.length() == 0) || selectInvalid) {
                enabled = false;
            } else {
                enabled = true;
            }

        } else if (mEAPSecurity == SECURITY_EAP_TTLS) {
            if ((mRealmView != null && mRealmView.length() == 0) ||
                    (mPriorityView != null && mPriorityView.length() == 0) ||
                    (mDomainView != null && mDomainView.length() == 0) ||
                    (mUsernameView != null && mUsernameView.length() == 0) ||
                    (mPasswordView != null && mPasswordView.length() == 0) || selectInvalid) {
                enabled = false;
            } else {
                enabled = true;
            }
        } else if (mEAPSecurity == SECURITY_EAP_SIM || mEAPSecurity == SECURITY_EAP_AKA) {
            if ((mPriorityView != null && mPriorityView.length() == 0) ||
                    (mDomainView != null && mDomainView.length() == 0) ||selectInvalid) {
                enabled = false;
            } else {
                enabled = true;
            }
        } else {
            if ((mRealmView != null && mRealmView.length() == 0) ||
                    (mDomainView != null && mDomainView.length() == 0) ||
                    (mPriorityView != null && mPriorityView.length() == 0) ||
                    (mRoamView != null && mRoamView.length() == 0) ||
                    (mUsernameView != null && mUsernameView.length() == 0) ||
                    (mPasswordView != null && mPasswordView.length() == 0) || selectInvalid) {
                enabled = false;
            } else {
                enabled = true;
            }
        }

        getButton(BUTTON_POSITIVE).setEnabled(enabled);
    }

    WifiConfiguration getConfig() {

        WifiConfiguration config = new WifiConfiguration();
        String caCert;
        String clientCert;
        int length;
        if (mPriorityView.getText().toString() != null) {
            config.priority = Integer.parseInt(mPriorityView.getText().toString());
        } else {
            config.priority = 0;
        }
        config.FQDN = mDomainView.getText().toString();
        config.providerFriendlyName = config.FQDN;
        if (config.enterpriseConfig == null ) {
            config.enterpriseConfig = new WifiEnterpriseConfig();
        }
        config.enterpriseConfig.setRealm(mRealmView.getText().toString());
        length = ((String)mSimSelectSpinner.getSelectedItem()).length();
        switch (mEAPSecurity) {
            case SECURITY_EAP_TLS:
                config.enterpriseConfig.setEapMethod(Eap.TLS);
                caCert = (String) mTlsCaCertSpinner.getSelectedItem();
                if (caCert.equals(unspecifiedCert)) caCert = "";
                config.enterpriseConfig.setCaCertificateAlias(caCert);
                clientCert = (String) mTlsUserCertSpinner.getSelectedItem();
                if (clientCert.equals(unspecifiedCert)) clientCert = "";
                config.enterpriseConfig.setClientCertificateAlias(clientCert);
                break;

            case SECURITY_EAP_TTLS:
                config.enterpriseConfig.setEapMethod(Eap.TTLS);
                caCert = (String) mTtlsCaCertSpinner.getSelectedItem();
                if (caCert.equals(unspecifiedCert)) caCert = "";
                config.enterpriseConfig.setCaCertificateAlias(caCert);
                config.enterpriseConfig.setPhase2Method(mTtlsSecondAuthSpinner.getSelectedItemPosition());
                break;

            case SECURITY_EAP_SIM:
                config.enterpriseConfig.setEapMethod(Eap.SIM);
                config.eap_sim_slot = ((mSimSelectSpinner.getSelectedItemPosition() == 0) ? -1 :
                    Integer.parseInt(((String)mSimSelectSpinner.getSelectedItem()).substring(length -1))) - 1;
                break;
            case SECURITY_EAP_AKA:
                config.enterpriseConfig.setEapMethod(Eap.AKA);
                config.eap_sim_slot = ((mSimSelectSpinner.getSelectedItemPosition() == 0) ? -1 :
                    Integer.parseInt(((String)mSimSelectSpinner.getSelectedItem()).substring(length -1))) - 1;
                break;
            default:
        }

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

        if (mUsernameView.length() > 0) {
            config.enterpriseConfig.setIdentity(mUsernameView.getText().toString());
        }
        return config;
    }
}
