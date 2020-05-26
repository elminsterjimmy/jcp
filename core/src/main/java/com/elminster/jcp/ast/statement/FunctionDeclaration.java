package com.elminster.jcp.ast.statement;

public interface FunctionDeclaration extends Block, Declaration {

  ParameterDef[] getParameterDefines();
}
