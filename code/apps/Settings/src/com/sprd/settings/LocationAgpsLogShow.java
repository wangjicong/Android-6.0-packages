/** Created by Spreadst */

package com.sprd.settings;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;

import android.app.Activity;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.android.settings.R;

public class LocationAgpsLogShow extends Activity {
    private static final String LOG_FILE_NAME = "/data/agps.log";
    private static final String TAG = LocationAgpsLogShow.class.getSimpleName();
    private static final int MENU_DELETE = 0;
    private static final int MSG_DELETE = 0;
    private static final int MSG_LOG_CHANGE = 1;
    private TextView mTextView;
    RandomAccessFile mRaf;
    private StringBuilder mBuffer = new StringBuilder();

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.arg1) {
                case MSG_DELETE:
                    deleteLogContent();
                    mTextView.setText("");
                    break;
                case MSG_LOG_CHANGE:
                    getAgpsLog();
                    break;
                default:
                    Log.d(TAG, "handleMesage default");
                    break;
            }
        };
    };

    @Override
    public void onCreate(Bundle SavedInstanceState) {
        super.onCreate(SavedInstanceState);
        setContentView(R.xml.show_agps_log);
        mTextView = (TextView) findViewById(R.id.agps_log_show);
        mTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        try {
            mRaf = new RandomAccessFile(LOG_FILE_NAME, "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        mFileObserver.startWatching();
    }

    FileObserver mFileObserver = new FileObserver(LOG_FILE_NAME) {
        @Override
        public void onEvent(int event, String path) {
            Log.d(TAG, event + " <-event  ;  path -> " + path);
            switch (event) {
                case FileObserver.MODIFY:
                    Message msg = Message.obtain();
                    msg.arg1 = MSG_LOG_CHANGE;
                    mHandler.sendMessage(msg);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        Message msg = Message.obtain();
        msg.arg1 = MSG_LOG_CHANGE;
        mHandler.sendMessage(msg);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_DELETE, 0, R.string.delete);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        String text = mTextView.getText().toString();
        if (text.length() == 0) {
            menu.findItem(MENU_DELETE).setEnabled(false);
        } else {
            menu.findItem(MENU_DELETE).setEnabled(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_DELETE:
                Message msg = Message.obtain();
                msg.arg1 = MSG_DELETE;
                mHandler.sendMessage(msg);
                break;
            default:
                Log.d(TAG, "onOptionsItemSelected default ");
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteLogContent() {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(LOG_FILE_NAME);
            pw.print("\n");
            // clear the content of buffer
            mBuffer.delete(0, mBuffer.length());
            // in order to move pointer to the start line of file
            if (mRaf != null) {
                mRaf.seek(0);
            } else {
                Log.d(TAG, "deleteLogContent mRaf is NULL !");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFileObserver.stopWatching();
        if (mRaf != null) {
            try {
                mRaf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ;
    }

    private void getAgpsLog() {
        if (mRaf == null) {
            Log.d(TAG, "getAgpsLog mRaf is NULL !");
            return;
        }
        try {
            String content = "";
            while ((content = mRaf.readLine()) != null) {
                mBuffer.append(content + "\n");
            }
            Log.d(TAG, "buffer string = " + mBuffer.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mTextView.setText(mBuffer.toString());
    }

}
