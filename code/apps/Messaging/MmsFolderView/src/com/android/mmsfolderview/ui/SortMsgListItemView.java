package com.android.mmsfolderview.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.android.mmsfolderview.data.SortMsgDataCollector;
import com.android.mmsfolderview.data.SortMsgListItemData;
import com.android.mmsfolderview.data.media.UriImageRequestDescriptor;
import com.android.mmsfolderview.util.ContentType;
import com.android.mmsfolderview.util.ImageUtils;
import com.android.mmsfolderview.util.MmsUtils;
import com.android.mmsfolderview.util.OsUtil;
import com.android.mmsfolderview.util.PhoneUtils;
import com.android.mmsfolderview.util.Typefaces;
import com.android.mmsfolderview.util.IntentUiUtils;
import com.android.mmsfolderview.util.UriUtil;
import com.android.mmsfolderview.util.SystemProperties;

import android.support.v4.text.BidiFormatter;
import android.support.v4.text.TextDirectionHeuristicsCompat;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.View.OnLongClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import com.android.mmsfolderview.R;
//fix for bug 534475 begin
import android.content.IntentFilter;
//fix for bug 534475 end

public class SortMsgListItemView extends FrameLayout implements OnClickListener,
        OnLongClickListener, OnLayoutChangeListener {

    static final int UNREAD_SNIPPET_LINE_COUNT = 3;
    static final int NO_UNREAD_SNIPPET_LINE_COUNT = 1;
    private static final String TAG = "SortMsgListItemView";
    public static final String ACTION_CALL_PRIVILEGED = "android.intent.action.CALL_PRIVILEGED";

    private int mListItemReadColor;
    private int mListItemUnreadColor;
    private Typeface mListItemReadTypeface;
    private Typeface mListItemUnreadTypeface;
    private static String sPlusOneString;
    private static String sPlusNString;
    private String mCallBackNumber;
    private AlertDialog mContextDialog;
    private final SortMsgListItemData mData;

    private int mAnimatingCount;
    private ViewGroup mSwipeableContainer;
    private ViewGroup mCrossSwipeBackground;
    private ViewGroup mSwipeableContent;
    private TextView mConversationNameView;
    private TextView mSnippetTextView;
    private TextView mSubjectTextView;
    private TextView mTimestampTextView;
    private TextView mSimNameTextView;
    private ContactIconView mContactIconView;
    private ImageView mContactCheckmarkView;
    private ImageView mNotificationBellView;
    private ImageView mFailedStatusIconView;
    private ImageView mCrossSwipeArchiveLeftImageView;
    private ImageView mCrossSwipeArchiveRightImageView;
    private AsyncImageView mImagePreviewView;
    private AudioAttachmentView mAudioAttachmentView;
    private HostInterface mHostInterface;

    private int firstSlotSubId = -1;
    private int secondSlotSubId = -1;
    private int thirdSlotSubId = -1;

    public SortMsgListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mData = new SortMsgListItemData();
        final Resources res = context.getResources();
    }

    public interface HostInterface {
        boolean isMessageSelected(final int messageId);

        void onMessageClicked(final SortMsgListItemData sortMsgListItemData, boolean isLongClick,
                final SortMsgListItemView sortMsgListItemView);

        boolean isSwipeAnimatable();

        void startFullScreenPhotoViewer(final Uri photosUri,final String type);//Modify for Bug:534532

        void startFullScreenVideoViewer(final Uri videoUri);

        boolean isSelectionMode();

        boolean isInActionMode();

        void deleteMessages(ArrayList<Integer> messagesId, boolean isFromContextMenu);
        // add for bug 532539 begin
        public boolean isEnableVoLte();
        // add for bug 532539 end
        void copySimSmsToPhone(int subId, String bobyText,
                               long receivedTimestamp, int messageStatus,
                               boolean isRead, String address);

        void copySmsToSim(int messageId, int phoneId);

        void deleteSimSms(int indexOnIcc, int subId);
    }

    private final OnClickListener fullScreenPreviewClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            Log.d(TAG, "onClick  ----");
            final String previewType = mData.getContentType();
            if (!(ContentType.isImageType(previewType)) && !(ContentType.isVideoType(previewType)))
                return;
            Log.d(TAG, "onClick  ----2");
            final Uri previewUri = mData.getMultiMediaUri();
            if (ContentType.isImageType(previewType)) {
                /*Modify by SPRD for Bug:534532  2016.03.09 Start */
                mHostInterface.startFullScreenPhotoViewer(previewUri,previewType);
                /*Modify by SPRD for Bug:534532  2016.03.09 End */
            } else {
                Log.d(TAG, "onClick  previewType=" + previewUri);
                mHostInterface.startFullScreenVideoViewer(previewUri);
            }
        }
    };

    public void bind(final Cursor cursor, final HostInterface hostInterface) {
        mHostInterface = hostInterface;
        mData.bind(cursor);

        resetAnimatingState();

        mSwipeableContainer.setOnClickListener(this);
        mSwipeableContainer.setOnLongClickListener(this);

        final Resources resources = getContext().getResources();
        int color;
        final int maxLines;
        final Typeface typeface;
        final int typefaceStyle = mData.getIsDrft() ? Typeface.ITALIC : Typeface.NORMAL;
        final String snippetText = getSnippetText();
        if (mData.getIsRead() || mData.getIsDrft()) {
            maxLines = TextUtils.isEmpty(snippetText) ? 0 : NO_UNREAD_SNIPPET_LINE_COUNT;
            color = mListItemReadColor;
            typeface = mListItemReadTypeface;
        } else {
            maxLines = TextUtils.isEmpty(snippetText) ? 0 : UNREAD_SNIPPET_LINE_COUNT;
            color = mListItemUnreadColor;
            typeface = mListItemUnreadTypeface;
        }

        if (mData.getIsMmsNoti()) {
            color = resources.getColor(R.color.primary_color);
        }

        if (!SortDisplayController.getInstance().isSimSms()){
            mSnippetTextView.setMaxLines(maxLines);
        }

        mSnippetTextView.setTextColor(color);
        mSnippetTextView.setTypeface(typeface, typefaceStyle);
        mSubjectTextView.setTextColor(color);
        mSubjectTextView.setTypeface(typeface, typefaceStyle);

        setSnippet();
        setMessageName();
        setSubject();

        final boolean isMessagingDefaultSmsApp = PhoneUtils.isMessagingDefaultSmsApp(getContext());
        // don't show the error state unless we're the default sms app
        if (mData.getIsFailedStatus() && isMessagingDefaultSmsApp) {
            mTimestampTextView.setTextColor(resources.getColor(R.color.conversation_list_error));
            mTimestampTextView.setTypeface(mListItemReadTypeface, typefaceStyle);
            int failureMessageId = R.string.message_status_download_failed;
            /*Add by SPRD for bug550315  2016.04.12 Start*/
            if (mData.getIsMessageTypeOutgoing()) {
                failureMessageId = MmsUtils.mapRawStatusToErrorResourceId(mData.getMessageStatus(),
                        mData.getRawStatus());
            }
            /*Add by SPRD for bug550315  2016.04.12 End*/
            mTimestampTextView.setText(resources.getString(failureMessageId));
            // } else if (mData.getIsDrft()
            // || mData.getMessageStatus() ==
            // SortMsgDataCollector.BUGLE_STATUS_UNKNOWN) {
            // mTimestampTextView.setTextColor(mListItemReadColor);
            // mTimestampTextView.setTypeface(mListItemReadTypeface,
            // typefaceStyle);
            // mTimestampTextView.setText(resources.getString(R.string.draft_message));
        } else {
            mTimestampTextView.setTextColor(mListItemReadColor);
            mTimestampTextView.setTypeface(mListItemReadTypeface, typefaceStyle);
            final String formattedTimestamp = mData.getFormattedTimestamp(getContext());
            if (mData.getIsSendRequested()) {
                mTimestampTextView.setText(R.string.message_status_sending);
            } else {
                mTimestampTextView.setText(formattedTimestamp);
                mTimestampTextView.append(" ");
            }
        }
        // sim card indicator
        final boolean simNameVisible = mData.isActiveSubscription();
        if (simNameVisible && !mData.getIsDrft()) {
            String simNameText = mData.getSubscriptionName();
            final String displayName = TextUtils.isEmpty(simNameText) ? getContext().getString(
                    R.string.sim_slot_identifier, mData.getDisplaySlotId()) : simNameText;
            mSimNameTextView.setText(displayName);
            mSimNameTextView.setTextColor(mData.getSubscriptionColor());
            mSimNameTextView.setVisibility(VISIBLE);
        } else if (mData.getIsDrft()) {
            mSimNameTextView.setTextColor(resources.getColor(R.color.conversation_list_error));
            mSimNameTextView.setTypeface(mListItemReadTypeface, typefaceStyle);
            mSimNameTextView.setText(resources.getString(R.string.draft_message));
            mSimNameTextView.setVisibility(VISIBLE);
        } else {
            mSimNameTextView.setText(null);
            mSimNameTextView.setVisibility(GONE);
        }
        // select mode: TODO
        final boolean isSelected = mHostInterface.isMessageSelected(mData.getMessageId());
        setSelected(isSelected);
        mContactIconView.setImageResource(mData.getMessageStatus(), mData.getMessageProtocol(),
                mData.getIsRead());
        int contactIconVisibility = GONE;
        int checkmarkVisiblity = GONE;
        int failStatusVisiblity = GONE;

        if (isSelected) {
            checkmarkVisiblity = VISIBLE;
        } else {
            contactIconVisibility = VISIBLE;
            if (mData.getIsFailedStatus()) {
                failStatusVisiblity = VISIBLE;
            }
        }
        mFailedStatusIconView.setVisibility(failStatusVisiblity);
        mContactIconView.setVisibility(contactIconVisibility);
        mContactCheckmarkView.setVisibility(checkmarkVisiblity);
        // preview image
        final Uri previewUri = mData.getMultiMediaUri();
        final String previewContentType = mData.getContentType();
        OnClickListener previewClickListener = null;
        Uri previewImageUri = null;
        int previewImageVisibility = GONE;
        int audioPreviewVisiblity = GONE;
        if (previewUri != null && !TextUtils.isEmpty(previewContentType)) {
            Log.d("tim_V6", "previewContentType:" + previewContentType);
            if (ContentType.isAudioType(previewContentType)) {
                mAudioAttachmentView.bind(previewUri, false);
                audioPreviewVisiblity = VISIBLE;
            } else if (ContentType.isVideoType(previewContentType)) {
                previewImageUri = UriUtil.getUriForResourceId(getContext(),
                        R.drawable.ic_preview_play);
                previewClickListener = fullScreenPreviewClickListener;
                previewImageVisibility = VISIBLE;
                Log.d("tim_V6", "isVideoType-previewImageUri=" + previewImageUri);
            } else if (ContentType.isImageType(previewContentType)) {
                previewImageUri = previewUri;
                previewClickListener = fullScreenPreviewClickListener;
                previewImageVisibility = VISIBLE;
            }

            final int imageSize = resources
                    .getDimensionPixelSize(R.dimen.conversation_list_image_preview_size);
            mImagePreviewView.setImageResourceId(new UriImageRequestDescriptor(previewImageUri,
                    imageSize, imageSize, true /* allowCompression */, false /* isStatic */,
                    false /* cropToCircle */,
                    ImageUtils.DEFAULT_CIRCLE_BACKGROUND_COLOR /* circleBackgroundColor */,
                    ImageUtils.DEFAULT_CIRCLE_STROKE_COLOR /* circleStrokeColor */));
            mImagePreviewView.setOnLongClickListener(this);
            mImagePreviewView.setVisibility(previewImageVisibility);
            mImagePreviewView.setOnClickListener(previewClickListener);
            mAudioAttachmentView.setVisibility(audioPreviewVisiblity);
        }
    }

    private void setSubject() {
        final String subjectText = mData.getIsDrft() ? mData.getMmsSubject() : MmsUtils
                .cleanseMmsSubject(getContext().getResources(), mData.getMmsSubject());
        if (!TextUtils.isEmpty(subjectText)) {
            final String subjectPrepend = getResources().getString(R.string.subject_label);
            mSubjectTextView.setText(TextUtils.concat(subjectPrepend, subjectText));
            mSubjectTextView.setVisibility(VISIBLE);
        } else {
            mSubjectTextView.setVisibility(GONE);
        }
    }

    private void setMessageName() {
        if (mData.getIsRead() || mData.getIsDrft()) {
            mConversationNameView.setTextColor(mListItemReadColor);
            mConversationNameView.setTypeface(mListItemReadTypeface);
        } else {
            mConversationNameView.setTextColor(mListItemUnreadColor);
            mConversationNameView.setTypeface(mListItemUnreadTypeface);
        }

        final String conversationName = mData.getParticipantName();

        final CharSequence ellipsizedName = IntentUiUtils.commaEllipsize(conversationName,
                mConversationNameView.getPaint(), mConversationNameView.getMeasuredWidth(),
                getPlusOneString(), getPlusNString());
        // RTL : To format conversation name if it happens to be phone number.
        final BidiFormatter bidiFormatter = BidiFormatter.getInstance();
        final String bidiFormattedName = bidiFormatter.unicodeWrap(ellipsizedName.toString(),
                TextDirectionHeuristicsCompat.LTR);

        mConversationNameView.setText(bidiFormattedName);
        if (SortDisplayController.getInstance().isSimSms()) {
            Linkify.addLinks(mConversationNameView, Linkify.PHONE_NUMBERS | Linkify.EMAIL_ADDRESSES);
        }
    }

    private void setSnippet() {
        mSnippetTextView.setText(getSnippetText());
        if (SortDisplayController.getInstance().isSimSms()){

            Linkify.addLinks(mSnippetTextView, Linkify.ALL);
        }
    }

    private String getPlusOneString() {
        if (sPlusOneString == null) {
            sPlusOneString = getContext().getResources().getString(R.string.plus_one);
        }
        return sPlusOneString;
    }

    private String getPlusNString() {
        if (sPlusNString == null) {
            sPlusNString = getContext().getResources().getString(R.string.plus_n);
        }
        return sPlusNString;
    }

    private void setShortAndLongClickable(final boolean clickable) {
        setClickable(clickable);
        setLongClickable(clickable);
    }

    private void resetAnimatingState() {
        mAnimatingCount = 0;
        setShortAndLongClickable(true);
        setSwipeTranslationX(0);
    }

    private boolean processClick(final View v, final boolean isLongClick) {
        if (v == mSwipeableContainer && mHostInterface != null) {
            mHostInterface.onMessageClicked(mData, isLongClick, this);
            return true;
        }
        return false;
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
            int oldTop, int oldRight, int oldBottom) {
        if (v == mConversationNameView) {
            setMessageName();
        } else if (v == mSnippetTextView) {
            setSnippet();
        } else if (v == mSubjectTextView) {
            setSubject();
        }
    }

    View.OnClickListener mContextMenuItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent;
            try {
                int messageId = mData.getMessageId();
                switch (v.getId()) {
                    case R.id.addToContact:
                        intent = IntentUiUtils.getAddToContactsIntent(mCallBackNumber);
                        getContext().startActivity(intent);
                        break;
                    case R.id.voiceCall:
                        intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + mCallBackNumber));
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        getContext().startActivity(intent);
                        break;
                    case R.id.editBeforeCall:
                        intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel",
                                mCallBackNumber, null));
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        getContext().startActivity(intent);
                        break;
                    case R.id.videoCall:
                        // add for bug 527156 begin
                        intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"
                                + mCallBackNumber));
//                        intent.putExtra("android.phone.extra.IS_VIDEOCALL", true);
//                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        intent.putExtra(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
                                VideoProfile.STATE_BIDIRECTIONAL);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        // add for bug 527156 end
                        getContext().startActivity(intent);
                        break;
                    case R.id.deleteMessage:
                        ArrayList<Integer> messagesId = new ArrayList<Integer>();
                        messagesId.add(mData.getMessageId());
                        mHostInterface.deleteMessages(messagesId, true);
                        break;
                    case R.id.copySimSmsToPhone:
                        mHostInterface.copySimSmsToPhone(mData.getSubId(),
                                mData.getBobyText(),
                                mData.getReceivedTimestamp(),
                                mData.getMessageStatus(),
                                mData.getIsRead(),
                                mData.getDisplayDestination());
                        break;
                    case R.id.copySmsToFirstSim:
                        mHostInterface.copySmsToSim(messageId, firstSlotSubId);
                        break;
                    case R.id.copySmsToSecondSim:
                        mHostInterface.copySmsToSim(messageId, secondSlotSubId);
                        break;
                    case R.id.copySmsToThirdSim:
                        mHostInterface.copySmsToSim(messageId, thirdSlotSubId);
                        break;
                    case R.id.delSimSms:
                        int subId = mData.getSubId();
                        mHostInterface.deleteSimSms(messageId, subId);
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
        AlertDialog.Builder builder = new Builder(getContext());
        builder.setTitle(R.string.message_context_menu_title);
        Context context = getContext();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        LinearLayout layout = (LinearLayout) inflater.inflate(
                R.layout.sort_msg_list_context_menu_layout, null);
        builder.setView(layout);
        TextView addToContactView = (TextView) layout.findViewById(R.id.addToContact);
        TextView voiceCallView = (TextView) layout.findViewById(R.id.voiceCall);
        TextView editBeforeCallView = (TextView) layout.findViewById(R.id.editBeforeCall);
        TextView videoCallView = (TextView) layout.findViewById(R.id.videoCall);
        TextView deleteMessageView = (TextView) layout.findViewById(R.id.deleteMessage);
        TextView copySimSmsToPhoneView = (TextView) layout.findViewById(R.id.copySimSmsToPhone);
        TextView delSimSmsView = (TextView) layout.findViewById(R.id.delSimSms);
        TextView copySmsToFirstSim = (TextView) layout.findViewById(R.id.copySmsToFirstSim);
        TextView copySmsToSecondSim = (TextView) layout.findViewById(R.id.copySmsToSecondSim);
        TextView copySmsToThirdSim = (TextView) layout.findViewById(R.id.copySmsToThirdSim);

        addToContactView.setOnClickListener(mContextMenuItemClickListener);
        voiceCallView.setOnClickListener(mContextMenuItemClickListener);
        editBeforeCallView.setOnClickListener(mContextMenuItemClickListener);
        videoCallView.setOnClickListener(mContextMenuItemClickListener);
        deleteMessageView.setOnClickListener(mContextMenuItemClickListener);
        if (SortDisplayController.getInstance().isSimSms()) {
            copySimSmsToPhoneView.setOnClickListener(mContextMenuItemClickListener);

            delSimSmsView.setVisibility(VISIBLE);
            delSimSmsView.setOnClickListener(mContextMenuItemClickListener);

            addToContactView.setVisibility(View.GONE);
            voiceCallView.setVisibility(View.GONE);
            editBeforeCallView.setVisibility(View.GONE);
            videoCallView.setVisibility(View.GONE);
            deleteMessageView.setVisibility(View.GONE);
        } else {
            copySimSmsToPhoneView.setVisibility(View.GONE);

            PhoneUtils.PhoneUtilsLMR1 phoneUtilsLMR1 = new PhoneUtils.PhoneUtilsLMR1(context);
            final List<SubscriptionInfo> infoList = phoneUtilsLMR1.getActiveSubscriptionInfoList();
            if (infoList != null && infoList.size() > 0 && canStoreToSim()) {
                for (SubscriptionInfo info : infoList){
                    if (info.getSimSlotIndex() == 0) {
                        copySmsToFirstSim.setVisibility(VISIBLE);
                        firstSlotSubId = info.getSubscriptionId();
                        String displayName = context.getString(R.string.copy_sms_to_sim);
                        String simNameText = info.getDisplayName().toString();
                        displayName += TextUtils.isEmpty(simNameText) ? context
                                .getString(R.string.sim_slot_identifier,
                                        info.getSimSlotIndex() + 1) : simNameText;
                        copySmsToFirstSim.setText(displayName);
                        copySmsToFirstSim.setOnClickListener(mContextMenuItemClickListener);
                    }

                    if (info.getSimSlotIndex() == 1) {
                        copySmsToSecondSim.setVisibility(VISIBLE);
                        secondSlotSubId = info.getSubscriptionId();
                        String displayName = context.getString(R.string.copy_sms_to_sim);
                        String simNameText = info.getDisplayName().toString();
                        displayName += TextUtils.isEmpty(simNameText) ? context
                                .getString(R.string.sim_slot_identifier,
                                        info.getSimSlotIndex() + 1) : simNameText;
                        copySmsToSecondSim.setText(displayName);
                        copySmsToSecondSim.setOnClickListener(mContextMenuItemClickListener);
                    }

                    if (info.getSimSlotIndex() == 2) {
                        copySmsToThirdSim.setVisibility(VISIBLE);
                        thirdSlotSubId = info.getSubscriptionId();
                        String displayName = context.getString(R.string.copy_sms_to_sim);
                        String simNameText = info.getDisplayName().toString();
                        displayName += TextUtils.isEmpty(simNameText) ? context
                                .getString(R.string.sim_slot_identifier,
                                        info.getSimSlotIndex() + 1) : simNameText;
                        copySmsToThirdSim.setText(displayName);
                        copySmsToThirdSim.setOnClickListener(mContextMenuItemClickListener);
                    }
                }
            }

            if (mData.getIsHavedStoreContactName()) {
                addToContactView.setEnabled(false);
                addToContactView.setText(getContext().getString(R.string.has_add_to_contacts));
            }
            if (mData.getParticipantCount() != 1) {
                addToContactView.setEnabled(false);
                // fix for bug 527191 before
                voiceCallView.setEnabled(false);
                editBeforeCallView.setEnabled(false);
                videoCallView.setEnabled(false);
                // fix for bug 527191 end
            }
            // fix for bug 534680 begin
            mCallBackNumber = mData.getParticipantDestination();
            // fix for bug 534680 end
            if (mData.getIsSms() || mData.getIsMms() && (mData.getParticipantCount() == 1)) {
                String isSupportVt = SystemProperties.get("persist.sys.support.vt");
                Log.e(TAG, "------------isSupportVt=" + isSupportVt);
                //mCallBackNumber = mData.getParticipantDestination();
                if (!MmsUtils.isEmailAddress(mCallBackNumber)) {
                    // fix for bug 534475 begin
                    if (!("true".equals(isSupportVt) && mHostInterface.isEnableVoLte()  && !isRelianceBoardInBattaryLow())) {
                    // fix for bug 534475 end
                        videoCallView.setVisibility(View.GONE);
                    }
                } else {
                    voiceCallView.setVisibility(View.GONE);
                    editBeforeCallView.setVisibility(View.GONE);
                    videoCallView.setVisibility(View.GONE);
                }
            }
        }
        mContextDialog = builder.create();
        mContextDialog.show();
    }

    private boolean canStoreToSim() {
        return SortDisplayController.isCMCC() && mData.getIsSms() &&
               (mData.getSortType() == SortMsgDataCollector.MSG_BOX_INBOX ||
                mData.getSortType() == SortMsgDataCollector.MSG_BOX_SENT);
    }

    // fix for bug 534475 begin
    private boolean isRelianceBoardInBattaryLow() {
        boolean isRelianceBoard = "reliance".equals(SystemProperties.get("ro.operator.volte"));
        Log.d(TAG, "isRelianceBoard=" + isRelianceBoard);
        // if (isRelianceBoard) {
        Intent batteryInfoIntent = getContext().registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryInfoIntent.getIntExtra("level", 0);
        int total = batteryInfoIntent.getIntExtra("scale", 0);
        int current = level * 100 / total;
        Log.d(TAG, "Current Battary: " + current);
        if (current < 15) {
            Log.d(TAG, "isRelianceBoardInBattaryLow: Battery < 15%");
            return true;
        } else {
            Log.d(TAG, "isRelianceBoardInBattaryLow: Battery > 15%");
            return false;
        }
        // }
    }
    // fix for bug 534475 end

    @Override
    public boolean onLongClick(View v) {
        if (!mHostInterface.isInActionMode()) {
            showCustomContextMenu();
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if (SortDisplayController.getInstance().isSimSms()){
            return;
        }
        processClick(v, false);
    }

    @Override
    protected void onFinishInflate() {
        mSwipeableContainer = (ViewGroup) findViewById(R.id.swipeableContainer);
        mCrossSwipeBackground = (ViewGroup) findViewById(R.id.crossSwipeBackground);
        mSwipeableContent = (ViewGroup) findViewById(R.id.swipeableContent);
        mConversationNameView = (TextView) findViewById(R.id.conversation_name);
        mSnippetTextView = (TextView) findViewById(R.id.conversation_snippet);
        mSubjectTextView = (TextView) findViewById(R.id.conversation_subject);
        mTimestampTextView = (TextView) findViewById(R.id.conversation_timestamp);
        mSimNameTextView = (TextView) findViewById(R.id.sim_name);
        mContactIconView = (ContactIconView) findViewById(R.id.conversation_icon);
        mContactCheckmarkView = (ImageView) findViewById(R.id.conversation_checkmark);
        mNotificationBellView = (ImageView) findViewById(R.id.conversation_notification_bell);
        mFailedStatusIconView = (ImageView) findViewById(R.id.conversation_failed_status_icon);
        mCrossSwipeArchiveLeftImageView = (ImageView) findViewById(R.id.crossSwipeArchiveIconLeft);
        mCrossSwipeArchiveRightImageView = (ImageView) findViewById(R.id.crossSwipeArchiveIconRight);
        mImagePreviewView = (AsyncImageView) findViewById(R.id.conversation_image_preview);
        mAudioAttachmentView = (AudioAttachmentView) findViewById(R.id.audio_attachment_view);
        mConversationNameView.addOnLayoutChangeListener(this);
        mSnippetTextView.addOnLayoutChangeListener(this);

        final Resources resources = getContext().getResources();
        mListItemReadColor = resources.getColor(R.color.conversation_list_item_read);
        mListItemUnreadColor = resources.getColor(R.color.conversation_list_item_unread);

        mListItemReadTypeface = Typefaces.getRobotoNormal();
        mListItemUnreadTypeface = Typefaces.getRobotoBold();

        if (OsUtil.isAtLeastL()) {
            setTransitionGroup(true);
        }
        if (SortDisplayController.getInstance().isSimSms()){
            mSnippetTextView.setEllipsize(null);
        }
    }

    private String getSnippetText() {
        String snippetText = mData.getBobyText();
        final String previewContentType = mData.getContentType();
        if (TextUtils.isEmpty(snippetText)) {
            Resources resources = getResources();
            // Use the attachment type as a snippet so the preview doesn't look
            // odd
            if (ContentType.isAudioType(previewContentType)) {
                snippetText = resources.getString(R.string.conversation_list_snippet_audio_clip);
            } else if (ContentType.isImageType(previewContentType)) {
                snippetText = resources.getString(R.string.conversation_list_snippet_picture);
            } else if (ContentType.isVideoType(previewContentType)) {
                snippetText = resources.getString(R.string.conversation_list_snippet_video);
            } else if (ContentType.isVCardType(previewContentType)) {
                snippetText = resources.getString(R.string.conversation_list_snippet_vcard);
            } else if (mData.getIsMmsNoti()) {
                snippetText = resources.getString(R.string.message_title_manual_download);
                if ((mData.getMessageStatus() == SortMsgDataCollector.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING)
                        || (mData.getMessageStatus() == SortMsgDataCollector.BUGLE_STATUS_INCOMING_MANUAL_DOWNLOADING)
                        || (mData.getMessageStatus() == SortMsgDataCollector.BUGLE_STATUS_INCOMING_RETRYING_AUTO_DOWNLOAD)
                        || (mData.getMessageStatus() == SortMsgDataCollector.BUGLE_STATUS_INCOMING_RETRYING_MANUAL_DOWNLOAD)) {
                    snippetText = resources.getString(R.string.message_status_downloading);
                }
            }
        }
        return snippetText;
    }

    public boolean isSwipeAnimatable() {
        // TODO Auto-generated method stub
        return false;
    }

    public void setSwipeTranslationX(float translationX) {
        mSwipeableContainer.setTranslationX(translationX);
        if (translationX == 0) {
            mCrossSwipeBackground.setVisibility(View.GONE);
            mCrossSwipeArchiveLeftImageView.setVisibility(GONE);
            mCrossSwipeArchiveRightImageView.setVisibility(GONE);

            mSwipeableContainer.setBackgroundColor(Color.TRANSPARENT);
        } else {
            mCrossSwipeBackground.setVisibility(View.VISIBLE);
            if (translationX > 0) {
                mCrossSwipeArchiveLeftImageView.setVisibility(VISIBLE);
                mCrossSwipeArchiveRightImageView.setVisibility(GONE);
            } else {
                mCrossSwipeArchiveLeftImageView.setVisibility(GONE);
                mCrossSwipeArchiveRightImageView.setVisibility(VISIBLE);
            }
            mSwipeableContainer.setBackgroundResource(R.drawable.swipe_shadow_drag);
        }
    }

    public float getSwipeTranslationX() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setAnimating(boolean b) {
        // TODO Auto-generated method stub

    }

    public void onSwipeComplete() {
        // TODO Auto-generated method stub

    }

    public boolean isAnimating() {
        // TODO Auto-generated method stub
        return false;
    }
}
