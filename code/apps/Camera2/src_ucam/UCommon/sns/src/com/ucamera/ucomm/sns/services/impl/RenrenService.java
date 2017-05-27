/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */

package com.ucamera.ucomm.sns.services.impl;

import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.RenrenApi20;
import org.scribe.model.MultiPartOAuthRequest;
import org.scribe.oauth.OAuthService;

import com.ucamera.ucomm.sns.services.AbstractService;
import com.ucamera.ucomm.sns.services.FileParameterAdapter;
import com.ucamera.ucomm.sns.services.ShareContent;
import com.ucamera.ucomm.sns.services.ShareError;
import com.ucamera.ucomm.sns.services.ShareFile;
import com.ucamera.ucomm.sns.services.AbstractService.ResponseChecker;

public class RenrenService extends AbstractService {
    private static final String API_URL = " http://api.renren.com/restserver.do";

    @Override
    public boolean share(ShareContent share, ShareFile file) {
        MultiPartOAuthRequest request = new MultiPartOAuthRequest(API_URL);
        request.addBodyParameter("method", "photos.upload");
        request.addBodyParameter("v", "1.0");
        request.addBodyParameter("format", "JSON");
        request.addBodyParameter("caption", share.getMessage());
        request.addFileParameter("upload", new FileParameterAdapter(file));
        getOAuthService().signRequest(getAccessToken(), request);
        return isRequestSuccess("share", request.send());
    }

    @Override
    protected ResponseChecker getResponseChecker(String action) {
        return new ResponseChecker() {
            @Override
            public boolean isSuccess(String response) throws Exception {
                return new JSONObject(response).has("pid");
            }
            @Override public ShareError getShareError(String response) throws Exception {
                return null;
            }
        };
    }

    protected OAuthService createOAuthService() {
        return new ServiceBuilder()
                .apiKey("50d98d4eaf0349609869476d606f17a8")
                .apiSecret("5e9df697bc3c43a180f01a04c8b5d9c4")
                .callback(CALLBACK_URL_ALTERNATE)
                .scope("photo_upload")
                .provider(RenrenApi20.class)
                .build();
    }

    @Override
    public String getServiceName() {
        return "Renren";
    }
}
