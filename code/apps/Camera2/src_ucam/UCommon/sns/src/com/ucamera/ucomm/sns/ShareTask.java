/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */
package com.ucamera.ucomm.sns;

import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.ucamera.ucomm.sns.services.ShareContent;
import com.ucamera.ucomm.sns.services.ShareError;
import com.ucamera.ucomm.sns.services.ShareFile;
import com.ucamera.ucomm.sns.services.ShareService;
import com.ucamera.ucomm.stat.StatApi;

class ShareTask extends AsyncTask<Void, ShareProgress.Item, Void> {
    private static String TAG = "ShareTask";

    private ShareProgress progress;

    private Context mContext;
    private List<ShareItemView> mShareTargets;
    private ShareContent mShareContent;
    private ShareFile mShareFile;

    ShareTask(Context context, List<ShareItemView> targets, ShareContent content, ShareFile file){
        mContext = context;
        mShareTargets = targets;
        mShareContent = content;
        mShareFile = file;
    }

    @Override
    protected void onPreExecute() {
        CharSequence[] items = new CharSequence[mShareTargets.size()];
        for (int i = 0; i < items.length; i++) {
            items[i] = mShareTargets.get(i).getText();
        }
        progress = new ShareProgress(mContext, items);
        progress.getDialog().setCancelable(false);
        progress.getDialog().setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                ShareTask.this.cancel(true);
            }
        });
        progress.getDialog().show();
    }

    @Override
    protected void onProgressUpdate(ShareProgress.Item... values) {
        if (values == null || values.length == 0 || values[0] == null){
            return;
        }
        if (progress != null){
            progress.update(values[0]);
        }

        ShareService service = mShareTargets.get(values[0].mIndex).getShareService();
        if (service != null ){
            if (values[0].mStatus.isFail() ) {
                try {
                    ShareError error = service.getShareError();
                    if (error != null) {
                        CharSequence msg = translateErrorCode(error.getCode());
                        if (msg == null){
                            msg = error.getMessage();
                        }
                        if (msg != null){
                            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                }catch (Exception e) {
                    // IGNORE
                }
            }
            /*
             * BUG COMMENT: stat is not correct, only share success to stat
             * Date: 2014-03-10
             */
            if(values[0].mStatus.isDone()) {
                StatApi.onEvent(mContext, StatApi.EVENT_SNS_SHARE, service.getServiceName());
            }
        }
    }

    @Override
    protected void onPostExecute(Void result) {
        Button button = progress.getDialog().getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setText(android.R.string.ok);
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        for (int i = 0; i < mShareTargets.size(); i++) {
            if (isCancelled()) {
                break;
            }

            ShareItemView item = mShareTargets.get(i);
            Log.d(TAG, "upload to: " + item.getText());
            publishProgress(new ShareProgress.Item(i, ShareProgress.Status.DOING));

            if (!item.getShareService().isAuthorized()) {
                publishProgress(new ShareProgress.Item(i, ShareProgress.Status.FAIL));
                continue;
            }

            boolean success = false;
            try {
                success =item.getShareService().share(mShareContent,mShareFile);
            } catch (Exception e) {
                Log.e(TAG, "Fail share to " + item.getText(), e);
                success = false;
            }

            if (success) {
                publishProgress(new ShareProgress.Item(i, ShareProgress.Status.DONE));
            } else {
                publishProgress(new ShareProgress.Item(i, ShareProgress.Status.FAIL));
            }
        }
        return null;
    }

    private CharSequence translateErrorCode(String key){
        if (key != null) {
            try {
                int id = mContext.getResources().getIdentifier(key, "string", mContext.getPackageName());
                return mContext.getString(id);
            } catch(Exception e) {
            }
        }
        return null;
    }
}