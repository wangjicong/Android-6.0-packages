package com.sprd.soundrecorder;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.soundrecorder.R;
import com.android.soundrecorder.Recorder;
import com.android.soundrecorder.SoundRecorder;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.Surface;

public class RecordingFileList extends ListActivity
        implements AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener , TextWatcher , Button.OnClickListener{

    private static final String TAG = "RecordingFileList";
    //fix bug 203115"checkbox state changes after changing language" on 20130820 begin
    private static final String BUNDLEKEY = "1";
    //fix bug 203115"checkbox state changes after changing language" on 20130820 end
    private static String DUPLICATE_NAME = "duplicate_name";
    private static int INPUT_MAX_LENGTH = 50;
    private ListView mListView;
    private CursorRecorderAdapter mAdapter;

    //modify by linying bug115502 begin
    private TextView remindTextView = null;
    //modify by linying bug115502 begin
    private HashMap<Integer,Boolean> checkboxes = new HashMap<Integer,Boolean>();
    private Map<Integer,RecorderItem> checkItem = new TreeMap<Integer,RecorderItem>();
    private List<RecorderItem> items = new ArrayList<RecorderItem>();
    private boolean mIsPause = false;
    private List<RecorderItem> mList = new ArrayList<RecorderItem>();
    LinearLayout mLayoutFooter;
    TextView mTextViewSelectCount;
    private Dialog mAlertDialog;
    private static final int START_RECORDING_DIALOG_SHOW = 1;
    public final static String SAVE_RECORD_TYPE = "recordType";
    public final static String SAVE_RECORD_TYPE_ID = "recordTypeId";
    public final static String SOUNDREOCRD_TYPE_AND_DTA = "soundrecord.type.and.data";
    //SPRD: fix bug 513105
    public final static String SYSTEM_VERSION = "systemVersion";
    public String mType = SoundRecorder.AUDIO_AMR;
    int index = 0;
    private boolean isFirstTime = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recording_file_list);
      //modify by linying bug115502 begin
        remindTextView = (TextView)findViewById(R.id.emptylist);
        //modify by linying bug115502 begin
        mLayoutFooter = (LinearLayout)findViewById(R.id.layout_footer);
        registerReceiver();
        
        //qiuyaobo,20170518,begin
        String defaultRecordType = getString(R.string.default_record_type);
        if(defaultRecordType.equals("audio/3gpp")){
    				mType = SoundRecorder.AUDIO_3GPP; //SoundRecorder.AUDIO_AMR;
    				index = 1;//0;        		
        }	
        //qiuyaobo,20170518,end
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG,"onResume");
        getCurrentTypeId();
        mIsPause = false;
        mListView = getListView();
        // fix bug 250910 add rename feature begin
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        // fix bug 250910 add rename feature end

        if (checkItem != null) {
            checkItem.clear();
        }

        mList = query();
        mAdapter = new CursorRecorderAdapter(mList, mActionMode != null ? false : true);
        setListAdapter(mAdapter);
        Log.i(TAG,"onResume update mAdapter.");
        // modify by linying bug115502 begin
        detectionList();
        // modify by linying bug115502 end

        if (mActionMode != null) {
            int checkedCount = 0;
            if (mList.size() == 0) {
                mActionMode.finish();
                return;
            }
            for (int i = 0; i < mList.size(); i++) {
                if (checkboxes.get(i) != null && checkboxes.get(i) == true) {
                    checkItem.put(i,mList.get(i));
                    checkedCount++;
                }
            }
            if(checkedCount == mList.size()){
                menu.findItem(R.id.item_select_all).setEnabled(false);
            }
            mActionMode.setTitle(String.valueOf(checkedCount));
            buttonClickableChanges();
         }

        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);
    }
    /* SPRD: delete for fix bug 256621 @{ */
    // //fix bug 203115"checkbox state changes after changing language" on
    // 20130820 begin
    // @Override
    // public void onSaveInstanceState(Bundle outState) {
    // super.onSaveInstanceState(outState);
    // outState.putSerializable(BUNDLEKEY, checkboxes);
    // }
    //
    // @SuppressWarnings("unchecked")
    // @Override
    // public void onRestoreInstanceState(Bundle savedInstanceState)
    // {
    // super.onRestoreInstanceState(savedInstanceState);
    // Log.e("TAG" , "checkboxes"+checkboxes.size());
    // if(savedInstanceState != null) {
    // checkboxes = (HashMap<Integer, Boolean>)
    // savedInstanceState.get(BUNDLEKEY);
    // }
    // }
    //fix bug 203115"checkbox state changes after changing language" on 20130820 end
    /* @} */

    /* SPRD: fix bug 257287  @{ */
    private BroadcastReceiver mExternalMountedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!mIsPause) {
                mList = query();
                mAdapter  = new CursorRecorderAdapter(mList , mActionMode != null ? false : true);
                setListAdapter(mAdapter);
                Log.i(TAG,"onReceive update mAdapter");
                detectionList();
            }
            clearContainer();
            buttonClickableChanges();
            if (mDelDlg != null && mDelDlg.isShowing()) {
                dismissDelDialog();
            }
        }

    };

    /*SPRD:fix bug 517044,Multi language, interface display is not full@{*/
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG,"onConfigurationChanged()");
        changeMenuString();
    }

    private void changeMenuString(){
        String selectAll = getString(R.string.menu_recording_list_select_all);
        String deselect = getString(R.string.menu_recording_list_deselect_all);
        if(menu != null){
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
                String ss = "...";
                if(selectAll.length()>14){
                    menu.findItem(R.id.item_select_all).setTitle(selectAll.substring(0, 11)+ss);
                }
                if(deselect.length()>12){
                    menu.findItem(R.id.item_deselect_all).setTitle(deselect.substring(0, 9)+ss);
                }
            }else{
                menu.findItem(R.id.item_select_all).setTitle(selectAll);
                menu.findItem(R.id.item_deselect_all).setTitle(deselect);
            }
        }
    }
    /*SPRD:fix bug 517044,Multi language, interface display is not full@}*/

    @Override
    public void onPause(){
        super.onPause();
        mIsPause = true;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d(TAG,"onDestroy");
        unregisterReceiver(mExternalMountedReceiver);
        if(mActionMode != null){
            mActionMode.finish();
        }
        dismissDelDialog();
        if(mAlertDialog != null){
            mAlertDialog.dismiss();
        }
    }
    private void registerReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addDataScheme("file");
        registerReceiver(mExternalMountedReceiver, intentFilter);
    }
    /* @} */


    /* SPRD: add for Bug 301423 - SoundRecorder does not have the entry for pathselect @{ */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (SoundRecorder.UNIVERSEUI_SUPPORT) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.options_menu_overlay, menu);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu.findItem(R.id.menu_select_more) != null && (mAdapter == null ||mAdapter.getCount() <= 0)) {
            menu.findItem(R.id.menu_select_more).setEnabled(false);
        }else{
            menu.findItem(R.id.menu_select_more).setEnabled(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
//            case R.id.menu_set_save_path:
//                if(!StorageInfos.isExternalStorageMounted() && !StorageInfos.isInternalStorageMounted()){
//                    Toast.makeText(this, R.string.stroage_not_mounted, Toast.LENGTH_LONG).show();
//                }else{
//                    intent = new Intent(RecordingFileList.this, PathSelect.class);
//                    startActivity(intent);
//                    return true;
//                }
//                break;
            case R.id.menu_select_type:
                mAlertDialog = new AlertDialog.Builder(RecordingFileList.this)
                .setTitle(R.string.select_file_type)
                .setSingleChoiceItems(new String[] {String.valueOf(getResources().getString(R.string.record_amr)),
                        String.valueOf(getResources().getString(R.string.record_3gpp))},index,
                        new DialogInterface.OnClickListener() {
                             public void onClick(DialogInterface dialog, int which) {
                                 Message m = new Message();
                                 m.what = START_RECORDING_DIALOG_SHOW;
                                 switch (which) {
                                 case 0:
                                     m.obj = SoundRecorder.AUDIO_AMR;
                                     index = which;
                                     break;
                                 case 1:
                                     m.obj = SoundRecorder.AUDIO_3GPP;
                                     index = which;
                                     break;
                                 default:
                                     m.obj = SoundRecorder.AUDIO_AMR;
                                 }
                                 hand.sendMessage(m);
                    }
               })
               .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                       mAlertDialog.dismiss();
                   }
               }).show();
                break;
            case R.id.menu_select_more:
                if (mActionMode == null && mAdapter != null) {
                    startActionMode(new itemLongClickCallback(mAdapter.findItem(0)));
                } else {
                    return false;
                }
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void saveRecordTypeAndSetting(){
        SharedPreferences recordSavePreferences = this.getSharedPreferences(SOUNDREOCRD_TYPE_AND_DTA, MODE_PRIVATE);
        SharedPreferences.Editor edit = recordSavePreferences.edit();
        edit.putString(RecordingFileList.SAVE_RECORD_TYPE, mType);
        edit.putInt(RecordingFileList.SAVE_RECORD_TYPE_ID, index);
        edit.commit();
        Log.i(TAG,"mType is saved:" + mType);
        Log.e(TAG,"mTypeId is saveds:" + index);
    }

    private Handler hand= new Handler(){

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG,"the Message is:" + msg.what);
            switch(msg.what){
                case START_RECORDING_DIALOG_SHOW:
                    mType=(String)msg.obj;
                    Log.i(TAG,"mType is:" + mType);
                    saveRecordTypeAndSetting();
                    mAlertDialog.dismiss();
                    break;
            }
        }
    };
    private int getCurrentTypeId(){
        SharedPreferences recordSavePreferences = this.getSharedPreferences(SOUNDREOCRD_TYPE_AND_DTA, MODE_PRIVATE);
        index = recordSavePreferences.getInt(RecordingFileList.SAVE_RECORD_TYPE_ID, index);
        return index;
    }
    /* @} */
    @Override
    public boolean onItemLongClick(AdapterView<?> adapter, View v, int pos, long id) {
        // boolean result = false;
        // RecorderItem item = mAdapter.findItem(pos);
        // if (item != null) {
        // RecorderItemClick click =
        // new RecorderItemClick(RecorderItemClick.LONG_CLICK, item);
        // click.show();
        // }
        /* SPRD: fix bug 250910 add rename feature@{ */
        if (mActionMode == null && mAdapter != null) {
            checkboxes.put(pos, true);
            RecorderItem item = mAdapter.findItem(pos);
            checkItem.put(pos, item);
            startActionMode(new itemLongClickCallback(mAdapter.findItem(pos)));
            if(isFirstTime) {
                buttonClickableChanges();
                isFirstTime = false;
           }
        } else {
            return false;
        }
        return true;
    }
    /* SPRD: delete for fix bug 256621 @{ */
    private ActionMode mActionMode = null;
    private Menu menu = null;
    private class itemLongClickCallback implements ActionMode.Callback {
        private RecorderItem items;

        public itemLongClickCallback(RecorderItem item) {
            this.items = item;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mActionMode = mode;
            RecordingFileList.this.menu = menu;
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.mutli_choice, menu);
            /*SPRD:fix bug 517044,Multi language, interface display is not full@{*/
            changeMenuString();
            /*SPRD:fix bug 517044,Multi language, interface display is not full@}*/
            mActionMode.setTitle("0");
            mLayoutFooter.setVisibility(View.VISIBLE);
            mAdapter.setCheckboxHidden(false);
            mAdapter.notifyDataSetChanged();
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            buttonClickableChanges();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            //mLayoutFooter.setVisibility(View.GONE);
            clearContainer();
            if (mAdapter != null) {
                mAdapter.setCheckboxHidden(true);
                mAdapter.notifyDataSetChanged();
            }
            isFirstTime = true;
            mLayoutFooter.setVisibility(View.GONE);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.item_deselect_all) {
                unSelectAll();
            } else if (item.getItemId() == R.id.item_select_all) {
                selectAll();
            }
            buttonClickableChanges();
            return true;
        }

    };

    /* @} */
    private void buttonClickableChanges(){
        int checkitemSize = checkItem.size();
        if(mActionMode != null){
            mActionMode.setTitle(String.valueOf(checkitemSize));
        }
        if(menu == null){
            return;
        }
        if(checkitemSize == mList.size() || mList.size() == 0){
            menu.findItem(R.id.item_select_all).setEnabled(false);
        }else{
            menu.findItem(R.id.item_select_all).setEnabled(true);
        }
        if(checkitemSize <= 0){
            menu.findItem(R.id.item_deselect_all).setEnabled(false);
        }else{
            menu.findItem(R.id.item_deselect_all).setEnabled(true);
        }
       Button buttonRename = (Button) findViewById(R.id.textview_file_rename);
       buttonRename.setOnClickListener(this);
       Button buttonFilepath = (Button) findViewById(R.id.textview_file_path);
       buttonFilepath.setOnClickListener(this);
       if (checkitemSize > 1 || checkitemSize <= 0) {
           buttonRename.setEnabled(false);
           buttonFilepath.setEnabled(false);
        } else {
            buttonRename.setEnabled(true);
            buttonFilepath.setEnabled(true);
        }
        Button buttonDelete = (Button) findViewById(R.id.textview_file_delete);
        buttonDelete.setOnClickListener(this);
        if (checkitemSize <= 0) {
            buttonDelete.setEnabled(false);
        } else {
            buttonDelete.setEnabled(true);
        }
    }
    /* @} */
    /**
     * update the DB
     * @param title
     * @param id
     */
    private int updateRecordingFileDB(String newPath, String title, String displayName, RecorderItem item) {
        Uri uri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;

        if (StorageInfos.isExternalStorageMounted() || !StorageInfos.isInternalStorageSupported()) {
            uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, displayName);
        values.put(MediaStore.Audio.Media.TITLE, title);
        values.put(MediaStore.Audio.Media.DATA, newPath);
        /* SPRD: fix bug 523045 @{ */
        int result = 0;
        try {
            result = getContentResolver().update(uri, values, "_ID=?", new String[] {
                    String.valueOf(item.getId())
                });
        } catch (Exception e) {
            e.printStackTrace();
        }
        /* @} */
        return result;
    }

    /**
     * refresh Recording file list
     */
    private void reNewRecordingFileList(String data, String displayName, RecorderItem item) {
        for(int i = 0 ; i < mList.size(); i++){
            RecorderItem recordItem = mList.get(i);
            if(recordItem.data.equals(item.data)){
                recordItem.setData(data);
                recordItem.setDisplayName(displayName);
                mList.set(i , recordItem);
                break;
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    /**
     * rename the file
     *
     * @param data
     * @param title
     */
    private String renameRecordingFile(String data, String extenison , String title) {
        File renameFile = new File(data);
        String newPath = renameFile.getParent() + File.separator + title + extenison;
        File file = new File(newPath);
        if (file.exists() && !file.isDirectory()) {
            return DUPLICATE_NAME;
        }
        boolean result = false;
        try{
            result = renameFile.renameTo(new File(newPath));
        }catch(Exception e){
            Log.w(TAG , "when rename recording file,renameTo() error");
        }
        if(result == true){
            return newPath;
        }else{
            return null;
        }
    }

    private void renameDialog(final RecorderItem item) {
        final EditText editText = new EditText(this);
        final String displayName = item.getDisplayName();
        final String extension = displayName.substring(displayName.lastIndexOf("."), displayName.length());
        final String diaplayNameExceptExtension = displayName.substring(0 , displayName.lastIndexOf("."));
        editText.setText(diaplayNameExceptExtension);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(INPUT_MAX_LENGTH)});
        editText.addTextChangedListener(this);
        editText.requestFocus();
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.rename).setView(editText)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String fileName = editText.getEditableText().toString().trim();
                        String specialChar = stringFilter(fileName);
                        if(!specialChar.isEmpty()){
                            Toast.makeText(getApplicationContext(), String.format(getResources().
                                    getString(R.string.special_char_exist), specialChar),
                                    Toast.LENGTH_LONG).show();
                        }else if (fileName.equals(diaplayNameExceptExtension)) {
                            Toast.makeText(getApplicationContext(), R.string.filename_is_not_modified,
                                    Toast.LENGTH_SHORT).show();
                        } else if(TextUtils.isEmpty(fileName)){
                            Toast.makeText(getApplicationContext(), R.string.filename_empty_error,
                                    Toast.LENGTH_SHORT).show();
                        }else {
                            String newPath = renameRecordingFile(item.data, extension , fileName);
                            if (newPath == null) {
                                Toast.makeText(getApplicationContext() , R.string.rename_nosave,
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }else if (newPath.equals(DUPLICATE_NAME)) {
                                Toast.makeText(getApplicationContext() , R.string.duplicate_name,
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            String newDisplayName = fileName + extension;
                            int result = updateRecordingFileDB(newPath , fileName, newDisplayName, item);
                            if (result > 0) {
                                reNewRecordingFileList(newPath, newDisplayName, item);
                                Toast.makeText(getApplicationContext(), R.string.rename_save,
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                renameRecordingFile(newPath, extension , diaplayNameExceptExtension);
                                Toast.makeText(getApplicationContext(), R.string.rename_nosave,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                        dialog.dismiss();
                    }

                }).setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create().show();
    }
//    public static void keepDialog(DialogInterface dialog, boolean isClose) {
//        try {
//            Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
//            field.setAccessible(true);
//            field.set(dialog, isClose);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
    /* @} */
    private String stringFilter(String str){
        String filter = "[/\\\\<>:*?|\"\n\t]";
        Pattern pattern = Pattern.compile(filter);
        Matcher matcher = pattern.matcher(str);
        StringBuffer buffer= new StringBuffer();
        boolean result = matcher.find();
        while(result){
            buffer.append(matcher.group());
            result = matcher.find();
        }
        return buffer.toString();
    }
    private void checkboxOnclick(int pos){
        Boolean result = checkboxes.get(pos);
        if(result == null || result == false){
            checkboxes.put(pos, true);
            RecorderItem item = mAdapter.findItem(pos);
            checkItem.put(pos,item);
        }else{
            checkboxes.put(pos, false);
            checkItem.remove(pos);
        }
        buttonClickableChanges();
    }

    private void invalidateCheckbox(CheckBox box,int pos){
        Boolean result = checkboxes.get(pos);
        if(result == null || result == false){
            box.setChecked(false);
        }else{
            RecorderItem item = mAdapter.findItem(pos);
            checkItem.put(pos, item);
            box.setChecked(true);
        }
    }
    /* SPRD: delete for fix bug 256621 @{ */
    // private static final int SELECT_ALL = 0;
    // private static final int DELETE = 1;
    // private static final int UN_SELECT_ALL = 2;
    //
    // public boolean onCreateOptionsMenu(Menu menu) {
    // menu.add(0, SELECT_ALL, 0,
    // getString(R.string.menu_recording_list_select_all));
    // menu.add(0, DELETE, 1, getString(R.string.menu_recording_list_delete));
    // menu.add(0, UN_SELECT_ALL, 2,
    // getString(R.string.menu_recording_list_deselect_all));
    // return true;
    // }
    //
    // //add for bug 143717 20130401 begin
    // @Override
    // public boolean onPrepareOptionsMenu(Menu menu) {
    // // TODO Auto-generated method stub
    // //add for bug 146628
    // if(mAdapter == null){
    // return true;
    // }
    // //add for bug 146628
    // int itemCount = mAdapter.getCount();
    // int checkedCount = 0;
    // for(int i = 0; i < itemCount; i++){
    // if(checkboxes.get(i) != null && checkboxes.get(i) == true){
    // checkedCount++;
    // }
    // }
    // if(itemCount == 0){
    // menu.findItem(SELECT_ALL).setEnabled(false);
    // menu.findItem(DELETE).setEnabled(false);
    // menu.findItem(UN_SELECT_ALL).setEnabled(false);
    // }else if(checkedCount == itemCount){
    // menu.findItem(SELECT_ALL).setEnabled(false);
    // menu.findItem(DELETE).setEnabled(true);
    // menu.findItem(UN_SELECT_ALL).setEnabled(true);
    // }else if(checkedCount == 0){
    // menu.findItem(SELECT_ALL).setEnabled(true);
    // menu.findItem(DELETE).setEnabled(false);
    // menu.findItem(UN_SELECT_ALL).setEnabled(false);
    // }else{
    // menu.findItem(SELECT_ALL).setEnabled(true);
    // menu.findItem(DELETE).setEnabled(true);
    // menu.findItem(UN_SELECT_ALL).setEnabled(true);
    // }
    // return true;
    // }

    // add for bug 143717 20130401 end
    /* @} */

    /* SPRD: delete for fix bug 256621 @{ */
    // public boolean onOptionsItemSelected(MenuItem item) {
    // switch (item.getItemId()) {
    // case SELECT_ALL:
    // selectAll();
    // break;
    // case DELETE:
    // if(checkItem!=null && checkItem.size() == 0){
    // //add by linying bug117293 begin
    // (Toast.makeText(this,
    // this.getResources().getString(R.string.choose_file),
    // Toast.LENGTH_SHORT)).show();
    // //add by linying bug117293 end
    // break;
    // }
    // showDelDialog();
    // break;
    // case UN_SELECT_ALL:
    // unSelectAll();
    // break;
    // }
    // return true;
    // }
    /* @} */
    private void unSelectAll() {
        clearContainer();
        mListView.invalidateViews();
    }

    private void selectAll(){
        if(items != null){
            items.clear();
        }
        if(checkItem != null){
            checkItem.clear();
        }
        //getAllDatas();
        Integer index = 0;
        for(RecorderItem item:mList){
            checkboxes.put(index,true);
            checkItem.put(index,item);
            index++;
        }
        mListView.invalidateViews();
    }

    private void clearContainer() {
        if (items != null) {
            items.clear();
        }
        if (checkboxes != null) {
            checkboxes.clear();
        }
        if (checkItem != null) {
            checkItem.clear();
        }
    }

    private void deleteFileAysnc() {
        /*SPRD fix bug 543825&386249,FATAL EXCEPTION occurs when delete files @{*/
        AsyncTask<Void, Long, Void> task = new AsyncTask<Void, Long, Void>() {
            ProgressDialog pd = null;
            private Map<Integer,RecorderItem> deleteItem = new TreeMap<Integer,RecorderItem>(checkItem);

            @Override
            protected void onPreExecute() {
                pd = new ProgressDialog(RecordingFileList.this);
                pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pd.setIcon(android.R.drawable.ic_delete);
                pd.setTitle(R.string.recording_file_delete_alert_title);
                pd.setCancelable(false);
                pd.setMax(deleteItem.size());
                pd.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                long i = 0;
                for (Map.Entry<Integer, RecorderItem> entry : deleteItem.entrySet()) {
                    RecorderItem item = entry.getValue();
                    if(item != null){
                        deleteFile(item);
                        publishProgress(++i, item.id);
                    }
                }
                Log.i(TAG,"delete finished.");
                return null;
            }

            @Override
            protected void onProgressUpdate(Long... values) {
                pd.setProgress(values[0].intValue());
                if(mAdapter != null){
                    mAdapter.deleteById(values[1]);
                }
            }

            @Override
            protected void onPostExecute(Void result) {
                if(!isFinishOrDestroy()){
                    pd.cancel();
                }
                 deleteFinish();
                 dismissDelDialog();
            }
        };
        task.execute((Void[])null);
    }

    private boolean isFinishOrDestroy(){
        if(RecordingFileList.this.isDestroyed() || RecordingFileList.this.isFinishing()){
            return true;
        }
        return false;
    }
    /*SPRD fix bug 543825&386249,FATAL EXCEPTION occurs when delete files @}*/

    private void deleteFinish(){
//        if(mActionMode != null){
//            mActionMode.finish();
//        }
        clearContainer();
        Log.i(TAG,"deleteFinish and notify mAdapter");
        mListView.setVisibility(View.GONE);
        mAdapter.notifyDataSetChanged();
        mListView.setVisibility(View.VISIBLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        //modify by linying bug115502 begin
        detectionList();
        //modify by linying bug115502 end
        if(mActionMode != null){
            mActionMode.finish();
        }
        Toast.makeText(RecordingFileList.this,R.string.recording_file_delete_success, Toast.LENGTH_SHORT).show();
    }

    private String [] cloum = new String[]{"_id,_data"};

    private void getAllDatas() {
        ContentResolver cr = getContentResolver();
        StringBuffer buff = new StringBuffer();
        buff.append(MediaStore.Audio.Media.COMPOSER).append("='").append(SoundRecorder.COMPOSER)
        .append("'");
            //delete by linying bug112767 begin
            //        " and ")
            //.append(MediaStore.Audio.Media.DISPLAY_NAME)
            //.append(" like 'recording%' or ")
            //.append(MediaStore.Audio.Media.DISPLAY_NAME)
            //.append(" like '.recording%'");
            //delete by linying bug112767 end

        String database = "internal";
        if (StorageInfos.isExternalStorageMounted() || !StorageInfos.isInternalStorageSupported()) {
            database = "external";
        }
        Cursor cursor = cr.query(MediaStore.Audio.Media.getContentUri(database), cloum, buff.toString(),null, null);
        try {
            while (cursor != null && cursor.moveToNext()) {
                items.add(new RecorderItem(cursor.getLong(0),cursor.getString(1),null));
            }
        } catch (Exception e) {
            Log.e(TAG, "getAllDatas e=" + e.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private AlertDialog mDelDlg = null;

    private void showDelDialog() {
        if (mDelDlg == null) {
            mDelDlg = new AlertDialog.Builder(this)
                    .setTitle(getString(android.R.string.dialog_alert_title))
                    //.setIcon(android.R.drawable.ic_dialog_alert)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(getString(R.string.confirm_del))
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    deleteFileAysnc();
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dismissDelDialog();
                                }
                            }).create();
        //modify by bug 145574 begin
        }
        mDelDlg.show();
        //modify by bug 145574 end
    }

    private void dismissDelDialog() {
        if (mDelDlg != null) {
            mDelDlg.dismiss();
            mDelDlg.cancel();
            mDelDlg = null;
        }
    }

    private void deleteFile(RecorderItem item) {
        if (StorageInfos.isExternalStorageMounted() || !StorageInfos.isInternalStorageSupported()) {
            getContentResolver().delete(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, item.id), null,null);
        } else {
            getContentResolver().delete(ContentUris.withAppendedId(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, item.id), null,null);
        }
        File del = new File(item.data);
        if (!del.exists() || !del.delete()) {
            return;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapter, View v, int pos, long id) {
        if (mActionMode==null) {
            RecorderItem item = mAdapter.findItem(pos);
            if (item != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                if (StorageInfos.isExternalStorageMounted() || !StorageInfos.isInternalStorageSupported()) {
                    intent.setDataAndType(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),item.mimeType);
                } else {
                    intent.setDataAndType(ContentUris.withAppendedId(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, id),item.mimeType);
                }
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(RecordingFileList.this,R.string.play_error, 1000).show();
                }
            }
        }else{
            CheckBox checkBox=(CheckBox)v.findViewById(R.id.recode_checkbox);
            if (null==checkBox) {
                return;
            }
            if (true==checkBox.isChecked()) {
                checkBox.setChecked(false);
            }else{
                checkBox.setChecked(true);
            }
            checkboxOnclick(pos);
        }
    }

    private class RecorderItemClick
                implements DialogInterface.OnClickListener {

        private static final int LONG_CLICK  = 1;
        private static final int SHORT_CLICK = 2;

        private final int event;
        private final RecorderItem item;

        private RecorderItemClick(int event, RecorderItem item) {
            this.item = item;
            this.event = event;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (SHORT_CLICK == -which) {
                dialog.dismiss();
                return;
            }

            int row = -1;
            StringBuffer buff = new StringBuffer();
            buff.append(MediaStore.Audio.Media._ID).append("=").append(item.id);
            int toast_msg = -1;
            try {
                // delete database row
                if (StorageInfos.isExternalStorageMounted() || !StorageInfos.isInternalStorageSupported()) {
                    row = RecordingFileList.this.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, buff.toString(), null);
                } else {
                    row = RecordingFileList.this.getContentResolver().delete(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, buff.toString(), null);
                }
                // validate database process
                if (row == -1) {
                    toast_msg = R.string.recording_file_database_failed;
                    return;
                }
                // validate file process
                File del = new File(item.data);
                if (!del.exists() || !del.delete()) {
                    toast_msg = R.string.recording_file_delete_failed;
                    return;
                }

                toast_msg = R.string.recording_file_delete_success;
            } catch (Exception e) {
                if (row == -1) toast_msg = R.string.recording_file_database_failed;
                Log.d(TAG, "execute delete recorder item failed; E: " + (e != null ? e.getMessage() : "NULL"));
            } finally {
                mAdapter.deleteById((row != -1 ? item.id : 0));
                Log.d(TAG, "mAdapter deleteItem row:" + row);
                clearContainer();//add 2013/08/09
                Toast.makeText(RecordingFileList.this, toast_msg, Toast.LENGTH_SHORT).show();
                //modify by linying bug115502 begin
                detectionList();
                //modify by linying bug115502 end
            }
        }

        void show() {
            if (event == 0 || item == null)
                throw new RuntimeException("RecorderItemClick failed; event == " + event + " --- item == " + item);

            new AlertDialog.Builder(RecordingFileList.this)
                .setTitle(getString(android.R.string.dialog_alert_title))
                //.setIcon(android.R.drawable.ic_dialog_alert)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(item.getAlertMessage())
                .setPositiveButton(R.string.button_delete, this)
                .setNegativeButton(R.string.button_cancel, this)
                .show();
        }
    }
    private class CursorRecorderAdapter extends BaseAdapter {
        private List<RecorderItem> mData = new ArrayList<RecorderItem>();
        private boolean hiddenFlag = true;
        CursorRecorderAdapter(List<RecorderItem> data , boolean hiddenFlag) {
            super();
            this.mData.clear();
            this.mData.addAll(data);
            this.hiddenFlag = hiddenFlag;
        }

        @Override
        public int getCount() {
            synchronized (mData) {
                return mData == null ? 0 : mData.size();
            }
        }

        @Override
        public Object getItem(int pos) {
            return mData.get(pos);
        }

        @Override
        public long getItemId(int pos) {
            long result = -1L;
            RecorderItem item = findItem(pos);
            if (item != null) result = item.id;
            return result;
        }

        @Override
        public View getView(int pos, View cvt, ViewGroup pat) {
            if (cvt == null) {
                LayoutInflater flater =
                    (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                cvt = flater.inflate(R.layout.recording_file_item, null);
                if (cvt == null)
                    throw new RuntimeException("inflater \"record_item.xml\" failed; pos == " + pos);
            }

            RecorderItem item = findItem(pos);
            if (item == null) throw new RuntimeException("findItem() failed; pos == " + pos);

            TextView tv = null;
            // get "record_title"
            // tv = (TextView) cvt.findViewById(R.id.record_title);
            // tv.setText(item.title);
            // get "record_displayname"
            tv = (TextView) cvt.findViewById(R.id.record_displayname);
            //modify by linying bug141776 begin
            tv.setText(item.display_name);
            //modify by linying bug141776 end
            // get "record_duration"
            tv = (TextView) cvt.findViewById(R.id.record_duration);
            tv.setText(Utils.makeTimeString4MillSec(RecordingFileList.this, item.duration));
            // get "record_size"
            tv = (TextView) cvt.findViewById(R.id.record_size);
            tv.setText(item.getSize());
            // get "record_time"
//            tv = (TextView) cvt.findViewById(R.id.record_time);
//            tv.setText(item.getTime());
            CheckBox cb = (CheckBox)cvt.findViewById(R.id.recode_checkbox);
            cb.setTag(pos);
            invalidateCheckbox(cb,pos);
            if(mActionMode != null){
                cb.setVisibility(View.VISIBLE);
                //invalidateCheckbox(cb,pos);
            }else{
                cb.setVisibility(View.GONE);
            }
            return cvt;
        }
        public void setCheckboxHidden(boolean flag){
            hiddenFlag = flag;
        }
        private RecorderItem findItem(int pos) {
            RecorderItem result = null;
            Object obj = getItem(pos);
            if (obj != null && obj instanceof RecorderItem) {
                result = (RecorderItem) obj;
            }
            return result;
        }

        /*SPRD fix bug 543825&386249,FATAL EXCEPTION occurs when delete files @{*/
        private void deleteById(long id){
            boolean result = false;
            List<RecorderItem> tmp = new ArrayList<RecorderItem>();
            tmp.addAll(mData);
            Iterator<RecorderItem> it = tmp.iterator();
            while (it.hasNext()) {
                RecorderItem del = it.next();
                if (id == del.id) {
                    it.remove();
                    result = true;
                    break;
                }
            }
            if (result) {
                synchronized (mData) {
                    mData.clear();
                    mData.addAll(tmp);
                    notifyDataSetChanged();
                }
            }
        }
        /*SPRD fix bug 543825&386249,FATAL EXCEPTION occurs when delete files @}*/
    }

    private ArrayList<RecorderItem> query() {
        final int INIT_SIZE = 10;
        ArrayList<RecorderItem> result =
            new ArrayList<RecorderItem>(INIT_SIZE);
        Cursor cur = null;
        try {
            StringBuilder where = new StringBuilder();
            /* SPRD: fix bug 513105 @{ */
            SharedPreferences systemVersionShare = this.getSharedPreferences(SOUNDREOCRD_TYPE_AND_DTA, MODE_PRIVATE);
            String systemVersion = systemVersionShare.getString(RecordingFileList.SYSTEM_VERSION, "0");
            Log.d(TAG, "query(): systemVersion="+systemVersion+", currentVersion="+android.os.Build.VERSION.RELEASE);
            if (systemVersion.equals("0")) {
                /* SPRD: fix bug 521597 @{ */
                File pathDir = null;
                String defaultExternalPath = "0";
                String defaultInternalPath = "0";
                if(StorageInfos.isExternalStorageMounted()) {
                    pathDir = StorageInfos.getExternalStorageDirectory();
                    if (pathDir != null)
                        defaultExternalPath = pathDir.getPath() + Recorder.DEFAULT_STORE_SUBDIR;
                }
                pathDir = StorageInfos.getInternalStorageDirectory();
                if (pathDir != null)
                    defaultInternalPath = pathDir.getPath() + Recorder.DEFAULT_STORE_SUBDIR;

                Log.d(TAG, "query(): defaultExternalPath="+defaultExternalPath+", defaultInternalPath="+defaultInternalPath);

                where.append("(")
                .append(MediaStore.Audio.Media.MIME_TYPE)
                .append("='")
                .append(SoundRecorder.AUDIO_AMR)
                .append("' or ")
                .append(MediaStore.Audio.Media.MIME_TYPE)
                .append("='")
                .append(SoundRecorder.AUDIO_3GPP)
                .append("' or ")
                .append(MediaStore.Audio.Media.MIME_TYPE)
                .append("='")
                .append(SoundRecorder.AUDIO_MP4)
                .append("') and (")
                .append(MediaStore.Audio.Media.DATA)
                .append(" like '")
                .append(defaultExternalPath)
                .append("%' or ")
                .append(MediaStore.Audio.Media.DATA)
                .append(" like '")
                .append(defaultInternalPath)
                .append("%')");
                /* @} */
            } else {
                where.append(MediaStore.Audio.Media.COMPOSER)
                .append("='")
                .append(SoundRecorder.COMPOSER)
                .append("'");
            }
            /* @} */

            cur = RecordingFileList.this.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[] {
                        RecorderItem._ID,
                        RecorderItem._DATA,
                        RecorderItem.SIZE,
                        RecorderItem.TITLE,
                        RecorderItem.DISPLAY_NAME,
                        RecorderItem.MOD_DATE,
                        RecorderItem.MIME_TYPE,
                        RecorderItem.DU_STRING},
                        where.toString(), null, null);

            // read cursor
            int index = -1;
            for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
                index = cur.getColumnIndex(RecorderItem._ID);
                // create recorder object
                long id = cur.getLong(index);
                RecorderItem item = new RecorderItem(id);
                // set "data" value
                index = cur.getColumnIndex(RecorderItem._DATA);
                item.data = cur.getString(index);
                // set "size" value
                index = cur.getColumnIndex(RecorderItem.SIZE);
                item.size = cur.getLong(index);
                // set "title" value
                index = cur.getColumnIndex(RecorderItem.TITLE);
                item.title = cur.getString(index);
                // SET "display name" value
                index = cur.getColumnIndex(RecorderItem.DISPLAY_NAME);
                item.display_name = cur.getString(index);
                // set "time" value
                index = cur.getColumnIndex(RecorderItem.MOD_DATE);
                item.time = cur.getLong(index);
                // set "mime-type" value
                index = cur.getColumnIndex(RecorderItem.MIME_TYPE);
                item.mimeType = cur.getString(index);
                // add to mData
                index = cur.getColumnIndex(RecorderItem.DU_STRING);
                item.duration = cur.getInt(index);
                Log.w("mytest", "duration:"+item.duration);
                /* SPRD: fix bug 513105 @{ */
                if (!item.data.endsWith(".3gpp") && item.mimeType.equals(SoundRecorder.AUDIO_MP4))
                    continue;
                result.add(item);
                if (systemVersion.equals("0")) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.Audio.Media.COMPOSER, SoundRecorder.COMPOSER);
                    ContentResolver resolver = getContentResolver();
                    Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cur.getInt(0));
                    Log.d(TAG, "query(): update COMPOSER to MediaStore, id="+cur.getInt(0));
                    resolver.update(uri, cv, null, null);
                }
                /* @} */
            }
            /* SPRD: fix bug 513105 @{ */
            if (!systemVersion.equals(android.os.Build.VERSION.RELEASE)) {
                SharedPreferences.Editor edit = systemVersionShare.edit();
                edit.putString(RecordingFileList.SYSTEM_VERSION, android.os.Build.VERSION.RELEASE);
                edit.commit();
            }
            /* @} */
        } catch (Exception e) {
            Log.v(TAG, "RecordingFileList.CursorRecorderAdapter failed; E: " + e);
        } finally {
            if (cur != null) cur.close();
        }
        return result;
    }
    @SuppressWarnings("unused")
    private class RecorderItem {
        private final long id;
        private String data;
        private String mimeType;
        private long size;
        private String title;
        private String display_name;
        private long time;
        private int duration;

        private static final String _ID         = MediaStore.Audio.Media._ID;
        private static final String SIZE        = MediaStore.Audio.Media.SIZE;
        private static final String _DATA       = MediaStore.Audio.Media.DATA;
        private static final String TITLE       = MediaStore.Audio.Media.TITLE;
        private static final String DISPLAY_NAME= MediaStore.Audio.Media.DISPLAY_NAME;
        private static final String MOD_DATE    = MediaStore.Audio.Media.DATE_MODIFIED;
        private static final String MIME_TYPE   = MediaStore.Audio.Media.MIME_TYPE;
        private static final String DU_STRING   = MediaStore.Audio.Media.DURATION;

        private static final String AUDIO_AMR   = "audio/amr";
        private static final String AUDIO_3GPP  = "audio/3gpp";
        private static final double NUMBER_KB   = 1024D;
        private static final double NUMBER_MB   = NUMBER_KB * NUMBER_KB;

        RecorderItem(long id) {
            this.id = id;
        }

        RecorderItem(long id, String data, String mimeType) {
            this(id);
            this.data = data;
            this.mimeType = mimeType;
        }

        RecorderItem(long id, String data, String mimeType, long size, String title) {
            this(id, data, mimeType);
            this.size = size;
            this.title = title;
        }

        public String getSize() {
            StringBuffer buff = new StringBuffer();
            if (size > 0) {
                String format = null;
                double calculate = -1D;
                if (size < NUMBER_KB) {
                    format = getResources().getString(R.string.list_recorder_item_size_format_b);
                    int calculate_b = (int) size;
                    buff.append(String.format(format, calculate_b));
                } else if (size < NUMBER_MB) {
                    format = getResources().getString(R.string.list_recorder_item_size_format_kb);
                    calculate = (size / NUMBER_KB);
                    DecimalFormat df = new DecimalFormat(".##");
                    String st = df.format(calculate);
                    buff.append(String.format(format, st));
                } else {
                    format = getResources().getString(R.string.list_recorder_item_size_format_mb);
                    calculate = (size / NUMBER_MB);
                    DecimalFormat df = new DecimalFormat(".##");
                    String st = df.format(calculate);
                    buff.append(String.format(format, st));
                }
            }
            return buff.toString();
        }

        //modify by linying bug133704 begin
//        public String getTime() {
//            StringBuffer buff = new StringBuffer();
//            if (time > 0) {
//                String format =
//                    getResources().getString(R.string.list_recorder_item_time_format);
//                java.util.Date d = new java.util.Date(time * 1000);
//                java.text.DateFormat formatter_date =
//                    java.text.DateFormat.getDateInstance();
//                java.text.DateFormat formatter_time =
//                    java.text.DateFormat.getTimeInstance();
//                buff.append(
//                    String.format(format,
//                        new Object[] { formatter_date.format(d), formatter_time.format(d) }));
//            }
//            return buff.toString();
//        }
        /* SPRD: fix bug 250910 add rename feature@{ */
        /**
         * get the file's name
         * @return file's name
         */
        public String getDisplayName() {
            return display_name;
        }
        public void setDisplayName(String displayName) {
            this.display_name = displayName;
        }

        public void setData(String data) {
            this.data = data;
        }

        public String getData(){
            return data;
        }

        /**
         * get the file's id
         * @return file's id
         */
        public long getId() {
            return id;
        }
        /* @} */
        /**
         * get last file modify date
         *
         * @return last file modify date
         */
        public String getDate() {
            if (time > 0) {
                java.util.Date d = new java.util.Date(time * 1000);
                java.text.DateFormat formatter_date =
                    java.text.DateFormat.getDateInstance();
                return formatter_date.format(d);
            }
            return null;
        }

        /**
         * get last file modify time
         *
         * @return modify time
         */
        public String getTime() {
            if (time > 0) {
                java.util.Date d = new java.util.Date(time * 1000);
                java.text.DateFormat formatter_time =
                    java.text.DateFormat.getTimeInstance();
                return formatter_time.format(d);
            }
            return null;
        }
        //modify by linying bug133704 end

        public String getAlertMessage() {
            String msg =
                getResources().getString(R.string.recording_file_delete_alert_message);
            String result = String.format(msg, (display_name != null ? display_name : ""));
            return result;
        }

        @Override
        public String toString() {
            StringBuffer buff = new StringBuffer();
            buff.append("id == ").append(id)
                .append(" --- data == ").append(data)
                .append(" --- mimeType == ").append(mimeType)
                .append(" --- size == ").append(size)
                .append(" --- title == ").append(title)
                .append(" --- display_name == ").append(display_name)
                .append(" --- time == ").append(time)
                .append(" --- duration == ").append(duration);
            return buff.toString();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
       /* if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }*/
        return super.onKeyDown(keyCode, event);
    }

    //modify by linying bug115502 begin
    /**
     * detection list items count, if the count less than 1, show the remind textview
     */
    private void detectionList() {
        if (mAdapter.getCount() < 1) {
            remindTextView.setVisibility(View.VISIBLE);
        } else {
            remindTextView.setVisibility(View.GONE);
        }
    }
    //modify by linying bug115502 end
    /* SPRD: fix bug 257713 check the length when rename@{ */
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if(s.toString().length() >= INPUT_MAX_LENGTH){
            Toast.makeText(this, R.string.input_length_overstep, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }
    /* @} */
    public void fileDelete(View v) {
        if (checkItem != null && checkItem.size() == 0) {
            // add by linying bug117293 begin
            (Toast.makeText(this, this.getResources().getString(R.string.choose_file),
                    Toast.LENGTH_SHORT)).show();
            // add by linying bug117293 end
            return;
        }
        showDelDialog();
    }

    public void fileRename(View v) {
        if (checkItem.size() == 0 || checkItem.size() > 1) {
            return;
        }
        for (Map.Entry<Integer, RecorderItem> entry : checkItem.entrySet()) {
            renameDialog(entry.getValue());
        }
        mActionMode.finish();
    }

    public void filePath(View v) {
        if (checkItem.size() == 0 || checkItem.size() > 1) {
            return;
        }
        Resources res = getResources();
        for (Map.Entry<Integer, RecorderItem> entry : checkItem.entrySet()) {
            LayoutInflater flater =
                    (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View filepathView = flater.inflate(R.layout.recording_file_path, null);
            TextView tvName = (TextView)filepathView.findViewById(R.id.file_name_value);
            String fileName = entry.getValue().getDisplayName();
            tvName.setText(fileName);
            TextView tvPath = (TextView)filepathView.findViewById(R.id.file_path_value);
            String absolutePath =entry.getValue().getData();
            String filePath = absolutePath.substring(0,absolutePath.lastIndexOf('/'));
            tvPath.setText(filePath);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(filepathView).create();
            builder.show();
        }
        mActionMode.finish();
    }

    public void onClick(View button){
        if (!button.isEnabled())
            return;
        switch (button.getId()) {
            case R.id.textview_file_rename:
                fileRename(null);
                break;
            case R.id.textview_file_delete:
                fileDelete(null);
                break;
            case R.id.textview_file_path:
                filePath(null);
                break;
        }
    }
    /* SPRD: fix bug 516659 @{ */
    @Override
    public void onBackPressed() {
        if (isResumed()) {
            super.onBackPressed();
        }
    }
    /* @} */
}
