/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */

package com.ucamera.ucomm.sns.services.impl;

import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.SohuWeiboApi10a;
import org.scribe.model.MultiPartOAuthRequest;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.ParameterList;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import org.scribe.utils.OAuthEncoder;

import com.ucamera.ucomm.sns.services.AbstractService;
import com.ucamera.ucomm.sns.services.FileParameterAdapter;
import com.ucamera.ucomm.sns.services.ShareContent;
import com.ucamera.ucomm.sns.services.ShareError;
import com.ucamera.ucomm.sns.services.ShareFile;


public class SohuService extends AbstractService {

    private static final String UPLOAD_URL = "http://api.t.sohu.com/statuses/upload.json";

    @Override
    public boolean share(ShareContent share, ShareFile file) {
        MultiPartOAuthRequest request = new MultiPartOAuthRequest(UPLOAD_URL);
        request.addBodyParameter("status",
                OAuthEncoder.encode(share.getMessage() + share.getUCamShare()));
        request.addFileParameter("pic", new FileParameterAdapter(file));
        getOAuthService().signRequest(getAccessToken(), request);
        return isRequestSuccess("share", request.send());
    }

    @Override
    protected ResponseChecker getResponseChecker(String action) {
        return new ResponseChecker() {
            @Override
            public boolean isSuccess(String response) throws Exception {
                return new JSONObject(response).has("id");
            }
            @Override public ShareError getShareError(String response) throws Exception {
                JSONObject r = new JSONObject(response);
                String code = "sohu" + r.optInt("code");
                if(code != null && code.equals("sohu400")) {
                    String msg  = r.optString("error");
                    return new ShareError(code, msg);
                }
                return null;
            }
        };
    }

    @Override
    protected void followMe() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = "http://api.t.sohu.com/friendships/create/330075766.json";
                    OAuthRequest request = new OAuthRequest(Verb.POST, url);
                    getOAuthService().signRequest(getAccessToken(), request);
                    isRequestSuccess("follow", request.send());
                }catch (Throwable e) {
                    //INGORE
                }
            }
        }).start();
    }

    protected OAuthService createOAuthService() {
        return new ServiceBuilder()
                .apiKey("IiHITnfET3crOO7rG7sj")
                .apiSecret("^O2mRwCh(c!9xJx-6wFOp##JK%mCi!Vs(Henjzr7")
                .callback(CALLBACK_URL)
                .provider(SohuWeiboApi10a.class)
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
        return "Sohu";
    }
}
