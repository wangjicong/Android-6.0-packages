/**
 *   Copyright (C) 2010,2011 Thundersoft Corporation
 *   All rights Reserved
 */
package org.scribe.builder.api;

import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.extractors.JsonTokenExtractor;
import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuth20ServiceImpl;
import org.scribe.oauth.OAuthService;
import org.scribe.utils.OAuthEncoder;

public class MixiApi20 extends DefaultApi20 {
    private static final String AUTHORIZE_URL =
            "https://mixi.jp/connect_authorize.pl?client_id=%s&redirect_uri=%s&response_type=code&display=touch";
    @Override
    public String getAccessTokenEndpoint() {
        return "https://secure.mixi-platform.com/2/token";
    }

    @Override
    public String getAuthorizationUrl(OAuthConfig config) {
        String authUrl = String.format(AUTHORIZE_URL, config.getApiKey(),
                OAuthEncoder.encode(config.getCallback()));
        if (config.hasScope()){
            authUrl += "&scope=" + OAuthEncoder.encode(config.getScope());
        }
        return authUrl;
    }

    @Override
    public OAuthService createService(OAuthConfig config) {
        return new MixiOAuth20ServiceImpl(this, config);
    }

    @Override
    public Verb getAccessTokenVerb() {
        return Verb.POST;
    }

    @Override
    public AccessTokenExtractor getAccessTokenExtractor() {
        return new JsonTokenExtractor();
    }

    public String getLogoutUrl(Token accessToken) {
        return null;
    }

    private static class MixiOAuth20ServiceImpl extends OAuth20ServiceImpl {
        public MixiOAuth20ServiceImpl(DefaultApi20 api, OAuthConfig config) {
            super(api, config);
        }
        @Override public void signRequest(Token accessToken, OAuthRequest request) {
            request.addHeader(OAuthConstants.HEADER, "OAuth " + accessToken.getToken());
        }
    }
}
