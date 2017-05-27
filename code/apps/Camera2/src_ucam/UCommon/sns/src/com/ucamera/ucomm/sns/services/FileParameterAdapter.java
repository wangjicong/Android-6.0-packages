/**
 *   Copyright (C) 2010,2011,2013 Thundersoft Corporation
 *   All rights Reserved
 */
package com.ucamera.ucomm.sns.services;

import java.io.InputStream;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.FileParameter;

public class FileParameterAdapter implements FileParameter {
    private ShareFile mFile;

    public FileParameterAdapter(ShareFile file) {
        mFile = file;
    }

    @Override
    public InputStream open() {
        try {
            return mFile.open();
        }catch (Exception e) {
           throw new OAuthException("Fail open " + getName(), e);
        }
    }

    @Override
    public String getMimeType() {
        return mFile.getMimeType();
    }

    @Override
    public String getName() {
        return mFile.getName();
    }
}
