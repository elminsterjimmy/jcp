package com.elminster.jcp.compile.exception;

import com.elminster.jcp.ast.SourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompileExceptionTest {

    @Test
    void constructor_WithMessage_SetsMessage() {
        CompileException ex = new CompileException("test error");
        assertEquals("test error", ex.getMessage());
        assertNull(ex.getLocation());
        assertNull(ex.getCause());
    }

    @Test
    void constructor_WithMessageAndCause_SetsMessageAndCause() {
        RuntimeException cause = new RuntimeException("root cause");
        CompileException ex = new CompileException("test error", cause);
        assertEquals("test error", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void constructor_WithMessageAndLocation_SetsMessageAndLocation() {
        SourceLocation location = SourceLocation.of("test.jcp", 10, 5);
        CompileException ex = new CompileException("test error", location);
        assertTrue(ex.getMessage().contains("test error"));
        assertEquals(location, ex.getLocation());
    }

    @Test
    void constructor_WithAllParams_SetsAll() {
        SourceLocation location = SourceLocation.of("test.jcp", 10, 5);
        RuntimeException cause = new RuntimeException("root cause");
        CompileException ex = new CompileException("test error", location, cause);
        assertTrue(ex.getMessage().contains("test error"));
        assertEquals(location, ex.getLocation());
        assertEquals(cause, ex.getCause());
    }
}
