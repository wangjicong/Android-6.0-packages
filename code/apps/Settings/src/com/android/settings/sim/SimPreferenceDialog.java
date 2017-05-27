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
package com.android.settings.sim;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.util.Log;
import com.android.settings.R;
import com.sprd.android.config.OptConfig;

public class SimPreferenceDialog extends Activity {

    private Context mContext;
    private SubscriptionInfo mSubInfoRecord;
    private int mSlotId;
    private int[] mTintArr;
    private String[] mColorStrings;
    private int mTintSelectorPos;
    private SubscriptionManager mSubscriptionManager;
    AlertDialog.Builder mBuilder;
    View mDialogLayout;
    private final String SIM_NAME = "sim_name";
    private final String TINT_POS = "tint_pos";
    private final String DISPLAY_NUMBER = "display_number";
    // SPRD: add for log tag
    private final String TAG = "SimPreferenceDialog";

    /* SPRD: modify for bug496697 @{ */
    private LinearLayout mSimEditLayout;
    private static final String KEY_FOCUS_LOCATION = "focus_location";
    private static final String KEY_FOCUS_ID = "focus_id";
    /* }@ */
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        mContext = this;
        final Bundle extras = getIntent().getExtras();
        mSlotId = extras.getInt(SimSettings.EXTRA_SLOT_ID, -1);
        mSubscriptionManager = SubscriptionManager.from(mContext);
        mSubInfoRecord = mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(mSlotId);
        /* SPRD: Add for SignalCluster colorful  @{ */
        boolean isColorfulSignal = false;
        CarrierConfigManager configManager = (CarrierConfigManager)mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle persistableBundle = configManager.getConfigForPhoneId(mSlotId);
        //Kalyy 20160901
        /*
        if (persistableBundle != null) {
            isColorfulSignal = persistableBundle
                    .getBoolean(CarrierConfigManager.KEY_SYSTEMUI_CARRIER_LABEL_WITH_SIMCOLOR_BOOL);
        }
        */
        isColorfulSignal = OptConfig.SIM_ICON_WITH_COLOR;
        //Kalyy 20160901
        if (!isColorfulSignal) {
            mTintArr = mContext.getResources().getIntArray(com.android.internal.R.array.sim_colors);
            mColorStrings = mContext.getResources().getStringArray(R.array.color_picker);
        } else {
            mTintArr = mContext.getResources().getIntArray(com.android.internal.R.array.sim_colors_light);
            mColorStrings = mContext.getResources().getStringArray(R.array.light_color_picker);
        }
        /* @} */
        mTintSelectorPos = 0;

        mBuilder = new AlertDialog.Builder(mContext);
        LayoutInflater inflater = (LayoutInflater)mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDialogLayout = inflater.inflate(R.layout.multi_sim_dialog, null);
        mSimEditLayout = (LinearLayout)mDialogLayout.findViewById(R.id.sim_edit_layout);
        mBuilder.setView(mDialogLayout);

        createEditDialog(bundle);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        savedInstanceState.clear();
        savedInstanceState.putInt(TINT_POS, mTintSelectorPos);
        final EditText nameText = (EditText)mDialogLayout.findViewById(R.id.sim_name);
        savedInstanceState.putString(SIM_NAME, nameText.getText().toString());
        /* SPRD: modify for bug493417 @{ */
        final EditText numberText = (EditText)mDialogLayout.findViewById(R.id.display_number);
        savedInstanceState.putString(DISPLAY_NUMBER, numberText.getText().toString());
        /* @} */

        /* SPRD: modify for bug496697 @{ */
        //SPRD: Del for bug 518800
        findFocus(mSimEditLayout, savedInstanceState);
        /* }@ */
        super.onSaveInstanceState(savedInstanceState);

    }
    /* SPRD: modify for bug496697 @{ */
    public int findFocus(ViewGroup child,Bundle bundle){
        int focusId = 0;
        int focuslocation = 0;
        for (int i = 0;i < child.getChildCount();i++ ){
            if (child.getChildAt(i) instanceof ViewGroup){
                focusId = findFocus((ViewGroup) child.getChildAt(i), bundle);
                if (focusId > 0) {
                    return focusId;
                }
            } else {
                for (int j = 0;j < child.getChildCount();j++) {
                    if (bundle.getInt(KEY_FOCUS_ID) == 0){
                        if (child.getChildAt(j).hasFocus()) {
                            focusId = child.getChildAt(j).getId();
                            focuslocation = ((EditText)child.getChildAt(j)).getSelectionStart();
                            bundle.putInt(KEY_FOCUS_ID,focusId);
                            bundle.putInt(KEY_FOCUS_LOCATION,focuslocation);
                            return focusId;
                        }
                    } else {
                        if (bundle.getInt(KEY_FOCUS_ID) == child.getChildAt(j).getId()){
                            focuslocation = bundle.getInt(KEY_FOCUS_LOCATION);
                            if (focuslocation >= 0 && focuslocation <= ((EditText)child.getChildAt(j)).length()){
                                child.getChildAt(j).requestFocus();
                                ((EditText) child.getChildAt(j)).setSelection(focuslocation);
                                return 0;
                            }
                        }
                    }
                }
            }
        }
        return focusId;
    }
    /* }@ */

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        int pos = savedInstanceState.getInt(TINT_POS);
        final Spinner tintSpinner = (Spinner) mDialogLayout.findViewById(R.id.spinner);
        tintSpinner.setSelection(pos);
        mTintSelectorPos = pos;

        EditText nameText = (EditText)mDialogLayout.findViewById(R.id.sim_name);
        nameText.setText(savedInstanceState.getString(SIM_NAME));
        /* SPRD: modify for bug493417 @{ */
        final EditText numberText = (EditText)mDialogLayout.findViewById(R.id.display_number);
        numberText.setText(savedInstanceState.getString(DISPLAY_NUMBER));
        /* @} */
        /* SPRD: modify for bug496697 @{ */
        // SPRD: Del for bug 518820
        findFocus(mSimEditLayout, savedInstanceState);
        /* }@ */
    }

    private void createEditDialog(Bundle bundle) {
        final Resources res = mContext.getResources();
        EditText nameText = (EditText)mDialogLayout.findViewById(R.id.sim_name);
        nameText.setText(mSubInfoRecord.getDisplayName());

        final Spinner tintSpinner = (Spinner) mDialogLayout.findViewById(R.id.spinner);
        SelectColorAdapter adapter = new SelectColorAdapter(mContext,
                R.layout.settings_color_picker_item, mColorStrings);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tintSpinner.setAdapter(adapter);

        for (int i = 0; i < mTintArr.length; i++) {
            if (mTintArr[i] == mSubInfoRecord.getIconTint()) {
                tintSpinner.setSelection(i);
                mTintSelectorPos = i;
                break;
            }
        }

        tintSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id){
                tintSpinner.setSelection(pos);
                mTintSelectorPos = pos;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        final TelephonyManager tm = (TelephonyManager) mContext.getSystemService(
                Context.TELEPHONY_SERVICE);
        TextView numberView = (TextView)mDialogLayout.findViewById(R.id.number);
        /* SPRD: add option edit sim card's number @{ */
        numberView.setVisibility(View.GONE);
        EditText displayNumberView = (EditText)mDialogLayout.findViewById(R.id.display_number);
        displayNumberView.setInputType(InputType.TYPE_CLASS_PHONE);
        final String rawNumber =  tm.getLine1NumberForSubscriber(
                mSubInfoRecord.getSubscriptionId());
        if (TextUtils.isEmpty(rawNumber)) {
            displayNumberView.setText(res.getString(com.android.internal.R.string.unknownName));
        } else {
            displayNumberView.setText(PhoneNumberUtils.formatNumber(rawNumber));
        }
        /* @} */

        String simCarrierName = tm.getSimOperatorNameForSubscription(mSubInfoRecord
                .getSubscriptionId());
        TextView carrierView = (TextView)mDialogLayout.findViewById(R.id.carrier);
        carrierView.setText(!TextUtils.isEmpty(simCarrierName) ? simCarrierName :
                mContext.getString(com.android.internal.R.string.unknownName));
                
        /*wangxing 20160628 modify for rename sim1 sim2 start @{ */
		String s = String.format(getResources().getString(R.string.sim_editor_title), (mSlotId + 1));
        s = s.replace("1","M");
        s = s.replace("2","S");
        if(OptConfig.SUNVOV_CUSTOM_C7301_XLL_FWVGA){
        	mBuilder.setTitle(s);
        }else{        
	        mBuilder.setTitle(String.format(res.getString(R.string.sim_editor_title),
	                (mSubInfoRecord.getSimSlotIndex() + 1)));
	    }
	    /*wangxing 20160628 modify for rename sim1 sim2 end @} */

        mBuilder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                final EditText nameText = (EditText)mDialogLayout.findViewById(R.id.sim_name);

                String displayName = nameText.getText().toString();
                /* SPRD: modify for bug492214 @{ */
                boolean isEmpty = displayName.isEmpty();
                if (isEmpty) {
                    Log.d(TAG, "user input displayname is null.");
                    displayName = "SIM" + (mSubInfoRecord.getSimSlotIndex() + 1);
                }
                /* @} */
                int subId = mSubInfoRecord.getSubscriptionId();
                mSubInfoRecord.setDisplayName(displayName);
                /* SPRD: modify for bug492214 @{ */
                mSubscriptionManager.setDisplayName(displayName, subId,
                      isEmpty ? SubscriptionManager.NAME_SOURCE_USER_INPUT_NULL : SubscriptionManager.NAME_SOURCE_USER_INPUT);
                /* @} */

                final int tintSelected = tintSpinner.getSelectedItemPosition();
                int tint = mTintArr[tintSelected];
                mSubInfoRecord.setIconTint(tint);
                mSubscriptionManager.setIconTint(tint, subId);

                /* SPRD: add option edit sim card's number @{ */
                final EditText numberText = (EditText)mDialogLayout.findViewById(R.id.display_number);
                String displayNumber = numberText.getText().toString();
                TelephonyManager tm = (TelephonyManager)
                        mContext.getSystemService(Context.TELEPHONY_SERVICE);;
                Log.d(TAG, "displayNumber = " + displayNumber + " subId = " + subId);
                if (displayNumber!= null) {
                    tm.setLine1NumberForDisplayForSubscriberEx(subId, "phoneNumber", displayNumber);
                    mSubInfoRecord.setNumber(displayNumber);
                    mSubscriptionManager.setDisplayNumber(displayNumber,subId);
                    SubscriptionInfo subInfo = mSubscriptionManager.getActiveSubscriptionInfo(subId);
                    if (subInfo != null ) {
                        subInfo.setNumber(displayNumber);
                    }
                }
                /* @} */

                dialog.dismiss();
            }
        });

        mBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        /* SPRD: modify for AOB bug496719 @{ */
        AlertDialog dialog = mBuilder.create();

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.d(TAG, "onDismiss finish activity!");
                finish();
            }
        });
        dialog.show();
        /* @} */
    }

    private class SelectColorAdapter extends ArrayAdapter<CharSequence> {
        private Context mContext;
        private int mResId;

        public SelectColorAdapter(
                Context context, int resource, String[] arr) {
            super(context, resource, arr);
            mContext = context;
            mResId = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)
                    mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView;
            final ViewHolder holder;
            Resources res = mContext.getResources();
            int iconSize = res.getDimensionPixelSize(R.dimen.color_swatch_size);
            int strokeWidth = res.getDimensionPixelSize(R.dimen.color_swatch_stroke_width);

            if (convertView == null) {
                // Cache views for faster scrolling
                rowView = inflater.inflate(mResId, null);
                holder = new ViewHolder();
                ShapeDrawable drawable = new ShapeDrawable(new OvalShape());
                drawable.setIntrinsicHeight(iconSize);
                drawable.setIntrinsicWidth(iconSize);
                drawable.getPaint().setStrokeWidth(strokeWidth);
                holder.label = (TextView) rowView.findViewById(R.id.color_text);
                holder.icon = (ImageView) rowView.findViewById(R.id.color_icon);
                holder.swatch = drawable;
                rowView.setTag(holder);
            } else {
                rowView = convertView;
                holder = (ViewHolder) rowView.getTag();
            }

            holder.label.setText(getItem(position));
            holder.swatch.getPaint().setColor(mTintArr[position]);
            holder.swatch.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
            holder.icon.setVisibility(View.VISIBLE);
            holder.icon.setImageDrawable(holder.swatch);
            return rowView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View rowView = getView(position, convertView, parent);
            final ViewHolder holder = (ViewHolder) rowView.getTag();

            if (mTintSelectorPos == position) {
                holder.swatch.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
            } else {
                holder.swatch.getPaint().setStyle(Paint.Style.STROKE);
            }
            holder.icon.setVisibility(View.VISIBLE);
            return rowView;
        }

        private class ViewHolder {
            TextView label;
            ImageView icon;
            ShapeDrawable swatch;
        }
    }
}
