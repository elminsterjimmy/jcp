package com.elminster.jcp.module.base.arrays;

import java.lang.reflect.Array;
import java.util.Objects;

/**
 * Array utility functions for the JCP base module.
 *
 * <p>All methods are static. Typed overloads exist for {@code int[]}, {@code String[]},
 * {@code boolean[]}, {@code double[]}, and {@code Object[]}. The {@code Object[]} overloads
 * handle any array type not covered by a more specific overload.
 *
 * <p>Callable from JCP programs as:
 * <pre>
 *   Arrays.length(a)
 *   Arrays.slice(a, 1, 3)
 *   Arrays.contains(a, v)
 *   Arrays.sort(a)
 *   Arrays.sort(persons, SortKey.by("age"))
 *   Arrays.sort(persons, SortKey.by("lastName"), SortKey.by("firstName"))
 *   Arrays.reverse(a)
 *   base::Arrays.length(a)  // explicit module form
 * </pre>
 *
 * @author jgu
 * @version 1.0
 */
public class Arrays {

    private Arrays() {}

    // --- length ---

    /**
     * Returns the number of elements in the array.
     * Accepts any array type via {@code Object} parameter (registered as {@code ANY → INT}).
     */
    public static int length(Object array) {
        return Array.getLength(array);
    }

    // --- slice ---

    /**
     * Returns a sub-array from {@code from} (inclusive) to {@code to} (exclusive).
     * Throws {@code ArrayIndexOutOfBoundsException} if indices are out of bounds.
     */
    public static int[] slice(int[] a, int from, int to) {
        return java.util.Arrays.copyOfRange(a, from, to);
    }

    public static String[] slice(String[] a, int from, int to) {
        return java.util.Arrays.copyOfRange(a, from, to);
    }

    public static boolean[] slice(boolean[] a, int from, int to) {
        return java.util.Arrays.copyOfRange(a, from, to);
    }

    public static double[] slice(double[] a, int from, int to) {
        return java.util.Arrays.copyOfRange(a, from, to);
    }

    public static Object[] slice(Object[] a, int from, int to) {
        return java.util.Arrays.copyOfRange(a, from, to);
    }

    // --- contains ---

    /** Returns {@code true} if the array contains the given value. */
    public static boolean contains(int[] a, int v) {
        for (int elem : a) {
            if (elem == v) return true;
        }
        return false;
    }

    public static boolean contains(String[] a, String v) {
        for (String elem : a) {
            if (Objects.equals(v, elem)) return true;
        }
        return false;
    }

    public static boolean contains(boolean[] a, boolean v) {
        for (boolean elem : a) {
            if (elem == v) return true;
        }
        return false;
    }

    /** Uses exact {@code ==} equality — no epsilon comparison. */
    public static boolean contains(double[] a, double v) {
        for (double elem : a) {
            if (elem == v) return true;
        }
        return false;
    }

    public static boolean contains(Object[] a, Object v) {
        for (Object elem : a) {
            if (Objects.equals(v, elem)) return true;
        }
        return false;
    }

    // --- sort ---

    /**
     * Sorts the array in-place and returns it.
     * For {@code boolean[]}: {@code false} comes before {@code true}.
     */
    public static int[] sort(int[] a) {
        java.util.Arrays.sort(a);
        return a;
    }

    public static String[] sort(String[] a) {
        java.util.Arrays.sort(a);
        return a;
    }

    /** Sorts in-place with {@code false} before {@code true}. */
    public static boolean[] sort(boolean[] a) {
        int falseCount = 0;
        for (boolean b : a) {
            if (!b) falseCount++;
        }
        for (int i = 0; i < a.length; i++) {
            a[i] = i >= falseCount;
        }
        return a;
    }

    public static double[] sort(double[] a) {
        java.util.Arrays.sort(a);
        return a;
    }

    /**
     * Sorts the array in-place and returns it.
     *
     * <p>Elements must implement {@link Comparable}; throws {@link ClassCastException} otherwise.
     * For struct arrays, use the {@link SortKey}-based overloads instead.
     */
    public static Object[] sort(Object[] a) {
        java.util.Arrays.sort(a);
        return a;
    }

    /**
     * Sorts a struct array in-place by a single field and returns it.
     * Elements whose field is missing or non-navigable sort last.
     */
    public static Object[] sort(Object[] a, SortKey k1) {
        return sortByKeys(a, k1);
    }

    /**
     * Sorts a struct array in-place by two fields (primary, secondary) and returns it.
     * Elements whose field is missing or non-navigable sort last.
     */
    public static Object[] sort(Object[] a, SortKey k1, SortKey k2) {
        return sortByKeys(a, k1, k2);
    }

    /**
     * Sorts a struct array in-place by three fields and returns it.
     * Elements whose field is missing or non-navigable sort last.
     */
    public static Object[] sort(Object[] a, SortKey k1, SortKey k2, SortKey k3) {
        return sortByKeys(a, k1, k2, k3);
    }

    private static Object[] sortByKeys(Object[] a, SortKey... keys) {
        java.util.Comparator<Object> comparator = keys[0].toComparator();
        for (int i = 1; i < keys.length; i++) {
            comparator = comparator.thenComparing(keys[i].toComparator());
        }
        java.util.Arrays.sort(a, comparator);
        return a;
    }

    // --- reverse ---

    /**
     * Reverses the array in-place and returns it.
     */
    public static int[] reverse(int[] a) {
        for (int i = 0, j = a.length - 1; i < j; i++, j--) {
            int tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
        return a;
    }

    public static String[] reverse(String[] a) {
        for (int i = 0, j = a.length - 1; i < j; i++, j--) {
            String tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
        return a;
    }

    public static boolean[] reverse(boolean[] a) {
        for (int i = 0, j = a.length - 1; i < j; i++, j--) {
            boolean tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
        return a;
    }

    public static double[] reverse(double[] a) {
        for (int i = 0, j = a.length - 1; i < j; i++, j--) {
            double tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
        return a;
    }

    public static Object[] reverse(Object[] a) {
        for (int i = 0, j = a.length - 1; i < j; i++, j--) {
            Object tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
        return a;
    }
}
