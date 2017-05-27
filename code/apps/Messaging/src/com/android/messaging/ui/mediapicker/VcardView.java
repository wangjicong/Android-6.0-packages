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

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.messaging.Factory;
import com.android.messaging.R;
import android.widget.ImageButton;

import android.util.Log;

/**
 * Hosts an audio recorder with tap and hold to record functionality.
 */
public class VcardView extends FrameLayout {
    /**
     * An interface that communicates with the hosted AudioRecordView.
     */
    public interface VcardViewListener {
        void onVcardPickerTouch();
    }

    // For accessibility, the touchable record button is bigger than the record
    // button visual.
    private View mVcardview;
    private TextView mHintTextView;
    private VcardViewListener mListener;
    private ImageButton mVcardButton;

    public VcardView(final Context context, final AttributeSet attrs) {
        super(context, attrs);

    }

    public void setHostInterface(final VcardViewListener hostInterface) {
        mListener = hostInterface;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mVcardview = findViewById(R.id.vcard_attach_button);
        mHintTextView = (TextView) findViewById(R.id.hint_text);
        mVcardButton = (ImageButton) findViewById(R.id.select_vcard_attach);
        mVcardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View clickView) {
                mListener.onVcardPickerTouch();
            }
        });

    }

}
