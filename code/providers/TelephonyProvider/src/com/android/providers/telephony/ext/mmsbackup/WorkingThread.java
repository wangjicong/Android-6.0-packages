
package com.android.providers.telephony.ext.mmsbackup;

import android.content.Context;

public class WorkingThread extends NotifyImpl implements Runnable {
    protected PublicParameter parameter;
    protected boolean mCancel = false;
    protected Context mContext = null;
    protected String fileName = null;

    /**
     * @param db
     * @param pduFileDes
     * @param threadId
     * @param callback
     */
    public WorkingThread(PublicParameter db, Context context) {
        parameter = db;
        mContext = context;
    }

    public void stopExec() {
        mCancel = true;
    }

    @Override
    public void run() {
    }
}
