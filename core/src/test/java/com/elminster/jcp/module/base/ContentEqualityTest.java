package com.elminster.jcp.module.base;

import com.elminster.jcp.module.base.dict.Dictionary;
import com.elminster.jcp.module.base.vb.ValueBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContentEqualityTest {

    @Test
    void elementEquals_bothNull() {
        assertTrue(ContentEquality.elementEquals(null, null));
    }

    @Test
    void elementEquals_firstNullSecondNonNull() {
        assertFalse(ContentEquality.elementEquals(null, "x"));
    }

    @Test
    void elementEquals_firstNonNullSecondNull() {
        assertFalse(ContentEquality.elementEquals("x", null));
    }

    @Test
    void elementEquals_sameReference() {
        String s = "hello";
        assertTrue(ContentEquality.elementEquals(s, s));
    }

    @Test
    void elementEquals_plainObjects() {
        assertTrue(ContentEquality.elementEquals(42, 42));
        assertFalse(ContentEquality.elementEquals(42, 99));
    }

    @Test
    void elementEquals_dictionaryVsNonDictionary() {
        Dictionary d = new Dictionary();
        d.put("k", "v");
        assertFalse(ContentEquality.elementEquals(d, "not a dict"));
    }

    @Test
    void elementEquals_valueBufferVsNonValueBuffer() {
        ValueBuffer vb = new ValueBuffer();
        vb.setHeader(new String[]{"col"});
        assertFalse(ContentEquality.elementEquals(vb, "not a vb"));
    }

    @Test
    void elementEquals_objectArraysSameContent() {
        Object[] a = {"x", 1, true};
        Object[] b = {"x", 1, true};
        assertTrue(ContentEquality.elementEquals(a, b));
    }

    @Test
    void elementEquals_objectArraysDifferentContent() {
        Object[] a = {"x", 1};
        Object[] b = {"x", 2};
        assertFalse(ContentEquality.elementEquals(a, b));
    }

    @Test
    void elementEquals_objectArraysDifferentLength() {
        Object[] a = {"x"};
        Object[] b = {"x", "y"};
        assertFalse(ContentEquality.elementEquals(a, b));
    }

    @Test
    void elementEquals_objectArrayVsNonArray() {
        Object[] a = {"x"};
        assertFalse(ContentEquality.elementEquals(a, "x"));
    }

    @Test
    void elementEquals_nestedObjectArrays() {
        Object[] inner1 = {1, 2};
        Object[] outer1 = {inner1, "a"};
        Object[] inner2 = {1, 2};
        Object[] outer2 = {inner2, "a"};
        assertTrue(ContentEquality.elementEquals(outer1, outer2));
    }
}
