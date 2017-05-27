package com.sprd.music.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.content.Intent;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import android.provider.MediaStore;
import com.sprd.music.filemanager.FileManager;
import com.sprd.music.lrc.StringConstant;
import com.android.music.*;

import java.io.File;
import com.sprd.android.config.OptConfig;//Kalyy

public class SPRDMusicUtils {
    private static final String LOGTAG = "SPRDMusicUtils";
    private static AudioManager audioManager;

    /* SPRD 499633@{*/
    private static MusicApplication sMusicApplication = MusicApplication.getInstance();
    public static void quitservice(Activity context) {
        Intent stopIntent = new Intent().setClass(context, MediaPlaybackService.class);
        context.stopService(stopIntent);
        //context.finish();
        sMusicApplication.exit();
        if(OptConfig.SUN_BREATHINGLIGHT){//Kalyy Bug49383
            Intent breathLightIntent = new Intent("system.media.stop");
            context.sendBroadcast(breathLightIntent);
        }
        /* @} */
    }

    /* SPRD 476974 @{ */
    public static String getTrackFileName(String parth, ContentResolver resolver) {
        Cursor cursor = null;
        String trackFileName = null;
        String LRC_EXTENSION = ".lrc";

        String[] LYRIC_PREJECTION = {
                MediaStore.Audio.Media.DATA
        };
        try {
            Uri uri = Uri.parse(parth);
            cursor = resolver.query(uri,
                    LYRIC_PREJECTION, null, null,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                trackFileName = cursor.getString(0);
                int ind = trackFileName.lastIndexOf('.');
                trackFileName = trackFileName.substring(0, ind)
                        + LRC_EXTENSION;
                int isd = trackFileName.lastIndexOf('/');
                trackFileName = trackFileName.substring(isd + 1, ind);
            }
        } catch (Exception e) {
            MusicLog.e(LOGTAG, "GET TRACK FILE NAME ERROR:" + e.getMessage());
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    MusicLog.e(LOGTAG, "GET TRACK FILE NAME CLOSE ERROR:" + e.getMessage());
                }
            }
        }
        return trackFileName;
    }

    public static String getLrcPath(String parth, ContentResolver resolver) {
        String lrcPath = null;
        Cursor cursor = null;
        String LRC_EXTENSION = ".lrc";

        String[] LYRIC_PREJECTION = {
                MediaStore.Audio.Media.DATA
        };
        try {
            Uri uri = Uri.parse(parth);
            cursor = resolver.query(uri,
                    LYRIC_PREJECTION, null, null,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                lrcPath = cursor.getString(0);
                int ind = lrcPath.lastIndexOf('.');
                lrcPath = lrcPath.substring(0, ind)
                        + LRC_EXTENSION;
                File file = new File(lrcPath);
                if (!file.exists()) {
                    int isd = lrcPath.lastIndexOf('/');
                    String tractName = lrcPath.substring(isd + 1, ind);
                    return StringConstant.CURRENT_PATH.getAbsolutePath() + File.separator
                            + StringConstant.LRC_DIRECTORY + File.separator + tractName
                            + StringConstant.LYRIC_SUFFIX;
                } else {
                    return lrcPath;
                }
            }
        } catch (Exception e) {
            MusicLog.e(LOGTAG, "GET LRC PATH ERROR:" + e.getMessage());
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    MusicLog.e(LOGTAG, "GET LRC PATH CLOSE ERROR:" + e.getMessage());
                }
            }
        }
        return lrcPath;
    }
    /* @} */
    /* SPRD 476975 @{ */
    public static int getnuminserted(ContentResolver resolver, Uri uri, String[] cols, int base) {
        int numinserted = 0;
        Cursor afterInsert = resolver.query(uri, cols, null, null, null);
        try {
            if (afterInsert != null && afterInsert.moveToFirst()) {
                /* SPRD: Modify for bug 523319 update PlayOrder */
                numinserted = afterInsert.getCount() - base;
            }
        } catch (Exception e) {

        } finally {
            if (afterInsert != null) {
                afterInsert.close();
            }
        }
        return numinserted;
    }
    /* SPRD 476972 @{ */
    public static AlertDialog showDeleteItemDialog(final Context context, String message, final long[] itemList) {
        Builder builder = new AlertDialog.Builder(context);
        builder.setPositiveButton(R.string.delete_confirm_button_text, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // delete the selected item(s)
                AsyncTask<Void, Void, Void> task = null;
                if(itemList.length <= 1) {
                    MusicUtils.deleteTracks(context, itemList);
                    String message = context.getResources().getQuantityString(
                            R.plurals.NNNtracksdeleted, itemList.length, Integer.valueOf(itemList.length));
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        AlertDialog delDialog = builder.create();
        delDialog.setTitle(R.string.delete_item);
        delDialog.setMessage(message);
        delDialog.setCanceledOnTouchOutside(false);
        delDialog.show();
        return delDialog;
    }
    /* @} */
    public static int addToPlaylistNoToast(Context context, long[] ids, long playlistid) {
        int numinserted = 0;
        if (ids == null) {
            // this shouldn't happen (the menuitems shouldn't be visible
            // unless the selected item represents something playable
            MusicLog.e(LOGTAG, "ListSelection null");
        } else {
            int size = ids.length;
            ContentResolver resolver = context.getContentResolver();
            // need to determine the number of items currently in the playlist,
            // so the play_order field can be maintained.
            String[] cols = new String[] {
                    "count(*)"
            };
            Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistid);
            Cursor cur = resolver.query(uri, cols, null, null, null);
            if (cur != null) {
                cur.moveToFirst();
                int base = cur.getInt(0);
                cur.close();
                for (int i = 0; i < size; i += 1000) {
                    MusicUtils.makeInsertItems(ids, i, 1000, base);
                    numinserted += resolver
                            .bulkInsert(uri, MusicUtils.sContentValuesCache);
                }
                numinserted = getnuminserted(resolver, uri, cols, base);
            }
        }
        return numinserted;
    }
    /* @} */
    /*SPRD 494136 new feature add for multi-sim@{ */
    public static void doChoiceRingtone(final Context context, final long audioId) {
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
        Log.i(LOGTAG, "phoneCount =" + phoneCount);
        Log.i(LOGTAG, "isSimCardExist(context, 0)=" + isSimCardExist(context, 0));
        Log.i(LOGTAG, "isSimCardExist(context, 0)=" + isSimCardExist(context, 1));
        boolean sim1Exist = isSimCardExist(context, 0);
        boolean sim2Exist = isSimCardExist(context, 1);
        if (phoneCount == 2) {
            if (sim1Exist && sim2Exist) {
                AlertDialog.Builder ringtonebuilder = new AlertDialog.Builder(context);
                String[] items = {
                        context.getString(R.string.ringtone_title_sim1),
                        context.getString(R.string.ringtone_title_sim2)
                };
                ringtonebuilder.setItems(items, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                setRingtone(context, audioId, 0);
                                break;
                            case 1:
                                setRingtone(context, audioId, 1);
                                break;
                            default:
                                MusicLog.e(LOGTAG, "dialoginterface onclick  is null");
                                break;
                        }
                    }
                });
                ringtonebuilder.setTitle(R.string.ringtone_menu_short);
                ringtonebuilder.show();
            } else if (sim1Exist && !sim2Exist) {
                setRingtone(context, audioId, 0);
            } else if (!sim1Exist && sim2Exist) {
                setRingtone(context, audioId, 1);
            } else {
                Toast.makeText(context, R.string.please_insert_sim_card, 1000).show();
            }
        } else {
            if (sim1Exist) {
                setRingtone(context, audioId, -1);
            } else {
                Toast.makeText(context, R.string.please_insert_sim_card, 1000).show();
            }
        }
    }

    private static boolean isSimCardExist(Context context, int phoneID) {
        return TelephonyManager.getDefault().hasIccCard(phoneID);
    }

    public static void setRingtone(Context context, long id, final int simID) {
        final Context tmpContext = context;
        final long tmpId = id;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        AsyncTask<Void, Void, Integer> setRingTask = new AsyncTask<Void, Void, Integer>() {
            private static final int SET_SUCESS = 0;
            private static final int SET_FAIL_4_CANNOT_PLAY = 1;
            private static final int SET_FAIL_4_DB = 2;
            private String path = "";

            @Override
            protected Integer doInBackground(Void... params) {
                return setRingtoneInternal(simID, tmpContext, tmpId);
            }

            private Integer setRingtoneInternal(final int simID, final Context tmpContext,
                    final long tmpId) {
                ContentResolver resolver = tmpContext.getContentResolver();
                // Set the flag in the database to mark this as a ringtone
                Uri ringUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, tmpId);
                if (!isCanPlay(tmpContext, ringUri)) {
                    return SET_FAIL_4_CANNOT_PLAY;
                }
                try {
                    ContentValues values = new ContentValues(2);
                    values.put(MediaStore.Audio.Media.IS_RINGTONE, "1");
                    values.put(MediaStore.Audio.Media.IS_ALARM, "1");
                    resolver.update(ringUri, values, null, null);
                } catch (UnsupportedOperationException ex) {
                    // most likely the card just got unmounted
                    Log.e(LOGTAG, "couldn't set ringtone flag for id " + tmpId);
                    return SET_FAIL_4_DB;
                }

                String[] cols = new String[] {
                        MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.TITLE
                };

                String where = MediaStore.Audio.Media._ID + "=" + tmpId;
                Cursor cursor = MusicUtils.query(tmpContext,
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        cols, where, null, null);
                try {
                    if (cursor != null && cursor.getCount() == 1) {
                        // Set the system setting to make this the current
                        // ringtone
                        cursor.moveToFirst();
                        if (simID == -1) {
                            RingtoneManager.setActualDefaultRingtoneUri(tmpContext,
                                    RingtoneManager.TYPE_RINGTONE, ringUri,-1);
                        } else {
                              RingtoneManager.setActualDefaultRingtoneUri(
                             tmpContext, RingtoneManager.TYPE_RINGTONE,
                              ringUri, simID);

                        }
                        path = cursor.getString(2);
                        return SET_SUCESS;
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return SET_FAIL_4_DB;
            }

            private boolean isCanPlay(Context tmpContext, Uri ringUri) {
                MediaPlayer mp = new MediaPlayer();
                mp.reset();
                try {
                    mp.setDataSource(tmpContext, ringUri);
                    return true;
                } catch (Exception e) {
                    return false;
                } finally {
                    if (mp != null) {
                        mp.release();
                        mp = null;
                    }
                }
            }

            @Override
            protected void onPostExecute(Integer result) {
                super.onPostExecute(result);
                if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT
                        || audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
                    Toast.makeText(tmpContext, R.string.ring_set_silent_vibrate, 1000).show();
                } else if (result == SET_SUCESS) {
                    String message = null;
                    if (simID == -1) {
                        message = tmpContext.getString(R.string.ringtone_set, path);
                    } else if (simID == 0) {
                        message = tmpContext.getString(R.string.ringtone_set_sim1, path);
                    } else {
                        message = tmpContext.getString(R.string.ringtone_set_sim2, path);
                    }
                    Toast.makeText(tmpContext, message, Toast.LENGTH_SHORT).show();
                } else if (result == SET_FAIL_4_CANNOT_PLAY) {
                    Toast.makeText(tmpContext, R.string.ring_set_fail, 1000).show();
                }
            }
        };
        setRingTask.execute((Void[]) null);
    }
    /* @} */
    /* SPRD 541220 @{ */
    public static void doChoiceAddMusicDialog(final Context context, final String playlist) {
        /*AlertDialog.Builder addmusicbuilder = new AlertDialog.Builder(context);
        String[] items = {
                context.getString(R.string.from_library), context.getString(R.string.from_folder)
        };
        addmusicbuilder.setItems(items, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent;
                switch (which) {
                    case 0:
                        intent = new Intent();
                        intent.setClass(context, MultiTrackChoiceActivity.class);
                        intent.putExtra("playlist", playlist);
                        context.startActivity(intent);
                        break;

                    case 1:
                        intent = new Intent();
                        intent.setClass(context, FileManager.class);
                        intent.putExtra("playlist", playlist);
                        context.startActivity(intent);
                        break;

                    default:
                        MusicLog.e(LOGTAG, "dialoginterface onclick  is null");
                        break;
                }

            }
        });

        addmusicbuilder.setTitle(R.string.add_music);
        addmusicbuilder.show();*/
        Intent intent;
        intent = new Intent();
        intent.setClass(context, MultiTrackChoiceActivity.class);
        intent.putExtra("playlist", playlist);
        context.startActivity(intent);
    }
    /* @} */
}
