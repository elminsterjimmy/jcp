package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.Identifier;

public interface Declaration extends Statement {

  Identifier getId();
  String getDataType();
}
