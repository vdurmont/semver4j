package com.vdurmont.semver4j;

public class SemverException extends RuntimeException {
    public SemverException(String msg) {
        super(msg);
    }

    public SemverException(String msg, Throwable t) {
        super(msg, t);
    }
}
