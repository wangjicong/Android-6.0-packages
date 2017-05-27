/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */
package com.ucamera.ucomm.sns.services.impl;

import android.text.TextUtils;

import com.ucamera.ucomm.sns.services.AbstractService;
import com.ucamera.ucomm.sns.services.FileParameterAdapter;
import com.ucamera.ucomm.sns.services.ShareContent;
import com.ucamera.ucomm.sns.services.ShareError;
import com.ucamera.ucomm.sns.services.ShareFile;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.QZoneApi20;
import org.scribe.model.MultiPartOAuthRequest;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Request;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QZoneService extends AbstractService {

    private static final String APP_KEY = "100247210";
    private static final String OPENID = "20B775BD33B0AC3FF62FEA1AC27282AB";

    private static final String UPLOAD_URL = "https://graph.qq.com/photo/upload_pic";

    @Override
    public boolean share(ShareContent share, ShareFile file) {
        FileParameterAdapter fileParam = new FileParameterAdapter(file);
        MultiPartOAuthRequest request = new MultiPartOAuthRequest(UPLOAD_URL);
        request.addBodyParameter("format", "json");
        request.addBodyParameter("photodesc", share.getMessage());
        if (!TextUtils.isEmpty(share.getLatitude())
                && !TextUtils.isEmpty(share.getLongitude())) {
            request.addBodyParameter("x", share.getLongitude());
            request.addBodyParameter("y", share.getLatitude());
        }
        request.addBodyParameter("title", fileParam.getName());
        request.addBodyParameter("oauth_consumer_key", APP_KEY);
        request.addFileParameter("picture", fileParam);
        request.addBodyParameter("openid", getOpenId());
        getOAuthService().signRequest(getAccessToken(), request);
        return isRequestSuccess("share",request.send());
    }

    @Override
    protected void followMe() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                String url = "https://graph.qq.com/relation/add_idol";
                OAuthRequest request = new OAuthRequest(Verb.POST, url);
                request.addBodyParameter("format", "json");
                request.addBodyParameter("name","1741197259");
                request.addBodyParameter("openid", getOpenId());
                request.addBodyParameter("oauth_consumer_key", APP_KEY);
                getOAuthService().signRequest(getAccessToken(), request);
                isRequestSuccess("follow",request.send());
                }catch (Throwable t) {
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
                .apiKey(APP_KEY)
                .apiSecret("72d7f3626d62dc66508b5b9b0e8d12ff")
                .callback(CALLBACK_URL)
                .scope("upload_pic,add_idol")
                .provider(QZoneApi20.class)
                .build();
    }

    @Override
    public String getServiceName() {
        return "QZone";
    }

    private final static Pattern OPENID_PATTERN = Pattern.compile("\"openid\"\\s*:\\s*\"(\\S*?)\"");
    private String getOpenId() {
        String url = "https://graph.qq.com/oauth2.0/me?access_token=" + getAccessToken().getToken();
        String response = new Request(Verb.GET, url).send().getBody();
        if (response != null) {
            Matcher matcher = OPENID_PATTERN.matcher(response);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

}
