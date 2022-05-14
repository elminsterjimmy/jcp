package com.elminster.jcp.eval.excpetion;

public class EvaluationException extends RuntimeException {

    public EvaluationException() {
        super();
    }

    public EvaluationException(String message) {
        super(message);
    }

    public EvaluationException(Throwable e) {
        super(e);
    }
}
