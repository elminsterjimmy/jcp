package com.elminster.jcp.module.base.arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Branch-coverage tests for {@link Arrays}.
 * Targets uncovered branches in contains(), sort(boolean[]), and reverse().
 */
class ArraysTest {

    // ── length ──────────────────────────────────────────────────────────────

    @Test
    void length_intArray() {
        assertEquals(3, Arrays.length(new int[]{1, 2, 3}));
    }

    @Test
    void length_emptyArray() {
        assertEquals(0, Arrays.length(new int[]{}));
    }

    // ── contains: found (true branch) and not-found (false branch) ──────────

    @Test
    void contains_int_found() {
        assertTrue(Arrays.contains(new int[]{1, 2, 3}, 2));
    }

    @Test
    void contains_int_notFound() {
        assertFalse(Arrays.contains(new int[]{1, 2, 3}, 5));
    }

    @Test
    void contains_string_found() {
        assertTrue(Arrays.contains(new String[]{"a", "b"}, "b"));
    }

    @Test
    void contains_string_notFound() {
        assertFalse(Arrays.contains(new String[]{"a", "b"}, "c"));
    }

    @Test
    void contains_string_nullValue_notFound() {
        assertFalse(Arrays.contains(new String[]{"a", "b"}, (String) null));
    }

    @Test
    void contains_boolean_trueFound() {
        assertTrue(Arrays.contains(new boolean[]{false, true}, true));
    }

    @Test
    void contains_boolean_notFound() {
        assertFalse(Arrays.contains(new boolean[]{false, false}, true));
    }

    @Test
    void contains_double_found() {
        assertTrue(Arrays.contains(new double[]{1.0, 2.0}, 2.0));
    }

    @Test
    void contains_double_notFound() {
        assertFalse(Arrays.contains(new double[]{1.0, 2.0}, 3.0));
    }

    @Test
    void contains_object_found() {
        assertTrue(Arrays.contains(new Object[]{"x", "y"}, "x"));
    }

    @Test
    void contains_object_notFound() {
        assertFalse(Arrays.contains(new Object[]{"x", "y"}, "z"));
    }

    // ── sort(boolean[]) — counts falses, then fills ──────────────────────────

    @Test
    void sort_booleanArray_allFalse() {
        boolean[] a = {false, false, false};
        boolean[] result = Arrays.sort(a);
        assertArrayEquals(new boolean[]{false, false, false}, result);
    }

    @Test
    void sort_booleanArray_allTrue() {
        boolean[] a = {true, true};
        boolean[] result = Arrays.sort(a);
        assertArrayEquals(new boolean[]{true, true}, result);
    }

    @Test
    void sort_booleanArray_mixed() {
        boolean[] a = {true, false, true, false};
        boolean[] result = Arrays.sort(a);
        assertArrayEquals(new boolean[]{false, false, true, true}, result);
    }

    @Test
    void sort_booleanArray_empty() {
        boolean[] a = {};
        boolean[] result = Arrays.sort(a);
        assertArrayEquals(new boolean[]{}, result);
    }

    // ── sort(int[]), sort(String[]), sort(double[]), sort(Object[]) ──────────

    @Test
    void sort_intArray() {
        int[] a = {3, 1, 2};
        assertArrayEquals(new int[]{1, 2, 3}, Arrays.sort(a));
    }

    @Test
    void sort_stringArray() {
        String[] a = {"banana", "apple"};
        assertArrayEquals(new String[]{"apple", "banana"}, Arrays.sort(a));
    }

    @Test
    void sort_doubleArray() {
        double[] a = {3.0, 1.0};
        assertArrayEquals(new double[]{1.0, 3.0}, Arrays.sort(a));
    }

    @Test
    void sort_objectArray() {
        Object[] a = {"b", "a"};
        assertArrayEquals(new Object[]{"a", "b"}, Arrays.sort(a));
    }

    // ── reverse ──────────────────────────────────────────────────────────────

    @Test
    void reverse_intArray_even() {
        int[] a = {1, 2, 3, 4};
        assertArrayEquals(new int[]{4, 3, 2, 1}, Arrays.reverse(a));
    }

    @Test
    void reverse_intArray_odd() {
        int[] a = {1, 2, 3};
        assertArrayEquals(new int[]{3, 2, 1}, Arrays.reverse(a));
    }

    @Test
    void reverse_intArray_singleElement() {
        int[] a = {42};
        assertArrayEquals(new int[]{42}, Arrays.reverse(a));
    }

    @Test
    void reverse_intArray_empty() {
        int[] a = {};
        assertArrayEquals(new int[]{}, Arrays.reverse(a));
    }

    @Test
    void reverse_stringArray() {
        String[] a = {"a", "b", "c"};
        assertArrayEquals(new String[]{"c", "b", "a"}, Arrays.reverse(a));
    }

    @Test
    void reverse_booleanArray() {
        boolean[] a = {true, false, true};
        assertArrayEquals(new boolean[]{true, false, true}, Arrays.reverse(a));
    }

    @Test
    void reverse_doubleArray() {
        double[] a = {1.0, 2.0, 3.0};
        assertArrayEquals(new double[]{3.0, 2.0, 1.0}, Arrays.reverse(a));
    }

    @Test
    void reverse_objectArray() {
        Object[] a = {"x", "y"};
        assertArrayEquals(new Object[]{"y", "x"}, Arrays.reverse(a));
    }

    // ── slice ────────────────────────────────────────────────────────────────

    @Test
    void slice_intArray() {
        assertArrayEquals(new int[]{2, 3}, Arrays.slice(new int[]{1, 2, 3, 4}, 1, 3));
    }

    @Test
    void slice_stringArray() {
        assertArrayEquals(new String[]{"b"}, Arrays.slice(new String[]{"a", "b", "c"}, 1, 2));
    }

    @Test
    void slice_booleanArray() {
        assertArrayEquals(new boolean[]{true}, Arrays.slice(new boolean[]{false, true, false}, 1, 2));
    }

    @Test
    void slice_doubleArray() {
        assertArrayEquals(new double[]{2.0, 3.0}, Arrays.slice(new double[]{1.0, 2.0, 3.0}, 1, 3));
    }

    @Test
    void slice_objectArray() {
        assertArrayEquals(new Object[]{"b", "c"}, Arrays.slice(new Object[]{"a", "b", "c"}, 1, 3));
    }
}
