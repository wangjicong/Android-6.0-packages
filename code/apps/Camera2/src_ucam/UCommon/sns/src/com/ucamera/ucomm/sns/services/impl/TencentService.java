/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */
package com.ucamera.ucomm.sns.services.impl;

import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TencentApi10a;
import org.scribe.model.MultiPartOAuthRequest;
import org.scribe.model.OAuthRequest;
import org.scribe.model.SignatureType;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import android.text.TextUtils;

import com.ucamera.ucomm.sns.services.AbstractService;
import com.ucamera.ucomm.sns.services.FileParameterAdapter;
import com.ucamera.ucomm.sns.services.ShareContent;
import com.ucamera.ucomm.sns.services.ShareError;
import com.ucamera.ucomm.sns.services.ShareFile;

public class TencentService extends AbstractService {
    public static final String UPLOAD_URL = "http://open.t.qq.com/api/t/add_pic";

    @Override
    public boolean share(ShareContent share, ShareFile file) {
        MultiPartOAuthRequest request = new MultiPartOAuthRequest(UPLOAD_URL);
        request.addBodyParameter("format", "json");
        request.addBodyParameter("content", share.getMessage() + share.getUCamShare());
        if (!TextUtils.isEmpty(share.getLatitude())
                && !TextUtils.isEmpty(share.getLongitude())) {
            request.addBodyParameter("jing", share.getLongitude());
            request.addBodyParameter("wei", share.getLatitude());
        }
        request.addFileParameter("pic", new FileParameterAdapter(file));
        getOAuthService().signRequest(getAccessToken(), request);
        return isRequestSuccess("share",request.send());
    }

    @Override
    protected void followMe() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = "http://open.t.qq.com/api/friends/add";
                    OAuthRequest request = new OAuthRequest(Verb.POST, url);
                    request.addBodyParameter("format", "json");
                    request.addBodyParameter("name", "ucamera");
                    getOAuthService().signRequest(getAccessToken(), request);
                    isRequestSuccess("follow", request.send());
                }catch (Throwable e) {
                    //IGNORE
                }
            }
        }).start();
    }

    @Override
    protected ResponseChecker getResponseChecker(String action) {
        return new ResponseChecker() {
            @Override
            public boolean isSuccess(String response) throws Exception {
                JSONObject json = new JSONObject(response);
                return json.has("ret") && json.getInt("ret") == 0;
            }
            @Override public ShareError getShareError(String response) throws Exception {
                return null;
            }
        };
    }

    protected OAuthService createOAuthService() {
        return new ServiceBuilder()
                .apiKey("801095658")
                .apiSecret("4e6e62561c9cb9501e1074c973b3e62e")
                .signatureType(SignatureType.QueryString)
                .callback(CALLBACK_URL)
                .provider(TencentApi10a.class)
                .build();
    }

    @Override
    public String getServiceName() {
        return "Tencent";
    }
}
