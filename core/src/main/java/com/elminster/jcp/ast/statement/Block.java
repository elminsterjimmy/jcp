package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.Statement;

import java.util.List;

public interface Block extends Statement {

  List<Statement> getBody();

  Block addStatement(Statement statement);
}