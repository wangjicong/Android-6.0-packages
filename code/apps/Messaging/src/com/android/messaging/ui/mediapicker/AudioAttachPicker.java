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
package com.android.messaging.ui.mediapicker;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.android.messaging.Factory;
import com.android.messaging.datamodel.data.PendingAttachmentData;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.util.ImageUtils;
import com.android.messaging.util.SafeAsyncTask;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import java.util.ArrayList;

import com.android.messaging.util.ContentType;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.MediaStore;
import com.android.messaging.util.UriUtil;
import com.android.messaging.sms.MmsUtils;
import android.media.RingtoneManager;

import com.sprd.messaging.drm.MessagingDrmSession;
import com.sprd.messaging.drm.MessagingDrmHelper;
import android.util.Log;
import com.android.messaging.util.ContentType;
import com.android.messaging.ui.VdataUtils;

import com.android.messaging.util.UiUtils;
import com.android.messaging.R;
/**
 * Wraps around the functionalities to allow the user to pick AudioAttach from the
 * Contacts picker. Instances of this class must be tied to a Fragment which is
 * able to delegate activity result callbacks.
 */
public class AudioAttachPicker {

    /**
     * An interface for a listener that listens for when a document has been
     * picked.
     */
    public interface AudioAttachPickerListener {
        /**
         * Called when Contacts is selected from picker. At this point, the file
         * hasn't been actually loaded and staged in the temp directory, so we
         * are passing in a pending MessagePartData, which the consumer should
         * use to display a placeholder image.
         *
         * @param pendingItem
         *            a temporary attachment data for showing the placeholder
         *            state.
         */
        void onAudioAttachSelected(PendingAttachmentData pendingItem);
    }

    // The owning fragment.
    private final Fragment mFragment;

    // The listener on the picker events.
    private final AudioAttachPickerListener mListener;
    private static final String TAG="AudioAttachPicker";

    /**
     * Creates a new instance of AudioAttachPicker.
     *
     * @param activity
     *            The activity that owns the picker, or the activity that hosts
     *            the owning fragment.
     */
    public AudioAttachPicker(final Fragment fragment,
            final AudioAttachPickerListener listener) {
        mFragment = fragment;
        mListener = listener;
    }

    /**
     * Intent out to open an image/video from document picker.
     */
    public void launchPicker() {
        UIIntents.get().launchAudioAttachPicker(mFragment);
    }

    /**
     * Must be called from the fragment/activity's onActivityResult().
     */
    public void onActivityResult(final int requestCode, final int resultCode,
            final Intent data) {
        if (requestCode == UIIntents.REQUEST_PICK_AUDIO_PICKER
                && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                final Uri uri = (Uri) data
                        .getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                prepareAudioAttachForAttachment(uri);
                // break;
            }
        }

    }

    private void prepareAudioAttachForAttachment(final Uri AudioAttachUri) {
        // Notify our listener with a PendingAttachmentData containing the
        // metadata.
        new SafeAsyncTask<Void, Void, String>() {

            protected String doInBackgroundTimed(final Void... params) {
                /* Modify by SPRD for Bug:527552 Start */
                String AudioType=VdataUtils.getAudioType(VdataUtils.getFileType(AudioAttachUri,"oog"));
                Log.d("TAG"," AudioType:"+AudioType+" AudioAttachUri:"+AudioAttachUri);
                return ContentType.getContentTypeFromExtension(
                        AudioAttachUri.toString(),
                        AudioType);
                /* Modify by SPRD for Bug:527552 end */
            }

            @Override
            protected void onPostExecute(final String contentType) {
                // Ask the listener to create a temporary placeholder item to
                // show the progress.
                String type = contentType;
                try {
                    String path = MessagingDrmSession.get().getPath(AudioAttachUri);
                    Log.d(TAG, "path is " + path);
                    if (path != null && MessagingDrmSession.get().drmCanHandle(path, null)) {
                        type = ContentType.APP_DRM_CONTENT;
                        if (!MessagingDrmHelper.isDrmSDFile(mFragment.getContext(), path, type)){
                            UiUtils.showToastAtBottom(R.string.drm_unsupported_toast);
                            return;
                        }
                        if (!path.contains("/DrmDownload/")) {
                            UiUtils.showToastAtBottom(R.string.drm_shared_not_drmdownload);
                            return;
                        }
                    }
                } catch (Exception ex) {
                    Log.d(TAG, "onActivityResult ex  " + ex);
                }
                Log.d(TAG, "type is " + type);
                final PendingAttachmentData pendingItem = PendingAttachmentData
                        .createPendingAttachmentData(type,
                                AudioAttachUri);
                mListener.onAudioAttachSelected(pendingItem);
            }
        }.executeOnThreadPool();
    }
}
