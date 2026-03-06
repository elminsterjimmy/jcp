package com.elminster.minilang;

import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.declaration.VariableDeclaration;
import com.elminster.jcp.ast.statement.declaration.FunctionDeclaration;
import com.elminster.jcp.ast.statement.control.IfElseStatement;
import com.elminster.jcp.ast.statement.control.WhileStatement;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ParseTreeConverter focusing on parser functionality.
 *
 * <p>These tests validate the converter's ability to parse various MiniLang constructs
 * into correct AST node types. For end-to-end dual-mode validation, see {@link MiniLangIntegrationTest}.
 */
public class ParseTreeConverterTest {

    private final ParseTreeConverter converter = new ParseTreeConverter("test.minilang", "");

    @Test
    void testParseSimpleVariableDeclaration() {
        String source = "let x: int = 42\n";
        Block program = converter.parse(source);

        assertEquals(1, program.getBody().size());
        assertTrue(program.getBody().get(0) instanceof VariableDeclaration);

        VariableDeclaration varDecl = (VariableDeclaration) program.getBody().get(0);
        assertEquals("x", varDecl.getId().getId());
        assertEquals(SystemDataType.INT, varDecl.getDataType());
        assertNotNull(varDecl.getInit());
    }

    @Test
    void testParseMultipleVariables() {
        String source = "let x: int = 10\nlet y: int = 20\nlet z: int = 30\n";
        Block program = converter.parse(source);

        assertEquals(3, program.getBody().size());
        assertTrue(program.getBody().stream().allMatch(s -> s instanceof VariableDeclaration));
    }

    @Test
    void testParseFunctionDeclaration() {
        String source = "func add(a: int, b: int) -> int {\nreturn a + b\n}\n";
        Block program = converter.parse(source);

        assertEquals(1, program.getBody().size());
        assertTrue(program.getBody().get(0) instanceof FunctionDeclaration);

        FunctionDeclaration funcDecl = (FunctionDeclaration) program.getBody().get(0);
        assertEquals("add", funcDecl.getId().getId());
        assertEquals(SystemDataType.INT, funcDecl.getDataType());
        assertEquals(2, funcDecl.getParameterDefines().length);
    }

    @Test
    void testParseIfStatement() {
        String source = "if x > 5 {\nresult = 1\n}\n";
        Block program = converter.parse(source);

        assertEquals(1, program.getBody().size());
        assertTrue(program.getBody().get(0) instanceof IfElseStatement);
    }

    @Test
    void testParseIfElseStatement() {
        String source = "if x > 5 {\nresult = 1\n} else {\nresult = 0\n}\n";
        Block program = converter.parse(source);

        assertEquals(1, program.getBody().size());
        assertTrue(program.getBody().get(0) instanceof IfElseStatement);
    }

    @Test
    void testParseWhileLoop() {
        String source = "while counter < 5 {\ncounter = counter + 1\n}\n";
        Block program = converter.parse(source);

        assertEquals(1, program.getBody().size());
        assertTrue(program.getBody().get(0) instanceof WhileStatement);
    }

    @Test
    void testTypeResolution() {
        String source = "let a: int = 1\nlet b: double = 2.0\nlet c: boolean = true\nlet d: string = \"text\"\n";
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
    void testSyntaxError() {
        String source = "let x int = 10\n";  // Missing colon

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            converter.parse(source);
        });

        assertTrue(exception.getMessage().contains("Syntax error"));
        assertTrue(exception.getMessage().contains("test.minilang"));
    }

    @Test
    void testEmptyProgram() {
        String source = "";
        Block program = converter.parse(source);

        assertNotNull(program);
        assertEquals(0, program.getBody().size());
    }

    @Test
    void testBlankLines() {
        String source = "\n\nlet x: int = 10\n\n\nlet y: int = 20\n\n";
        Block program = converter.parse(source);

        // Should only have 2 statements, blank lines filtered out
        assertEquals(2, program.getBody().size());
    }

    @Test
    void testComments() {
        String source = "# This is a comment\nlet x: int = 10  # Inline comment\n# Another comment\nlet y: int = 20\n";
        Block program = converter.parse(source);

        // Comments should be filtered out
        assertEquals(2, program.getBody().size());
    }
}
