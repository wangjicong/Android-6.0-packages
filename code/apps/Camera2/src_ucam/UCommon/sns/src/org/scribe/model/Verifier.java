/**
 *   Copyright (C) 2010,2011 Thundersoft Corporation
 *   All rights Reserved
 */
package org.scribe.model;

import org.scribe.utils.*;

/**
 * Represents an OAuth verifier code.
 *
 * @author Pablo Fernandez
 */
public class Verifier {

    private final String value;

    public Verifier(String value) {
        Preconditions.checkNotNull(value, "Must provide a valid string as verifier");
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
