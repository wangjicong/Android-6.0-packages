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

package com.android.mmsfolderview.ui;

import com.android.mmsfolderview.data.SortMsgDataCollector;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import com.android.mmsfolderview.R;

/**
 * A view used to render contact icons. This class derives from AsyncImageView,
 * so it loads contact icons from MediaResourceManager, and it handles more
 * rendering logic than an AsyncImageView (draws a circular bitmap).
 */
public class ContactIconView extends AsyncImageView {

    private final static String TAG = "ContactIconView";

    public ContactIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public void setImageResource(int status, int protocol, boolean isRead) {
        int sortTypeb= SortMsgDataCollector.getSortTypeByStatus(status);
        switch (sortTypeb) {
            // Received:
            case SortMsgDataCollector.MSG_BOX_INBOX:
                if (SortMsgDataCollector.getIsSms(protocol)) {
                    if (isRead) {
                        setImageResource(R.drawable.msg_readed);
                    } else {
                        setImageResource(R.drawable.msg_unread);
                    }
                } else if (SortMsgDataCollector.getIsMms(protocol)) {
                    if (isRead) {
                        setImageResource(R.drawable.mms_readed);
                    } else {
                        setImageResource(R.drawable.mms_unread);
                    }
                }
                break;
            // Sent:
            case SortMsgDataCollector.MSG_BOX_SENT:
                if (SortMsgDataCollector.getIsSms(protocol)) {
                   setImageResource(R.drawable.ic_sent);
                } else if (SortMsgDataCollector.getIsMms(protocol)) {
                    setImageResource(R.drawable.ic_sent_mms);
                }
                break;
            // Sending & Fail:
            case SortMsgDataCollector.MSG_BOX_OUTBOX:
                if (SortMsgDataCollector.getIsSms(protocol)) {
                    setImageResource(R.drawable.ic_outbox);
                 } else if (SortMsgDataCollector.getIsMms(protocol)) {
                     setImageResource(R.drawable.ic_outbox_mms);
                 }
                break;
            //Draft:
            case SortMsgDataCollector.MSG_BOX_DRAFT:
                if (SortMsgDataCollector.getIsSms(protocol)) {
                    setImageResource(R.drawable.msg_drafts_sms);
                 } else if (SortMsgDataCollector.getIsMms(protocol)) {
                     setImageResource(R.drawable.msg_drafts_mms);
                 }
                break;
            default:
                Log.e(TAG, "Unknow message status, ContactIconView will be null");
                break;
        }
        
    }
}
