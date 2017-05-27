
package com.android.mmsfolderview.data;

import com.android.mmsfolderview.util.Dates;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

public class SortMsgListItemData {

    private int mSortType;
    private int mMessageId;
    private int mConversationId;
    private int mSenderId;
    private long mSentTimestamp;
    private long mReceivedTimestamp;
    private int mProtocol;
    private int mMessageStatus;
    private boolean mIsSeen;
    private boolean mIsRead;
    private String mSmsMessageUri;
    private int mSmsPriority;
    private int mSmsMessageSize;
    private String mMmsSubject;
    private String mMmsTransactionId;
    private String mContentLocation;
    private long mMmsExpiry;
    private int mRawStatus;
    private String mSelfId;
    private long mRetryStartTimestamp;
    private String mParticipantName;
    private String mParticipantDestination;
    private int mParticipantCount;
    private int mSubId;
    private int mSlotId;
    private String mSubscriptionName;
    private int mSubscriptionColor;
    private String mBobyText;
    private String mMultiMediaUriString;
    private String mContentType;
    private String mDisplayDestination;
    private String mFullName;
    private String mFirstName;

    private static int sIndexIncrementer = 0;
    public static final int INDEX_ID = sIndexIncrementer++;
    private static final int INDEX_CONVERSATION_ID = sIndexIncrementer++;
    private static final int INDEX_SENDER_ID = sIndexIncrementer++;
    private static final int INDEX_SENT_TIMESTAMP = sIndexIncrementer++;
    private static final int INDEX_RECEIVED_TIMESTAMP = sIndexIncrementer++;
    private static final int INDEX_MESSAGE_PROTOCOL = sIndexIncrementer++;
    private static final int INDEX_MESSAGE_STATUS = sIndexIncrementer++;
    private static final int INDEX_SEEN = sIndexIncrementer++;
    private static final int INDEX_READ = sIndexIncrementer++;
    private static final int INDEX_SMS_MESSAGE_URI = sIndexIncrementer++;
    private static final int INDEX_SMS_PRIORITY = sIndexIncrementer++;
    private static final int INDEX_SMS_MESSAGE_SIZE = sIndexIncrementer++;
    private static final int INDEX_MMS_SUBJECT = sIndexIncrementer++;
    private static final int INDEX_TRANSACTION_ID = sIndexIncrementer++;
    private static final int INDEX_MMS_CONTENT_LOCATION = sIndexIncrementer++;
    private static final int INDEX_MMS_EXPIRY = sIndexIncrementer++;
    private static final int INDEX_RAW_STATUS = sIndexIncrementer++;
    private static final int INDEX_SELF_ID = sIndexIncrementer++;
    private static final int INDEX_RETRY_START_TIMESTAMP = sIndexIncrementer++;
    private static final int INDEX_NAME = sIndexIncrementer++;
    private static final int INDEX_OTHER_PARTICIPANT_NORMALIZED_DESTINATION = sIndexIncrementer++;
    private static final int INDEX_PARTICIPANT_COUNT = sIndexIncrementer++;
    private static final int INDEX_SUB_ID = sIndexIncrementer++;
    private static final int INDEX_SIM_SLOT_ID = sIndexIncrementer++;
    private static final int INDEX_SUBSCRIPTION_NAME = sIndexIncrementer++;
    private static final int INDEX_SUBSCRIPTION_COLOR = sIndexIncrementer++;
    private static final int INDEX_TEXT = sIndexIncrementer++;
    private static final int INDEX_URI = sIndexIncrementer++;
    private static final int INDEX_CONTENT_TYPE = sIndexIncrementer++;
    private static final int INDEX_DISPLAY_DESTINATION = sIndexIncrementer++;
    private static final int INDEX_FULL_NAME = sIndexIncrementer++;
    private static final int INDEX_FIRST_NAME = sIndexIncrementer++;

    public void bind(final Cursor cursor) {
        mMessageId = cursor.getInt(INDEX_ID);
        mConversationId = cursor.getInt(INDEX_CONVERSATION_ID);
        mSenderId = cursor.getInt(INDEX_SENDER_ID);
        mSentTimestamp = cursor.getLong(INDEX_SENT_TIMESTAMP);
        mReceivedTimestamp = cursor.getLong(INDEX_RECEIVED_TIMESTAMP);
        mProtocol = cursor.getInt(INDEX_MESSAGE_PROTOCOL);
        mMessageStatus = cursor.getInt(INDEX_MESSAGE_STATUS);
        mIsSeen = (cursor.getInt(INDEX_SEEN) != 0);
        mIsRead = (cursor.getInt(INDEX_READ) != 0);
        mSmsMessageUri = cursor.getString(INDEX_SMS_MESSAGE_URI);
        mSmsPriority = cursor.getInt(INDEX_SMS_PRIORITY);
        mSmsMessageSize = cursor.getInt(INDEX_SMS_MESSAGE_SIZE);
        mMmsSubject = cursor.getString(INDEX_MMS_SUBJECT);
        mMmsTransactionId = cursor.getString(INDEX_TRANSACTION_ID);
        mContentLocation = cursor.getString(INDEX_MMS_CONTENT_LOCATION);
        mMmsExpiry = cursor.getLong(INDEX_MMS_EXPIRY);
        mRawStatus = cursor.getInt(INDEX_RAW_STATUS);
        mSelfId = cursor.getString(INDEX_SELF_ID);
        mRetryStartTimestamp = cursor.getLong(INDEX_RETRY_START_TIMESTAMP);
        mParticipantName = cursor.getString(INDEX_NAME);
        mParticipantDestination = cursor.getString(INDEX_OTHER_PARTICIPANT_NORMALIZED_DESTINATION);
        mParticipantCount = cursor.getInt(INDEX_PARTICIPANT_COUNT);
        mSubId = cursor.getInt(INDEX_SUB_ID);
        mSlotId = cursor.getInt(INDEX_SIM_SLOT_ID);
        mSubscriptionColor = cursor.getInt(INDEX_SUBSCRIPTION_COLOR);
        mSubscriptionName = cursor.getString(INDEX_SUBSCRIPTION_NAME);
        mBobyText = cursor.getString(INDEX_TEXT);
        mMultiMediaUriString = cursor.getString(INDEX_URI);
        mContentType = cursor.getString(INDEX_CONTENT_TYPE);
        mSortType = SortMsgDataCollector.getSortTypeByStatus(mMessageStatus);
        mDisplayDestination = cursor.getString(INDEX_DISPLAY_DESTINATION);
        mFullName = cursor.getString(INDEX_FULL_NAME);
        mFirstName = cursor.getString(INDEX_FULL_NAME);
    }

    public int getMessageId() {
        return mMessageId;
    }

    public int getConversationId() {
        return mConversationId;
    }

    public int getSenderId() {
        return mSenderId;
    }

    public long getSentTimestamp() {
        return mSentTimestamp;
    }

    public long getReceivedTimestamp() {
        return mReceivedTimestamp;
    }

    public int getMessageProtocol() {
        return mProtocol;
    }

    public int getMessageStatus() {
        return mMessageStatus;
    }

    public boolean getIsSeen() {
        return mIsSeen;
    }

    public boolean getIsRead() {
        return mIsRead;
    }

    public String getSmsMessageUri() {
        return mSmsMessageUri;
    }

    public int getSmsPriority() {
        return mSmsPriority;
    }

    public int getSmsMessageSize() {
        return mSmsMessageSize;
    }

    public String getMmsSubject() {
        return mMmsSubject;
    }

    public String getMmsTransactionId() {
        return mMmsTransactionId;
    }

    public String getContentLocation() {
        return mContentLocation;
    }

    public long getMmsExpiry() {
        return mMmsExpiry;
    }

    public int getRawStatus() {
        return mRawStatus;
    }

    public String getSelfId() {
        return mSelfId;
    }

    public long getRetryStartTimestamp() {
        return mRetryStartTimestamp;
    }

    public String getParticipantName() {
        return mParticipantName;
    }

    public String getParticipantDestination() {
        return mParticipantDestination;
    }

    public int getParticipantCount() {
        return mParticipantCount;
    }

    public int getSubId() {
        return mSubId;
    }

    public int getSlotId() {
        return mSlotId;
    }

    public int getDisplaySlotId() {
        return getSlotId() + 1;
    }

    public String getDisplayDestination() {
        return mDisplayDestination;
    }

    public String getFullName() {
        return mFullName;
    }

    public String getFirstName() {
        return mFirstName;
    }

    public boolean getIsHavedStoreContactName() {
        return (mFullName != null || mFirstName != null);
    }

    public int getSubscriptionColor() {
        // Force the alpha channel to 0xff to ensure the returned color is
        // solid.
        return mSubscriptionColor | 0xff000000;
    }

    public String getSubscriptionName() {
        return mSubscriptionName;
    }

    public boolean isActiveSubscription() {
        return mSlotId != SortMsgDataCollector.INVALID_SLOT_ID;
    }

    public String getBobyText() {
        return mBobyText;
    }

    public Uri getMultiMediaUri() {
        return TextUtils.isEmpty(mMultiMediaUriString) ? null : Uri.parse(mMultiMediaUriString);
    }

    public String getContentType() {
        return mContentType;
    }

    public int getSortType() {
        return mSortType;
    }

    public boolean getIsDrft() {
        return (mMessageStatus == SortMsgDataCollector.BUGLE_STATUS_OUTGOING_DRAFT);
    }

    public boolean getIsSms() {
        return (mProtocol == SortMsgDataCollector.PROTOCOL_SMS);
    }

    public boolean getIsMms() {
        return (mProtocol == SortMsgDataCollector.PROTOCOL_MMS);
    }

    public boolean getIsMmsNoti() {
        return (mProtocol == SortMsgDataCollector.PROTOCOL_MMS_PUSH_NOTIFICATION);
    }

    public final boolean getIsFailedStatus() {
        return (mMessageStatus == SortMsgDataCollector.BUGLE_STATUS_OUTGOING_FAILED
                || mMessageStatus == SortMsgDataCollector.BUGLE_STATUS_OUTGOING_FAILED_EMERGENCY_NUMBER
                || mMessageStatus == SortMsgDataCollector.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED || mMessageStatus == SortMsgDataCollector.BUGLE_STATUS_INCOMING_EXPIRED_OR_NOT_AVAILABLE);
    }

    public final boolean getIsSendFailedStatus() {
        return (mMessageStatus == SortMsgDataCollector.BUGLE_STATUS_OUTGOING_FAILED || mMessageStatus == SortMsgDataCollector.BUGLE_STATUS_OUTGOING_FAILED_EMERGENCY_NUMBER);
    }

    public final boolean getIsSendRequested() {
        return (mMessageStatus == SortMsgDataCollector.BUGLE_STATUS_OUTGOING_YET_TO_SEND
                || mMessageStatus == SortMsgDataCollector.BUGLE_STATUS_OUTGOING_AWAITING_RETRY
                || mMessageStatus == SortMsgDataCollector.BUGLE_STATUS_OUTGOING_SENDING || mMessageStatus == SortMsgDataCollector.BUGLE_STATUS_OUTGOING_RESENDING);
    }

    public boolean getIsMessageTypeOutgoing() {
        return !SortMsgDataCollector.getIsIncoming(mMessageStatus);
    }

    public String getFormattedTimestamp(Context context) {
        return Dates.getMessageTimeString(context, mReceivedTimestamp).toString();
    }

    public boolean isGroupMessage() {
        return ((getParticipantCount() > 1) || (TextUtils.isEmpty(getParticipantDestination())));
    }
}
