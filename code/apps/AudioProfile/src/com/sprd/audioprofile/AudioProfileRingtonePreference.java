

package com.sprd.audioprofile;

import com.sprd.audioprofile.AudioProfileSoundSettings;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.RingtonePreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.provider.MediaStore;
import java.io.IOException;
import android.text.TextUtils;

import android.view.View;
import android.widget.Toast;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Gravity;
import android.telephony.TelephonyManager;
import android.content.ContentResolver;
import android.database.Cursor;
import com.sprd.android.config.OptConfig;//Kalyy

public class AudioProfileRingtonePreference extends RingtonePreference {
    private static final String TAG = "AudioProfileDefaultRingtonePreference";

    private MediaPlayer mLocalPlayer;
    private AudioProfile mProfile;
    int mType = RingtoneManager.TYPE_RINGTONE;
    private String mkey = "ringtone0";
    private int mEditId = -1;
    private int mPhoneCount = 1;
    private int mPhoneId = 0;

    public AudioProfileRingtonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareRingtonePickerIntent(Intent ringtonePickerIntent) {
        super.onPrepareRingtonePickerIntent(ringtonePickerIntent);
        init();
        Uri uri = null;
        switch (mType) {
            case RingtoneManager.TYPE_RINGTONE:
                if (mProfile != null) {
                    for (int i = 0; i < mPhoneCount; i++) {
                        if (AudioProfileSoundSettings.ringtoneKey[i] != null
                                && mkey.equals(AudioProfileSoundSettings.ringtoneKey[i])
                                /* SPRD: Modified for bug 532594, set mPhoneId correctly @{ */
                                /*&& mProfile.mRingtoneUri[i] != null*/) {
                            if(mProfile.mRingtoneUri[i] != null) {
                                uri = Uri.parse(mProfile.mRingtoneUri[i]);
                            }
                            /* @} */
                            mPhoneId = i;
                            break;
                        }
                    }

                }
                break;
            case RingtoneManager.TYPE_NOTIFICATION:
                if (mProfile != null && mProfile.mNotificationUri != null) {
                    uri = Uri.parse(mProfile.mNotificationUri);
                }
                break;
            case AudioProfileSoundSettings.RINGTONETYPE_MESSAGE:
                if (mProfile != null) {
                    for (int i = 0; i < mPhoneCount; i++) {
                        if (AudioProfileSoundSettings.mMessagetoneKey[i] != null
                        && mkey.equals(AudioProfileSoundSettings.mMessagetoneKey[i])
                        /* SPRD: Modified for bug 532594, set mPhoneId correctly @{ */
                        /*&& mProfile.mMessagetoneUri[i] != null*/) {
                            if(mProfile.mMessagetoneUri[i] != null) {
                                uri = Uri.parse(mProfile.mMessagetoneUri[i]);
                            }
                            mPhoneId = i;
                            /* @} */
                            break;
                        }
                    }
                }
                break;
        // TODO:add mms ringtone
        }
        if(OptConfig.SUN_CUSTOM_RINGTONE) {
            ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_MORE_RINGTONES,true);
        }else{
            ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_MORE_RINGTONES,false);
        }

        if (uri != null) {
            ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri);
            ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, uri);
        }
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
    }

    public void setEditId(int id) {
        mEditId = id;
    }

    @Override
    protected void onSaveRingtone(Uri ringtoneUri) {
        Context context = getContext();
        int type = getRingtoneType();
        mLocalPlayer = new MediaPlayer();
        Uri localUri = ringtoneUri;
        if (localUri == null) {
            Toast toast = Toast.makeText(context, R.string.ringtone_not_exist_message,
                    Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        } else {
            Log.d(TAG, "localUri = " + localUri.toString());
        }
        try {
            mLocalPlayer.setDataSource(context, localUri);
        } catch (SecurityException e) {
            Log.e(TAG, "1---SecurityException");
            destroyLocalPlayer();
        } catch (IOException e) {
            Log.e(TAG, "1---IOException");
            destroyLocalPlayer();
            String defaultUriString;
            mLocalPlayer = new MediaPlayer();
            try {
                switch (type) {
                    case RingtoneManager.TYPE_RINGTONE:
                        defaultUriString = Settings.System.getString(context.getContentResolver(),
                                Settings.System.DEFAULT_RINGTONE + mPhoneId);//sprd 20160908
                        localUri = (defaultUriString != null ? Uri.parse(defaultUriString) : null);
                        break;
                    case RingtoneManager.TYPE_NOTIFICATION:
                        defaultUriString = Settings.System.getString(context.getContentResolver(),
                                Settings.System.DEFAULT_NOTIFICATION);
                        localUri = (defaultUriString != null ? Uri.parse(defaultUriString) : null);
                        break;
                    case AudioProfileSoundSettings.RINGTONETYPE_MESSAGE:
                        defaultUriString = Settings.System.getString(context.getContentResolver(),
                                Settings.System.DEFAULT_MESSAGE + mPhoneId);//sprd 20160908
                        localUri = (defaultUriString != null ? Uri.parse(defaultUriString) : null);
                        break;
                }
                if (localUri != null) {
                    mLocalPlayer.setDataSource(context, localUri);
                    Toast toast = Toast.makeText(context, R.string.ringtone_default_message,
                            Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            } catch (SecurityException ex) {
                Log.e(TAG, "2---SecurityException");

                destroyLocalPlayer();
            } catch (IOException ex) {
                Log.e(TAG, "2---IOException");
                destroyLocalPlayer();
            } finally {
                destroyLocalPlayer();
            }
        }finally {
            destroyLocalPlayer();
        }

        // save update to loacal DB
        ContentValues values = new ContentValues();
        switch (type) {
            case RingtoneManager.TYPE_RINGTONE:
                values.put((AudioProfileProvider.ringtoneStr[mPhoneId]),localUri == null ? null : localUri.toString());
                break;
            case RingtoneManager.TYPE_NOTIFICATION:
                values.put(AudioProfileColumns.NOTIFICATION_URI, localUri == null ? null : localUri.toString());
                break;
            case AudioProfileSoundSettings.RINGTONETYPE_MESSAGE:
                values.put((AudioProfileProvider.mMessagetoneStr[mPhoneId]),localUri == null ? null : localUri.toString());
        }

        if (mProfile == null) {
            init();
        }
        mProfile.update(context, values);
        if (mProfile.mIsSelected == AudioProfile.NOT_SELECTED) {
            return;
        } else if(type == AudioProfileSoundSettings.RINGTONETYPE_MESSAGE){
            Settings.System.putString(getContext().getContentResolver(), "messagetone" + mPhoneId, localUri.toString());
        } else {
            Log.d(TAG, "onSaveRingtone---setActualDefaultRingtoneUri---mPhoneId = " + mPhoneId + ", localUri = " + localUri);
            RingtoneManager.setActualDefaultRingtoneUri(context, mType, localUri, mPhoneId);
            Uri actRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, mType, mPhoneId);
        }
    }

    private void destroyLocalPlayer() {
        if (mLocalPlayer != null) {
            mLocalPlayer.reset();
            mLocalPlayer.release();
            mLocalPlayer = null;
        }
    }

    @Override
    protected Uri onRestoreRingtone() {
        return RingtoneManager.getActualDefaultRingtoneUri(getContext(), getRingtoneType(),
                mPhoneId);
    }

    private void init() {
        Log.d(TAG,"mEditId = " + mEditId);
        if (mEditId != -1) {
            mProfile = AudioProfile.restoreProfileWithId(getContext(), mEditId);
        }
        mType = getRingtoneType();
        mkey = getKey();
        Log.d(TAG,"mkey = "+mkey);
        mPhoneCount = TelephonyManager.getDefault().getPhoneCount();
    }

}
