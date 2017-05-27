package com.ucamera.ucomm.sns;
/*
 * Copyright (C) 2011,2014 Thundersoft Corporation
 * All rights Reserved
 */
import java.util.ArrayList;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import com.ucamera.ucomm.sns.R;
public class IDphotoPrintDialog extends Dialog implements View.OnClickListener{
    private static final String TAG = "IDphotoPrintDialog";
    private Activity mActivity;
    private View mContentView;
    private Handler mHandler;
    private ArrayList<String> mList;
    public IDphotoPrintDialog(Activity activity) {
        super(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        mActivity = activity;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.idphoto_print_dialog);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        default:
            break;
        }
        dismiss();
    }
    @Override
    public void onBackPressed() {
        dismiss();
    }
}
