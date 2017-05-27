package com.android.messaging.smil.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.android.messaging.datamodel.data.ConversationMessageData;
import com.android.messaging.datamodel.data.DraftMessageData;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.smil.adapter.SmilAdapter;
import com.android.messaging.smil.data.MeasureSizeWorkerThread;
import com.android.messaging.smil.data.SmilDraftDataManager;
import com.android.messaging.smil.data.SmilPartEntity;
import com.android.messaging.smil.data.MeasureSizeWorkerThread.DrafTMessageDateImp;
import com.android.messaging.smil.view.SmilListview;
import com.android.messaging.smil.view.SmileditPar;
import com.android.messaging.smil.view.TextImage;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.ui.mediapicker.MediaPicker;
import com.android.messaging.ui.mediapicker.SmilMediaPicker;
import com.android.messaging.util.ContentType;
import com.android.messaging.util.GlobleUtil;
import com.android.messaging.util.OsUtil;
import com.android.messaging.util.PhoneUtils;
import com.android.messaging.util.UiUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.TextUtils.TruncateAt;
import android.text.style.SuperscriptSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.mms.pdu.PduPart;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;

import com.android.messaging.Factory;
import com.android.messaging.R;

import android.widget.LinearLayout.LayoutParams;
import android.telephony.SubscriptionInfo;

import com.android.messaging.smil.ui.SmilMainActivity.OnbackKeyListener;

public class SmilMainFragment extends Fragment implements OnClickListener,SmilMainActivity.OnbackKeyListener{

    private SmilAdapter mSmilAdapter;
    private FrameLayout mMediaPickFrameLayout;
    private FragmentManager mFragmentManager;
    private FragmentTransaction mFragmentTransaction;
    private SmilMediaPicker mSmilMediaPicker;
    private ArrayList<MessagePartData> mList;
    private ListView mSmilListview;
    private LinearLayout mButtonsLayout;

    private MessagePartData mPartData;
    private boolean isDraft = false;
    private MessagePartData mMessagePartData;
    private ConversationMessageData mConversationMessageData;
    private DraftMessageData mDraftMessageData;
    private DraftMessageData mSavingmDraftMessageData;

    public static final String FRAGMENT_TAG = "SmilMainFragment";
    public static final String TAG = "SmilMainFragment";

    public static final int URI_MESSAGE_FLAG = 20001;
    public static final int RELAYOUT_LISTVIEW = 20002;
    public static final int REFRESH_LISTVIEW = 20003;
    public static final int REMOVE_LISTVIE_ITEM = 20004;
    public static final int REPLACE_TEXTIMAGE = 20005;
    public static final int OUT_OF_SIZE = 20006;
    public static final int ADD_ATTCHMENT = 20007;
    public static final int ACTION_COMPARE = 20008;

    private static final int HAVE_NO_SIMS = 0;
    private static final int SUBJECT_SIZE = 40;
    private static final int MAX_SLIDE_NUMBER = 20;

    private boolean isSended = false;
    private boolean isAddattachment = false;
    private Activity mActivity;
    private AdapterContextMenuInfo selectedMenuInfo = null;
    private int mPosition = -1;
    private EditText mSubjectEdit;
    private String mMessageSubject;

    private MeasureSizeWorkerThread mMeasureSizeWorkerThread;
    public static Handler mWorkHandler;

    private List<SmilPartEntity> mPartEntities;
    private int mTextParPosition = -1; // flag : whether a SmilParEntity has a
                                       // textPar , -1 : no , >0 : yes

    private Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {
            Uri uri = null;
            switch (msg.what) {
            case OUT_OF_SIZE:
                Toast.makeText(Factory.get().getApplicationContext(),
                        R.string.smil_out_of_size, 300).show();
                break;
            case REMOVE_LISTVIE_ITEM:
                Log.i(TAG, "after mSmilAdapter.getAdapterList().size()--->"
                        + mSmilAdapter.getAdapterList().size());
                Log.i(TAG, "REMOVE_LISTVIE_ITEM----mPosition--->" + mPosition);
                removeSlideView(mPosition);
            case URI_MESSAGE_FLAG:

                break;
            case RELAYOUT_LISTVIEW:

                break;
            case REPLACE_TEXTIMAGE:
                Log.i(TAG, "after Postion-->" + mPosition + "----size---"
                        + mSmilAdapter.getAdapterList().size());
                uri = (Uri) msg.obj;
                TextImage textImagePart = (TextImage) GlobleUtil.mTemptSmileditPar
                        .getChildAt(GlobleUtil.mViewPosition);
                MessagePartData data = mSmilAdapter.getAdapterList()
                        .get(mPosition).get(GlobleUtil.mViewPosition);
                data.setContentType(null);
                data.setContentUri(uri);
                textImagePart.initUri(uri, data, getActivity());
                textImagePart.requestLayout();

                mWorkHandler
                        .sendEmptyMessage(MeasureSizeWorkerThread.ACTION_REFRESH_SIZE);
                break;
            case REFRESH_LISTVIEW:
                break;
            case ADD_ATTCHMENT:
                uri = (Uri) msg.obj;
                SmileditPar smileditPar = GlobleUtil.mTemptSmileditPar;
                if (smileditPar != null) {
                    MessagePartData  partData = new MessagePartData();
                    partData.setContentUri(uri);
              
                    smileditPar.addRectangelViewFromFragment(
                            GlobleUtil.mViewPosition + 1, partData);
                }
                break;
            case ACTION_COMPARE:
                mWorkHandler.sendEmptyMessage(MeasureSizeWorkerThread.ACTION_REFRESH_SIZE);
                break;
            }
        };
    };
    
    private void showKeyboard(){
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private  void closeBoard(Context mcontext) {
        InputMethodManager imm = (InputMethodManager) mcontext
          .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive())
         imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT,
           InputMethodManager.HIDE_NOT_ALWAYS);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (mSubjectEdit.hasFocus()) {
            mSubjectEdit.clearFocus();
        }
        Log.i(TAG, "MenuItem----item>" + item.getItemId());
        SmileditPar smileditPar = GlobleUtil.mTemptSmileditPar;
        MessagePartData partData = null;
        switch (item.getItemId()) {
        case R.id.smil_subject_edit:
            mSubjectEdit.setFocusableInTouchMode(true);
            mSubjectEdit.setFocusable(true);
            mSubjectEdit.requestFocus();
            showKeyboard();
            break;
        case R.id.smil_text_edit:
            mTextParPosition = checkTextContentTypePar(mPosition);
            if (mTextParPosition < 0) {
                partData = new MessagePartData();
                Uri uri = Uri.parse("content://"+System.currentTimeMillis()); 
                partData.setContentUri(uri);
                partData.setContentType("text/plain");
                smileditPar.addRectangelViewFromFragment(
                        GlobleUtil.mViewPosition, partData);
                UiUtils.showToastAtBottom(getActivity().getResources().getString(R.string.smil_add_text));
            }
            showDialogToEdit(smileditPar);
            
            break;
        case R.id.smil_repalce_slide:
            replaceSildeView(mPosition);
            break;
        case R.id.smil_remove_slide:
            removeSlideView(mPosition);
            break;
        case R.id.smil_insert_befor_slide:
            addSlideView(mPosition);
            break;
        case R.id.smil_insert_after_slide:
            addSlideView(mPosition + 1);
            break;
        case R.id.smil_add_attachment:
            isAddattachment = true;
            openSmilMediaPicker();
            break;
        case R.id.smil_remove_attachment:
            if (smileditPar != null) {
                smileditPar.removeRectangelView(getActivity(),
                        GlobleUtil.mViewPosition);
                mWorkHandler
                        .sendEmptyMessage(MeasureSizeWorkerThread.ACTION_REFRESH_SIZE);
            }
            break;
        case R.id.smil_repalce_attachment:
            isAddattachment = false;
            openSmilMediaPicker();
            break;
        }
        return super.onContextItemSelected(item);
    }

    private int checkTextContentTypePar(int postion) {
        int positionFlag = -1;
        if (mSmilAdapter != null) {
            SmilPartEntity entity = mSmilAdapter.getAdapterList().get(postion);
            MessagePartData data;
            for (int i = 0; i < entity.size(); i++) {
                data = entity.get(i);
                if(data.getContentType() != null && "text/plain".equals(data.getContentType())){ //Modify by SPRD for bug 561492 2016.05.19
                      positionFlag = i;
                      return positionFlag;
                }else if(data.getContentType() == null && data.getContentUri() == null && data.getText() == null){
                        positionFlag = i;
                        return positionFlag;
                }
            }
        }
        return positionFlag;
    }

    private int checkTextContentTypeParInSlide(int postion) {
        int positionFlag = -1;
        if (mSmilAdapter != null) {
            SmilPartEntity entity = mSmilAdapter.getAdapterList().get(postion);
            MessagePartData data;
            for (int i = 0; i < entity.size(); i++) {
                data = entity.get(i);
                if(data.getContentType() != null && "text/plain".equals(data.getContentType())){ //Modify by SPRD for bug 561492 2016.05.19
                      positionFlag = i;
                      return positionFlag;
                }else if(data.getContentType() == null && data.getContentUri() == null && data.getText() == null){
                    positionFlag = i;
                    return positionFlag;
                }
            }
        }
        return positionFlag;
    }


    public void setDatechanged(Uri uri) {
        Message message = mHandler.obtainMessage();
        message.obj = uri;
        if(!isAddattachment){
            message.what = REPLACE_TEXTIMAGE;
        }else{
            message.what = ADD_ATTCHMENT;
        }
        mHandler.sendMessage(message);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // add for bug 553998 begin
        if (!OsUtil.hasRequiredPermissions()) {
            return null;
        }
        // add for bug 553998 end
        GlobleUtil.isSmilAttament = true;
        final View view = inflater.inflate(R.layout.smail_main_fragment,
                container, false);
        mSubjectEdit = (EditText) view.findViewById(R.id.smil_mms_subject);
        mSmilListview = (ListView) view.findViewById(R.id.smil_main_listview);
        mMediaPickFrameLayout = (FrameLayout) view
                .findViewById(R.id.smail_mediapicker);
        mButtonsLayout = (LinearLayout) view.findViewById(R.id.layout_btns);

        mSmilListview.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                    int position, long id) {
                mPosition = position;
                return false;
            }
        });

        mFragmentManager = getActivity().getFragmentManager();
        mFragmentTransaction = mFragmentManager.beginTransaction();
        mSmilMediaPicker = new SmilMediaPicker(Factory.get().getApplicationContext());
        mSmilMediaPicker.setFragment(this);
        mFragmentTransaction.replace(R.id.smail_mediapicker, mSmilMediaPicker);
        mFragmentTransaction.commit();
        mSubjectEdit.setFilters(new InputFilter[] { new BytesLengthFilter(
                SUBJECT_SIZE) });
        mSubjectEdit.setText(mMessageSubject);
        mSubjectEdit.setVisibility(View.VISIBLE);
        mSubjectEdit.setFilterTouchesWhenObscured(false);
        mSubjectEdit.setFocusable(false);
        mSubjectEdit.clearFocus();

        if (isDraft) {
            if(!mMessageSubject.equals("")){
                mSubjectEdit.setHint(getActivity().getResources().getText(R.string.smil_subject_hint));
            }else{
                mSubjectEdit.setText(mMessageSubject);
            }
            mSubjectEdit.setText(mDraftMessageData.getMessageSubject());
            constructSendBtns();
            mWorkHandler = MeasureSizeWorkerThread.initMeasureSizeThread(
                    Factory.get().getApplicationContext(), mHandler);
            mMeasureSizeWorkerThread = MeasureSizeWorkerThread
                    .getMeasureSizeWorkerThread();
            mMeasureSizeWorkerThread
                    .setDrafTMessageDateImp(new DrafTMessageDateImp() {

                        @Override
                        public DraftMessageData getCurrentDraftMessageData() {                                
                            return getSmilCurrentDraftMessageData(mDraftMessageData);
                        }
                    });
            mSmilListview.setAdapter(mSmilAdapter);
        } else {
            mSubjectEdit.setText(mConversationMessageData.getMmsSubject());
            mSubjectEdit.setFilterTouchesWhenObscured(false);
            mSubjectEdit.setFocusable(false);
            mSubjectEdit.clearFocus();
            mSmilListview.setAdapter(mSmilAdapter);
        }
        return view;
    }

    private void openSmilMediaPicker() {
        if (mSmilMediaPicker != null) {
            mSmilMediaPicker.open(MediaPicker.MEDIA_TYPE_DEFAULT, true);
        }
    }

    private void addSlideView(int index) {
        Log.i(TAG,
                "addSlideView  before mSmilAdapter.getAdapterList().size()--->"
                        + "" + mSmilAdapter.getAdapterList().size());
        if (mSmilAdapter.getAdapterList().size() >= MAX_SLIDE_NUMBER) {
            return;
        }

        if (index < 0 || index > mSmilAdapter.getCount()) {
            Log.i(TAG, "addSlideView  failed  out of bounds");
            return;
        }
        MessagePartData newMessagePartData = new MessagePartData();
        Uri uri = Uri.parse("content://"+System.currentTimeMillis());
        newMessagePartData.setContentUri(uri);
        newMessagePartData.setContentType("text/plain");
        SmilPartEntity entity = new SmilPartEntity();
        entity.add(newMessagePartData);
 
        if (mSmilAdapter != null) {
            mSmilAdapter.getAdapterList().add(index, entity);
        }
        mSmilAdapter.notifyDataSetChanged();
    }

    private void removeSlideView(int index) {
        Log.i(TAG,
                "removeSlideView  before mSmilAdapter.getAdapterList().size()--->"
                        + mSmilAdapter.getAdapterList().size());
        if (index < 0 || index >= mSmilAdapter.getCount()) {
            Log.i("TAG", "removeSlideView  failed , out of bounds");
            return;
        }
        mSmilAdapter.getAdapterList().remove(index);
        if(mSmilAdapter.getAdapterList().size() == 0){
            addSlideView(0);
        }
        mSmilAdapter.notifyDataSetChanged();
    }

    private void replaceSildeView(int index) {

        if (index < 0 || index > mSmilAdapter.getCount()) {
            Log.i(TAG, "replaceSildeView  failed  out of bounds");
            return;
        }
        mSmilAdapter.getAdapterList().remove(index);
        addSlideView(index);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // add for bug 553998 begin
        if (!OsUtil.hasRequiredPermissions()) {
            return;
        }
        // add for bug 553998 end
        mActivity = getActivity();
        isSended = false;
        Intent intent = getActivity().getIntent();
        if (intent != null) {
            isDraft = intent.getBooleanExtra("isDraft", false);
            if (isDraft) {
                if(GlobleUtil.getEditedDraftMessageDate() != null ){
                    if(GlobleUtil.getEditedDraftMessageDate().getConversationId().equals
                       (GlobleUtil.getDraftMessageData().getConversationId())){
                        mDraftMessageData = GlobleUtil.getEditedDraftMessageDate();
                        if (mDraftMessageData!=null) {
                            mMessageSubject = mDraftMessageData.getMessageSubject();
                        }
                        mPartEntities = GlobleUtil.getEditedDraftMessageDateEntities();
                    }else{
                        mDraftMessageData = GlobleUtil.getDraftMessageData();
                        /*Modify by SPRD for bug 561492 2016.05.19 Start*/
                        if (mDraftMessageData ==null) {
                            mDraftMessageData = new DraftMessageData(GlobleUtil.getConId(mActivity));
                        }
                        /*Modify by SPRD for bug 561492 2016.05.19 End*/
                        mMessageSubject = mDraftMessageData.getMessageSubject();
                        mList = convertListToArrarylist(mDraftMessageData
                                .getReadOnlyAttachments());
                        mPartEntities = convertNormalMmsPartToSmilPart(mList);
                    }
                }else{
                    mDraftMessageData = GlobleUtil.getDraftMessageData();
                    /*Modify by SPRD for bug 561492 2016.05.19 Start*/
                    if(mDraftMessageData == null){
                        mDraftMessageData = new DraftMessageData(GlobleUtil.getConId(mActivity));
                    }
                    /*Modify by SPRD for bug 561492 2016.05.19 End*/
                    mMessageSubject = mDraftMessageData.getMessageSubject();
                    mList = convertListToArrarylist(mDraftMessageData
                            .getReadOnlyAttachments());
                    mPartEntities = convertNormalMmsPartToSmilPart(mList);
                }

            } else {
                mConversationMessageData = GlobleUtil.getConvMessageData();
                mMessageSubject = mConversationMessageData.getMmsSubject();
                mList = convertListToArrarylist(mConversationMessageData
                        .getParts());
                mPartEntities = convertNormalMmsPartToSmilPart(mList);
            }

            if (mPartEntities.size() == 0) {
                SmilPartEntity entity = new SmilPartEntity();
                MessagePartData data = new MessagePartData();
                data.setContentUri(Uri.parse("content://"+System.currentTimeMillis()));
                data.setContentType(ContentType.TEXT_PLAIN);
                entity.add(data);
                mPartEntities.add(entity);
            }
        }

        mSmilAdapter = new SmilAdapter(mPartEntities, Factory.get()
                .getApplicationContext(), mHandler, isDraft);
        mSmilAdapter.setActivity(mActivity);
    }

    private List<SmilPartEntity> convertNormalMmsPartToSmilPart(
            ArrayList<MessagePartData> list) {
        SmilPartEntity entity;
        List<SmilPartEntity> entities = new ArrayList<SmilPartEntity>();
        for (int i = 0; i < list.size(); i++) {
            MessagePartData partData = list.get(i);
            if(!ContentType.APP_SMIL.equals(partData.getContentType())){ //Modify by SPRD for bug 561492 2016.05.19
              entity = new SmilPartEntity();
              entity.add(list.get(i));
              entities.add(entity);
            }
        }
        return entities;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!GlobleUtil.isSmilAttament){
            GlobleUtil.isSmilAttament = true;
        }
        // add for bug 553998 begin
        if (!OsUtil.hasRequiredPermissions()) {
            return;
        }
        // add for bug 553998 end
    }

    private void changedListViewHeight(ListView listView, SmilAdapter adapter) {
        if (listView == null && adapter == null) {
            return;
        }
        int height = 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            View itemView = adapter.getView(i, null, listView);
            itemView.measure(0, 0);
            height += itemView.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = height
                + (listView.getDividerHeight() * (adapter.getCount() - 1));
        ((MarginLayoutParams) params).setMargins(10, 10, 10, 10);
        listView.setLayoutParams(params);
    }

    private ArrayList<MessagePartData> convertListToArrarylist(
            List<MessagePartData> datas) {
        ArrayList<MessagePartData> arrayList = new ArrayList<MessagePartData>(
                datas.size());
        for (MessagePartData partData : datas) {
            arrayList.add(partData);
        }
        return arrayList;
    }

    /****************************************************
     * ******************************************************
     ****************************************************/
    private void setItemView(View v) {
        mview = v;
    }

    private View getItemView() {
        return mview;
    }

    private View mview;

    private void clearEmptyAttachments(){
        if(mSmilAdapter == null) return;
        SmilPartEntity tempSmilPartEntity;
        List<SmilPartEntity> tempEntities = new ArrayList<SmilPartEntity>();
        int dataSize = mSmilAdapter.getAdapterList().size();
        for(int i = 0 ; i < dataSize ;  i++){
            Log.i(TAG, "mSmilAdapter.getAdapterList().size()---index--i->"+i);
            SmilPartEntity partEntity = mSmilAdapter.getAdapterList().get(i);
            Log.i(TAG, "partEntity.size()-"+partEntity.size());
            for(int j = 0 ; j < partEntity.size() ; j ++){
                MessagePartData data = partEntity.get(j);
                tempSmilPartEntity = new SmilPartEntity();
                if(data.getContentUri() == null){
                    tempSmilPartEntity.add(data);
                }else{
                    if(ContentType.TEXT_PLAIN.equals(data.getContentType())){ //Modify by SPRD for bug 561492 2016.05.19
                        if(data.getText() == null){
                            tempSmilPartEntity.add(data);
                        }else{
                            if("".equals(data.getText())){ //Modify by SPRD for bug 561492 2016.05.19
                                tempSmilPartEntity.add(data);
                            }
                        }
                    }
                }
                if(tempSmilPartEntity.size() != partEntity.size()){
                    Log.i(TAG, "tempSmilPartEntity.size() != partEntity.size()");
                    partEntity.removeAll(tempSmilPartEntity);
                }else{
                    Log.i(TAG, "tempSmilPartEntity.size() == partEntity.size()");
                    tempEntities.add(partEntity);
                }
           }
        }
        Log.i(TAG, "tempEntities.size()-->"+tempEntities.size());
        if(tempEntities.size() > 0){
             mSmilAdapter.getAdapterList().removeAll(tempEntities);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        /*Modify by SPRD for bug 561492 2016.05.19 Start*/
        if (isDraft) {
            if (!isSended) {
                if (mDraftMessageData != null) {
                    mSavingmDraftMessageData = mDraftMessageData;
                    mSavingmDraftMessageData.setMessageSubject(mSubjectEdit
                            .getText().toString());
                    List<SmilPartEntity> smileditPars = null;
                    if (mSmilAdapter != null) {
                        smileditPars = mSmilAdapter.getAdapterList();

                    }
                    mSavingmDraftMessageData.getAttachments().clear();
                    for (int i = 0; i < smileditPars.size(); i++) {
                        for (int j = 0; j < smileditPars.get(i).size(); j++) {
                            mSavingmDraftMessageData.getAttachments().add(
                                    smileditPars.get(i).get(j));
                        }
                    }
                    SmilDraftDataManager.saveSmilDraftData(Factory.get()
                            .getApplicationContext(), mSavingmDraftMessageData,
                            smileditPars);
                    GlobleUtil.setEditedDraftMessageDate(mSavingmDraftMessageData , mSmilAdapter.getAdapterList());
                }
            }
        }
        /*Modify by SPRD for bug 561492 2016.05.19 End*/
    }

    private DraftMessageData getSmilCurrentDraftMessageData(
            DraftMessageData draftMessageData) {
        /* Modify by SPRD for bug 562014 2016.05.19 */
        DraftMessageData currentdraftMessageData = new DraftMessageData(draftMessageData.getConversationId());
        List<SmilPartEntity> smileditPars = null;
        if (mSmilAdapter != null) {
            smileditPars = mSmilAdapter.getAdapterList();
        }
        currentdraftMessageData.getAttachments().clear();
        for (int i = 0; i < smileditPars.size(); i++) {
            for (int j = 0; j < smileditPars.get(i).size(); j++) {
                currentdraftMessageData.getAttachments().add(
                        smileditPars.get(i).get(j));
            }
        }
        return currentdraftMessageData;
    }

    /* *************************************************************************
     * Add for bug 537586 begin
     * **************************************************
     * ************************
     */
    private static class BytesLengthFilter implements InputFilter {
        private final int mMax;

        public BytesLengthFilter(int max) {
            mMax = max;
        }

        public CharSequence filter(CharSequence source, int start, int end,
                Spanned dest, int dstart, int dend) {
            try {
                int orgBytes = dest.toString().getBytes().length;
                int reBytes = dest.subSequence(dstart, dend).toString()
                        .getBytes().length;
                int srcBytes = source.subSequence(start, end).toString()
                        .getBytes().length;
                int bKeep = mMax - (orgBytes - reBytes);
                if (bKeep <= 0) {
                    return "";
                } else if (bKeep >= srcBytes) {
                    return null; // keep original
                } else {
                    int mid;
                    int midLenth;
                    int cKeep = 0;
                    int startIndex = start;
                    int endIndex = end;
                    while (startIndex <= endIndex) {
                        mid = startIndex + (endIndex - startIndex) / 2;
                        midLenth = source.subSequence(start, mid).toString()
                                .getBytes().length;
                        if (midLenth < bKeep) {
                            if (source.subSequence(start, mid + 1).toString()
                                    .getBytes().length > bKeep) {
                                cKeep = mid;
                                break;
                            } else {
                                startIndex = mid + 1;
                            }
                        } else if (midLenth > bKeep) {
                            if (source.subSequence(start, mid - 1).toString()
                                    .getBytes().length < bKeep) {
                                cKeep = mid - 1;
                                break;
                            } else {
                                endIndex = mid - 1;
                            }
                        } else {
                            cKeep = mid;
                            break;
                        }
                    }
                    if (cKeep == 0) {
                        return "";
                    }
                    cKeep += start;
                    if (Character.isHighSurrogate(source.charAt(cKeep - 1))) {
                        --cKeep;
                        if (cKeep == start) {
                            return "";
                        }
                    }
                    return source.subSequence(start, cKeep);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return "";
            }
        }
    }

    private void showDialogToEdit(final SmileditPar smileditPar){
        final AlertDialog.Builder alertDialog = new android.app.AlertDialog.Builder(getActivity());
        View alertDialogView = View.inflate(getActivity(), R.layout.smil_dialog_edit, null);
        final EditText editText = (EditText)alertDialogView.findViewById(R.id.dialog_smil_edit);
        Button confirm_bt = (Button)alertDialogView.findViewById(R.id.dialog_smil_confirm);
        Button cancel_bt = (Button)alertDialogView.findViewById(R.id.dialog_smil_cancel);
        editText.requestFocus();
        
        final TextImage image = (TextImage) smileditPar.getChildAt(GlobleUtil.mViewPosition);

        final AlertDialog showAlertDialog = alertDialog.create();

        confirm_bt.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                  if(showAlertDialog.isShowing()){
                      showAlertDialog.hide();
                  }
                  if(!"".equals(editText.getText().toString())){ //Modify by SPRD for bug 561492 2016.05.19
                      setTextImageTextCharacter(editText.getText().toString(),checkTextContentTypeParInSlide(mPosition));
                  }
                  closeBoard(Factory.get()
                          .getApplicationContext());
                  //add for bug 567717 begin
                  showAlertDialog.dismiss();
                  //add for bug 567717 end
            }
        });
        cancel_bt.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(showAlertDialog.isShowing()){
                    showAlertDialog.hide();
                }
                closeBoard(Factory.get()
                        .getApplicationContext());
                //add for bug 567717 begin
                showAlertDialog.dismiss();
                //add for bug 567717 end
            }
        });
        showAlertDialog.setView(alertDialogView, 0, 0, 0, 0);
        showAlertDialog.setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                  showKeyboard();
            }
        });

        showAlertDialog.show();
    }

    private void setTextImageTextCharacter(String content , int postion){
        Log.i("setTextImageTextCharacter", "postion---->"+postion);
        Log.i("setTextImageTextCharacter", "mPosition---->"+mPosition);
        if(mSmilAdapter != null){
             mSmilAdapter.getAdapterList().get(mPosition).get(postion).setText(content);
             mSmilAdapter.notifyDataSetChanged();
        }
    }

    private void constructSendBtns() {
        if (getSimsData().size() == 0) {
            Button btn = new Button(getActivity());
            btn.setId(HAVE_NO_SIMS);
            btn.setOnClickListener(this);
            btn.setBackground(null);
            btn.setText("No Sim");
            btn.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            mButtonsLayout.setGravity(Gravity.CENTER);
            mButtonsLayout.addView(btn);
        } else if (getSimsData().size() == 1) {
            Button btn = new Button(getActivity());
            btn.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            btn.setText(getSimsData().get(0).mDisplayName);
            btn.setTextColor(getSimsData().get(0).mTextTint);
            btn.setId(getSimsData().get(0).mId);
            btn.setOnClickListener(this);
            btn.setEllipsize(TruncateAt.END);
            mButtonsLayout.setGravity(Gravity.CENTER);
            mButtonsLayout.addView(btn);
        } else {
            mButtonsLayout.setOrientation(LinearLayout.HORIZONTAL);
            for (int i = 0; i < getSimsData().size(); i++) {
                Button btn = new Button(getActivity());
                btn.setText(getSimsData().get(i).mDisplayName);
                btn.setMaxLines(1);
                btn.setId(getSimsData().get(i).mId);
                btn.setTextColor(getSimsData().get(i).mTextTint);
                btn.setEllipsize(TruncateAt.END);
                btn.setOnClickListener(this);
                btn.setLayoutParams(new LinearLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1));
                mButtonsLayout.addView(btn);
            }
        }
    }

    class SimDatas {
        private int mId;
        private int mSimSlotIndex;
        private CharSequence mDisplayName;
        private int mTextTint;

        public SimDatas(int id, int simSlotIndex, CharSequence displayName,
                int iconTint) {
            this.mId = id;
            this.mSimSlotIndex = simSlotIndex;
            this.mDisplayName = displayName;
            this.mTextTint = iconTint;
        }
    }

    private ArrayList<SimDatas> getSimsData() {
        final ArrayList<SimDatas> simDataList = new ArrayList<SimDatas>();
        final List<SubscriptionInfo> InfoList = PhoneUtils.getDefault()
                .toLMr1().getActiveSubscriptionInfoList();
        if (InfoList != null && InfoList.size() != 0) {
            Iterator iterator = InfoList.iterator();
            while (iterator.hasNext()) {
                SubscriptionInfo subscriptionInfo = (SubscriptionInfo) iterator
                        .next();
                String simNameText = subscriptionInfo.getDisplayName()
                        .toString();
                String displayName = TextUtils.isEmpty(simNameText) ? SmilMainFragment.this
                        .getString(R.string.sim_slot_identifier,
                                subscriptionInfo.getSimSlotIndex() + 1)
                        : simNameText;
                simDataList.add(new SimDatas(subscriptionInfo
                        .getSubscriptionId(), subscriptionInfo
                        .getSimSlotIndex(), subscriptionInfo.getDisplayName(),
                        subscriptionInfo.getIconTint()));
            }
        }
        return simDataList;
    }

    @Override
    public void onClick(View v) {
        if(isDraft){
            if(getSimsData().size() == 0){
                return;
            }
            clearEmptyAttachments();
            if(!"".equals(mSubjectEdit.getText().toString())){ //Modify by SPRD for bug 561492 2016.05.19
                mDraftMessageData.setMessageSubject(mSubjectEdit.getText().toString());
            }
            if(mSmilAdapter.getAdapterList().size() == 0 && (mDraftMessageData.getMessageSubject() == null ||
                    "".equals(mDraftMessageData.getMessageSubject()))){ //Modify by SPRD for bug 561492 2016.05.19
                  addSlideView(0);
                  UiUtils.showToastAtBottom(getActivity().getResources().getString(R.string.smil_empty_content));
                  return;
            }
            SmilDraftDataManager.sendMessage(getActivity(),
                    getSmilCurrentDraftMessageData(mDraftMessageData),
                    mSmilAdapter.getAdapterList(), v.getId());
            GlobleUtil.setEditedDraftMessageDate(null,null);
            isSended = true;
            getActivity().finish();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        // TODO Auto-generated method stub
        super.onAttach(activity);
        ((SmilMainActivity)(activity)).setOnbackKeyListener(this);
    }

    @Override
    public void onBackPressedSmilMainActivity() {
        if (isDraft) {
            if (!isSended) {
                if (mDraftMessageData != null) {

                    clearEmptyAttachments();
                    if(!"".equals(mSubjectEdit.getText().toString())){ //Modify by SPRD for bug 561492 2016.05.19
                        mDraftMessageData.setMessageSubject(mSubjectEdit.getText().toString());
                    }else{
                        mDraftMessageData.setMessageSubject(null);
                    }
                    mSavingmDraftMessageData = mDraftMessageData;
                    List<SmilPartEntity> smileditPars = null;
                    if (mSmilAdapter != null) {
                        smileditPars = mSmilAdapter.getAdapterList();
                    }
                    mSavingmDraftMessageData.getAttachments().clear();
                    for (int i = 0; i < smileditPars.size(); i++) {
                        for (int j = 0; j < smileditPars.get(i).size(); j++) {
                            mSavingmDraftMessageData.getAttachments().add(
                                    smileditPars.get(i).get(j));
                        }
                    }
                    SmilDraftDataManager.saveSmilDraftData(Factory.get()
                            .getApplicationContext(), mSavingmDraftMessageData,
                            smileditPars);
                    if(mSavingmDraftMessageData.getAttachments().size() > 0){
                        GlobleUtil.setEditedDraftMessageDate(mSavingmDraftMessageData , mSmilAdapter.getAdapterList());
                    }else{
                        GlobleUtil.setEditedDraftMessageDate(null , null);
                    }
                    getActivity().finish();
                }
            }
        }else{
            getActivity().finish();
        }
    }
}
