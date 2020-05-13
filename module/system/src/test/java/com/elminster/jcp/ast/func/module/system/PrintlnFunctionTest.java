package com.elminster.jcp.ast.func.module.system;

import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.data.StringFlowData;
import com.elminster.jcp.ast.express.core.FunctionCallExpression;
import com.elminster.jcp.ast.expression.ConstantExpression;
import com.elminster.jcp.ast.expression.Expression;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.Function;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.EvalContextImpl;
import com.elminster.jcp.util.ModuleLoader;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Set;

public class PrintlnFunctionTest {

  @BeforeClass
  public static void init() {
    ModuleLoader.INSTANCE.loadModule(SystemModuleFunction.SYSTEM_MODULE_NAME);
  }

  @Test
  public void testPrintlnFunction() {
    FunctionCallExpression funcall = new FunctionCallExpression("println",
        new Expression[]{new ConstantExpression(StringFlowData.newString("TEST"))});
    Statement statement = new ExpressionStatement(funcall);

    EvalContext context = new EvalContextImpl();
    ModuleLoader.INSTANCE.register(new PrintlnFunction());
    List<Function> functions = ModuleLoader.INSTANCE.loadModule(SystemModuleFunction.SYSTEM_MODULE_NAME);
    for (Function function : functions) {
      context.addFunction(function);
    }

    EvalVisitor visitor = new EvalVisitor(context);
    visitor.visit(statement);
  }
}
