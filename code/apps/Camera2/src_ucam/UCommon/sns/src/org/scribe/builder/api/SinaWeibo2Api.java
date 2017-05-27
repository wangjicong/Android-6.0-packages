/**
 *   Copyright (C) 2010,2011 Thundersoft Corporation
 *   All rights Reserved
 */
package org.scribe.builder.api;

import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.extractors.JsonTokenExtractor;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.utils.OAuthEncoder;
import org.scribe.utils.Preconditions;

public class SinaWeibo2Api extends DefaultApi20 {

    private static final String AUTHORIZE_URL =
            "https://api.weibo.com/oauth2/authorize?client_id=%s&redirect_uri=%s&response_type=code&display=mobile";

    public String getAccessTokenEndpoint() {
        return "https://api.weibo.com/oauth2/access_token";
    }

    public String getAuthorizationUrl(OAuthConfig config) {
        Preconditions.checkValidUrl(config.getCallback(), "Must provide a valid url as callback.");
        return String.format(AUTHORIZE_URL, config.getApiKey(),
                OAuthEncoder.encode(config.getCallback()));
    }

    public Verb getAccessTokenVerb() {
        return Verb.POST;
    }

    public AccessTokenExtractor getAccessTokenExtractor() {
        return new JsonTokenExtractor();
    }

    public String getLogoutUrl(Token accessToken) {
        return "https://api.weibo.com/2/account/end_session.json";
    }
}
