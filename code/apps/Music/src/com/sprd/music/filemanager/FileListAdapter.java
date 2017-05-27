package com.sprd.music.filemanager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.music.R;

public class FileListAdapter extends BaseAdapter {

    public static final String TAG = "MusicFileListAdapter";
    public boolean mCancelLoading = false;
    public volatile boolean mIsLoading = false;

    private Context mContext;
    private LayoutInflater mInflater;
    private List<FileInfo> mFileList;
    private HandlerThread mHandlerThread = new HandlerThread("MusicViewFilesThread");
    private Handler mHandler;
    private Handler mMainThreadHandler;
    private ExecuteFileRun mExecuteFileRun = null;
    private File mCurrentPath;
    private PopupRunnable mPopupRunnable = new PopupRunnable();

    private LoadingFileListener mLoadingFileListener;
    private OnTrackClickListener mOnTrackClickListener;
    private OnPtahChangeListener mOnPtahChangeListener;
    private static final String PATH_NO_NEED_TO_SHOW = "/storage/emulated/legacy";
    public interface OnTrackClickListener {
        public void onTrackClick(String path);
    }

    public interface OnPtahChangeListener {
        public void onPathChange(String path);
    }

    public interface LoadingFileListener {
        public void onLoadFileStart();

        public void onLoadFileFinished();
    }

    private Runnable mRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshInternal();
        }
    };

    private Runnable mClearListRunnable = new Runnable() {

        @Override
        public void run() {
            synchronized (mFileList) {
                mFileList.clear();
                notifyDataSetChanged();
            }
        }
    };

    public FileListAdapter(Context context, File path) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mFileList = new ArrayList<FileInfo>();
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mMainThreadHandler = new Handler(context.getApplicationContext().getMainLooper());
        mExecuteFileRun = new ExecuteFileRun();
        refreshWithPath(path);
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        if (mIsLoading) {
            notifyLoadingStart();
        } else {
            notifyLoadingFinish();
        }
    }

    public void notifyChangeOnly() {
        super.notifyDataSetChanged();
    }

    public void refreshWithPath(File path) {
        if (path == null) {
            throw new NullPointerException("topPath is null");
        }
        mCurrentPath = path;
        refresh();
    }

    private void refresh() {
        mHandler.post(mRefreshRunnable);
    }

    private void refreshInternal() {
        Log.d(TAG, "refreshInternal : " + mCurrentPath.getAbsolutePath());
        mIsLoading = true;
        notifyLoadingStart();
        mCancelLoading = false;
        mMainThreadHandler.post(new Runnable() {

            @Override
            public void run() {
                mOnPtahChangeListener.onPathChange(mCurrentPath.getAbsolutePath());
            }
        });
        File[] files = mCurrentPath.listFiles();
        if (files == null) {
            Log.d(TAG, "listFiles is null");
            mIsLoading = false;
            mMainThreadHandler.post(mClearListRunnable);
            return;
        }
        final ArrayList<FileInfo> tmpFiles = new ArrayList<FileInfo>();
        for (int i = 0; i < files.length; i++) {
            if (mCancelLoading) return;
            File file = files[i];
            if (FileUtil.isSupportType(mContext, file)) {
                FileInfo fileInfo = new FileInfo();
                fileInfo.Name = file.getName();
                fileInfo.mIsDirectory = file.isDirectory();
                fileInfo.mPath = file.getPath();
                fileInfo.Size = file.length();
                if(fileInfo.mPath.equals(PATH_NO_NEED_TO_SHOW)){
                    continue;
                }
                tmpFiles.add(fileInfo);
            }
        }
        if (mCancelLoading) {
            mIsLoading = false;
            Log.d(TAG, "CancelLoading in refreshInternal");
            return;
        }
        Collections.sort(tmpFiles, new FileComparator());
        Log.d(TAG, "loading finish, notify update listview");
        mIsLoading = false;
        synchronized (mFileList) {
            mMainThreadHandler.post(new Runnable() {

                @Override
                public void run() {
                    updateFileList(tmpFiles);
                }
            });
        }
        return;
    }

    private void updateFileList(List<FileInfo> list) {
        Log.d(TAG, "updateFileList with loading result");
        synchronized (mFileList) {
            mFileList.clear();
            for (int i = 0; i < list.size(); i++) {
                mFileList.add(list.get(i));
            }
        }
        notifyDataSetChanged();
    }

    public void execute(int position, int firstVisiblePosition) {
        mExecuteFileRun.execute(position, firstVisiblePosition);
    }

    public void popupFolder() {
        if (mCurrentPath == null) {
            Log.e(TAG, "currentpath == null");
            mCurrentPath = FileUtil.getRootPath();
            refresh();
        }
        mPopupRunnable.popupFolder();
    }

    public void destroyThread() {
        mHandlerThread.quit();
    }

    public void setLoadingFileListener(LoadingFileListener listener) {
        mLoadingFileListener = listener;
    }

    public void setOnTrackClickListener(OnTrackClickListener listener) {
        mOnTrackClickListener = listener;
    }

    public void setOnPathChangeListener(OnPtahChangeListener listener) {
        mOnPtahChangeListener = listener;
    }

    private Runnable mLoadStartRunnable = new Runnable() {
        @Override
        public void run() {
            if (mLoadingFileListener != null) {
                mLoadingFileListener.onLoadFileStart();
            }
        }
    };
    private Runnable mLoadFinishRunnable = new Runnable() {
        @Override
        public void run() {
            if (mLoadingFileListener != null) {
                mLoadingFileListener.onLoadFileFinished();
            }
        }
    };


    protected void notifyLoadingFinish() {
        if (!mCancelLoading) {
            mMainThreadHandler.post(mLoadFinishRunnable);
        }
    }


    protected void notifyLoadingStart() {
        mMainThreadHandler.post(mLoadStartRunnable);
    }

    public int getCount() {

        return mFileList.size();
    }

    public Object getItem(int position) {

        return mFileList.get(position);
    }

    public long getItemId(int position) {

        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder = null;

        if (convertView == null) {

            convertView = mInflater.inflate(R.layout.list_item, null);
            holder = new ViewHolder();
            holder.name = (TextView) convertView
                    .findViewById(R.id.list_item_name);
            holder.icon = (ImageView) convertView
                    .findViewById(R.id.list_item_icon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        FileInfo f = null;
        synchronized (mFileList) {
            f = mFileList.get(position);
        }
        holder.name.setText(f.Name);
        holder.icon.setImageResource(f.getIconResourceId());
        return convertView;
    }

    private class ViewHolder {
        TextView name;
        ImageView icon;
    }

    class ExecuteFileRun implements Runnable {

        private int mPosition;
        private int mFirstVisiblePosition;

        public void execute(int which, int firstVisiblePosition) {
            mPosition = which;
            mFirstVisiblePosition = firstVisiblePosition;
            mHandler.post(this);
        }

        @Override
        public void run() {
            Log.d(TAG, "ExecuteFileRun.run");
            FileInfo f = null;
            synchronized (mFileList) {
               if (mPosition < mFileList.size()) {
                    f = mFileList.get(mPosition);
                } else {
                    mMainThreadHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            notifyChangeOnly();
                        }
                    });
                    return;
                }
            }
            final FileInfo fileInfo = f;
            if (fileInfo.mIsDirectory) {
                mCurrentPath = new File(fileInfo.mPath);
                refreshInternal();
            } else {
                mMainThreadHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        mOnTrackClickListener.onTrackClick(fileInfo.mPath);
                    }
                });
            }
        }

    }

    class PopupRunnable implements Runnable {

        public void popupFolder() {
            mHandler.post(this);
        }

        @Override
        public void run() {
            mCurrentPath = mCurrentPath.getParentFile();
            if (mCurrentPath == null) {
                mCurrentPath = FileUtil.getRootPath();
            }
            Log.d(TAG, "popupFolder's path = " + mCurrentPath.getAbsolutePath());
            if (mCurrentPath.getAbsolutePath().equals("/mnt") ||
                    mCurrentPath.getAbsolutePath().equals("/")){
                mMainThreadHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        if(mContext instanceof FileManager) {
                            ((FileManager)mContext).finish();
                        }
                    }
                });
                return;
            }
            refreshInternal();
        }

    }
}
