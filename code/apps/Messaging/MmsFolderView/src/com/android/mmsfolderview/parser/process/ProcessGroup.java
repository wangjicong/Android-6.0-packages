
package com.android.mmsfolderview.parser.process;

import java.util.HashMap;

import com.android.mmsfolderview.parser.XmlDomParser;

import android.text.TextUtils;
import android.util.Log;

public class ProcessGroup {

    private static final String TAG = "ProcessorGroup";

    public static final String KEY_START = "start";
    public static final String KEY_END = "end";

    private HashMap<String, IParamProcess> mProcessMap = new HashMap<String, IParamProcess>();
    private static ProcessGroup mInstance = null;

    public static synchronized ProcessGroup getInstance() {
        if (mInstance == null) {
            mInstance = new ProcessGroup();
            mInstance.configDefaultProcessMap();
        }
        return mInstance;
    }

    private void configDefaultProcessMap() {
        mProcessMap.put(KEY_START, new TimeQuantumProcess());
        mProcessMap.put(KEY_END, new TimeQuantumProcess());
    }

    public ProcessGroup() {
    }

    public void setProcessMap(String argKey, IParamProcess process) {
        if (mProcessMap.containsKey(argKey)) {
            Log.w(XmlDomParser.TAG, TAG + ".setProcessMap: Warming--It will replace the key="
                    + argKey + " process in mProcessMap.");
        }
        mProcessMap.put(argKey, process);
    }

    public IParamProcess getIParamProcess(String argKey) {
        if (TextUtils.isEmpty(argKey)) {
            Log.e(XmlDomParser.TAG, TAG + ".getIParamProcess: input argKey is Empty");
            throw new RuntimeException("input argKey is Empty");
        }
        if (mProcessMap.containsKey(argKey)) {
            return mProcessMap.get(argKey);
        } else {
            return new BaseProcess();
        }
    }
}
