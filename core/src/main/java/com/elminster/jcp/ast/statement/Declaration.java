package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.data.DataType;

public interface Declaration extends Statement {

  String getId();
  DataType getDataType();
}
