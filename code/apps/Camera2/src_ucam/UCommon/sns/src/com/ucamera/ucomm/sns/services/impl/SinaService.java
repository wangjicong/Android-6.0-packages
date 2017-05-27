/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */

package com.ucamera.ucomm.sns.services.impl;

import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.SinaWeibo2Api;
import org.scribe.model.MultiPartOAuthRequest;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.ParameterList;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import android.text.TextUtils;

import com.ucamera.ucomm.sns.services.AbstractService;
import com.ucamera.ucomm.sns.services.FileParameterAdapter;
import com.ucamera.ucomm.sns.services.ShareContent;
import com.ucamera.ucomm.sns.services.ShareError;
import com.ucamera.ucomm.sns.services.ShareFile;
import com.ucamera.ucomm.sns.services.AbstractService.ResponseChecker;

public class SinaService extends AbstractService {

    private static final String UPLOAD_URL = "https://api.weibo.com/2/statuses/upload.json";

    @Override
    public boolean share(ShareContent share, ShareFile file) {
        MultiPartOAuthRequest request = new MultiPartOAuthRequest(UPLOAD_URL);
        request.addBodyParameter("status", share.getMessage() + share.getUCamShare());
        if (!TextUtils.isEmpty(share.getLatitude())
                && !TextUtils.isEmpty(share.getLongitude())) {
            request.addBodyParameter("lat", share.getLatitude());
            request.addBodyParameter("long", share.getLongitude());
        }
        request.addFileParameter("pic", new FileParameterAdapter(file));
        getOAuthService().signRequest(getAccessToken(), request);
        return isRequestSuccess("share", request.send());
    }

    protected void followMe() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = "https://api.weibo.com/2/friendships/create.json";
                    final String UCAM_UID = "2429583781";
                    OAuthRequest request = new OAuthRequest(Verb.POST, url);
                    request.addBodyParameter("uid", UCAM_UID);
                    getOAuthService().signRequest(getAccessToken(), request);
                    isRequestSuccess("follow", request.send());
                } catch (Throwable e) {
                    // IGNORE
                }
            }
        }).start();
    }

    @Override
    protected ResponseChecker getResponseChecker(String action) {
        return new ResponseChecker() {
            @Override public boolean isSuccess(String response) throws Exception {
                return new JSONObject(response).has("id");
            }
            @Override public ShareError getShareError(String response) throws Exception {
                JSONObject r = new JSONObject(response);
                String code = "sina_" + r.optInt("error_code");
                String msg  = r.optString("error");
                return new ShareError(code, msg);
            }
        };
    }

    @Override
    protected OAuthService createOAuthService() {
        return new ServiceBuilder()
                .apiKey("841854309")
                .apiSecret("df3605f9f6b34eb3a62bbe8724ec3f50")
                .callback(CALLBACK_URL)
                .provider(SinaWeibo2Api.class)
                .build();
    }

    protected String wrapAuthorizationUrl(String url) {
        url = super.wrapAuthorizationUrl(url);
        ParameterList parameterList = new ParameterList();
        parameterList.add(OAuthConstants.CALLBACK, CALLBACK_URL);
        return parameterList.appendTo(url);
    }

    @Override
    public String getServiceName() {
        return "Sina";
    }
}
