
package com.android.providers.telephony.ext.mmsbackup;

import android.os.ParcelFileDescriptor;

import com.sprd.plat.Impl.BaseUserMessage;

public class NotifyImpl extends BaseUserMessage implements Defines {

    protected void reportFail(int errCode, Throwable info) {
        GetCallBack().OnNotify(CMD_REPORT_RESUALT, -1, errCode, info, null);
    }

    protected void reportFail(int errCode, String info) {
        GetCallBack().OnNotify(CMD_REPORT_RESUALT, -1, errCode, info, null);
    }

    protected ParcelFileDescriptor getFileDescriptor(boolean isRead, String name) {
        InOutParameter iop = new InOutParameter();
        iop.setString(name);
        iop.setBoolean(isRead);
        try {
            if (GetCallBack().OnNotify(CMD_GET_PARAMETER, PARAMETER_FD, 0, iop, null) != SUCC) {
                BackupLog.log("getFileDescriptor", "get parameter fd - " + name + " failed.");
                return null;
            }
        } catch (Exception e) {
            BackupLog.log("getFileDescriptor", "open " + name + " error.", e);
            return null;
        }
        return (ParcelFileDescriptor) iop.getObject();
    }

    protected boolean isOldVersionFile() {
        InOutParameter iop = new InOutParameter();
        try {
            if (GetCallBack().OnNotify(CMD_GET_PARAMETER, PARAMETER_VERSION, 0, iop, null) != SUCC) {
                BackupLog.log("getVersion", "getVersion failed.");
                return false;
            }
        } catch (Exception e) {
            BackupLog.log("getVersion", "getVersion error.", e);
            return false;
        }
        return iop.getBoolean();
    }
}
