
/*Created by Spreadst for new activity*/
package com.android.providers.media;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;


import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import java.io.File;
import android.database.sqlite.SQLiteException;


import android.media.AudioManager;
import android.content.Context;
import android.view.Gravity;
import android.media.AudioManager.OnAudioFocusChangeListener;



/**
 * The {@link SdcardRingtonePickerActivity} allows the user to choose one from all of the
 * available ringtones. The chosen ringtone's URI will be persisted as a string.
 *
 * @see RingtoneManager#ACTION_RINGTONE_PICKER
 */
public final class SdcardRingtonePickerActivity extends AlertActivity implements
        AdapterView.OnItemSelectedListener, Runnable, DialogInterface.OnClickListener,
        AlertController.AlertParams.OnPrepareListViewListener {

    private static final String TAG = "SdcardRingtonePickerActivity";

    private static final int DELAY_MS_SELECTION_PLAYED = 300;

    private static final String SAVE_CLICKED_POS = "clicked_pos";
    private static final String SAVE_CAN_PLAY = "can_play";

    private RingtoneManager mRingtoneManager;

    private Cursor mCursor;
    private Handler mHandler;

    /** The position in the list of the 'Silent' item. */
    private int mSilentPos = -1;

    /** The position in the list of the 'Default' item. */
    private int mDefaultRingtonePos = -1;

    /** The position in the list of the last clicked item. */
    private int mClickedPos = -1;

    /** The position in the list of the ringtone to sample. */
    private int mSampleRingtonePos = -1;

    /** The Uri to place a checkmark next to. */
    private Uri mExistingUri;

    /** The number of static items in the list. */
    private int mStaticItemCount;

    /** Whether this list has the 'Default' item. */
    private boolean mHasDefaultItem;

    /** The Uri to play when the 'Default' item is clicked. */
    private Uri mUriForDefaultItem;
    private Context mContext;
    private int types;


    AudioManager mAudioManager;
    private Ringtone ringtone;


    /**
     * A Ringtone for the default ringtone. In most cases, the RingtoneManager
     * will stop the previous ringtone. However, the RingtoneManager doesn't
     * manage the default ringtone for us, so we should stop this one manually.
     */
    private Ringtone mDefaultRingtone;
    private boolean mCanPlay = true;

    private DialogInterface.OnClickListener mRingtoneClickListener =
            new DialogInterface.OnClickListener() {

        /*
         * On item clicked
         */
        public void onClick(DialogInterface dialog, int which) {
            // Save the position of most recently clicked item
            mClickedPos = which;

            // Play clip
            playRingtone(which, 0);
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mHandler = new Handler();

        Intent intent = getIntent();

        /* TODO: Resolve the multi-theme condition. So if you want to set this dialog's theme,
         * please don't forget to put the theme you want into Intent. Only effected UniverseUI.
         */
//        if (UNIVERSEUI_SUPPORT) {
//            this.setTheme(intent.getIntExtra(THEME_KEY,com.android.internal.R.style.Theme_UniverseUI_Dialog_Alert));
//        }

        /*
         * Get whether to show the 'Default' item, and the URI to play when the
         * default is clicked
         */
        mHasDefaultItem = intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        mUriForDefaultItem = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI);
        if (mUriForDefaultItem == null) {
            mUriForDefaultItem = Settings.System.DEFAULT_RINGTONE_URI;
        }

        if (savedInstanceState != null) {
            mClickedPos = savedInstanceState.getInt(SAVE_CLICKED_POS, -1);
            // SPRD:save mCanPlay to fix bug 544944
            mCanPlay = savedInstanceState.getBoolean(SAVE_CAN_PLAY, true);
            Log.d(TAG, "onCreate:mCanPlay=" + mCanPlay);
        }

        // Give the Activity so it can do managed queries
        Context context = this.getApplicationContext();
        mRingtoneManager = new RingtoneManager(context);


        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);


        // Get whether to include DRM ringtones
        // SPRD: modify, default false, not show drm files
        final boolean includeDrm = intent.getBooleanExtra(
                RingtoneManager.EXTRA_RINGTONE_INCLUDE_DRM, false);
        mRingtoneManager.setIncludeDrm(includeDrm);
        // Get whether to include External ringtones
        boolean includeExternal = intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_INCLUDE_EXTERNAL, false);
        mRingtoneManager.setIncludeExternal(includeExternal);
        // Get the types of ringtones to show
         types = intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, -1);
        if (types != -1) {
            mRingtoneManager.setType(types);
        }

        mCursor = mRingtoneManager.getExternalMusics();
        if((null == mCursor) || (mCursor.getCount() == 0)){
            Toast toast = Toast.makeText(this, R.string.ringtone_dialog_message, Toast.LENGTH_SHORT);
            // SPRD: remove the setting of centrally located
            //toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            finish();
        }
        // The volume keys will control the stream that we are choosing a ringtone for
        setVolumeControlStream(mRingtoneManager.inferStreamType());

        // Get the URI whose list item should have a checkmark
        mExistingUri = intent
                .getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI);

        final AlertController.AlertParams p = mAlertParams;
        p.mCursor = mCursor;
        p.mOnClickListener = mRingtoneClickListener;
        p.mLabelColumn = MediaStore.Audio.Media.TITLE;
        p.mIsSingleChoice = true;
        p.mOnItemSelectedListener = this;
        p.mPositiveButtonText = getString(com.android.internal.R.string.ok);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(com.android.internal.R.string.cancel);
        p.mPositiveButtonListener = this;
        p.mOnPrepareListViewListener = this;

        p.mTitle = intent.getCharSequenceExtra(RingtoneManager.EXTRA_RINGTONE_TITLE);
        if (p.mTitle == null) {
            p.mTitle = getString(com.android.internal.R.string.ringtone_picker_title);
        }

        setupAlert();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_CLICKED_POS, mClickedPos);
        // SPRD:save mCanPlay to fix bug 544944
        Log.d(TAG, "onSaveInstanceState:mCanPlay=" + mCanPlay);
        outState.putBoolean(SAVE_CAN_PLAY, mCanPlay);
    }

    public void onPrepareListView(ListView listView) {
/*        if ((mCursor == null) || (mCursor.getCount() == 0)) {
            Toast.makeText(this, R.string.ringtone_dialog_message, Toast.LENGTH_LONG).show();
        }*/

        if (mHasDefaultItem) {
//            mDefaultRingtonePos = addDefaultRingtoneItem(listView);

            if (RingtoneManager.isDefault(mExistingUri) && mClickedPos == -1) {
                mClickedPos = mDefaultRingtonePos;
            }
        }

        if (mClickedPos == -1) {
            mClickedPos = getListPosition(mRingtoneManager.getCunstomRingtonePosition(mExistingUri));
        }

        // Put a checkmark next to an item.
        mAlertParams.mCheckedItem = mClickedPos;
    }

    /**
     * Adds a static item to the top of the list. A static item is one that is not from the
     * RingtoneManager.
     *
     * @param listView The ListView to add to.
     * @param textResId The resource ID of the text for the item.
     * @return The position of the inserted item.
     */
    private int addStaticItem(ListView listView, int textResId) {
        TextView textView = (TextView) getLayoutInflater().inflate(
                com.android.internal.R.layout.select_dialog_singlechoice_holo, listView, false);
        textView.setText(textResId);
        listView.addHeaderView(textView);
        mStaticItemCount++;
        return listView.getHeaderViewsCount() - 1;
    }

    private int addDefaultRingtoneItem(ListView listView) {
        return addStaticItem(listView, com.android.internal.R.string.ringtone_default);
    }

    private int addSilentItem(ListView listView) {
        return addStaticItem(listView, com.android.internal.R.string.ringtone_silent);
    }

    /*
     * On click of Ok/Cancel buttons
     */
    public void onClick(DialogInterface dialog, int which) {
        boolean positiveResult = which == DialogInterface.BUTTON_POSITIVE;

        // Stop playing the previous ringtone
        mRingtoneManager.stopPreviousRingtone();

        if (positiveResult) {
            Intent resultIntent = new Intent();
            Uri uri = null;

            if (mClickedPos == mDefaultRingtonePos) {
                // Set it to the default Uri that they originally gave us
                uri = mUriForDefaultItem;
            } else if (mClickedPos == mSilentPos) {
                // A null Uri is for the 'Silent' item
                uri = null;
            } else {
                uri = mRingtoneManager.getCustomRingtoneUri(getRingtoneManagerPosition(mClickedPos));
            }


            /* SPRD: default alarm changed when user not selected any ring @{ */
            if (mClickedPos != -1 && mCanPlay) {
                resultIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, uri);
                setResult(RESULT_OK, resultIntent);
            } else {
                setResult(RESULT_CANCELED);
            }
            /* @} */
        } else {
            setResult(RESULT_CANCELED);
        }

/*        getWindow().getDecorView().post(new Runnable() {
            public void run() {
                mCursor.deactivate();
            }
        });*/

        finish();
    }

    /*
     * On item selected via keys
     */
    public void onItemSelected(AdapterView parent, View view, int position, long id) {
        playRingtone(position, DELAY_MS_SELECTION_PLAYED);
    }

    public void onNothingSelected(AdapterView parent) {
    }

    private void playRingtone(int position, int delayMs) {
        mHandler.removeCallbacks(this);
        mSampleRingtonePos = position;
        mHandler.postDelayed(this, delayMs);
    }

    private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(int focusChange) {
            if (mDefaultRingtone == null && ringtone == null) {
                //maybe we should delete this, because it wont be efficient probably.
                mAudioManager.abandonAudioFocus(this);
                Log.d(TAG, "all ringtone is null");
                return;
            }
            Log.d(TAG, "focusChange = " + focusChange);
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (mDefaultRingtone != null) {
                        mDefaultRingtone.stop();
                    }
                    if (ringtone != null) {
                        ringtone.stop();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                    if (mDefaultRingtone != null) {
                        mDefaultRingtone.play();
                    }
                    if(ringtone != null){
                        ringtone.play();
                    }
                    break;
            }
        }
    };

    public void run() {
        /* SPRD: Add this to forbit the ringtone to play when in call @{ */
        boolean success = false;
        if (mAudioManager != null) {
            success = mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_RING,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            if(!success) {
                return;
            }
        }
        /* @} */


        if (mSampleRingtonePos == mSilentPos) {
            mRingtoneManager.stopPreviousRingtone();
            return;
        }

        /*
         * Stop the default ringtone, if it's playing (other ringtones will be
         * stopped by the RingtoneManager when we get another Ringtone from it.
         */
        if (mDefaultRingtone != null && mDefaultRingtone.isPlaying()) {
            mDefaultRingtone.stop();
            mDefaultRingtone = null;
        }

        if (mSampleRingtonePos == mDefaultRingtonePos) {
            if (mDefaultRingtone == null) {
                mDefaultRingtone = RingtoneManager.getRingtone(this, mUriForDefaultItem);
            }
            ringtone = mDefaultRingtone;

            /*
             * Normally the non-static RingtoneManager.getRingtone stops the
             * previous ringtone, but we're getting the default ringtone outside
             * of the RingtoneManager instance, so let's stop the previous
             * ringtone manually.
             */
            mRingtoneManager.stopPreviousRingtone();

        } else {
            ringtone = mRingtoneManager.getCustomRingtone(getRingtoneManagerPosition(mSampleRingtonePos));
        }

        if (ringtone != null) {
            String title = ringtone.getTitle(mContext);
            CharSequence summary = mContext.getString(com.android.internal.R.string.ringtone_default);
            Cursor defCursor = null;
            Uri uri;
            if (mClickedPos == mDefaultRingtonePos) {
                uri = mUriForDefaultItem;
            } else if (mClickedPos == mSilentPos) {
                uri = null;
            } else {
                uri = mRingtoneManager.getCustomRingtoneUri(getRingtoneManagerPosition(mClickedPos));
            }
            try {
                if (uri != null) {
                    Log.d(TAG,"ringtone uri="+uri);
                    defCursor = mContext.getContentResolver().query(uri,
                            new String[] { MediaStore.Audio.Media.TITLE+" as "+MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA }, null, null, null);}

                if (defCursor != null && defCursor.getCount() > 0) {
                    if (defCursor.moveToFirst()) {
                        File filePath = new File(defCursor.getString(1));
                        summary = defCursor.getString(0);
                    }
                }
            } catch (SQLiteException sqle) {

            } catch (IllegalArgumentException e) {
                Log.e(TAG,"IllegalArgumentException"+e);
            }finally {
                if (defCursor != null) {
                    defCursor.close();
                    defCursor = null;
                }
            }
            String title2 = summary.toString();
            if(title2.equals(title)){
                MediaPlayer mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(mContext, uri);
                    mediaPlayer.prepare();
                    ringtone.play();
                    mCanPlay = true;
                } catch (Exception e) {
                    mCanPlay = false;
                    Toast toast = Toast.makeText(this, R.string.ringtone_default_message,Toast.LENGTH_SHORT);
                    toast.show();
                } finally {
                    mediaPlayer.reset();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
            } else {
                mCanPlay=false;
               Toast toast = Toast.makeText(this, R.string.ringtone_default_message, Toast.LENGTH_SHORT);
                // SPRD: remove the setting of centrally located
                //toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        }

        }

    @Override
    protected void onStop() {
        super.onStop();
        stopAnyPlayingRingtone();
    }

    @Override
     protected void onPause() {
        super.onPause();
        stopAnyPlayingRingtone();
    }

    private void stopAnyPlayingRingtone() {

        if (mDefaultRingtone != null && mDefaultRingtone.isPlaying()) {
            mDefaultRingtone.stop();
        }
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }

        if (mRingtoneManager != null) {
            mRingtoneManager.stopPreviousRingtone();
        }


        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(mAudioFocusListener);
        }

    }

    private int getRingtoneManagerPosition(int listPos) {
        return listPos - mStaticItemCount;
    }

    private int getListPosition(int ringtoneManagerPos) {

        // If the manager position is -1 (for not found), return that
        if (ringtoneManagerPos < 0) return ringtoneManagerPos;

        return ringtoneManagerPos + mStaticItemCount;
    }

    protected void onDestroy() {
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }

        super.onDestroy();
    }
}
