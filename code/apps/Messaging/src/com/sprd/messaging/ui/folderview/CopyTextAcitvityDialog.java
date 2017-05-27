package com.sprd.messaging.ui.folderview;

import com.android.messaging.R;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class CopyTextAcitvityDialog extends Activity {
    private EditText mEditContent;
    private Button mBtnOk;
    private Button mBtnCancel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.copy_text_activity_dialog);

        mEditContent = (EditText) findViewById(R.id.copy_part_message);
        if(mEditContent != null){
            Intent intent = this.getIntent();
            if(intent != null && intent.getStringExtra("Text") != null){
                mEditContent.setText(intent.getStringExtra("Text"));
            }else{
                mEditContent.setText("");
            }
        }

        mBtnOk = (Button) findViewById(R.id.dialog_btn_ok);
        if (mBtnOk != null) {
            mBtnOk.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    ClipboardManager clipboard = (ClipboardManager) CopyTextAcitvityDialog.this
                            .getSystemService(Context.CLIPBOARD_SERVICE);
                    if (mEditContent != null && clipboard != null) {
                        clipboard.setPrimaryClip(ClipData.newPlainText(null,
                                mEditContent.getText().toString()));
                    }
                    CopyTextAcitvityDialog.this.finish();
                }
            });
        }

        mBtnCancel = (Button) findViewById(R.id.dialog_btn_cancel);
        if (mBtnCancel != null) {
            mBtnCancel.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    CopyTextAcitvityDialog.this.finish();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}
