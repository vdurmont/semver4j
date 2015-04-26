package com.vdurmont.semver;

public class SemverException extends RuntimeException {
    public SemverException(String msg) {
        super(msg);
    }

    public SemverException(String msg, Throwable t) {
        super(msg, t);
    }
}
