package com.android.messaging.smil.view;

import java.util.Collections;
import java.util.List;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.datamodel.media.ImageRequestDescriptor;
import com.android.messaging.datamodel.media.MessagePartImageRequestDescriptor;
import com.android.messaging.datamodel.media.UriImageRequestDescriptor;
import com.android.messaging.ui.AsyncImageView;
import com.android.messaging.ui.AudioAttachmentView;
import com.android.messaging.ui.OtherItemView;
import com.android.messaging.ui.PersonItemView;
import com.android.messaging.ui.VcalendarItemView;
import com.android.messaging.ui.VdataUtils;
import com.android.messaging.ui.VideoThumbnailView;
import com.android.messaging.util.Assert;
import com.android.messaging.util.ContentType;
import com.android.messaging.util.GlobleUtil;
import com.android.messaging.util.ImageUtils;
import com.android.messaging.util.YouTubeUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;

import com.android.messaging.datamodel.media.MessagePartVideoThumbnailRequestDescriptor;
import com.sprd.messaging.drm.MessagingUriUtil;

public class TextImage extends RelativeLayout {

    private Context mContext;
    private Activity mActivity;
    private Handler mHandler;

    private MessagePartVideoThumbnailRequestDescriptor messagePartVideoThumbnailRequestDescriptor;

    public TextImage(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public TextImage(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setActivity(Activity activity) {
        mActivity = activity;
    }

    public TextImage(Context context) {
        super(context);
    }

    protected void onFinishInflate() {
        Log.i(SmileditPar.TAG, "TextImage--->onFinishInflate()-->");
        mContainer = (LinearLayout) findViewById(R.id.container_rectangle);
        mImage = (AsyncImageView) findViewById(R.id.media_img);
        mEditText = (EditText) findViewById(R.id.text_edit);
        mTextView = (TextView) findViewById(R.id.smil_textview);
        setDisableEdit();
    }

    void setHandler(Handler handler){
        mHandler = handler;
    }

    public void setEditable() {
        getEditText().setFocusableInTouchMode(true);
        getEditText().setFocusable(true);
        getEditText().requestFocus();
    }

    public void setDisableEdit() {
        getEditText().setFocusableInTouchMode(false);
        getEditText().setFocusable(false);
    }

    public void ChangeToVedio() {
        // change Style;
        mContainer.setOrientation(LinearLayout.VERTICAL);

        // RelativeLayout.LayoutParams lp1 = new RelativeLayout.LayoutParams
        // (ViewGroup.LayoutParams.WRAP_CONTENT,
        // ViewGroup.LayoutParams.WRAP_CONTENT);
        // lp1.addRule(RelativeLayout.CENTER_HORIZONTAL);
        // lp1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        // getImageView().setLayoutParams(lp1);
        //
        // RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams
        // (ViewGroup.LayoutParams.WRAP_CONTENT,
        // ViewGroup.LayoutParams.WRAP_CONTENT);
        //
        // lp2.addRule(RelativeLayout.BELOW);
        // lp2.addRule(RelativeLayout.CENTER_HORIZONTAL);
        // lp2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        // getEditText().setLayoutParams(lp1);
    }

    public void setDrawable(Drawable image) {
        getImageView().setImageDrawable(image);
    }

    public void setDrawable(int resId) {
        getImageView().setImageResource(resId);
    }

    public void setDrawable(Bitmap bmp) {
        getImageView().setImageBitmap(bmp);
    }

    public void setText(String szText) {
        getEditText().setText(szText);
    }

    public TextView getTextView(){
        return mTextView;
    }

    public String getText() {
        return getEditText().getText().toString();
    }

    public EditText getEditText() {
        return mEditText;
    }

    public AsyncImageView getImageView() {
        return mImage;
    }

    public void initUri(Uri uri, MessagePartData messagePartData,
            Context context) {

            if(messagePartData.getContentUri() == null && messagePartData.getContentType() != null){
                if(messagePartData.getContentType().equalsIgnoreCase("text/plain")){
                    if(messagePartData.getText() != null){
                        mTextView.setText(messagePartData.getText());
                    }else{
                        mTextView.setText(context.getResources().getString(R.string.hint_for_empty));
                    }
                    setVisible(View.GONE, View.GONE);
                    mTextView.setVisibility(View.VISIBLE);
                }
                return;
            }
            if(messagePartData.getContentUri() == null && messagePartData.getContentType() == null){
                Log.i("initUri", "uri--mMessagePartData.getContentUri()--> null");
                if(messagePartData.getText() != null){
                    if(!messagePartData.getText().equals("")){
                        mTextView.setText(messagePartData.getText());
                    }else{
                        mTextView.setText(context.getResources().getString(R.string.hint_for_empty));
                    }
                }else{
                    mTextView.setText(context.getResources().getString(R.string.hint_for_empty));
                }
                setVisible(View.GONE, View.GONE);
                mTextView.setVisibility(View.VISIBLE);
                return;
            }
            if(messagePartData.getContentUri() != null){
                Log.i("initUri", "uri--mMessagePartData.getContentUri()-->" + mMessagePartData.getContentUri());
                if(messagePartData.getContentType() == null){
                    getContentType(uri, messagePartData,context);
                }
                showFilePicture(messagePartData, context);
                if(messagePartData.getContentType().equalsIgnoreCase("text/plain")){
                    if(messagePartData.getText() != null){
                        mTextView.setText(messagePartData.getText());
                    }else{
                        mTextView.setText(context.getResources().getString(R.string.hint_for_empty));
                    }
                    setVisible(View.GONE, View.GONE);
                    mTextView.setVisibility(View.VISIBLE);
                }
            }
    }

    private void getContentType(Uri uri, MessagePartData messagePartData, Context context) {
        switch (GlobleUtil.FLAG_TYPE_URI) {
        case IMAGE_URI_TEXT:
            messagePartData.setContentType("text/plain");
            break;
        case IMAGE_URI_FLAG:
            messagePartData.setContentType(ImageUtils.getContentType(Factory
                    .get().getApplicationContext().getContentResolver(), uri));
            break;
        case AUDIO_URI_FLAG:
            String AudioType = VdataUtils.getAudioType(VdataUtils.getFileType(
                    uri, "oog"));
            messagePartData.setContentType(ContentType
                    .getContentTypeFromExtension(uri.toString(), AudioType));
            break;
        case VCARD_URI_TYPE:
            messagePartData.setContentType(ContentType.TEXT_LOWER_VCARD);
            break;
        case VCALENDAR_URI_TYPE:
            messagePartData.setContentType(ContentType
                    .getContentTypeFromExtension(uri.toString(),
                            ContentType.TEXT_VCALENDAR));
            break;
        case VEDIO_URI_FLAG:
            messagePartData.setContentType(VdataUtils.getVideoType(VdataUtils
                    .getFileType(uri, "3gp")));
            break;
        case VCARD_OTHER_FLAG:

            break;
        }
        Log.i(SmileditPar.TAG, "type---->" + messagePartData.getContentType());
    }

    private void showFilePicture(MessagePartData messagePartData,
            Context context) {
 
        String flagType = "";
        Log.i("SmilMainFragment", "uri---->" + mMessagePartData.getContentUri());
        Log.i("SmilMainFragment",
                "contenttype---->" + mMessagePartData.getContentType());
        if (messagePartData.isVideo()) {
            messagePartVideoThumbnailRequestDescriptor = new MessagePartVideoThumbnailRequestDescriptor(
                    messagePartData);
            mImage.setImageResourceId(messagePartVideoThumbnailRequestDescriptor);
            ChangeToVedio();
            // setType();
            flagType = context.getResources().getString(R.string.video_content_type);
            adjustImageViewBounds(messagePartData);
            getEditText().setText(flagType  + messagePartData.getContentUri());
            setVisible(View.VISIBLE, View.VISIBLE);
            mTextView.setVisibility(View.GONE);

        } else if (messagePartData.isImage()) {
            // String imagepath = MessagingUriUtil.getPath(context,
            // messagePartData.getContentUri());
            // Bitmap bitmap = BitmapFactory.decodeFile(imagepath);
            updateMessageAttachments(context, messagePartData);
            flagType = context.getResources().getString(R.string.iamge_content_type);
            ChangeToVedio();
            getEditText().setText(flagType  + messagePartData.getContentUri());
            setVisible(View.VISIBLE, View.VISIBLE);
            mTextView.setVisibility(View.GONE);
        } else if (messagePartData.isAudio()) {
            flagType = context.getResources().getString(R.string.audio_content_type);
            mImage.setImageDrawable(context.getResources().getDrawable(
                    R.drawable.ic_preview_play));
            getEditText().setText(flagType  + messagePartData.getContentUri());
            setVisible(View.VISIBLE, View.VISIBLE);
            mTextView.setVisibility(View.GONE);
        } else if (messagePartData.isVCard()) {
            flagType = context.getResources().getString(R.string.vcard_content_type);
            mImage.setImageDrawable(context.getResources().getDrawable(
                    R.drawable.ic_attach_vcard));
            getEditText().setText(flagType  + messagePartData.getContentUri());
            setVisible(View.VISIBLE, View.VISIBLE);
            mTextView.setVisibility(View.GONE);
        } else if (messagePartData.isVCalendar()) {
            flagType = context.getResources().getString(R.string.vcalendar_content_type);
            mImage.setImageDrawable(context.getResources().getDrawable(
                    R.drawable.ic_attach_vcalendar_dark));
            getEditText().setText(flagType  + messagePartData.getContentUri());
            setVisible(View.VISIBLE, View.VISIBLE);
            mTextView.setVisibility(View.GONE);
        }
    }

    public void setVisible(int nimageVisible, int nEditVisible) {
        getImageView().setVisibility(nimageVisible);
        getEditText().setVisibility(nEditVisible);
    }

    public void setTextColor(int nColor) {
        getEditText().setTextColor(nColor);
    }

    public void setTextSize(float fltSize) {
        getEditText().setTextSize(fltSize);
    }

    public void setOnClick(View.OnClickListener l) {
        if (l == null) {
            getEditText().setClickable(false);
            getEditText().setOnClickListener(null);
        } else {
            getEditText().setClickable(true);
            getEditText().setOnClickListener(l);
        }
    }

    private void updateMessageAttachments(Context context,
            MessagePartData imagePart) {

        // We will show the message image view if there is one attachment or one
        // youtube link
        // Get the display metrics for a hint for how large to pull the image
        // data into
        final WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        final int iconSize = getResources().getDimensionPixelSize(
                R.dimen.conversation_message_contact_icon_size);
        final int desiredWidth = displayMetrics.widthPixels - 3 * iconSize - 3
                * iconSize;

        // If the image is big, we want to scale it down to save memory since
        // we're going to
        // scale it down to fit into the bubble width. We don't constrain the
        // height.
        final ImageRequestDescriptor imageRequest = new MessagePartImageRequestDescriptor(
                imagePart, 360, 360, false);
        if (imagePart.isDrmType()) {
            imageRequest.setDrmType(true);
            imageRequest.setDrmContentType(imagePart.getDrmOrigContentType());
            imageRequest.setDrmDataPath(imagePart.getDrmDataPath());
        }
        adjustImageViewBounds(imagePart);
        mImage.setImageResourceId(imageRequest);
        mImage.setTag(imagePart);
    }

    /**
     * If we don't know the size of the image, we want to show it in a
     * fixed-sized frame to avoid janks when the image is loaded and resized.
     * Otherwise, we can set the imageview to take on normal layout params.
     */
    private void adjustImageViewBounds(final MessagePartData imageAttachment) {
        final ViewGroup.LayoutParams layoutParams = mImage.getLayoutParams();
        if (imageAttachment.getWidth() == MessagePartData.UNSPECIFIED_SIZE
                || imageAttachment.getHeight() == MessagePartData.UNSPECIFIED_SIZE) {
            // We don't know the size of the image attachment, enable
            // letterboxing on the image
            // and show a fixed sized attachment. This should happen at most
            // once per image since
            // after the image is loaded we then save the image dimensions to
            // the db so that the
            // next time we can display the full size.
            layoutParams.width = getResources().getDimensionPixelSize(
                    R.dimen.image_attachment_fallback_width);
            layoutParams.height = getResources().getDimensionPixelSize(
                    R.dimen.image_attachment_fallback_height);
            mImage.setScaleType(ScaleType.CENTER_CROP);
        } else {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            // ScaleType.CENTER_INSIDE and FIT_CENTER behave similarly for most
            // images. However,
            // FIT_CENTER works better for small images as it enlarges the image
            // such that the
            // minimum size ("android:minWidth" etc) is honored.
            mImage.setScaleType(ScaleType.FIT_START);
        }
    }

    public MessagePartData getTextImageMessagePartData() {
        return mMessagePartData;
    }

    public void bindMessagePartData(MessagePartData messagePartData) {
        mMessagePartData = messagePartData;
    }

    private OnCreateContextMenuListener OnCreateContextMenuListener;

    public void setMyOnCreateContextMenuListener(
            OnCreateContextMenuListener myOnCreateContextMenuListener) {
        OnCreateContextMenuListener = myOnCreateContextMenuListener;
    }

    private OnLongClickListener mClickListener;

    private void setRectangleLongclickListner(OnLongClickListener listener) {
        this.mClickListener = listener;
    }

    public int getType() {
        return mnType;
    }

    public void setType(int nType) {
        mnType = nType;
    }

    public void setContextType(String szContentType) {
        mszContentType = szContentType;

    };

    private int mnType;
    private String mszContentType;
    private AsyncImageView mImage;
    private LinearLayout mContainer;
    private EditText mEditText;
    private MessagePartData mMessagePartData;
    private TextView mTextView;

    public static final int IMAGE_URI_TEXT = 0x00000001;
    public static final int IMAGE_URI_FLAG = 0x00000002;
    public static final int AUDIO_URI_FLAG = 0x00000010;

    public static final int VCARD_URI_TYPE = 0X00000005;
    public static final int VCALENDAR_URI_TYPE = 0X00000008;

    public static final int VCARD_OTHER_FLAG = 0x00000000;
    public static final int VEDIO_URI_FLAG = AUDIO_URI_FLAG | IMAGE_URI_FLAG;

}
