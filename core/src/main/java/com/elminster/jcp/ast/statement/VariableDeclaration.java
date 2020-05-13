package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.data.DataType;
import com.elminster.jcp.ast.expression.Expression;

public interface VariableDeclaration extends Declaration {

  String getId();

  DataType getDataType();

  Expression getInit();
}
