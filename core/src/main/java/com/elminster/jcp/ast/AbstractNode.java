package com.elminster.jcp.ast;

abstract public class AbstractNode implements Node {

  @Override
  public String toString() {
    return this.getName();
  }
}
