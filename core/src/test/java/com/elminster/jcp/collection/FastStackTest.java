package com.elminster.jcp.collection;

import com.elminster.jcp.collection.exception.StackUnderflowException;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

class FastStackTest {

    @Test
    void testPushAndPop() {
        FastStack<Integer> stack = new FastStack<>();
        stack.push(1);
        stack.push(2);
        stack.push(3);

        assertEquals(3, stack.pop());
        assertEquals(2, stack.pop());
        assertEquals(1, stack.pop());
    }

    @Test
    void testPeek_ReturnsTopWithoutRemoving() {
        FastStack<String> stack = new FastStack<>();
        stack.push("first");
        stack.push("second");

        assertEquals("second", stack.peek());
        assertEquals("second", stack.peek()); // Still there
        assertEquals("second", stack.pop());
        assertEquals("first", stack.peek());
    }

    @Test
    void testPop_EmptyStack_ThrowsException() {
        FastStack<Integer> stack = new FastStack<>();
        assertThrows(StackUnderflowException.class, () -> stack.pop());
    }

    @Test
    void testPeek_EmptyStack_ThrowsException() {
        FastStack<Integer> stack = new FastStack<>();
        assertThrows(StackUnderflowException.class, () -> stack.peek());
    }

    @Test
    void testSearch_FindsElement() {
        FastStack<String> stack = new FastStack<>();
        stack.push("apple");
        stack.push("banana");
        stack.push("cherry");

        assertEquals("banana", stack.search("banana"));
    }

    @Test
    void testSearch_NullElement_ReturnsNull() {
        FastStack<String> stack = new FastStack<>();
        stack.push("apple");

        assertNull(stack.search(null));
    }

    @Test
    void testSearch_EmptyStack_ThrowsException() {
        FastStack<String> stack = new FastStack<>();
        assertThrows(StackUnderflowException.class, () -> stack.search("value"));
    }

    @Test
    void testReverseIterator() {
        FastStack<Integer> fastStack = new FastStack<>();
        for (int i = 0; i <= 10; i++) {
            fastStack.push(i);
        }

        Iterator<Integer> iterator = fastStack.reverseIterator();
        int expected = 10;
        while (iterator.hasNext()) {
            Integer next = iterator.next();
            assertEquals(expected--, next.intValue());
        }
    }

    @Test
    void testReverseIterator_EmptyStack_ThrowsException() {
        FastStack<Integer> stack = new FastStack<>();
        assertThrows(StackUnderflowException.class, () -> stack.reverseIterator());
    }
}