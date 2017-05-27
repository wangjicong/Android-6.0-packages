package com.sprd.ext.unreadnotifier;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.ArrayList;

/**
 * Created by SPRD on 2016/11/22.
 */

public abstract class UnreadBaseItem {
    protected int mType;
    private int mUnreadCount;
    protected BaseContentObserver mContentObserver;
    protected String mPermission;
    protected String mPrefKey;
    public Context mContext;
    protected ComponentName mDefaultCn;
    protected boolean mDefaultState;

    public String mCurrentCn = "";
    ArrayList<String> mInstalledList = new ArrayList<>();

    public UnreadBaseItem(Context context) {
        mContext = context;
    }

    public boolean checkPermission() {
        boolean isChecked = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isChecked = mContext.checkSelfPermission(mPermission) == PackageManager.PERMISSION_GRANTED;
        }
        return isChecked;
    }

    public boolean isPersistChecked() {
        return AppListPreference.isPreferenceChecked(mContext, mPrefKey, mDefaultState);
    }

    /**
     * Send broadcast to update the unread info.
     */
    void updateUIFromDatabase() {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return readUnreadCount();
            }

            @Override
            protected void onPostExecute(Integer unReadNum) {
                setUnreadCount(unReadNum);
                UnreadInfoManager.updateUI(mContext, mCurrentCn);
            }
        }.execute();

    }

    ComponentName getCurrentComponentName() {
        ComponentName componentName = null;
        String value = readSavedValues();
        if (!TextUtils.isEmpty(value)) {
            componentName = ComponentName.unflattenFromString(value);
        }

        return componentName;
    }

    void setUnreadCount(int num) {
        if (num >= 0) {
            mUnreadCount = num;
        }
    }

    int getUnreadCount() {
        return mUnreadCount;
    }

    public String readSavedValues() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        return sp.getString(mPrefKey, "");
    }

    private void saveValues(String value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        editor.putString(mPrefKey, value);
        editor.apply();
    }

    public void verifyDefaultCN(final ArrayList<String> installedList, final ComponentName defCn) {
        if (installedList.size() == 0) {
            return;
        }

        String savedValue = readSavedValues();

        int N = installedList.size();
        String cN = null;

        if (N == 1) {
            cN = installedList.get(0);
            mCurrentCn = cN;
            if (!cN.equals(savedValue)) {
                saveValues(cN);
            }
        } else {
            String defaultCn = null;
            if(defCn != null) {
                defaultCn = defCn.flattenToShortString();
            }

            boolean isSavedValueEmpty = TextUtils.isEmpty(savedValue);
            boolean isDefaultCNEmpty = TextUtils.isEmpty(defaultCn);

            boolean isSavedValueFound = false;
            boolean isDefaultCNFound = false;
            for (String cName: installedList) {
                //if the default app value is existed
                if(!isDefaultCNEmpty && !isDefaultCNFound) {
                    if (cName.equals(defaultCn)) {
                        cN = defaultCn;
                        isDefaultCNFound = true;
                    }
                }

                //if the current app value is existed
                if(!isSavedValueEmpty && !isSavedValueFound) {
                    if (cName.equals(savedValue)) {
                        isSavedValueFound = true;
                    }
                }

            }

            if (isDefaultCNFound) {
                if(isSavedValueEmpty || !isSavedValueFound) {
                    mCurrentCn = cN;
                    saveValues(mCurrentCn);
                } else {
                    mCurrentCn = savedValue;
                }
            } else {
                if (isSavedValueEmpty || !isSavedValueFound) {
                    mCurrentCn = "";
                    saveValues(mCurrentCn);
                } else {
                    mCurrentCn = savedValue;
                }
            }
        }
    }

    public void setInstalledList(ArrayList<String> list) {
        mInstalledList = list;
    }

    public abstract int readUnreadCount();

    public abstract String getUnreadHintString();

    public abstract ArrayList<String> loadApps(Context context);

}
