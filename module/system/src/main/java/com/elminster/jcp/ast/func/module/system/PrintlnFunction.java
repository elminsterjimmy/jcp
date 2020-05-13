package com.elminster.jcp.ast.func.module.system;

import com.elminster.common.util.Assert;
import com.elminster.jcp.ast.FunctionDefinition;
import com.elminster.jcp.ast.data.AnyFlowData;
import com.elminster.jcp.ast.data.DataType;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.data.FlowDataFactory;
import com.elminster.jcp.eval.context.EvalContext;

public class PrintlnFunction extends SystemModuleFunction {

  public PrintlnFunction() {
    super(new FunctionDefinition("println", new FlowData[]{
        FlowDataFactory.INSTANCE.createFlowDataVariable("msg", DataType.STRING)
    }));
  }

  @Override
  public String getName() {
    return "PRINTLN";
  }

  @Override
  protected FlowData doFunc(EvalContext evalContext) {
    Assert.isTrue(getParameters().length > 0);
    FlowData msg = getParameters()[0];
    System.out.println(msg.get());
    return AnyFlowData.EMPTY;
  }
}
