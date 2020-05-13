package com.elminster.jcp.ast.data;

public interface Adaptable<T> {

  boolean accept(Object from);

  T adapt(Object from);
}
