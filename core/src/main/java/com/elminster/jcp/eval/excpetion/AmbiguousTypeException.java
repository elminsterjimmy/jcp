package com.elminster.jcp.eval.excpetion;

/**
 * Thrown when a simple-name type lookup matches multiple registered fully-qualified types.
 * The caller should use the fully-qualified class name to disambiguate.
 */
public class AmbiguousTypeException extends RuntimeException {

    private final String simpleName;
    private final String candidates;

    public AmbiguousTypeException(String simpleName, String candidates) {
        super("Ambiguous type name '" + simpleName + "': multiple classes match: " + candidates
                + ". Use the fully-qualified class name to disambiguate.");
        this.simpleName = simpleName;
        this.candidates = candidates;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getCandidates() {
        return candidates;
    }
}
