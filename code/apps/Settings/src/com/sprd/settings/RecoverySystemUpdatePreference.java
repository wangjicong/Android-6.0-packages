/** Created by Spreadst*/
package com.sprd.settings;



import android.content.Context;
import android.os.Environment;
import android.os.PowerManager;
import android.os.RecoverySystem;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

import com.android.settings.R;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class RecoverySystemUpdatePreference extends DialogPreference {

    Context mContext;
    // SPRD:DELETE the str.
    // private File RECOVERY_DIR = new File("/cache/recovery");
    // private File COMMAND_FILE = new File(RECOVERY_DIR, "command");
    private static final int MINIMUM_LEVEL_POWER = 35;//Porting from Android 4.4

    public RecoverySystemUpdatePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        setDialogIcon(null);
        setDialogMessage(R.string.recovery_update_message);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            String storageState="";
            String storageDirectory="";
            //external sdcard
            storageState = Environment.getExternalStoragePathState();
            storageDirectory = Environment.getExternalStoragePath().getAbsolutePath();

            if (storageState.equals(Environment.MEDIA_MOUNTED)) {
                // sdcard 可用
                File file=new File(storageDirectory+"/update.zip");
                if(file.exists()){
                    int levelPower = getBacBatteryCallBack().getBatteryLevel();
                    if (levelPower >= MINIMUM_LEVEL_POWER) {
                        /*
                         * SPRD:DELETE the function is changed to update the
                         * system. RECOVERY_DIR.mkdirs(); // In case we need it
                         * COMMAND_FILE.delete(); // In case it's not writable
                         * try { FileWriter command = new
                         * FileWriter(COMMAND_FILE);
                         * command.write("--update_package=/sdcard/update.zip");
                         * command.write("\n"); command.close(); } catch
                         * (IOException e) { e.printStackTrace(); } PowerManager
                         * pm = (PowerManager)
                         * cont.getSystemService(Context.POWER_SERVICE);
                         * pm.reboot("recovery");
                         */

                        // SPRD:ADD the new function to update the system.
                        try {
                            RecoverySystem.installPackage(mContext, file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(mContext, R.string.recovery_update_level, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(mContext, R.string.recovery_no_package, Toast.LENGTH_LONG).show();
                }
            }else {
                // sdcard 不可用
                Toast.makeText(mContext, R.string.recovery_sdacrd_status, Toast.LENGTH_LONG).show();
            }
        }
    }

    /* SPRD: for Bug271433 when BroadcastReceiver is already unregistered mustn't unregister it again  @{ */
    private BatteryCallBack mCallBack;

    public interface BatteryCallBack{
        int getBatteryLevel();
    }

    public void setBatteryCallBack(BatteryCallBack callBack){
        this.mCallBack = callBack;
    }
    private BatteryCallBack getBacBatteryCallBack(){
        if(mCallBack == null){
            throw new IllegalStateException("mCallBack is null.");
        }
        return mCallBack;
    }
    /* @} */
}

