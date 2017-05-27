
package com.sprd.messaging.ui.folderview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony;

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView.FindListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;
import android.widget.ImageView.ScaleType;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.MessagingContentProvider;
import com.android.messaging.datamodel.action.RedownloadMmsAction;
import com.android.messaging.datamodel.action.ResendMessageAction;
import com.android.messaging.datamodel.binding.Binding;
import com.android.messaging.datamodel.binding.BindingBase;
import com.android.messaging.datamodel.data.ConversationData;
import com.android.messaging.datamodel.data.ConversationMessageData;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.datamodel.data.PendingAttachmentData;
import com.android.messaging.datamodel.media.ImageRequestDescriptor;
import com.android.messaging.datamodel.media.MessagePartImageRequestDescriptor;
import com.android.messaging.sms.MmsUtils;
import com.android.messaging.ui.AsyncImageView;
import com.android.messaging.ui.AudioAttachmentView;
import com.android.messaging.ui.BugleActionBarActivity;
import com.android.messaging.ui.ConversationDrawables;
import com.android.messaging.ui.MultiAttachmentLayout;
import com.android.messaging.ui.MultiAttachmentLayout.OnAttachmentClickListener;
import com.android.messaging.ui.conversation.ConversationFragment.SaveAttachmentTask;
import com.android.messaging.ui.PersonItemView;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.ui.VideoThumbnailView;
import com.android.messaging.util.Assert;
import com.android.messaging.util.BuglePrefs;
import com.android.messaging.util.BuglePrefsKeys;
import com.android.messaging.util.ChangeDefaultSmsAppHelper;
import com.android.messaging.util.ContentType;
import com.android.messaging.util.Dates;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.OsUtil;
import com.android.messaging.util.PhoneUtils;
import com.android.messaging.util.TextUtil;
import com.android.messaging.util.UiUtils;
import com.android.messaging.widget.WidgetConversationProvider;
import com.android.messaging.datamodel.ParticipantRefresh;

import com.google.common.base.Predicate;

//add for bug 553054 begin
import android.text.format.Formatter;
//add for bug 553054 end

//add for Bug 558980 begin
import android.os.HandlerThread;
//add for Bug 558980 end

public class MessageDetailsActivity extends BugleActionBarActivity implements View.OnClickListener,
        View.OnLongClickListener, OnAttachmentClickListener {

    private static final String TAG = "MessageDetailsActivity";

    public static final int INVALID_ID = -1;
    public static final int SIZE_MEASURE_COMPLETE = 0;//Add for Bug 558980
    public static String MESSAGING_PACKAGE_NAME = "com.android.messaging";
    public static String MESSAGING_NEW_MESSAGE = "com.android.messaging.ui.conversation.ConversationActivity";
    public static String MESSAGING_COMMON_SERVICE = "com.sprd.messaging.ui.folderview.FolderViewMessagingCommService";

    private static final String SMS_SCHEME = "smsto:";
    private static final String MMS_SCHEME = "mmsto:";
    private static final String ADDRESS = "address";
    private static final String IS_FROM_FOLDER_VIEW_REPLY = "is_from_folder_view_reply";
    // Context Menu Item
    private static final int MENU_COPY_MESSAGE_TEXT = 0;
    private static final int MENU_LOCK_MESSAGE = 1;
    private static final int MENU_UNLOCK_MESSAGE = 2;
    private static final int MENU_COPY_MESSAGE_TO_SIM = 3;
    private static final int MENU_SAVE_ALL_ATTACHMENTS = 4;
    private static final int MENU_RESEND = 5;

    public static final int REQUEST_CODE_SET_DEFAULT_SMS_APP_TO_DELETE_MSG = 1;
    public static final int REQUEST_CODE_SET_DEFAULT_SMS_APP_TO_SEND_MSG = 2;
    public static final int REQUEST_CODE_SET_DEFAULT_SMS_APP_TO_DOWNLOAD_MSG = 3;
    // Intent extras
    public static final String UI_INTENT_EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String UI_INTENT_EXTRA_MESSAGE_ID = "message_id";
    public static final String UI_INTENT_EXTRA_PARTICIPANT_NAME = "participant_name";
    public static final String UI_INTENT_EXTRA_MMS_SUBJECT = "mms_subject";
    public static final String UI_INTENT_EXTRA_RECEIVED_TIMESTAMP = "received_timestamp";
    public static final String UI_INTENT_EXTRA_MESSAGE_PROTOCOL = "message_protocol";
    public static final String UI_INTENT_EXTRA_READ = "read";
    public static final String UI_INTENT_EXTRA_SEEN = "seen";
    public static final String UI_INTENT_EXTRA_BODY_TEXT = "body_text";
    public static final String UI_INTENT_EXTRA_MESSAGE_STATUS = "message_status";
    public static final String UI_INTENT_EXTRA_TELEPHONY_DB_URI = "sms_message_uri";
    public static final String UI_INTENT_EXTRA_CONTENT_TYPE = "content_type";
    public static final String UI_INTENT_EXTRA_WITH_CUSTOM_TRANSITION = "with_custom_transition";
    public static final String UI_INTENT_EXTRA_PARTICIPANT_NORMALIZED_DESTINATION = "participant_normalized_destination";
    public static final String UI_INTENT_EXTRA_IS_HAVED_STORE_CONTACT_NAME = "is_haved_store_contact_name";
    public static final String UI_INTENT_EXTRA_IS_COME_FROM_NOTIFICATION = "is_come_from_notification";

    private ConversationMessageData mData;
    private LinearLayout mMessageAttachmentsView;
    private MultiAttachmentLayout mMultiAttachmentView;
    private AsyncImageView mMessageImageView;
    private Button mDownloadButton;
    private TextView mDownloadStatus;
    private TextView mRecipeintsTextView;
    private TextView mSubjectLabel;
    private TextView mSubjectTextView;
    private TextView mContentTextView;
    private TextView mDateTextView;
    private TextView mSizeTextView; // add for bug 553054
    private View mSubjectDividerView;
    private ImageButton mAttachmentShowButton;
    private ZoomControls mZCButton;
    private float mTextSize = 16.0f;
    private float mTextS = 16.0f;
    private final float DISTANCE = 2.0f;
    private final float MAX_TEXT_SIZE = 24.0f;
    private final float MIN_TEXT_SIZE = 10.0f;

    private int mSortType;
    private int mOriginalSortType;
    private int mMessageStatus;
    private int mMessageId;
    private String mConversationId;
    private long mReceivedTimestamp;
    private int mProtocol;
    private boolean mIsRead;
    private boolean mIsSeen;
    private boolean mIsSendFailedStatus;
    private boolean mIsActivityVisibility;
    private String mSmsMessageUri;
    private String mMmsSubject;
    private String mParticipantName;
    private String mBobyText;
    private boolean mHasAttachment;
    private String mParticipant;
    private boolean mIsHavedStoreContactName;
    private boolean mIsComefromNotification;
    private AlertDialog mContextDialog;

    // add for Bug 558980 begin
    private Handler mSubHandler;
    private static final int EVENT_COUNT_MMS_SIZE = 1;
    private static final int EVENT_RETURN_MMS_SIZE = 2;
    private static final int EVENT_QUIT_WORK_THREAD = 3;
    // add for Bug 558980 end

    public static final String TRANSMIT_STATUS_ACTION = WidgetConversationProvider.ACTION_NOTIFY_MESSAGES_CHANGED;
    private BroadcastReceiver mTransmitStatusBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // add for bug 527147 begin
            if ((mConversationId != null) && (intent != null) && TRANSMIT_STATUS_ACTION.equals(intent.getAction())
            //add for bug 527147 end
                    && mConversationId.equals(intent
                            .getStringExtra(UI_INTENT_EXTRA_CONVERSATION_ID))) {
                Log.d("tim_t", "onReceive:mData.getStatus()=" + mData.getStatus());
                updateMessageDetails(false);
            }
        }
    };

    // Add for Bug 558980 begin
    private Handler mMainHandler = new Handler() {

        public void handleMessage(Message msg) {
            LogUtil.d("MessageDetailsActivity", "mHander handleMessage msg.what = " + msg.what);
            switch (msg.what) {
                case EVENT_RETURN_MMS_SIZE:
                    final StringBuilder size = new StringBuilder();
                    size.append(MessageDetailsActivity.this.getResources()
                            .getString(R.string.message_size_label).trim());
                    int subjectSize = TextUtil.isAllWhitespace(mMmsSubject) ? 0 : mMmsSubject
                            .getBytes().length;
                    int messageSize = msg.arg1 + subjectSize;
                    size.append(Formatter.formatFileSize(MessageDetailsActivity.this, messageSize));
                    mSizeTextView.setText(size.toString());
                    break;

                default:
                    break;
            }
        }
    };

    private class SizeCounterWorkThread extends HandlerThread implements Handler.Callback {

        public SizeCounterWorkThread(String name) {
            super(name);
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_COUNT_MMS_SIZE:
                    int size = MmsUtils.getPartsLength(MessageDetailsActivity.this, mData);
                    Message retMsg = mMainHandler.obtainMessage(EVENT_RETURN_MMS_SIZE);
                    retMsg.arg1 = size;
                    mMainHandler.sendMessage(retMsg);
                    break;
                case EVENT_QUIT_WORK_THREAD:
                    if (getLooper() != null) {
                        Log.d(TAG, "quit the work thread.");
                        getLooper().quit();
                    }
                    break;
            }
            return true;
        }
    }

    private void initWorkThread() {
        initCountSizeThread();
    }

    private void initCountSizeThread() {
        SizeCounterWorkThread handlerThread = new SizeCounterWorkThread(TAG);
        handlerThread.start();
        mSubHandler = new Handler(handlerThread.getLooper(), handlerThread);
    }

    // Add for Bug 558980 end

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_details_activity);
        invalidateActionBar();
        initResource();
        // add for Bug 558980 begin
        initWorkThread();
        // add for Bug 558980 end
        getIntentValues();
        mData = new ConversationMessageData();
        updateMessageDetails(true);
        if (!mIsSeen) {// In here, mIsSeen is true, just ensure the
                       // notification had been cancel.
            startCommonServiceToCancelNotification();
        }
        if (!mIsRead) {
            markAsReadInBugleDatabase();
            markAsReadInTelephonyDatabase();
        }
        registerReceiver(mTransmitStatusBroadcastReceiver, new IntentFilter(TRANSMIT_STATUS_ACTION));
    }

    private void updateMessageDetails(boolean isFirstTime) {
        bindCursorData();
        updateMessageContent(isFirstTime/* first time */);
        updateMessageTransmitStatus();
        updateMessageAttachments();
    }

    private void getIntentValues() {
        if (getIntent() != null) {
            mMessageId = getIntent().getIntExtra(UI_INTENT_EXTRA_MESSAGE_ID, -1);
            if (mMessageId == -1) {
                Log.e(TAG, "ERROR messageId, finish MessageDetailsActivity.");
                finish();
            }
            mIsComefromNotification = getIntent().getBooleanExtra(
                    UI_INTENT_EXTRA_IS_COME_FROM_NOTIFICATION, false);
            mParticipantName = getIntent().getStringExtra(UI_INTENT_EXTRA_PARTICIPANT_NAME);
            mIsHavedStoreContactName = getIntent().getBooleanExtra(
                    UI_INTENT_EXTRA_IS_HAVED_STORE_CONTACT_NAME, false);

        }
    }

    private void bindCursorData() {
        Uri uri = MessagingContentProvider.buildSingleMessageUri(String.valueOf(mMessageId));
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                mData.bind(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "BindData Error:", new Throwable());
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
                cursor = null;
            }
        }

    }

    @Override
    protected void onResume() {
        mIsActivityVisibility = true;
        super.onResume();
    }

    @Override
    protected void onPause() {
        mIsActivityVisibility = false;
        super.onPause();
    }

    private void markAsReadInBugleDatabase() {
        // Update local db
        final ContentValues values = new ContentValues();
        values.put(SortMsgDefinitionCollector.MESSAGE_READ, 1);
        values.put(SortMsgDefinitionCollector.MESSAGE_SEEN, 1); // if they read
                                                                // it, they saw
                                                                // it
        Uri uri = ContentUris.withAppendedId(SortMsgDefinitionCollector.MESSAGE_SPECIFY_UPDATE_URI,
                mMessageId);
        getContentResolver().update(uri, values, null, null);
    }

    private void markAsReadInTelephonyDatabase() {
        // Update telephony db
        final ContentValues values = new ContentValues();
        values.put(SortMsgDefinitionCollector.MESSAGE_READ, 1);
        values.put(SortMsgDefinitionCollector.MESSAGE_SEEN, 1); // if they read
                                                                // it,they saw
                                                                // it
        if (!TextUtils.isEmpty(mSmsMessageUri)) {
            Log.d("tim_V6_db", "mSmsMessageUri=" + mSmsMessageUri);
            getContentResolver().update(Uri.parse(mSmsMessageUri), values, null, null);
        }
    }

    private static Intent getRemoteActivityIntent(String packageName, String className) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, className));
        return intent;
    }

    private static Intent getFolderViewMessagingCommServiceIntent() {
        return getRemoteActivityIntent(MESSAGING_PACKAGE_NAME, MESSAGING_COMMON_SERVICE);
    }

    private void startCommonServiceToCancelNotification() {
        try {
            Intent intent = getFolderViewMessagingCommServiceIntent();
            if (mSortType == SortMsgDefinitionCollector.MSG_BOX_INBOX) {
                intent.putExtra(SortMsgDefinitionCollector.KEY_COMM,
                        SortMsgDefinitionCollector.KEY_SMS_NOTIFICATION_ID);
            } else if (mSortType == SortMsgDefinitionCollector.MSG_BOX_OUTBOX) {
                intent.putExtra(SortMsgDefinitionCollector.KEY_COMM,
                        SortMsgDefinitionCollector.KEY_MSG_SEND_ERROR);
            }
            MessageDetailsActivity.this.startService(intent);
        } catch (Exception e) {
            Log.e("tim_V6_noti", "start service error, action="
                    + SortMsgDefinitionCollector.ACTION_FOLDER_VIEW_MESSAGING_COMM, e);
        }
    }

    private void initResource() {
        try {
            mMessageAttachmentsView = (LinearLayout) findViewById(R.id.message_attachments);
            Log.d("tim_V6_null", "mMessageAttachmentsView=" + mMessageAttachmentsView);
            mMultiAttachmentView = (MultiAttachmentLayout) findViewById(R.id.multiple_attachments);
            mMultiAttachmentView.setOnAttachmentClickListener(this);
            mMessageImageView = (AsyncImageView) findViewById(R.id.message_image);
            mMessageImageView.setOnClickListener(this);
            mMessageImageView.setOnLongClickListener(this);

            mDownloadButton = (Button) findViewById(R.id.btn_download_msg);
            mDownloadButton.setOnClickListener(this);
            mDownloadStatus = (TextView) findViewById(R.id.text_download_status);

            mRecipeintsTextView = (TextView) findViewById(R.id.recipeints);
            mSubjectLabel = (TextView) findViewById(R.id.subject_label);
            mSubjectTextView = (TextView) findViewById(R.id.subject);
            mSubjectDividerView = findViewById(R.id.subject_div);
            mContentTextView = (TextView) findViewById(R.id.content);
            mDateTextView = (TextView) findViewById(R.id.date);
            mSizeTextView = (TextView) findViewById(R.id.size);// add for bug 553054
            mContentTextView.setOnLongClickListener(this);
            mZCButton = (ZoomControls) findViewById(R.id.zoomControls);
            // mTextSize =
            // getResources().getDimension(R.dimen.conversation_message_text_size);
            setTextSizeForZoomControl();
            mZCButton.setOnZoomInClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (mTextSize > MAX_TEXT_SIZE) {
                        mZCButton.setIsZoomInEnabled(false);
                        Toast.makeText(MessageDetailsActivity.this, R.string.max_zoom,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mZCButton.setIsZoomOutEnabled(true);
                    mTextSize = mTextSize + DISTANCE;
                    setTextSizeForZoomControl();
                }
            });

            mZCButton.setOnZoomOutClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mTextSize < MIN_TEXT_SIZE) {
                        mZCButton.setIsZoomOutEnabled(false);
                        Toast.makeText(MessageDetailsActivity.this, R.string.min_zoom,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mZCButton.setIsZoomInEnabled(true);
                    mTextSize = mTextSize - DISTANCE;
                    setTextSizeForZoomControl();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateMessageTransmitStatus() {
        supportInvalidateOptionsMenu();
        int statusResId = -1;
        mDownloadStatus.setVisibility(View.VISIBLE);
        mDownloadButton.setVisibility(View.VISIBLE);
        switch (mData.getStatus()) {
            case MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING:
            case MessageData.BUGLE_STATUS_INCOMING_MANUAL_DOWNLOADING:
            case MessageData.BUGLE_STATUS_INCOMING_RETRYING_AUTO_DOWNLOAD:
            case MessageData.BUGLE_STATUS_INCOMING_RETRYING_MANUAL_DOWNLOAD:
                mDownloadButton.setVisibility(View.GONE);
                statusResId = R.string.message_status_downloading;
                break;

            case MessageData.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD:
            case MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED:
                statusResId = R.string.message_title_manual_download;
                break;

            case MessageData.BUGLE_STATUS_INCOMING_EXPIRED_OR_NOT_AVAILABLE:
                mDownloadButton.setVisibility(View.GONE);
                statusResId = R.string.message_status_download_error;
                break;

            case MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND:
            case MessageData.BUGLE_STATUS_OUTGOING_SENDING:
                mDownloadButton.setVisibility(View.GONE);
                statusResId = R.string.message_status_sending;
                break;

            case MessageData.BUGLE_STATUS_OUTGOING_RESENDING:
            case MessageData.BUGLE_STATUS_OUTGOING_AWAITING_RETRY:
                mDownloadButton.setVisibility(View.GONE);
                statusResId = R.string.message_status_send_retrying;
                break;

            case MessageData.BUGLE_STATUS_OUTGOING_FAILED:
            case MessageData.BUGLE_STATUS_OUTGOING_FAILED_EMERGENCY_NUMBER:
                mDownloadButton.setVisibility(View.GONE);
                if (mOriginalSortType == SortMsgDefinitionCollector.MSG_BOX_OUTBOX) {
                    statusResId = R.string.message_outgoing_failure;
                } else {
                    mDownloadStatus.setVisibility(View.GONE);
                }
                break;

            case MessageData.BUGLE_STATUS_OUTGOING_COMPLETE:
                mDownloadButton.setVisibility(View.GONE);
                if (mOriginalSortType == SortMsgDefinitionCollector.MSG_BOX_OUTBOX) {
                    statusResId = R.string.message_outgoing_successful;
                } else {
                    mDownloadStatus.setVisibility(View.GONE);
                }
                break;

            case MessageData.BUGLE_STATUS_INCOMING_COMPLETE:
//                if (mIsActivityVisibility) {
//                    startCommonServiceToCancelNotification();
//                }
                if (!mIsRead) {
                    markAsReadInBugleDatabase();
                    markAsReadInTelephonyDatabase();
                }
            default:
                mDownloadStatus.setVisibility(View.GONE);
                mDownloadButton.setVisibility(View.GONE);
                break;
        }
        if (statusResId > -1) {
            mDownloadStatus.setText(statusResId);
        }
    }

    private void updateMessageContent(boolean isFirstTime/* first time */) {
        mMessageStatus = mData.getStatus();
        mSortType = SortMsgDefinitionCollector.getSortTypeByStatus(mMessageStatus);
        if (isFirstTime) {
            mOriginalSortType = mSortType;
        }
        mIsSendFailedStatus = getIsSendFailedStatus();
        mConversationId = mData.getConversationId();
        mReceivedTimestamp = mData.getReceivedTimeStamp();
        mProtocol = mData.getProtocol();
        mIsRead = mData.getIsRead();
        mIsSeen = mData.getIsSeen();
        mSmsMessageUri = mData.getSmsMessageUri();
        mMmsSubject = mData.getMmsSubject();
//        mParticipantName = TextUtils.isEmpty(mData.getSenderDisplayName()) ? mParticipantName
//                : mData.getSenderDisplayName();// the view in db, incoming msg
                                               // have sender name, others is
                                               // null.
        mBobyText = mData.getText();
        mHasAttachment = mData.hasAttachments();
        mParticipant = mData.getSenderNormalizedDestination();
        mRecipeintsTextView.setText(mParticipantName);
        if ((mParticipantName != null) && !mIsHavedStoreContactName) {
            Linkify.addLinks(mRecipeintsTextView, Linkify.PHONE_NUMBERS | Linkify.EMAIL_ADDRESSES);
        }
        if (!TextUtils.isEmpty(mMmsSubject)) {
            mSubjectTextView.setText(mMmsSubject);
            mSubjectTextView.setVisibility(View.VISIBLE);
            mSubjectLabel.setVisibility(View.VISIBLE);
            mSubjectDividerView.setVisibility(View.VISIBLE);
            Linkify.addLinks(mSubjectTextView, Linkify.ALL);
        }
        if (!TextUtils.isEmpty(mBobyText)) {
            mContentTextView.setText(mBobyText);
            mContentTextView.setVisibility(View.VISIBLE);
            Linkify.addLinksSprd(mContentTextView, Linkify.ALL);
        }

        mDateTextView.setText(Dates.getMessageTimeString(mReceivedTimestamp));
        // add for bug 553054 begin
        LogUtil.d("MessageDetailsActivity", "updateMessageContent status = " + mData.getStatus());
        if (!mData.getIsSms()) {// fix for Bug 558980
            mSizeTextView.setVisibility(View.VISIBLE);
            final StringBuilder size = new StringBuilder();
            size.append(MessageDetailsActivity.this.getResources()
                    .getString(R.string.message_size_label).trim());
            LogUtil.d("MessageDetailsActivity", "origin size = " + mData.getSmsMessageSize());
            // fox for Bug 558980 begin
            if (mData.getSmsMessageSize() > 0) {
                size.append(Formatter.formatFileSize(MessageDetailsActivity.this,
                        mData.getSmsMessageSize()));
            } else {
                Log.d(TAG, "need count the mms size.");
                size.append(MessageDetailsActivity.this.getResources()
                        .getString(R.string.message_size_counting).trim());
                Message msg = mSubHandler.obtainMessage(EVENT_COUNT_MMS_SIZE);
                mSubHandler.sendMessage(msg);
            }
            // fix for Bug 558980 end
            mSizeTextView.setText(size.toString());
        } else {
            mSizeTextView.setVisibility(View.GONE);
        }
        // add for bug 553054 end
    }

    public final boolean getIsSendFailedStatus() {
        return (mMessageStatus == MessageData.BUGLE_STATUS_OUTGOING_FAILED || mMessageStatus == MessageData.BUGLE_STATUS_OUTGOING_FAILED_EMERGENCY_NUMBER);
    }

    private void hideInput(Context context, View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void setTextSizeForZoomControl() {
        mRecipeintsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSize);
        mSubjectTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSize);
        mContentTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSize);
        mDateTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSize);
        // add for bug 553054 begin
        mSizeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSize);
        // add for bug 553054 end
    }

    @Override
    protected void updateActionBar(ActionBar actionBar) {
        actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(
                R.color.action_bar_background_color)));
        actionBar.setTitle(MessageDetailsActivity.this.getString(R.string.message_details_text));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sort_message_details_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.setGroupVisible(R.id.menu_all, true);
        MenuItem item = menu.findItem(R.id.action_forward);
        if (mSortType != SortMsgDefinitionCollector.MSG_BOX_INBOX) {
            if (!mIsSendFailedStatus) {
                menu.findItem(R.id.action_resend).setVisible(false);
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
            menu.removeItem(R.id.action_reply);
            if (mSortType == SortMsgDefinitionCollector.MSG_BOX_DRAFT) {
                menu.findItem(R.id.action_forward).setVisible(false);
                MenuItem editItem = menu.findItem(R.id.action_edit);
                editItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            } else {
                menu.findItem(R.id.action_edit).setVisible(false);
            }
        } else {
            menu.findItem(R.id.action_resend).setVisible(false);
            // item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }

        // When mms is notification,do not show edit,lock,forward.
        if (mProtocol == SortMsgDefinitionCollector.PROTOCOL_MMS_PUSH_NOTIFICATION) {
            menu.findItem(R.id.action_edit).setVisible(false);
            menu.findItem(R.id.action_forward).setVisible(false);
        } else if (mSortType != SortMsgDefinitionCollector.MSG_BOX_DRAFT) {
            // if (mIsLocked) {
            // menu.add(0, MENU_UNLOCK_MESSAGE, 0, R.string.menu_unlock);
            // } else {
            // menu.add(0, MENU_LOCK_MESSAGE, 0, R.string.menu_lock);
            // }
        }
        if (mHasAttachment) {
            if (menu.findItem(MENU_SAVE_ALL_ATTACHMENTS) == null) {
                menu.add(0, MENU_SAVE_ALL_ATTACHMENTS, 1, R.string.save_all_attachments);
            }
        } else {
            menu.removeItem(MENU_SAVE_ALL_ATTACHMENTS);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    protected void createDeleteMessageDialog(int titleId) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(titleId)
                .setMessage(R.string.delete_message_confirmation_dialog_text)
                .setPositiveButton(R.string.delete_message_confirmation_button,
                        new AlertDialog.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                confirmDeleteMessage();
                            }
                        }).setNegativeButton(android.R.string.cancel, null);
        builder.create().show();

    }

    @Override
    public void onBackPressed() {
        startSortMsgListActivityIfNeeded();
        super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTransmitStatusBroadcastReceiver != null) {
            unregisterReceiver(mTransmitStatusBroadcastReceiver);
        }
        // Add for Bug 558980 begin
        Message msg = mSubHandler.obtainMessage(EVENT_QUIT_WORK_THREAD);
        mSubHandler.sendMessage(msg);
        // Add for Bug 558980 end
    }

    private void startSortMsgListActivityIfNeeded() {
        if (mIsComefromNotification) {
            Intent intent = UIIntents.get().getSortMsgListActivityIntent(
                    MessageDetailsActivity.this);
            if (BuglePrefsKeys.SIMSMS_STATE == BuglePrefs.getApplicationPrefs().getInt(BuglePrefsKeys.IS_FOLDER_VIEW_MODE, BuglePrefsKeys.CONVERSATION_STATE)) {
                intent.putExtra("simsms_bottom", true);
            }
            BuglePrefs.getApplicationPrefs().putInt(BuglePrefsKeys.IS_FOLDER_VIEW_MODE, BuglePrefsKeys.FOLDERVIEW_STATE);
            startActivity(intent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                startSortMsgListActivityIfNeeded();
                finish();
                break;
            case R.id.action_compose_new:
                createNewMessage();
                break;
            case R.id.action_edit:
                editMessage();
                finish();
                break;
            case R.id.action_reply:
                replyMessageUseMessaging();
                break;
            case R.id.action_forward:
                forwardMessage();
                break;
            case R.id.action_delete:
                createDeleteMessageDialog(R.string.delete_message_confirmation_dialog_title);
                break;
            case MENU_LOCK_MESSAGE: {
                return true;
            }

            case MENU_UNLOCK_MESSAGE: {
                return true;
            }

            case MENU_SAVE_ALL_ATTACHMENTS:
                saveAllAttachments();
                break;
            case R.id.action_resend:
                resendMessage();
                break;
            default:
                return true;
        }
        return false;
    }

    private void saveAllAttachments() {
        if (OsUtil.hasStoragePermission()) {
            final SaveAttachmentTask saveAttachmentTask = new SaveAttachmentTask(this);
            for (final MessagePartData part : mData.getAttachments()) {
                saveAttachmentTask.addAttachmentToSave(part.getContentUri(), part.getContentType());
            }
            if (saveAttachmentTask.getAttachmentCount() > 0) {
                saveAttachmentTask.executeOnThreadPool();
            }
        } else {
            this.requestPermissions(new String[] {
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 0);
        }
    }

    /**
     * Create a message to be forwarded from an existing message.
     */
    public MessageData createForwardedMessage(final ConversationMessageData message, boolean isForwarded) {
        final MessageData forwardedMessage = new MessageData();

        final String originalSubject = MmsUtils.cleanseMmsSubject(
                this.getResources(), message.getMmsSubject());
        if (!TextUtils.isEmpty(originalSubject)) {
            if (isForwarded) {
                forwardedMessage.setMmsSubject(this.getResources().getString(
                        R.string.message_fwd, originalSubject));
            } else {
                forwardedMessage.setMmsSubject(originalSubject);
            }
        }

        for (final MessagePartData part : message.getParts()) {
            MessagePartData forwardedPart;

            // Depending on the part type, if it is text, we can directly create
            // a text part;
            // if it is attachment, then we need to create a pending attachment
            // data out of it, so
            // that we may persist the attachment locally in the scratch folder
            // when the user picks
            // a conversation to forward to.
            if (part.isText()) {
                forwardedPart = MessagePartData.createTextMessagePart(part.getText());
            } else {
                final PendingAttachmentData pendingAttachmentData = PendingAttachmentData
                        .createPendingAttachmentData(part.getContentType(), part.getContentUri());
                forwardedPart = pendingAttachmentData;
            }
            forwardedMessage.addPart(forwardedPart);
        }
        return forwardedMessage;
    }

    private void createNewMessage() {
        UIIntents.get().launchConversationActivity(this, null, null);
    }

    private void resendMessage() {
        if (UiUtils.isReadyForAction()) {
            ResendMessageAction.resendMessage(String.valueOf(mMessageId));
        } else {
            warnOfMissingActionConditions(REQUEST_CODE_SET_DEFAULT_SMS_APP_TO_SEND_MSG);
        }
    }

    private void forwardMessage() {
        final MessageData message = createForwardedMessage(mData, true);
        UIIntents.get().launchForwardMessageActivity(this, message);
    }

    private void editMessage() {
        final MessageData message = createForwardedMessage(mData, false);
        UIIntents.get().launchConversationActivity(this, mConversationId, message);
    }

    private void replyMessageUseMessaging() {
        //final MessageData message = createForwardedMessage(mData, false);
        UIIntents.get().launchConversationActivity(this, mConversationId, null);
    }

    private void replyMessageUseDefaultApp() {
        String scheme = SMS_SCHEME;
        try {
            if (SortMsgDefinitionCollector.getIsMms(mProtocol)) {
                scheme = MMS_SCHEME;
            }
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(scheme));
            if (mParticipant != null) {
                intent.putExtra(ADDRESS, mParticipant);
            }
            intent.putExtra(IS_FROM_FOLDER_VIEW_REPLY, true);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "onOptionsItemSelected: can't launch this action: " + Intent.ACTION_SENDTO
                    + " scheme:" + scheme, e);
        }
    }

    private void downloadMessage() {
        if (UiUtils.isReadyForAction()) {
            mDownloadStatus.setVisibility(View.VISIBLE);
            mDownloadButton.setVisibility(View.GONE);
            mDownloadStatus.setText(R.string.message_status_downloading);
            RedownloadMmsAction.redownloadMessage(String.valueOf(mMessageId));
        } else {
            warnOfMissingActionConditions(REQUEST_CODE_SET_DEFAULT_SMS_APP_TO_DOWNLOAD_MSG);
        }
    }

    public void warnOfMissingActionConditions(int code) {
        final PhoneUtils phoneUtils = PhoneUtils.getDefault();
        final boolean isSmsCapable = phoneUtils.isSmsCapable();
        final boolean hasPreferredSmsSim = phoneUtils.getHasPreferredSmsSim();
        final boolean isDefaultSmsApp = phoneUtils.isDefaultSmsApp();

        // Supports SMS?
        if (!isSmsCapable) {
            UiUtils.showToast(R.string.sms_disabled);

            // Has a preferred sim?
        } else if (!hasPreferredSmsSim) {
            UiUtils.showToast(R.string.no_preferred_sim_selected);

            // Is the default sms app?
        } else if (!isDefaultSmsApp) {
            final Intent defSmsIntent = getChangeDefaultSmsAppIntent(MESSAGING_PACKAGE_NAME);
            startActivityForResult(defSmsIntent, code);
        }

        Log.d(TAG, "Unsatisfied action condition: " + "isSmsCapable=" + isSmsCapable + ", "
                + "hasPreferredSmsSim=" + hasPreferredSmsSim + ", " + "isDefaultSmsApp="
                + isDefaultSmsApp);
    }

    private void startCommonServiceToResendMessage() {
        try {
            Intent intent = getFolderViewMessagingCommServiceIntent();
            intent.putExtra(SortMsgDefinitionCollector.KEY_COMM,
                    SortMsgDefinitionCollector.KEY_MESSAGE_RESEND);
            intent.putExtra(UI_INTENT_EXTRA_MESSAGE_ID, mMessageId);
            Log.e("tim_V6_noti", "startCommonServiceToResendMessage");
            MessageDetailsActivity.this.startService(intent);
        } catch (Exception e) {
            Log.e("tim_V6_noti", "start service error, action="
                    + SortMsgDefinitionCollector.ACTION_FOLDER_VIEW_MESSAGING_COMM, e);
        }
    }

    public static Intent getChangeDefaultSmsAppIntent(String packageName) {
        final Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName);
        return intent;
    }

    protected void confirmDeleteMessage() {
        if (!PhoneUtils.getDefault().isDefaultSmsApp()) {
            final Intent defSmsIntent = getChangeDefaultSmsAppIntent(MESSAGING_PACKAGE_NAME);
            startActivityForResult(defSmsIntent, REQUEST_CODE_SET_DEFAULT_SMS_APP_TO_DELETE_MSG);
        } else {
            deleteMessagesFromDatabase();
        }
    }

    private void deleteMessagesFromDatabase() {
        Uri uri = ContentUris.withAppendedId(SortMsgDefinitionCollector.MESSAGE_SPECIFY_DELETE_URI,
                mMessageId);
        getContentResolver().delete(uri, null, null);
        MessageDetailsActivity.this.finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SET_DEFAULT_SMS_APP_TO_DELETE_MSG:
                if (resultCode == RESULT_OK) {
                    deleteMessagesFromDatabase();
                } else {
                    toastDefaultAppIsNeeded();

                }
                break;
            case REQUEST_CODE_SET_DEFAULT_SMS_APP_TO_SEND_MSG:
                if (resultCode == RESULT_OK) {
                    resendMessage();
                } else {
                    toastDefaultAppIsNeeded();
                }
                break;
            case REQUEST_CODE_SET_DEFAULT_SMS_APP_TO_DOWNLOAD_MSG:
                if (resultCode == RESULT_OK) {
                    downloadMessage();
                } else {
                    toastDefaultAppIsNeeded();
                }
                break;
            default:
                break;
        }
    }

    private void toastDefaultAppIsNeeded() {
        String needDef = getString(R.string.need_messaging_is_default_app);
        String string = String.format(needDef, getString(R.string.host_app_name));
        UiUtils.showToastAtBottom(string);
    }

    View.OnClickListener mContextMenuItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent;
            try {
                switch (v.getId()) {
                    case R.id.coptText:
                        intent = new Intent(MessageDetailsActivity.this,
                                CopyTextAcitvityDialog.class);
                        intent.putExtra("Text", mBobyText);
                        MessageDetailsActivity.this.startActivity(intent);
                        break;
                    case R.id.coptMsgToSim:
                        // TODO
                        break;

                    default:
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "showCustomContextMenu onClick ERROR", e);
            }
            mContextDialog.dismiss();
        }
    };

    private void showCustomContextMenu() {
        AlertDialog.Builder builder = new Builder(MessageDetailsActivity.this);
        builder.setTitle(R.string.message_context_menu_title);
        LayoutInflater inflater = LayoutInflater.from(MessageDetailsActivity.this);
        LinearLayout layout = (LinearLayout) inflater.inflate(
                R.layout.message_details_context_menu_layout, null);
        builder.setView(layout);
        TextView coptBodyView = (TextView) layout.findViewById(R.id.coptText);
        TextView coptMsgToSimView = (TextView) layout.findViewById(R.id.coptMsgToSim);
        coptMsgToSimView.setVisibility(View.GONE);// TODO
        coptBodyView.setOnClickListener(mContextMenuItemClickListener);
        coptMsgToSimView.setOnClickListener(mContextMenuItemClickListener);

        if ((mProtocol != SortMsgDefinitionCollector.PROTOCOL_SMS)
                || (mSortType == SortMsgDefinitionCollector.MSG_BOX_DRAFT)
                || (mSortType == SortMsgDefinitionCollector.MSG_BOX_OUTBOX)) {
            coptMsgToSimView.setVisibility(View.GONE);
        }
        mContextDialog = builder.create();
        mContextDialog.show();
    }

    @Override
    public boolean onLongClick(View v) {
        if (!TextUtils.isEmpty(mBobyText)) {
            showCustomContextMenu();
        }
        return true;
    }

    private void bindAttachmentsOfSameType(final Predicate<MessagePartData> attachmentTypeFilter,
            final int attachmentViewLayoutRes, final AttachmentViewBinder viewBinder,
            final Class<?> attachmentViewClass) {
        final LayoutInflater layoutInflater = LayoutInflater.from(this);

        // Iterate through all attachments of a particular type (video, audio,
        // etc).
        // Find the first attachment index that matches the given type if
        // possible.
        int attachmentViewIndex = -1;
        View existingAttachmentView;
        do {
            existingAttachmentView = mMessageAttachmentsView.getChildAt(++attachmentViewIndex);
        } while (existingAttachmentView != null
                && !(attachmentViewClass.isInstance(existingAttachmentView)));

        for (final MessagePartData attachment : mData.getAttachments(attachmentTypeFilter)) {
            View attachmentView = mMessageAttachmentsView.getChildAt(attachmentViewIndex);
            if (!attachmentViewClass.isInstance(attachmentView)) {
                attachmentView = layoutInflater.inflate(attachmentViewLayoutRes,
                        mMessageAttachmentsView, false /* attachToRoot */);
                attachmentView.setOnClickListener(this);
                attachmentView.setOnLongClickListener(this);
                mMessageAttachmentsView.addView(attachmentView, attachmentViewIndex);
            }
            viewBinder.bindView(attachmentView, attachment);
            attachmentView.setTag(attachment);
            attachmentView.setVisibility(View.VISIBLE);
            attachmentViewIndex++;
        }
        // If there are unused views left over, unbind or remove them.
        while (attachmentViewIndex < mMessageAttachmentsView.getChildCount()) {
            final View attachmentView = mMessageAttachmentsView.getChildAt(attachmentViewIndex);
            if (attachmentViewClass.isInstance(attachmentView)) {
                mMessageAttachmentsView.removeViewAt(attachmentViewIndex);
            } else {
                // No more views of this type; we're done.
                break;
            }
        }
    }

    private void updateMessageAttachments() {
        // Bind video, audio, and VCard attachments. If there are multiple, they
        // stack vertically.
        bindAttachmentsOfSameType(sVideoFilter, R.layout.message_details_video_attachment,
                mVideoViewBinder, VideoThumbnailView.class);
        bindAttachmentsOfSameType(sAudioFilter, R.layout.message_audio_attachment,
                mAudioViewBinder, AudioAttachmentView.class);
        bindAttachmentsOfSameType(sVCardFilter, R.layout.message_vcard_attachment,
                mVCardViewBinder, PersonItemView.class);

        // Bind image attachments. If there are multiple, they are shown in a
        // collage view.
        final List<MessagePartData> imageParts = mData.getAttachments(sImageFilter);
        if (imageParts.size() > 1) {
            Collections.sort(imageParts, sImageComparator);
            mMultiAttachmentView.bindAttachments(imageParts, null, imageParts.size());
            mMultiAttachmentView.setVisibility(View.VISIBLE);
        } else {
            mMultiAttachmentView.setVisibility(View.GONE);
        }

        if (imageParts.size() == 1) {
            // Get the display metrics for a hint for how large to pull the
            // image data into
            final WindowManager windowManager = (WindowManager) MessageDetailsActivity.this
                    .getSystemService(Context.WINDOW_SERVICE);
            final DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);

            final int desiredWidth = displayMetrics.widthPixels;

            final MessagePartData imagePart = imageParts.get(0);
            // If the image is big, we want to scale it down to save memory
            // since we're going to
            // scale it down to fit into the bubble width. We don't
            // constrain the height.
            final ImageRequestDescriptor imageRequest = new MessagePartImageRequestDescriptor(
                    imagePart, desiredWidth, MessagePartData.UNSPECIFIED_SIZE, false);
            adjustImageViewBounds(imagePart);
            mMessageImageView.setImageResourceId(imageRequest);
            mMessageImageView.setTag(imagePart);
            mMessageImageView.setVisibility(View.VISIBLE);
        } else {
            mMessageImageView.setImageResourceId(null);
            mMessageImageView.setVisibility(View.GONE);
        }

        // Show the message attachments container if any of its children are
        // visible
        boolean attachmentsVisible = false;
        for (int i = 0, size = mMessageAttachmentsView.getChildCount(); i < size; i++) {
            final View attachmentView = mMessageAttachmentsView.getChildAt(i);
            if (attachmentView.getVisibility() == View.VISIBLE) {
                attachmentsVisible = true;
                break;
            }
        }
        mMessageAttachmentsView.setVisibility(attachmentsVisible ? View.VISIBLE : View.GONE);
    }

    /**
     * If we don't know the size of the image, we want to show it in a
     * fixed-sized frame to avoid janks when the image is loaded and resized.
     * Otherwise, we can set the imageview to take on normal layout params.
     */
    private void adjustImageViewBounds(final MessagePartData imageAttachment) {
        final ViewGroup.LayoutParams layoutParams = mMessageImageView.getLayoutParams();
        // if (imageAttachment.getWidth() == MessagePartData.UNSPECIFIED_SIZE
        // || imageAttachment.getHeight() == MessagePartData.UNSPECIFIED_SIZE) {
        // // We don't know the size of the image attachment, enable
        // // letterboxing on the image
        // // and show a fixed sized attachment. This should happen at most
        // // once per image since
        // // after the image is loaded we then save the image dimensions to
        // // the db so that the
        // // next time we can display the full size.
        // layoutParams.width = getResources().getDimensionPixelSize(
        // R.dimen.image_attachment_fallback_width);
        // layoutParams.height = getResources().getDimensionPixelSize(
        // R.dimen.image_attachment_fallback_height);
        // mMessageImageView.setScaleType(ScaleType.CENTER_CROP);
        // } else {
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        // ScaleType.CENTER_INSIDE and FIT_CENTER behave similarly for most
        // images. However,
        // FIT_CENTER works better for small images as it enlarges the image
        // such that the
        // minimum size ("android:minWidth" etc) is honored.
        mMessageImageView.setScaleType(ScaleType.FIT_CENTER);
        // }
    }

    @Override
    public void onClick(final View view) {
        final Object tag = view.getTag();
        if (tag instanceof MessagePartData) {
            final Rect bounds = UiUtils.getMeasuredBoundsOnScreen(view);
            onAttachmentClick((MessagePartData) tag, bounds, false /* longPress */);
        } else if (view.getId() == R.id.btn_download_msg) {
            downloadMessage();
        }
    }

    // Sort photos in MultiAttachLayout in the same order as the
    // ConversationImagePartsView
    static final Comparator<MessagePartData> sImageComparator = new Comparator<MessagePartData>() {
        @Override
        public int compare(final MessagePartData x, final MessagePartData y) {
            return x.getPartId().compareTo(y.getPartId());
        }
    };
    static final Predicate<MessagePartData> sVideoFilter = new Predicate<MessagePartData>() {
        @Override
        public boolean apply(final MessagePartData part) {
            return part.isVideo();
        }
    };

    static final Predicate<MessagePartData> sAudioFilter = new Predicate<MessagePartData>() {
        @Override
        public boolean apply(final MessagePartData part) {
            return part.isAudio();
        }
    };

    static final Predicate<MessagePartData> sVCardFilter = new Predicate<MessagePartData>() {
        @Override
        public boolean apply(final MessagePartData part) {
            return part.isVCard();
        }
    };

    static final Predicate<MessagePartData> sImageFilter = new Predicate<MessagePartData>() {
        @Override
        public boolean apply(final MessagePartData part) {
            return part.isImage();
        }
    };

    interface AttachmentViewBinder {
        void bindView(View view, MessagePartData attachment);

        void unbind(View view);
    }

    final AttachmentViewBinder mVideoViewBinder = new AttachmentViewBinder() {
        @Override
        public void bindView(final View view, final MessagePartData attachment) {
            ((VideoThumbnailView) view).setSource(attachment, mData.getIsIncoming());
        }

        @Override
        public void unbind(final View view) {
            ((VideoThumbnailView) view).setSource((Uri) null, mData.getIsIncoming());
        }
    };

    final AttachmentViewBinder mAudioViewBinder = new AttachmentViewBinder() {
        @Override
        public void bindView(final View view, final MessagePartData attachment) {
            final AudioAttachmentView audioView = (AudioAttachmentView) view;
            audioView.bindMessagePartData(attachment, mData.getIsIncoming(), false);
            audioView.setBackground(ConversationDrawables.get().getBubbleDrawable(false,
                    mData.getIsIncoming(), false /* needArrow */, mData.hasIncomingErrorStatus()));
        }

        @Override
        public void unbind(final View view) {
            ((AudioAttachmentView) view).bindMessagePartData(null, mData.getIsIncoming(), false);
        }
    };

    final AttachmentViewBinder mVCardViewBinder = new AttachmentViewBinder() {
        @Override
        public void bindView(final View view, final MessagePartData attachment) {
            final PersonItemView personView = (PersonItemView) view;
            personView.bind(DataModel.get().createVCardContactItemData(MessageDetailsActivity.this,
                    attachment));
            personView.setBackground(ConversationDrawables.get().getBubbleDrawable(false,
                    mData.getIsIncoming(), false /* needArrow */, mData.hasIncomingErrorStatus()));
            final int nameTextColorRes;
            final int detailsTextColorRes;
            if (false) {
                nameTextColorRes = R.color.message_text_color_incoming;
                detailsTextColorRes = R.color.message_text_color_incoming;
            } else {
                nameTextColorRes = mData.getIsIncoming() ? R.color.message_text_color_incoming
                        : R.color.message_text_color_outgoing;
                detailsTextColorRes = mData.getIsIncoming() ? R.color.timestamp_text_incoming
                        : R.color.timestamp_text_outgoing;
            }
            personView.setNameTextColor(getResources().getColor(nameTextColorRes));
            personView.setDetailsTextColor(getResources().getColor(detailsTextColorRes));
        }

        @Override
        public void unbind(final View view) {
            ((PersonItemView) view).bind(null);
        }
    };

    @Override
    public boolean onAttachmentClick(MessagePartData attachment, Rect viewBoundsOnScreen,
            boolean longPress) {
        if (!longPress) {
            if (attachment.isImage()) {
                displayPhoto(attachment.getContentUri(), viewBoundsOnScreen);
            }

            if (attachment.isVCard()) {
                UIIntents.get().launchVCardDetailActivity(MessageDetailsActivity.this,
                        attachment.getContentUri());
            }
        }
        return false;
    }

    public void displayPhoto(final Uri photoUri, final Rect imageBounds) {
        displayPhoto(photoUri, imageBounds, mConversationId);
    }

    public void displayPhoto(final Uri photoUri, final Rect imageBounds, final String conversationId) {
        final Uri imagesUri = (mSortType == SortMsgDefinitionCollector.MSG_BOX_DRAFT) ? MessagingContentProvider
                .buildDraftImagesUri(conversationId) : MessagingContentProvider
                .buildConversationImagesUri(conversationId);
        UIIntents.get().launchFullScreenPhotoViewer(MessageDetailsActivity.this, photoUri,
                imageBounds, imagesUri);
    }
}
