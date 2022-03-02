package com.elminster.jcp.ast.statement.declaration;

import com.elminster.jcp.ast.Expression;

public interface VariableDeclaration extends Declaration {

  Expression getInit();
}
