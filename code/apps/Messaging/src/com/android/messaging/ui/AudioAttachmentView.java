/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.messaging.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.datamodel.MediaScratchFileProvider;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.datamodel.media.PoolableImageCache.ReusableImageResourcePool;
import com.android.messaging.ui.mediapicker.PausableChronometer;
import com.android.messaging.util.Assert;
import com.android.messaging.util.ContentType;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.MediaUtil;
import com.android.messaging.util.UiUtils;
import com.android.messaging.util.AudioPlayerPolicy;

import android.util.Log;

import com.sprd.messaging.drm.MessagingDrmSession;

import android.widget.Toast;
import android.content.DialogInterface;
import android.app.AlertDialog;

import com.android.messaging.R;
import com.sprd.messaging.drm.MessagingDrmHelper;

import java.io.File;
import java.lang.SecurityException;
import com.android.messaging.util.OsUtil;

/**
 * A reusable widget that hosts an audio player for audio attachment playback.
 * This widget is used by both the media picker and the conversation message
 * view to show audio attachments.
 */
public class AudioAttachmentView extends LinearLayout implements
        AudioPlayerPolicy.AudioPlayerPolicyCallback {
    private static final String TAG = "AudioAttachmentView";

    /**
     * The normal layout mode where we have the play button, timer and progress
     * bar
     */
    private static final int LAYOUT_MODE_NORMAL = 0;

    /**
     * The compact layout mode with only the play button and the timer beneath
     * it. Suitable for displaying in limited space such as multi-attachment
     * layout
     */
    private static final int LAYOUT_MODE_COMPACT = 1;

    /** The sub-compact layout mode with only the play button. */
    private static final int LAYOUT_MODE_SUB_COMPACT = 2;

    private static final int PLAY_BUTTON = 0;
    private static final int PAUSE_BUTTON = 1;

    private AudioAttachmentPlayPauseButton mPlayPauseButton;
    private PausableChronometer mChronometer;
    private AudioPlaybackProgressBar mProgressBar;
    private MediaPlayer mMediaPlayer;

    private Uri mDataSourceUri;

    // The corner radius for drawing rounded corners. The default value is zero
    // (no rounded corners)
    private final int mCornerRadius;
    private final Path mRoundedCornerClipPath;
    private int mClipPathWidth;
    private int mClipPathHeight;

    private boolean mUseIncomingStyle;
    private int mThemeColor;

    private boolean mStartPlayAfterPrepare;
    // should the MediaPlayer be prepared lazily when the user chooses to play
    // the audio (as
    // opposed to preparing it early, on bind)
    private boolean mPrepareOnPlayback;
    private boolean mPrepared;
    private boolean mPlaybackFinished; // Was the audio played all the way to
                                       // the end
    private boolean isManPaused = false;
    private boolean isLosFocusPaused = false;
    // SPREAD: FIX FOR BUG 502893  START
    private boolean isLooping = false;
    // SPREAD: FIX FOR BUG 502893  END
    private final int mMode;

    private Context mContext;
    private MessagePartData mPart;
    private boolean mIsDrm;
    private boolean mIsFirstPlay = true;

    public AudioAttachmentView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        final TypedArray typedAttributes = context.obtainStyledAttributes(
                attrs, R.styleable.AudioAttachmentView);
        mMode = typedAttributes.getInt(
                R.styleable.AudioAttachmentView_layoutMode, LAYOUT_MODE_NORMAL);
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.audio_attachment_view, this, true);
        typedAttributes.recycle();

        setWillNotDraw(mMode != LAYOUT_MODE_SUB_COMPACT);
        mRoundedCornerClipPath = new Path();
        mCornerRadius = context.getResources().getDimensionPixelSize(
                R.dimen.conversation_list_image_preview_corner_radius);
        setContentDescription(context
                .getString(R.string.audio_attachment_content_description));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mPlayPauseButton = (AudioAttachmentPlayPauseButton) findViewById(R.id.play_pause_button);
        mChronometer = (PausableChronometer) findViewById(R.id.timer);
        mProgressBar = (AudioPlaybackProgressBar) findViewById(R.id.progress);
        mPlayPauseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                // Has the MediaPlayer already been prepared?
                if (mPart!=null) {
                    String contentType = mPart.getContentType();
                    boolean isDrm = ContentType.isDrmType(contentType);
                    Log.d(TAG, " content type is " + contentType + " is drm " + isDrm);
                    if (isDrm) {
                        //String path = mPart.getDrmDataPath();
                        Log.d(TAG, " content type is " + contentType + " path " + mPart.getDrmDataPath());
                        if (mPart.getDrmFileRightsStatus() == false) {
                            Factory.get().getUIIntents().launchDrmRightRequestActivity(mContext, mPart);
                            return;
                        }
                        Log.d(TAG, " mPlaybackFinished  " + mPlaybackFinished);
                        if (mIsFirstPlay || mPlaybackFinished) {
                            //mIsFirstPlay = false;
                            checkDrmRightsConsume(mPart.getDrmDataPath());
                            return;
                        }
                    }
                }
                if (mMediaPlayer != null && mPrepared) {
                    if (mMediaPlayer.isPlaying()) {
                        mMediaPlayer.pause();
                        mChronometer.pause();
                        mProgressBar.pause();
                        isManPaused = true;
                    } else {
                        isManPaused = false;
                        playAudio();
                    }
                } else {
                    // Either eager preparation is still going on (the user must
                    // have clicked
                    // the Play button immediately after the view is bound) or
                    // this is lazy
                    // preparation.
                    if (mStartPlayAfterPrepare) {
                        // The user is (starting and) pausing before the
                        // MediaPlayer is prepared
                        mStartPlayAfterPrepare = false;
                    } else {
                        mStartPlayAfterPrepare = true;
                        setupMediaPlayer();
                    }
                }
                updatePlayPauseButtonState();
            }
        });
        updatePlayPauseButtonState();
        initializeViewsForMode();
    }

    private void updateChronometerVisibility(final boolean playing) {
        if (mChronometer.getVisibility() == View.GONE) {
            // The chronometer is always GONE for LAYOUT_MODE_SUB_COMPACT
            Assert.equals(LAYOUT_MODE_SUB_COMPACT, mMode);
            return;
        }

        if (mPrepareOnPlayback) {
            // For lazy preparation, the chronometer will only be shown during
            // playback
            mChronometer.setVisibility(playing ? View.VISIBLE : View.INVISIBLE);
        } else {
            mChronometer.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Bind the audio attachment view with a MessagePartData.
     * 
     * @param incoming
     *            indicates whether the attachment view is to be styled as a
     *            part of an incoming message.
     */
    public void bindMessagePartData(final MessagePartData messagePartData,
            final boolean incoming, final boolean showAsSelected) {
        Assert.isTrue(messagePartData == null|| messagePartData.isAudio());
        final Uri contentUri = (messagePartData == null) ? null
                : messagePartData.getContentUri();
        mPart = messagePartData;
        mIsDrm = mPart.isDrmType();
        bind(contentUri, incoming, showAsSelected);
    }

    public void bind(final Uri dataSourceUri, final boolean incoming,
            final boolean showAsSelected) {
        final String currentUriString = (mDataSourceUri == null) ? ""
                : mDataSourceUri.toString();
        final String newUriString = (dataSourceUri == null) ? ""
                : dataSourceUri.toString();
        final int themeColor = ConversationDrawables.get()
                .getConversationThemeColor();
        final boolean useIncomingStyle = incoming || showAsSelected;
        final boolean visualStyleChanged = mThemeColor != themeColor
                || mUseIncomingStyle != useIncomingStyle;
        mUseIncomingStyle = useIncomingStyle;
        mThemeColor = themeColor;
        mPrepareOnPlayback = incoming
                && !MediaUtil.canAutoAccessIncomingMedia();
        if (!TextUtils.equals(currentUriString, newUriString)) {
            mDataSourceUri = dataSourceUri;
            resetToZeroState();
        }
        //spread : fix bug for 507270,507529 start
//        else if (visualStyleChanged) {
//            //updateVisualStyle();
//        }
        updateVisualStyle();
        //spread : fix bug for 507270,507529 end
    }

    private void playAudio() {
        Assert.notNull(mMediaPlayer);
        if (mPlaybackFinished) {
            mMediaPlayer.seekTo(0);
            mChronometer.restart();
            mProgressBar.restart();
            mPlaybackFinished = false;
        } else {
            mChronometer.resume();
            mProgressBar.resume();
        }
        AudioPlayerPolicy.get(mContext).startAudioPlayer(mMediaPlayer);
        mMediaPlayer.start();
    }

    private void onAudioReplayError(final int what, final int extra,
            final Exception exception) {
        if (exception == null) {
            LogUtil.e(LogUtil.BUGLE_TAG, "audio replay failed, what=" + what
                    + ", extra=" + extra);
        } else {
            LogUtil.e(LogUtil.BUGLE_TAG, "audio replay failed, exception="
                    + exception);
        }
        // SPREAD: FIX FOR BUG 502893  START
        if(!isLooping){
            if (mPart !=null && mPart.isDrmType()&&mPart.getDrmFileRightsStatus()){
                  UiUtils.showToastAtBottom(R.string.audio_recording_replay_failed);
            }else if(mPart !=null&&!mPart.isDrmType()){
               /*Modify by SPRD for Bug:543263 2016.03.29 Start*/
               if(!OsUtil.hasSmsPermission()){
                  UiUtils.showToastAtBottom(R.string.no_sms_permissions);
               }else{
                  UiUtils.showToastAtBottom(R.string.audio_recording_replay_failed);
               }
               /*Modify by SPRD for Bug:543263 2016.03.29 End*/
            }
            if (mPart == null){
                  UiUtils.showToastAtBottom(R.string.audio_recording_replay_failed);
            }
        }
        // SPREAD: FIX FOR BUG 502893  END
        releaseMediaPlayer();
    }

    /**
     * Prepare the MediaPlayer, and if mPrepareOnPlayback, start playing the
     * audio
     */
    private void setupMediaPlayer() {
        Assert.notNull(mDataSourceUri);
         if (mMediaPlayer == null) {
            Assert.isTrue(!mPrepared);
            mMediaPlayer = new MediaPlayer();
            AudioPlayerPolicy.get(mContext).registerAudioPlayer(mMediaPlayer,
                    this);
            try {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setDataSource(Factory.get()
                        .getApplicationContext(), mDataSourceUri);
                mMediaPlayer
                        .setOnCompletionListener(new OnCompletionListener() {
                            @Override
                            public void onCompletion(final MediaPlayer mp) {
                                updatePlayPauseButtonState();
                                mChronometer.reset();
                                mChronometer.setBase(SystemClock
                                        .elapsedRealtime()
                                        - mMediaPlayer.getDuration());
                                updateChronometerVisibility(false /* playing */);
                                mProgressBar.reset();
                                AudioPlayerPolicy.get(mContext)
                                        .stopAudioPlayer(mMediaPlayer);
                                mPlaybackFinished = true;
                                // SPREAD: FIX FOR BUG 502893  START
                                if(mp.isPlaying() || mp.isLooping()){
                                    Log.i(TAG, "mp.isPlaying() || mp.isLooping()");
                                    mp.stop();
                                    mp.seekTo(0);
                                    updateChronometerVisibility(true);
                                    if (mStartPlayAfterPrepare) {
                                        mPlayPauseButton.setDisplayedChild(PAUSE_BUTTON);
                                    } else {
                                        mPlayPauseButton.setDisplayedChild(PLAY_BUTTON);
                                    }
                                    isLooping = true;
                                }else{
                                    Log.i(TAG, "!mp.isPlaying() || !mp.isLooping()");
                                    isLooping = false;
                                }
	                         if (mIsDrm){
                                     releaseMediaPlayer();
                                }
                                // SPREAD: FIX FOR BUG 502893  END
                            }
                        });

                mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {
                    @Override
                    public void onPrepared(final MediaPlayer mp) {
                        // Set base on the chronometer so we can show the full
                        // length of the audio.
                        mChronometer.setBase(SystemClock.elapsedRealtime()
                                - mMediaPlayer.getDuration());
                        mProgressBar.setDuration(mMediaPlayer.getDuration());
                        mMediaPlayer.seekTo(0);
                        mPrepared = true;

                        if (mStartPlayAfterPrepare) {
                            mStartPlayAfterPrepare = false;
                            playAudio();
                            updatePlayPauseButtonState();
                        }
                    }
                });

                mMediaPlayer.setOnErrorListener(new OnErrorListener() {
                    @Override
                    public boolean onError(final MediaPlayer mp,
                            final int what, final int extra) {
                        mStartPlayAfterPrepare = false;
                        onAudioReplayError(what, extra, null);
                        return true;
                    }
                });

                AudioPlayerPolicy.get(mContext)
                        .prepareAudioPlayer(mMediaPlayer);
                mMediaPlayer.prepareAsync();
            } catch (final Exception exception) {
                onAudioReplayError(0, 0, exception);
                releaseMediaPlayer();
            }

       }
    }

    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            AudioPlayerPolicy.get(mContext).unregisterAudioPlayer(mMediaPlayer);
            mMediaPlayer.release();
            mMediaPlayer = null;
            mPrepared = false;
            mStartPlayAfterPrepare = false;
            //mPlaybackFinished = false;
            mPlaybackFinished = true;
            mChronometer.reset();
            mProgressBar.reset();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // The view must have scrolled off. Stop playback.
        releaseMediaPlayer();

        /* Modify by SPRD for Bug:511442 2016.01.29 Start */
        mIsDetached = true;
        /* Modify by SPRD for Bug:511442 2016.01.29 End */

    }

    /* Modify by SPRD for Bug:511442 2016.01.29 Start */
    /**
     * mIsDetached Avoiding multi-calling for "updatePlayPauseButtonState" when class initialization.
     */
    private boolean mIsDetached;
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(mIsDetached) {
            updatePlayPauseButtonState();
            mIsDetached = false;
        }
    }
    /* Modify by SPRD for Bug:511442 2016.01.29 End */

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        resetToZeroState();
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        if (mMode != LAYOUT_MODE_SUB_COMPACT) {
            return;
        }

        final int currentWidth = this.getWidth();
        final int currentHeight = this.getHeight();
        if (mClipPathWidth != currentWidth || mClipPathHeight != currentHeight) {
            final RectF rect = new RectF(0, 0, currentWidth, currentHeight);
            mRoundedCornerClipPath.reset();
            mRoundedCornerClipPath.addRoundRect(rect, mCornerRadius,
                    mCornerRadius, Path.Direction.CW);
            mClipPathWidth = currentWidth;
            mClipPathHeight = currentHeight;
        }

        canvas.clipPath(mRoundedCornerClipPath);
        super.onDraw(canvas);
    }

    private void updatePlayPauseButtonState() {
        final boolean playing = mMediaPlayer != null
                && mMediaPlayer.isPlaying();
        Log.i("jerry", "updatePlayPauseButtonState()");
        Log.i("jerry", "playing()-----------playing =["+playing+"]");
        updateChronometerVisibility(playing);
        if (mStartPlayAfterPrepare || playing) {
            mPlayPauseButton.setDisplayedChild(PAUSE_BUTTON);
        } else {
            mPlayPauseButton.setDisplayedChild(PLAY_BUTTON);
        }
    }

    private void resetToZeroState() {
        // Release the media player so it may be set up with the new audio
        // source.
        releaseMediaPlayer();
        updateVisualStyle();
        updateChronometerVisibility(false /* playing */);

        if (mDataSourceUri != null && !mPrepareOnPlayback && !mIsDrm) {
            // Prepare the media player, so we can read the duration of the
            // audio.
            /* Modify for Bug 532298 begin */
            if (isAudioFileExists(mContext, mDataSourceUri)) {
                setupMediaPlayer();
            }
            /* Modify for Bug 532298 end */
        }
    }

     /* Modify for Bug 532298 begin */
    private boolean isAudioFileExists(Context context, Uri uri) {
        if (MediaScratchFileProvider.isMediaScratchSpaceUri(uri)) {
            String inputFilePath = MediaScratchFileProvider.getFileFromUri(uri).getAbsolutePath();
            File inputFile = new File(inputFilePath);
            if (inputFile.exists()) {
                return true;
            }
        }
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Audio.Media.DATA}, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                if (cursor.moveToFirst()) {
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                    String fileString = cursor.getString(column_index);
                    if (fileString == null || "".equals(fileString.trim())) {
                        return false;
                    }
                    File filePath = new File(fileString);
                    LogUtil.d(TAG, " filePath = " + filePath);
                    /**
                     * We can not judge if a file of another proces exists,
                     * and the situation that it is not exist but we can query is a very low probability.
                     * so if we can get the path via query, we think it exist
                     */
                    if (filePath != null) {
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLiteException sqle) {
            LogUtil.e(TAG, sqle.toString());
        }/*Add by SPRD for Bug:543263 2016.03.29 Start*/
        catch (SecurityException se){
            Log.d(TAG,"AudioAttachmentView : has no permission and the exception is :"+se);
        }/*Add by SPRD for Bug:543263 2016.03.29 Start*/
        finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
        return false;
    }
    /* Modify for Bug 532298 end */

    private void updateVisualStyle() {
        if (mMode == LAYOUT_MODE_SUB_COMPACT) {
            // Sub-compact mode has static visual appearance already set up
            // during initialization.
            updatePlayPauseButtonState();
            return;
        }

        if (mUseIncomingStyle) {
            mChronometer.setTextColor(getResources().getColor(
                    R.color.message_text_color_incoming));
        } else {
            mChronometer.setTextColor(getResources().getColor(
                    R.color.message_text_color_outgoing));
        }
        mProgressBar.setVisualStyle(mUseIncomingStyle);
        mPlayPauseButton.setVisualStyle(mUseIncomingStyle);
        updatePlayPauseButtonState();
    }

    private void initializeViewsForMode() {
        switch (mMode) {
        case LAYOUT_MODE_NORMAL:
            setOrientation(HORIZONTAL);
            mProgressBar.setVisibility(VISIBLE);
            break;

        case LAYOUT_MODE_COMPACT:
            setOrientation(VERTICAL);
            mProgressBar.setVisibility(GONE);
            ((MarginLayoutParams) mPlayPauseButton.getLayoutParams())
                    .setMargins(0, 0, 0, 0);
            ((MarginLayoutParams) mChronometer.getLayoutParams()).setMargins(0,
                    0, 0, 0);
            break;

        case LAYOUT_MODE_SUB_COMPACT:
            setOrientation(VERTICAL);
            mProgressBar.setVisibility(GONE);
            mChronometer.setVisibility(GONE);
            ((MarginLayoutParams) mPlayPauseButton.getLayoutParams())
                    .setMargins(0, 0, 0, 0);
            final ImageView playButton = (ImageView) findViewById(R.id.play_button);
            playButton.setImageDrawable(getResources().getDrawable(
                    R.drawable.ic_preview_play));
            final ImageView pauseButton = (ImageView) findViewById(R.id.pause_button);
            pauseButton.setImageDrawable(getResources().getDrawable(
                    R.drawable.ic_preview_pause));
            break;

        default:
            Assert.fail("Unsupported mode for AudioAttachmentView!");
            break;
        }
    }

    /*
     * implements AudioPlayerPolicyCallback
     */
    public void reportResetAudioPlayer(MediaPlayer player) {
        if (player != mMediaPlayer) {
            Log.e(TAG, " report player " + player + " is not playing player "
                    + mMediaPlayer);
            return;
        }
        resetToZeroState();
    }

    /*
     * implements AudioPlayerPolicyCallback
     */
    public void reportReleaseAudioPlayer(MediaPlayer player) {
        return;
    }


    private OnAudioFocusChangeListener mOnAudioFocusChangeListener = new OnAudioFocusChangeListener(){

        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
               Log.i(TAG, "AudioManager.AUDIOFOCUS_GAIN");
               if(mMediaPlayer != null && mPrepared){
                   if(isLosFocusPaused && !isManPaused ){
                       isLosFocusPaused = false;
                       playAudio();
                   }
               }
               break;
            case AudioManager.AUDIOFOCUS_LOSS:
                Log.i(TAG, "AudioManager.AUDIOFOCUS_LOSS");
                resetToZeroState();
                releaseMediaPlayer();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Log.i(TAG, "AudioManager.AUDIOFOCUS_LOSS_TRANSIENT");
                if (mMediaPlayer != null && mPrepared) {
                    if (mMediaPlayer.isPlaying()) {
                        mMediaPlayer.pause();
                        mChronometer.pause();
                        mProgressBar.pause();
                        isLosFocusPaused = true;
                    }
                }

                break;
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                Log.i(TAG, "AudioManager.AUDIOFOCUS_GAIN_TRANSIENT");
                if(mMediaPlayer != null && !mMediaPlayer.isPlaying()){
                    mMediaPlayer.start();
                }

                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Log.i(TAG, "AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                if (mMediaPlayer != null && mPrepared) {
                    if (mMediaPlayer.isPlaying()) {
                        mMediaPlayer.pause();
                        mChronometer.pause();
                        mProgressBar.pause();
                        isLosFocusPaused = true;
                    }
                }
                break;
            }

        }

    };

    @Override
    public OnAudioFocusChangeListener getOnAudioFocusChangeListener(
           MediaPlayer player) {
        if(player != mMediaPlayer){
             Log.e(TAG, " getOnAudioFocusChangeListener " + player + " is not playing player "
                   + mMediaPlayer);
             return null;
        }
        return mOnAudioFocusChangeListener;
    }

    private void playDrmAudio(){
          if (mMediaPlayer != null && mPrepared) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                    mChronometer.pause();
                    mProgressBar.pause();
                    isManPaused = true;
                } else {
                    isManPaused = false;
                    playAudio();
                }
            } else {
                    // Either eager preparation is still going on (the user must
                    // have clicked
                    // the Play button immediately after the view is bound) or
                    // this is lazy
                    // preparation.
                if (mStartPlayAfterPrepare) {
                        // The user is (starting and) pausing before the
                        // MediaPlayer is prepared
                    mStartPlayAfterPrepare = false;
                } else {
                    mStartPlayAfterPrepare = true;
                    setupMediaPlayer();
                }
            }
            updatePlayPauseButtonState();

    }
    private void checkDrmRightsConsume(String path){
        AlertDialog.Builder builder = MessagingDrmSession.get().showProtectInfo(mContext, path, false/*is picture?*/);

        /* Modify by SPRD for Bug:524873  2015.01.21 Start */
//        builder.setTitle(mContext.getString(R.string.drm_consume_title))
//               .setMessage(mContext.getString(R.string.drm_consume_hint))
        builder.setPositiveButton(mContext.getString(R.string.ok_drm_rights_consume),
        /* Modify by SPRD for Bug:524873  2015.01.21 End */

                new AlertDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            mIsFirstPlay = false;
                            playDrmAudio();
                        } catch (Exception e) {
                            Toast.makeText(
                                    mContext,
                                    mContext.getString(R.string.drm_no_application_open),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(mContext.getString(R.string.cancel_drm_rights_consume), null).show();
   }
}
