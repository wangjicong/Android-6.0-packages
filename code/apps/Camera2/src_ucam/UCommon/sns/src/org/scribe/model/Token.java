/**
 *   Copyright (C) 2010,2011 Thundersoft Corporation
 *   All rights Reserved
 */
package org.scribe.model;

import java.io.Serializable;

/**
 * Represents an OAuth token (either request or access token) and its secret
 *
 * @author Pablo Fernandez
 */
public class Token implements Serializable {
    private static final long serialVersionUID = 715000866082812683L;

    private final String token;
    private final String secret;
    private long expires = 0;
    private String refreshToken;
    private final String rawResponse;

    /**
     * Default constructor
     *
     * @param token token value
     * @param secret token secret
     */
    public Token(String token, String secret) {
        this(token, secret, null);
    }

    public Token(String token, String secret, long expires){
        this(token,secret);
        this.expires = expires;
    }

    public Token(String token, String secret, String rawResponse) {
        this.token = token;
        this.secret = secret;
        this.rawResponse = rawResponse;
    }

    public String getToken() {
        return token;
    }

    public String getSecret() {
        return secret;
    }

    public Token setExpiresIn(String expiresIn){
        if (expiresIn != null && !expiresIn.equals("")) {
            long expires = expiresIn.equals("0")
                    ? 0
                    : System.currentTimeMillis() + Long.parseLong(expiresIn) * 1000L;
            this.expires = expires;
        }
        return this;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }

    public Token setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }

    public boolean isValid(){
        return this.expires == 0 || System.currentTimeMillis() < this.expires;
    }

    public long getExpires(){
        return expires;
    }

    public String getRawResponse() {
        if (rawResponse == null) {
            throw new IllegalStateException(
                    "This token object was not constructed by scribe and does not have a rawResponse");
        }
        return rawResponse;
    }

    @Override
    public String toString() {
        return String.format("Token[%s , %s]", token, secret);
    }
}
