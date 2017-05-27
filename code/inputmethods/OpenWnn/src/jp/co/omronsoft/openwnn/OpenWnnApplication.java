/* SPRD:Add for bug 492853, input method crash when the current ime is not jp @{ */
package jp.co.omronsoft.openwnn;

import android.app.Application;
import android.content.Context;

public class OpenWnnApplication extends Application {
    private static OpenWnnApplication mApplication;
    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        mApplication = this;
    }

    public static final Context getContext() {
        return mApplication.getApplicationContext();
    }
}
/* @} */