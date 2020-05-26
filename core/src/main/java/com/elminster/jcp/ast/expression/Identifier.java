package com.elminster.jcp.ast.expression;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;

public interface Identifier extends Node, Expression {

  String getId();
}
