package com.elminster.jcp.collection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

class FastStackTest {

    @Test
    public void testReverseIterator() {
        FastStack<Integer> fastStack = new FastStack<>();
        for (int i = 0; i <= 10; i++) {
            fastStack.push(i);
        }

        Iterator<Integer> iterator = fastStack.reverseIterator();
        int expected = 10;
        while (iterator.hasNext()) {
            Integer next = iterator.next();
            Assertions.assertEquals(expected--, next.intValue());
        }
    }

}