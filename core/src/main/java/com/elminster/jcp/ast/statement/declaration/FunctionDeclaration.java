package com.elminster.jcp.ast.statement.declaration;

import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.function.ParameterDef;

public interface FunctionDeclaration extends Block, Declaration {

  ParameterDef[] getParameterDefines();
}
