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


import android.text.TextUtils;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;

import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.BugleNotifications;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseHelper;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.MessagingContentProvider;
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.sms.MmsUtils;
import com.android.messaging.util.LogUtil;
import com.android.messaging.mmslib.pdu.PduHeaders;
import com.android.messaging.Factory;

import android.content.ContentUris;
import android.database.Cursor;
import android.content.ContentResolver;

/**
 * Action used to delete a single message.
 */
public class SendReadReportAction extends Action implements Parcelable {
    private static final String TAG = LogUtil.BUGLE_DATAMODEL_TAG;
    private static final String KEY_CONVERSATION_ID = "conversation_id";

    public static void SendReadReport(final String conversationId) {
        final SendReadReportAction action = new SendReadReportAction(conversationId);
        action.start();
    }

    private SendReadReportAction(final String conversationId) {
        super();
        actionParameters.putString(KEY_CONVERSATION_ID, conversationId);
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
        String selection = Mms.MESSAGE_TYPE + " = " + PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF
                + " AND " + Mms.READ + " = 0"
                + " AND " + Mms.READ_REPORT + " = " + PduHeaders.VALUE_YES;
        final DatabaseWrapper db = DataModel.get().getDatabase();

        // First find the thread id for this conversation.
        final String conversationId = actionParameters.getString(KEY_CONVERSATION_ID);
        LogUtil.d(TAG, "doBackgroundWork conversationId = " + conversationId);
        if (!TextUtils.isEmpty(conversationId)) {
            // Check message still exists
            final long threadId = BugleDatabaseOperations.getThreadId(db, conversationId);
            if (threadId != -1) {
                selection = selection + " AND " + Mms.THREAD_ID + " = " + threadId;
            }
            LogUtil.d(TAG, "doBackgroundWork selection = " + selection);
            final Context context = Factory.get().getApplicationContext();
            final ContentResolver resolver = Factory.get().getApplicationContext().getContentResolver();

            final Cursor c = resolver.query(
                    Mms.Inbox.CONTENT_URI, new String[]{Mms._ID, Mms.MESSAGE_ID, Mms.SUBSCRIPTION_ID},
                    selection, null, null);
            LogUtil.d(TAG, "count:" + c.getCount());
            try {
                while (c.moveToNext()) {
                    int subId = c.getInt(2);
                    boolean b = MmsUtils.isEnabelReturnMmsReadReport(subId);
                    LogUtil.d(TAG, "doBackgroundWork subId = " + subId);
                    if (b) {
                        Uri uri = ContentUris.withAppendedId(Mms.CONTENT_URI, c.getLong(0));
                        LogUtil.d(TAG, "sendReadReport: uri = " + uri);
                        MmsUtils.sendReadRec(context, subId,
                                MmsUtils.getFrom(context, uri),
                                c.getString(1) != null ? c.getString(1) : "",
                                PduHeaders.READ_STATUS_READ);
                    }
                }
            }catch(Exception ex){
                LogUtil.d(TAG, "cursor while exception = " + ex);
            }finally{
                if (c != null && !c.isClosed()) {
                    c.close();
                }
            }
            //bug565044 start
            if (threadId != -1) {
                MmsUtils.updateSmsReadStatus(threadId, Long.MAX_VALUE);
            }

            // Update local db
            db.beginTransaction();
            try {
                final ContentValues values = new ContentValues();
                values.put(MessageColumns.CONVERSATION_ID, conversationId);
                values.put(MessageColumns.READ, 1);
                values.put(MessageColumns.SEEN, 1);     // if they read it, they saw it

                final int count = db.update(DatabaseHelper.MESSAGES_TABLE, values,
                        "(" + MessageColumns.READ + " !=1 OR " +
                                MessageColumns.SEEN + " !=1 ) AND " +
                                MessageColumns.CONVERSATION_ID + "=?",
                        new String[] { conversationId });
                if (count > 0) {
                    MessagingContentProvider.notifyMessagesChanged(conversationId);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            // After marking messages as read, update the notifications. This will
            // clear the now stale notifications.
            BugleNotifications.update(false/*silent*/, BugleNotifications.UPDATE_ALL);
            //bug565044 end
        }
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

    private SendReadReportAction(final Parcel in) {
        super(in);
    }

    public static final Parcelable.Creator<SendReadReportAction> CREATOR
            = new Parcelable.Creator<SendReadReportAction>() {
        @Override
        public SendReadReportAction createFromParcel(final Parcel in) {
            return new SendReadReportAction(in);
        }

        @Override
        public SendReadReportAction[] newArray(final int size) {
            return new SendReadReportAction[size];
        }
    };

    @Override
    public void writeToParcel(final Parcel parcel, final int flags) {
        writeActionToParcel(parcel, flags);
    }
}
