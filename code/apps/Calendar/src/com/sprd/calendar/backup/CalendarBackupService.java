/* SPRD: for bug473564, add backup info @{ */
package com.sprd.calendar.backup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.Reader;
import java.io.Writer;
import java.io.OutputStream;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.content.Intent;
import android.os.Binder;
import android.app.Service;
import com.sprd.appbackup.service.Account;
import com.sprd.appbackup.service.IAppBackupAgent;
import com.sprd.appbackup.service.IAppBackupRepository;
import com.sprd.appbackup.service.IAppBackupRestoreObserver;
import com.sprd.appbackup.service.AbstractAppBackupAgent;
import android.content.Context;
import android.os.AsyncTask;
import com.sprd.calendar.vcalendar.VcalendarInfo;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.net.Uri;
import android.content.ContentValues;
import android.os.PowerManager;

public class CalendarBackupService extends Service {

    private static final String TAG = "CalendarBackupService";
    private static final String CALENDAR_FILE = "calendar.vcs";
    private static final String UNIQUE_CALENDAR_URI = "content://com.android.calendar/events/unique";
    private static final int MODE_BACKUP = 1;
    private static final int MODE_RESTORE = 2;
    public final static int FLAG_DUPLICATION_UNSUPPORT = 4;
    public final static int FLAG_DUPLICATION_SUCCESS = 5;

    private AgendaBackup mAgendaBackup;
    private AgendaRestore mAgendaRestore;
    private Context mContext;
    private CalendarBackupTask mCalendarBackupTask;

    private BroadcastReceiver mSdCardReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)
                    || intent.getAction().equals(Intent.ACTION_MEDIA_BAD_REMOVAL)
                    || intent.getAction().equals(Intent.ACTION_MEDIA_REMOVED)
                    || intent.getAction().equals(Intent.ACTION_MEDIA_EJECT)
                    || intent.getAction().equals(Intent.ACTION_MEDIA_SHARED)) {
                if (mCalendarBackupTask != null) {
                    mCalendarBackupTask.cancel(true);
                    mCalendarBackupTask = null;
                }
                if (mAgendaBackup != null) {
                    mAgendaBackup.cancel();
                    mAgendaBackup = null;
                }
                if (mAgendaRestore != null) {
                    mAgendaRestore.cancel();
                    mAgendaRestore = null;
                }
            }
        }

    };

    @Override
    public Binder onBind(Intent intent) {
        mContext = this;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addAction(Intent.ACTION_MEDIA_SHARED);

        filter.addDataScheme("file");
        registerReceiver(mSdCardReceiver, filter);
        Log.e(TAG, "calendar: onBind");
        return new AbstractAppBackupAgent() {
            public int onBackup(IAppBackupRepository repo, IAppBackupRestoreObserver observer) {
                Log.e(TAG, "onBackup");
                mCalendarBackupTask = new CalendarBackupTask(mContext, repo, observer, MODE_BACKUP, null);
                mCalendarBackupTask.execute();
                return 0;
            }

            public int onBackup(IAppBackupRepository repo, IAppBackupRestoreObserver observer,
                    int categoryCode, List<Account> accounts) {
                Log.e(TAG, "onBackup multi accounts !");
                mCalendarBackupTask = new CalendarBackupTask(mContext, repo, observer, MODE_BACKUP, accounts);
                mCalendarBackupTask.execute();
                return 0;
            }

            public int onRestore(IAppBackupRepository repo, IAppBackupRestoreObserver observer) {
                Log.e(TAG, "onRestore");
                mCalendarBackupTask = new CalendarBackupTask(mContext, repo, observer, MODE_RESTORE, null);
                mCalendarBackupTask.execute();
                return 0;
            }

            public int onDeduplicate(final IAppBackupRestoreObserver observer,int categoryCode) {
                Log.e(TAG, "onDeduplicate");
                new Thread(new Runnable() {
                    public void run() {
                        mContext.getContentResolver().update(Uri.parse(UNIQUE_CALENDAR_URI),
                                new ContentValues(), null, null);
                        try {
                            if (observer != null) {
                                observer.onResult(FLAG_DUPLICATION_SUCCESS);
                            }
                        } catch (Exception e) {
                            // TODO: handle exception
                        }
                    }
                }).start();
                return 0;

            }

            public int onCancel() {
                if (mCalendarBackupTask != null) {
                    mCalendarBackupTask.cancel(true);
                    mCalendarBackupTask = null;
                }
                if (mAgendaBackup != null) {
                    mAgendaBackup.cancel();
                    mAgendaBackup = null;
                }
                if (mAgendaRestore != null) {
                    mAgendaRestore.cancel();
                    mAgendaRestore = null;
                }
                return 0;
            }

            public boolean isEnabled() {
                Log.e(TAG, "getEnable");
                // can here backup? ture or false
                if (mAgendaBackup == null) {
                    mAgendaBackup = new AgendaBackup(mContext);
                }

                return mAgendaBackup.isEnabled(null);
            }

            public List<Account> getAccounts() {
                if(mAgendaBackup == null) {
                    mAgendaBackup = new AgendaBackup(mContext);
                }
                return mAgendaBackup.getAccounts();
            }

            public String getBackupInfo(IAppBackupRepository repo) {
                Log.e(TAG, "getBackupInfo");
                return "this  is a bckupinfo from calendar";
            }
        };
    }

    @Override
    public void onDestroy() {
        if (mSdCardReceiver != null) {
            unregisterReceiver(mSdCardReceiver);

        }
        super.onDestroy();
    }

    private void cancelUpdate(IAppBackupRestoreObserver observer, int result) {
        try {
            Log.i(TAG, "cancelUpdate() ! ");
            if (observer != null) {
                observer.onResult(result);
                observer.onUpdate(-1, -1);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }

    }
    class CalendarBackupTask extends AsyncTask<Void, Integer, Integer> {

        private Context tContext;
        private IAppBackupRepository tRepo;
        private IAppBackupRestoreObserver tObserver;
        private int tTaskMode;
        private AgendaBackup tAgendaBackup;
        private ArrayList<Account> tAccountList;
        private PowerManager.WakeLock tWakeLock;
        private PowerManager tPowerManager;

        public CalendarBackupTask(Context context, IAppBackupRepository repo,
                IAppBackupRestoreObserver observer, int taskMode, List<Account> accountList) {
            this.tContext = context;
            this.tRepo = repo;
            this.tObserver = observer;
            this.tTaskMode = taskMode;
            this.tAccountList = (ArrayList)accountList;
        }

        @Override
        protected void onPreExecute() {
            tPowerManager = (PowerManager) tContext.getSystemService(Context.POWER_SERVICE);
            tWakeLock = tPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK |
                    PowerManager.ON_AFTER_RELEASE, TAG);
        }

        @Override
        protected Integer doInBackground(Void... arg0) {
            // TODO Auto-generated method stub
            try {
                if (tWakeLock != null) {
                    tWakeLock.acquire();
                }
                if (tTaskMode == MODE_BACKUP) {
                    Writer writer = null;
                    int doneCount = 0;
                    ParcelFileDescriptor fd = null;
                    try {
                        if (tAgendaBackup == null) {
                            tAgendaBackup = new AgendaBackup(mContext);
                        }
                        String[] writeArrray = tAgendaBackup
                                .getCalendarWriteDataString(tAccountList);
                        if (writeArrray == null || writeArrray.length == 0) {
                            cancelUpdate(tObserver, -2);
                            Log.i(TAG, "MODE_BACKUP --- Have no data ! tObserver.onResult(-2)! ");
                            return -2;
                        }
                        fd = tRepo.write(CALENDAR_FILE);
                        if (fd == null) {
                            cancelUpdate(tObserver, -1);
                            Log.i(TAG, "MODE_BACKUP --- fd == null ! tObserver.onResult(-1)! ");
                            return -1;
                        }

                        if (tObserver != null) {
                            tObserver.onUpdate(doneCount, writeArrray.length);
                        }
                        OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(fd);
                        writer = new BufferedWriter(new OutputStreamWriter(out));
                        for (int i = 0; i < writeArrray.length; i++) {
                            if (isCancelled()) {
                                cancelUpdate(tObserver, -1);
                                Log.i(TAG, "MODE_BACKUP --- canceled ! tObserver.onResult(-1)! ");
                                return -1;
                            }
                            writer.write(writeArrray[i]);
                            doneCount++;
                            if (tObserver != null) {
                                tObserver.onUpdate(doneCount, writeArrray.length);
                            }
                        }

                        Log.e(TAG, "wrote calendar info to " + CALENDAR_FILE);
                    } catch (Exception e) {
                        cancelUpdate(tObserver, -1);
                        Log.i(TAG, "MODE_BACKUP --- Exception ! tObserver.onResult(-1)! ");
                        return -1;
                    } finally {
                        doneCount = 0;
                        try {
                            if (writer != null) {
                                writer.close();
                                writer = null;
                            }
                            if (fd != null) {
                                fd.close();
                                fd = null;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                } else if (tTaskMode == MODE_RESTORE) {
                    boolean isOldVersionFile = false;
                    try {
                        isOldVersionFile = tRepo.isOldVersionFile();
                    } catch (Exception e) {
                        cancelUpdate(tObserver, -1);
                        Log.i(TAG, "MODE_RESTORE --- Exception ! tRepo.isOldVersionFile()! ");
                        return -1;
                    }
                    if (isOldVersionFile) {
                        int doneCount = 0;
                        String oldVersionFilePath = null;
                        File oldVersionFileDir = null;
                        ArrayList<File> vcsFiles = new ArrayList<File>();
                        try {
                            oldVersionFilePath = tRepo.getOldVersionFilePath();
                            if (oldVersionFilePath != null) {
                                oldVersionFileDir = new File(oldVersionFilePath);
                            }
                            if (oldVersionFileDir != null
                                    && oldVersionFileDir.exists()
                                    && oldVersionFileDir.isDirectory()) {
                                File [] files = oldVersionFileDir.listFiles();
                                for (File file : files) {
                                    if (file.exists()
                                            && file.isFile()
                                            && file.length() > 0
                                            && file.getAbsolutePath().endsWith(".vcs")) {
                                        vcsFiles.add(file);
                                    }
                                }
                            }
                            if (vcsFiles.size() <= 0) {
                                cancelUpdate(tObserver, -1);
                                Log.i(TAG, "MODE_RESTORE --- Exception ! vcsFiles.size() <= 0! ");
                                return -1;
                            }
                            for (File file : vcsFiles) {
                                InputStream in = new FileInputStream(file);
                                if (mAgendaRestore == null) {
                                    mAgendaRestore = new AgendaRestore(mContext);
                                }
                                ArrayList<VcalendarInfo> calendarInfoList = mAgendaRestore
                                        .getCalendarInfoListFromInputStream(in);
                                in.close();
                                if (calendarInfoList == null) {
                                    cancelUpdate(tObserver, -1);
                                    Log.i(TAG,
                                            "MODE_RESTORE --- calendarInfoList == null ! tObserver.onResult(-1)! ");
                                    return -1;
                                } else if (calendarInfoList.size() <= 0) {
                                    cancelUpdate(tObserver, -1);
                                    Log.i(TAG, "MODE_RESTORE --- calendarInfoList.size() == "
                                            + calendarInfoList.size() + " ! tObserver.onResult(-1)! ");
                                    return -1;
                                }
                                if (tObserver != null) {
                                    tObserver.onUpdate(doneCount, calendarInfoList.size());
                                }
                                for (VcalendarInfo cInfo : calendarInfoList) {
                                    if (isCancelled()) {
                                        cancelUpdate(tObserver, -1);
                                        Log.i(TAG, "MODE_RESTORE --- canceled ! tObserver.onResult(-1)! ");
                                        return -1;
                                    }
                                    mAgendaRestore.restoreOneEvent(cInfo);
                                    doneCount++;
                                    if (tObserver != null) {
                                        tObserver.onUpdate(doneCount, calendarInfoList.size());
                                    }
                                }
                            }
                        } catch(Exception e) {
                            cancelUpdate(tObserver, -1);
                            Log.i(TAG, "MODE_RESTORE --- Exception ! tObserver.onResult(-1)! ");
                            return -1;
                        } finally {
                            doneCount = 0;
                        }
                    } else {
                        int doneCount = 0;
                        ParcelFileDescriptor fd = null;
                        try {
                            fd = tRepo.read(CALENDAR_FILE);
                            if (fd == null) {
                                // file does not exist
                                cancelUpdate(tObserver, -1);
                                Log.i(TAG, "MODE_RESTORE --- fd == null ! fd == null! ");
                                return -1;
                            }
                            InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(fd);

                            if (mAgendaRestore == null) {
                                mAgendaRestore = new AgendaRestore(mContext);
                            }
                            ArrayList<VcalendarInfo> calendarInfoList = mAgendaRestore
                                    .getCalendarInfoListFromInputStream(in);
                            if (calendarInfoList == null) {
                                cancelUpdate(tObserver, -1);
                                Log.i(TAG,
                                        "MODE_RESTORE --- calendarInfoList == null ! tObserver.onResult(-1)! ");
                                return -1;
                            } else if (calendarInfoList.size() <= 0) {
                                cancelUpdate(tObserver, -1);
                                Log.i(TAG, "MODE_RESTORE --- calendarInfoList.size() == "
                                        + calendarInfoList.size() + " ! tObserver.onResult(-1)! ");
                                return -1;
                            }
                            if (tObserver != null) {
                                tObserver.onUpdate(doneCount, calendarInfoList.size());
                            }
                            for (VcalendarInfo cInfo : calendarInfoList) {
                                if (isCancelled()) {
                                    cancelUpdate(tObserver, -1);
                                    Log.i(TAG, "MODE_RESTORE --- canceled ! tObserver.onResult(-1)! ");
                                    return -1;
                                }
                                mAgendaRestore.restoreOneEvent(cInfo);
                                doneCount++;
                                if (tObserver != null) {
                                    tObserver.onUpdate(doneCount, calendarInfoList.size());
                                }
                            }
                        } catch (Exception e) {
                            cancelUpdate(tObserver, -1);
                            Log.i(TAG, "MODE_RESTORE --- Exception ! tObserver.onResult(-1)! ");
                            return -1;
                        } finally {
                            doneCount = 0;
                            try {
                                if (fd != null) {
                                    fd.close();
                                    fd = null;
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } finally {
                if (tWakeLock != null) {
                    tWakeLock.release();
                }
            }

            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            // TODO Auto-generated method stub
            try {
                Log.i(TAG, "onPostExecute() = " + result);
                if (tObserver != null) {
                    tObserver.onResult(result);
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }
}
/* @} */