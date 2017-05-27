/* create by Spreadst */

package com.sprd.calendar;

import com.android.calendar.R;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/* SPRD: bug290535 2014-03-19 New event title can not be too long */
public class SprdTextWatcher implements TextWatcher {

    private Context mContext;
    private TextView mEditText;
    private int mMax;
    private View mAddEventView;
    private String mCostStr;

    /* SPRD : strengthen the title length control */
    public SprdTextWatcher(Context c, TextView textview, int max) {
        this.mContext = c;
        this.mEditText = textview;
        this.mMax = max;
    }

    public SprdTextWatcher(Context c, TextView textview, View view, int max) {
        this.mContext = c;
        this.mEditText = textview;
        this.mAddEventView = view;
        this.mMax = max;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        mCostStr = s.toString();
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (mAddEventView != null) {
            mAddEventView.setEnabled(s.length() > 0);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        showMessage(s.toString(), mCostStr, mEditText, mMax);
    }

    /**
     * if new string's length greater than max, the string revert old.
     *
     * @param text new string
     * @param costStr old string
     * @param editText input view
     * @param max Max length
     */
    /* SPRD: Add 20130829 of bug 209021,limit text length @{ */
    public void showMessage(String text, String costStr, TextView textview, int max) {
        Toast textLenghtToast = null;
        if (text.length() > max) {
            int index = textview.getSelectionStart();
            int temp = text.length() - costStr.length();
            int costLenght = costStr.length();
            if (costLenght > max) {
                costStr = costStr.substring(0, max);
            } else if (costLenght < max) {
                costStr = text.substring(0, max);
                temp = costStr.length() - costLenght;
            }
            if (index == costLenght) {
                index = costStr.length();
            } else if (costLenght == costStr.length()) {
                index = index - temp;
            }
            if (index > max) {
                index = max;
            } else if (index < 0) {
                index = 0;
            }
            textview.setText(costStr);
            ((EditText) textview).setSelection(index);
            textLenghtToast = Toast.makeText(mContext, R.string.title_too_long,
                    Toast.LENGTH_SHORT);
            textLenghtToast.show();
        }
    }
    /* @} */
}
