package com.sprd.dialer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.dialer.R;
import com.sprd.android.config.OptConfig;

/**
 * Dialog that show which sim call log to view
 */
public class CallLogFilterFragment extends DialogFragment {
    private static final String TAG = "CallLogFilterFragment";
    private final int sOFFSET = 1;

    public static final String SHOW_TYPE = "which_sim_to_view";
    public static final int TYPE_ALL = -1;

    /** Preferred way to show this dialog */
    public static void show(FragmentManager fragmentManager) {
        CallLogFilterFragment dialog = new CallLogFilterFragment();
        try {
            dialog.show(fragmentManager, TAG);
        } catch (Exception ex) {
            Log.i(TAG, "No problem - the activity is no longer available to display the dialog");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity().getApplicationContext();

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                int position = which - sOFFSET;
                setCallLogShowType(context, position);
                dialog.dismiss();
            }
        };

        CharSequence[] items = getItems(context);
        int checkedItem = getCallLogShowType(context) + sOFFSET;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setSingleChoiceItems(items, checkedItem, listener);
        return builder.setCancelable(true).create();
    }

    private CharSequence[] getItems(Context context){
        TelephonyManager teleMgr =(TelephonyManager) getActivity()
                .getSystemService(Context.TELEPHONY_SERVICE);
        int phoneCount = teleMgr.getPhoneCount();
        CharSequence[] items = new CharSequence[phoneCount + sOFFSET];
        items[0] = context.getString(R.string.item_all_calls);
        for (int i = 0; i < phoneCount; i++) {
            int index = i + sOFFSET;
            /*sunvov:dlj add for change sim1 sim2 to master sim slave sim 160805 start {@*/
          		items[index] = context.getString(R.string.item_sim_calls, index);
          		if(OptConfig.SUNVOV_CUSTOM_C7301_XLL_FWVGA){
          			String str=(String)items[index];
	          		if(index==1){
	          			items[index]=str.replace("SIM1","Master SIM");
	          		}else if(index==2){
	          			items[index]=str.replace("SIM2","Slave SIM");
	          		}
          		}
          	/*sunvov:dlj add for change sim1 sim2 to master sim slave sim 160805 end @}*/
            
        }
        return items;
    }

    public static int getCallLogShowType(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int type = prefs.getInt(SHOW_TYPE, TYPE_ALL);
        return type;
    }

    public static void setCallLogShowType(Context context, int which) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putInt(SHOW_TYPE, which);
        ed.apply();
    }
}
