/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */
package com.ucamera.ucomm.sns;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class ShareProgress {

    private Context mContext;
    private AlertDialog mDialog;
    private CheckBox[] mItems;

    public ShareProgress(Context context, CharSequence[] items) {
        mContext = context;

        LinearLayout contentView = new LinearLayout(mContext);
        contentView.setOrientation(LinearLayout.VERTICAL);

        mItems = new CheckBox[items.length];
        for (int i = 0; i < items.length; i++) {
            mItems[i] = new CheckBox(context);
            mItems[i].setClickable(false);
            mItems[i].setText(items[i]);
            mItems[i].setButtonDrawable(Status.TODO.getIcon());
            contentView.addView(mItems[i]);
        }

        ScrollView container = new ScrollView(context);
        container.addView(contentView);
        mDialog = new AlertDialog.Builder(context)
                    .setView(container)
                    .setTitle(R.string.sns_title_share_to)
                    .setPositiveButton(android.R.string.cancel, null)
                    .create();
    }

    public AlertDialog getDialog(){
        return mDialog;
    }

    public void update(Item item) {
        Drawable drawable = mDialog.getContext().getResources().getDrawable(item.mStatus.getIcon());
        mItems[item.mIndex].setButtonDrawable(drawable);
        if (drawable instanceof AnimationDrawable){
            ((AnimationDrawable)drawable).start();
        }
    }

    enum Status {
        TODO(R.drawable.sns_ic_todo),
        DOING(R.drawable.sns_ic_doing),
        DONE(R.drawable.sns_ic_done),
        FAIL(R.drawable.sns_ic_fail);

        private final int mIcon;
        private Status(int v){ mIcon = v;}
        private int getIcon() { return this.mIcon; }
        public boolean isFail() {return this == FAIL; }
        public boolean isDone() {return this == Status.DONE;}
    }

    public static class Item {
        Item(int i, Status status){
            mIndex = i;
            mStatus = status;
        }
        int mIndex;
        Status mStatus;
    }
}
