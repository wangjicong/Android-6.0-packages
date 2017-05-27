/**
 *   Copyright (C) 2010,2011 Thundersoft Corporation
 *   All rights Reserved
 */
package org.scribe.builder.api;

import java.security.MessageDigest;
import java.util.Iterator;

import org.scribe.exceptions.OAuthException;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.extractors.JsonTokenExtractor;
import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Parameter;
import org.scribe.model.ParameterList;
import org.scribe.model.Token;
import org.scribe.oauth.OAuth20ServiceImpl;
import org.scribe.oauth.OAuthService;
import org.scribe.utils.OAuthEncoder;

public class RenrenApi20 extends DefaultApi20 {

    private static final String AUTHORIZE_URL = "https://graph.renren.com/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code&display=touch";

    @Override
    public String getAccessTokenEndpoint() {
        return "https://graph.renren.com/oauth/token";
    }

    @Override
    public String getAuthorizationUrl(OAuthConfig config) {
        String authUrl =
                String.format(AUTHORIZE_URL, config.getApiKey(),
                        OAuthEncoder.encode(config.getCallback()));

        if (config.hasScope()) {
            authUrl += String.format("&scope=%s", OAuthEncoder.encode(config.getScope()));
        }
        return authUrl;
    }

    @Override
    public AccessTokenExtractor getAccessTokenExtractor() {
        return new JsonTokenExtractor();
    }

    public OAuthService createService(OAuthConfig config) {
        return new RenrenOAuth20ServiceImpl(this, config);
    }

    private static class RenrenOAuth20ServiceImpl extends OAuth20ServiceImpl {
        public RenrenOAuth20ServiceImpl(DefaultApi20 api, OAuthConfig config) {
            super(api, config);
        }

        public void signRequest(Token accessToken, OAuthRequest request) {
            request.addBodyParameter(OAuthConstants.ACCESS_TOKEN, accessToken.getToken());
            this.addSignature(request);
        }

        private void addSignature(OAuthRequest request) {
            ParameterList params = request.getBodyParams().sort();
            StringBuilder builder = new StringBuilder();
            for (Iterator<Parameter> it = params.iterator(); it.hasNext();) {
                Parameter p = it.next();
                builder.append(p.getKey()).append("=").append(p.getValue());
            }
            builder.append(config.getApiSecret());
            config.log("params to sign: " + builder.toString());
            try {
                byte[] bytes = builder.toString().getBytes("UTF-8");
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                byte[] digest = md5.digest(bytes);
                int len = digest.length;
                StringBuilder sb = new StringBuilder(len << 1);
                for (int i = 0; i < len; i++) {
                    sb.append(Character.forDigit((digest[i] & 0xf0) >> 4, 16));
                    sb.append(Character.forDigit(digest[i] & 0x0f, 16));
                }
                request.addBodyParameter("sig", sb.toString());
            } catch (Exception e) {
                throw new OAuthException("Fail sign request", e);
            }
        }

    }

    public String getLogoutUrl(Token accessToken) {
        return null;
    }

}
