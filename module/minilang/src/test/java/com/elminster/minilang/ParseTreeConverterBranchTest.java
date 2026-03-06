package com.elminster.minilang;

import com.elminster.jcp.ast.statement.Block;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional converter tests targeting specific code paths to improve branch coverage.
 */
public class ParseTreeConverterBranchTest {

    @Test
    void testEmptyProgram() throws Exception {
        String source = "";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);
        Block program = converter.parse(source);
        assertNotNull(program);
    }

    @Test
    void testProgramWithOnlyNewlines() throws Exception {
        String source = "\n\n\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);
        Block program = converter.parse(source);
        assertNotNull(program);
    }

    @Test
    void testFunctionWithNoParameters() throws Exception {
        String source = "func test() -> int {\nreturn 42\n}\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);
        Block program = converter.parse(source);
        assertNotNull(program);
    }

    @Test
    void testFunctionWithMultipleParameters() throws Exception {
        String source = "func add(a: int, b: int, c: int) -> int {\nreturn a\n}\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);
        Block program = converter.parse(source);
        assertNotNull(program);
    }

    @Test
    void testIfWithoutElse() throws Exception {
        String source = "func test() -> boolean {\nif true {\nreturn true\n}\nreturn false\n}\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);
        Block program = converter.parse(source);
        assertNotNull(program);
    }

    @Test
    void testIfWithElse() throws Exception {
        String source = "func test() -> boolean {\nif true {\nreturn true\n} else {\nreturn false\n}\n}\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);
        Block program = converter.parse(source);
        assertNotNull(program);
    }

    @Test
    void testFunctionCallWithNoArguments() throws Exception {
        String source = "func test() -> int {\nreturn 0\n}\nlet x: int = test()\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);
        Block program = converter.parse(source);
        assertNotNull(program);
    }

    @Test
    void testFunctionCallWithMultipleArguments() throws Exception {
        String source = "func add(a: int, b: int) -> int {\nreturn a\n}\nlet x: int = add(1, 2)\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);
        Block program = converter.parse(source);
        assertNotNull(program);
    }

    @Test
    void testAllLiteralTypes() throws Exception {
        String source = "let i: int = 42\nlet d: double = 3.14\nlet s: string = \"hello\"\nlet b: boolean = true\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);
        Block program = converter.parse(source);
        assertNotNull(program);
    }

    @Test
    void testAllBinaryOperators() throws Exception {
        String source = "func test() -> boolean {\n" +
                "let a: int = 1 + 2\n" +
                "let b: int = 3 - 1\n" +
                "let c: int = 2 * 3\n" +
                "let d: int = 6 / 2\n" +
                "let e: int = 5 % 2\n" +
                "let f: boolean = 1 < 2\n" +
                "let g: boolean = 3 > 2\n" +
                "let h: boolean = 1 <= 1\n" +
                "let i: boolean = 2 >= 2\n" +
                "let j: boolean = 1 == 1\n" +
                "let k: boolean = 1 != 2\n" +
                "let l: boolean = true && true\n" +
                "let m: boolean = false || true\n" +
                "return true\n" +
                "}\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);
        Block program = converter.parse(source);
        assertNotNull(program);
    }

    @Test
    void testUnaryOperators() throws Exception {
        String source = "func test() -> boolean {\n" +
                "let x: boolean = !true\n" +
                "return x\n" +
                "}\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);
        Block program = converter.parse(source);
        assertNotNull(program);
    }

    @Test
    void testMemberAccessModuleFunction() throws Exception {
        String source = "let x: boolean = Assertions.assertTrue(true)\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);
        Block program = converter.parse(source);
        assertNotNull(program);
    }

    @Test
    void testExpressionStatement() throws Exception {
        String source = "func test() {\nlet x: int = 5\n}\ntest()\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);
        Block program = converter.parse(source);
        assertNotNull(program);
    }

    @Test
    void testReturnVoid() throws Exception {
        String source = "func test() {\nlet x: int = 5\n}\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);
        Block program = converter.parse(source);
        assertNotNull(program);
    }

    @Test
    void testParenthesesExpression() throws Exception {
        String source = "let x: int = (5 + 3) * 2\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);
        Block program = converter.parse(source);
        assertNotNull(program);
    }

    @Test
    void testAssignmentExpression() throws Exception {
        String source = "let x: int = 5\nx = 10\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);
        Block program = converter.parse(source);
        assertNotNull(program);
    }

    @Test
    void testWhileWithBreak() throws Exception {
        String source = "func test() -> boolean {\nwhile true {\nbreak\n}\nreturn true\n}\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);
        Block program = converter.parse(source);
        assertNotNull(program);
    }

    @Test
    void testWhileWithContinue() throws Exception {
        String source = "func test() -> boolean {\nlet x: int = 0\nwhile x < 5 {\nx = x + 1\ncontinue\n}\nreturn true\n}\n";
        ParseTreeConverter converter = new ParseTreeConverter("test.minilang", source);
        Block program = converter.parse(source);
        assertNotNull(program);
    }
}
