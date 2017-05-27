package com.android.settings.sales;


import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.android.settings.R;

public class SalesTrackerTestActivity extends Activity implements OnClickListener {
	private TextView numText;
	private EditText numEdit;
	private Button sendbutton;
	private Button delbutton;
	private TextView explainText;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        numText=(TextView)findViewById(R.id.Textview);
        numText.setText(R.string.receive_num);
        
        numEdit=(EditText)findViewById(R.id.EditText);       
        numEdit.setText(SalesTrackerInfo.Number);
				
        sendbutton=(Button)findViewById(R.id.Edit_Button);
        sendbutton.setText(R.string.send);
        sendbutton.setOnClickListener(this);

        delbutton=(Button)findViewById(R.id.Del_Button);
        delbutton.setText(R.string.delete);
        delbutton.setOnClickListener(this);
		
        explainText=(TextView)findViewById(R.id.Textview2);
        explainText.setText(R.string.explain);
    }

	public void onClick(View v) {
        switch (v.getId()) {
        case R.id.Edit_Button:
		String string=numEdit.getText().toString();
		if(string.length()==0)
		{
			return;
		}
		Intent testintent = new Intent("com.android.sales.trakcertest");
		testintent.putExtra("test_num", string);
		testintent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		sendBroadcast(testintent);
		Toast.makeText(SalesTrackerTestActivity.this, R.string.toast, Toast.LENGTH_LONG).show();
		finish();            
		break;
        case R.id.Del_Button:
		Intent deleteintent = new Intent("com.android.sales.deletefile");
		deleteintent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		sendBroadcast(deleteintent);
            break;
        }
	}	

}
