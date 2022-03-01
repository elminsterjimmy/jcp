package utils;

import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.Data;

abstract public class AssertableEvalVisitor extends EvalVisitor {

    public AssertableEvalVisitor(EvalContext context) {
        super(context);
    }

    @Override
    protected void afterEval(Data eval) {
        super.afterEval(eval);
        assertEval(eval);
    }

    abstract protected void assertEval(Data eval);
}
