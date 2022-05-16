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

    private static void fastfail(boolean expected, boolean actual) {
        throw new AssertException(String.format("Assertions failed: expected [%s] but got [%s]",
                expected, actual));
    }
}
