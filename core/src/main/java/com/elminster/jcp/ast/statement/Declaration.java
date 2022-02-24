package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.eval.data.DataType;

public interface Declaration extends Statement {

  Identifier getId();
  DataType getDataType();
}
