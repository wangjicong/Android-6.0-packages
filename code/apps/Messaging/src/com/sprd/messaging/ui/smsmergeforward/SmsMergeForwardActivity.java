package com.sprd.messaging.ui.smsmergeforward;

import android.app.Activity;
import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.provider.Telephony.Sms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import com.android.messaging.R;
import com.android.messaging.datamodel.DatabaseHelper;
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns;
import com.android.messaging.util.Assert;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns;
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns;
import com.android.messaging.datamodel.DatabaseHelper.PartColumns;
import com.android.messaging.datamodel.data.MessageData;


public class SmsMergeForwardActivity extends ListActivity{
    private static final String TAG = "SmsMergeForwardActivity";
    private static final String URI_MERGE_FORWARD = "sms_merge_forward_uri";
    private static final String SMS_MESSAGE_PROJECTION = "sms_merge_forward_project";
    private static final String WHERE = "sms_merge_forward_condition";
    private static final String ORDERBY = "sms_merge_forward_orderby";
    
    private SmsMsgAdapter mSmsMsgAdapter;
    private SmsMsgListQueryHandler mSmsQueryHandler;
    private TextView mEmptyMsg;
    private Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            mSmsMsgAdapter.changeCursor((Cursor)msg.obj);
            Log.d(TAG,"=======sms merge forward=======count: "+mSmsMsgAdapter.getCount());
            if (mSmsMsgAdapter.getCount() == 0) {
                mEmptyMsg.setVisibility(View.VISIBLE);
                getListView().setEmptyView(mEmptyMsg);
                mEmptyMsg.setText(R.string.sms_merge_forward_empty);
            }
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sms_merge_forward_list);
        init();
    }

    private void init() {
        mEmptyMsg = (TextView) findViewById(R.id.empty_message);
        mSmsMsgAdapter = new SmsMsgAdapter(this, null, false, getListView());
        setListAdapter(mSmsMsgAdapter);

        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (view != null) {
                    mSmsMsgAdapter.startSelectMode();
                    mSmsMsgAdapter.updateCheckedState(view);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        String smsmergeforward_from =  getIntent().getStringExtra("SMS_MERGE_FORWARD_FROM");
        if("ConversationFragment".equalsIgnoreCase(smsmergeforward_from)){
            final String threadId = getIntent().getStringExtra("thread_id");
            Log.d(TAG,"=======sms merge forward========smsmergeforward_from: "+smsmergeforward_from+"     threadId: "+threadId);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final DatabaseWrapper db = DataModel.get().getDatabase();
                    Cursor cursor = null;
                    String sql = "SELECT "
                                + DatabaseHelper.CONVERSATIONS_TABLE+"."+ConversationColumns.NAME +","
                                + DatabaseHelper.MESSAGES_TABLE+"."+MessageColumns._ID +","
                                + DatabaseHelper.MESSAGES_TABLE+"."+MessageColumns.RECEIVED_TIMESTAMP +","                         
                                + DatabaseHelper.MESSAGES_TABLE+"."+MessageColumns.STATUS +","
                                + DatabaseHelper.PARTS_TABLE+"."+PartColumns.TEXT
                                + " FROM " + DatabaseHelper.CONVERSATIONS_TABLE
                                + " JOIN " + DatabaseHelper.MESSAGES_TABLE
                                + " ON " + DatabaseHelper.CONVERSATIONS_TABLE + "." + ConversationColumns._ID +"=" +  DatabaseHelper.MESSAGES_TABLE + "." + MessageColumns.CONVERSATION_ID
                                + " JOIN " +  DatabaseHelper.PARTS_TABLE
                                + " ON " + DatabaseHelper.MESSAGES_TABLE + "." + MessageColumns._ID +"=" +  DatabaseHelper.PARTS_TABLE + "." + PartColumns.MESSAGE_ID
                                + " WHERE "
                                + DatabaseHelper.CONVERSATIONS_TABLE + "." +ConversationColumns._ID + "=" + threadId 
                                + " AND " 
                                + DatabaseHelper.MESSAGES_TABLE+"."+MessageColumns.PROTOCOL +"=" + String.valueOf(MessageData.PROTOCOL_SMS)
                                + " AND "
                                + "(" + DatabaseHelper.MESSAGES_TABLE+"."+MessageColumns.STATUS +"=" + String.valueOf(MessageData.BUGLE_STATUS_OUTGOING_COMPLETE)
                                + " OR " + DatabaseHelper.MESSAGES_TABLE+"."+MessageColumns.STATUS +"=" + String.valueOf(MessageData.BUGLE_STATUS_OUTGOING_DELIVERED)
                                + " OR " + DatabaseHelper.MESSAGES_TABLE+"."+MessageColumns.STATUS +"=" + String.valueOf(MessageData.BUGLE_STATUS_INCOMING_COMPLETE)
                                + ")"
                                + " order by " + DatabaseHelper.MESSAGES_TABLE+"."+MessageColumns.RECEIVED_TIMESTAMP + " desc";
                  
                    cursor = db.rawQuery(sql,null);
                    Message.obtain(msgHandler, 0, cursor).sendToTarget();
                }
            }).start();
        }else{
            Log.d(TAG,"====sms merge forward======onStart=====UriString: "+getIntent().getStringExtra(URI_MERGE_FORWARD)+"  intent: "+getIntent().toString());
            Uri smsmergeforwdUri = Uri.parse(getIntent().getStringExtra(URI_MERGE_FORWARD));
            String[] smsmergeforwd_project = getIntent().getStringArrayExtra(SMS_MESSAGE_PROJECTION);
            String smsmergeforwd_where = getIntent().getStringExtra(WHERE);
            String smsmergeforwd_orderby = getIntent().getStringExtra(ORDERBY);
            
            mSmsQueryHandler = new SmsMsgListQueryHandler(getContentResolver());
            mSmsQueryHandler.startQuery(0, null, smsmergeforwdUri, smsmergeforwd_project,
                    smsmergeforwd_where, null, smsmergeforwd_orderby);
        }
    }

    private class SmsMsgListQueryHandler extends AsyncQueryHandler {
        public SmsMsgListQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            mSmsMsgAdapter.changeCursor(cursor);

            if (mSmsMsgAdapter.getCount() == 0) {
                mEmptyMsg.setVisibility(View.VISIBLE);
                getListView().setEmptyView(mEmptyMsg);
                mEmptyMsg.setText(R.string.sms_merge_forward_empty);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStop() {
        if(mSmsMsgAdapter != null) {
            mSmsMsgAdapter.changeCursor(null);
        }
        super.onStop();
    }

}
