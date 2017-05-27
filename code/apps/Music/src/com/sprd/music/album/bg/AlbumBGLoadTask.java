
package com.sprd.music.album.bg;

import com.android.music.MusicUtils;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.widget.ImageView;

public class AlbumBGLoadTask implements Runnable {

    Context mContext = null;
    Handler mHandler;
    long mArtIndex = -1;
    BitmapDrawable mDefaultBitmap = null;
    ImageView mIcon = null;

    public interface OnAlbumBGLoadCompleteListener {
        public void OnAlbumBGLoadComplete(long mArtIndex, Drawable drawable);
    }

    @SuppressWarnings("unused")
    private AlbumBGLoadTask() {

    }

    public AlbumBGLoadTask(Context context, Handler handler, long artIndx,
            BitmapDrawable defaultBitmap, ImageView iv) {
        this.mContext = context;
        this.mHandler = handler;
        this.mArtIndex = artIndx;
        this.mDefaultBitmap = defaultBitmap;
        this.mIcon = iv;
    }

    @Override
    public void run() {
        final Drawable d = MusicUtils.getCachedArtwork(mContext, mArtIndex, mDefaultBitmap);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mIcon != null) {
                    mIcon.setImageDrawable(d);
                }
            }
        });
    }

}
