/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */
package com.ucamera.ucomm.sns;

import org.scribe.model.Token;
import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.ucamera.ucomm.sns.services.LoginListener;
import com.ucamera.ucomm.sns.services.LogoutListener;
import com.ucamera.ucomm.sns.services.ServiceProvider;
import com.ucamera.ucomm.sns.services.ShareService;


public class AccountsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sns_account_settings);
        ((TextView)findViewById(R.id.sns_account_title)).setText(R.string.sns_title_account_settings);
        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
        params.bottomMargin = getResources().getDimensionPixelSize(R.dimen.sns_margin)/2;
        if(ShareActivity.mShowTencentShare) {
            Button btn_back = (Button) findViewById(R.id.sns_account_btn_back);
            btn_back.setVisibility(View.VISIBLE);
            btn_back.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    AccountsActivity.this.finish();
                }
            });
        }

        LinearLayout itemsContainer = (LinearLayout)findViewById(R.id.sns_account_settings_items);
        for (ShareItem s : ShareItem.sortedAccountValues()) {
            View itemView = getLayoutInflater().inflate(R.layout.sns_account_settings_item, null);
            itemView.setLayoutParams(params);
            TextView item = (TextView) itemView.findViewById(R.id.sns_item_label);
            item.setText(s.getLabel());
            item.setCompoundDrawablesWithIntrinsicBounds(s.getIcon(), 0, 0, 0);
            itemsContainer.addView(itemView);
            final TextView textViewBind = (TextView) itemView.findViewById(R.id.sns_item_bind);
            final ShareService service = ServiceProvider.getProvider().getService(s.getService());
            service.loadSession(this);

            final boolean isVatar = service.getServiceName().equals("QQVatar");
            if ((!isVatar && service.isAuthorized()) || (isVatar && service.isAuthorized(this))) {
                bind(textViewBind, item, s);
            } else {
                unbind(textViewBind, item, s);
            }

            itemView.setOnClickListener(new ViewOnClickListener(service, isVatar, textViewBind, item, s));
//            itemView.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if ((!isVatar && service.isAuthorized()) || (isVatar && service.isAuthorized(AccountsActivity.this))) {
//                        service.logout(AccountsActivity.this, new LogoutListener() {
//                            public void onSuccess() {
//                                unbind(textViewBind);
//                            }
//                            public void onFail() {}
//                        });
//                    } else {
//                        service.login(AccountsActivity.this, new LoginListener() {
//                            public void onCancel() {}
//                            public void onSuccess(Token accessToken) {
//                                if (accessToken != null){
//                                    bind(textViewBind);
//                                    service.saveSession(AccountsActivity.this);
//                                }
//                            }
//                            public void onFail(String error) {}
//                        });
//                    }
//                }
//            });
        }
    }

    class ViewOnClickListener implements OnClickListener {
        private ShareService service;
        private boolean isVatar;
        private TextView textViewBind;
        private TextView iconViewBind;
        private ShareItem shareItem;
        public ViewOnClickListener (ShareService service, boolean isVatar, TextView textViewBind, TextView iconViewBind, ShareItem shareItem) {
            this.service = service;
            this.isVatar = isVatar;
            this.textViewBind = textViewBind;
            this.iconViewBind = iconViewBind;
            this.shareItem = shareItem;
        }
        @Override
        public void onClick(View v) {
            if ((!isVatar && service.isAuthorized()) || (isVatar && service.isAuthorized(AccountsActivity.this))) {
                service.logout(AccountsActivity.this, new LogoutListener() {
                    public void onSuccess() {
                        unbind(textViewBind, iconViewBind, shareItem);
                    }
                    public void onFail() {}
                });
            } else {
                service.login(AccountsActivity.this, new LoginListener() {
                    public void onCancel() {}
                    public void onSuccess(Token accessToken) {
                        if (accessToken != null){
                            bind(textViewBind, iconViewBind, shareItem);
                            service.saveSession(AccountsActivity.this);
                        }
                    }
                    public void onFail(String error) {}
                });
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
//        StatApi.onResume(this);
    }

    /*
     * Do NOT remove, used for Stat
     * (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();
//        StatApi.onPause(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void bind(TextView item, TextView icon, ShareItem shareItem){
        item.setTextAppearance(this, R.style.SNSbindedTextView);
        item.setText(R.string.sns_label_unbind);
        icon.setCompoundDrawablesWithIntrinsicBounds(shareItem.getSelectedIcon(), 0, 0, 0);
    }

    private void unbind(TextView item, TextView iconView, ShareItem shareItem){
        item.setTextAppearance(this, R.style.SNSunbindTextView);
        item.setText(R.string.sns_label_bind);
        iconView.setCompoundDrawablesWithIntrinsicBounds(shareItem.getIcon(), 0, 0, 0);
    }
}
