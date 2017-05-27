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
package com.android.messaging.datamodel.media;

import android.content.Context;
import android.graphics.*;
import com.sprd.messaging.drm.MessagingDrmSession;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.*;
//import java.io.InputStream;
import java.util.List;

/**
 * Serves local content URI based image requests.
 */
public class UriImageRequest<D extends UriImageRequestDescriptor> extends ImageRequest<D> {
    private static final String TAG = "UriImageRequest";
    private boolean mNotBitmapNeedCache;
    public UriImageRequest(final Context context, final D descriptor) {
        super(context, descriptor);
    }

    @Override
    protected boolean getCachePolicy(){
             return mNotBitmapNeedCache;
    }
 
    private Bitmap getGenericBitmap(){
             Bitmap bitmap = null;
             int resourceId = MessagingDrmSession.get().getIconImage(mDrmDataPath, mDrmContentType);
	      bitmap = BitmapFactory.decodeResource(mContext.getResources(), resourceId, null);
             Log.d(TAG, " bitmap is "+bitmap+" resourceId "+resourceId);
             mNotBitmapNeedCache = true;
             return bitmap;
    }

    @Override
    protected InputStream getInputStreamForResource() throws FileNotFoundException {
        if (isDrm && mDrmDataPath != null){
	       Log.d(TAG, "getInputStreamForResource is drm path "+mDrmDataPath);
		Bitmap bitmap = null;
		//if (MessagingDrmSession.get().getDrmFileRightsStatus(mDrmDataPath, mDrmContentType) == true){
		if (mDrmPreviewInPhotoViewActivity){
                 byte[] ret = MessagingDrmSession.get().decodeDrmByteArray(mDrmDataPath, mDecryptHandle);
                 if (ret != null){
                     mNotBitmapNeedCache = false;
		       return new ByteArrayInputStream(ret);
                 }
              }
		if (bitmap == null)bitmap = getGenericBitmap();
              ByteArrayOutputStream baos = new ByteArrayOutputStream();  
              bitmap.compress(Bitmap.CompressFormat.PNG, 50, baos);  
              return  new ByteArrayInputStream(baos.toByteArray());              
         }
        return mContext.getContentResolver().openInputStream(mDescriptor.uri);
    }

    @Override
    protected ImageResource loadMediaInternal(List<MediaRequest<ImageResource>> chainedTasks)
            throws IOException {
        final ImageResource resource = super.loadMediaInternal(chainedTasks);
        // Check if the caller asked for compression. If so, chain an encoding task if possible.
        if (mDescriptor.allowCompression && chainedTasks != null) {
            @SuppressWarnings("unchecked")
            final MediaRequest<ImageResource> chainedTask = (MediaRequest<ImageResource>)
                    resource.getMediaEncodingRequest(this);
            if (chainedTask != null) {
                chainedTasks.add(chainedTask);
                // Don't cache decoded image resource since we'll perform compression and cache
                // the compressed resource.
                if (resource instanceof DecodedImageResource) {
                    ((DecodedImageResource) resource).setCacheable(false);
                }
            }
        }
        return resource;
    }
}
