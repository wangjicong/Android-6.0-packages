/*
 * Copyright (C) 2014,2015 Thundersoft Corporation
 * All rights Reserved
 */
package com.ucamera.ucomm.sns;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class NetPrintDialog {
    private Context mContext;
    private int mResources;
    private String mType;
    private Dialog dialog;
    private LayoutInflater mLayoutInflater;
    protected View view;
    public static final String TOLOT = "TOLOT";
    public static final String SHI = "SHI";
    private final String SHI_URL = UphotoKddiDownloadUrl.SHI_DOWNLOAD_URL;
    private final String TOLOT_URL = UphotoKddiDownloadUrl.TOLOT_DOWNLOAD_URL;

    public NetPrintDialog(Context context, /*int resources,*/ String type) {
        this.mContext = context;
        this.mType = type;
        this.mResources = R.layout.net_print_details;
        this.mLayoutInflater = LayoutInflater.from(mContext);
    }
    public void showDialog() {
        view = mLayoutInflater.inflate(mResources, null);
        dialog = new Dialog(mContext, R.style.NetPrintDialog);
        dialog.setContentView(view);
        dialog.show();
        setViewListener();
    }
    private void setViewListener() {
        TextView tv_message = (TextView) view.findViewById(R.id.net_print_message);
        Button btn_cancel = (Button) view.findViewById(R.id.btn_cancel);
        Button btn_confirm = (Button) view.findViewById(R.id.btn_confirm);

        if(mType.equals(SHI)) {
            tv_message.setText(R.string.sns_net_print_shi_no_message);
        } else if(mType.equals(TOLOT)) {
            tv_message.setText(R.string.sns_net_print_tolot_no_message);
        }
        btn_cancel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                doNegative();
            }
        });

        btn_confirm.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                doPositive(mType);
            }
        });
    }
    private void doNegative(){
        dialog.dismiss();
    }
    private void doPositive(String type){
        if(type.equals(TOLOT)) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent .setData(Uri.parse(TOLOT_URL));
            mContext.startActivity(intent);
        } else if(type.equals(SHI)) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent .setData(Uri.parse(SHI_URL));
            mContext.startActivity(intent);
        }
        dialog.dismiss();
    }
}
