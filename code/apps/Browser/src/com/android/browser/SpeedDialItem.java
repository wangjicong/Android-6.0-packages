/**
 * Add for navigation tab
 *@{
 */

package com.android.browser;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class SpeedDialItem extends LinearLayout {

    private String mUrl;

    private int mId;

    public SpeedDialItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SpeedDialItem(Context context) {
        super(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setDialId(int id){
        mId = id;
    }

    public int getDialId(){
        return mId;
    }

    public void setUrl(String url){
        mUrl = url;
    }

    public String getUrl(){
        return mUrl;
    }
}
