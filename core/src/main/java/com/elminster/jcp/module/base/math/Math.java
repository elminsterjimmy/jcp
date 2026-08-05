package com.elminster.jcp.module.base.math;

/**
 * Numeric utilities for the JCP base module.
 *
 * <p>All methods are static and delegate directly to {@link java.lang.Math}. The
 * caller writes one clean name (e.g. {@code Math.abs(x)}); the typed Java overloads
 * live underneath and overload resolution selects the exact-type implementation —
 * C {@code <tgmath.h>} semantics.
 *
 * <p>Unlike {@link com.elminster.jcp.module.base.convert.Convert} — which uses
 * distinct method names because it predates the exact-match resolver — {@code Math}
 * relies on the resolver preferring an exact-type overload over a widening one. An
 * INT argument to {@code abs} selects {@code abs(int)} (returning int, no type loss);
 * a DOUBLE argument selects {@code abs(double)}. Methods that only make sense over
 * reals ({@code sqrt}, {@code pow}, {@code floor}, {@code ceil}) expose a single
 * {@code double} overload; INT arguments widen to double automatically.
 *
 * <p>Callable from JCP programs as:
 * <pre>
 *   Math.abs(-5)          // → 5   (int)
 *   Math.abs(-5.0)        // → 5.0 (double)
 *   Math.sqrt(9)          // → 3.0 (int widens to double)
 *   Math.min(1, 2.0)      // → 1.0 (mixed args promote to double,double)
 *   base::Math.max(3, 4)  // explicit module form
 * </pre>
 *
 * @author jgu
 * @version 1.0
 */
public class Math {

    private Math() {}

    public static int abs(int v) {
        return java.lang.Math.abs(v);
    }

    public static double abs(double v) {
        return java.lang.Math.abs(v);
    }

    public static double sqrt(double v) {
        return java.lang.Math.sqrt(v);
    }

    public static int min(int a, int b) {
        return java.lang.Math.min(a, b);
    }

    public static double min(double a, double b) {
        return java.lang.Math.min(a, b);
    }

    public static int max(int a, int b) {
        return java.lang.Math.max(a, b);
    }

    public static double max(double a, double b) {
        return java.lang.Math.max(a, b);
    }

    public static double pow(double a, double b) {
        return java.lang.Math.pow(a, b);
    }

    public static double floor(double v) {
        return java.lang.Math.floor(v);
    }

    public static double ceil(double v) {
        return java.lang.Math.ceil(v);
    }
}
