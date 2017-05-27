/**
 *   Copyright (C) 2010,2011 Thundersoft Corporation
 *   All rights Reserved
 */
package org.scribe.builder.api;

import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.extractors.JsonTokenExtractor;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Token;
import org.scribe.utils.OAuthEncoder;

public class KaixinApi20 extends DefaultApi20 {
    private static final String AUTHORIZE_URL =
            "http://api.kaixin001.com/oauth2/authorize?client_id=%s&redirect_uri=%s&response_type=code&oauth_client=1";

    @Override
    public String getAccessTokenEndpoint() {
        return "https://api.kaixin001.com/oauth2/access_token";
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
    public AccessTokenExtractor getAccessTokenExtractor() {
        return new JsonTokenExtractor();
    }

    public String getLogoutUrl(Token accessToken) {
        return null;
    }
}
