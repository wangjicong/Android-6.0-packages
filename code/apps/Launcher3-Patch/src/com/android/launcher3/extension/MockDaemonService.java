package com.android.launcher3.extension;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by lewa on 16-9-21.
 */
public class MockDaemonService extends Service{

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
