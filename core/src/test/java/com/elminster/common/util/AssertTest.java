package com.elminster.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AssertTest {

    @Test
    void notNull_WithNonNullObject_DoesNotThrow() {
        assertDoesNotThrow(() -> Assert.notNull("test"));
    }

    @Test
    void notNull_WithNull_ThrowsAssertException() {
        AssertException ex = assertThrows(AssertException.class, () -> Assert.notNull(null));
        assertEquals("Argument must not be null", ex.getMessage());
    }

    @Test
    void notNull_WithNullAndCustomMessage_ThrowsWithMessage() {
        AssertException ex = assertThrows(AssertException.class, () -> Assert.notNull(null, "Custom message"));
        assertEquals("Custom message", ex.getMessage());
    }

    @Test
    void notNull_WithNonNullObjectAndMessage_DoesNotThrow() {
        assertDoesNotThrow(() -> Assert.notNull("test", "Custom message"));
    }
}
