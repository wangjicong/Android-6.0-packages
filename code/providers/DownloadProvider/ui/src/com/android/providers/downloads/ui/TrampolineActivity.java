/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.providers.downloads.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.FragmentManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.providers.downloads.Constants;
import com.android.providers.downloads.OpenHelper;

import libcore.io.IoUtils;
import android.content.ContentValues;
import android.provider.Downloads;
import com.android.providers.downloads.ui.uiplugin.DownloaduiDRMUtils;
/**
 * Intercept all download clicks to provide special behavior. For example,
 * PackageInstaller really wants raw file paths.
 */
public class TrampolineActivity extends Activity {
    private static final String TAG_PAUSED = "paused";
    private static final String TAG_FAILED = "failed";

    private static final String KEY_ID = "id";
    private static final String KEY_REASON = "reason";

    /**
     *Add for download pause&resume
     *@{
     */
    private static final String TAG_RESUME = "resume";
    private static final int TAG_PAUSED_BY_OWNER = 3;
    /*@}*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final long id = ContentUris.parseId(getIntent().getData());

        final DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        dm.setAccessAllDownloads(true);

        final int status;
        final int reason;
        /**
         *Add for download pause&resume
         *@{
         */
        int controller = 0;
        int readStatus = 0;
        /*@}*/
        final Cursor cursor = dm.query(new Query().setFilterById(id));
        try {
            if (cursor.moveToFirst()) {
                status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
            } else {
                Toast.makeText(this, R.string.dialog_file_missing_body, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }
        /**
         *Add for download pause&resume
         *@{
         */
        final Cursor cursor2 = getContentResolver().query(ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, id), null, null, null, null);
        try{
            if (cursor2.moveToFirst()) {
                controller = cursor2.getInt(cursor2.getColumnIndexOrThrow(Downloads.Impl.COLUMN_CONTROL));
                readStatus = cursor2.getInt(cursor2.getColumnIndexOrThrow(Downloads.Impl.COLUMN_STATUS));
            } else {
                Toast.makeText(this, R.string.dialog_file_missing_body, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } finally {
            IoUtils.closeQuietly(cursor2);
        }
        /*@}*/
        Log.d(Constants.TAG, "Found " + id + " with status " + status + ", reason " + reason + ", controller=" + controller + ",readStatus="+readStatus);
        switch (status) {
            case DownloadManager.STATUS_PENDING:
            case DownloadManager.STATUS_RUNNING:
                /**
                 *Add for download pause&resume
                 *Original Android code:
                 *    sendRunningDownloadClickedBroadcast(id);
                 *    finish();
                 *@{
                 */
                PausedDownloadFragment.show(getFragmentManager(), id);
                /*@}*/
                break;

            case DownloadManager.STATUS_PAUSED:
                if (reason == DownloadManager.PAUSED_QUEUED_FOR_WIFI) {
                    PausedDialogFragment.show(getFragmentManager(), id);
                } else {
                    /**
                     *Add for download pause&resume
                     *Original Android code:
                     *    sendRunningDownloadClickedBroadcast(id);
                     *    finish();
                     *@{
                     */
                    if(readStatus == Downloads.Impl.STATUS_PAUSED_BY_APP){
                        if(controller == TAG_PAUSED_BY_OWNER){
                            ResumeDownloadFragment.show(getFragmentManager(), id);
                        }else{
                            Toast.makeText(this, R.string.dialog_wait_and_retry, Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }else{
                        ResumeDownloadFragment.show(getFragmentManager(), id);
                    }
                    /*@}*/
                }
                break;

            case DownloadManager.STATUS_SUCCESSFUL:
                /*
                 * for downloadprovider_DRM
                 *@{
                 */
	        if (DownloaduiDRMUtils.getInstance(this).isDRMDownloadSuccess(id, this)) {
                    break;
                }
                /*@}*/
                if (!OpenHelper.startViewIntent(this, id, 0)) {
                    Toast.makeText(this, R.string.download_no_application_title, Toast.LENGTH_SHORT)
                            .show();
                }
                finish();
                break;

            case DownloadManager.STATUS_FAILED:
                FailedDialogFragment.show(getFragmentManager(), id, reason);
                break;
        }
    }

    private void sendRunningDownloadClickedBroadcast(long id) {
        final Intent intent = new Intent(Constants.ACTION_LIST);
        intent.setPackage(Constants.PROVIDER_PACKAGE_NAME);
        intent.putExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS, new long[] { id });
        sendBroadcast(intent);
    }

    public static class PausedDialogFragment extends DialogFragment {
        public static void show(FragmentManager fm, long id) {
            final PausedDialogFragment dialog = new PausedDialogFragment();
            final Bundle args = new Bundle();
            args.putLong(KEY_ID, id);
            dialog.setArguments(args);
            dialog.show(fm, TAG_PAUSED);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final DownloadManager dm = (DownloadManager) context.getSystemService(
                    Context.DOWNLOAD_SERVICE);
            dm.setAccessAllDownloads(true);

            final long id = getArguments().getLong(KEY_ID);

            final AlertDialog.Builder builder = new AlertDialog.Builder(
                    context, AlertDialog.THEME_HOLO_LIGHT);
            builder.setTitle(R.string.dialog_title_queued_body);
            builder.setMessage(R.string.dialog_queued_body);

            builder.setPositiveButton(R.string.keep_queued_download, null);

            builder.setNegativeButton(
                    R.string.remove_download, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dm.remove(id);
                        }
                    });

            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            if(null != getActivity()) {
                getActivity().finish();
            }
        }
    }

    public static class FailedDialogFragment extends DialogFragment {
        public static void show(FragmentManager fm, long id, int reason) {
            final FailedDialogFragment dialog = new FailedDialogFragment();
            final Bundle args = new Bundle();
            args.putLong(KEY_ID, id);
            args.putInt(KEY_REASON, reason);
            dialog.setArguments(args);
            dialog.show(fm, TAG_FAILED);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final DownloadManager dm = (DownloadManager) context.getSystemService(
                    Context.DOWNLOAD_SERVICE);
            dm.setAccessAllDownloads(true);

            final long id = getArguments().getLong(KEY_ID);
            final int reason = getArguments().getInt(KEY_REASON);

            final AlertDialog.Builder builder = new AlertDialog.Builder(
                    context, AlertDialog.THEME_HOLO_LIGHT);
            builder.setTitle(R.string.dialog_title_not_available);

            final String message;
            switch (reason) {
                case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                    builder.setMessage(R.string.dialog_file_already_exists);
                    break;
                case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                    builder.setMessage(R.string.dialog_insufficient_space_on_external);
                    break;
                case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                    builder.setMessage(R.string.dialog_media_not_found);
                    break;
                case DownloadManager.ERROR_CANNOT_RESUME:
                    builder.setMessage(R.string.dialog_cannot_resume);
                    break;
                default:
                    builder.setMessage(R.string.dialog_failed_body);
            }

            builder.setNegativeButton(
                    R.string.delete_download, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dm.remove(id);
                        }
                    });

            builder.setPositiveButton(
                    R.string.retry_download, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dm.restartDownload(id);
                        }
                    });

            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            if(null != getActivity()) {
                getActivity().finish();
            }
        }
    }

    /**
     *Add for download pause&resume
     *@{
     */
    public static class PausedDownloadFragment extends DialogFragment {
        public static void show(FragmentManager fm, long id) {
            final PausedDownloadFragment dialog = new PausedDownloadFragment();
            final Bundle args = new Bundle();
            args.putLong(KEY_ID, id);
            dialog.setArguments(args);
            dialog.show(fm, TAG_PAUSED);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final DownloadManager dm = (DownloadManager) context.getSystemService(
                    Context.DOWNLOAD_SERVICE);
            dm.setAccessAllDownloads(true);

            final long id = getArguments().getLong(KEY_ID);
            final Cursor cursorPause = dm.query(new Query().setFilterById(id));
            String title = "Pause";
            try {
                if (cursorPause.moveToFirst()) {
                    title = cursorPause.getString(cursorPause.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_TITLE));
                    Log.d(Constants.TAG, "pause cursor.moveToFirst() fileName = " + title);
                } else {
                    Log.d(Constants.TAG, "pause cursor.moveToFirst() failed");
                }
            } finally {
                IoUtils.closeQuietly(cursorPause);
            }

            final AlertDialog.Builder builder = new AlertDialog.Builder(
                    context, AlertDialog.THEME_HOLO_LIGHT);
            builder.setTitle(title);
            builder.setItems(R.array.download_pause_entries, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialoginterface, int i) {
                    int mStatus = 0;
                    final Cursor mQueryCursor = dm.query(new Query().setFilterById(id));
                    try {
                        if (mQueryCursor.moveToFirst()) {
                            mStatus = mQueryCursor.getInt(mQueryCursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                        } else {
                            Log.d(Constants.TAG, "cursor.moveToFirst() failed");
                        }
                    } finally {
                        IoUtils.closeQuietly(mQueryCursor);
                    }
                    if(DownloadManager.STATUS_RUNNING == mStatus){
                        Log.d(Constants.TAG, "pause to running clicked");
                        ContentValues values = new ContentValues();
                        values.put(Downloads.Impl.COLUMN_CONTROL, Downloads.Impl.CONTROL_PAUSED);
                        values.put(Downloads.Impl.COLUMN_STATUS, Downloads.Impl.STATUS_PAUSED_BY_APP);

                        Log.d(Constants.TAG, "pauseDownload " + id);
                        context.getContentResolver().update(
                                ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,id),
                                values, null, null);
                    }
                }
            });
            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            if(null != getActivity()) {
                getActivity().finish();
            }
        }
    }

    public static class ResumeDownloadFragment extends DialogFragment {
        public static void show(FragmentManager fm, long id) {
            final ResumeDownloadFragment dialog = new ResumeDownloadFragment();
            final Bundle args = new Bundle();
            args.putLong(KEY_ID, id);
            dialog.setArguments(args);
            dialog.show(fm, TAG_RESUME);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final DownloadManager dm = (DownloadManager) context.getSystemService(
                    Context.DOWNLOAD_SERVICE);
            dm.setAccessAllDownloads(true);
            final long id = getArguments().getLong(KEY_ID);
            final Cursor cursorPause = dm.query(new Query().setFilterById(id));
            String title = "Resume";
            try {
                if (cursorPause.moveToFirst()) {
                    title = cursorPause.getString(cursorPause.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_TITLE));
                    Log.d(Constants.TAG, "resume cursor.moveToFirst() fileName = " + title);
                } else {
                    Log.d(Constants.TAG, "resume cursor.moveToFirst() fail");
                }
            } finally {
                IoUtils.closeQuietly(cursorPause);
            }

            final AlertDialog.Builder builder = new AlertDialog.Builder(
                    context, AlertDialog.THEME_HOLO_LIGHT);
            builder.setTitle(title);
            builder.setItems(R.array.download_resum_entries, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialoginterface, int i) {
                    ContentValues values = new ContentValues();
                    values.put(Downloads.Impl.COLUMN_CONTROL, Downloads.Impl.CONTROL_RUN);
                    values.put(Downloads.Impl.COLUMN_STATUS, Downloads.Impl.STATUS_PENDING);
                    values.put(Downloads.Impl.COLUMN_NO_INTEGRITY, true);

                    Log.d(Constants.TAG, "resumeDownload " + id);
                    context.getContentResolver().update(
                        ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, id),
                        values, null, null);
                }
            });
            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            if(null != getActivity()) {
                getActivity().finish();
            }
        }
    }
    /*@}*/
}
