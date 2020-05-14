package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.data.FlowData;

public interface FunctionDeclaration extends Block, Declaration {

  FlowData[] getParameterDefines();
}
