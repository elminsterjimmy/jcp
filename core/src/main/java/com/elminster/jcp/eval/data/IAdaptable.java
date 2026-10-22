package com.elminster.jcp.eval.data;

import java.util.Collections;
import java.util.Set;

/**
 * Declares that an implementor can adapt itself to satisfy a Java type.
 * JCP→Java direction: can a JCP value be used where a Java type is expected?
 */
public interface IAdaptable {

    default Set<Class<?>> getCompatibleJavaTypes() {
        return Collections.emptySet();
    }

    default boolean isAdaptableTo(Class<?> javaType) {
        if (javaType == Object.class) {
            return true;
        }
        for (Class<?> compatible : getCompatibleJavaTypes()) {
            if (javaType.isAssignableFrom(compatible)) {
                return true;
            }
        }
        return false;
    }
}
