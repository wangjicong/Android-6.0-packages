/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.messaging.ui;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import android.app.Activity;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.MessagingContentProvider;
import com.android.messaging.datamodel.binding.Binding;
import com.android.messaging.datamodel.binding.BindingBase;
import com.android.messaging.datamodel.data.LaunchConversationData;
import com.android.messaging.util.ContentType;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.OsUtil;
import com.android.messaging.util.UiUtils;
import com.android.messaging.util.UriUtil;
import android.app.ActivityManager;
import android.app.Activity;

/**
 * Activity to check if the user has required permissions. If not, it will try to prompt the user
 * to grant permissions. However, the OS may not actually prompt the user if the user had
 * previously checked the "Never ask again" checkbox while denying the required permissions.
 */
public class PermissionCheckActivity extends Activity implements
LaunchConversationData.LaunchConversationDataListener {
    private static final int REQUIRED_PERMISSIONS_REQUEST_CODE = 1;
    private static final long AUTOMATED_RESULT_THRESHOLD_MILLLIS = 250;
    private static final String PACKAGE_URI_PREFIX = "package:";
    private long mRequestTimeMillis;
    private TextView mNextView;
    private TextView mSettingsView;
    static final String SMS_BODY = "sms_body";
    static final String ADDRESS = "address";
    static final String CMP = "component_name";
    final Binding<LaunchConversationData> mBinding = BindingBase.createBinding(this);
    String mSmsBody;
    private static PermissionCheckActivity mLastActivity = null;
    private String TAG = "PermissionCheckActivity";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ActivityManager.isUserAMonkey()) {
            Log.i(TAG, " onCreate() monkey------>> mLastActivity."
                    + mLastActivity);
            if (mLastActivity != null) {
                mLastActivity.finish();
            }
            mLastActivity = this;
        }

        if (redirectIfNeeded()) {
            return;
        }

        setContentView(R.layout.permission_check_activity);
        UiUtils.setStatusBarColor(this, getColor(R.color.permission_check_activity_background));

        findViewById(R.id.exit).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                finish();
            }
        });

        mNextView = (TextView) findViewById(R.id.next);
        mNextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                tryRequestPermission();
            }
        });

        mSettingsView = (TextView) findViewById(R.id.settings);
        mSettingsView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse(PACKAGE_URI_PREFIX + getPackageName()));
                startActivity(intent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (redirectIfNeeded()) {
            return;
        }
    }

    private void tryRequestPermission() {
        final String[] missingPermissions = OsUtil.getMissingRequiredPermissions();
        if (missingPermissions.length == 0) {
            //redirect();
            return;
        }

        mRequestTimeMillis = SystemClock.elapsedRealtime();
        requestPermissions(missingPermissions, REQUIRED_PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String permissions[], final int[] grantResults) {
        if (requestCode == REQUIRED_PERMISSIONS_REQUEST_CODE) {
            // We do not use grantResults as some of the granted permissions might have been
            // revoked while the permissions dialog box was being shown for the missing permissions.
            if (OsUtil.hasRequiredPermissions()) {
                Factory.get().onRequiredPermissionsAcquired();
                //redirect();
                redirect2Conversation();
            } else {
                final long currentTimeMillis = SystemClock.elapsedRealtime();
                // If the permission request completes very quickly, it must be because the system
                // automatically denied. This can happen if the user had previously denied it
                // and checked the "Never ask again" check box.
                if ((currentTimeMillis - mRequestTimeMillis) < AUTOMATED_RESULT_THRESHOLD_MILLLIS) {
                    mNextView.setVisibility(View.GONE);

                    mSettingsView.setVisibility(View.VISIBLE);
                    findViewById(R.id.enable_permission_procedure).setVisibility(View.VISIBLE);
                }
            }
        }
    }

    /** Returns true if the redirecting was performed */
    private boolean redirectIfNeeded() {
        if (!OsUtil.hasRequiredPermissions()) {
            return false;
        }
        Log.i("PermissionCheckActivity", "redirectIfNeeded");
        //redirect();
        redirect2Conversation();
        return true;
    }

    private void redirect() {
        Log.i("PermissionCheckActivity", "redirect");
        UIIntents.get().launchConversationListActivity(this);
        finish();
    }

    private void redirect2Conversation() {
        Log.i("PermissionCheckActivity", "redirect2Conversation");
        final Intent intent = getIntent();
        final String action = intent.getAction();
        Log.i("newIntent", "intent: " + intent);
        if (Intent.ACTION_SENDTO.equals(action) || Intent.ACTION_VIEW.equals(action)) {
            String[] recipients = UriUtil.parseRecipientsFromSmsMmsUri(intent.getData());
            final boolean haveAddress = !TextUtils.isEmpty(intent.getStringExtra(ADDRESS));
            final boolean haveEmail = !TextUtils.isEmpty(intent.getStringExtra(Intent.EXTRA_EMAIL));
            if (recipients == null && (haveAddress || haveEmail)) {
                if (haveAddress) {
                    recipients = new String[] { intent.getStringExtra(ADDRESS) };
                } else {
                    recipients = new String[] { intent.getStringExtra(Intent.EXTRA_EMAIL) };
                }
            }
            mSmsBody = intent.getStringExtra(SMS_BODY);
            if (TextUtils.isEmpty(mSmsBody)) {
                // Used by intents sent from the web YouTube (and perhaps others).
                mSmsBody = getBody(intent.getData());
                if (TextUtils.isEmpty(mSmsBody)) {
                    // If that fails, try yet another method apps use to share text
                    if (ContentType.TEXT_PLAIN.equals(intent.getType())) {
                        mSmsBody = intent.getStringExtra(Intent.EXTRA_TEXT);
                    }
                }
            }
            if (recipients != null) {
                if (!mBinding.isBound()) {
                    mBinding.bind(DataModel.get().createLaunchConversationData(this));
                    mBinding.getData().getOrCreateConversation(mBinding,recipients);
                }
            } else {
                // No recipients were specified in the intent.
                // Start a new conversation with contact picker. The new conversation will be
                // primed with the (optional) message in mSmsBody.
                onGetOrCreateNewConversation(null);
            }
        } else if(intent.getParcelableExtra(CMP) != null) {
            intent.setComponent((ComponentName)intent.getParcelableExtra(CMP));
            //startActivity(intent);
            if(intent.getData() != null) {
                String[] tmps = intent.getData().toString().split("/");
                UIIntents.get().launchConversationActivityNewTask(this, tmps[tmps.length-1]);
            }else
                redirect();
        }else {
            redirect();
            LogUtil.w(LogUtil.BUGLE_TAG, "Unsupported conversation intent action : " + action);
        }
        finish();
    }

    private String getBody(final Uri uri) {
        if (uri == null) {
            return null;
        }
        String urlStr = uri.getSchemeSpecificPart();
        if (!urlStr.contains("?")) {
            return null;
        }
        urlStr = urlStr.substring(urlStr.indexOf('?') + 1);
        final String[] params = urlStr.split("&");
        for (final String p : params) {
            if (p.startsWith("body=")) {
                try {
                    return URLDecoder.decode(p.substring(5), "UTF-8");
                } catch (final UnsupportedEncodingException e) {
                    // Invalid URL, ignore
                }
            }
        }
        return null;
    }

    @Override
    public void onGetOrCreateNewConversation(final String conversationId) {
        final Context context = Factory.get().getApplicationContext();
        UIIntents.get().launchConversationActivityWithParentStack(context, conversationId,
                mSmsBody);
    }

    @Override
    public void onGetOrCreateNewConversationFailed() {
        UiUtils.showToastAtBottom(R.string.conversation_creation_failure);
    }

    /* SPRD 534913 @{ */
    @Override
    public void onBackPressed() {
        Log.i("PermissionCheckActivity", "Override onBackPressed ");
        finish();
    }
    /* @} */

    @Override
    protected void onDestroy() {
        if (OsUtil.hasSmsPermission()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    MessagingContentProvider.onSimMessageChage();
                }
            }).start();
        }
        super.onDestroy();
    }
}
