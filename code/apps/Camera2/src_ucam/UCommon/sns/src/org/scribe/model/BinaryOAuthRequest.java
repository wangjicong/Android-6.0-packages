/*
 * Copyright (C) 2011,2012 Thundersoft Corporation
 * All rights Reserved
 */
package org.scribe.model;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.scribe.exceptions.OAuthException;

import com.ucamera.ucomm.sns.Util;

public class BinaryOAuthRequest extends OAuthRequest {

    public BinaryOAuthRequest( String url) {
        super(Verb.POST, url);
    }

    private FileParameter mParamFile;
    public void setFileParameter(FileParameter file) {
        mParamFile = file;
        addHeader(CONTENT_TYPE, mParamFile.getMimeType());
    }

    @Override
    protected ByteArrayOutputStream createStreamBodyContent() {
        if (mParamFile == null) return null;
        InputStream is = null;
        try {
            is = mParamFile.open();
            final ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
            byte[] buf = new byte[1024];
            int count = -1;
            while( (count = is.read(buf)) != -1 ) {
                bos.write(buf, 0, count);
            }
            return bos;
        } catch (Exception e) {
            throw new OAuthException("fail build binary request body", e);
        } finally {
            Util.closeSilently(is);
        }
    }
}
