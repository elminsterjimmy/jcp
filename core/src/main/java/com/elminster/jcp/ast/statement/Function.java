package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.Data;

public interface Function extends Block {

  Identifier getId();
  ParameterDef[] getParameterDefs();
  Data[] getArguments();
  void setArguments(Data... arguments);
  DataType getResultDataType();

  String getFullName();
}