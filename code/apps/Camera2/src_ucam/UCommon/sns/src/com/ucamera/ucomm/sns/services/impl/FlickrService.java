/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */
package com.ucamera.ucomm.sns.services.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.FlickrApi10a;
import org.scribe.model.MultiPartOAuthRequest;
import org.scribe.oauth.OAuthService;

import com.ucamera.ucomm.sns.services.AbstractService;
import com.ucamera.ucomm.sns.services.FileParameterAdapter;
import com.ucamera.ucomm.sns.services.ShareContent;
import com.ucamera.ucomm.sns.services.ShareError;
import com.ucamera.ucomm.sns.services.ShareFile;

public class FlickrService extends AbstractService {

    private static final String UPLOAD_URL = "http://api.flickr.com/services/upload";

    @Override
    public boolean share(ShareContent share, ShareFile file) {
        MultiPartOAuthRequest request = new MultiPartOAuthRequest(UPLOAD_URL);
        request.addBodyParameter("title", share.getMessage());
        request.addBodyParameter("description", share.getTitle());
        request.addFileParameter("photo", new FileParameterAdapter(file));
        getOAuthService().signRequest(getAccessToken(), request);
        return isRequestSuccess("share", request.send());
    }

    @Override
    protected ResponseChecker getResponseChecker(String action) {
        return new ResponseChecker() {
            @Override
            public boolean isSuccess(String response) throws Exception {
                Matcher matcher = Pattern.compile("<rsp stat=\"([a-z]+)\">").matcher(response);
                return matcher.find() && "ok".equals(matcher.group(1));
            }
            @Override public ShareError getShareError(String response) throws Exception {
                return null;
            }
        };
    }

    protected OAuthService createOAuthService() {
        return new ServiceBuilder()
                .apiKey("710efadcb4aef4d114bda87e3a774aa2")
                .apiSecret("bbfa474c7bb033b7")
                .callback(CALLBACK_URL)
                .provider(FlickrApi10a.class)
                .build();
    }

    @Override
    public String getServiceName() {
        return "Flickr";
    }
}
