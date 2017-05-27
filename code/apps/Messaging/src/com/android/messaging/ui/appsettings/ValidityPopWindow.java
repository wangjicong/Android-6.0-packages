package com.android.messaging.ui.appsettings;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.telephony.SmsManager;
import android.graphics.drawable.ColorDrawable;
import android.preference.Preference;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.android.messaging.R;
import com.android.messaging.sms.SystemProperties;
import com.android.messaging.util.PhoneUtils;
import com.android.messaging.util.LogUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidityPopWindow extends PopupWindow implements AdapterView.OnItemClickListener {

    private View mContentView;
    private Context mContext;
    private String mKey;
    private Preference mPref;
    private ListView mlistValidity;
    private List<String> mValidityKeyList;
    private List<String> mValidityDataList;

    private ISaveData mcbf = new DefaultSaveDataImpl();
    private final String TAG = ValidityPopWindow.this.getClass().getSimpleName();

    public ValidityPopWindow(final Context context) {
        mContext = context;
        Resources res = ((Activity) mContext).getResources();
        mValidityKeyList = new ArrayList<String>();
        Collections.addAll(mValidityKeyList, res.getStringArray(R.array.mms_validity_pref_entries));
        mValidityDataList = new ArrayList<String>();
        Collections.addAll(mValidityDataList, res.getStringArray(R.array.mms_validity_pref_entry_values));

        final String mccMnc = PhoneUtils.getMccMncString(PhoneUtils.getDefault().getMccMnc());
        if ("440".equals(mccMnc.substring(0, 3))) {
            if (mValidityKeyList.size() > 0 && mValidityKeyList.size() > 0) {
                mValidityKeyList.remove(mValidityKeyList.size() - 1);
                mValidityDataList.remove(mValidityDataList.size() - 1);
            }
        }
    }

    //get the validity list
    public void initPopWin(Preference preference, String key) {
        mKey = key;
        mPref = preference;
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContentView = inflater.inflate(R.layout.validity_list, null);
        mlistValidity = (ListView) mContentView.findViewById(R.id.validity_list);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, mValidityKeyList);
        mlistValidity.setAdapter(adapter);
        mlistValidity.setOnItemClickListener(this);
        int h = ((Activity) mContext).getWindowManager().getDefaultDisplay().getHeight();
        int w = ((Activity) mContext).getWindowManager().getDefaultDisplay().getWidth();
        this.setContentView(mContentView);
        this.setWidth(w / 2);
        this.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        this.setFocusable(true);
        this.setOutsideTouchable(true);
        ColorDrawable dw = new ColorDrawable(0x00000000);
        this.setBackgroundDrawable(dw);
        this.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams params = ((Activity) mContext).getWindow().getAttributes();
                params.alpha = 1f;
                ((Activity) mContext).getWindow().setAttributes(params);
            }
        });
        this.update();
    }

    public void showPopupWindow() {
        if (!this.isShowing()) {
            WindowManager.LayoutParams params = ((Activity) mContext).getWindow().getAttributes();
            params.alpha = 0.7f;
            ((Activity) mContext).getWindow().setAttributes(params);
            showAtLocation(mContentView, Gravity.CENTER, 0, 0);

        } else {
            this.dismiss();
        }
    }

    public void closePopWin() {
        this.dismiss();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        getInstance().saveData(mKey, mValidityDataList.get(position));
        mPref.setSummary(mValidityKeyList.get(position));
        closePopWin();
    }

    private View getContextView() {
        return mContentView;
    }

    private ListView getListView() {
        return mlistValidity;
    }

    private List<String> getValidityKeyList() {
        return mValidityKeyList;
    }

    private List<String> getValidityDataList() {
        return mValidityDataList;
    }

    public String getKeyValidity(String key) {
        String val = mcbf.readData(key, "-1");
        return getKeyFromList(val);
    }

    public String getKeyFromList(String value) {
        if (value.equals("") || !mValidityDataList.contains(value)) {
            return null;
        } else {
            return mValidityKeyList.get(mValidityDataList.indexOf(value));
        }
    }

    public ISaveData getInstance() {
        if (mcbf == null) {
            throw new RuntimeException("System Not Set SaveData Interface");
        }
        return mcbf;
    }

    public void setSaveData(ISaveData cbf) {
        mcbf = cbf;
    }

    /**
     * *****************************************************************************
     * read or save the validity value
     * ******************************************************************************
     */
    class DefaultSaveDataImpl implements ISaveData {

        public DefaultSaveDataImpl() {
        }

        public boolean saveData(String szKey, String szValue) {
            //    Settings.Global.putInt(mContext.getContentResolver(), szKey, Integer.parseInt(szValue));
            SmsManager manager = SmsManager.getDefault();
            manager.setProperty(szKey, szValue);
            return true;
        }

        public String readData(String szKey, String nDefault) {
            //     int value = Settings.Global.getInt(mContext.getContentResolver(), szKey, Integer.parseInt(nDefault));
            //SmsManager manager = SmsManager.getDefault();
            String value = SystemProperties.get(szKey);
            return value.isEmpty() ? nDefault : value;
        }
    }

    public interface ISaveData {
        public boolean saveData(String szKey, String szValue);

        public String readData(String szKey, String nDefault);
    }
}
