
package com.android.mmsfolderview.parser.process;

public interface IParamProcess {
    public String getProcessName();
    public String process(String argKey, String argValue, Object obj);
}
