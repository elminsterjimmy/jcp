package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.expression.StaticMethodCallExpression;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.excpetion.UndeclaredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StaticMethodCallEvaluator.
 */
class StaticMethodCallEvaluatorTest {

    private RootEvalContext context;

    @BeforeEach
    void setUp() {
        context = new RootEvalContext();
    }

    /**
     * Tests that calling an undefined static method throws UndeclaredException.
     * <pre>
     * SomeType.undefinedMethod()  // throws UndeclaredException
     * </pre>
     */
    @Test
    void testUndefinedStaticMethod_ThrowsException() {
        Block program = new BlockImpl();
        program.addStatement(new ExpressionStatement(
            new StaticMethodCallExpression("NonExistentType", "undefinedMethod")
        ));

        assertThrows(UndeclaredException.FunctionUndeclaredException.class, () ->
            new EvalVisitor(context).visit(program)
        );
    }
}
