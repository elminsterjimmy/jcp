package com.elminster.jcp.module.base;

import com.elminster.jcp.module.base.dict.Dictionary;
import com.elminster.jcp.module.base.vb.ValueBuffer;

import java.util.Arrays;

/**
 * Recursive content equality for JCP collection types.
 * For Dictionary and ValueBuffer elements, delegates to their contentEquals.
 * For other Java objects, falls back to Object.equals() which may use
 * reference identity if the class does not override equals.
 */
public class ContentEquality {

    public static boolean elementEquals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a instanceof Dictionary && b instanceof Dictionary) {
            return ((Dictionary) a).contentEquals((Dictionary) b);
        }
        if (a instanceof ValueBuffer && b instanceof ValueBuffer) {
            return ((ValueBuffer) a).contentEquals((ValueBuffer) b);
        }
        if (a instanceof Object[] && b instanceof Object[]) {
            return arrayEquals((Object[]) a, (Object[]) b);
        }
        return a.equals(b);
    }

    private static boolean arrayEquals(Object[] a, Object[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!elementEquals(a[i], b[i])) {
                return false;
            }
        }
        return true;
    }
}
