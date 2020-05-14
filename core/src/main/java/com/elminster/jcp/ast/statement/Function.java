package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.data.DataType;
import com.elminster.jcp.ast.data.FlowData;

public interface Function extends Block {

  String getId();
  FlowData[] getParameterDefs();
  FlowData[] getParameters();
  void setParameters(FlowData... parameters);
  DataType getResultDataType();
}