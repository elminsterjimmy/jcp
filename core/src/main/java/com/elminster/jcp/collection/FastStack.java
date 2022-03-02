package com.elminster.jcp.collection;

import com.elminster.jcp.collection.exception.StackUnderflowException;

import java.util.LinkedList;
import java.util.List;

public class FastStack<E> {

    private final List<E> list = new LinkedList<E>();

    public void push(E element) {
        list.add(element);
    }

    public E peek() {
        checkIfEmpty();
        return list.get(0);
    }

    public E pop() {
        checkIfEmpty();
        return list.remove(0);
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
}
