/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */

package com.ucamera.ucomm.sns.services.impl;

import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.FacebookApi20;
import org.scribe.model.MultiPartOAuthRequest;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import android.text.TextUtils;

import com.ucamera.ucomm.sns.services.AbstractService;
import com.ucamera.ucomm.sns.services.FileParameterAdapter;
import com.ucamera.ucomm.sns.services.ShareContent;
import com.ucamera.ucomm.sns.services.ShareError;
import com.ucamera.ucomm.sns.services.ShareFile;

public class FacebookService extends AbstractService {

    protected static String GRAPH_BASE_URL = "https://graph.facebook.com/";
    protected static String RESTSERVER_URL = "https://api.facebook.com/restserver.php";

    public boolean share(ShareContent share, ShareFile file) {
        MultiPartOAuthRequest request = new MultiPartOAuthRequest(GRAPH_BASE_URL + "me/photos");
        request.addBodyParameter("caption", share.getMessage());
        request.addFileParameter("photo", new FileParameterAdapter(file));
        getOAuthService().signRequest(getAccessToken(), request);
        return isRequestSuccess("share", request.send());
    }

    @Override
    protected boolean doLogout() {
        String url = getOAuthService().getLogoutUrl(getAccessToken());
        if (!TextUtils.isEmpty(url)){
            OAuthRequest request = new OAuthRequest(Verb.GET, url);
            request.addQuerystringParameter("method", "auth.expireSession");
            request.addQuerystringParameter("format", "json");
            getOAuthService().signRequest(getAccessToken(), request);
            return isRequestSuccess("logout", request.send());
        }
        return true;
    }

    @Override
    protected ResponseChecker getResponseChecker(String action) {
        return new ResponseChecker() {

            @Override public boolean isSuccess(String response) throws Exception {
                if (response.equals("false")) {
                    return false;
                }

                if (response.equals("true")) {
                    return true;
                }

                JSONObject json = new JSONObject(response);
                if (json.has("error")
                        || json.has("error_code")
                        || json.has("error_msg")
                        || json.has("error_reason")) {
                    return false;
                }
                return true;
            }

            @Override public ShareError getShareError(String response) throws Exception {
                return null;
            }
        };
    }

    protected OAuthService createOAuthService() {
        return new ServiceBuilder()
                .apiKey("182565251849818")
                .apiSecret("fbd459c3873b49dcd2bc8a07adddbcde")
                .callback(CALLBACK_URL_FACEBOOK)
                .scope("publish_stream")
                .provider(FacebookApi20.class)
                .build();
    }

    @Override
    public String getServiceName() {
        return "Facebook";
    }
}
