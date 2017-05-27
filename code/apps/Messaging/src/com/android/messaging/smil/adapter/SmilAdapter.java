package com.android.messaging.smil.adapter;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.support.v7.mms.pdu.PduPart;

import com.android.messaging.R;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.smil.data.SmilPartEntity;
import com.android.messaging.smil.view.SmileditPar;
import com.android.messaging.ui.mediapicker.SmilMediaPicker;
import com.android.messaging.util.GlobleUtil;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class SmilAdapter extends BaseAdapter {

    private List<SmilPartEntity> mList;
    private Context mContext;
    private LayoutInflater mInflater;
    private Handler mHandler;
    private SmilMediaPicker mSmilMediaPicker;
    private boolean isDraft = false;
    private Activity mActivity;
 
    public SmilAdapter(List<SmilPartEntity> mList, Context context,
            Handler handler, boolean isdraft) {

        this.mList = mList;
        this.mContext = context;
        this.mHandler = handler;
        mInflater = LayoutInflater.from(mContext);
        isDraft = isdraft;
        Log.i("SmileditPar", "SmilAdapter size ---->"+mList.size());
    }

    public void setActivity(Activity activity) {
        mActivity = activity;
    }

    @Override
    public int getCount() {
        if (mList != null) {
            return mList.size();
        } else {
            return -1;
        }
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.e("SmileditPar", "====>>>>getView Posistion = ["+position+"]");
        final SmileditPar smileditPar;
        
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.smil_editpar, null);
        }
        
        if (mList != null && mList.size() > 0) {
            smileditPar = (SmileditPar) convertView;
            smileditPar.setId(position);
            smileditPar.setActivity(mActivity);
            smileditPar.setHandler(mHandler);
            smileditPar.setDraftFlag(isDraft);
            smileditPar.setSmilPartEntity(mList.get(position),mContext);
        }
        return convertView;
    }

    public List<SmilPartEntity> getAdapterList() {
        return mList;
    }

    public void setSmilMediaPicker(SmilMediaPicker milMediaPicker) {
        mSmilMediaPicker = milMediaPicker;
    }
}
