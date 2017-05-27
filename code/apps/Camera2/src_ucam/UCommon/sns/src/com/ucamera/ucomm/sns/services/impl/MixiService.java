/*
 * Copyright (C) 2010,2013 Thundersoft Corporation
 * All rights Reserved
 */
package com.ucamera.ucomm.sns.services.impl;

import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.MixiApi20;
import org.scribe.model.BinaryOAuthRequest;
import org.scribe.model.SignatureType;
import org.scribe.oauth.OAuthService;

import com.ucamera.ucomm.sns.services.AbstractService;
import com.ucamera.ucomm.sns.services.FileParameterAdapter;
import com.ucamera.ucomm.sns.services.ShareContent;
import com.ucamera.ucomm.sns.services.ShareError;
import com.ucamera.ucomm.sns.services.ShareFile;
import com.ucamera.ucomm.sns.services.AbstractService.ResponseChecker;

public class MixiService extends AbstractService {

    private static final String UPLOAD_URL="http://api.mixi-platform.com/2/photo/mediaItems/@me/@self/@default";
    @Override
    public boolean share(ShareContent share, ShareFile file) {
        BinaryOAuthRequest request = new BinaryOAuthRequest(UPLOAD_URL);
        request.addQuerystringParameter("title", share.getMessage());
        request.setFileParameter(new FileParameterAdapter(file));
        getOAuthService().signRequest(getAccessToken(), request);
        return isRequestSuccess("share", request.send());
    }

    @Override
    protected OAuthService createOAuthService() {
        return new ServiceBuilder()
            .apiKey("27876807c3e4a9ef9d81")
            .apiSecret("470f366b09a39d02c54dc2640337b443f55ece1a")
            .callback(CALLBACK_URL_ALTERNATE)
            .signatureType(SignatureType.QueryString)
            .scope("w_photo r_profile")
            .provider(MixiApi20.class)
            .build();
    }

    @Override
    protected ResponseChecker getResponseChecker(String action) {
        return new ResponseChecker() {
            @Override
            public boolean isSuccess(String response) throws Exception {
                return new JSONObject(response).has("id");
            }
            @Override public ShareError getShareError(String response) throws Exception {
                return null;
            }
        };
    }

    @Override
    public String getServiceName() {
        return "Mixi";
    }
}
