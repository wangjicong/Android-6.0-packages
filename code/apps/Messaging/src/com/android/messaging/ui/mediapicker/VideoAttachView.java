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
import android.widget.ImageButton;
import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.util.ContentType;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.UiUtils;
import android.util.Log;

/**
 * Hosts an audio recorder with tap and hold to record functionality.
 */
public class VideoAttachView extends FrameLayout {
    /**
     * An interface that communicates with the hosted AudioRecordView.
     */
    public interface VideoAttachViewListener {
        void onVideoAttachPickerTouch();
    }

    private View mVideoView;
    private TextView mHintTextView;
    private VideoAttachViewListener mListener;
    private ImageButton mVideoutton;

    public VideoAttachView(final Context context, final AttributeSet attrs) {
        super(context, attrs);

    }

    public void setHostInterface(final VideoAttachViewListener hostInterface) {
        mListener = hostInterface;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mVideoView = findViewById(R.id.video_attach_button);
        mHintTextView = (TextView) findViewById(R.id.hint_text);
        mVideoutton = (ImageButton) findViewById(R.id.select_video_attach);
        mVideoutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View clickView) {
                mListener.onVideoAttachPickerTouch();
            }
        });

    }

}
