/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */
package com.ucamera.ucomm.sns;

import org.scribe.model.Token;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import com.ucamera.ucomm.sns.services.LoginListener;
import com.ucamera.ucomm.sns.services.ServiceProvider;
import com.ucamera.ucomm.sns.services.ShareService;

public class ShareItemView extends CompoundButton
        implements LoginListener, View.OnClickListener {

    private ShareService mShareService;

    public ShareItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ShareItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ShareItemView(Context context) {
        super(context);
    }

    public static ShareItemView create(Context context, ShareItem item,ViewGroup group) {
        LayoutInflater inflater = (LayoutInflater)
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View tmp = inflater.inflate(R.layout.sns_share_item, group);
        ShareItemView view= (ShareItemView)tmp.findViewById(R.id.sns_item);
        if (item != null) {
            view.setId(item.getId());
            view.setText(item.getLabel());
            view.setCompoundDrawablesWithIntrinsicBounds(0, item.getIcon(), 0, 0);
            view.setOnClickListener(view);
            view.setup(ServiceProvider.getProvider().getService(item.getService()));
        }
        return view;
    }


    public ShareService getShareService() {
        return this.mShareService;
    }

    private void setup(ShareService shareService) {
        this.mShareService = shareService;
        if (shareService != null) {
            this.mShareService.loadSession(getContext());
        }
    }

    @Override
    public void onFail(String error) {
        this.setChecked(false);
        Util.showAlert(getContext(),
                getContext().getString(R.string.sns_title_login),
                getContext().getString(R.string.sns_msg_fail_login));
    }

    @Override
    public void onCancel() {
        this.setChecked(false);
    }

    @Override
    public void onSuccess(Token accessToken) {
        this.setChecked(accessToken != null);
        if (mShareService != null) {
            mShareService.saveSession(getContext());
        }
    }

    @Override
    public void onClick(View v) {
        if (v instanceof ShareItemView) {
            ShareItemView itemView = (ShareItemView) v;
            if (!itemView.getShareService().isAuthorized()) {
                itemView.getShareService().login(v.getContext(), itemView);
            }
        }
    }
}
