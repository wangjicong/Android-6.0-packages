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

package com.android.messaging.ui.conversationlist;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.datamodel.data.ConversationListItemData;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.datamodel.data.ParticipantData;
import com.android.messaging.datamodel.data.PendingAttachmentData;
import com.android.messaging.sms.MmsConfig;
import com.android.messaging.ui.BaseBugleActivity;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.util.Assert;
import com.android.messaging.util.ContentType;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.MediaMetadataRetrieverWrapper;
import com.android.messaging.util.OsUtil;
import com.android.messaging.util.SafeAsyncTask;
import com.android.messaging.util.UiUtils;
import com.sprd.messaging.drm.MessagingDrmSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
//import android.util.Log;
import android.app.ActivityManager;

public class ShareIntentActivity extends BaseBugleActivity implements
        ShareIntentFragment.HostInterface {

    private MessageData mDraftMessage;
    private static ShareIntentActivity mLastActivity = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(isFinishActivity){
            return;
        }
        if(mLastActivity!=null){
            mLastActivity.finish();
        }
        mLastActivity = this;

        if(ActivityManager.isUserAMonkey()){
             finish();
             return;
        }

        // spread: fixed for bug 516261,518334 start
        if(!hasPermission()){
            OsUtil.getStoragePreminssion(this);
        }
        // spread: fixed for bug 516261,518334 end

        /* Modify by SPRD for Bug:530742 2016.02.02 Start */
//        final Intent intent = getIntent();
//        if (Intent.ACTION_SEND.equals(intent.getAction()) &&
//                (!TextUtils.isEmpty(intent.getStringExtra("address")) ||
//                !TextUtils.isEmpty(intent.getStringExtra(Intent.EXTRA_EMAIL)))) {
//            // This is really more like a SENDTO intent because a destination is supplied.
//            // It's coming through the SEND intent because that's the intent that is used
//            // when invoking the chooser with Intent.createChooser().
//            final Intent convIntent = UIIntents.get().getLaunchConversationActivityIntent(this);
//            // Copy the important items from the original intent to the new intent.
//            convIntent.putExtras(intent);
//            convIntent.setAction(Intent.ACTION_SENDTO);
//            convIntent.setDataAndType(intent.getData(), intent.getType());
//            // We have to fire off the intent and finish before trying to show the fragment,
//            // otherwise we get some flashing.
//            startActivity(convIntent);
//            finish();
//            return;
//        }
//        new ShareIntentFragment().show(getFragmentManager(), "ShareIntentFragment");
        if (isAddressSendIntent()) {
            handleAddressSendIntent();
        } else {
            mPreSendUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            mDraftMessage = null;
            SafeAsyncTask.executeOnThreadPool(new HandleSendIntentRunnable());
            /*Delete by SPRD for Bug:536450 2016.02.26  Start */
            //new ShareIntentFragment().show(getFragmentManager(), "ShareIntentFragment");
            /*Delete by SPRD for Bug:536450 2016.02.26  End */
        }
        /* Modify by SPRD for Bug:530742 2016.02.02 End */
    }

    // spread: fixed for bug 516261,518334 start
    private boolean hasPermission(){
        return OsUtil.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE) && OsUtil.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }
    // spread: fixed for bug 516261,518334 end

/* Delete by SPRD for Bug:530742 2016.02.02 Start */
//    @Override
//    public void onAttachFragment(final Fragment fragment) {
//        final Intent intent = getIntent();
//        final String action = intent.getAction();
//        //Log.e("ShareIntentActivity", "intent " +intent.toString());
//        if (Intent.ACTION_SEND.equals(action)) {
//            final Uri contentUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
//            final String contentType = extractContentType(contentUri, intent.getType());
//            if (LogUtil.isLoggable(LogUtil.BUGLE_TAG, LogUtil.DEBUG)) {
//                LogUtil.d(LogUtil.BUGLE_TAG, String.format(
//                        "onAttachFragment: contentUri=%s, intent.getType()=%s, inferredType=%s",
//                        contentUri, intent.getType(), contentType));
//            }
//            if (ContentType.TEXT_PLAIN.equals(contentType)) {
//               /* Add for Bug:489215 (add text share )  Start */
//                String sharedText =null;
//              if (contentUri != null) {
//                  Log.d("ShareIntentActivity",
//                          "=======zhongjihao====handleMessage======contentUri: "
//                                  + contentUri.toString()
//                                  + "    contentType: " + contentType);
//                  sharedText = getTextFromTxt(getContentResolver(),
//                          contentUri);
//
//              } else {
//                  sharedText = intent
//                          .getStringExtra(Intent.EXTRA_TEXT);
//              }
//               /* Add for Bug:489215 (add text share )  End */
//                if (sharedText != null) {
//                    mDraftMessage = MessageData.createSharedMessage(sharedText);
//                } else {
//                    mDraftMessage = null;
//                }
//            } else if (ContentType.isImageType(contentType) ||
//                    ContentType.isVCardType(contentType) ||
//                    ContentType.isAudioType(contentType) ||
//                    ContentType.isVideoType(contentType) ||
//                    ContentType.isVcalendarType(contentType)||
//                    /* Add by SPRD for Bug:505976 2015.11.30 Start */
//                    ContentType.isDrmType(contentType)) {
//                    /* Add by SPRD for Bug:505976 2015.11.30 End */
//                if (contentUri != null) {
//                    if(ContentType.isDrmType(contentType)){
//                        try {
//                            String dataPath = MessagingDrmSession.get().getPath(contentUri);
//                            String drmOrigContentType = MessagingDrmSession.get().getDrmOrigMimeType(dataPath, ContentType.APP_DRM_CONTENT);
//                            if (!ContentType.isImageType(drmOrigContentType) &&
//                                    !ContentType.isAudioType(drmOrigContentType) &&
//                                    !ContentType.isVideoType(drmOrigContentType)) {
//                                UiUtils.showToastAtBottom(getStringInt(contentType));
//                                finish();
//                            }
//                            if (ContentType.isImageType(drmOrigContentType)&&!isDrmImageSupport(drmOrigContentType)){
//                                UiUtils.showToastAtBottom(getStringInt(contentType));
//                                finish();
//                            }
//                            LogUtil.d(LogUtil.BUGLE_TAG, "drmOrigContentType:" + drmOrigContentType);
//                            if (dataPath != null && !dataPath.contains("/DrmDownload/")) {
//                                UiUtils.showToastAtBottom(R.string.drm_shared_not_drmdownload);
//                                finish();
//                            }
//                        }catch(Exception ex){
//
//                        }
//                  } else if (ContentType.isImageType(contentType)) {
//                      if (contentType.equals("image/tiff")) {
//                          UiUtils.showToastAtBottom(getStringInt(contentType));
//                          finish();
//                      }
//                  }
//                    mDraftMessage = MessageData.createSharedMessage(null);
//                    addSharedImagePartToDraft(contentType, contentUri);
//                } else {
//                    mDraftMessage = null;
//                }
//            } else {
//                // Unsupported content type.
//                Assert.fail("Unsupported shared content type for " + contentUri + ": " + contentType
//                        + " (" + intent.getType() + ")");
//
//                /* Add by SPRD for Bug:521086 2016.01.15 Start */
//                // Handle all unsupported content type.
//                UiUtils.showToastAtBottom(R.string.share_unsupported_type);
//                /* Add by SPRD for Bug:521086 2016.01.15 End */
//
//            }
//        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
//            final String contentType = intent.getType();
//            if (ContentType.isImageType(contentType)) {
//                // Handle sharing multiple images.
//                final ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(
//                        Intent.EXTRA_STREAM);
//                for (final Uri imageUri : imageUris) {
//                    final String actualContentType = extractContentType(imageUri, contentType);
//                    if (ContentType.isDrmType(actualContentType)) {
//                        try {
//                            String dataPath = MessagingDrmSession.get().getPath(imageUri);
//                            String drmOrigContentType = MessagingDrmSession.get().getDrmOrigMimeType(dataPath, ContentType.APP_DRM_CONTENT);
//                            if (ContentType.isImageType(drmOrigContentType) && !isDrmImageSupport(drmOrigContentType)) {
//                                UiUtils.showToastAtBottom(getStringInt(actualContentType));
//                                finish();
//                                return;
//                            }
//                        } catch (Exception ex) {
//
//                        }
//                    }
//                }
//                if (imageUris != null && imageUris.size() > 0) {
//                    mDraftMessage = MessageData.createSharedMessage(null);
//                    for (final Uri imageUri : imageUris) {
//                        final String actualContentType = extractContentType(imageUri, contentType);
//                        addSharedImagePartToDraft(actualContentType, imageUri);
//                    }
//                } else {
//                    mDraftMessage = null;
//                }
//            } else {
//                // Unsupported content type.
//                // Assert.fail("Unsupported shared content type: " +
//                // contentType);
//                LogUtil.i(LogUtil.BUGLE_TAG, "contentType " + contentType);
//                UiUtils.showToastAtBottom(R.string.share_noimgae_exceeded);
//                finish();
//            }
//        } else {
//            // Unsupported action.
//            Assert.fail("Unsupported action type for sharing: " + action);
//        }
//    }
    /* Delete by SPRD for Bug:530742 2016.02.02 End */


    private static String extractContentType(final Uri uri, final String contentType) {
        if (uri == null) {
            return contentType;
        }
        if(ContentType.VIDEO_3GPP.equals(contentType)) {
            return contentType;
        }
        // First try looking at file extension.  This is less reliable in some ways but it's
        // recommended by
        // https://developer.android.com/training/secure-file-sharing/retrieve-info.html
        // Some implementations of MediaMetadataRetriever get things horribly
        // wrong for common formats such as jpeg (reports as video/ffmpeg)
        /*SPRD: add try-catch fro Bug 517054 begin*/
        try{
            String dataPath = MessagingDrmSession.get().getPath(uri);
            if (dataPath!=null){
                if (MessagingDrmSession.get().drmCanHandle(dataPath, null)){
                    return ContentType.APP_DRM_CONTENT;
                }
            }
        } catch (Exception e) {

        }
        /*SPRD: add try-catch fro Bug 517054 end*/

        final ContentResolver resolver = Factory.get().getApplicationContext().getContentResolver();
        final String typeFromExtension = resolver.getType(uri);
        if (typeFromExtension != null) {

            /* Add by SPRD for Bug:521086 2016.01.15 Start */
            if ("application/f4v".equals(typeFromExtension)) {
                return contentType;
            }
            /* Add by SPRD for Bug:521086 2016.01.15 End */

            return typeFromExtension;
        }
        final MediaMetadataRetrieverWrapper retriever = new MediaMetadataRetrieverWrapper();
        try {
            retriever.setDataSource(uri);
            final String extractedType = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            if (extractedType != null) {

                /* Add by SPRD for Bug:521086 2016.01.15 Start */
                if ("application/f4v".equals(extractedType)) {
                    return contentType;
                }
                /* Add by SPRD for Bug:521086 2016.01.15 End */

                return extractedType;
            }
        } catch (final IOException e) {
            LogUtil.i(LogUtil.BUGLE_TAG, "Could not determine type of " + uri, e);

        /* Add by SPRD for Bug:518316 2015.12.29 Start */
        } catch (Exception e) {
            //  java.lang.SecurityException: Permission Denial
            LogUtil.e(LogUtil.BUGLE_TAG, "extractContentType...fail: " + e, e);
        /* Add by SPRD for Bug:518316 2015.12.29 End */

        } finally {
            retriever.release();
        }
        return contentType;
    }

    private void addSharedImagePartToDraft(final String contentType, final Uri dataUri) {
        mDraftMessage.addPart(PendingAttachmentData.createPendingAttachmentData(contentType, dataUri));
    }

    @Override
    public void onConversationClick(final ConversationListItemData conversationListItemData) {
        UIIntents.get().launchConversationActivity(
                this, conversationListItemData.getConversationId(), mDraftMessage);
        finish();
    }

    @Override
    public void onCreateConversationClick() {
        UIIntents.get().launchCreateNewConversationActivity(this, mDraftMessage);
        finish();
    }

    private int getStringInt(String contentType) {
        int stringid = 0;
        if (ContentType.isImageType(contentType)) {
            /* Modify by SPRD for Bug:521561 Start */
            if (contentType.equals("image/tiff")) {
                stringid = R.string.orgdrm_no_share;
            } else {
            stringid = R.string.share_image_exceeded;
            }
            /* Modify by SPRD for Bug:521561 end */
        } else if (ContentType.isAudioType(contentType)) {
            stringid = R.string.share_audio_exceeded;
        } else if (ContentType.isVideoType(contentType)) {
            stringid = R.string.share_video_exceeded;
        } else if (ContentType.APP_DRM_CONTENT.equals(contentType)){
            stringid = R.string.orgdrm_no_share;
         /* Add by SPRD for Bug 522885 Start */
        } else if (ContentType.isVCardType(contentType)){
            stringid = R.string.attachment_exceeded;
        }
        /* Add by SPRD for Bug 522885 end */
        return stringid;
    }

    private String getTextFromTxt(ContentResolver cr, Uri uri) {
        String encode = getCharset(cr, uri);
        LogUtil.i(LogUtil.BUGLE_TAG, "the text encode is :" + encode);
        StringBuilder total = new StringBuilder();
        int uriTextLength = 0;
        try {
            final InputStream is = cr.openInputStream(uri);
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, encode));
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        if(total.length() >  MmsConfig.getMaxMaxTxtFileSize()){
                            reader.close();
                            break;
                        }
                        total.append(line);
                    }
                } finally {
                    reader.close();
                }
            }
            uriTextLength = total.length();
        } catch (IOException e) {
            LogUtil.e(LogUtil.BUGLE_TAG, "Failed to read text uri =" + uri);
        }

        return total.toString().substring(0, uriTextLength/*MmsConfig.getMaxMaxTxtFileSize()*/);
    }

    private String getCharset(ContentResolver cr, Uri uri) {
        LogUtil.i(LogUtil.BUGLE_TAG, "the text uri is :" + uri);
        String code = null;
        try {
            InputStream in = cr.openInputStream(uri);
            int p = (in.read() << 8) + in.read();
            switch (p) {
                case 0xefbb:
                    code = "UTF-8";
                    break;
                case 0xfffe:
                    code = "UNICODE";
                    break;
                case 0xfeff:
                    code = "UTF-16BE";
                    break;
                default:
                    code = "GBK";
            }
        } catch (IOException e) {
            LogUtil.e(LogUtil.BUGLE_TAG, "Failed to read text");
        }
        return code;
    }

    private boolean isDrmImageSupport(String origType){
        return ContentType.isDrmImageSupport(origType);
    }

    /* And by SPRD for Bug:530742 2016.02.02 Start */
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if (isAddressSendIntent()) {
            handleAddressSendIntent();
        } else if (!isSameSendIntent()) {
            mPreSendUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            mDraftMessage = null;
            SafeAsyncTask.executeOnThreadPool(new HandleSendIntentRunnable());
            /*Delete by SPRD for Bug:536450 2016.02.26  Start */
            //new ShareIntentFragment().show(getFragmentManager(), "ShareIntentFragment");
            /*Delete by SPRD for Bug:536450 2016.02.26  End */
        } else {
            if(LogUtil.isLoggable(LogUtil.BUGLE_TAG, LogUtil.DEBUG)) {
                Log.d(LogUtil.BUGLE_TAG, "same share image uri, do not create a a fragment.");
            }
            // TODO: not handle multi-image sharing.
        }
    }

    private boolean isSendIntent() {
        return getIntent() != null && getIntent().getAction().equals(Intent.ACTION_SEND);
    }

    private boolean isAddressSendIntent() {
        return isSendIntent() && (!TextUtils.isEmpty(getIntent().getStringExtra("address")) ||
                !TextUtils.isEmpty(getIntent().getStringExtra(Intent.EXTRA_EMAIL)));
    }

    private Uri mPreSendUri;

    private boolean isSameSendIntent() {
        return isSendIntent() && (mPreSendUri != null
                && mPreSendUri.equals(getIntent().getParcelableExtra(Intent.EXTRA_STREAM)));
    }

    private void handleAddressSendIntent() {
        // This is really more like a SENDTO intent because a destination is supplied.
        // It's coming through the SEND intent because that's the intent that is used
        // when invoking the chooser with Intent.createChooser().
        Intent intent = getIntent();
        final Intent convIntent = UIIntents.get().getLaunchConversationActivityIntent(this);
        // Copy the important items from the original intent to the new intent.
        convIntent.putExtras(intent);
        convIntent.setAction(Intent.ACTION_SENDTO);
        convIntent.setDataAndType(intent.getData(), intent.getType());
        // We have to fire off the intent and finish before trying to show the fragment,
        // otherwise we get some flashing.
        startActivity(convIntent);
        finish();
    }


    private class AsyncResult {
        private String infoId;
        private boolean needFinish;
    }

    private class HandleSendIntentRunnable implements Runnable {
        @Override
        public void run() {
            Intent intent = getIntent();
            String action = intent.getAction();
            //Log.e("ShareIntentActivity", "intent " +intent.toString());
            AsyncResult ar = new AsyncResult();
            if (isSendIntent()) {
                final Uri contentUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                final String contentType = extractContentType(contentUri, intent.getType());
//                if (LogUtil.isLoggable(LogUtil.BUGLE_TAG, LogUtil.DEBUG)) {
                    LogUtil.d(LogUtil.BUGLE_TAG, String.format(
                            "lxg onAttachFragment: contentUri=%s, intent.getType()=%s, inferredType=%s",
                            contentUri, intent.getType(), contentType));
//                }
                if (ContentType.TEXT_PLAIN.equals(contentType)) {
               /* Add for Bug:489215 (add text share )  Start */
                    String sharedText = null;
                    if (contentUri != null) {
                        Log.d("ShareIntentActivity",
                                "=======zhongjihao====handleMessage======contentUri: "
                                        + contentUri.toString()
                                        + "    contentType: " + contentType);
                        sharedText = getTextFromTxt(getContentResolver(),
                                contentUri);

                    } else {
                        sharedText = intent
                                .getStringExtra(Intent.EXTRA_TEXT);
                    }
               /* Add for Bug:489215 (add text share )  End */
                    if (sharedText != null) {
                        mDraftMessage = MessageData.createSharedMessage(sharedText);
                    } else {
                        mDraftMessage = null;
                    }
                } else if (ContentType.canSharedContentType(contentType)) {
                    if (contentUri != null) {
                        if (ContentType.isDrmType(contentType)) {
                            try {
                                String dataPath = MessagingDrmSession.get().getPath(contentUri);
                                String drmOrigContentType = MessagingDrmSession.get().getDrmOrigMimeType(dataPath, ContentType.APP_DRM_CONTENT);
                                if (!ContentType.isImageType(drmOrigContentType) &&
                                        !ContentType.isAudioType(drmOrigContentType) &&
                                        !ContentType.isVideoType(drmOrigContentType)) {
                                    ar.infoId = getResources().getString(getStringInt(contentType));
                                    ar.needFinish = true;
                                } else if (ContentType.isImageType(drmOrigContentType) && !isDrmImageSupport(drmOrigContentType)) {
                                    ar.infoId = getResources().getString(getStringInt(contentType));
                                    ar.needFinish = true;
                                } else
                                    LogUtil.d(LogUtil.BUGLE_TAG, "drmOrigContentType:" + drmOrigContentType);
                                if (dataPath != null && !dataPath.contains("/DrmDownload/")) {
                                    ar.infoId = getResources().getString(R.string.drm_shared_not_drmdownload);
                                    ar.needFinish = true;
                                }
                            } catch (Exception ex) {

                            }
                        } else if (ContentType.isImageType(contentType)) {
                            if (contentType.equals("image/tiff")) {
                                ar.infoId = getResources().getString(getStringInt(contentType));
                                ar.needFinish = true;
                            }
                        }
                        if (!ar.needFinish) {
                            mDraftMessage = MessageData.createSharedMessage(null);
                            addSharedImagePartToDraft(contentType, contentUri);
                        }
                    } else {
                        mDraftMessage = null;
                    }
                } else {
                    // Unsupported content type.
                    Assert.fail("Unsupported shared content type for " + contentUri + ": " + contentType
                            + " (" + intent.getType() + ")");

                /* Add by SPRD for Bug:521086 2016.01.15 Start */
                    // Handle all unsupported content type.
                    ar.infoId = getResources().getString(R.string.share_unsupported_type);
                /* Add by SPRD for Bug:521086 2016.01.15 End */

                }
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                final String contentType = intent.getType();
                if (ContentType.isImageType(contentType)) {
                    // Handle sharing multiple images.
                    final ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(
                            Intent.EXTRA_STREAM);
                    if(LogUtil.isLoggable(LogUtil.BUGLE_TAG, LogUtil.DEBUG)) {
                        Log.d(LogUtil.BUGLE_TAG, "original share images number:" + imageUris.size());
                    }
                    int retainNumber = getMaxSharedImageLimit();
                    boolean exceedImageLimit = false;
                    String unsupportedDrmImageContentType = "";
                    for(int i = 0; i < imageUris.size(); i++) {
                        Uri imageUri = imageUris.get(i);
                        String actualContentType = extractContentType(imageUri, contentType);
                        if (ContentType.isDrmType(actualContentType)) {
                            try {
                                String dataPath = MessagingDrmSession.get().getPath(imageUri);
                                String drmOrigContentType = MessagingDrmSession.get().getDrmOrigMimeType(dataPath, ContentType.APP_DRM_CONTENT);
                                if (ContentType.isImageType(drmOrigContentType) && !isDrmImageSupport(drmOrigContentType)) {
                                    unsupportedDrmImageContentType = actualContentType;
                                    imageUris.remove(i);
                                    continue;
                                }
                            } catch (Exception ex) {
                                continue;
                            }
                        }
                        if(retainNumber > 0) {
                            --retainNumber;
                        }
                        if(retainNumber == 0) {
                            exceedImageLimit = true;
                            imageUris.remove(i);
                        }
                    }
                    if(LogUtil.isLoggable(LogUtil.BUGLE_TAG, LogUtil.DEBUG)) {
                        Log.d(LogUtil.BUGLE_TAG, "share images number:" + imageUris.size());
                    }
                    if (imageUris.size() == 0) {
                        if (!TextUtils.isEmpty(unsupportedDrmImageContentType)) {
                            ar.infoId = getResources().getString(getStringInt(unsupportedDrmImageContentType));
                            ar.needFinish = true;
                        }
                    } else if (exceedImageLimit) {
                        ar.infoId = String.format(getResources().getString(R.string.shared_image_number_exceed), getMaxSharedImageLimit());
                        ar.needFinish = false;
                    }


                    for (final Uri imageUri : imageUris) {
                        final String actualContentType = extractContentType(imageUri, contentType);
                        if (ContentType.isDrmType(actualContentType)) {
                            try {
                                String dataPath = MessagingDrmSession.get().getPath(imageUri);
                                String drmOrigContentType = MessagingDrmSession.get().getDrmOrigMimeType(dataPath, ContentType.APP_DRM_CONTENT);
                                if (ContentType.isImageType(drmOrigContentType) && !isDrmImageSupport(drmOrigContentType)) {
                                    ar.infoId = getResources().getString(getStringInt(actualContentType));
                                    ar.needFinish = true;
                                    break;
                                }
                            } catch (Exception ex) {

                            }
                        }
                    }
                    if (!ar.needFinish && imageUris != null && imageUris.size() > 0) {
                        mDraftMessage = MessageData.createSharedMessage(null);
                        for (final Uri imageUri : imageUris) {
                            final String actualContentType = extractContentType(imageUri, contentType);
                            addSharedImagePartToDraft(actualContentType, imageUri);
                        }
                    } else {
                        mDraftMessage = null;
                    }
                } else {
                    // Unsupported content type.
                    // Assert.fail("Unsupported shared content type: " +
                    // contentType);
                    LogUtil.i(LogUtil.BUGLE_TAG, "contentType " + contentType);
                    ar.infoId = getResources().getString(R.string.share_noimgae_exceeded);
                    ar.needFinish = true;
                }
            } else {
                // TODO: HERE, not handle: Unsupported action.
                Assert.fail("Unsupported action type for sharing: " + action);
            }
            /*Modify by SPRD for Bug:536450 2016.02.26  Start */
            //if (ar.needFinish || !TextUtils.isEmpty(ar.infoId)) {
                Message msg = mHandler.obtainMessage();
                msg.obj = ar;
                mHandler.sendMessage(msg);
            //}
            /*Modify by SPRD for Bug:536450 2016.02.26  End */
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.obj != null && msg.obj instanceof AsyncResult) {
                AsyncResult r = (AsyncResult) msg.obj;
                if (!TextUtils.isEmpty(r.infoId)) {
                    UiUtils.showToastAtBottom(r.infoId);
                }
                if (r.needFinish) {
                    finish();
                /*Add by SPRD for Bug:536450 2016.02.26  Start */
                }else{
                    showFragment();
                }
                /*Add by SPRD for Bug:536450 2016.02.26  End */
            }
        }
    };

    /*Add by SPRD for Bug:536450 2016.02.26  Start */
    private void showFragment() {
        if (!isFinishing() && !isDestroyed()) {
            ShareIntentFragment shareIntentFragment = new ShareIntentFragment();
            try {
                shareIntentFragment.show(getFragmentManager(), "ShareIntentFragment");
            } catch (Exception e) {
                Log.e(LogUtil.BUGLE_TAG, "ShareIntentActivity-showFragment error:", e);
            }
        }
    }
    /*Add by SPRD for Bug:536450 2016.02.26  End */

    private int getMaxSharedImageLimit() {
        int maxSharedImageLimit = MmsConfig.get(ParticipantData.DEFAULT_SELF_SUB_ID).getSharedImageLimit();
        if(LogUtil.isLoggable(LogUtil.BUGLE_TAG, LogUtil.DEBUG)) {
            Log.d(LogUtil.BUGLE_TAG, "max shared images number:" + maxSharedImageLimit);
        }
        return maxSharedImageLimit;
    }
    /* And by SPRD for Bug:530742 2016.02.02 End */
}
