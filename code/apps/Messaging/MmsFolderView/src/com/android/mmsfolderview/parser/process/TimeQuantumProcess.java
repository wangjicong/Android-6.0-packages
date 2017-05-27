
package com.android.mmsfolderview.parser.process;

import com.android.mmsfolderview.parser.XmlDomParser;

import android.text.TextUtils;
import android.util.Log;

public class TimeQuantumProcess extends BaseProcess {

    private static final String TAG = "TimeQuantumProcess";
    private static final long DAY_MILLIS = 24 * 60 * 60 * 1000;

    public TimeQuantumProcess() {
        super(TAG);
    }

    private long getZeroTimeMillis() {
        long cTime = System.currentTimeMillis();
        return (cTime - (cTime % DAY_MILLIS)); // today zero time
    }

    @Override
    public String process(String argKey, String argValue, Object obj) {
        if (!TextUtils.isEmpty(argValue)) {
            long offsetMillis = Long.valueOf(argValue) * 1000; // second to millisecond
            long result = getZeroTimeMillis() + offsetMillis;
            return "" + result;
        } else {
            Log.e(XmlDomParser.TAG, TAG + ".process: input param error");
            throw new RuntimeException("Input Parameter Error");
        }
    }
}
