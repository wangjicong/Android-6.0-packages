/**
 *   Copyright (C) 2010,2011 Thundersoft Corporation
 *   All rights Reserved
 */
package org.scribe.builder.api;

import org.scribe.model.*;

import org.scribe.utils.*;

public class FacebookApi20 extends DefaultApi20 {
    private static final String AUTHORIZE_URL =
        " https://m.facebook.com/dialog/oauth?type=user_agent&client_id=%s&redirect_uri=%s&display=touch";
    private static final String SCOPED_AUTHORIZE_URL = AUTHORIZE_URL + "&scope=%s";

    @Override
    public String getAccessTokenEndpoint() {
        return "https://graph.facebook.com/oauth/access_token";
    }

    @Override
    public String getAuthorizationUrl(OAuthConfig config) {
        Preconditions.checkValidUrl(config.getCallback(),
                "Must provide a valid url as callback. Facebook does not support OOB");

        // Append scope if present
        if (config.hasScope()) {
            return String.format(SCOPED_AUTHORIZE_URL, config.getApiKey(),
                    OAuthEncoder.encode(config.getCallback()),
                    OAuthEncoder.encode(config.getScope()));
        } else {
            return String.format(AUTHORIZE_URL, config.getApiKey(),
                    OAuthEncoder.encode(config.getCallback()));
        }
    }

    @Override
    public String getLogoutUrl(Token accessToken) {
        return "https://api.facebook.com/restserver.php";
    }
}
