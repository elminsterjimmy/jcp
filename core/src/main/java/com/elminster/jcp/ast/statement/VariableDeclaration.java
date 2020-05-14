package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.Expression;

public interface VariableDeclaration extends Declaration {

  Expression getInit();
}
