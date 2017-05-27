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

package com.android.mmsfolderview.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.Mms;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.List;

import com.android.mmsfolderview.data.SortMsgDataCollector;
import com.android.mmsfolderview.data.SortMsgListData;
import com.android.mmsfolderview.data.SortMsgListItemData;
import com.android.mmsfolderview.ui.SortMsgListActivity;

import com.android.mmsfolderview.R;

public class IntentUiUtils {

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

    public static final Interpolator DEFAULT_INTERPOLATOR = new CubicBezierInterpolator(0.4f, 0.0f,
            0.2f, 1.0f);
    private static final String TAG = "IntentUiUtils";

    public static CharSequence commaEllipsize(final String text, final TextPaint paint,
            final int width, final String oneMore, final String more) {
        CharSequence ellipsized = TextUtils.commaEllipsize(text, paint, width, oneMore, more);
        if (TextUtils.isEmpty(ellipsized)) {
            ellipsized = text;
        }
        return ellipsized;
    }

    public static Intent getFolderViewMessagingCommServiceIntent() {
        return getRemoteActivityIntent(MESSAGING_PACKAGE_NAME, MESSAGING_COMMON_SERVICE);
    }

    public static Intent getSortMsgDetailActivityIntent(final Context context,
            SortMsgListItemData listItemData, boolean hasCustomTransitions) {
        final Intent intent = getRemoteActivityIntent(MESSAGING_PACKAGE_NAME,
                MESSAGING_DETAILS_ACTIVITY);
        // Always try to reuse the same activity in the current task
        // so that we don't have two activities in the back stack.
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        intent.putExtra(UI_INTENT_EXTRA_CONVERSATION_ID, listItemData.getConversationId());
        intent.putExtra(UI_INTENT_EXTRA_MESSAGE_ID, listItemData.getMessageId());
        intent.putExtra(UI_INTENT_EXTRA_PARTICIPANT_NAME, listItemData.getParticipantName());
        intent.putExtra(UI_INTENT_EXTRA_MMS_SUBJECT, listItemData.getMmsSubject());
        intent.putExtra(UI_INTENT_EXTRA_RECEIVED_TIMESTAMP, listItemData.getReceivedTimestamp());
        intent.putExtra(UI_INTENT_EXTRA_MESSAGE_PROTOCOL, listItemData.getMessageProtocol());
        intent.putExtra(UI_INTENT_EXTRA_READ, listItemData.getIsRead());
        intent.putExtra(UI_INTENT_EXTRA_BODY_TEXT, listItemData.getBobyText());
        intent.putExtra(UI_INTENT_EXTRA_MESSAGE_STATUS, listItemData.getMessageStatus());
        intent.putExtra(UI_INTENT_EXTRA_TELEPHONY_DB_URI, listItemData.getSmsMessageUri());
        intent.putExtra(UI_INTENT_EXTRA_CONTENT_TYPE, listItemData.getContentType());
        intent.putExtra(UI_INTENT_EXTRA_PARTICIPANT_NORMALIZED_DESTINATION,
                listItemData.getParticipantDestination());
        intent.putExtra(UI_INTENT_EXTRA_SEEN, listItemData.getIsSeen());
        intent.putExtra(UI_INTENT_EXTRA_IS_HAVED_STORE_CONTACT_NAME,
                listItemData.getIsHavedStoreContactName());

        if (hasCustomTransitions) {
            intent.putExtra(UI_INTENT_EXTRA_WITH_CUSTOM_TRANSITION, true);
        }

        if (!(context instanceof Activity)) {
            // If the caller supplies an application context, and not an
            // activity context, we must include this flag
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return intent;
    }

    public static Intent getAddToContactsIntent(String address) {
        // address must be a single recipient
        Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.setType(Contacts.CONTENT_ITEM_TYPE);
        if (MmsUtils.isEmailAddress(address)) {
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, address);
        } else {
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, address);
            /* Delete by SPRD for Bug:543017 2016.04.07 Start */
//            intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE,
//                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
            /* Delete by SPRD for Bug:543017 2016.04.07 End */
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        return intent;
    }

    /** Show a simple toast at the bottom */
    public static void showToastAtBottom(Context context, final String message) {
        final Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
    }

    /** Show a simple toast at the bottom */
    public static void showToastAtBottom(Context context, final int messageId) {
        showToastAtBottom(context, context.getString(messageId));
    }

    /**
     * remote activity package info
     */
    public static String MESSAGING_PACKAGE_NAME = "com.android.messaging";
    public static String MESSAGING_NEW_MESSAGE = "com.android.messaging.ui.conversation.ConversationActivity";
    public static String MESSAGING_CONVERSATION_LIST_VIEW = "com.android.messaging.ui.conversationlist.ConversationListActivity";
    public static String MESSAGING_SETTING_ACTIVITY = "com.android.messaging.ui.appsettings.SettingsActivity";
    public static String MESSAGING_COMMON_SERVICE = "com.sprd.messaging.ui.folderview.FolderViewMessagingCommService";
    public static String MESSAGING_ATTACHMENT_DISPLAY = "com.android.messaging.ui.FolderViewAttachmentDisplay";
    public static String MESSAGING_DETAILS_ACTIVITY = "com.sprd.messaging.ui.folderview.MessageDetailsActivity";

    public static String CELLBROADCASTRECEIVER_PACKAGE_NAME = "com.android.cellbroadcastreceiver";
    public static String CELLBROADCASTRECEIVER_CELLBROADCAST_LIST_ACTIVITY = "com.android.cellbroadcastreceiver.CellBroadcastListActivity";

    public static Intent getRemoteActivityIntent(String packageName, String className) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, className));
        return intent;
    }

    public static void launchFullScreenVideoViewer(final Context context, final Uri videoUri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // So we don't see "surrounding" images in Gallery
        intent.putExtra("SingleItemOnly", true);
        intent.setDataAndType(videoUri, ContentType.VIDEO_UNSPECIFIED);
        Log.d("tim_V6", "IntentUiUtils  videoUri=" + videoUri);
        startExternalActivity(context, intent);
    }

    public static void launchFullScreenPhotoViewer(final Context context, final Uri photoUri,final String photoType) {//Modify for Bug:534532
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // So we don't see "surrounding" images in Gallery
        intent.putExtra("SingleItemOnly", true);
        /*Modify by SPRD for Bug:534532  2016.03.09 Start */
        Log.d(TAG,"ItentUiUtils --- photoType:"+photoType);
        intent.setDataAndType(photoUri, photoType);
        /*Modify by SPRD for Bug:534532  2016.03.09 End */
        Log.d("tim_V6", "IntentUiUtils  photoUri=" + photoUri);
        startExternalActivity(context, intent);
    }

    /**
     * Provides a safe way to handle external activities which may not exist.
     */
    private static void startExternalActivity(final Context context, final Intent intent) {
        try {
            context.startActivity(intent);
        } catch (final ActivityNotFoundException ex) {
            Log.d("tim_V6", "Couldn't find activity:", ex);
            showToastAtBottom(context, R.string.activity_not_found_message);
        }
    }

    /**
     * Reveals/Hides a view with a scale animation from view center.
     * 
     * @param view the view to animate
     * @param desiredVisibility desired visibility (e.g. View.GONE) for the
     *            animated view.
     * @param onFinishRunnable an optional runnable called at the end of the
     *            animation
     */
    public static void revealOrHideViewWithAnimation(Context context, final View view,
            final int desiredVisibility, @Nullable
            final Runnable onFinishRunnable) {
        final boolean needAnimation = view.getVisibility() != desiredVisibility;
        if (needAnimation) {
            final float fromScale = desiredVisibility == View.VISIBLE ? 0F : 1F;
            final float toScale = desiredVisibility == View.VISIBLE ? 1F : 0F;
            final ScaleAnimation showHideAnimation = new ScaleAnimation(fromScale, toScale,
                    fromScale, toScale, ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
            /** Generic duration for revealing/hiding a view */
            int REVEAL_ANIMATION_DURATION = context.getResources().getInteger(
                    R.integer.reveal_view_animation_duration);
            showHideAnimation.setDuration(REVEAL_ANIMATION_DURATION);
            showHideAnimation.setInterpolator(DEFAULT_INTERPOLATOR);
            showHideAnimation.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationStart(final Animation animation) {
                }

                @Override
                public void onAnimationRepeat(final Animation animation) {
                }

                @Override
                public void onAnimationEnd(final Animation animation) {
                    if (onFinishRunnable != null) {
                        // Rather than running this immediately, we post it to
                        // happen next so that
                        // the animation will be completed so that the view can
                        // be detached from
                        // it's window. Otherwise, we may leak memory.
                        ThreadUtil.getMainThreadHandler().post(onFinishRunnable);
                    }
                }
            });
            view.clearAnimation();
            view.startAnimation(showHideAnimation);
            // We are playing a view Animation; unlike view property animations,
            // we can commit the
            // visibility immediately instead of waiting for animation end.
            view.setVisibility(desiredVisibility);
        } else if (onFinishRunnable != null) {
            // Make sure onFinishRunnable is always executed.
            ThreadUtil.getMainThreadHandler().post(onFinishRunnable);
        }
    }
}
