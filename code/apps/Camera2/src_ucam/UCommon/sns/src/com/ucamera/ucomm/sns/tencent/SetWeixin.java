/*
 * Copyright (C) 2014,2015 Thundersoft Corporation
 * All rights Reserved
 */
package com.ucamera.ucomm.sns.tencent;

import java.io.ByteArrayOutputStream;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.widget.Toast;

import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.SendMessageToWX;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.openapi.WXImageObject;
import com.tencent.mm.sdk.openapi.WXMediaMessage;
import com.ucamera.ucomm.sns.R;
import com.ucamera.ucomm.sns.Util;
import com.ucamera.ucomm.stat.StatApi;

public class SetWeixin {
    private IWXAPI api;
    private static SetWeixin mInstance;
    private static final String APP_ID = "wxcc927de47f3700b7";
    private Activity mActivity;
    private String mPath;
    private SetWeixin(Activity activity) {
        mActivity = activity;
        api = WXAPIFactory.createWXAPI(mActivity, APP_ID, false);
        api.registerApp(APP_ID);
    }
    private boolean checkWeiXinInstalled() {
        if(!api.isWXAppInstalled()) {
            Util.showAlert(mActivity, mActivity.getString(android.R.string.dialog_alert_title),
                    mActivity.getString(R.string.sns_msg_weixin_notinstall));
            return false;
        }
        return true;
    }
    private void shareFail() {
        Toast.makeText(mActivity, R.string.sns_share_failed, Toast.LENGTH_SHORT).show();
    }
    public static SetWeixin getInstance(Activity activity) {
        if(mInstance == null) {
            mInstance = new SetWeixin(activity);
        }
        return mInstance;
    }
    public void sendToFriend(String uri) {
        if (!Util.checkNetworkShowAlert(mActivity)){
            return;
        }
        if(!checkWeiXinInstalled()) {
            return;
        }
        mPath = uri;
        WXImageObject imgObj = new WXImageObject();
        imgObj.setImagePath(mPath);

        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = imgObj;
        Bitmap thumbBmp = scaleBitmap(null);
        if(thumbBmp == null) {
            shareFail();
            return;
        }
        msg.thumbData = bmpToByteArray(thumbBmp, true);

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = String.valueOf(System.currentTimeMillis());
        req.message = msg;
        api.sendReq(req);
        StatApi.onEvent(mActivity, StatApi.EVENT_SNS_SHARE, "weixinfriend");
    }
    public void sendToFriendQuan(String path) {
        if (!Util.checkNetworkShowAlert(mActivity)){
            return;
        }
        if(!checkWeiXinInstalled()) {
            return;
        }
        mPath = path;
        WXImageObject imgObj = new WXImageObject();
        imgObj.setImagePath(mPath);

        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = imgObj;
        Bitmap thumbBmp = scaleBitmap(null);
        if(thumbBmp == null) {
            shareFail();
            return;
        }
        msg.thumbData = bmpToByteArray(thumbBmp, true);

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = String.valueOf(System.currentTimeMillis());
        req.message = msg;
        req.scene = SendMessageToWX.Req.WXSceneTimeline;
        api.sendReq(req);
        StatApi.onEvent(mActivity, StatApi.EVENT_SNS_SHARE, "weixinquan");
    }
    public static byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(CompressFormat.PNG, 100, output);
        if (needRecycle) {
            bmp.recycle();
        }
        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
    private Bitmap scaleBitmap(Bitmap bitmap) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mPath, options);
        options.inSampleSize = computeSampleSize(options, 200, 10 * 1024);
        options.inJustDecodeBounds = false;
        options.inDither = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(mPath, options);
    }
    private int computeSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength,
                maxNumOfPixels);
        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }
        return roundedSize;
    }
    private int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;
        int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == -1) ? 128 : (int) Math.min(Math.floor(w / minSideLength), Math.floor(h / minSideLength));
        if (upperBound < lowerBound) {
            return lowerBound;
        }
        if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
            return 1;
        } else if (minSideLength == -1) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }
}
