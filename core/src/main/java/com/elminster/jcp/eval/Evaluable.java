package com.elminster.jcp.eval;

import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.eval.context.EvalContext;

public interface Evaluable {

  FlowData eval(EvalContext evalContext);
}
