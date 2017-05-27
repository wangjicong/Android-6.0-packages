
package com.android.mmsfolderview.parser.process;

import com.android.mmsfolderview.parser.XmlDomParser;

import android.text.TextUtils;
import android.util.Log;


public class BaseProcess implements IParamProcess {

    private static final String TAG = "BaseProcess";
    private String mName;

    public BaseProcess(String name) {
        mName = new String(name);
    }

    public BaseProcess() {
        mName = new String(TAG);
    }

    @Override
    public String getProcessName() {
        return mName;
    }

    @Override
    public String process(String argKey, String argValue, Object obj) {
        if (TextUtils.isEmpty(argValue)) {
            Log.e(XmlDomParser.TAG, TAG + ".process: input param error");
            throw new RuntimeException("Input Parameter Error");
        } else {
            return argValue;
        }
    }

}
