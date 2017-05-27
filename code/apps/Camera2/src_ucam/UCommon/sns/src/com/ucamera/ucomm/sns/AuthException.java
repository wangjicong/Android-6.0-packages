/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */
package com.ucamera.ucomm.sns;

public class AuthException extends RuntimeException {

    public AuthException() {
        super();
    }

    public AuthException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public AuthException(String detailMessage) {
        super(detailMessage);
    }

    public AuthException(Throwable throwable) {
        super(throwable);
    }
}
