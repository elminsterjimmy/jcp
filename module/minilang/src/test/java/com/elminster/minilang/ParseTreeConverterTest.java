package com.elminster.minilang;

import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.declaration.VariableDeclaration;
import com.elminster.jcp.ast.statement.declaration.FunctionDeclaration;
import com.elminster.jcp.ast.statement.control.IfElseStatement;
import com.elminster.jcp.ast.statement.control.WhileStatement;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static com.elminster.minilang.TestUtils.loadTestScript;

/**
 * Unit tests for ParseTreeConverter using separate test script files.
 *
 * <p>Test scripts are stored in src/test/resources/test-scripts/ for easy review and modification.
 * Each test validates specific parsing functionality by loading and parsing dedicated script files.
 *
 * <p>For end-to-end dual-mode validation, see {@link MiniLangDualModeTest}.
 */
public class ParseTreeConverterTest {

    private final ParseTreeConverter converter = new ParseTreeConverter("test.minilang", "");

    @Test
    void testParseSimpleVariableDeclaration() throws Exception {
        String source = loadTestScript("unit-tests/simple-variable.minilang");
        Block program = converter.parse(source);

        assertEquals(1, program.getBody().size());
        assertTrue(program.getBody().get(0) instanceof VariableDeclaration);

        VariableDeclaration varDecl = (VariableDeclaration) program.getBody().get(0);
        assertEquals("x", varDecl.getId().getId());
        assertEquals(SystemDataType.INT, varDecl.getDataType());
        assertNotNull(varDecl.getInit());
    }

    @Test
    void testParseMultipleVariables() throws Exception {
        String source = loadTestScript("unit-tests/multiple-variables.minilang");
        Block program = converter.parse(source);

        assertEquals(3, program.getBody().size());
        assertTrue(program.getBody().stream().allMatch(s -> s instanceof VariableDeclaration));
    }

    @Test
    void testParseFunctionDeclaration() throws Exception {
        String source = loadTestScript("functions/function-declaration.minilang");
        Block program = converter.parse(source);

        assertEquals(1, program.getBody().size());
        assertTrue(program.getBody().get(0) instanceof FunctionDeclaration);

        FunctionDeclaration funcDecl = (FunctionDeclaration) program.getBody().get(0);
        assertEquals("add", funcDecl.getId().getId());
        assertEquals(SystemDataType.INT, funcDecl.getDataType());
        assertEquals(2, funcDecl.getParameterDefines().length);
    }

    @Test
    void testParseIfStatement() throws Exception {
        String source = loadTestScript("control-flow/if-statement.minilang");
        Block program = converter.parse(source);

        assertEquals(1, program.getBody().size());
        assertTrue(program.getBody().get(0) instanceof IfElseStatement);
    }

    @Test
    void testParseIfElseStatement() throws Exception {
        String source = loadTestScript("control-flow/if-else-statement.minilang");
        Block program = converter.parse(source);

        assertEquals(1, program.getBody().size());
        assertTrue(program.getBody().get(0) instanceof IfElseStatement);
    }

    @Test
    void testParseWhileLoop() throws Exception {
        String source = loadTestScript("control-flow/while-loop.minilang");
        Block program = converter.parse(source);

        assertEquals(1, program.getBody().size());
        assertTrue(program.getBody().get(0) instanceof WhileStatement);
    }

    @Test
    void testTypeResolution() throws Exception {
        String source = loadTestScript("unit-tests/type-resolution.minilang");
        Block program = converter.parse(source);

        VariableDeclaration intDecl = (VariableDeclaration) program.getBody().get(0);
        assertEquals(SystemDataType.INT, intDecl.getDataType());

        VariableDeclaration doubleDecl = (VariableDeclaration) program.getBody().get(1);
        assertEquals(SystemDataType.DOUBLE, doubleDecl.getDataType());

        VariableDeclaration boolDecl = (VariableDeclaration) program.getBody().get(2);
        assertEquals(SystemDataType.BOOLEAN, boolDecl.getDataType());

        VariableDeclaration stringDecl = (VariableDeclaration) program.getBody().get(3);
        assertEquals(SystemDataType.STRING, stringDecl.getDataType());
    }

    @Test
    void testSyntaxError() throws Exception {
        String source = loadTestScript("unit-tests/syntax-error.minilang");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            converter.parse(source);
        });

        assertTrue(exception.getMessage().contains("Syntax error"));
    }

    @Test
    void testEmptyProgram() {
        String source = "";
        Block program = converter.parse(source);

        assertNotNull(program);
        assertEquals(0, program.getBody().size());
    }

    @Test
    void testBlankLines() throws Exception {
        String source = loadTestScript("unit-tests/blank-lines.minilang");
        Block program = converter.parse(source);

        // Should only have 2 statements, blank lines filtered out
        assertEquals(2, program.getBody().size());
    }

    @Test
    void testComments() throws Exception {
        String source = loadTestScript("unit-tests/comments.minilang");
        Block program = converter.parse(source);

        // Comments should be filtered out
        assertEquals(2, program.getBody().size());
    }
}
