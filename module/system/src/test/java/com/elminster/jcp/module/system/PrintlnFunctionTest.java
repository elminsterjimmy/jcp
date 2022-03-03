package com.elminster.jcp.module.system;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.expression.literal.Literal;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.function.Function;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.util.ModuleLoader;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class PrintlnFunctionTest {

  @BeforeClass
  public static void init() {
    ModuleLoader.INSTANCE.loadModule(SystemModuleFunction.SYSTEM_MODULE_NAME);
  }

  @Test
  public void testPrintlnFunction() {
    FunctionCallExpression funcall = new FunctionCallExpression(Identifier.fromName("print"),
        new Expression[] {new LiteralExpression(StringLiteral.of("TEST"))});
    Statement statement = new ExpressionStatement(funcall);

    EvalContext context = new RootEvalContext();
    ModuleLoader.INSTANCE.register(new PrintFunction());

    List<Function> functions = ModuleLoader.INSTANCE.loadModule(SystemModuleFunction.SYSTEM_MODULE_NAME);
    for (Function function : functions) {
      context.addFunction(function);
    }

    EvalVisitor visitor = new EvalVisitor(context);
    visitor.visit(statement);
  }
}
