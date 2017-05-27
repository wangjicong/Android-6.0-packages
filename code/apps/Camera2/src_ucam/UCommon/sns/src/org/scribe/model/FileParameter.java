/**
 *   Copyright (C) 2010,2011 Thundersoft Corporation
 *   All rights Reserved
 */
package org.scribe.model;

import java.io.InputStream;

public interface FileParameter {
    public String getMimeType();
    public String getName();
    public InputStream open();
}
