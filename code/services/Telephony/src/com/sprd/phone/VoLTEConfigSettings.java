/*
 * SPRD: Add for plmn white list
 *
 */
package com.sprd.phone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.phone.R;
import com.sprd.android.internal.telephony.VolteConfig;

public class VoLTEConfigSettings extends Activity{

    private String LOG_TAG = "VoLTEConfigSettings";
    private final int MENU_ADD = Menu.FIRST;
    private final int INVALID_PLMN_POSITION = -1;
    private VolteConfig mVolteConfig;
    private List<String> mAllList = new ArrayList<String>();
    private List<String> mPrebuiltPlmnList = new ArrayList<String>();
    private List<String> mUserPlmnList = new ArrayList<String>();
    private VoListAdapter mPlmnListAdapter;
    private ListView mListView = null;
    private SharedPreferences mSharedPreferences = null;
    private SharedPreferences.Editor mEditor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.volte_config_list);
        mListView =  (ListView) findViewById(R.id.plmn_config_list);
        mVolteConfig = VolteConfig.getInstance();
        mSharedPreferences = getSharedPreferences("volteconfig", Activity.MODE_WORLD_READABLE);
        mEditor = mSharedPreferences.edit();
        mPrebuiltPlmnList = mVolteConfig.getPrebuiltConfig();
        loadSharedPreVolist();
        getAllVoList();
        mPlmnListAdapter = new VoListAdapter(this,mAllList);
        mListView.setAdapter(mPlmnListAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ADD, 0, R.string.menu_new_plmn)
                .setIcon(android.R.drawable.ic_menu_add);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ADD:
                showDialog("add", "", INVALID_PLMN_POSITION);
                return true;
            default :
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isUserPlmnExisted(String plmn) {
        for (int i = 0; i < mAllList.size(); i++) {
            if (mAllList.get(i).equals(plmn)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPrebuiltPlmnExisted(String plmn) {
        for (int i = 0; i < mPrebuiltPlmnList.size(); i++) {
            if (mPrebuiltPlmnList.get(i).equals(plmn)) {
                return true;
            }
        }
        return false;
    }

    private void showDialog(final String action, final String oldPlmn,
            final int position) {
        LayoutInflater factory = LayoutInflater.from(VoLTEConfigSettings.this);
        final View view = factory.inflate(R.layout.volist_add_ex, null);
        final EditText editText = (EditText) view.findViewById(R.id.plmn_value);
        final String editAction = VoLTEConfigSettings.this.getString(R.string.plmn_edit_tip);
        final String newAction = VoLTEConfigSettings.this.getString(R.string.plmn_new_tip);
        final boolean isEditPlmn = action.equals(editAction);
        if (isEditPlmn) {
            editText.setText(oldPlmn);
        } else {
            editText.setHint(R.string.enter_plmn_hint);
        }
        AlertDialog dialog = new AlertDialog.Builder(VoLTEConfigSettings.this)
                .setTitle(isEditPlmn
                        ? editAction : newAction)
                .setView(view)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                final String addValue = editText.getText()
                                        .toString();
                                if (!checkInputValidation(addValue)) {
                                    if (isEditPlmn) {
                                        editPlmn(oldPlmn, addValue, position);
                                    } else {
                                        addPlmn(addValue);
                                    }
                                    mPlmnListAdapter.notifyDataSetChanged();
                                    Log.d(LOG_TAG,"action : " + action
                                            + "; add : " + addValue + ";old : " + oldPlmn);
                                }
                            }
                        }).setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();
    }

    private void getAllVoList() {
        mAllList.addAll(mPrebuiltPlmnList);
        mAllList.addAll(mUserPlmnList);
        Log.d(LOG_TAG, "mUserPlmnList : " + mUserPlmnList + "\n mPrebuiltPlmnList : " + mPrebuiltPlmnList);
    }

    private void loadSharedPreVolist() {
        Map<String, String> carrierVoMap = new HashMap<String, String>();
        carrierVoMap = (Map<String, String>) mSharedPreferences.getAll();
        for (String key : carrierVoMap.keySet()) {
            mUserPlmnList.add(key);
        }
    }

    private void addPlmn(String key){
        mAllList.add(key);
        mEditor.putBoolean(key, true);
        mEditor.commit();
    }

    private void deletePlmn(String key, int position){
        mAllList.remove(position);
        mEditor.remove(key);
        mEditor.commit();
    }

    private void editPlmn(String oldKey, String newKey, int position) {
        mAllList.remove(position);
        mAllList.add(newKey);
        mEditor.remove(oldKey);
        mEditor.putBoolean(newKey, true);
        mEditor.commit();
    }

    private boolean checkInputValidation(String plmn) {
        boolean isWrong = false;
        if (plmn == null || plmn.length() == 0 || plmn.length() < 5) {
            isWrong = true;
            Toast.makeText(VoLTEConfigSettings.this, getString(R.string.plmn_enter_error),
                    Toast.LENGTH_SHORT).show();
            return isWrong;
        } else if(isUserPlmnExisted(plmn)){
            isWrong = true;
            Toast.makeText(VoLTEConfigSettings.this, getString(R.string.plmn_exist),
                    Toast.LENGTH_SHORT).show();
            return isWrong;
        }
        return isWrong;
    }

    public class VoListAdapter extends BaseAdapter {

        private Context context;
        private List<String> voList;

        private VoListAdapter(Context context, List<String> list) {
            this.context = context;
            if (list != null) {
                this.voList = list;
            } else {
                list = new ArrayList<String>();
            }
        }

        @Override
        public int getCount() {
            return voList.size();
        }

        @Override
        public Object getItem(int position) {
            return voList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup vewGroup) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.volist_item_view_ex,
                        null);
            }
            final TextView textView = (TextView) convertView.findViewById(R.id.tv_plmn);
            ImageButton imaBtnDelete = (ImageButton) convertView.findViewById(R.id.imaBtn_delete);
            ImageButton imaBtnEdit = (ImageButton) convertView.findViewById(R.id.imaBtn_edit);
            textView.setText(voList.get(position));
            if (isPrebuiltPlmnExisted(voList.get(position))) {
                textView.setEnabled(false);
                imaBtnDelete.setVisibility(View.GONE);
                imaBtnEdit.setVisibility(View.GONE);
            } else {
                textView.setEnabled(true);
                imaBtnDelete.setVisibility(View.VISIBLE);
                imaBtnEdit.setVisibility(View.VISIBLE);
            }
            imaBtnDelete.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(LOG_TAG, "delete plmn : " + textView.getText()
                            + ";position : " + position);
                    deletePlmn(textView.getText().toString(), position);
                    mPlmnListAdapter.notifyDataSetChanged();
                }
            });
            imaBtnEdit.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    showDialog("Edit", textView.getText().toString(), position);
                }
            });
            return convertView;
        }
    }

}
