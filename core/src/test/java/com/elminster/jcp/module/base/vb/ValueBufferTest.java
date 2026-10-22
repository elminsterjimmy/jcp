package com.elminster.jcp.module.base.vb;

import com.elminster.jcp.module.base.dict.Dictionary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValueBufferTest {

    private ValueBuffer buffer;

    @BeforeEach
    void setUp() {
        buffer = new ValueBuffer();
    }

    @Test
    void setAndGetHeader_SetsAndReturnsHeader() {
        String[] header = {"col1", "col2", "col3"};
        buffer.setHeader(header);
        assertArrayEquals(header, buffer.getHeader());
    }

    @Test
    void appendRow_AddsRowToBuffer() {
        Object[] row = {1, "test", true};
        buffer.appendRow(row);
        assertEquals(1, buffer.length());
        assertArrayEquals(row, buffer.getRow(0));
    }

    @Test
    void getRow_ReturnsRowAtIndex() {
        Object[] row1 = {1, "first"};
        Object[] row2 = {2, "second"};
        buffer.appendRow(row1);
        buffer.appendRow(row2);

        assertArrayEquals(row1, buffer.getRow(0));
        assertArrayEquals(row2, buffer.getRow(1));
    }

    @Test
    void setRow_ReplacesRowAtIndex() {
        Object[] row1 = {1, "first"};
        Object[] row2 = {2, "second"};
        Object[] newRow = {3, "replaced"};

        buffer.appendRow(row1);
        buffer.appendRow(row2);
        buffer.setRow(0, newRow);

        assertArrayEquals(newRow, buffer.getRow(0));
        assertArrayEquals(row2, buffer.getRow(1));
    }

    @Test
    void set_UpdatesCellValue() {
        Object[] row = {1, "test", true};
        buffer.appendRow(row);

        buffer.set(0, 1, "updated");

        assertEquals("updated", buffer.getRow(0)[1]);
    }

    @Test
    void removeRow_RemovesRowAtIndex() {
        Object[] row1 = {1, "first"};
        Object[] row2 = {2, "second"};

        buffer.appendRow(row1);
        buffer.appendRow(row2);
        assertEquals(2, buffer.length());

        buffer.removeRow(0);
        assertEquals(1, buffer.length());
        assertArrayEquals(row2, buffer.getRow(0));
    }

    @Test
    void length_ReturnsNumberOfRows() {
        assertEquals(0, buffer.length());

        buffer.appendRow(new Object[]{1});
        assertEquals(1, buffer.length());

        buffer.appendRow(new Object[]{2});
        assertEquals(2, buffer.length());
    }

    @Test
    void contentEquals_sameContent() {
        ValueBuffer a = new ValueBuffer();
        a.setHeader(new String[]{"col"});
        a.appendRow(new Object[]{"hello"});

        ValueBuffer b = new ValueBuffer();
        b.setHeader(new String[]{"col"});
        b.appendRow(new Object[]{"hello"});

        assertTrue(a.contentEquals(b));
    }

    @Test
    void contentEquals_sameReference() {
        buffer.setHeader(new String[]{"col"});
        buffer.appendRow(new Object[]{1});
        assertTrue(buffer.contentEquals(buffer));
    }

    @Test
    void contentEquals_null() {
        assertFalse(buffer.contentEquals(null));
    }

    @Test
    void contentEquals_differentHeader() {
        ValueBuffer a = new ValueBuffer();
        a.setHeader(new String[]{"col1"});
        a.appendRow(new Object[]{1});

        ValueBuffer b = new ValueBuffer();
        b.setHeader(new String[]{"col2"});
        b.appendRow(new Object[]{1});

        assertFalse(a.contentEquals(b));
    }

    @Test
    void contentEquals_differentRowCount() {
        ValueBuffer a = new ValueBuffer();
        a.setHeader(new String[]{"col"});
        a.appendRow(new Object[]{1});

        ValueBuffer b = new ValueBuffer();
        b.setHeader(new String[]{"col"});
        b.appendRow(new Object[]{1});
        b.appendRow(new Object[]{2});

        assertFalse(a.contentEquals(b));
    }

    @Test
    void contentEquals_differentCellValues() {
        ValueBuffer a = new ValueBuffer();
        a.setHeader(new String[]{"col"});
        a.appendRow(new Object[]{1});

        ValueBuffer b = new ValueBuffer();
        b.setHeader(new String[]{"col"});
        b.appendRow(new Object[]{2});

        assertFalse(a.contentEquals(b));
    }

    @Test
    void contentEquals_differentRowLength() {
        ValueBuffer a = new ValueBuffer();
        a.setHeader(new String[]{"col"});
        a.appendRow(new Object[]{1});

        ValueBuffer b = new ValueBuffer();
        b.setHeader(new String[]{"col"});
        b.appendRow(new Object[]{1, 2});

        assertFalse(a.contentEquals(b));
    }

    @Test
    void contentEquals_nestedDictionary() {
        Dictionary d1 = new Dictionary();
        d1.put("x", 1);
        ValueBuffer a = new ValueBuffer();
        a.setHeader(new String[]{"data"});
        a.appendRow(new Object[]{d1});

        Dictionary d2 = new Dictionary();
        d2.put("x", 1);
        ValueBuffer b = new ValueBuffer();
        b.setHeader(new String[]{"data"});
        b.appendRow(new Object[]{d2});

        assertTrue(a.contentEquals(b));
    }

    @Test
    void contentEquals_nestedValueBuffer() {
        ValueBuffer inner1 = new ValueBuffer();
        inner1.setHeader(new String[]{"v"});
        inner1.appendRow(new Object[]{42});
        ValueBuffer a = new ValueBuffer();
        a.setHeader(new String[]{"nested"});
        a.appendRow(new Object[]{inner1});

        ValueBuffer inner2 = new ValueBuffer();
        inner2.setHeader(new String[]{"v"});
        inner2.appendRow(new Object[]{42});
        ValueBuffer b = new ValueBuffer();
        b.setHeader(new String[]{"nested"});
        b.appendRow(new Object[]{inner2});

        assertTrue(a.contentEquals(b));
    }
}
