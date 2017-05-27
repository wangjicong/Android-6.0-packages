
package com.sprd.messaging.ui.appsettings;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.os.Bundle;

//import android.app.ListActivity;
import android.support.v7.app.ActionBarActivity;
import android.widget.ArrayAdapter;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

import com.sprd.messaging.ui.appsettings.ShowSmscEditDialogActivity;
import com.sprd.messaging.ui.appsettings.SmscManager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;

import com.android.messaging.sms.MmsSmsUtils;

import android.view.View.OnClickListener;
import android.view.LayoutInflater;
import android.database.Cursor;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.RadioButton;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.CursorAdapter;

import java.util.ArrayList;

import com.android.messaging.OperatorFactory;
import com.android.messaging.R;

public class SmscListActivity extends ActionBarActivity {
    private ListView mSmscListView;
    private TextView mEmptyView;
    private int mSubId;
    protected SmscListAdapter mCursorAdapter;
    private Cursor mCursor;
    public static int mSelectIndex = 0;

    private Context getContext() {
        return SmscListActivity.this;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smsc_list_ex);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mSmscListView = (ListView) findViewById(R.id.list);
        mEmptyView = (TextView) findViewById(R.id.empty);
        Intent intent = getIntent();
        mSubId = intent.getIntExtra("subId", -1);

        mCursor = SmscManager.query(mSubId);
        if (mCursor == null) {
            Intent otherIntent = new Intent(this, ShowSmscEditDialogActivity.class);
            otherIntent.putExtra("subId", mSubId);
            startActivity(otherIntent);
            finish();
        } else {
            if (mCursorAdapter == null) {
                mCursorAdapter = new SmscListAdapter(this, mCursor);
                mSmscListView.setAdapter(mCursorAdapter);
                mSmscListView.setItemChecked(mSelectIndex, true);
            }
            mSmscListView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
                    TextView currentSmsc = (TextView) v.findViewById(R.id.smsc_number);
                    showDialog(currentSmsc, position);
                }
            });
        }
    }

    private void showDialog(final TextView tv, final int index) {
        AlertDialog.Builder editDialog = new AlertDialog.Builder(this);
        final EditText editSmsc = new EditText(editDialog.getContext());
        OperatorFactory.setViewEnabled(editSmsc);
        editSmsc.setText(tv.getText().toString());
        final String oldSmsc = editSmsc.getText().toString();

        editDialog
                .setView(editSmsc)
                .setTitle(R.string.pref_title_manage_sim_smsc)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newSmsc = editSmsc.getText().toString();
                        System.out.println("[SmscListActivity]====newSmsc:" + newSmsc);

                        if (!TextUtils.isEmpty(newSmsc)) {
                            if ((!newSmsc.equals(oldSmsc)) && MmsSmsUtils.isPhoneNumber(newSmsc)) {
                                SmscManager.getInstance().update(oldSmsc, newSmsc, index, mSubId); // update
                                                                                                   // iccprovider
                                tv.setText(newSmsc);// if yes,no need to invoke
                                                    // onDataChange()

                                if (index == mSelectIndex) {
                                    boolean result = SmscManager.setSmscString(getContext(),
                                            newSmsc, mSubId);
                                    if (!result) {
                                        editSmsc.setText(SmscManager.getSmscString(getContext(),
                                                mSubId));
                                    }
                                }
                                Toast.makeText(getContext(), R.string.smsc_update_success,
                                        Toast.LENGTH_LONG).show();
                                onDataChange();

                            } else if (!MmsSmsUtils.isPhoneNumber(newSmsc)) {
                                Toast.makeText(getContext(), R.string.smsc_should_be_number,
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(getContext(), R.string.smsc_cannot_be_null,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        editDialog.show();
    }

    private void onDataChange() {
        mCursor = SmscManager.query(mSubId);
        mCursorAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class SmscListAdapter extends CursorAdapter {
        private SmscListActivity mContext;

        private SmscListAdapter(SmscListActivity context, Cursor cursor) {
            super(context, cursor);
            mContext = context;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final TextView smscText = (TextView) view.findViewById(R.id.smsc_number);
            final RadioButton rbCheck = (RadioButton) view.findViewById(R.id.check_smsc);
            String smsc = cursor.getString(cursor.getColumnIndexOrThrow(SmscManager.COLUMNS[0]));
            final int currentPosition = cursor.getPosition();
            System.out.println("[SmscListActivity]====currrentposition:" + currentPosition);
            smscText.setText(smsc);
            if (!smsc.isEmpty()) {
                rbCheck.setVisibility(View.VISIBLE);
            } else {
                rbCheck.setVisibility(View.GONE);
            }
            rbCheck.setChecked(mContext.mSelectIndex == currentPosition);

            rbCheck.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v == rbCheck) {
                        mContext.mSelectIndex = currentPosition;
                        System.out.println("[SmscListActivity]====you click:" + currentPosition);
                        System.out.println("[SmscListActivity]====new smsc:"
                                + smscText.getText().toString());
                        boolean result = SmscManager.setSmscString(mContext, smscText.getText()
                                .toString(), mSubId);

                        notifyDataSetChanged();
                    }
                }
            });
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater mInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = mInflater.inflate(R.layout.smsc_list_item_checked, parent, false);
            return view;
        }
    }

}
