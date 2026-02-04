package com.elminster.jcp.compile.exception;

/**
 * Exception thrown during compilation.
 */
public class CompileException extends RuntimeException {

    public CompileException(String message) {
        super(message);
    }

    public CompileException(String message, Throwable cause) {
        super(message, cause);
    }
}
