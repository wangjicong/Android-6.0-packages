/**
 *   Copyright (C) 2010,2011 Thundersoft Corporation
 *   All rights Reserved
 */
package org.scribe.builder.api;

import org.scribe.model.OAuthConfig;
import org.scribe.model.Token;
import org.scribe.utils.OAuthEncoder;

public class QZoneApi20 extends DefaultApi20 {
    private static final String AUTHORIZE_URL
        = "https://graph.qq.com/oauth2.0/authorize?client_id=%s&redirect_uri=%s&response_type=code&display=mobile";

    public String getAccessTokenEndpoint() {
        return "https://graph.qq.com/oauth2.0/token";
    }

    public String getAuthorizationUrl(OAuthConfig config) {
        String url =  String.format(AUTHORIZE_URL, config.getApiKey(),
                OAuthEncoder.encode(config.getCallback()));
        if (config.hasScope()) {
            url += "&scope=" + OAuthEncoder.encode(config.getScope());
        }
        return url;
    }

    public String getLogoutUrl(Token accessToken) {
        return null;
    }
}
