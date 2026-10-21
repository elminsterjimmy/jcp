package com.elminster.jcp.eval.data;

/**
 * Declares that an implementor can be cast to a target type within the
 * JCP type system.
 */
public interface ICastable<T> {
    boolean isCastableTo(T target);
}
