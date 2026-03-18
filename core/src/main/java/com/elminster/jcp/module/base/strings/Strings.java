package com.elminster.jcp.module.base.strings;

/**
 * String utility functions for the JCP base module.
 *
 * <p>All methods are static and delegate directly to {@link String} instance methods
 * with DSL-friendly names. Null inputs throw {@link NullPointerException} naturally.
 *
 * <p>Callable from JCP programs as:
 * <pre>
 *   Strings.length(s)
 *   Strings.upper(s)
 *   base::Strings.upper(s)  // explicit module form
 * </pre>
 *
 * @author jgu
 * @version 1.0
 */
public class Strings {

    private Strings() {}

    public static int length(String s) {
        return s.length();
    }

    public static String sub(String s, int from, int to) {
        return s.substring(from, to);
    }

    public static String concat(String a, String b) {
        return a.concat(b);
    }

    public static int indexOf(String s, String t) {
        return s.indexOf(t);
    }

    public static boolean contains(String s, String t) {
        return s.contains(t);
    }

    public static String upper(String s) {
        return s.toUpperCase();
    }

    public static String lower(String s) {
        return s.toLowerCase();
    }

    public static String trim(String s) {
        return s.trim();
    }

    public static String replace(String s, String old, String newStr) {
        return s.replace(old, newStr);
    }

    public static boolean startsWith(String s, String prefix) {
        return s.startsWith(prefix);
    }

    public static boolean endsWith(String s, String suffix) {
        return s.endsWith(suffix);
    }

    public static boolean isEmpty(String s) {
        return s.isEmpty();
    }

    public static String[] split(String s, String delimiter) {
        return s.split(delimiter);
    }
}
