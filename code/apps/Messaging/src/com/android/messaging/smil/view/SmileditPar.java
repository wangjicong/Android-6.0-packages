package com.android.messaging.smil.view;

import java.io.ObjectInputStream.GetField;
import java.util.ArrayList;
import java.util.List;

import com.android.messaging.R;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.smil.data.SmilPartEntity;
import com.android.messaging.smil.ui.SmilMainFragment;
import com.android.messaging.ui.mediapicker.SmilMediaPicker;
import com.android.messaging.util.GlobleUtil;

import android.R.drawable;
import android.support.v7.mms.pdu.PduPart;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView.OnEditorActionListener;

public class SmileditPar extends LinearLayout {

    private PduPart mPart;
    private Handler mHandler;
    private SmilMediaPicker mSmilMediaPicker;

    private boolean isDraft;
    private Activity mActivity;
    private SmilPartEntity mPartEntity;

    private int mnIndex = 0;
    protected  int getIndex(){
        return mnIndex ; 
    }
    public void setIndex(int nIndex){
        Log.e(TAG, "=====>>>  SmilPart=+setIndex("+nIndex+") ");
        mnIndex = nIndex; 
    }
    private SmilPartEntity getSmilPartEntity() {
        return mPartEntity;
    }

    public SmileditPar(Context context) {
        super(context);
    }

    public SmileditPar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SmileditPar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void initFromPar() {

    }

    public void setDraftFlag(boolean isdraft) {
        isDraft = isdraft;
    }

    public void init() {
        removeAllViews();

        mHandler.post(runnable);
    }
    

    public void setSmilPartEntity(SmilPartEntity smileditPar) {
        mPartEntity = null;
        mPartEntity = smileditPar;

    }

    public void setSmilPartEntity(SmilPartEntity smileditPar, Context context) {

        setSmilPartEntity(smileditPar);

        removeAllViews();

        mHandler.post(runnable);
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            initSmilPart(getSmilPartEntity());
        }
    };

    protected void initSmilPart(SmilPartEntity smileditPar) {
        for (int i = 0; i < smileditPar.size(); i++) {
            Log.i(TAG,
                    "SmilPart=["+getIndex()+"]th initSmilPart --smileditPar.size()=["
                            + smileditPar.size()+"]  index = ["+i+"] MessagePartData");
            formatPar(i, smileditPar.get(i));
        }
        Log.i(TAG,
                "SmilPart=["+getIndex()+"]th initSmilPart --smileditPar.size()=["
                        + smileditPar.size()+"]");
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public void setActivity(Activity activity) {
        mActivity = activity;
    }

    protected void initPart(int nIndex,  MessagePartData data) {
        formatPar(nIndex,  data);
    }

    public void formatPar(int nIndex, MessagePartData data) {
        if (data == null) {
            return;
        }
        addRectangelView(nIndex, data);

    }

    protected void onFinishInflate() {
        Log.i(TAG, "SmilPart=["+getIndex()+"]th  onFinishInflate---", new Exception());
        Log.i(TAG, "SmilPart=["+getIndex()+"]th  SmileditPar--->onFinishInflate()--");
        mMainItemLayout = (LinearLayout) findViewById(R.id.item_edit);
    }

    private void intRectangleContextMenu(TextImage image) {

        image.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
 
                v.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
                    
                    @Override
                    public void onCreateContextMenu(ContextMenu menu, View v,
                            ContextMenuInfo menuInfo) {
                        if (mActivity != null) {
                            MenuInflater menuInflater = mActivity.getMenuInflater();
                            menuInflater.inflate(R.menu.smil_context_menu, menu);
                        }
                        
                    }
                });

                GlobleUtil.mTemptSmileditPar = (SmileditPar) v.getParent();
                ArrayList<TextImage> textImages = new ArrayList<TextImage>();
                for (int i = 0; i < getMainItemLayout().getChildCount(); i++) {
                    textImages.add((TextImage) getMainItemLayout()
                            .getChildAt(i));
                }
                int postion = -1;

                for (TextImage mimage : textImages) {
                    postion++;
                    if (mimage.equals(v)) {
                        GlobleUtil.mViewPosition = postion;
                        Log.i(TAG, "SmilPart=["+getIndex()+"]th  GlobleUtil.mViewPosition ---->"
                                + GlobleUtil.mViewPosition);
                        break;
                    }
                }
                return false;
            }
        });
    }



    public void addRectangelView(int index, MessagePartData data) {
        Log.i(TAG,     "=========>SmilPar["+ getIndex()+"  SmileditPar--->addRectangelView("+index+")");
        TextImage ins = initControlFromLayout();
        if (ins != null) {
            ins.getEditText().setTag(ins);
            ins.setTag(this);

            getMainItemLayout().addView(ins);

            ins.bindMessagePartData(data);
            ins.initUri(data.getContentUri(),  data, getContext());

            if (isDraft) {
                intRectangleContextMenu(ins);
            }

        }
    }

    public void addRectangelViewFromFragment(int index, MessagePartData data) {

        TextImage ins = initControlFromLayout();
        if (ins != null) {
            ins.getEditText().setTag(ins);
            ins.setTag(this);

            ins.bindMessagePartData(data);
            getSmilPartEntity().add(index, data);
            ins.initUri(data.getContentUri(),
                    data, getContext());
            
            getMainItemLayout().addView(ins);

            if (isDraft) {
                intRectangleContextMenu(ins);
            }
            this.requestLayout();
            mHandler.sendEmptyMessage(SmilMainFragment.ACTION_COMPARE);

            Log.i(TAG,
                    "SmilPart=["+getIndex()+"]th --->addRectangelView()-after-getSmilPartEntity().size()--->"
                            + getSmilPartEntity().size());
            // callback to remesure the listview mmHandler.sasdadas
        }

    }

    public void removeRectangelView(Context context, int index) {
        if (index < 0) {
            index = 0;
        }
        if (getMainItemLayout() != null) {
            ((ViewGroup) getMainItemLayout()).removeView(getChildAt(index));
            getSmilPartEntity().remove(index);
            this.requestLayout();

            Log.i(TAG, "SmilPart=["+getIndex()+"]th  --->deleteRectangelView()-->"
                    + getSmilPartEntity().size());
            if (getSmilPartEntity().size() == 0) {
                mHandler.sendEmptyMessage(SmilMainFragment.REMOVE_LISTVIE_ITEM);
            }
        }
    }

 
    protected TextImage initControlFromLayout() {
        return (TextImage) LayoutInflater.from(getContext()).inflate(
                R.layout.smil_rectangle, null);
    }

    public LinearLayout getMainItemLayout() {
        return this;
    }

   

    private LinearLayout mMainItemLayout; //
    // private TextImage mFirstRectangle;

    private ArrayList<TextImage> mlist = new ArrayList<TextImage>();
    public static final String TAG = "SmileditPar";

    private OnClickListener getTextClick() {
        return mIns;
    }

    private OnClickListener getBlankClick() {
        return myBLankEditClick;
    }

    private MyTextClick mIns = new MyTextClick();
    private MyBLankEditClick myBLankEditClick = new MyBLankEditClick();

    /***************************************************
     * 
     * @author yao_y.chen
     *
     */
    class MyTextClick implements OnClickListener {

        @Override
        public void onClick(View v) {

        }
    }

    /**
	 * 
	 */
    /***************************************************
     * 
     * @author yao_y.chen
     *
     */
    class MyBLankEditClick implements OnClickListener {

        @Override
        public void onClick(View v) {

        }

   

        protected byte[] convertStringtobyte(String filename) {
            byte[] byBuffer = new byte[200];
            try {
                byBuffer = filename.getBytes("gb2312");
                return byBuffer;
            } catch (Exception e) {
                System.out.println("convertStringtobyte  error");
            }
            return null;
        }

        public void setSmilMediaPicker(SmilMediaPicker smilMediaPicker) {
            mSmilMediaPicker = smilMediaPicker;
        }
        
        private MyOnCreateContextMenuListener getMyOnCreateContextMenuListener(){
            return contextMenuListener;
        }

        private final MyOnCreateContextMenuListener contextMenuListener = new MyOnCreateContextMenuListener();

        class MyOnCreateContextMenuListener implements
                OnCreateContextMenuListener {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v,
                    ContextMenuInfo menuInfo) {
                if (mActivity != null) {
                    MenuInflater menuInflater = mActivity.getMenuInflater();
                    menuInflater.inflate(R.menu.smil_context_menu, menu);
                }
            }

        }
    }
}