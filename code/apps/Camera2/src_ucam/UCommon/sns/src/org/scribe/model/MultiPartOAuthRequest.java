/**
 *   Copyright (C) 2010,2011 Thundersoft Corporation
 *   All rights Reserved
 */
package org.scribe.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import org.scribe.exceptions.OAuthException;

import com.ucamera.ucomm.sns.Util;

public class MultiPartOAuthRequest extends OAuthRequest {

    public static final String BOUNDARY = "3i2ndDfv2rTHiSisAbouNdArYfORhtTPEefj3q2f";
    public static final String MP_BOUNDARY = "--" + BOUNDARY;
    public static final String END_MP_BOUNDARY = "--" + BOUNDARY + "--";
    public static final String END_OF_LINE ="\r\n";

    public MultiPartOAuthRequest(String url) {
        super(Verb.POST, url);
    }

    private String        mParamName;
    private FileParameter mParamFile;

    @Override
    protected ByteArrayOutputStream createStreamBodyContent() {
        if (mParamFile == null) return null;
         try {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
            addParam(bos);
            addFile(bos, mParamFile);
            return bos;
        } catch (Exception e) {
            throw new OAuthException("fail build upload request body", e);
        }
    }

    public void addFileParameter(String name, FileParameter file) {
        mParamName = name;
        mParamFile     = file;
        if (mParamFile != null) {
            addHeader(CONTENT_TYPE, MULTIPART_FORM_DATA + "; boundary=" + BOUNDARY);
        }
    }

    private void addFile(final OutputStream out, FileParameter file) {
        StringBuilder temp = new StringBuilder();
        temp.append(MP_BOUNDARY).append(END_OF_LINE);
        temp.append("Content-Disposition: form-data; name=\"").append(mParamName)
            .append("\"; filename=\"").append(file.getName()).append("\"")
            .append(END_OF_LINE);

        String mime = file.getMimeType();
        if (mime == null || mime.trim().length() == 0) {
            mime = "content/unknown";
        }
        temp.append("Content-Type: ").append(mime)
            .append(END_OF_LINE).append(END_OF_LINE);

        InputStream is = null;
        try {
            out.write(temp.toString().getBytes(getCharset()));

            is = file.open();
            byte[] buf = new byte[1024];
            int count = -1;
            while( (count = is.read(buf)) != -1 ) {
                out.write(buf, 0, count);
            }
            out.write(END_OF_LINE.getBytes());
            out.write(END_MP_BOUNDARY.getBytes(getCharset()));
            out.write(END_OF_LINE.getBytes());
        } catch (IOException e) {
            throw new OAuthException("fail upload file.", e);
        } finally {
            Util.closeSilently(is);
        }
    }

    protected void addParam(OutputStream out) {
        for (Iterator<Parameter> it = getBodyParams().iterator(); it.hasNext();) {
            Parameter param = it.next();
            StringBuilder temp = new StringBuilder(10);
            temp.append(MP_BOUNDARY).append(END_OF_LINE);
            temp.append("content-disposition: form-data; name=\"").append(param.getKey()).append("\"")
                .append(END_OF_LINE).append(END_OF_LINE);
            temp.append(param.getValue()).append(END_OF_LINE);
            try {
                out.write(temp.toString().getBytes(getCharset()));
            } catch (IOException e) {
                throw new OAuthException("fail add params", e);
            }
        }
    }
}
