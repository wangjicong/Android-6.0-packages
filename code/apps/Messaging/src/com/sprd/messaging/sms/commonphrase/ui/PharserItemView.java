
package com.sprd.messaging.sms.commonphrase.ui;

import com.android.messaging.R;
import com.sprd.messaging.sms.commonphrase.model.ItemData;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PharserItemView extends LinearLayout {

    public PharserItemView(Context context) {
        super(context);
    }

    public PharserItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressLint("NewApi")
    public PharserItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void onFinishInflate() {
        mTextView = (TextView) findViewById(R.id.text_view);
        mCheckbox = (CheckBox) findViewById(R.id.checkbox);

        Log.e(TAG, "=====onFinishInflate");

    }

    public void init() {
        Log.e(TAG, "=====init");
        getTextView().setText(getUserData().getPharser());

        if (mCheckbox == null) {
            Log.e(TAG, "System not fond control", new Exception(" value is NULL "));
        }
    }

    public ItemData getUserData() {
        Object obj = getTag();
        if (obj instanceof ItemData) {
            return (ItemData) obj;
        } else {
            return null;
        }
    }

    public TextView getTextView() {
        return mTextView;
    }

    public CheckBox getCheckBox() {
        return mCheckbox;
    }

    private TextView mTextView;
    private CheckBox mCheckbox;
    private static String TAG = "PharserItemView";

}
