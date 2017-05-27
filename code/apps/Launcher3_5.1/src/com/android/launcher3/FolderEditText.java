package com.android.launcher3;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

/* SPRD: bug399384 2015-02-02 Feature limit folder title max length. @{ */
import java.util.ArrayList;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.Toast;
/* SPRD: bug399384 2015-02-02 Feature limit folder title max length. @} */

public class FolderEditText extends EditText {

    private Folder mFolder;

    public FolderEditText(Context context) {
        super(context);
    }

    public FolderEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FolderEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setFolder(Folder folder) {
        mFolder = folder;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        // Catch the back button on the soft keyboard so that we can just close the activity
        if (event.getKeyCode() == android.view.KeyEvent.KEYCODE_BACK) {
            mFolder.doneEditingFolderName(true);
        }
        return super.onKeyPreIme(keyCode, event);
    }

    /* SPRD: bug399384 2015-02-02 Feature limit folder title max length. @{ */
    public void promptIfLargerThanLength(int maxLen, int resId) {
        ArrayList<InputFilter> arrayFilters = new ArrayList<InputFilter>();
        InputFilter[] filters = getFilters();
        if (filters != null && filters.length > 0) {
            for (InputFilter filter : filters) {
                if (!(filter instanceof InputFilter.LengthFilter)) {
                    arrayFilters.add(filter);
                }
            }
        }
        arrayFilters.add(new ToastLengthInputFilter(maxLen, getContext(), resId));
        InputFilter[] finalFilters = new InputFilter[arrayFilters.size()];
        setFilters(arrayFilters.toArray(finalFilters));
    }

    private class ToastLengthInputFilter extends InputFilter.LengthFilter {
        private int mResId;
        private Context mContext;

        public ToastLengthInputFilter(int max, Context context, int resId) {
            super(max);
            mContext = context;
            mResId = resId;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                Spanned dest, int dstart, int dend) {
            CharSequence result = super.filter(source, start, end, dest, dstart, dend);
            if (result != null) {
                Toast.makeText(mContext, mResId, Toast.LENGTH_SHORT).show();
            }
            return result;
        }
    }
    /* SPRD: bug399384 2015-02-02 Feature limit folder title max length. @} */
}
