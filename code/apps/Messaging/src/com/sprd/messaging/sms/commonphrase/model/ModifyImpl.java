
package com.sprd.messaging.sms.commonphrase.model;

import android.util.Log;

public class ModifyImpl implements IModify {

    @Override
    public int getFlag() {
        // TODO Auto-generated method stub
        return mnFlag;
    }

    @Override
    public void setFlag(int nFlag) {
        switch (nFlag) {
            case OP_UNKNOW:
            case OP_NORMAL:
            case OP_INSERT:
            case OP_DELETE:
            case OP_UPDATE:
                mnFlag = nFlag;
                break;
            default:
                Log.e("ModifyImpl", "error operater!", new RuntimeException("SetFlag Error"));
        }

    }

    private int mnFlag = OP_UNKNOW;

}
