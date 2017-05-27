
package com.sprd.phone;

import com.android.phone.R;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.OnItemLongClickListener;

import com.android.internal.telephony.IccUtils;
import com.android.internal.telephony.PhoneFactory;

public class UplmnSettings extends Activity {
    private static final String LOG_TAG = "UplmnSettings";
    private static final boolean DBG = true;
    private ListView mListView = null;
    private ArrayAdapter mAdapter;
    private EditText mEditText01, mEditText02, mEditText03;
    private EventHandler mHandler;
    private String[] mShowUPLMN = null;
    private String[] mOriginalUPLMN = null;
    private String[] mStrUorG = null;
    private int[] mOrder = null;
    private List<String> mData = new ArrayList<String>();
    private List<Integer> mOffset = new ArrayList<Integer>();
    private int mSubId = 0;

    private int LEN_UNIT = 0;
    private final static int USIM_LEN_UNIT = 10;
    private final static int SIM_LEN_UNIT = 6;
    private int at_read_lenth = 0;
    private int uplmn_list_num = 0;

    private int ENENT_GET_UPLMNLEN = 0;
    private int EVENT_GET_UPLMN = 1;
    private int EVENT_SET_UPLMN = 2;
    private int EVENT_SET_UPLMN_DONE = 3;
    private int EVENT_NEW_UPLMN = 4;
    private int EVENT_NEW_UPLMN_DONE = 5;
    private int EVENT_DELETE_UPLMN = 6;
    private int EVENT_DELETE_UPLMN_DONE = 7;

    private int CMD_READ_BINARY = 176;
    private int CMD_GET_RESPONSE = 192;
    private int CMD_UPDATE_BINARY = 214;

    private int FILEID_USIM = 28512;
    private int FILEID_SIM = 28464;

    private static final int MENU_NEW_UPLMN =  0;
    private static final int MENU_DELETE_UPLMN = 1;

    private static final String STRING_OK = "OK";

    private int mFileID;

    private String AT_GET_UPLMNLEN = "AT+CRSM=192,28512,0,0,15,0,3F007FFF";
    private String AT_GET_SIM_UPLMNLEN = "AT+CRSM=192,28464,0,0,15,0,3F007FFF";
    private String AT_GET_UPLMN = "AT+CRSM=176,28512,0,0";
    private String AT_GET_SIM_UPLMN = "AT+CRSM=176,28464,0,0";
    private String AT_SET_UPLMN = "AT+CRSM=214,28512,0,0";
    private String AT_SET_SIM_UPLMN = "AT+CRSM=214,28464,0,0";
    private boolean mIsUsim;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Material_Settings);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uplmn_list_ex);
        /* SPRD: modify the bug385615 @{ */
        mSubId = this.getIntent().getIntExtra("sub_id", 0);
        mIsUsim = getIntent().getBooleanExtra("is_usim", false);
        LEN_UNIT = mIsUsim ? USIM_LEN_UNIT : SIM_LEN_UNIT;
        /* @} */
        mFileID = mIsUsim? FILEID_USIM : FILEID_SIM;
        if (DBG)
            Log.v(LOG_TAG, "mSubId=" + mSubId);
        initialPara();
        mListView = (ListView) findViewById(R.id.ListView01);
        mAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mData);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int position, long id) {
                showEditDialog(position,false);
            }
        });
        mListView.setOnItemLongClickListener(new OnItemLongClickListener(){
            public boolean onItemLongClick(AdapterView<?> parent,View view,int position, long id){
                final int pos= position;
                Log.i(LOG_TAG,"onItemLongClick, pos = "+pos);
                showConfirmDeleteDialog(pos);
                return true;
            }
        });
    }

    private void initialPara() {
        Looper looper;
        looper = Looper.myLooper();
        mHandler = new EventHandler(looper);
        mHandler.removeMessages(0);
        new Thread(new Runnable(){

            @Override
            public void run() {
                int len = 15;
                iccExchangeSimIOwithSubId(mFileID, CMD_GET_RESPONSE, len, ENENT_GET_UPLMNLEN);
            }
        }).start();
    }

    private void showEditDialog(int pos,boolean isNew) {
        final int offset = pos;
        final boolean isNewPlmn = isNew;
        LayoutInflater factory = LayoutInflater.from(UplmnSettings.this);
        final View view = factory.inflate(R.layout.uplmn_edit_ex, null);
        mEditText01 = (EditText) view.findViewById(R.id.index_value);
        mEditText02 = (EditText) view.findViewById(R.id.id_value);
        mEditText03 = (EditText) view.findViewById(R.id.type_value);
        mEditText01.setText(Integer.toString(pos));
        mEditText02.setText(isNewPlmn ? "" : mShowUPLMN[pos]);
        mEditText03.setText(isNewPlmn ? "" : "" + mOrder[pos]);
        AlertDialog dialog = new AlertDialog.Builder(UplmnSettings.this)
        // SPRD: Add for Bug 493978.
        .setTitle(R.string.uplmn_setting)
        .setView(view)
        .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,
                    int which) {
                final String editIndex = mEditText01.getText()
                        .toString();
                final String editId = mEditText02.getText()
                        .toString();
                final String editTag = mEditText03.getText()
                        .toString();
                if (!checkInputParametersIsWrong(editIndex,
                        editId, editTag)) {
                    Message m = mHandler.obtainMessage(
                            isNewPlmn ? EVENT_NEW_UPLMN:EVENT_SET_UPLMN,0,0,0);
                    m.arg1 = offset;
                    mHandler.sendMessage(m);
                }
            }
        }).setNegativeButton(android.R.string.cancel, null)
        .create();
        dialog.show();
    }


    private boolean checkInputParametersIsWrong(String str01, String str02,
            String str03) {
        boolean IsWrong = false;
        /* SPRD: modify the bug367475 @{ */
        Matcher m = Pattern.compile("[^0-9]").matcher(str03);
        if (m.matches()) {
            IsWrong = true;
            DisplayToast(getString(R.string.type_is_wrong_uplmn));
            return IsWrong;
        }
        /* @} */
        /* SPRD:Add for 494642. The UplmnId must be a number. @{ */
        Matcher checkId = Pattern.compile("[0-9]+").matcher(str02);
        if (!checkId.matches()) {
            IsWrong = true;
            DisplayToast(getString(R.string.id_is_wrong_uplmn));
            return IsWrong;
        }
        /* @} */
        if (str01.length() == 0) {
            IsWrong = true;
            DisplayToast(getString(R.string.index_error_uplmn));
            return IsWrong;
        }
        if (str03.length() == 0) {
            IsWrong = true;
            DisplayToast(getString(R.string.type_is_emplty_error_uplmn));
            return IsWrong;
        }
        if (str02.length() < 5) {
            IsWrong = true;
            DisplayToast(getString(R.string.number_too_short_uplmn));
            return IsWrong;
        }
        if (Integer.parseInt(str03) > 3 || Integer.parseInt(str03) < 0) {
            IsWrong = true;
            DisplayToast(getString(R.string.type_is_wrong_uplmn));
            return IsWrong;
        }
        return IsWrong;

    }

    private void DisplayToast(String str) {
        Toast mToast = Toast.makeText(this, str, Toast.LENGTH_SHORT);
        mToast.show();
    }

    private String changeDataFromAT(String s) {
        String string = "";
        if (s.charAt(2) == 'F') {
            char[] c = new char[5];
            c[0] = s.charAt(1);
            c[1] = s.charAt(0);
            c[2] = s.charAt(3);
            c[3] = s.charAt(5);
            c[4] = s.charAt(4);
            string = String.valueOf(c);
        } else {
            char[] c = new char[6];
            c[0] = s.charAt(1);
            c[1] = s.charAt(0);
            c[2] = s.charAt(3);
            c[3] = s.charAt(5);
            c[4] = s.charAt(4);
            c[5] = s.charAt(2);
            string = String.valueOf(c);
        }
        return string;
    }

    private String changeDataFromEdit(String s) {
        String string = "";
        char[] c = new char[6];
        if (s.length() == 5) {
            c[0] = s.charAt(1);
            c[1] = s.charAt(0);
            c[2] = 'F';
            c[3] = s.charAt(2);
            c[4] = s.charAt(4);
            c[5] = s.charAt(3);
            string = String.valueOf(c);
        } else if (s.length() == 6) {
            c[0] = s.charAt(1);
            c[1] = s.charAt(0);
            c[2] = s.charAt(5);
            c[3] = s.charAt(2);
            c[4] = s.charAt(4);
            c[5] = s.charAt(3);
            string = String.valueOf(c);
        }
        return string;
    }

    private String transferUTRANorGSM(String s) {
        String string = "";
        if (s.equals("0")) {
            string = "8000";
        } else if (s.equals("1")) {
            string = "0080";
        } else if (s.equals("2")) {
            string = "4000";
        } else if (s.equals("3")) {
            string = "C0C0";
        }
        return string;
    }

    private void setAllParameters(int len) {
        mOriginalUPLMN = new String[len];
        mShowUPLMN = new String[len];
        mStrUorG = new String[len];
        mOrder = new int[len];
    }

    private String getLenStringFromResponse(String str) {
        int total_len = str.length();
        int offset = 4;
        int index = 0;
        String strBody = str.substring(4, total_len);
        String result = "0";
        while (index < strBody.length()) {
            int len = transferStringToInt(strBody.substring(index + 2, index + 4));
            if ((strBody.substring(index, index + 2)).equals("80")) {
                result = strBody.substring(index + offset, index + offset + 2 * len);
                break;
            }
            index = index + len * 2 + offset;
        }
        return result;
    }

    private int transferStringToInt(String str) {
        int num = Integer.parseInt(str, 16);
        return num;
    }

    private void sendMessageToGetUPLMNList(int length) {
        mHandler.removeMessages(0);
        final int len = length;
        new Thread(new Runnable(){

            @Override
            public void run() {
                iccExchangeSimIOwithSubId(mFileID, CMD_READ_BINARY, len, EVENT_GET_UPLMN);
            }
        }).start();
    }

    // end display
    private class EventHandler extends Handler {
        public EventHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ENENT_GET_UPLMNLEN) {
                String responseValue = (String)msg.obj;
                if(DBG)
                    Log.d(LOG_TAG, "ENENT_GET_UPLMNLEN: responseValue = " + responseValue);
                String lenString = null;
                if(responseValue.length() <= 4){
                    lenString = responseValue;
                }else if(responseValue.length()>=8){
                    lenString = responseValue.substring(4, 8);
                }
                at_read_lenth = Integer.parseInt(lenString, 16);
                if (at_read_lenth >= 250) {
                    at_read_lenth = 100;
                }
                uplmn_list_num = mIsUsim ? (at_read_lenth / 5) : (at_read_lenth / 3);
                setAllParameters(uplmn_list_num);

                if (DBG) {
                    Log.d(LOG_TAG, "lenString=" + lenString);
                    Log.d(LOG_TAG, "at_read_lenth=" + at_read_lenth);
                    Log.d(LOG_TAG, "uplmn_list_num=" + uplmn_list_num);
                }

                sendMessageToGetUPLMNList(at_read_lenth);

            } else if (msg.what == EVENT_GET_UPLMN) {
                String responseValue = (String)msg.obj;
                if (DBG)
                    Log.v(LOG_TAG, "EVENT_GET_UPLMN: responseValue=" + responseValue);

                for (int i = 0; i < uplmn_list_num; i++) {
                    mOriginalUPLMN[i] = (responseValue.substring(i * LEN_UNIT, (i + 1) * LEN_UNIT)).toUpperCase();
                    if (!mOriginalUPLMN[i].substring(0, 2).equals("FF")) {
                        mShowUPLMN[i] = changeDataFromAT(mOriginalUPLMN[i].substring(0, 6));
                        if (DBG) {
                            Log.d(LOG_TAG, "mOriginalUPLMN[i]=" + mOriginalUPLMN[i]
                                    + ", mShowUPLMN[i]="
                                    + mShowUPLMN[i]);
                        }
                        if(mIsUsim){
                            setType(i);
                        }else{
                            mOrder[i] = 1;
                            mStrUorG[i] = "G";
                        }
                        mOffset.add(i);
                        mData.add(mShowUPLMN[i] + ":" + mStrUorG[i]);
                        mAdapter.notifyDataSetChanged();
                        if (DBG)
                            Log.d(LOG_TAG, "mOffset=" + mOffset + ", mData=" + mData);
                    }
                }

            } else if (msg.what == EVENT_SET_UPLMN) {
                String plmn = mEditText02.getText().toString();
                String type = mEditText03.getText().toString();
                String str1 = changeDataFromEdit(plmn);
                String str2 = transferUTRANorGSM(type);
                String repalce = str1 + str2;
                String str = "";
                int position = msg.arg1;
                if (DBG)
                    Log.v(LOG_TAG, "EVENT_SET_UPLMN: postion=" + position);
                for (int i = 0; i < uplmn_list_num; i++) {
                    if (i == mOffset.get(position)) {
                        mOriginalUPLMN[i] = mIsUsim ? repalce : repalce.substring(0, 6);
                    }
                    str = str + mOriginalUPLMN[i];
                }
                if (DBG)
                    Log.d(LOG_TAG, "EVENT_SET_UPLMN: plmn=" + plmn + ", type=" + type + ", str="
                            + str);

                final String replacePlmn = str;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        iccExchangeSimIOUpdate(mFileID, CMD_UPDATE_BINARY, replacePlmn, EVENT_SET_UPLMN_DONE);
                    }
                }).start();

            } else if (msg.what == EVENT_SET_UPLMN_DONE) {
                String responseValue = (String) msg.obj;
                Log.d(LOG_TAG, "EVENT_SET_UPLMN_DONE: responseValue = " + responseValue);
                if (responseValue != null) {
                    finish();
                } else {
                    DisplayToast(getString(R.string.set_uplmn_unsuccessful));
                }
            } else if(msg.what == EVENT_NEW_UPLMN){
                String plmn = mEditText02.getText().toString();
                String type = mEditText03.getText().toString();
                String str1 = changeDataFromEdit(plmn);
                String str2 = transferUTRANorGSM(type);
                String repalce = str1 + str2;
                String str = "";
                int position = msg.arg1;
                if (DBG)
                    Log.v(LOG_TAG, "EVENT_NEW_UPLMN: uplmn_list_num = " + uplmn_list_num + ", postion = " + position);
                if (position < uplmn_list_num) {
                    mOffset.add(position);
                    for (int i = 0; i < uplmn_list_num; i++) {
                        if (i == mOffset.get(position)) {
                            mOriginalUPLMN[i] = repalce;
                            mShowUPLMN[i] = changeDataFromEdit(mOriginalUPLMN[i].substring(0, 6));
                            setType(i);
                            mData.add(mShowUPLMN[i] + ":" + mStrUorG[i]);
                            mAdapter.notifyDataSetChanged();
                            if(!mIsUsim) {
                                mOriginalUPLMN[i] = mOriginalUPLMN[i].substring(0, 6);
                            }
                        }
                        str = str + mOriginalUPLMN[i];
                    }
                    if (DBG)
                        Log.d(LOG_TAG, "EVENT_NEW_UPLMN: plmn=" + plmn + ", type=" + type + ", str=" + str);
                    final String replacePlmn = str;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            iccExchangeSimIOUpdate(mFileID, CMD_UPDATE_BINARY, replacePlmn, EVENT_NEW_UPLMN_DONE);
                        }
                    }).start();
                }else{
                    DisplayToast(getString(R.string.uplmn_exceeds_capacity));
                }
            } else if(msg.what == EVENT_NEW_UPLMN_DONE){
                String responseValue = (String)msg.obj;
                Log.d(LOG_TAG, "EVENT_NEW_UPLMN_DONE: responseValue = " + responseValue);
                if (responseValue != null) {
                    finish();
                }
            } else if(msg.what == EVENT_DELETE_UPLMN){
                int position = msg.arg1;
                String str= "";
                for (int i = 0; i < uplmn_list_num; i++) {
                    if (i != mOffset.get(position)) {
                        str = str + mOriginalUPLMN[i];
                    }
                }
                for(int i = 0;i< LEN_UNIT;i++)
                    str = str + "F";
                final String deletePlmn = str;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        iccExchangeSimIOUpdate(mFileID, CMD_UPDATE_BINARY, deletePlmn, EVENT_DELETE_UPLMN_DONE);
                    }
                }).start();
            } else if(msg.what == EVENT_DELETE_UPLMN_DONE){
                String responseValue = (String) msg.obj;
                Log.d(LOG_TAG, "EVENT_DELETE_UPLMN_DONE: responseValue = " + responseValue);
                if (responseValue != null ) {
                    finish();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_NEW_UPLMN, 0,getResources().getString(R.string.menu_new_uplmn))
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_NEW_UPLMN:
            //showNewUplmnDialog();
            showEditDialog(mOffset.size(),true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showConfirmDeleteDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(UplmnSettings.this);
        builder.setMessage(R.string.menu_delete_uplmn)
        .setCancelable(true)
        .setPositiveButton(R.string.delete,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog,
                    int which) {
                Message m = mHandler.obtainMessage(EVENT_DELETE_UPLMN, 0, 0, 0);
                m.arg1 = position;
                mHandler.sendMessage(m);
            }
        }).setNegativeButton(R.string.cancel, null).show();
    }

    private void setType(int position) {
        String tag1 = mOriginalUPLMN[position].substring(6, 7);
        String tag2 = mOriginalUPLMN[position].substring(8, 9);
        String actUtran = formatBinaryType(Integer.toBinaryString(Integer.parseInt(tag1, 16)));
        String actGsm = formatBinaryType(Integer.toBinaryString(Integer.parseInt(tag2, 16)));
        if (DBG)
            Log.v(LOG_TAG, "setType: actUtran = "+ actUtran + ", actGsm = " +actGsm);
        boolean isUtran = "1".equals(actUtran.substring(0, 1));
        boolean isEutran = "1".equals(actUtran.substring(1, 2));
        boolean isGsm = "1".equals(actGsm.substring(0, 1)) || "1".equals(actGsm.substring(1, 2));
        if(isUtran && isEutran) {
            mOrder[position] = 3;
            mStrUorG[position] = "U/E/G";
        } else if(isUtran) {
            mOrder[position] = 0;
            mStrUorG[position] = "U";
        } else if(isEutran) {
            mOrder[position] = 2;
            mStrUorG[position] = "E";
        } else if(isGsm) {
            mOrder[position] = 1;
            mStrUorG[position] = "G";
        }
     }

    private String formatBinaryType(String act) {
        int length = act.length();
        for(int i = 0; i < 4 - length; i++ ) {
            act = "0" + act;
        }
        return act;
    }

    private void iccExchangeSimIOwithSubId(int fileID, int command, int len, int what) {
        Log.d(LOG_TAG, "iccExchangeSimIOwithSubId: fileID = " + fileID + ", command = " + command + ", len = " + len);
        byte[] result = TelephonyManager.from(UplmnSettings.this).iccExchangeSimIOwithSubId(fileID, command, 0, 0, len, "3F007FFF", mSubId);
        sendMessage(result, what);
    }

    private void iccExchangeSimIOUpdate(int fileID, int command, String data,
            int what) {
        Log.d(LOG_TAG, "iccExchangeSimIOUpdate: fileID = " + fileID + ", command = " + command + ", data = " + data);
        byte[] result = TelephonyManager.from(UplmnSettings.this).iccExchangeSimIOUpdate(fileID, command, 0, 0, at_read_lenth, data, "3F007FFF", mSubId);
        sendMessage(result, what);
    }

    private void sendMessage(byte[] result, int what) {
        String responValue = IccUtils.bytesToHexString(result);
        Log.d(LOG_TAG, "sendMessage: responValue = " + responValue);
        Message m = mHandler.obtainMessage(what, 0, 0, responValue);
        mHandler.sendMessage(m);
    }
}
