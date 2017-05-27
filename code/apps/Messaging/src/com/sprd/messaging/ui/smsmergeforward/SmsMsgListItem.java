package com.sprd.messaging.ui.smsmergeforward;

import com.sprd.messaging.ui.smsmergeforward.MutiSelectCursorAdapter.Checkable;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.util.Log;

import com.android.messaging.R;
import com.android.messaging.datamodel.data.MessageData;

public class SmsMsgListItem extends RelativeLayout implements Checkable {
    private TextView tv_address;
    private TextView tv_date;
    private TextView tv_body;
    private ImageView mImageView;
    private SmsMessageItem mSmsMessageItem;
    private int mPosition;
    private Drawable mDefaultBackupround;

    public SmsMsgListItem(Context context) {
        super(context);
    }

    public SmsMsgListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        tv_address = (TextView) findViewById(R.id.msg_address);
        tv_date = (TextView) findViewById(R.id.msg_date);
        tv_body = (TextView) findViewById(R.id.msg_body);
        mImageView = (ImageView) findViewById(R.id.msg_type_image);
        mDefaultBackupround = getBackground();
    }

    public void bind(SmsMessageItem smsMessageItem, int position) {
        mSmsMessageItem = smsMessageItem;
        mPosition = position;
        fillItem();
        setImageViewResource(mImageView, getSmsTypeFlag());
    }

    @Override
    public int getCheckedPosition() {
        return mPosition;
    }

    public void fillItem() {
        tv_address.setText(mSmsMessageItem.mAddress);
        tv_date.setText(mSmsMessageItem.mtimeStamp);
        tv_body.setText(mSmsMessageItem.mBody);
    }

    private int getSmsTypeFlag() {
        int resId;
        if (mSmsMessageItem.mType == MessageData.BUGLE_STATUS_INCOMING_COMPLETE) {
            resId = R.drawable.msg_readed;
        } else if (mSmsMessageItem.mType == MessageData.BUGLE_STATUS_OUTGOING_COMPLETE || mSmsMessageItem.mType == MessageData.BUGLE_STATUS_OUTGOING_DELIVERED) {
            resId = R.drawable.ic_sent;
        } else {
            resId = R.drawable.ic_outbox;
        }
        return resId;
    }

    private void setImageViewResource(ImageView imageView, int resId) {
        if (imageView != null && resId != -1) {
            imageView.setBackgroundResource(resId);
        }
    }

    @Override
    public void setChecked(boolean isSelectMode, boolean isChecked) {
        Log.d("SmsMsgListItem","=======sms merge forward=====isSelectMode: "+isSelectMode+"    isChecked: "+isChecked);
        if (!isSelectMode) {
            setOnClickListener(null);
            setClickable(false);
        }

        int backgroundId;
        if (isSelectMode && isChecked) {
            backgroundId = getResources().getColor(
                    R.color.convresation_item_selected_background);
            tv_body.setTextColor(getResources().getColor(
                    R.color.convresation_item_content_selected));
            tv_date.setTextColor(getResources().getColor(
                    R.color.convresation_item_content_selected));
            tv_address.setTextColor(getResources().getColor(
                    R.color.convresation_item_content_selected));

            setBackgroundColor(backgroundId);
        } else {
            tv_body.setTextColor(getResources().getColor(R.color.section_title));
            tv_date.setTextColor(getResources().getColor(R.color.section_title));
            tv_address.setTextColor(getResources().getColor(
                    R.color.convresation_item_from_read));
            setBackground(mDefaultBackupround);
        }
    }
}
