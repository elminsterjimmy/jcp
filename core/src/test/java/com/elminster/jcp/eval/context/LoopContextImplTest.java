package com.elminster.jcp.eval.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoopContextImplTest {

    private LoopContextImpl loopContext;

    @BeforeEach
    void setUp() {
        loopContext = new LoopContextImpl(null);
    }

    @Test
    void getLoopTime_InitiallyZero() {
        assertEquals(0, loopContext.getLoopTime());
    }

    @Test
    void increaseLoopTime_IncrementsCounter() {
        loopContext.increaseLoopTime();
        assertEquals(1, loopContext.getLoopTime());

        loopContext.increaseLoopTime();
        assertEquals(2, loopContext.getLoopTime());
    }

    @Test
    void clear_ResetsLoopTime() {
        loopContext.increaseLoopTime();
        loopContext.increaseLoopTime();
        assertEquals(2, loopContext.getLoopTime());

        loopContext.clear();
        assertEquals(0, loopContext.getLoopTime());
    }

    @Test
    void getParent_InitiallyNull() {
        assertNull(loopContext.getParent());
    }

    @Test
    void addToParent_SetsParent() {
        LoopContextImpl parent = new LoopContextImpl(null);
        loopContext.addToParent(parent);
        assertEquals(parent, loopContext.getParent());
    }

    @Test
    void getLoopStatement_ReturnsLoopStatement() {
        // Constructor was given null
        assertNull(loopContext.getLoopStatement());
    }

    @Test
    void setBreakBlock_SetsBreakFlag() {
        assertFalse(loopContext.isBreakBlock());

        loopContext.setBreakBlock(true);
        assertTrue(loopContext.isBreakBlock());

        loopContext.setBreakBlock(false);
        assertFalse(loopContext.isBreakBlock());
    }

    @Test
    void isBreakBlock_InitiallyFalse() {
        assertFalse(loopContext.isBreakBlock());
    }
}
