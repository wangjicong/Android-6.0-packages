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

import android.Manifest;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.messaging.R;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.util.OsUtil;
import android.provider.ContactsContract;
import android.content.Intent;
import android.app.Activity;

/**
 * Chooser which allows the user to record audio
 */
class VcalendarAttachMediaChooser extends MediaChooser implements
        VcalendarAttachView.VcalendarAttachViewListener {
    private View mEnabledView;
    private View mMissingPermissionView;

    VcalendarAttachMediaChooser(final MediaPicker mediaPicker) {
        super(mediaPicker);
    }

    @Override
    public int getSupportedMediaTypes() {
        return MediaPicker.MEDIA_TYPE_ATTACH_VCALENDAR;
    }

    @Override
    public int[] getIconResource() {
        return new int[] {
                R.drawable.ic_attach_vcalendar_light,
                R.drawable.ic_attach_vcalendar_dark
        };
    }

    @Override
    public int getIconDescriptionResource() {
        return R.string.attach_vcalendar;
    }

    /*
     * @Override public void onAudioRecorded(final MessagePartData item) {
     * mMediaPicker.dispatchItemsSelected(item, true); }
     */

    public void onVcalendarAttachPickerTouch() {
        mMediaPicker.launchVcalendarAttachPicker();
    }

    @Override
    protected View createView(final ViewGroup container) {
        final LayoutInflater inflater = getLayoutInflater();
        final VcalendarAttachView view = (VcalendarAttachView) inflater
                .inflate(R.layout.mediapicker_vcalendar_chooser,
                        container /* root */, false /* attachToRoot */);
        // view.setHostInterface(this);
        // view.setThemeColor(mMediaPicker.getConversationThemeColor());
        mEnabledView = view.findViewById(R.id.mediapicker_enabled);
        mMissingPermissionView = view
                .findViewById(R.id.missing_permission_view);
        view.setHostInterface(this);
        return view;
    }

    @Override
    int getActionBarTitleResId() {
        return R.string.attach_vcalendar;
    }

}
