package com.android.messaging.ui.mediapicker;

import java.util.ArrayList;
import java.util.Collection;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.Contacts;
import android.util.Log;

import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.smil.ui.SmilMainFragment;
import com.android.messaging.smil.view.TextImage;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.util.GlobleUtil;

public class SmilMediaPicker extends MediaPicker {

    private Fragment  mFragment;


    public SmilMediaPicker() {
        super();
    }
 
    public SmilMediaPicker(Context context) {
        super(context);
    }

    public static final String TAG = "SmilMediaPicker";

    public void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        // mDocumentImagePicker.onActivityResult(requestCode, resultCode, data);
        if(resultCode != Activity.RESULT_OK || data == null){
            Log.i(TAG, "onActivityResult----resultCode is not ok");
            return;
        }
        Uri uri = null;   
        if (requestCode == UIIntents.REQUEST_PICK_IMAGE_FROM_DOCUMENT_PICKER) {
            String url = data.getStringExtra("photo_url");
            if (url == null) {
                url = data.getDataString();
                if (url == null) {
                    final Bundle extras = data.getExtras();
                    if (extras != null) {
                        uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
                        if (uri != null) {
                            Log.i(TAG, "photoUri---->"+uri.toString());
                        }
                    }
                }
            }
            if(url != null){
                uri  = Uri.parse(url);
            }
            GlobleUtil.FLAG_TYPE_URI = TextImage.IMAGE_URI_FLAG;
          
        } else if (requestCode == UIIntents.REQUEST_PICK_VCARD_PICKER) {
            ArrayList<String> lookupStringKeys = data
                    .getStringArrayListExtra("result");
            if (lookupStringKeys != null) {
                final Uri vcardUri = getVcardUri(lookupStringKeys);
                uri = vcardUri;
                Log.i(TAG, "vcardUri---->"+vcardUri.toString());
                GlobleUtil.FLAG_TYPE_URI = TextImage.VCARD_URI_TYPE;
            }
        } else if (requestCode == UIIntents.REQUEST_PICK_VIDEO_PICKER) {
            uri = data.getData();
            Log.i(TAG, "videoUri---->"+uri.toString());
            GlobleUtil.FLAG_TYPE_URI = TextImage.VEDIO_URI_FLAG;
        } else if (requestCode == UIIntents.REQUEST_PICK_AUDIO_PICKER) {
            if (data != null) {
                uri = (Uri) data
                        .getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                  //prepareAudioAttachForAttachment(uri);
                Log.i(TAG, "audioUri---->"+uri.toString());
                GlobleUtil.FLAG_TYPE_URI = TextImage.AUDIO_URI_FLAG;
            }
        } else if (requestCode == UIIntents.REQUEST_PICK_VCALENDAR_PICKER) {
            uri = data.getData();
            Log.i(TAG, "vcalendarUri---->"+uri.toString());
            GlobleUtil.FLAG_TYPE_URI = TextImage.VCALENDAR_URI_TYPE;
        }
        ((SmilMainFragment) mFragment).setDatechanged(uri);
        dismiss(true);
    };

    public void setFragment(SmilMainFragment fragment){
        mFragment = fragment;
    }


    public void setHandler(Handler handler){
        mHandler = handler;
    }

    public static Uri getVcardUri(ArrayList<String> lookupKeys) {
        StringBuilder uriListBuilder = new StringBuilder();
        int index = 0;
        for (String key : lookupKeys) {
            if (index != 0)
                uriListBuilder.append(':');
            uriListBuilder.append(key);
            index++;
        }

        String lookupKeyStrings = lookupKeys.size() > 1 ? Uri
                .encode(uriListBuilder.toString()) : uriListBuilder.toString();
        Uri uri = Uri.withAppendedPath(
                lookupKeys.size() > 1 ? Contacts.CONTENT_MULTI_VCARD_URI
                        : Contacts.CONTENT_VCARD_URI, lookupKeyStrings);
        return uri;
    }

    @Override
    void dispatchItemsSelected(Collection<MessagePartData> items,
            boolean dismissMediaPicker) {
        if(items == null && items.size() <= 0){
            return;
        }
        Uri uri = null;
        for(MessagePartData data : items){
            uri = data.getContentUri();
        }

        ((SmilMainFragment) mFragment).setDatechanged(uri);
        dismiss(true);
    }

    private Handler mHandler;

}
