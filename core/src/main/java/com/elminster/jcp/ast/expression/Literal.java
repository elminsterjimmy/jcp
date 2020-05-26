package com.elminster.jcp.ast;

public interface Literal<T> extends Node, Expression {

  T getValue();
}
