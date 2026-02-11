package com.elminster.jcp.module.base.vb;

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
}
