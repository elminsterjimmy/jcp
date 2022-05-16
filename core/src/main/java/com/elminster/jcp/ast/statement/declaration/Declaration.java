package com.elminster.jcp.ast.statement.declaration;

import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.module.Modulable;

public interface Declaration extends Statement, Modulable {

  Identifier getId();
  DataType getDataType();
}
