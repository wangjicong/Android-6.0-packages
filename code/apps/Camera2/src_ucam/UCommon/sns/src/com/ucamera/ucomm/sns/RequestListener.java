/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */
package com.ucamera.ucomm.sns;

import android.os.Bundle;

/**
 * Callback interface for API requests. Each method includes a 'state' parameter
 * that identifies the calling request. It will be set to the value passed when
 * originally calling the request method, or null if none was passed.
 */
public interface RequestListener {

    /**
     * Called when a request completes with the given response. Executed by a
     * background thread: do not update the UI in this method.
     */
    public void onComplete(Bundle data);

    /**
     * Called when a request has a network or request error. Executed by a
     * background thread: do not update the UI in this method.
     */
    public void onException(RequestException e);

    public void onCancel();
}
