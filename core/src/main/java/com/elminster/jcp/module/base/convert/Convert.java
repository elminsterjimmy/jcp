package com.elminster.jcp.module.base.convert;

/**
 * Type conversion utilities for the JCP base module.
 *
 * <p>All methods are static and delegate directly to the corresponding {@link String},
 * {@link Integer}, {@link Double}, or {@link Boolean} static method. No locale-aware
 * parsing, no number bases. Bad numeric input propagates {@link NumberFormatException}
 * naturally.
 *
 * <p>Three distinct "to-string" methods are exposed instead of overloading on parameter
 * type because the JCP module-function resolver matches by type-compatibility (INT is
 * compatible with DOUBLE via numeric promotion), so overloads on {@code (int)} vs
 * {@code (double)} would be ambiguous to call from JCP.
 *
 * <p>Callable from JCP programs as:
 * <pre>
 *   Convert.intToString(42)
 *   Convert.toInt(s)
 *   base::Convert.toInt(s)  // explicit module form
 * </pre>
 *
 * @author jgu
 * @version 1.0
 */
public class Convert {

    private Convert() {}

    public static String intToString(int v) {
        return String.valueOf(v);
    }

    public static String doubleToString(double v) {
        return String.valueOf(v);
    }

    public static String booleanToString(boolean v) {
        return String.valueOf(v);
    }

    public static int toInt(String s) {
        return Integer.parseInt(s);
    }

    public static double toDouble(String s) {
        return Double.parseDouble(s);
    }

    public static boolean toBoolean(String s) {
        return Boolean.parseBoolean(s);
    }
}
