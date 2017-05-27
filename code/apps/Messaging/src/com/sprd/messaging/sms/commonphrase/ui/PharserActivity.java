
package com.sprd.messaging.sms.commonphrase.ui;

import java.util.ArrayList;
import java.util.Collection;

import com.android.messaging.R;
import com.sprd.messaging.sms.commonphrase.model.ItemData;
import com.sprd.messaging.sms.commonphrase.model.PharserManager;

import android.support.v7.app.ActionBarActivity;

import android.os.Bundle;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.text.InputFilter;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import static com.sprd.messaging.sms.commonphrase.model.IModify.OP_INSERT;

    /* Modify by SPRD for Bug:505782 2015.11.30 Start */
//public class PharserActivity extends ActionBarActivity {
public class PharserActivity extends ActionBarActivity implements PharserListAdapter.PharserHost {
    /* Modify by SPRD for Bug:505782 2015.11.30 End */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pharser_activity_ex);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        /* And by SPRD for Bug:505782 2015.11.30 Start */
        // Fixed know bug: Delete one or more phrases, pressing finish, and a new phrase,
        // switch system language, back to ui, the new phrase disappear.
        // Load data from DB:
        // 1. If Intent from caller has not extras, savedInstanceState is null.
        // 2. savedInstanceState dose not contain KEY_HAS_LOADED.
        // 3. If contains, the boolean value is false.
        if (savedInstanceState == null || !savedInstanceState.getBoolean(KEY_HAS_LOADED, false)) {
            Log.d(TAG, "onCreate...will load data from DB!");
            PharserManager.getInstance().LoadFromDatabase(getContext());
        } else {
            Log.d(TAG, "onCreate...will not load data from DB!");
        }
        /* And by SPRD for Bug:505782 2015.11.30 End */

        PharserManager.getInstance().MapToArrayList(mDataList);
        init();
        reload(); //Add by SPRD for Bug:522913 2016.02.03
    }

    /* And by SPRD for Bug:509485 2015.12.11 Start */
    private void reload() {
        PharserManager.getInstance().reloadFromDB(this);
        OnDataChange();
    }
    /* And by SPRD for Bug:509485 2015.12.11 End */

    private void init() {

        mListView = (ListView) findViewById(R.id.pharser_list_view);
        mListView.setOnItemClickListener(new ListViewItemClick());
        mEmptyView = (View) findViewById(R.id.empty);

        mListAdapter = new PharserListAdapter(getContext(), getDataSource());

        /* And by SPRD for Bug:505782 2015.11.30 Start */
        mListAdapter.setHost(this);
        /* And by SPRD for Bug:505782 2015.11.30 End */

        getListView().setAdapter(mListAdapter);
        getAdarpter().notifyDataSetChanged();

        mCallback = new MultiModeCallBack(getContext());
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setMultiChoiceModeListener(mCallback);
        Log.d(TAG, "mListAdapter.getCount():" + mListAdapter.getCount());
        if(mListAdapter.getCount() == 0) {
            mListView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mListView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
        }
    }

    public static ArrayList<String> intentCommonPhrase(int intentType, Context context) { // use
                                                                                          // for
                                                                                          // mms
                                                                                          // or
                                                                                          // phone
        Cursor cursor = PharserManager.LoadFromDatabase(intentType, context);
        if (cursor == null || cursor.getCount() == 0) {
            return null;
        } else {
            return PharserManager.intentArray(cursor);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.clear();
        menu.add(0, OP_INSERT, 0, R.string.add).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    public void startActionmode() {
        if (mActionMode == null) {
            mActionMode = startActionMode(mActionModeCallBack);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case OP_INSERT: {
                showItemDetailDialog();
                break;
            }
            case android.R.id.home:
                Log.d(TAG, "=====zhongjihao======onOptionsItemSelected===home=========");
                if(isEditPharse){
                    createAlertDialog();
                    return true;
                }
                reload();
                finish();
                break;
        }
        return true;
    }

    private void showEditDialog(ItemData datainfo) {
        // TODO Auto-generated method stub
        AlertDialog.Builder builder = new AlertDialog.Builder(PharserActivity.this);
        builder.setTitle(R.string.edit_pharser).setMessage(datainfo.getPharser())
                .setPositiveButton(R.string.edit, new EditButtonListener(datainfo))
                .setNeutralButton(R.string.delete, new DeleteButtonListener(datainfo))
                .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    public void OnDataChange() {
            Log.d(TAG, "=========>MapToArrayList(mDataList)");
            mDataList.clear();
            PharserManager.getInstance().MapToArrayList(mDataList);
            for (ItemData item : mDataList) {
                item.Debug();
            }
            Log.d(TAG, "OnDataChange, getAdarpter().getCount():" + getAdarpter().getCount());
            if(getAdarpter().getCount() == 0) {
                mListView.setVisibility(View.GONE);
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                mListView.setVisibility(View.VISIBLE);
                mEmptyView.setVisibility(View.GONE);
            }
            getAdarpter().notifyDataSetChanged();
    }

    private Context getContext() {
        return PharserActivity.this;
    }

    private ListView getListView() {
        return mListView;
    }

    private PharserListAdapter getAdarpter() {
        return mListAdapter;
    }

    private ArrayList<ItemData> getDataSource() {
        return mDataList;
    }

    private ListView mListView;
    private ArrayList<ItemData> mDataList = new ArrayList<ItemData>();
    private PharserListAdapter mListAdapter;
    private View mEmptyView;

    private ActionMode mActionMode;
    private MultiModeCallBack mCallback;
    private static final int MAX_EDITABLE_LENGTH = 100;
    public static int PHRASE_SELECT = 1;// add for choose phrase
    private static String TAG = "PharserActivity";

    private ActionMode.Callback mActionModeCallBack = new ActionModeCallback();

    /********************************************************************************************************************************************************************
     * @author ActionModeCallback
     ******************************************************************************************************************************************************************/

    private class ActionModeCallback implements ActionMode.Callback {

        @SuppressLint("NewApi")
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // TODO Auto-generated method stub
            switch (item.getItemId()) {
                case R.id.finish:
                    PharserManager.getInstance().WriteToDisk(getContext());
                    reload();//SPRD: Add for Bug 515773
                    isEditPharse = false;
                    mode.finish();
                    return true;
                case R.id.cancel:

                    /* Modify by SPRD for Bug:509485 2015.12.11 Start */
//                    PharserManager.getInstance().RecoveryByID(mDataList);
//                    getAdarpter().notifyDataSetChanged();
                    reload();
                    /* Modify by SPRD for Bug:509485 2015.12.11 Start */
                    isEditPharse = false;
                    mode.finish();
                    return false;
               //sprd #513174
                case android.R.id.home:
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // TODO Auto-generated method stub
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.finish_menu, menu);
            menu.findItem(R.id.finish).setVisible(true);
            menu.findItem(R.id.cancel).setVisible(true);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            System.out.println("============onDestoryActionMode======");
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // TODO Auto-generated method stub
            return false;
        }
    }

    /********************************************************************************************************************************************************************
     * @author All Click Events
     ******************************************************************************************************************************************************************/
    private class BaseInfo {
        public BaseInfo(ItemData itemdata) {
            mitemdata = itemdata;
        }

        public BaseInfo(ItemData itemdata, EditText editText, String szOtherValue) {
            this(itemdata);
            mEditText = editText;
            mszValue = szOtherValue;
        }

        protected String getValues() {
            return mszValue;
        }

        protected ItemData getItemData() {
            return mitemdata;
        }

        private ItemData mitemdata;
        private String mszValue;

        private EditText mEditText;
    }

    private class DeleteButtonListener extends BaseInfo implements OnClickListener {
        public DeleteButtonListener(ItemData itemdata) {
            super(itemdata);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            PharserManager.getInstance().DelByID(getItemData().getRowID());
            OnDataChange();
            startActionmode();
            isEditPharse = true;
            dialog.dismiss();
        }
    }

    private class EditButtonListener extends BaseInfo implements OnClickListener {
        @SuppressLint("NewApi")
        public EditButtonListener(ItemData datainfo) {
            super(datainfo);
        }

        public void onClick(DialogInterface dialog, int which) {
            // TODO Auto-generated method stub
            dialog.dismiss();
            AlertDialog.Builder editDialog = new AlertDialog.Builder(PharserActivity.this);
            EditText edittext = new EditText(editDialog.getContext());
            edittext.computeScroll();
            edittext.setText(getItemData().getPharser());
            edittext.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(MAX_EDITABLE_LENGTH)
            });
            edittext.requestFocus();
            Log.d(TAG, "after click 'Edit'====>the str is:" + getItemData().getPharser());
            editDialog
                    .setTitle(R.string.edit_pharser)
                    .setView(edittext)
                    .setPositiveButton(
                            android.R.string.ok,
                            new UpdateButtonListener(getItemData(), edittext, edittext.getText()
                                    .toString()))
                    .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
        }

    }

    private class UpdateButtonListener extends BaseInfo implements OnClickListener {

        public UpdateButtonListener(ItemData datainfo, EditText editText, String szValue) {
            super(datainfo, editText, szValue);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            Log.d(TAG, "edit done,--after click ok====>current str is:"
                    + super.mEditText.getText().toString());

            if ((super.mEditText.getText().toString().trim().isEmpty())) {
                Toast.makeText(getContext(), getString(R.string.empty_pharser_not_save), Toast.LENGTH_LONG)
                        .show();
            } else if (!(getValues().equals(super.mEditText.getText().toString()))) {
                PharserManager.getInstance().updateByID(getItemData().getRowID(),
                        super.mEditText.getText().toString(), null);
                OnDataChange();
                startActionmode();
                isEditPharse = true;
            }
        }
    }

    class ListViewItemClick implements OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // TODO Auto-generated method stub
            if (getAdarpter() != null) {
                showEditDialog((ItemData) getAdarpter().getItem(position));
            }
        }
    };

    /********************************************************************************************************************************************************************
     * @author Show Edit Dialog
     ******************************************************************************************************************************************************************/

    private void showItemDetailDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());

        final EditText edit = new EditText(getContext());
        edit.setHint(R.string.type_to_pharser);
        edit.setFilters(new InputFilter[] {
            new InputFilter.LengthFilter(MAX_EDITABLE_LENGTH)
        });

        dialog.setTitle(R.string.add_pharser).setView(edit)
                .setPositiveButton(R.string.positive, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // TODO Auto-generated method stub
                        String str = edit.getText().toString();
                        if (str.trim().isEmpty()) {
                            Toast.makeText(getContext(), getString(R.string.empty_pharser_not_save),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            PharserManager.getInstance().addNewData(str, 1, 0);
                            OnDataChange();
                            startActionmode();
                            isEditPharse = true;
                        }

                    }
                //delete for sprd bug 510265
                })/*.setNeutralButton(R.string.save_as_teltype, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        String str = edit.getText().toString();
                        if (str.trim().isEmpty()) {
                            Toast.makeText(getContext(), getString(R.string.empty_pharser_not_save),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            PharserManager.getInstance().addNewData(str, 0, 1);
                            OnDataChange();
                            startActionmode();
                        }
                    }
                })*/.setNegativeButton(android.R.string.cancel, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    /****************************************************************************************************************
     * Multi-delete
     *************************************************************************************************************/
    public class MultiModeCallBack implements MultiChoiceModeListener {
        private View mMultiSelectActionBarView;
        // private TextView mSelectedCount;
        private Context mContext;

        public MultiModeCallBack(Context context) {
            mContext = context;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // TODO Auto-generated method stub
            switch (item.getItemId()) {
                case R.id.menu_cancel:
                    getAdarpter().setItemMultiCheckable(false);
                    getAdarpter().clearSelectedItem();
                    /* Modify by SPRD for Bug:509485 2015.12.11 Start */
//                    PharserManager.getInstance().RecoveryByID(mDataList);
//                    getAdarpter().notifyDataSetChanged();
                    reload();
                    /* Modify by SPRD for Bug:509485 2015.12.11 Start */
                    mode.finish();
                    break;
                case R.id.menu_delete:
                    Log.d(TAG, "SelectedItem.size()====>" + getAdarpter().SelectedItems().size());
                    for (ItemData itemdata : getAdarpter().SelectedItems()) {
                        PharserManager.getInstance().DelByID(itemdata.getRowID());
                    }
                    isEditPharse = true;
                    getAdarpter().notifyDataSetChanged();
                    mode.invalidate();
                    mode.finish();
                    OnDataChange();
                    if (getAdarpter().SelectedItems() != null) {
                        startActionmode();// If we d'not choose any item,should
                                          // not
                                          // invoke startActionmode()
                    }
                    break;
                default:
                    break;
            }
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // TODO Auto-generated method stub
            mode.getMenuInflater().inflate(R.menu.multi_actionmode_menu, menu);
            getAdarpter().setItemMultiCheckable(true);
            getAdarpter().notifyDataSetChanged();
            if (mMultiSelectActionBarView == null) {
                mMultiSelectActionBarView = LayoutInflater.from(mContext).inflate(
                        R.layout.list_multi_select_actionbar_ex, null);
                // mSelectedCount = (TextView) mMultiSelectActionBarView
                // .findViewById(R.id.selected_item_count);
            }
            mode.setCustomView(mMultiSelectActionBarView);
            // ((TextView) mMultiSelectActionBarView.findViewById(R.id.title))
            // .setText(R.string.select_item);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Log.i("cy", "onDestroyActionMode");
            getAdarpter().setItemMultiCheckable(false);
            getAdarpter().clearSelectedItem();
            getAdarpter().notifyDataSetChanged();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // TODO Auto-generated method stub
            return false;
        }

        @SuppressLint("NewApi")
        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                boolean checked) {
            if(getAdarpter() == null) return;
            if (checked) {
                ItemData item = null;
                Object obj = getAdarpter().getItem(position);
                if(obj == null){
                    return ;
                }
                if(obj instanceof ItemData)
                  item = (ItemData)obj;

                getAdarpter().addSelectedItem(item.getIndexOfArray());
                Log.d(TAG, "========the item is checked! Position is=====>" + position);
            } else {
                getAdarpter().cancelSelectedItem(position);
                Log.d(TAG, "========the item is unchecked! Position is=====>" + position);
            }
            getAdarpter().notifyDataSetChanged();
            mode.invalidate();
        }
        // public void updateSelectedCount(){
        // mSelectedCount.setText(Integer.toString(mListView.getCheckedItemCount())+"");//
        // }
    }

    /* And by SPRD for Bug:505782 2015.11.30 Start */
    private final static String KEY_HAS_LOADED = "k-h-l";
    private final static String KEY_SELECTED_POSITIONS = "k-s-p";
    private ArrayList<Integer> mSelectedPositions = new ArrayList<>();

    private boolean isEmptyCollection(Collection<?> c) {
        return c == null || c.isEmpty();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_HAS_LOADED, true);
        Log.d(TAG, "onSaveInstanceState...mSelectedPositions=" + mSelectedPositions);
        if (!isEmptyCollection(mSelectedPositions)) {
            outState.putIntegerArrayList(KEY_SELECTED_POSITIONS, mSelectedPositions);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        ArrayList<Integer> savedPositions = savedInstanceState.getIntegerArrayList(KEY_SELECTED_POSITIONS);
        if (!isEmptyCollection(savedPositions)) {
            for (Integer position : savedPositions) {
                if (getAdarpter() != null && position >= 0 && position < getAdarpter().getCount()) {
                    getAdarpter().addSelectedItem(position);
                }
            }
        }
    }

    @Override
    public void onSelectedItemAdded(Integer position) {
        if (!mSelectedPositions.contains(position)) {
            mSelectedPositions.add(position);
        }
    }

    @Override
    public void onSelectedItemRemoved(Integer position) {
        // If condition must be set, avoid invoking method remove(int index);
        if (mSelectedPositions.contains(position)) {
            mSelectedPositions.remove(position);
        }
    }

    @Override
    public void onAllSelectedIntemRemoved() {
        mSelectedPositions.clear();
    }
    /* And by SPRD for Bug:505782 2015.11.30 End */

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "====zhongjihao====onBackPressed===1===");
        if (keyCode == KeyEvent.KEYCODE_BACK ){
            Log.d(TAG, "====zhongjihao====onBackPressed===2===");
            if(isEditPharse){
                createAlertDialog();
                return true;
            }
        }
        return super.onKeyDown(keyCode,event);
    }

    private boolean isEditPharse = false;

    private void createAlertDialog(){
        AlertDialog.Builder isExit = new AlertDialog.Builder(this);
        isExit.setCancelable(false);
        isExit.setTitle(R.string.exit_pharser_dialog_title).setMessage(R.string.exit_pharser_dialog_content).setPositiveButton(android.R.string.ok,
                new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        PharserManager.getInstance().WriteToDisk(getContext());
                        reload();
                        isEditPharse = false;
                        finish();
                    }
                })
        .setNegativeButton(android.R.string.cancel, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                reload();
                isEditPharse = false;
                finish();
            }
        }).show();
    }
}
