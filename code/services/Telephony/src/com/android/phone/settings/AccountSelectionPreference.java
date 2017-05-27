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
 * limitations under the License
 */

package com.android.phone.settings;

import com.android.phone.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import java.util.List;
import java.util.Objects;
import com.sprd.android.config.OptConfig;

public class AccountSelectionPreference extends ListPreference implements
        Preference.OnPreferenceChangeListener {

    public interface AccountSelectionListener {
        boolean onAccountSelected(AccountSelectionPreference pref, PhoneAccountHandle account);
        void onAccountSelectionDialogShow(AccountSelectionPreference pref);
        void onAccountChanged(AccountSelectionPreference pref);
    }

    private final Context mContext;
    private AccountSelectionListener mListener;
    private PhoneAccountHandle[] mAccounts;
    private String[] mEntryValues;
    private CharSequence[] mEntries;
    private boolean mShowSelectionInSummary = true;
    private static String TAG = "AccountSelectionPreference";

    public AccountSelectionPreference(Context context) {
        this(context, null);
    }

    public AccountSelectionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setOnPreferenceChangeListener(this);
    }

    public void setListener(AccountSelectionListener listener) {
        mListener = listener;
    }

    public void setShowSelectionInSummary(boolean value) {
        mShowSelectionInSummary = value;
    }

    public void setModel(
            TelecomManager telecomManager,
            List<PhoneAccountHandle> accountsList,
            PhoneAccountHandle currentSelection,
            CharSequence nullSelectionString) {

        // SPRD: add for bug520354
        TelephonyManager telephonyManager = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);
        mAccounts = accountsList.toArray(new PhoneAccountHandle[accountsList.size()]);
        mEntryValues = new String[mAccounts.length + 1];
        mEntries = new CharSequence[mAccounts.length + 1];
        // SPRD: add for bug492166
        CharSequence[] origLabel = new CharSequence[mAccounts.length + 1];

        PackageManager pm = mContext.getPackageManager();

        int selectedIndex = mAccounts.length;  // Points to nullSelectionString by default
        int i = 0;
        for ( ; i < mAccounts.length; i++) {
            CharSequence label = telecomManager.getPhoneAccount(mAccounts[i]).getLabel();
            if (label != null) {
                label = pm.getUserBadgedLabel(label, mAccounts[i].getUserHandle());
            }
            /* SPRD: add for bug520354 @{ */
            int subId = telephonyManager.getSubIdForPhoneAccount(telecomManager
                    .getPhoneAccount(mAccounts[i]));
            int phoneId = telephonyManager.getPhoneId(subId);
            if (phoneId == SubscriptionManager.INVALID_PHONE_INDEX) {
                continue;
            }
            String simLabelPrefix = String.format(
                    mContext.getResources().getString(R.string.sim_label),
                    phoneId + 1);
                    
            
            if(simLabelPrefix != null && OptConfig.SUNVOV_CUSTOM_C7301_XLL_FWVGA){
		        simLabelPrefix = simLabelPrefix.replace("SIM1","Master Sim");
		        simLabelPrefix = simLabelPrefix.replace("SIM2","Slave Sim");
            }
            /* SPRD: add for bug492166 @{ */
            if (TextUtils.isEmpty(label)) {
                origLabel[i] = simLabelPrefix;
            } else {
                origLabel[i] = simLabelPrefix + label.toString();
            }
            /* @} */
            if (!TextUtils.isEmpty(label)) {
                CharSequence target = "%";
                CharSequence replacement = "%%";
                if (label.toString().contains(target)) {
                    label = label.toString().replace(target, replacement);
                }
            }
            /* @} */
            mEntries[i] = label == null ? null : label.toString();
            mEntryValues[i] = Integer.toString(i);
            if (Objects.equals(currentSelection, mAccounts[i])) {
                selectedIndex = i;
            }
        }
        mEntryValues[i] = Integer.toString(i);
        mEntries[i] = nullSelectionString;
        origLabel[i] = nullSelectionString;

        setEntryValues(mEntryValues);
        // SPRD: add for bug492166
        setEntries(origLabel);
        setValueIndex(selectedIndex);
        if (mShowSelectionInSummary) {
            setSummary(mEntries[selectedIndex]);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mListener != null) {
            int index = Integer.parseInt((String) newValue);
            PhoneAccountHandle account = index < mAccounts.length ? mAccounts[index] : null;
            if (mListener.onAccountSelected(this, account)) {
                if (mShowSelectionInSummary) {
                    setSummary(mEntries[index]);
                }
                if (index != findIndexOfValue(getValue())) {
                    setValueIndex(index);
                    mListener.onAccountChanged(this);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Modifies the dialog to change the default "Cancel" button to "Choose Accounts", which
     * triggers the {@link PhoneAccountSelectionPreferenceActivity} to be shown.
     *
     * @param builder The {@code AlertDialog.Builder}.
     */
    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        // Notify the listener that the dialog is about to be built.  This is important so that the
        // list of enabled accounts can be updated prior to showing the dialog.
        /* SPRD: modify for bug512537 @{ */
        if (mListener == null) {
            return;
        }
        /* @} */
        mListener.onAccountSelectionDialogShow(this);

        super.onPrepareDialogBuilder(builder);
    }
}
