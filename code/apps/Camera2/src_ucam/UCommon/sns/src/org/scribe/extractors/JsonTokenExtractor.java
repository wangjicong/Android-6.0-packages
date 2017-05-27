/**
 *   Copyright (C) 2010,2011 Thundersoft Corporation
 *   All rights Reserved
 */
package org.scribe.extractors;

import java.util.regex.*;

import org.scribe.exceptions.*;
import org.scribe.model.*;
import org.scribe.utils.*;

public class JsonTokenExtractor implements AccessTokenExtractor {
    private Pattern accessTokenPattern = Pattern.compile("\"access_token\"\\s*:\\s*\"(\\S*?)\"");
    private Pattern expiresPattern = Pattern.compile("\"expires_in\"\\s*:\\s*\"?(\\d+)\"?");
    private Pattern refreshTokenPattern = Pattern.compile("\"refresh_token\"\\s*:\\s*\"(\\S*?)\"");

    @Override
    public Token extract(String response) {
        Preconditions.checkEmptyString(response,
                "Cannot extract a token from a null or empty String");
        Matcher matcher = accessTokenPattern.matcher(response);
        if (matcher.find()) {
            return new Token(matcher.group(1), "", response)
                    .setExpiresIn(extractExpire(response))
                    .setRefreshToken(extractRefreshToken(response));
        } else {
            throw new OAuthException("Cannot extract an acces token. Response was: " + response);
        }
    }

    private String extractExpire(String response) {
        Matcher matcher = expiresPattern.matcher(response);
        if (matcher.find() && matcher.groupCount() >= 1) {
            return OAuthEncoder.decode(matcher.group(1));
        }
        return null;
    }

    private String extractRefreshToken(String response) {
        Matcher matcher = refreshTokenPattern.matcher(response);
        if (matcher.find() && matcher.groupCount() >= 1) {
            return OAuthEncoder.decode(matcher.group(1));
        }
        return null;
    }
}
