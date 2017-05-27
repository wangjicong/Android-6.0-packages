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
import com.android.messaging.util.GlobleUtil;
import com.android.messaging.util.ImageUtils;
import com.android.messaging.util.SafeAsyncTask;

import android.provider.ContactsContract.Contacts;
import android.util.Log;

import java.util.ArrayList;

import com.android.messaging.util.ContentType;

/**
 * Wraps around the functionalities to allow the user to pick vcard from the
 * Contacts picker. Instances of this class must be tied to a Fragment which is
 * able to delegate activity result callbacks.
 */
public class VcardPicker {

    /**
     * An interface for a listener that listens for when a document has been
     * picked.
     */
    public interface VcardPickerListener {
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
        void onVcardSelected(PendingAttachmentData pendingItem);
    }

    // The owning fragment.
    private final Fragment mFragment;

    // The listener on the picker events.
    private final VcardPickerListener mListener;

    /**
     * Creates a new instance of VcardPicker.
     *
     * @param activity
     *            The activity that owns the picker, or the activity that hosts
     *            the owning fragment.
     */
    public VcardPicker(final Fragment fragment,
            final VcardPickerListener listener) {
        mFragment = fragment;
        mListener = listener;
    }

    /**
     * Intent out to open an image/video from document picker.
     */
    public void launchPicker() {
        UIIntents.get().launchVcardPicker(mFragment);
    }

    /**
     * Must be called from the fragment/activity's onActivityResult().
     */
    public void onActivityResult(final int requestCode, final int resultCode,
            final Intent data) {
        if (requestCode == UIIntents.REQUEST_PICK_VCARD_PICKER
                && resultCode == Activity.RESULT_OK) {
            ArrayList<String> lookupStringKeys = data
                    .getStringArrayListExtra("result");
            if (lookupStringKeys != null) {
                final Uri vcardUri = getVcardUri(lookupStringKeys);
                if (vcardUri != null) {
                    if(!GlobleUtil.isSmilAttament){
                    	GlobleUtil.setEditedDraftMessageDate(null, null);
                    }
                    prepareVcardForAttachment(vcardUri);
                }
            }

        }
    }

    public static Uri getVcardUri(ArrayList<String> lookupKeys) {
        StringBuilder uriListBuilder = new StringBuilder();
        int index = 0;
        for (String key : lookupKeys) {
            if (index != 0)
                uriListBuilder.append(':');
            uriListBuilder.append(key);
            index++;
        }

        String lookupKeyStrings = lookupKeys.size() > 1 ? Uri
                .encode(uriListBuilder.toString()) : uriListBuilder.toString();
        Uri uri = Uri.withAppendedPath(
                lookupKeys.size() > 1 ? Contacts.CONTENT_MULTI_VCARD_URI
                        : Contacts.CONTENT_VCARD_URI, lookupKeyStrings);
        return uri;
    }

    private void prepareVcardForAttachment(final Uri vcardUri) {
        // Notify our listener with a PendingAttachmentData containing the
        // metadata.
        new SafeAsyncTask<Void, Void, String>() {

            protected String doInBackgroundTimed(final Void... params) {
                /* Modify by SPRD for Bug:527552 Start */
                return ContentType.TEXT_LOWER_VCARD;
                /* Modify by SPRD for Bug:527552 end */
            }

            @Override
            protected void onPostExecute(final String contentType) {
                // Ask the listener to create a temporary placeholder item to
                // show the progress.
                final PendingAttachmentData pendingItem = PendingAttachmentData
                        .createPendingAttachmentData(contentType, vcardUri);
                mListener.onVcardSelected(pendingItem);
            }
        }.executeOnThreadPool();
    }
}
