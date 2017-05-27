/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */
package com.ucamera.ucomm.sns.services;

import org.scribe.model.Token;

/**
 * Callback interface for authorization events.
 */
public interface LoginListener {

    /**
     * Called when a auth flow completes successfully and a valid OAuth Token
     * was received. Executed by the thread that initiated the authentication.
     * API requests can now be made.
     */
    public void onSuccess(Token accessToken);

    /**
     * Called when a login completes unsuccessfully with an error. Executed by
     * the thread that initiated the authentication.
     */
    public void onFail(String error);

    public void onCancel();
}
