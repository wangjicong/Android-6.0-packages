/**
 * Add for navigation tab
 *@{
 */

package com.android.browser;

import com.android.browser.DefaultHomeView.GridAdapter;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Adapter;
import android.widget.GridView;

import java.util.IllegalFormatCodePointException;
import com.android.browser.util.Util;

public class HomeGridView extends GridView {
    public HomeGridView(Context context) {
        super(context);
        setSelector(android.R.color.transparent);
    }

    public HomeGridView(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.gridViewStyle);
        setSelector(android.R.color.transparent);
    }

    public HomeGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setSelector(android.R.color.transparent);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Adapter adapter = getAdapter();
        if(adapter != null && adapter instanceof GridAdapter){
            ((GridAdapter)adapter).refreshData();
        }
    }
}
