/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.sprd.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import android.util.Log;

import com.android.settings.R;

public class SprdChooseOS extends Activity implements AdapterView.OnItemSelectedListener, OnClickListener{

    private Button mCancelButton;
    private Button mNextButton;
    private Spinner mSpinner;
    private View mOSView;
    private View mNextView;
    private ArrayAdapter<String> mAdapter ;
    private String [] mOSArray;
    private String [] mIPArray;
    private String resultIP;
    private boolean isGuidance = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chooseos);

        mCancelButton = (Button) findViewById(R.id.cancel_button);
        mNextButton = (Button) findViewById(R.id.next_button);
        mSpinner = (Spinner) findViewById(R.id.os_spinner);
        mOSView = (View) findViewById(R.id.os);
        mNextView = (View) findViewById(R.id.next);
        mOSArray = getResources().getStringArray(R.array.os_string_array);
        mIPArray = getResources().getStringArray(R.array.os_ip_values);
        mAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,mOSArray);

        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(mAdapter);
        mSpinner.setOnItemSelectedListener(this);
        mCancelButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);
        mNextButton.setEnabled(false);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        resultIP = mIPArray[position];
        if (position == 0) {
            mNextButton.setEnabled(false);
        } else {
            mNextButton.setEnabled(true);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, SprdUsbSettings.class);
        switch (v.getId()) {
            case R.id.next_button:
                if (isGuidance) {
                    intent.putExtra("callback", 1);
                    intent.putExtra("ip", resultIP);
                    startActivity(intent);
                    finish();
                } else {
                    isGuidance = true;
                    updateUI(true);
                }
                break;

            case R.id.cancel_button:
                if (isGuidance) {
                    isGuidance = false;
                    updateUI(false);
                } else {
                    intent.putExtra("callback", 0);
                    startActivity(intent);
                    finish();
                }
                break;
        }
    }

    private void updateUI(boolean toNext) {
        if (toNext) {
            mOSView.setVisibility(View.GONE);
            mNextView.setVisibility(View.VISIBLE);
            mCancelButton.setText(R.string.usb_pc_back);
            mNextButton.setText(R.string.usb_pc_ok);
        } else {
            mOSView.setVisibility(View.VISIBLE);
            mNextView.setVisibility(View.GONE);
            mCancelButton.setText(R.string.usb_pc_cancel);
            mNextButton.setText(R.string.usb_pc_next);
        }
    }

}
