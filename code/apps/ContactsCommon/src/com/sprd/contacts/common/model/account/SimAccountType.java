/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.sprd.contacts.common.model.account;

import com.android.contacts.common.R;
import com.android.contacts.common.model.account.AccountType.DefinitionException;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.IccPBForEncodeException;
import android.accounts.Account;
import android.accounts.AccountManager;

import com.android.contacts.common.model.account.BaseAccountType;
import com.android.contacts.common.model.dataitem.DataKind;

import android.util.Log;

import java.util.Locale;
import java.util.ArrayList;

public class SimAccountType extends BaseAccountType {
    private static final String TAG = "SimAccountType";

    public static final String ACCOUNT_TYPE = "sprd.com.android.account.sim";

    public SimAccountType(Context context, String resPackageName) {
        this.accountType = ACCOUNT_TYPE;
        this.resourcePackageName = resPackageName;
        this.syncAdapterPackageName = resPackageName;
        try {
            addDataKindStructuredName(context);
            addDataKindDisplayName(context);
            addDataKindPhone(context);
            mIsInitialized = true;
        } catch (DefinitionException e) {
            Log.e(TAG, "Problem building account type", e);
        }
    }

    @Override
    protected DataKind addDataKindStructuredName(Context context) throws DefinitionException {
        DataKind kind = addKind(new DataKind(StructuredName.CONTENT_ITEM_TYPE,
                R.string.nameLabelsGroup, -1, true));
        kind.actionHeader = new SimpleInflater(R.string.nameLabelsGroup);
        kind.actionBody = new SimpleInflater(Nickname.NAME);

        kind.typeOverallMax = 1;

        kind.fieldList = new ArrayList();
        kind.fieldList.add(new EditField(StructuredName.DISPLAY_NAME,
                R.string.full_name, FLAGS_PERSON_NAME).setOptional(false));
        return kind;
    }

    @Override
    protected DataKind addDataKindDisplayName(Context context) throws DefinitionException {
        DataKind kind = addKind(new DataKind(DataKind.PSEUDO_MIME_TYPE_DISPLAY_NAME,
                R.string.nameLabelsGroup, -1, true));
        kind.typeOverallMax = 1;
        kind.fieldList = new ArrayList();
        kind.fieldList.add(new EditField(StructuredName.DISPLAY_NAME,
                R.string.full_name, FLAGS_PERSON_NAME).setOptional(false));
        return kind;
    }

    @Override
    protected DataKind addDataKindPhoneticName(Context context) throws DefinitionException {
        // int phoneticName;
        // phoneticName = R.layout.phonetic_name_editor_view;
        // }

        DataKind kind = addKind(new DataKind(DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME,
                R.string.name_phonetic, -1, true));
        kind.actionHeader = new SimpleInflater(R.string.nameLabelsGroup);
        kind.actionBody = new SimpleInflater(Nickname.NAME);

        kind.typeOverallMax = 1;

        kind.fieldList = new ArrayList();
        kind.fieldList.add(new EditField(StructuredName.PHONETIC_FAMILY_NAME,
                R.string.name_phonetic_family, FLAGS_PHONETIC));
        kind.fieldList.add(new EditField(StructuredName.PHONETIC_GIVEN_NAME,
                R.string.name_phonetic_given, FLAGS_PHONETIC));

        return kind;
    }

    @Override
    protected DataKind addDataKindPhone(Context context) throws DefinitionException {
        final DataKind kind = super.addDataKindPhone(context);

        kind.typeColumn = Phone.TYPE;
        // kind.typeOverallMax = 1;
        kind.typeList = new ArrayList();
        // kind.typeList.add(buildPhoneType(Phone.TYPE_HOME).setSpecificMax(1));
        kind.typeList.add(buildPhoneType(Phone.TYPE_MOBILE).setSpecificMax(1));

        kind.fieldList = new ArrayList();
        kind.fieldList.add(new EditField(Phone.NUMBER, R.string.phoneLabelsGroup, FLAGS_PHONE));

        return kind;
    }

    @Override
    public boolean isGroupMembershipEditable() {
        return false;
    }

    @Override
    public boolean areContactsWritable() {
        return true;
    }

    /**
     * @return length of string by bytes, for Sim and Usim.
     */
    @Override
    public int getAccountTypeFieldsLength(Context context, Account account,
            String mimeType) {
        if (account == null || account.type == null || mimeType == null
                || context == null) {
            return -1;
        }
        AccountManager am = AccountManager.get(context);
        if (am == null) {
            return -1;
        }
        if (mimeType.equals(FIELDS_STRUCTUREDNAME)
                || mimeType.equals(FIELDS_PHONETICNAME)) {
            mimeType = StructuredName.CONTENT_ITEM_TYPE;
        }
        String tmpLen =am.getUserData(account, mimeType + "_length");
        if (tmpLen != null) {
            return Integer.parseInt(tmpLen);
        } else {
            return -1;
        }
    }

    @Override
    public int getTextFieldsEditorLength(String txtString, int maxLength) {
        if (txtString == null || maxLength <= 0) {
            return -1;
        }
        if (getGsmAlphabetBytes(txtString).length > maxLength) {
            int end = 1;
            int txtLen = txtString.length();
            String tmpStr = txtString.substring(0, end);
            if (txtLen <= 0 || tmpStr == null) {
                return -1;
            }
            while (getGsmAlphabetBytes(tmpStr).length <= maxLength) {
                end++;
                if (end > txtLen) {
                    break;
                }
                tmpStr = txtString.substring(0, end);
            }
            end--;
            return end;
        } else {
            return -1;
        }
    }

    private static byte[] getGsmAlphabetBytes(String txtString) {
        byte[] bytes = new byte[0];
        if (txtString == null) {
            txtString = "";
        }
        try {
            bytes = IccUtils.stringToGsmAlphaSS(txtString);
        } catch (IccPBForEncodeException e) {
            try {
                bytes = txtString.getBytes("utf-16be");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return bytes;
    }

    @Override
    public Drawable getDisplayIcon(Context context) {
        return context.getResources().getDrawable(R.drawable.icon_sim_sprd);
    }
}
