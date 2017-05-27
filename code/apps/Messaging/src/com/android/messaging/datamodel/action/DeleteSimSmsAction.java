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

/**
 * Action used to delete a single message.
 */
public class DeleteSimSmsAction extends Action implements Parcelable {
    private static final String TAG = LogUtil.BUGLE_DATAMODEL_TAG;
    public static final Uri ICC_URI = Uri.parse("content://sms/icc");

    public static void deleteMessage(final int indexOnIcc, final int subId) {
        LogUtil.d(LogUtil.BUGLE_TAG, "DeleteSimSmsAction executeAction indexOnIcc:[" + indexOnIcc + "]");
        final DeleteSimSmsAction action = new DeleteSimSmsAction(indexOnIcc, subId);
        action.start();
    }

    private static final String KEY_INDEX_ON_ICC = "key_index_on_icc";
    private static final String KEY_SUB_ID = "key_sub_id";

    private DeleteSimSmsAction(final int indexOnIcc, final int subId) {
        super();
        actionParameters.putInt(KEY_INDEX_ON_ICC, indexOnIcc);
        actionParameters.putInt(KEY_SUB_ID, subId);
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
        final DatabaseWrapper db = DataModel.get().getDatabase();

        // First find the thread id for this conversation.
        final int indexOnIcc = actionParameters.getInt(KEY_INDEX_ON_ICC);
        final int subId = actionParameters.getInt(KEY_SUB_ID);
        Uri simSmsUri = ICC_URI.buildUpon().appendPath(String.valueOf(indexOnIcc)).build();
        simSmsUri = simSmsUri.buildUpon().appendPath("sub_id").build();
        simSmsUri = simSmsUri.buildUpon().appendPath(String.valueOf(subId)).build();
        LogUtil.d(LogUtil.BUGLE_TAG, "DeleteSimSmsAction doBackgroundWork Uri:[" + simSmsUri + "]");
        MmsUtils.deleteSimSmsMessage(simSmsUri);

        return null;
    }

    /**
     * Delete the message.
     */
    @Override
    protected Object executeAction() {
        LogUtil.d(LogUtil.BUGLE_TAG, "DeleteSimSmsAction executeAction");
        requestBackgroundWork();
        return null;
    }

    private DeleteSimSmsAction(final Parcel in) {
        super(in);
    }

    public static final Creator<DeleteSimSmsAction> CREATOR
            = new Creator<DeleteSimSmsAction>() {
        @Override
        public DeleteSimSmsAction createFromParcel(final Parcel in) {
            return new DeleteSimSmsAction(in);
        }

        @Override
        public DeleteSimSmsAction[] newArray(final int size) {
            return new DeleteSimSmsAction[size];
        }
    };

    @Override
    public void writeToParcel(final Parcel parcel, final int flags) {
        writeActionToParcel(parcel, flags);
    }
}
