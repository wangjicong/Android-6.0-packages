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

package com.android.messaging.datamodel.action;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.MessagingContentProvider;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.sms.MmsUtils;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.PhoneUtils;

import android.telephony.SubscriptionInfo;

import java.util.List;

/**
 *
 */
public class CopySmsToSimAction extends Action implements Parcelable {
    private static final String TAG = "CopySmsToSimAction";
    public static final Uri ICC_URI = Uri.parse("content://sms/icc");

    public static void copySmsToSim(final String messageId, int subId) {
        final CopySmsToSimAction action = new CopySmsToSimAction(messageId, subId);
        action.start();
    }

    private static final String KEY_MESSAGE_ID = "message_id";
    private static final String KEY_SUB_ID = "sub_id";

    private CopySmsToSimAction(final String messageId, int phoneId) {
        super();
        actionParameters.putString(KEY_MESSAGE_ID, messageId);
        actionParameters.putInt(KEY_SUB_ID, phoneId);
    }

    // Doing this work in the background so that we're not competing with sync
    // which could bring the deleted message back to life between the time we deleted
    // it locally and deleted it in telephony (sync is also done on doBackgroundWork).
    //
    // Previously this block of code deleted from telephony first but that can be very
    // slow (on the order of seconds) so this was modified to first delete locally, trigger
    // the UI update, then delete from telephony.
    @Override
    protected Bundle doBackgroundWork() {
        LogUtil.d(TAG, "CopySmsToSimAction doBackgroundWork");
        final String messageId = actionParameters.getString(KEY_MESSAGE_ID);
        final int subId = actionParameters.getInt(KEY_SUB_ID);
        final DatabaseWrapper db = DataModel.get().getDatabase();
        final MessageData message = BugleDatabaseOperations.readMessage(db, messageId);
        LogUtil.d(TAG, "CopySmsToSimAction " + " subId:[" + subId + "]");
        MmsUtils.copySmsMessageToSim(message, subId);
        LogUtil.d(TAG, "CopySmsToSimAction doBackgroundWork end");
        //Uri simSmsUri = ICC_URI.buildUpon().appendPath(String.valueOf(indexOnIcc)).build();
        return null;
    }

    /**
     * Delete the message.
     */
    @Override
    protected Object executeAction() {
        requestBackgroundWork();
        return null;
    }

    private CopySmsToSimAction(final Parcel in) {
        super(in);
    }

    public static final Creator<CopySmsToSimAction> CREATOR
            = new Creator<CopySmsToSimAction>() {
        @Override
        public CopySmsToSimAction createFromParcel(final Parcel in) {
            return new CopySmsToSimAction(in);
        }

        @Override
        public CopySmsToSimAction[] newArray(final int size) {
            return new CopySmsToSimAction[size];
        }
    };

    @Override
    public void writeToParcel(final Parcel parcel, final int flags) {
        writeActionToParcel(parcel, flags);
    }
}
