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
}
