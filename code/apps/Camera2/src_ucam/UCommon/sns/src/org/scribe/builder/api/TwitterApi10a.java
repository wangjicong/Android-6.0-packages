/**
 *   Copyright (C) 2010,2011 Thundersoft Corporation
 *   All rights Reserved
 */
package org.scribe.builder.api;

import org.scribe.extractors.BaseStringExtractor;
import org.scribe.extractors.BaseStringExtractorImpl;
import org.scribe.model.OAuthRequest;
import org.scribe.model.ParameterList;
import org.scribe.model.Token;

public class TwitterApi10a extends DefaultApi10a {
    private static final String AUTHORIZE_URL = "https://api.twitter.com/oauth/authorize?oauth_token=%s";

    @Override
    public String getAccessTokenEndpoint() {
        return "https://api.twitter.com/oauth/access_token";
    }

    @Override
    public String getRequestTokenEndpoint() {
        return "https://api.twitter.com/oauth/request_token";
    }

    @Override
    public String getAuthorizationUrl(Token requestToken) {
        return String.format(AUTHORIZE_URL, requestToken.getToken());
    }

    @Override
    public BaseStringExtractor getBaseStringExtractor() {
        return new TwitterBaseStringExtractor();
    }

    public String getLogoutUrl(Token accessToken) {
        return null;
    }

    private static class TwitterBaseStringExtractor extends BaseStringExtractorImpl {
        protected String getSortedAndEncodedParams(OAuthRequest request) {
            ParameterList params = new ParameterList();
            if (!request.isMultipartRequest()) {
                params.addAll(request.getQueryStringParams());
                params.addAll(request.getBodyParams());
            }
            params.addAll(new ParameterList(request.getOauthParameters()));
            return params.sort().asOauthBaseString();
        }
    }
}
