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
/**
 * Wraps around the functionalities to allow the user to pick VideoAttach from the
 * Contacts picker. Instances of this class must be tied to a Fragment which is
 * able to delegate activity result callbacks.
 */
public class VcalendarAttachPicker {

    /**
     * An interface for a listener that listens for when a document has been
     * picked.
     */
    public interface VcalendarAttachPickerListener {
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
        void onVcalendarAttachSelected(PendingAttachmentData pendingItem);
    }

    // The owning fragment.
    private final Fragment mFragment;

    // The listener on the picker events.
    private final VcalendarAttachPickerListener mListener;

    /**
     * Creates a new instance of VideoAttachPicker.
     *
     * @param activity
     *            The activity that owns the picker, or the activity that hosts
     *            the owning fragment.
     */
    public VcalendarAttachPicker(final Fragment fragment,
            final VcalendarAttachPickerListener listener) {
        mFragment = fragment;
        mListener = listener;
    }

    /**
     * Intent out to open an image/video from document picker.
     */
    public void launchPicker() {
        UIIntents.get().launchVcalendarAttachPicker(mFragment);
    }

    /**
     * Must be called from the fragment/activity's onActivityResult().
     */
    public void onActivityResult(final int requestCode, final int resultCode,
            final Intent data) {
        if (requestCode == UIIntents.REQUEST_PICK_VCALENDAR_PICKER
                && resultCode == Activity.RESULT_OK) {
            final Uri uri = data.getData();
            prepareVcalendarAttachForAttachment(uri);
        }
    }

    private static final String[] MEDIA_PROJECTION = new String[] { MediaStore.MediaColumns.MIME_TYPE };

    private static final int INDEX_CONTENT_TYPE = 0;

    public static String getVcalendarContentType(final ContentResolver cr,
            final Uri uri) {
        // Figure out the content type of media.
        String contentType = null;
        Cursor cursor = null;
        if (UriUtil.isMediaStoreUri(uri)) {
            try {
                cursor = cr.query(uri, MEDIA_PROJECTION, null, null, null);

                if (cursor != null && cursor.moveToFirst()) {
                    contentType = cursor.getString(INDEX_CONTENT_TYPE);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (contentType == null) {
            // Last ditch effort to get the content type. Look at the file
            // extension.
            contentType = ContentType.getContentTypeFromExtension(
                    uri.toString(), ContentType.TEXT_VCALENDAR);
        }
        return contentType;
    }

    private void prepareVcalendarAttachForAttachment(final Uri VcalendarAttachUri) {
        // Notify our listener with a PendingAttachmentData containing the
        // metadata.
        new SafeAsyncTask<Void, Void, String>() {

            protected String doInBackgroundTimed(final Void... params) {
                return getVcalendarContentType(Factory.get()
                        .getApplicationContext().getContentResolver(),
                        VcalendarAttachUri);
            }

            @Override
            protected void onPostExecute(final String contentType) {
                // Ask the listener to create a temporary placeholder item to
                // show the progress.
                final PendingAttachmentData pendingItem = PendingAttachmentData
                        .createPendingAttachmentData(contentType,
                                VcalendarAttachUri);
                mListener.onVcalendarAttachSelected(pendingItem);
            }
        }.executeOnThreadPool();
    }
}
