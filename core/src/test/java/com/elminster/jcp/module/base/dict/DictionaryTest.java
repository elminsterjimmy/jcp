package com.elminster.jcp.module.base.dict;

import com.elminster.jcp.module.base.vb.ValueBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DictionaryTest {

    @Test
    void testCopyConstructor() {
        Dictionary original = new Dictionary();
        original.put("a", 1);
        original.put("b", 2);

        Dictionary copy = new Dictionary(original);

        assertEquals(2, copy.size());
        assertEquals(1, copy.get("a"));
        assertEquals(2, copy.get("b"));

        original.put("c", 3);
        assertEquals(2, copy.size());
        assertFalse(copy.containsKey("c"));
    }

    @Test
    void testCopyConstructorEmpty() {
        Dictionary empty = new Dictionary();
        Dictionary copy = new Dictionary(empty);
        assertEquals(0, copy.size());
    }

    @Test
    void testValues() {
        Dictionary dict = new Dictionary();
        dict.put("name", "Alice");

        ValueBuffer vals = dict.values();
        assertArrayEquals(new String[]{"key", "value"}, vals.getHeader());
        assertEquals(1, vals.length());
        assertEquals("name", vals.getRow(0)[0]);
        assertEquals("Alice", vals.getRow(0)[1]);
    }

    @Test
    void testContentEqualsSameContent() {
        Dictionary a = new Dictionary();
        a.put("k", "v");
        Dictionary b = new Dictionary();
        b.put("k", "v");
        assertTrue(a.contentEquals(b));
    }

    @Test
    void testContentEqualsSameReference() {
        Dictionary a = new Dictionary();
        a.put("k", "v");
        assertTrue(a.contentEquals(a));
    }

    @Test
    void testContentEqualsNull() {
        Dictionary a = new Dictionary();
        assertFalse(a.contentEquals(null));
    }

    @Test
    void testContentEqualsDifferentValues() {
        Dictionary a = new Dictionary();
        a.put("k", "v1");
        Dictionary b = new Dictionary();
        b.put("k", "v2");
        assertFalse(a.contentEquals(b));
    }

    @Test
    void testContentEqualsDifferentKeys() {
        Dictionary a = new Dictionary();
        a.put("k1", "v");
        Dictionary b = new Dictionary();
        b.put("k2", "v");
        assertFalse(a.contentEquals(b));
    }

    @Test
    void testContentEqualsDifferentSize() {
        Dictionary a = new Dictionary();
        a.put("k1", "v1");
        Dictionary b = new Dictionary();
        b.put("k1", "v1");
        b.put("k2", "v2");
        assertFalse(a.contentEquals(b));
    }

    @Test
    void testContentEqualsNestedDictionary() {
        Dictionary inner1 = new Dictionary();
        inner1.put("x", 1);
        Dictionary outer1 = new Dictionary();
        outer1.put("nested", inner1);

        Dictionary inner2 = new Dictionary();
        inner2.put("x", 1);
        Dictionary outer2 = new Dictionary();
        outer2.put("nested", inner2);

        assertTrue(outer1.contentEquals(outer2));
    }

    @Test
    void testContentEqualsNestedDictionaryDifferent() {
        Dictionary inner1 = new Dictionary();
        inner1.put("x", 1);
        Dictionary outer1 = new Dictionary();
        outer1.put("nested", inner1);

        Dictionary inner2 = new Dictionary();
        inner2.put("x", 2);
        Dictionary outer2 = new Dictionary();
        outer2.put("nested", inner2);

        assertFalse(outer1.contentEquals(outer2));
    }

    @Test
    void testContentEqualsNestedValueBuffer() {
        ValueBuffer vb1 = new ValueBuffer();
        vb1.setHeader(new String[]{"col"});
        vb1.appendRow(new Object[]{10});
        Dictionary a = new Dictionary();
        a.put("data", vb1);

        ValueBuffer vb2 = new ValueBuffer();
        vb2.setHeader(new String[]{"col"});
        vb2.appendRow(new Object[]{10});
        Dictionary b = new Dictionary();
        b.put("data", vb2);

        assertTrue(a.contentEquals(b));
    }

    @Test
    void testContentEqualsNullValue() {
        Dictionary a = new Dictionary();
        a.put("k", null);
        Dictionary b = new Dictionary();
        b.put("k", null);
        assertTrue(a.contentEquals(b));
    }

    @Test
    void testContentEqualsNullVsNonNull() {
        Dictionary a = new Dictionary();
        a.put("k", null);
        Dictionary b = new Dictionary();
        b.put("k", "v");
        assertFalse(a.contentEquals(b));
    }
}
