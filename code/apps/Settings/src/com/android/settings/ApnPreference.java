/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.provider.Telephony;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RelativeLayout;

public class ApnPreference extends Preference implements
        CompoundButton.OnCheckedChangeListener, OnClickListener {
    final static String TAG = "ApnPreference";
    // SPRD: Bug 497469 Remember last selected APN for each sub
    private int mSubId;

    // SPRD: default apn
    private String type;

    public void setType(String mType){
        type = mType;
    }
    public String getType(){
        return type;
    }
    // SPRD: default apn
    public ApnPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ApnPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.apnPreferenceStyle);
    }

    public ApnPreference(Context context) {
        this(context, null);
    }

    /* SPRD: Bug 497469 Use sparse array to remember selected key for each sub. @{ */
    // Initial size is 2(dual sim).
    private static SparseArray<String> mSelectedKey = new SparseArray<String>(2);
    private static SparseArray<CompoundButton> mCurrentChecked = new SparseArray<CompoundButton>(2);
    /* @} */
    private boolean mProtectFromCheckedChange = false;
    private boolean mSelectable = true;

    @Override
    public View getView(View convertView, ViewGroup parent) {
        View view = super.getView(convertView, parent);

        View widget = view.findViewById(R.id.apn_radiobutton);
        if ((widget != null) && widget instanceof RadioButton) {
            RadioButton rb = (RadioButton) widget;
            if (mSelectable) {
                rb.setOnCheckedChangeListener(this);

                // SPRD: Bug 497469 Remember last selected APN for each sub
                boolean isChecked = getKey().equals(mSelectedKey.get(mSubId));
                if (isChecked) {
                    /* SPRD: Bug 497469 Remember last selected APN for each sub @{ */
                    mCurrentChecked.put(mSubId, rb);
                    mSelectedKey.put(mSubId, getKey());
                    /* @} */
                }

                mProtectFromCheckedChange = true;
                rb.setChecked(isChecked);
                mProtectFromCheckedChange = false;
                rb.setVisibility(View.VISIBLE);
            } else {
                rb.setVisibility(View.GONE);
            }
        }

        View textLayout = view.findViewById(R.id.text_layout);
        if ((textLayout != null) && textLayout instanceof RelativeLayout) {
            textLayout.setOnClickListener(this);
        }

        return view;
    }

    public boolean isChecked() {
        // SPRD: Bug 497469 Remember last selected APN for each sub
        return getKey().equals(mSelectedKey.get(mSubId));
    }

    public void setChecked() {
        // SPRD: Bug 497469 Remember last selected APN for each sub
        mSelectedKey.put(mSubId, getKey());
    }

    /* SPRD: Bug 497469 Remember last selected APN for each sub @{ */
    public void setSubscriptionId(int subId) {
        mSubId = subId > 0 ? subId : 0;
    }
    /* @} */

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.i(TAG, "ID: " + getKey() + " :" + isChecked);
        if (mProtectFromCheckedChange) {
            return;
        }

        if (isChecked) {
            /* SPRD: Bug 497469 Remember last selected APN for each sub @{ */
            if (mCurrentChecked.get(mSubId) != null) {
                mCurrentChecked.get(mSubId).setChecked(false);
            }
            mCurrentChecked.put(mSubId, buttonView);
            String key = getKey();
            mSelectedKey.put(mSubId, key);
            callChangeListener(key);
            /* @} */
        } else {
            /* SPRD: Bug 497469 Remember last selected APN for each sub @{ */
            mCurrentChecked.remove(mSubId);
            mSelectedKey.remove(mSubId);
            /* @} */
        }
    }

    public void onClick(android.view.View v) {
        if ((v != null) && (R.id.text_layout == v.getId())) {
            Context context = getContext();
            if (context != null) {
                int pos = Integer.parseInt(getKey());
                Uri url = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, pos);
                /* SPRD: Bug 538335 add support for MVNO type pnn @{ */
                Intent intent = new Intent(Intent.ACTION_EDIT, url);
                intent.putExtra(ApnSettings.SUB_ID, mSubId);
                context.startActivity(intent);
                /* @} */
            }
        }
    }

    public void setSelectable(boolean selectable) {
        mSelectable = selectable;
    }

    public boolean getSelectable() {
        return mSelectable;
    }
}
