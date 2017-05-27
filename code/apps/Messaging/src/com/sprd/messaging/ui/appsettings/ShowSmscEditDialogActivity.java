
package com.sprd.messaging.ui.appsettings;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.util.Log;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.preference.Preference;

import com.android.messaging.Factory;
import com.android.messaging.OperatorFactory;
import com.android.messaging.sms.MmsSmsUtils;
import com.android.messaging.util.Assert;
import com.android.messaging.util.BuglePrefs;
import com.android.messaging.R;

public class ShowSmscEditDialogActivity extends Activity {
    private int mSubId;

    private Context getContext() {
        return ShowSmscEditDialogActivity.this;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smsc_dialog_background_ex);
        Intent intent = getIntent();
        mSubId = intent.getIntExtra("subId", -1);
        show();
    }

    private void show() {
        AlertDialog.Builder editDialog = new AlertDialog.Builder(getContext());
        // Assert.isNull(editDialog);
        final EditText editSmsc = new EditText(editDialog.getContext());
        OperatorFactory.setViewEnabled(editSmsc);
        editSmsc.setText(SmscManager.getSmscString(getContext(), mSubId));
        final String oldSmsc = editSmsc.getText().toString();
        editDialog
                .setView(editSmsc)
                .setTitle(R.string.pref_title_manage_sim_smsc)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newSmsc = editSmsc.getText().toString();
                        System.out.println("[ShowSmscEditDialog]=====newSmsc===>" + newSmsc);
                        if ((!TextUtils.isEmpty(newSmsc)) && (!newSmsc.equals(oldSmsc))) {
                            boolean setResult = false;
                            setResult = SmscManager.setSmscString(getContext(), newSmsc, mSubId);
                            System.out.println("[ShowSmscEditDialog]=====setResult====>"
                                    + setResult);

                            if (!setResult) {
                                editSmsc.setText(SmscManager.getSmscString(getContext(), mSubId));
                            }
                        }
                        if (TextUtils.isEmpty(newSmsc)) {
                            Toast.makeText(getContext(), R.string.smsc_cannot_be_null,
                                    Toast.LENGTH_LONG).show();
                        }
                        finishActivity();
                    }
                })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                finishActivity();
                            }
                        })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        Log.d("ShowSmscEditDialogActivity",
                                "========onDismiss====");
                        finishActivity();
                    }
                }).show();
    }

    private void finishActivity() {
        ShowSmscEditDialogActivity.this.finish();
    }

}
