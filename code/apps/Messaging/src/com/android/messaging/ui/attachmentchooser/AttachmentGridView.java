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
package com.android.messaging.ui.attachmentchooser;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.GridView;

import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.ui.attachmentchooser.AttachmentChooserFragment.AttachmentGridAdapter;
import com.android.messaging.util.Assert;
import com.android.messaging.util.UiUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.util.Log;
import android.widget.Toast;
import android.content.DialogInterface;
import android.app.AlertDialog;

import com.android.messaging.R;
import com.sprd.messaging.drm.MessagingDrmSession;
import com.android.messaging.util.ContentType;
import com.android.messaging.ui.UIIntents;

/**
 * Displays a grid of attachment previews for the user to choose which to select/unselect
 */
public class AttachmentGridView extends GridView implements
        AttachmentGridItemView.HostInterface {
    public interface AttachmentGridHost {
        void displayPhoto(final Rect viewRect, final Uri photoUri);
        void updateSelectionCount(final int count);
    }

    // By default everything is selected so only need to keep track of the unselected set.
    private final Set<MessagePartData> mUnselectedSet;
    private AttachmentGridHost mHost;
    private static final String TAG = "AttachmentGridView";
    private Context mContext;

    public AttachmentGridView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mUnselectedSet = new HashSet<>();
    }

    public void setHost(final AttachmentGridHost host) {
        mHost = host;
    }

    @Override
    public boolean isItemSelected(final MessagePartData attachment) {
        return !mUnselectedSet.contains(attachment);
    }

    private void checkDrmRightsConsume(final MessagePartData attachment,final AttachmentGridItemView view){
        AlertDialog.Builder builder = MessagingDrmSession.get().showProtectInfo(getContext(), attachment.getDrmDataPath(), true/*is picture*/);

        /* Modify by SPRD for Bug:524873  2015.01.21 Start */
//        builder.setTitle(mContext.getString(R.string.drm_consume_title))
//               .setMessage(mContext.getString(R.string.drm_consume_hint))
        builder.setPositiveButton(mContext.getString(R.string.ok_drm_rights_consume),
        /* Modify by SPRD for Bug:524873  2015.01.21 End */

                new AlertDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            mHost.displayPhoto(UiUtils.getMeasuredBoundsOnScreen(view), attachment.getContentUri());
                        } catch (Exception e) {
                            Toast.makeText(
                                    mContext,
                                    mContext.getString(R.string.drm_no_application_open),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(mContext.getString(R.string.cancel_drm_rights_consume),null).show();
   }

    @Override
    public void onItemClicked(final AttachmentGridItemView view, final MessagePartData attachment) {
        // If the item is an image, show the photo viewer. All the other types (video, audio,
        // vcard) have internal click handling for showing previews so we don't need to handle them
        if (attachment.isImage()) {
             String contentType = attachment.getContentType();
             boolean isDrm = ContentType.isDrmType(contentType);
             Log.d(TAG, " content type is "+contentType+" is drm "+isDrm);
              if (isDrm){
	            Log.d(TAG, " content type is "+contentType+" path "+attachment.getDrmDataPath());
                   if ( attachment.getDrmFileRightsStatus()== false){
                        UIIntents.get().launchDrmRightRequestActivity(mContext, attachment);
                        return ;
                   }
                   checkDrmRightsConsume(attachment, view);
	            return ;
            }
            mHost.displayPhoto(UiUtils.getMeasuredBoundsOnScreen(view), attachment.getContentUri());
        }
    }

    @Override
    public void onItemCheckedChanged(AttachmentGridItemView view, MessagePartData attachment) {
        // Toggle selection.
        if (isItemSelected(attachment)) {
            mUnselectedSet.add(attachment);
        } else {
            mUnselectedSet.remove(attachment);
        }
        view.updateSelectedState();
        updateSelectionCount();
    }

    public Set<MessagePartData> getUnselectedAttachments() {
        return Collections.unmodifiableSet(mUnselectedSet);
    }

    private void updateSelectionCount() {
        final int count = getAdapter().getCount() - mUnselectedSet.size();
        Assert.isTrue(count >= 0);
        mHost.updateSelectionCount(count);
    }

    private void refreshViews() {
        if (getAdapter() instanceof AttachmentGridAdapter) {
            ((AttachmentGridAdapter) getAdapter()).notifyDataSetChanged();
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        final SavedState savedState = new SavedState(superState);
        savedState.unselectedParts = mUnselectedSet
                .toArray(new MessagePartData[mUnselectedSet.size()]);
        return savedState;
    }

    @Override
    public void onRestoreInstanceState(final Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        final SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        mUnselectedSet.clear();
        for (int i = 0; i < savedState.unselectedParts.length; i++) {
            final MessagePartData unselectedPart = savedState.unselectedParts[i];
            mUnselectedSet.add(unselectedPart);
        }
        refreshViews();
    }

    /**
     * Persists the item selection state to saved instance state so we can restore on activity
     * recreation
     */
    public static class SavedState extends BaseSavedState {
        MessagePartData[] unselectedParts;

        SavedState(final Parcelable superState) {
            super(superState);
        }

        private SavedState(final Parcel in) {
            super(in);

            // Read parts
            final int partCount = in.readInt();
            unselectedParts = new MessagePartData[partCount];
            for (int i = 0; i < partCount; i++) {
                unselectedParts[i] = ((MessagePartData) in.readParcelable(
                        MessagePartData.class.getClassLoader()));
            }
        }

        @Override
        public void writeToParcel(final Parcel out, final int flags) {
            super.writeToParcel(out, flags);

            // Write parts
            out.writeInt(unselectedParts.length);
            for (final MessagePartData image : unselectedParts) {
                out.writeParcelable(image, flags);
            }
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(final Parcel in) {
                return new SavedState(in);
            }
            @Override
            public SavedState[] newArray(final int size) {
                return new SavedState[size];
            }
        };
    }
}
