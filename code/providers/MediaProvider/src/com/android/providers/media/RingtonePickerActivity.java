/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.providers.media;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import android.media.AudioManager;
import android.content.Context;
import android.media.AudioManager.OnAudioFocusChangeListener;

/**
 * The {@link RingtonePickerActivity} allows the user to choose one from all of the
 * available ringtones. The chosen ringtone's URI will be persisted as a string.
 *
 * @see RingtoneManager#ACTION_RINGTONE_PICKER
 */
public final class RingtonePickerActivity extends AlertActivity implements
        AdapterView.OnItemSelectedListener, Runnable, DialogInterface.OnClickListener,
        AlertController.AlertParams.OnPrepareListViewListener {

    private static final int POS_UNKNOWN = -1;

    private static final String TAG = "RingtonePickerActivity";

    private static final int DELAY_MS_SELECTION_PLAYED = 300;

    private static final String SAVE_CLICKED_POS = "clicked_pos";
    /// M: Request codes to MusicPicker for add more ringtone
    private static final int ADD_MORE_RINGTONES = 1;

    private RingtoneManager mRingtoneManager;
    //Give a init value for ringtone type.
    //private int mType;

    private Cursor mCursor;
    private Handler mHandler;

    /** The position in the list of the 'Silent' item. */
    private int mSilentPos = POS_UNKNOWN;

    /** The position in the list of the 'Default' item. */
    private int mDefaultRingtonePos = POS_UNKNOWN;

    /** The position in the list of the last clicked item. */
    private int mClickedPos = POS_UNKNOWN;

    /** The position in the list of the ringtone to sample. */
    private int mSampleRingtonePos = POS_UNKNOWN;

    /** Whether this list has the 'Silent' item. */
    private boolean mHasSilentItem;

    /** The Uri to place a checkmark next to. */
    private Uri mExistingUri;

    /** The number of static items in the list. */
    private int mStaticItemCount;

    /** Whether this list has the 'Default' item. */
    private boolean mHasDefaultItem;

    /** The Uri to play when the 'Default' item is clicked. */
    private Uri mUriForDefaultItem;

    /** M: Whether this list has the 'More Ringtongs' item. */
    private boolean mHasMoreRingtonesItem = false;

    /** M: The position in the list of the 'More Ringtongs' item. */
    private int mMoreRingtonesPos = POS_UNKNOWN;

    /** M: The ringtone type to show and add in the list. */
    private int mType = -1;

    /** M: Whether need to refresh listview after activity on resume. */
    private boolean mNeedRefreshOnResume = false;

    /**
     * A Ringtone for the default ringtone. In most cases, the RingtoneManager
     * will stop the previous ringtone. However, the RingtoneManager doesn't
     * manage the default ringtone for us, so we should stop this one manually.
     */
    private Ringtone mDefaultRingtone;

    /**
     * The ringtone that's currently playing, unless the currently playing one is the default
     * ringtone.
     */
    private Ringtone mCurrentRingtone;

    private int mAttributesFlags;

    /**
     * Keep the currently playing ringtone around when changing orientation, so that it
     * can be stopped later, after the activity is recreated.
     */
    private static Ringtone sPlayingRingtone;

    // SPRD: Add this to stop the music when ringtone and music sounds at the same time.
    AudioManager mAudioManager;
    // SPRD: add
    private Ringtone ringtone;

    private DialogInterface.OnClickListener mRingtoneClickListener =
            new DialogInterface.OnClickListener() {

        /*
         * On item clicked
         */
        public void onClick(DialogInterface dialog, int which) {
            // Save the position of most recently clicked item
            //mClickedPos = which;

            /// M: Show MusicPicker activity to let user choose song to be ringtone @{
            if (which == mMoreRingtonesPos) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("vnd.android.cursor.dir/audio");
                intent.setData(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, ADD_MORE_RINGTONES);
            /// @}
            } else {
                // Save the position of most recently clicked item
                mClickedPos = which;
                // Play clip
                playRingtone(which, 0);
                /// M: save the uri of current position
                mExistingUri = mRingtoneManager.getRingtoneUri(getRingtoneManagerPosition(which));
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();

        Intent intent = getIntent();

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
            mClickedPos = savedInstanceState.getInt(SAVE_CLICKED_POS, POS_UNKNOWN);
        }
        // Get whether to show the 'Silent' item
        mHasSilentItem = intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        // AudioAttributes flags
        mAttributesFlags |= intent.getIntExtra(
                RingtoneManager.EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS,
                0 /*defaultValue == no flags*/);

        /// M: Get whether to show the 'More Ringtones' item
        mHasMoreRingtonesItem = intent.getBooleanExtra(
                RingtoneManager.EXTRA_RINGTONE_SHOW_MORE_RINGTONES, false);

        // Give the Activity so it can do managed queries
        //mRingtoneManager = new RingtoneManager(this);
        Context context = this.getApplicationContext();
        mRingtoneManager = new RingtoneManager(context);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // Get the types of ringtones to show
        mType = intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, -1);
        if (mType != -1) {
            mRingtoneManager.setType(mType);
        }

        mCursor = mRingtoneManager.getCursor();

        // The volume keys will control the stream that we are choosing a ringtone for
        setVolumeControlStream(mRingtoneManager.inferStreamType());

        // Get the URI whose list item should have a checkmark
        mExistingUri = intent
                .getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI);
        Log.d(TAG,"onCreate mExistingUri="+mExistingUri);
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
    }

    /* SPRD :Add this to fix the StateException, refer bug110136 @{ */
    @Override
    public void onBackPressed() {
        try {
            super.onBackPressed();
        } catch (IllegalStateException e) {
            finish();
        }
    }
    /* @} */

    public void onPrepareListView(ListView listView) {

        if (mHasMoreRingtonesItem) {
            mMoreRingtonesPos = addMoreRingtonesItem(listView);
        }
        if (mHasDefaultItem) {
            mDefaultRingtonePos = addDefaultRingtoneItem(listView);

            if (mClickedPos == POS_UNKNOWN && RingtoneManager.isDefault(mExistingUri)) {
                mClickedPos = mDefaultRingtonePos;
            }
        }

        if (mHasSilentItem) {
            mSilentPos = addSilentItem(listView);

            // The 'Silent' item should use a null Uri
            if (mClickedPos == POS_UNKNOWN && mExistingUri == null) {
                mClickedPos = mSilentPos;
            }
        }

        if (mClickedPos == POS_UNKNOWN) {
            /// M: if the given uri not exist, show default ringtone.
            Log.d(TAG,"onPrepareListView mExistingUri="+mExistingUri);
            if (mExistingUri != null&&RingtoneManager.isRingtoneExist(getApplicationContext(), mExistingUri)) {
                mClickedPos = getListPosition(mRingtoneManager.getRingtonePosition(mExistingUri));
            } else {
                if (mHasDefaultItem) {
                    mClickedPos = mDefaultRingtonePos;
                } else {
                    mClickedPos = getListPosition(mRingtoneManager.getRingtonePosition(
                            RingtoneManager.getDefaultUri(mType)));
                }

            }
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
                com.android.internal.R.layout.select_dialog_singlechoice_material, listView, false);
        textView.setText(textResId);
        listView.addHeaderView(textView);
        mStaticItemCount++;
        return listView.getHeaderViewsCount() - 1;
    }

    private int addDefaultRingtoneItem(ListView listView) {
        if (mType == RingtoneManager.TYPE_NOTIFICATION) {
            return addStaticItem(listView, R.string.notification_sound_default);
        } else if (mType == RingtoneManager.TYPE_ALARM) {
            return addStaticItem(listView, R.string.alarm_sound_default);
        }

        return addStaticItem(listView, R.string.ringtone_default);
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
                uri = mRingtoneManager.getRingtoneUri(getRingtoneManagerPosition(mClickedPos));
            }
            /* SPRD: default alarm changed when user not selected any ring @{ */
            if (mClickedPos != -1) {
                resultIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, uri);
                setResult(RESULT_OK, resultIntent);
            } else {
                setResult(RESULT_CANCELED);
            }
            /* @} */
        } else {
            setResult(RESULT_CANCELED);
        }
        /* SPRD:Removed this to fix StaleDataExcepion in monkey test @{
        getWindow().getDecorView().post(new Runnable() {
            public void run() {
                mCursor.deactivate();
            }
        });
        @} */
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

    /* SPRD: Add @{ */
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
                    if(ringtone != null) {
                        ringtone.play();
                    }
                    break;

            }
        }
    };
    /* @} */

    public void run() {
        stopAnyPlayingRingtone();
        if (mSampleRingtonePos == mSilentPos) {
            return;
        }

        /* SPRD: Add this to stop the music when ringtone and music sounds at the same time @{ */
        boolean success = false;
        if (mAudioManager != null) {
            success = mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_RING,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            if (!success) {
                return;
            }
        }
        /* @} */

        if (mSampleRingtonePos == mSilentPos) {
            /* SPRD: Add this to stop the defaultringtone's playing  @{ */
            if (mDefaultRingtone != null && mDefaultRingtone.isPlaying()) {
                mDefaultRingtone.stop();
                mDefaultRingtone = null;
            } else {
                mRingtoneManager.stopPreviousRingtone();
            }
            /* @} */
            return;
        }

        if (mSampleRingtonePos == mDefaultRingtonePos) {
            if (mDefaultRingtone == null) {
                /* SPRD : Add this to fix StaleDataException @{ */
                if (isFinishing()) {
                    Log.d(TAG, "Activity is Finished");
                    return;
                }
                /* @} */
                mDefaultRingtone = RingtoneManager.getRingtone(this, mUriForDefaultItem);
            }
           /*
            * Stream type of mDefaultRingtone is not set explicitly here.
            * It should be set in accordance with mRingtoneManager of this Activity.
            */
            if (mDefaultRingtone != null) {
                mDefaultRingtone.setStreamType(mRingtoneManager.inferStreamType());
            }
            ringtone = mDefaultRingtone;
            mCurrentRingtone = null;
        } else {
            /* SPRD : Add this to fix StaleDataException @{ */
            if (isFinishing()) {
                Log.d(TAG, "activity is finished");
                return;
            }
            /* @} */
            ringtone = mRingtoneManager.getRingtone(getRingtoneManagerPosition(mSampleRingtonePos));
            mCurrentRingtone = ringtone;
        }

        if (ringtone != null) {
            if (mAttributesFlags != 0) {
                ringtone.setAudioAttributes(
                        new AudioAttributes.Builder(ringtone.getAudioAttributes())
                                .setFlags(mAttributesFlags)
                                .build());
            }
            ringtone.play();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(mAudioFocusListener);
        }
        if (!isChangingConfigurations()) {
            stopAnyPlayingRingtone();
        } else {
            saveAnyPlayingRingtone();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isChangingConfigurations()) {
            stopAnyPlayingRingtone();
        }
        mNeedRefreshOnResume = true;
    }

    private void saveAnyPlayingRingtone() {
        if (mDefaultRingtone != null && mDefaultRingtone.isPlaying()) {
            sPlayingRingtone = mDefaultRingtone;
        } else if (mCurrentRingtone != null && mCurrentRingtone.isPlaying()) {
            sPlayingRingtone = mCurrentRingtone;
        }
    }

    private void stopAnyPlayingRingtone() {
        if (sPlayingRingtone != null && sPlayingRingtone.isPlaying()) {
            sPlayingRingtone.stop();
        }
        sPlayingRingtone = null;

        if (mDefaultRingtone != null && mDefaultRingtone.isPlaying()) {
            mDefaultRingtone.stop();
        }

        /* SPRD: Add @{ */
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        /* @} */

        if (mRingtoneManager != null) {
            mRingtoneManager.stopPreviousRingtone();
        }
    }

    private int getRingtoneManagerPosition(int listPos) {
        return listPos - mStaticItemCount;
    }

    private int getListPosition(int ringtoneManagerPos) {
		Log.d(TAG,"getListPosition ringtoneManagerPos="+ringtoneManagerPos);

        // If the manager position is -1 (for not found), return that
        if (ringtoneManagerPos < 0) return ringtoneManagerPos;

        return ringtoneManagerPos + mStaticItemCount;
    }
    /// M: Add to restore user's choice after activity has been killed.
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "onRestoreInstanceState: savedInstanceState = " + savedInstanceState
                + ",mClickedPos = " + mClickedPos + ",this = " + this);
        super.onRestoreInstanceState(savedInstanceState);
        mClickedPos = savedInstanceState.getInt(SAVE_CLICKED_POS, mClickedPos);
    }

    /// M: Add to refresh activity because some new ringtones will insert to listview.
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume>>>: mNeedRefreshOnResume = " + mNeedRefreshOnResume);
        /// When activity first start, just return. Only when restart we need to refresh in resume.
        if (!mNeedRefreshOnResume) {
            return;
        }
        /*SUN:jicong.wang add for bug ringtone pick fatal start {@*/
        if (!mHasMoreRingtonesItem){
            return;
        }
        /*SUN:jicong.wang add for bug ringtone pick fatal end @]*/
        Log.d(TAG, "onResume>>>: mClickedPos = " + mClickedPos);
        ListView listView = mAlert.getListView();
        if (null == listView) {
            Log.e(TAG, "onResume: listview is null, return!");
            return;
        }
        /// Refresh the checked position after activity resume,
        /// because maybe there are new ringtone insert to listview.
        ListAdapter adapter = listView.getAdapter();
        ListAdapter headAdapter = adapter;
        if (null != headAdapter && (headAdapter instanceof HeaderViewListAdapter)) {
            /// Get the cursor adapter with the listview
            adapter = ((HeaderViewListAdapter) headAdapter).getWrappedAdapter();
            mCursor = mRingtoneManager.getNewCursor();
            ((SimpleCursorAdapter) adapter).changeCursor(mCursor);
            Log.d(TAG, "onResume: notify adapter update listview with new cursor!");
        } else {
            Log.e(TAG, "onResume: cursor adapter is null!");
        }
        /// Get position from ringtone list with this uri, if the return position is
        /// valid value, set it to be current clicked position
        Log.d(TAG,"onResume mExistingUri="+mExistingUri);
        if ((mClickedPos >= mStaticItemCount || (mHasSilentItem && mClickedPos == 1))
                && (null != mExistingUri)) {
            /// M: TODO avoid cursor out of bound, so move cursor position.
            if (null != mCursor && mCursor.moveToFirst()) {
                mClickedPos = getListPosition(mRingtoneManager.getRingtonePosition(mExistingUri));
                Log.d(TAG, "onResume: get the position of uri = " + mExistingUri
                        + ", position = " + mClickedPos);
                if (POS_UNKNOWN != mClickedPos) {
                    mAlertParams.mCheckedItem = mClickedPos;
                } else {
                    Log.w(TAG, "onResume: get position is invalid!");
                }
            }
        }

        /// If no ringtone has been checked, show default instead.
        if (POS_UNKNOWN == mClickedPos) {
            Log.w(TAG, "onResume: no ringtone checked, show default instead!");
            if (mHasDefaultItem) {
                mClickedPos = mDefaultRingtonePos;
            } else {
                if (null != mCursor && mCursor.moveToFirst()) {
                    mClickedPos = getListPosition(mRingtoneManager.getRingtonePosition(
                            RingtoneManager.getDefaultUri(mType)));
                }
            }
        }
        listView.setItemChecked(mClickedPos, true);
        listView.setSelection(mClickedPos);
        mNeedRefreshOnResume = false;
        Log.d(TAG, "onResume<<<: set position to be checked: mClickedPos = " + mClickedPos);
    }
    /* SPRD: Add @{ */
    public void onDestroy() {
        //further modification needed, because SortCursor don't rewrite method isClose()
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        super.onDestroy();
    }
    /* @} */
    /**
     * M: Add more ringtone item to given listview and return it's position.
     *
     * @param listView The listview which need to add more ringtone item.
     * @return The position of more ringtone item in listview
     */
    private int addMoreRingtonesItem(ListView listView) {
        TextView textView = (TextView) getLayoutInflater().inflate(
                com.android.internal.R.layout.simple_list_item_1, listView, false);
        textView.setText(R.string.Add_More_Ringtones);
        listView.addHeaderView(textView);
        mStaticItemCount++;
        return listView.getHeaderViewsCount() - 1;
    }

    /// M: Add to handle user choose a ringtone from MusicPicker
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case ADD_MORE_RINGTONES:
                if (resultCode == RESULT_OK) {
                    Uri uri = (null == intent ? null : intent.getData());
                    if (uri != null) {
                        setRingtone(this.getContentResolver(), uri);
                        Log.v(TAG, "onActivityResult: RESULT_OK, so set to be ringtone! "
                                + uri);
                    }
                } else {
                    Log.v(TAG, "onActivityResult: Cancel to choose more ringtones, "
                            + "so do nothing!");
                }
                break;
        }
    }

    /**
     * M: Set the given uri to be ringtone
     *
     * @param resolver content resolver
     * @param uri the given uri to set to be ringtones
     */
    private void setRingtone(ContentResolver resolver, Uri uri) {
        /// Set the flag in the database to mark this as a ringtone
        try {
            ContentValues values = new ContentValues(1);
            if (RingtoneManager.TYPE_RINGTONE == mType||mType==3){
                values.put(MediaStore.Audio.Media.IS_RINGTONE, "1");
            } else if (RingtoneManager.TYPE_ALARM == mType) {
                values.put(MediaStore.Audio.Media.IS_ALARM, "1");
            } else if (RingtoneManager.TYPE_NOTIFICATION == mType) {
                values.put(MediaStore.Audio.Media.IS_NOTIFICATION, "1");
            } else {
                Log.e(TAG, "Unsupport ringtone type =  " + mType);
                return;
            }
            resolver.update(uri, values, null, null);
            /// Restore the new uri and set it to be checked after resume
            mExistingUri = uri;
        } catch (UnsupportedOperationException ex) {
            /// most likely the card just got unmounted
            Log.e(TAG, "couldn't set ringtone flag for uri " + uri);
        }
    }
}
