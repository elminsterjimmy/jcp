package com.elminster.jcp.compile.operator.arithmetic;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.operation.Plus;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for integer arithmetic compilation.
 */
public class IntArithmeticCompileTest extends AbstractCompileTest {

    @Test
    void testArithmeticExpression() throws Exception {
        // int x = 1 + 2;
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                new Plus(
                        LiteralExpression.of(1),
                        LiteralExpression.of(2)
                )
        ));

        byte[] bytecode = compiler.compileToBytes(program, uniqueClassName("TestArithmetic"));
        assertNotNull(bytecode);
    }
}
