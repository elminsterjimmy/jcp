package com.elminster.jcp.ast.func.module.system;

import com.elminster.common.util.Assert;
import com.elminster.jcp.ast.expression.Identifier;
import com.elminster.jcp.ast.expression.base.IdentifierExpression;
import com.elminster.jcp.ast.statement.ParameterDef;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataFactory;

public class PrintlnFunction extends SystemModuleFunction {

  @Override
  public String getName() {
    return "PRINTLN";
  }

  @Override
  protected Data doFunction(Data[] parameters) {
    Assert.isTrue(getParameters().length > 0);
    Data msg = getParameters()[0];
    System.out.println(msg.get());
    return AnyData.EMPTY;
  }

  @Override
  public Identifier getId() {
    return new IdentifierExpression("println");
  }

  @Override
  public ParameterDef[] getParameterDefs() {
    return new ParameterDef[]{
        new ParameterDef("msg", DataType.SystemDataType.STRING.getName())
    };
  }

  @Override
  public DataType getResultDataType() {
    return DataType.SystemDataType.VOID;
  }
}
