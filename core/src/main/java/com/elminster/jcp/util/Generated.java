package com.elminster.jcp.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks methods that are excluded from JaCoCo coverage analysis.
 * JaCoCo ignores any method annotated with an annotation whose
 * simple name includes "Generated".
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface Generated {
}
