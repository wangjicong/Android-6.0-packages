/**
 *   Copyright (C) 2010,2011 Thundersoft Corporation
 *   All rights Reserved
 */
package org.scribe.extractors;

import java.util.regex.*;

import org.scribe.exceptions.*;
import org.scribe.model.*;
import org.scribe.utils.*;

/**
 * Default implementation of {@AccessTokenExtractor}.
 * Conforms to OAuth 2.0
 */
public class TokenExtractor20Impl implements AccessTokenExtractor {
    private static final String TOKEN_REGEX = "access_token=([^&]+)";
    private static final String EXPIRES_REGEX = "expires_in=([^&]+)";
    private static final String REFRESH_TOKEN_REGEX = "refresh_token=([^&]+)";
    private static final String EMPTY_SECRET = "";

    /**
     * {@inheritDoc}
     */
    public Token extract(String response) {
        Preconditions.checkEmptyString(response,
                "Response body is incorrect. Can't extract a token from an empty string");

        Matcher matcher = Pattern.compile(TOKEN_REGEX).matcher(response);
        if (matcher.find()) {
            String token = OAuthEncoder.decode(matcher.group(1));
            return new Token(token, EMPTY_SECRET, response)
                            .setExpiresIn(extractExpire(response))
                            .setRefreshToken(extractRefreshToken(response));
        } else {
            throw new OAuthException(
                    "Response body is incorrect. Can't extract a token from this: '" + response
                            + "'", null);
        }
    }

    private String extractExpire(String response) {
        Matcher matcher = Pattern.compile(EXPIRES_REGEX).matcher(response);
        if (matcher.find() && matcher.groupCount() >= 1) {
            return OAuthEncoder.decode(matcher.group(1));
        }
        return null;
    }

    private String extractRefreshToken(String response) {
        Matcher matcher = Pattern.compile(REFRESH_TOKEN_REGEX).matcher(response);
        if (matcher.find() && matcher.groupCount() >= 1) {
            return OAuthEncoder.decode(matcher.group(1));
        }
        return null;
    }
}
