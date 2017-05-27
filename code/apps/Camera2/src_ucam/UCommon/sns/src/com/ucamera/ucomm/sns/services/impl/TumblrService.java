/*
 * Copyright (C) 2011,2013 Thundersoft Corporation
 * All rights Reserved
 */

package com.ucamera.ucomm.sns.services.impl;

import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TumblrApi10a;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.MultiPartOAuthRequest;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import com.ucamera.ucomm.sns.services.AbstractService;
import com.ucamera.ucomm.sns.services.FileParameterAdapter;
import com.ucamera.ucomm.sns.services.ShareContent;
import com.ucamera.ucomm.sns.services.ShareError;
import com.ucamera.ucomm.sns.services.ShareFile;

public class TumblrService extends AbstractService {
    private static final String UPLOAD_URL = "http://api.tumblr.com/v2/blog/%s.tumblr.com/post";

    @Override
    public boolean share(ShareContent share, ShareFile file) {
        String apiUri = String.format(UPLOAD_URL, getUsername());
        MultiPartOAuthRequest request = new MultiPartOAuthRequest(apiUri);
        request.addBodyParameter("caption", share.getMessage());
        request.addBodyParameter("type",  "photo");
        request.addFileParameter("data[]", new FileParameterAdapter(file));
        getOAuthService().signRequest(getAccessToken(), request);
        return isRequestSuccess("share",request.send());
    }

    private String mUsername = null;

    protected String getUsername() {
        try {
            if (mUsername == null || mUsername.length() == 0) {
                OAuthRequest request = new OAuthRequest(Verb.POST, "http://api.tumblr.com/v2/user/info");
                getOAuthService().signRequest(getAccessToken(), request);
                final Response response = request.send();
                if (isRequestSuccess("getUsername", response)) {
                    mUsername = new JSONObject(response.getBody())
                            .getJSONObject("response")
                            .getJSONObject("user")
                            .getString("name");
                }
            }
            return mUsername;
        } catch (JSONException e) {
            throw new OAuthException("Fail get username.");
        }
    }

    /* (non-Javadoc)
     * @see com.ucamera.ucam.sns.services.AbstractService#createOAuthService()
     */
    @Override
    protected OAuthService createOAuthService() {
        return new ServiceBuilder()
                .apiKey("HWELZZOLlsG7QiPIOZAiS0Vs72g01hovaa5MLMyh8AiMX3YIhX")
                .apiSecret("5nJDyfd7qNvSvRap4BMTDTF2K42i7acUCfQcOlBgCBxfbnVbab")
                .callback(CALLBACK_URL)
                .provider(TumblrApi10a.class)
                .build();
    }

    /* (non-Javadoc)
     * @see com.ucamera.ucam.sns.services.AbstractService#getResponseChecker(java.lang.String)
     */
    @Override
    protected ResponseChecker getResponseChecker(String action) {
        return new ResponseChecker() {
            public boolean isSuccess(String response) throws Exception {
                int statusCode = new JSONObject(response)
                             .getJSONObject("meta")
                             .getInt("status");
                return statusCode == 200 || statusCode == 201;
            }
            @Override public ShareError getShareError(String response) throws Exception {
                return null;
            }
        };
    }


    @Override
    public String getServiceName() {
        return "Tumblr";
    }
}
