package com.elminster.jcp.eval.data;

public interface Adaptable<T> {

  boolean accept(Object from);

  T adapt(Object from);
}
