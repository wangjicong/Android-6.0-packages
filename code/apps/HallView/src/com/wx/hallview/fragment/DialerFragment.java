package com.wx.hallview.fragment;

/**
 * Created by Administrator on 16-1-23.
 */
import android.view.View;
import android.widget.TextView;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.telecom.TelecomManager;
import android.telecom.PhoneAccountHandle;
import java.util.List;
import android.os.Parcelable;
import android.view.ViewGroup;
import android.content.res.Resources;
import android.widget.Button;
import android.view.LayoutInflater;
import com.wx.hallview.R;
import com.wx.hallview.views.CircleLayout;

public class DialerFragment extends BaseFragmentView implements View.OnClickListener, View.OnLongClickListener {
    private TextView mDigits;
    
    public DialerFragment(Context context) {
        super(context);
    }
    
    public boolean needShowBackButton() {
        return false;
    }
    
    public View onCreateView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.circle_dialer_layout, container, false);
        mDigits = (TextView)view.findViewById(R.id.digits);
        initButtonText(view);
        view.findViewById(R.id.call_button).setOnClickListener(new DialerFragment.DialButtonClickListener());
        return view;
    }
    
    
    private Intent createCallIntent(String number) {
        Uri uri = Uri.fromParts("tel", number, null);
        Intent intent = new Intent("android.intent.action.CALL_PRIVILEGED", uri);
        intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        TelecomManager telecomManager = (TelecomManager)getContext().getSystemService("telecom");
        PhoneAccountHandle account = telecomManager.getDefaultOutgoingPhoneAccount("tel");
        if(account == null) {
            List<PhoneAccountHandle> accounts = telecomManager.getCallCapablePhoneAccounts();
            if(accounts.size() >= 1) {
                account = accounts.get(0);
            }
        }
        if(account != null) {
            intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", account);
        }
        return intent;
    }
    
    private void initButtonText(View root) {
        CircleLayout cl = (CircleLayout) root.findViewById(R.id.button_container);
        int childCount =  cl.getChildCount();
        String[] childText = getContext().getResources().getStringArray(R.array.dialer_str);
        for(int i = 0; i < childCount; i++) {
            Button child = (Button)cl.getChildAt(i);
            child.setText(childText[i]);
            child.setOnClickListener(this);
            if(i == (childCount - 1)) {
                child.setOnLongClickListener(this);
            }
        }
    }
    
    public void onClick(View v) {
        int id = v.getId();
        String text = "";
        switch(id) {
            case R.id.buttion_0:
                text = "0";
                break;
            
            case R.id.buttion_1:
                text = "1";
                break;
                
            case R.id.buttion_2:
                text = "2";
                break;
                
            case R.id.buttion_3:
                text = "3";
                break;
                
            case R.id.buttion_4:
                text = "4";
                break;
                
            case R.id.buttion_5:
                text = "5";
                break;
                
            case R.id.buttion_6:
                text = "6";
                break;
                
            case R.id.buttion_7:
                text = "7";
                break;
                
            case R.id.buttion_8:
                text = "8";
                break;
                
            case R.id.buttion_9:
                text = "9";
                break;
                
            case R.id.buttion_c:
                String digits = mDigits.getText().toString();
                if(digits.length() >= 1) {
                    digits = digits.substring(0, (digits.length() - 1));
                    mDigits.setText(digits);
                }
                return;    
                
        }
        
        
        mDigits.append(text);
    }
    
    public boolean onLongClick(View v) {
        mDigits.setText("");
        return true;
    }
    
  private class DialButtonClickListener
    implements View.OnClickListener
  {
    private DialButtonClickListener()
    {
    }

    public void onClick(View paramView)
    {
      String number = mDigits.getText().toString();
      if(number.length()==0){//Kalyy Bug 48991
          return;
      }
      Intent intent = createCallIntent(number);
      getContext().startActivity(intent);
      mDigits.setText("");
    }
  }
}