package com.elminster.jcp.module.base.assertions;

import com.elminster.common.util.AssertException;

/**
 * The assertions utils.
 *
 * @author jgu
 * @version 1.0
 */
public class Assertions {

    public static void assertTrue(boolean condition) {
        if (!condition) {
            fastfail(true, false);
        }
    }

    public static void assertFalse(boolean condition) {
        if (condition) {
            fastfail(false, true);
        }
    }

    public static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            fastfail(expected, actual);
        }
    }

    public static void assertNull(Object value) {
        if (value != null) {
            fastfail(null, value);
        }
    }

    public static void assertNotNull(Object value) {
        if (value == null) {
            fastfail("non-null", null);
        }
    }

    private static void fastfail(boolean expected, boolean actual) {
        throw new AssertException(String.format("Assertions failed: expected [%s] but got [%s]",
                expected, actual));
    }

    private static void fastfail(Object expected, Object actual) {
        throw new AssertException(String.format("Assertions failed: expected [%s] but got [%s]",
                expected, actual));
    }
}
