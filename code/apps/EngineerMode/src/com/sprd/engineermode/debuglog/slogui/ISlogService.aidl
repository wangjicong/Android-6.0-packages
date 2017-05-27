package com.sprd.engineermode.debuglog.slogui;

import android.os.Bundle;

/**
 * Applications can control slog configuration using this aidl.
 */
interface ISlogService {
    /**
     * Set one of slog configurations.
     * Maybe it is useless currently
     * @param option, the switch you want to turn on/off
     * @param enable, the state you want to change
     */
    void setState(String keyName, boolean state, boolean isLastOptions);

    /**
     * set General Switch
     * 
     */
    void setGeneralState(int state);

    Bundle getAllStates();

    boolean getState(String keyName);
    
    void setNotification(int which, boolean show);
}
