package com.elminster.jcp.eval.declare;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.statement.declaration.VariableDeclaration;
import com.elminster.jcp.eval.base.AbstractAstEvaluator;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataFactory;
import com.elminster.jcp.eval.excpetion.CannotCastException;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;
import com.elminster.jcp.util.DataTypeUtils;

public class VariableDeclarationEvaluator extends AbstractAstEvaluator {

  public VariableDeclarationEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    VariableDeclaration variableDeclaration = (VariableDeclaration) astNode;
    Identifier id = variableDeclaration.getId();
    Expression initExpress = variableDeclaration.getInit();
    Data variable = DataFactory.INSTANCE.createVariable(id,
        DataTypeUtils.getDataType(variableDeclaration.getDataType().getName(), evalContext));

    if (null != initExpress) {
      Data initValue = AstEvaluatorFactory.getEvaluator(initExpress).eval(evalContext);
      if (!initValue.getDataType().isCastableTo(variable.getDataType())) {
        throw new CannotCastException(initValue.getDataType(), variable.getDataType(), getSourceLocation());
      }

      // For custom types (like structs), use the instance directly instead of wrapping
      if (initValue.getDataType() instanceof com.elminster.jcp.eval.data.StructType) {
        // Replace variable with the struct instance but with the correct identifier
        com.elminster.jcp.eval.data.StructData structData = (com.elminster.jcp.eval.data.StructData) initValue;
        variable = new com.elminster.jcp.eval.data.StructData(
            id,
            structData.getStructType(),
            (java.util.Map<String, Data>) structData.get()
        );
      } else if (variable.getDataType() == com.elminster.jcp.eval.data.DataType.SystemDataType.ANY
                 && initValue.getDataType() != com.elminster.jcp.eval.data.DataType.SystemDataType.ANY) {
        // Declared as ANY but initializer has a concrete type — promote the variable's type
        // so subsequent function calls can resolve methods on the concrete type.
        variable = new AnyData<>(id, initValue.getDataType(), initValue.get());
      } else {
        variable.set(initValue.get());
      }
    }

    evalContext.addVariable(variable);
    return AnyData.EMPTY;
  }
}
