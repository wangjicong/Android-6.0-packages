
package com.sprd.gallery3d.drm;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.GalleryAppImpl;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.R;

import android.app.AddonManager;
import android.app.AlertDialog;
import android.content.Context;

public class SomePageUtils {

    static SomePageUtils sInstance;

    public static SomePageUtils getInstance() {
        if (sInstance != null)
            return sInstance;
        sInstance = (SomePageUtils) AddonManager.getDefault()
                .getAddon(R.string.feature_drm_somepage, SomePageUtils.class);
        return sInstance;
    }

    public boolean checkPressedIsDrm(AbstractGalleryActivity activity,
            MediaItem item, AlertDialog.OnClickListener listener, boolean getContent) {
        return false;
    }

    public boolean checkIsDrmFile(MediaSet targetSet) {
        return false;
    }
    public boolean canGetFromDrm(Context context, boolean getContentForSetAs, MediaItem item) {
        return false;
    }
}
