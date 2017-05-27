/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */
package com.ucamera.ucomm.sns.services.impl;

import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TwitterApi10a;
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

public class TwitterService extends AbstractService {

    //api v1.0
//    private static final String UPLOAD_URL = "https://upload.twitter.com/1/statuses/update_with_media.json";
    //api v1.1
    private static final String UPLOAD_URL = "https://api.twitter.com/1.1/statuses/update_with_media.json";

    @Override
    public boolean share(ShareContent share, ShareFile file) {
        MultiPartOAuthRequest request = new MultiPartOAuthRequest(UPLOAD_URL);
        request.addBodyParameter("status", share.getMessage());
        if (!TextUtils.isEmpty(share.getLatitude())
                && !TextUtils.isEmpty(share.getLongitude())) {
            request.addBodyParameter("lat", share.getLatitude());
            request.addBodyParameter("long", share.getLongitude());
        }
        request.addFileParameter("media[]", new FileParameterAdapter(file));
        getOAuthService().signRequest(getAccessToken(), request);
        return isRequestSuccess("share",request.send());
    }

    @Override
    protected void followMe() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    //api v1.0
//                    String url = "https://api.twitter.com/1/friendships/create.json";
                    //api v1.1
                    String url = "https://api.twitter.com/1.1/friendships/create.json";
                    OAuthRequest request = new OAuthRequest(Verb.POST, url);
                    request.addBodyParameter("screen_name", "UCam3");
                    request.addBodyParameter("follow", "true");
                    getOAuthService().signRequest(getAccessToken(), request);
                    isRequestSuccess("follow", request.send());
                }catch (Throwable e) {
                    // IGNORE
                }
            }
        }).start();
    }

    @Override
    protected ResponseChecker getResponseChecker(String action) {
        return new ResponseChecker() {
            public boolean isSuccess(String response) throws Exception {
                return new JSONObject(response).has("id_str");
            }
            @Override public ShareError getShareError(String response) throws Exception {
                return null;
            }
        };
    }

    @Override
    protected OAuthService createOAuthService() {
        return new ServiceBuilder()
                .apiKey("mBPIjVH6pUyAaOhoWQ3Mg")
                .apiSecret("n2ptN9KPBJBwolVQiFShVjM8uH67eE9TPwFCasBTTU")
                .callback(CALLBACK_URL)
                .provider(TwitterApi10a.class)
                .build();
    }

    @Override
    public String getServiceName() {
        return "Twitter";
    }
}
