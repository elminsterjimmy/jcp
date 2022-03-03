package com.elminster.jcp.collection;

import com.elminster.jcp.collection.exception.StackUnderflowException;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class FastStack<E> {

    private final LinkedList<E> list = new LinkedList<E>();

    public void push(E element) {
        list.addLast(element);
    }

    public E peek() {
        checkIfEmpty();
        return list.getLast();
    }

    public E pop() {
        checkIfEmpty();
        return list.removeLast();
    }

    public E search(E element) {
        checkIfEmpty();
        if (null == element) {
            return null;
        }
        return list.stream().filter(e -> e.equals(element)).findFirst().get();
    }

    private void checkIfEmpty() {
        if (list.isEmpty()) {
            throw new StackUnderflowException();
        }
    }

    public Iterator<E> reverseIterator() {
        checkIfEmpty();
        return new Iterator<E>() {
            ListIterator<E> eListIterator = list.listIterator(list.size());

            @Override
            public boolean hasNext() {
                return eListIterator.hasPrevious();
            }

            @Override
            public E next() {
                return eListIterator.previous();
            }
        };
    }
}
