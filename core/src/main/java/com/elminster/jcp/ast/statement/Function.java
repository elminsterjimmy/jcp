package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.expression.Identifier;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.Data;

public interface Function extends Block {

  Identifier getId();
  ParameterDef[] getParameterDefs();
  Data[] getParameters();
  void setParameters(Data... parameters);
  DataType getResultDataType();
}