/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */
package com.ucamera.ucomm.sns.services.impl;

import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.KaixinApi20;
import org.scribe.model.MultiPartOAuthRequest;
import org.scribe.oauth.OAuthService;

import android.text.TextUtils;

import com.ucamera.ucomm.sns.services.AbstractService;
import com.ucamera.ucomm.sns.services.FileParameterAdapter;
import com.ucamera.ucomm.sns.services.ShareContent;
import com.ucamera.ucomm.sns.services.ShareError;
import com.ucamera.ucomm.sns.services.ShareFile;

public class KaixinService extends AbstractService {
    private static final String UPLOAD_URL = "https://api.kaixin001.com/records/add.json";

    @Override
    public boolean share(ShareContent share, ShareFile file) {
        MultiPartOAuthRequest request = new MultiPartOAuthRequest(UPLOAD_URL);
        request.addBodyParameter("content", share.getMessage() + share.getUCamShare());
        if (!TextUtils.isEmpty(share.getLatitude())
                && !TextUtils.isEmpty(share.getLongitude())) {
            request.addBodyParameter("lat", share.getLatitude());
            request.addBodyParameter("lon", share.getLongitude());
        }
        request.addFileParameter("pic", new FileParameterAdapter(file));
        getOAuthService().signRequest(getAccessToken(), request);
        return isRequestSuccess("share",request.send());
    }

    @Override
    protected ResponseChecker getResponseChecker(String action) {
        return new ResponseChecker() {
            public boolean isSuccess(String response) throws Exception {
                return  new JSONObject(response).has("rid");
            }
            @Override public ShareError getShareError(String response) throws Exception {
                return null;
            }
        };
    }

    protected OAuthService createOAuthService() {
        return new ServiceBuilder()
                .apiKey("61488552953958ced9ae7059716c5087")
                .apiSecret("794bc88f15117025fd9039ff6d1289bc")
                .callback(CALLBACK_URL_ALTERNATE)
                .scope("create_records")
                .provider(KaixinApi20.class)
                .build();
    }

    @Override
    public String getServiceName() {
        return "Kaixin";
    }

}
