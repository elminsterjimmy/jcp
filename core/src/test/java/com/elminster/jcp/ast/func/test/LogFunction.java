package com.elminster.jcp.ast.func.test;

import com.elminster.common.util.Assert;
import com.elminster.jcp.ast.AbstractFunction;
import com.elminster.jcp.ast.data.AnyFlowData;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.ast.data.DataType;
import com.elminster.jcp.ast.FunctionDefinition;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.data.FlowDataFactory;

public class LogFunction extends AbstractFunction {

  public LogFunction() {
    super(new FunctionDefinition("log", new FlowData[]{
        FlowDataFactory.INSTANCE.createFlowDataVariable("msg", DataType.STRING)
    }));
  }

  @Override
  public String getName() {
    return "LOG";
  }

  @Override
  protected FlowData doFunc(EvalContext evalContext) {
    Assert.isTrue(getParameters().length > 0);
    FlowData msg = getParameters()[0];
    System.out.println(msg.get());
    return AnyFlowData.EMPTY;
  }
}
